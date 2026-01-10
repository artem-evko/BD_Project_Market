package com.shop.system.service.impl;

import com.shop.system.domain.entity.*;
import com.shop.system.dto.request.*;
import com.shop.system.dto.response.*;
import com.shop.system.exception.BusinessException;
import com.shop.system.repository.*;
import com.shop.system.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;



import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final SupplyInvoiceRepository supplyInvoiceRepository;
    private final SupplyInvoiceItemRepository supplyInvoiceItemRepository;
    private final DiscrepancyRequestRepository discrepancyRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final StorageZoneRepository storageZoneRepository;
    private final BatchRepository batchRepository;
    private final BatchLocationRepository batchLocationRepository;
    private final WarehouseOperationRepository warehouseOperationRepository;


    // ========================
    // 1. Поиск по номеру
    // ========================
    @Override
    public UUID getSupplyInvoiceIdByNumber(String invoiceNumber) {
        SupplyInvoice invoice = supplyInvoiceRepository.findByInvoiceNumberIgnoreCase(invoiceNumber)
                .orElseThrow(() -> new BusinessException("Накладная с номером " + invoiceNumber + " не найдена"));
        return invoice.getId();
    }

    // ========================
    // 2. Получить накладную для приёмки
    // ========================
    @Override
    public GoodsReceiptResponse getSupplyInvoiceForReceipt(UUID invoiceId) {
        SupplyInvoice invoice = supplyInvoiceRepository.findForReceiptById(invoiceId)
                .orElseThrow(() -> new BusinessException("Накладная не найдена"));

        return mapToResponse(invoice);
    }

    // ========================
    // 3. Сохранить факт (кладовщик)
    // ========================
    @Override
    @Transactional
    public ApiResponse saveReceiptFact(UUID invoiceId, GoodsReceiptRequest request) {
        Employee currentEmployee = getCurrentEmployeeOrThrow();

        SupplyInvoice invoice = supplyInvoiceRepository.findForReceiptById(invoiceId)
                .orElseThrow(() -> new BusinessException("Накладная не найдена"));

        // (опционально) обновляем шапку
        if (request.getActualDate() != null) {
            invoice.setActualDate(request.getActualDate());
        }
        if (request.getReceivedZoneId() != null) {
            StorageZone zone = storageZoneRepository.findById(request.getReceivedZoneId())
                    .orElseThrow(() -> new BusinessException("Зона не найдена"));
            invoice.setReceivedZone(zone);
        }

        // кладовщик (если в накладной не выставлен — поставим первого, кто ввёл факт)
        // если хочешь строго через SecurityContext — скажи, и я вставлю твой способ.
        // пока безопасно: если есть storekeeper_id уже — не трогаем.
        if (invoice.getStorekeeper() == null) {
            // можно оставить null, если у вас storekeeper ставится иначе
            // invoice.setStorekeeper(currentEmployee);
        }

        for (GoodsReceiptItemFactRequest itemReq : request.getItems()) {
            SupplyInvoiceItem item = supplyInvoiceItemRepository.findById(itemReq.getSupplyInvoiceItemId())
                    .orElseThrow(() -> new BusinessException("Строка накладной не найдена: " + itemReq.getSupplyInvoiceItemId()));

            item.setQuantityActual(itemReq.getQuantityActual());
            item.setManufactureDate(itemReq.getManufactureDate());
            item.setExpirationDate(itemReq.getExpirationDate());
            item.setFactEnteredAt(Instant.now());
            item.setFactEnteredBy(currentEmployee);

            boolean hasDiscrepancy = Boolean.TRUE.equals(itemReq.getHasDiscrepancy());

            if (hasDiscrepancy) {
                if (itemReq.getDiscrepancyType() == null || itemReq.getDiscrepancyType().isBlank()) {
                    throw new BusinessException("Не указан тип несоответствия (discrepancyType)");
                }
                if (itemReq.getDescription() == null || itemReq.getDescription().isBlank()) {
                    throw new BusinessException("Не заполнено описание несоответствия (description)");
                }

                DiscrepancyRequest dr = item.getDiscrepancyRequest();
                if (dr == null) {
                    dr = new DiscrepancyRequest();
                }

                // ВАЖНО: в вашей сущности эти поля должны существовать (по миграции они есть)
                dr.setSupplyInvoice(invoice);
                dr.setSupplyInvoiceItem(item);
                dr.setProduct(item.getProduct());

                dr.setDiscrepancyType(itemReq.getDiscrepancyType());
                dr.setDescription(itemReq.getDescription());

                dr.setQuantityExpected(item.getQuantityExpected());
                dr.setQuantityActual(item.getQuantityActual());
                dr.setCreatedByEmployee(currentEmployee);

                if (dr.getStatus() == null) {
                    dr.setStatus("pending");
                } else {
                    dr.setStatus("pending");
                }

                if (dr.getCreatedAt() == null) {
                    dr.setCreatedAt(Instant.now());
                }

                // created_by_employee_id NOT NULL — если у тебя там строго обязательно:
                // dr.setCreatedByEmployee(currentEmployee);
                // сейчас оставлено как есть (если уже создано — не трогаем)
                if (dr.getCreatedByEmployee() == null) {
                    // Если в БД NOT NULL — нужно подставлять реального сотрудника.
                    // Скажи — и я вставлю ваш способ получения текущего.
                    throw new BusinessException("createdByEmployee обязателен для discrepancy_requests (подключим текущего сотрудника)");
                }

                dr = discrepancyRequestRepository.save(dr);

                item.setDiscrepancyRequest(dr);
                item.setLineStatus("discrepancy_pending");
            } else {
                // если раньше было несоответствие pending — снимаем
                DiscrepancyRequest old = item.getDiscrepancyRequest();
                if (old != null && "pending".equalsIgnoreCase(old.getStatus())) {
                    item.setDiscrepancyRequest(null);
                    supplyInvoiceItemRepository.save(item);
                    discrepancyRequestRepository.delete(old);
                }

                item.setLineStatus("ok");
            }

            supplyInvoiceItemRepository.save(item);
        }

        invoice.setStatus("in_progress");
        supplyInvoiceRepository.save(invoice);

        return ApiResponse.ok("Факт приёмки сохранён");
    }


    // ========================
    // 4. Подтверждение приёмки
    // ========================
    @Override
    @Transactional
    public ApiResponse confirmReceipt(UUID invoiceId, GoodsReceiptConfirmRequest request) {
        Employee currentEmployee = getCurrentEmployeeOrThrow();
        SupplyInvoice invoice = supplyInvoiceRepository.findForReceiptById(invoiceId)
                .orElseThrow(() -> new BusinessException("Накладная не найдена"));

        // шапка: actualDate / receivedZone
        if (invoice.getActualDate() == null && request.getActualDate() == null) {
            throw new BusinessException("Не указана дата приёмки");
        }
        if (invoice.getReceivedZone() == null && request.getReceivedZoneId() == null) {
            throw new BusinessException("Не указана зона приёмки");
        }

        if (request.getActualDate() != null) {
            invoice.setActualDate(request.getActualDate());
        }
        if (request.getReceivedZoneId() != null) {
            StorageZone zone = storageZoneRepository.findById(request.getReceivedZoneId())
                    .orElseThrow(() -> new BusinessException("Зона не найдена"));
            invoice.setReceivedZone(zone);
        }

        // нельзя подтверждать, если есть не введённый факт или pending discrepancy
        boolean hasPendingLines = invoice.getItems().stream()
                .anyMatch(it -> "pending".equalsIgnoreCase(it.getLineStatus()));
        if (hasPendingLines) {
            throw new BusinessException("Есть строки со статусом pending (факт не введён). Подтверждение невозможно.");
        }

        boolean hasPendingDiscrepancy = invoice.getItems().stream()
                .anyMatch(it -> "discrepancy_pending".equalsIgnoreCase(it.getLineStatus()));
        if (hasPendingDiscrepancy) {
            throw new BusinessException("Есть строки с несоответствиями (discrepancy_pending). Подтверждение невозможно.");
        }

        UUID toZoneId = invoice.getReceivedZone().getId();

        int accepted = 0;
        int rejected = 0;

        for (SupplyInvoiceItem item : invoice.getItems()) {

            if ("rejected".equalsIgnoreCase(item.getLineStatus())) {
                rejected++;
                continue;
            }

            if (!"ok".equalsIgnoreCase(item.getLineStatus()) && !"accepted".equalsIgnoreCase(item.getLineStatus())) {
                throw new BusinessException("Некорректный статус строки для подтверждения: " + item.getLineStatus());
            }

            if (item.getQuantityActual() == null || item.getQuantityActual().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("quantityActual должен быть > 0 для принятия строки (lineNo=" + item.getLineNo() + ")");
            }

            // создаём batch (если ещё не создан)
            Batch batch = item.getBatch();
            if (batch == null) {
                batch = Batch.builder()
                        .product(item.getProduct())
                        .supplyInvoice(invoice)
                        .manufactureDate(item.getManufactureDate())
                        .expirationDate(item.getExpirationDate())
                        .purchasePrice(item.getPurchasePrice())
                        .initialQuantity(item.getQuantityActual())
                        .createdAt(Instant.now())
                        .build();
                batch = batchRepository.save(batch);
                item.setBatch(batch);
            }

            // batch_locations: добавляем количество в зону приемки
            Optional<BatchLocation> blOpt =
                    batchLocationRepository.findByBatchIdAndStorageZoneId(batch.getId(), toZoneId);

            BatchLocation bl;
            if (blOpt.isPresent()) {
                bl = blOpt.get();
            } else {
                bl = new BatchLocation();
                bl.setBatch(batch);
                bl.setStorageZone(invoice.getReceivedZone());
                bl.setQuantity(BigDecimal.ZERO);
            }

            bl.setQuantity(bl.getQuantity().add(item.getQuantityActual()));
            bl.setUpdatedAt(Instant.now());
            batchLocationRepository.save(bl);

            bl.setQuantity(bl.getQuantity().add(item.getQuantityActual()));
            bl.setUpdatedAt(Instant.now());
            batchLocationRepository.save(bl);

            // warehouse_operations: создаём операцию RECEIPT
            WarehouseOperation op = new WarehouseOperation();
            op.setType("RECEIPT");
            op.setProduct(item.getProduct());
            op.setFromZone(null);
            op.setToZone(invoice.getReceivedZone());
            op.setQuantity(item.getQuantityActual());
            op.setOperationDate(Instant.now());
            op.setEmployee(currentEmployee);
            op.setReason("Приёмка по накладной " + invoice.getInvoiceNumber());
            op.setBatch(batch);
            op.setSourceDocumentType("SUPPLY_INVOICE");
            op.setSourceDocumentId(invoice.getId());
            warehouseOperationRepository.save(op);

            item.setLineStatus("accepted");
            supplyInvoiceItemRepository.save(item);
            accepted++;
        }

        // статус накладной
        if (accepted > 0 && rejected == 0) {
            invoice.setStatus("received");
        } else if (accepted > 0 && rejected > 0) {
            invoice.setStatus("partially_received");
        } else if (accepted == 0 && rejected > 0) {
            invoice.setStatus("rejected");
        } else {
            // на всякий случай
            invoice.setStatus("received");
        }

        supplyInvoiceRepository.save(invoice);
        return ApiResponse.ok("Приёмка подтверждена");
    }

    // ========================
    // 5. Решение по несоответствию
    // ========================
    @Override
    @Transactional
    public ApiResponse decideDiscrepancy(UUID discrepancyId, DiscrepancyDecisionRequest request) {
        Employee currentEmployee = getCurrentEmployeeOrThrow();
        DiscrepancyRequest dr = discrepancyRequestRepository.findById(discrepancyId)
                .orElseThrow(() -> new BusinessException("Несоответствие не найдено"));

        SupplyInvoiceItem item = supplyInvoiceItemRepository.findByDiscrepancyRequestId(discrepancyId)
                .orElseThrow(() -> new BusinessException("Не найдена строка накладной для discrepancy: " + discrepancyId));

        String decision = request.getDecision() == null ? "" : request.getDecision().trim().toLowerCase();

        switch (decision) {
            case "approve" -> {
                dr.setStatus("approved");
                dr.setDecisionComment(request.getComment());
                dr.setDecisionAt(Instant.now());
                dr.setDecisionByEmployee(currentEmployee);

                item.setLineStatus("ok");
            }
            case "reject" -> {
                if (request.getComment() == null || request.getComment().isBlank()) {
                    throw new BusinessException("Комментарий обязателен при отклонении");
                }
                dr.setStatus("rejected");
                dr.setDecisionComment(request.getComment());
                dr.setDecisionAt(Instant.now());
                // dr.setDecisionByEmployee(currentEmployee);

                item.setLineStatus("rejected");
            }
            default -> throw new BusinessException("Некорректное решение: " + request.getDecision());
        }

        discrepancyRequestRepository.save(dr);
        supplyInvoiceItemRepository.save(item);

        return ApiResponse.ok("Решение по несоответствию сохранено");
    }

    // ========================
    // Маппинг в DTO
    // ========================
    private GoodsReceiptResponse mapToResponse(SupplyInvoice invoice) {
        return GoodsReceiptResponse.builder()
                .supplyInvoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .status(invoice.getStatus())
                .contractId(invoice.getContract().getId())
                .storageLocationId(invoice.getStorageLocation().getId())
                .expectedDate(invoice.getExpectedDate())
                .actualDate(invoice.getActualDate())
                .receivedZoneId(invoice.getReceivedZone() != null ? invoice.getReceivedZone().getId() : null)
                .storekeeperId(invoice.getStorekeeper() != null ? invoice.getStorekeeper().getId() : null)
                .merchandiserId(invoice.getMerchandiser() != null ? invoice.getMerchandiser().getId() : null)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .items(invoice.getItems().stream().map(this::mapItem).collect(Collectors.toList()))
                .build();
    }

    private GoodsReceiptItemResponse mapItem(SupplyInvoiceItem item) {
        return GoodsReceiptItemResponse.builder()
                .supplyInvoiceItemId(item.getId())
                .lineNo(item.getLineNo())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantityExpected(item.getQuantityExpected())
                .purchasePrice(item.getPurchasePrice())
                .quantityActual(item.getQuantityActual())
                .manufactureDate(item.getManufactureDate())
                .expirationDate(item.getExpirationDate())
                .lineStatus(item.getLineStatus())
                .batchId(item.getBatch() != null ? item.getBatch().getId() : null)
                .discrepancy(item.getDiscrepancyRequest() != null ? mapDiscrepancy(item.getDiscrepancyRequest()) : null)
                .build();
    }

    private DiscrepancyResponse mapDiscrepancy(DiscrepancyRequest dr) {
        return DiscrepancyResponse.builder()
                .id(dr.getId())
                .discrepancyType(dr.getDiscrepancyType())
                .description(dr.getDescription())
                .quantityExpected(dr.getQuantityExpected())
                .quantityActual(dr.getQuantityActual())
                .status(dr.getStatus())
                .decisionComment(dr.getDecisionComment())
                .decisionAt(dr.getDecisionAt())
                .hqDecisionComment(dr.getHqDecisionComment())
                .hqDecisionAt(dr.getHqDecisionAt())
                .build();
    }

    private Employee getCurrentEmployeeOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("Пользователь не авторизован");
        }

        Object principal = auth.getPrincipal();

        // 1) пробуем вытащить employeeId из custom principal (через reflection)
        try {
            var m = principal.getClass().getMethod("getEmployeeId");
            Object val = m.invoke(principal);
            if (val instanceof UUID employeeId) {
                return employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new BusinessException("Employee не найден для employeeId=" + employeeId));
            }
        } catch (NoSuchMethodException ignored) {
            // нет метода getEmployeeId — идём дальше
        } catch (Exception e) {
            throw new BusinessException("Не удалось получить employeeId из principal: " + e.getMessage());
        }

        // 2) fallback: пробуем взять login из custom principal (через reflection)
        String login = null;
        try {
            var m = principal.getClass().getMethod("getLogin");
            Object val = m.invoke(principal);
            if (val != null) login = val.toString();
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new BusinessException("Не удалось получить login из principal: " + e.getMessage());
        }

        // 3) если не получилось — пробуем auth.getName()
        if (login == null || login.isBlank()) {
            login = auth.getName();
        }

        // 4) если login всё ещё выглядит как "CurrentUserPrincipal[...]" — вытащим login=...
        if (login != null && login.contains("login=")) {
            int start = login.indexOf("login=") + "login=".length();
            int end = login.indexOf(',', start);
            if (end == -1) end = login.indexOf(']', start);
            if (end == -1) end = login.length();
            login = login.substring(start, end).trim();
        }

        UserAccount ua = userAccountRepository.findByLoginIgnoreCase(login)
                .orElseThrow(() -> new BusinessException("UserAccount не найден для login="));

        if (ua.getEmployee() == null) {
            throw new BusinessException("У аккаунта нет привязанного сотрудника (employee_id is null)");
        }

        return ua.getEmployee();
    }


}
