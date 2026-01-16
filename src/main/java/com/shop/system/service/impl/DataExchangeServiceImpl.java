package com.shop.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.ExchangeFileDto;
import com.shop.system.exception.BusinessException;
import com.shop.system.service.DataExchangeService;
import com.shop.system.service.support.ExchangeLogWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DataExchangeServiceImpl implements DataExchangeService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ExchangeLogWriter exchangeLogWriter;
    private final PlatformTransactionManager transactionManager;

    private static final List<String> REFERENCE_TABLES = List.of(
            "manufacturer",
            "product_categories",
            "contractors",
            "storage_locations",
            "departments",
            "positions",
            "product",
            "product_category_links"
    );

    // caches metadata
    private final Map<String, Set<String>> columnsCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> udtTypesCache = new ConcurrentHashMap<>();
    private final Map<String, String> pkConstraintCache = new ConcurrentHashMap<>();

    // -------------------- REFERENCE PACK --------------------

    /**
     * Важно: НЕ наследуем возможную внешнюю транзакцию (в т.ч. readOnly),
     * иначе логирование/запросы могут попасть в "сломанный" контекст.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ExchangeFileDto buildReferencePack() {
        UUID opEntityId = UUID.randomUUID();
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "REFERENCE_PACK");
            root.put("generatedAt", LocalDateTime.now().toString());

            ObjectNode data = objectMapper.createObjectNode();
            for (String table : REFERENCE_TABLES) {
                data.set(table, objectMapper.valueToTree(selectAll(table)));
            }
            root.set("data", data);

            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
            String filename = "reference-pack-" + LocalDate.now() + ".json";

            logSuccessNewTx("reference_pack", opEntityId, "update", "download",
                    "Скачан пакет справочников. tables=" + REFERENCE_TABLES + ", bytes=" + bytes.length);

            return new ExchangeFileDto(filename, bytes);
        } catch (Exception e) {
            logErrorNewTx("reference_pack", opEntityId, "update", "download", e);
            throw new BusinessException("Не удалось сформировать пакет справочников: " + rootSqlMessage(e));
        }
    }

    /**
     * Важно: делаем метод без внешней транзакции.
     * Все реальные операции — в REQUIRES_NEW внутри.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ApiResponse importReferencePack(MultipartFile file, boolean overwrite) {
        if (file == null || file.isEmpty()) throw new BusinessException("Файл не передан или пустой");

        UUID opEntityId = UUID.randomUUID();
        Map<UUID, UUID> productIdMap = new HashMap<>();

        try {
            JsonNode json = objectMapper.readTree(file.getBytes());
            JsonNode dataNode = json.get("data");
            if (dataNode == null || !dataNode.isObject()) {
                throw new BusinessException("Некорректный формат пакета: отсутствует объект data");
            }

            Map<String, Integer> importedCounts = new LinkedHashMap<>();

            for (String table : REFERENCE_TABLES) {
                JsonNode arr = dataNode.get(table);
                if (arr == null || !arr.isArray() || arr.isEmpty()) continue;

                int cnt = importOneTableInNewTx(table, arr, overwrite, productIdMap);
                importedCounts.put(table, cnt);
            }

            logSuccessNewTx("reference_pack", opEntityId, "update", "upload",
                    "Импорт пакета справочников. overwrite=" + overwrite + ", importedCounts=" + importedCounts);

            return new ApiResponse(true, "Пакет справочников импортирован");
        } catch (BusinessException be) {
            logErrorNewTx("reference_pack", opEntityId, "update", "upload", be.getMessage());
            throw be;
        } catch (Exception e) {
            String root = rootSqlMessage(e);
            logErrorNewTx("reference_pack", opEntityId, "update", "upload", root);
            throw new BusinessException("Не удалось импортировать пакет: " + root);
        }
    }

    private int importOneTableInNewTx(String table, JsonNode arr, boolean overwrite, Map<UUID, UUID> productIdMap) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.setReadOnly(false);

        return tt.execute(status -> {
            try {
                Set<String> colsLower = getTableColumnsLower(table);
                Map<String, String> udtMap = getColumnUdtTypesLower(table);

                if ("product".equals(table)) {
                    return importProductsWithBarcodeMapping(arr, overwrite, colsLower, udtMap, productIdMap);
                }

                if ("product_category_links".equals(table)) {
                    return importProductCategoryLinks(arr, overwrite, colsLower, udtMap, productIdMap);
                }

                return overwrite
                        ? upsertTable(table, arr, colsLower, udtMap)
                        : insertIgnoreConflicts(table, arr, colsLower, udtMap);

            } catch (Exception e) {
                status.setRollbackOnly();
                // ВАЖНО: не продолжаем выполнять запросы в этой TX
                throw e;
            }
        });
    }

    // -------------------- EXPORTS --------------------

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ExchangeFileDto exportSales(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null || dateTo == null) throw new BusinessException("dateFrom/dateTo обязательны");
        if (dateFrom.isAfter(dateTo)) throw new BusinessException("dateFrom не может быть позже dateTo");

        UUID opEntityId = UUID.randomUUID();

        try {
            var orders = jdbcTemplate.queryForList(
                    "SELECT * FROM orders WHERE order_date >= ? AND order_date < ?",
                    dateFrom.atStartOfDay(),
                    dateTo.plusDays(1).atStartOfDay()
            );

            var items = jdbcTemplate.queryForList(
                    "SELECT oi.* FROM order_items oi " +
                            "JOIN orders o ON o.id = oi.order_id " +
                            "WHERE o.order_date >= ? AND o.order_date < ?",
                    dateFrom.atStartOfDay(),
                    dateTo.plusDays(1).atStartOfDay()
            );

            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "SALES_EXPORT");
            root.put("dateFrom", dateFrom.toString());
            root.put("dateTo", dateTo.toString());
            root.set("orders", objectMapper.valueToTree(orders));
            root.set("orderItems", objectMapper.valueToTree(items));

            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
            String filename = "sales-" + dateFrom + "-" + dateTo + ".json";

            logSuccessNewTx("orders", opEntityId, "insert", "upload",
                    "Экспорт продаж: orders=" + orders.size() + ", items=" + items.size() + ", bytes=" + bytes.length);

            return new ExchangeFileDto(filename, bytes);
        } catch (Exception e) {
            logErrorNewTx("orders", opEntityId, "insert", "upload", e);
            throw new BusinessException("Не удалось выгрузить продажи: " + rootSqlMessage(e));
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ExchangeFileDto exportStock(LocalDate snapshotDate) {
        if (snapshotDate == null) throw new BusinessException("snapshotDate обязателен");

        UUID opEntityId = UUID.randomUUID();

        try {
            var inventory = jdbcTemplate.queryForList(
                    "SELECT * FROM inventory WHERE inventory_date = ?",
                    snapshotDate
            );

            var inventoryItems = jdbcTemplate.queryForList(
                    "SELECT ii.* FROM inventory_items ii " +
                            "JOIN inventory i ON i.id = ii.inventory_id " +
                            "WHERE i.inventory_date = ?",
                    snapshotDate
            );

            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "STOCK_EXPORT");
            root.put("snapshotDate", snapshotDate.toString());
            root.set("inventory", objectMapper.valueToTree(inventory));
            root.set("inventoryItems", objectMapper.valueToTree(inventoryItems));

            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
            String filename = "stock-" + snapshotDate + ".json";

            logSuccessNewTx("inventory", opEntityId, "update", "upload",
                    "Экспорт остатков: inventory=" + inventory.size()
                            + ", items=" + inventoryItems.size()
                            + ", bytes=" + bytes.length);

            return new ExchangeFileDto(filename, bytes);

        } catch (Exception e) {
            logErrorNewTx("inventory", opEntityId, "update", "upload", e);
            throw new BusinessException("Не удалось выгрузить остатки: " + rootSqlMessage(e));
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ExchangeFileDto exportStopList(LocalDate date) {
        if (date == null) throw new BusinessException("date обязателен");

        UUID opEntityId = UUID.randomUUID();

        try {
            List<Map<String, Object>> stopList;

            if (hasColumn("stop_list", "start_date") && hasColumn("stop_list", "end_date")) {
                String activePart = hasColumn("stop_list", "is_active")
                        ? "AND (is_active = TRUE OR is_active IS NULL)"
                        : "";

                stopList = jdbcTemplate.queryForList(
                        "SELECT * FROM stop_list " +
                                "WHERE (start_date IS NULL OR start_date <= ?) " +
                                "AND (end_date IS NULL OR end_date >= ?) " +
                                activePart,
                        date, date
                );
            } else {
                stopList = jdbcTemplate.queryForList("SELECT * FROM stop_list");
            }

            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "STOP_LIST_EXPORT");
            root.put("date", date.toString());
            root.set("stopList", objectMapper.valueToTree(stopList));

            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
            String filename = "stop-list-" + date + ".json";

            logSuccessNewTx("stop_list", opEntityId, "update", "upload",
                    "Экспорт стоп-листа: rows=" + stopList.size() + ", bytes=" + bytes.length);

            return new ExchangeFileDto(filename, bytes);
        } catch (Exception e) {
            logErrorNewTx("stop_list", opEntityId, "update", "upload", e);
            throw new BusinessException("Не удалось выгрузить стоп-лист: " + rootSqlMessage(e));
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ExchangeFileDto exportSnapshot(LocalDate snapshotDate) {
        if (snapshotDate == null) throw new BusinessException("snapshotDate обязателен");

        UUID opEntityId = UUID.randomUUID();

        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "DB_SNAPSHOT");
            root.put("snapshotDate", snapshotDate.toString());
            root.put("generatedAt", LocalDateTime.now().toString());

            ObjectNode data = objectMapper.createObjectNode();

            ObjectNode refs = objectMapper.createObjectNode();
            for (String table : REFERENCE_TABLES) {
                refs.set(table, objectMapper.valueToTree(selectAll(table)));
            }
            data.set("referencePack", refs);

            data.set("stock", objectMapper.valueToTree(
                    jdbcTemplate.queryForList("SELECT * FROM inventory WHERE date = ?", snapshotDate)
            ));

            data.set("stopList", objectMapper.valueToTree(
                    jdbcTemplate.queryForList("SELECT * FROM stop_list")
            ));

            root.set("data", data);

            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
            String filename = "snapshot-" + snapshotDate + ".json";

            logSuccessNewTx("snapshot", opEntityId, "update", "upload",
                    "Сформирован слепок базы. bytes=" + bytes.length);

            return new ExchangeFileDto(filename, bytes);
        } catch (Exception e) {
            logErrorNewTx("snapshot", opEntityId, "update", "upload", e);
            throw new BusinessException("Не удалось сформировать слепок: " + rootSqlMessage(e));
        }
    }

    // -------------------- IMPORT: PRODUCTS (barcode mapping) --------------------

    private int importProductsWithBarcodeMapping(
            JsonNode arrayNode,
            boolean overwrite,
            Set<String> existingColsLower,
            Map<String, String> udtMap,
            Map<UUID, UUID> productIdMap
    ) {
        int processed = 0;

        for (JsonNode rowNode : arrayNode) {
            if (!rowNode.isObject()) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> row = objectMapper.convertValue(rowNode, Map.class);

            UUID incomingId = (UUID) convertForUdt("uuid", getRawIgnoreCase(row, "id"));
            String barcode = (String) convertForUdt("text", getRawIgnoreCase(row, "barcode"));
            if (incomingId == null || barcode == null || barcode.isBlank()) continue;

            UUID existingId = findProductIdByBarcode(barcode);

            if (existingId != null) {
                productIdMap.put(incomingId, existingId);
                if (overwrite) updateProductById(existingId, row, existingColsLower, udtMap);
                processed++;
                continue;
            }

            try {
                insertRowPlain("product", row, existingColsLower, udtMap);
                productIdMap.put(incomingId, incomingId);
            } catch (Exception ex) {
                if (isDuplicateKey(ex)) {
                    UUID idNow = findProductIdByBarcode(barcode);
                    if (idNow != null) {
                        productIdMap.put(incomingId, idNow);
                        if (overwrite) updateProductById(idNow, row, existingColsLower, udtMap);
                    }
                } else {
                    throw ex;
                }
            }

            processed++;
        }

        return processed;
    }

    private UUID findProductIdByBarcode(String barcode) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM product WHERE barcode = ?",
                    UUID.class,
                    barcode
            );
        } catch (DataAccessException ex) {
            return null;
        }
    }

    private void updateProductById(UUID id, Map<String, Object> row, Set<String> existingColsLower, Map<String, String> udtMap) {
        List<String> cols = row.keySet().stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .filter(existingColsLower::contains)
                .filter(c -> !c.equals("id"))
                .filter(c -> !c.equals("barcode"))
                .sorted()
                .toList();

        if (cols.isEmpty()) return;

        String setPart = String.join(", ", cols.stream().map(c -> c + " = ?").toList());
        String sql = "UPDATE product SET " + setPart + " WHERE id = ?";

        List<Object> args = new ArrayList<>();
        for (String c : cols) args.add(getTypedIgnoreCase(row, c, udtMap.getOrDefault(c, "text")));
        args.add(id);

        jdbcTemplate.update(sql, args.toArray());
    }

    // -------------------- IMPORT: LINKS (use mapping) --------------------

    private int importProductCategoryLinks(
            JsonNode arrayNode,
            boolean overwrite,
            Set<String> existingColsLower,
            Map<String, String> udtMap,
            Map<UUID, UUID> productIdMap
    ) {
        int processed = 0;

        for (JsonNode rowNode : arrayNode) {
            if (!rowNode.isObject()) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> row = objectMapper.convertValue(rowNode, Map.class);

            UUID incomingProductId = (UUID) convertForUdt("uuid", getRawIgnoreCase(row, "product_id"));
            if (incomingProductId != null) {
                UUID mapped = productIdMap.get(incomingProductId);
                if (mapped != null) {
                    row.put("product_id", mapped.toString());
                } else {
                    // если продукт реально не найден — пропускаем, чтобы не ловить FK
                    continue;
                }
            }

            JsonNode single = objectMapper.valueToTree(List.of(row));

            if (overwrite) {
                processed += upsertTable("product_category_links", single, existingColsLower, udtMap);
            } else {
                processed += insertIgnoreConflictsSkipDuplicates("product_category_links", single, existingColsLower, udtMap);
            }
        }

        return processed;
    }

    // -------------------- IMPORT CORE (generic) --------------------

    private int insertIgnoreConflicts(String table, JsonNode arrayNode, Set<String> existingColsLower, Map<String, String> udtMap) {
        int inserted = 0;
        String pkConstraint = getPkConstraintName(table);

        for (JsonNode rowNode : arrayNode) {
            if (!rowNode.isObject()) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> row = objectMapper.convertValue(rowNode, Map.class);

            List<String> cols = row.keySet().stream()
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .filter(existingColsLower::contains)
                    .sorted()
                    .toList();

            if (cols.isEmpty()) continue;

            String colList = String.join(", ", cols);
            String placeholders = String.join(", ", cols.stream().map(c -> "?").toList());

            String sql = "INSERT INTO " + table + " (" + colList + ") VALUES (" + placeholders + ") " +
                    "ON CONFLICT ON CONSTRAINT " + pkConstraint + " DO NOTHING";

            Object[] args = cols.stream()
                    .map(c -> getTypedIgnoreCase(row, c, udtMap.getOrDefault(c, "text")))
                    .toArray();

            int upd = jdbcTemplate.update(sql, args);
            if (upd > 0) inserted++;
        }

        return inserted;
    }

    private int upsertTable(String table, JsonNode arrayNode, Set<String> existingColsLower, Map<String, String> udtMap) {
        int processed = 0;
        String pkConstraint = getPkConstraintName(table);

        for (JsonNode rowNode : arrayNode) {
            if (!rowNode.isObject()) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> row = objectMapper.convertValue(rowNode, Map.class);

            List<String> cols = row.keySet().stream()
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .filter(existingColsLower::contains)
                    .sorted()
                    .toList();

            if (cols.isEmpty()) continue;

            String colList = String.join(", ", cols);
            String placeholders = String.join(", ", cols.stream().map(c -> "?").toList());

            List<String> updCols = cols.stream().filter(c -> !c.equals("id")).toList();
            String updates = String.join(", ", updCols.stream().map(c -> c + " = EXCLUDED." + c).toList());

            String sql = updates.isBlank()
                    ? "INSERT INTO " + table + " (" + colList + ") VALUES (" + placeholders + ") " +
                    "ON CONFLICT ON CONSTRAINT " + pkConstraint + " DO NOTHING"
                    : "INSERT INTO " + table + " (" + colList + ") VALUES (" + placeholders + ") " +
                    "ON CONFLICT ON CONSTRAINT " + pkConstraint + " DO UPDATE SET " + updates;

            Object[] args = cols.stream()
                    .map(c -> getTypedIgnoreCase(row, c, udtMap.getOrDefault(c, "text")))
                    .toArray();

            jdbcTemplate.update(sql, args);
            processed++;
        }

        return processed;
    }

    private int insertIgnoreConflictsSkipDuplicates(String table, JsonNode arrayNode, Set<String> existingColsLower, Map<String, String> udtMap) {
        int inserted = 0;

        for (JsonNode rowNode : arrayNode) {
            if (!rowNode.isObject()) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> row = objectMapper.convertValue(rowNode, Map.class);

            List<String> cols = row.keySet().stream()
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .filter(existingColsLower::contains)
                    .sorted()
                    .toList();

            if (cols.isEmpty()) continue;

            String colList = String.join(", ", cols);
            String placeholders = String.join(", ", cols.stream().map(c -> "?").toList());
            String sql = "INSERT INTO " + table + " (" + colList + ") VALUES (" + placeholders + ")";

            Object[] args = cols.stream()
                    .map(c -> getTypedIgnoreCase(row, c, udtMap.getOrDefault(c, "text")))
                    .toArray();

            try {
                jdbcTemplate.update(sql, args);
                inserted++;
            } catch (Exception ex) {
                if (isDuplicateKey(ex)) continue;
                throw ex;
            }
        }

        return inserted;
    }

    private void insertRowPlain(String table, Map<String, Object> row, Set<String> existingColsLower, Map<String, String> udtMap) {
        List<String> cols = row.keySet().stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .filter(existingColsLower::contains)
                .sorted()
                .toList();
        if (cols.isEmpty()) return;

        String colList = String.join(", ", cols);
        String placeholders = String.join(", ", cols.stream().map(c -> "?").toList());
        String sql = "INSERT INTO " + table + " (" + colList + ") VALUES (" + placeholders + ")";

        Object[] args = cols.stream()
                .map(c -> getTypedIgnoreCase(row, c, udtMap.getOrDefault(c, "text")))
                .toArray();

        jdbcTemplate.update(sql, args);
    }

    // -------------------- METADATA --------------------

    private List<Map<String, Object>> selectAll(String table) {
        return jdbcTemplate.queryForList("SELECT * FROM " + table);
    }

    private boolean hasColumn(String table, String column) {
        Integer cnt = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                table,
                column
        );
        return cnt != null && cnt > 0;
    }

    private Set<String> getTableColumnsLower(String table) {
        return columnsCache.computeIfAbsent(table, t -> {
            List<String> cols = jdbcTemplate.queryForList(
                    """
                    SELECT lower(column_name)
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = ?
                    """,
                    String.class,
                    t
            );
            return new HashSet<>(cols);
        });
    }

    private Map<String, String> getColumnUdtTypesLower(String table) {
        return udtTypesCache.computeIfAbsent(table, t -> {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    """
                    SELECT lower(column_name) AS col, lower(udt_name) AS udt
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = ?
                    """,
                    t
            );
            Map<String, String> map = new HashMap<>();
            for (Map<String, Object> r : rows) {
                map.put((String) r.get("col"), (String) r.get("udt"));
            }
            return map;
        });
    }

    private String getPkConstraintName(String table) {
        return pkConstraintCache.computeIfAbsent(table, t -> {
            String pk = jdbcTemplate.queryForObject(
                    """
                    SELECT c.conname
                    FROM pg_constraint c
                    JOIN pg_class cl ON cl.oid = c.conrelid
                    JOIN pg_namespace n ON n.oid = cl.relnamespace
                    WHERE c.contype = 'p'
                      AND n.nspname = current_schema()
                      AND cl.relname = ?
                    """,
                    String.class,
                    t
            );
            if (pk == null || pk.isBlank()) {
                throw new BusinessException("Для таблицы " + t + " не найден PRIMARY KEY constraint");
            }
            return pk;
        });
    }

    // -------------------- VALUE TYPING --------------------

    private Object getTypedIgnoreCase(Map<String, Object> row, String lowerKey, String udt) {
        Object raw = getRawIgnoreCase(row, lowerKey);
        return convertForUdt(udt, raw);
    }

    private Object getRawIgnoreCase(Map<String, Object> row, String lowerKey) {
        if (row.containsKey(lowerKey)) return row.get(lowerKey);
        for (String k : row.keySet()) {
            if (k != null && k.equalsIgnoreCase(lowerKey)) return row.get(k);
        }
        return null;
    }

    private Object convertForUdt(String udt, Object value) {
        if (value == null) return null;
        if (!(value instanceof String s)) return value;
        if (s.isBlank()) return null;

        return switch (udt) {
            case "uuid" -> UUID.fromString(s);
            case "date" -> LocalDate.parse(s);
            case "timestamp" -> {
                try { yield LocalDateTime.parse(s); }
                catch (Exception ignore) { yield OffsetDateTime.parse(s).toLocalDateTime(); }
            }
            case "timestamptz" -> {
                try { yield OffsetDateTime.parse(s); }
                catch (Exception ignore) { yield LocalDateTime.parse(s).atOffset(ZoneOffset.UTC); }
            }
            case "bool", "boolean" -> Boolean.parseBoolean(s);
            case "numeric", "decimal" -> new BigDecimal(s);
            case "int2" -> Short.valueOf(s);
            case "int4" -> Integer.valueOf(s);
            case "int8" -> Long.valueOf(s);
            case "float4" -> Float.valueOf(s);
            case "float8" -> Double.valueOf(s);
            default -> s;
        };
    }

    // -------------------- DUPLICATE KEY DETECTOR --------------------

    private boolean isDuplicateKey(Exception ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof SQLException sqlEx) {
                return "23505".equals(sqlEx.getSQLState());
            }
            t = t.getCause();
        }
        return false;
    }

    // -------------------- LOGGING IN NEW TX --------------------

    private void logSuccessNewTx(String entityName, UUID entityId, String operation, String direction, String message) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.setReadOnly(false);
        tt.execute(status -> {
            exchangeLogWriter.success(entityName, entityId, operation, direction, message);
            return null;
        });
    }

    private void logErrorNewTx(String entityName, UUID entityId, String operation, String direction, Throwable e) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.setReadOnly(false);
        tt.execute(status -> {
            exchangeLogWriter.error(entityName, entityId, operation, direction, rootSqlMessage(e));
            return null;
        });
    }


    private void logErrorNewTx(String entityName, UUID entityId, String operation, String direction, String message) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.setReadOnly(false);
        tt.execute(status -> {
            exchangeLogWriter.error(entityName, entityId, operation, direction, message);
            return null;
        });
    }

    // -------------------- ERROR UNWRAP --------------------

    private String rootSqlMessage(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof SQLException sqlEx) {
                return "SQLState=" + sqlEx.getSQLState() + ", message=" + sqlEx.getMessage();
            }
            t = t.getCause();
        }
        if (e instanceof DataAccessException dae && dae.getMostSpecificCause() != null) {
            return dae.getMostSpecificCause().getMessage();
        }
        return e.getMessage() == null ? "error" : e.getMessage();
    }
}