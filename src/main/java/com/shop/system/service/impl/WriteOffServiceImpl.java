package com.shop.system.service.impl;

import com.shop.system.domain.entity.*;
import com.shop.system.dto.request.WriteOffCreateRequest;
import com.shop.system.dto.response.WriteOffResponse;
import com.shop.system.exception.EntityNotFoundException;
import com.shop.system.repository.*;
import com.shop.system.security.CurrentUserPrincipal;
import com.shop.system.service.context.StorageLocationContextService;
import com.shop.system.service.WriteOffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WriteOffServiceImpl implements WriteOffService {

    private final WriteOffRepository writeOffRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final StorageZoneRepository storageZoneRepository;
    private final BatchLocationRepository batchLocationRepository;
    private final WarehouseOperationRepository warehouseOperationRepository;
    private final StorageLocationContextService storageLocationContextService;

    private static final Set<String> ALLOWED_REASONS = Set.of(
            "EXPIRED",
            "THEFT",
            "DAMAGE",
            "FORCE_MAJEURE",
            "EMPLOYEE_NEGLIGENCE"
    );

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "DRAFT",
            "SUBMITTED",
            "APPROVED",
            "REJECTED",
            "CANCELLED"
    );

    @Override
    public Page<WriteOffResponse> getWriteOffs(String status, int page, int size) {
        StorageLocation location = storageLocationContextService.getCurrentStorageLocation();

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "dateWrittenOff")
        );

        String statusFilter = null;
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            String normalized = normalizeStatus(status);
            statusFilter = normalized;
        }

        Page<WriteOff> result = writeOffRepository.findByLocationAndStatus(
                location.getId(),
                statusFilter,
                pageable
        );

        return result.map(this::toResponse);
    }

    @Override
    @Transactional
    public WriteOffResponse createWriteOff(WriteOffCreateRequest request) {
        ensureAnyRole("STOREKEEPER", "MERCHANDISER");

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("quantity должен быть > 0");
        }

        StorageLocation location = storageLocationContextService.getCurrentStorageLocation();
        Employee currentEmployee = storageLocationContextService.getCurrentEmployee();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Товар не найден: " + request.getProductId()));

        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new EntityNotFoundException("Партия не найдена: " + request.getBatchId()));

        if (!batch.getProduct().getId().equals(product.getId())) {
            throw new IllegalArgumentException("Партия не принадлежит указанному товару");
        }

        StorageZone zone = storageZoneRepository.findById(request.getStorageZoneId())
                .orElseThrow(() -> new EntityNotFoundException("Зона хранения не найдена: " + request.getStorageZoneId()));

        if (!zone.getStorageLocation().getId().equals(location.getId())) {
            throw new IllegalArgumentException("Зона хранения принадлежит другой торговой точке");
        }

        String normalizedReason = normalizeReason(request.getReason());

        BigDecimal available = batchLocationRepository.getQuantityForBatchInZone(
                batch.getId(),
                zone.getId()
        );
        if (available == null) {
            available = BigDecimal.ZERO;
        }

        if (available.compareTo(request.getQuantity()) < 0) {
            throw new IllegalArgumentException(
                    "Недостаточно остатка в зоне '%s': доступно=%s, запрошено=%s"
                            .formatted(zone.getName(), available, request.getQuantity())
            );
        }

        String status = request.isSubmit() ? "SUBMITTED" : "DRAFT";

        WriteOff entity = WriteOff.builder()
                .product(product)
                .batch(batch)
                .storageZone(zone)
                .quantity(request.getQuantity())
                .reason(normalizedReason)
                .status(status)
                .employee(currentEmployee)
                .documentNumber(trimToNull(request.getDocumentNumber()))
                .comment(trimToNull(request.getComment()))
                .dateWrittenOff(LocalDate.now())
                .build();

        WriteOff saved = writeOffRepository.save(entity);

        log.info("WRITE_OFF CREATED: id={}, status={}, productId={}, batchId={}, zoneId={}",
                saved.getId(), saved.getStatus(), product.getId(), batch.getId(), zone.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public WriteOffResponse submitWriteOff(UUID id) {
        ensureAnyRole("STOREKEEPER", "MERCHANDISER");

        StorageLocation location = storageLocationContextService.getCurrentStorageLocation();

        WriteOff writeOff = writeOffRepository.findByIdAndLocation(id, location.getId())
                .orElseThrow(() -> new EntityNotFoundException("Акт списания не найден: " + id));

        if (!"DRAFT".equalsIgnoreCase(writeOff.getStatus())) {
            throw new IllegalArgumentException("Отправить на согласование можно только DRAFT акт");
        }

        writeOff.setStatus("SUBMITTED");
        WriteOff saved = writeOffRepository.save(writeOff);

        log.info("WRITE_OFF SUBMITTED: id={}", id);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public WriteOffResponse approveWriteOff(UUID id) {
        ensureAnyRole("DIRECTOR");

        StorageLocation location = storageLocationContextService.getCurrentStorageLocation();
        Employee director = storageLocationContextService.getCurrentEmployee();

        WriteOff writeOff = writeOffRepository.findByIdAndLocation(id, location.getId())
                .orElseThrow(() -> new EntityNotFoundException("Акт списания не найден: " + id));

        if (!"SUBMITTED".equalsIgnoreCase(writeOff.getStatus())) {
            throw new IllegalArgumentException("Утвердить можно только SUBMITTED акт");
        }

        if (writeOff.getStorageZone() == null) {
            throw new IllegalStateException("У акта списания не указана зона хранения");
        }

        BigDecimal available = batchLocationRepository.getQuantityForBatchInZone(
                writeOff.getBatch().getId(),
                writeOff.getStorageZone().getId()
        );
        if (available == null) {
            available = BigDecimal.ZERO;
        }

        if (available.compareTo(writeOff.getQuantity()) < 0) {
            throw new IllegalArgumentException(
                    "Недостаточно остатка для списания: зона='%s', доступно=%s, запрошено=%s"
                            .formatted(writeOff.getStorageZone().getName(), available, writeOff.getQuantity())
            );
        }

        // 1) списываем из batch_locations
        applyWriteOffToBatchLocations(writeOff);

        // 2) создаём warehouse_operation WRITE_OFF
        WarehouseOperation operation = WarehouseOperation.builder()
                .type("WRITE_OFF")
                .product(writeOff.getProduct())
                .fromZone(writeOff.getStorageZone())
                .toZone(null)
                .quantity(writeOff.getQuantity())
                .operationDate(Instant.now())
                .employee(director)
                .reason(buildOperationReason(writeOff))
                .batch(writeOff.getBatch())
                .sourceDocumentType("WRITE_OFF")
                .sourceDocumentId(writeOff.getId())
                .build();

        warehouseOperationRepository.save(operation);

        writeOff.setStatus("APPROVED");
        writeOff.setApprovedByDirector(director);
        writeOff.setApprovedAt(Instant.now());

        WriteOff saved = writeOffRepository.save(writeOff);

        log.info("WRITE_OFF APPROVED: id={}, operationId={}", saved.getId(), operation.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public WriteOffResponse rejectWriteOff(UUID id, String rejectComment) {
        ensureAnyRole("DIRECTOR");

        if (rejectComment == null || rejectComment.trim().isEmpty()) {
            throw new IllegalArgumentException("comment обязателен при отклонении акта");
        }

        StorageLocation location = storageLocationContextService.getCurrentStorageLocation();
        Employee director = storageLocationContextService.getCurrentEmployee();

        WriteOff writeOff = writeOffRepository.findByIdAndLocation(id, location.getId())
                .orElseThrow(() -> new EntityNotFoundException("Акт списания не найден: " + id));

        if (!"SUBMITTED".equalsIgnoreCase(writeOff.getStatus())) {
            throw new IllegalArgumentException("Отклонить можно только SUBMITTED акт");
        }

        writeOff.setStatus("REJECTED");
        writeOff.setApprovedByDirector(director);
        writeOff.setApprovedAt(Instant.now());
        writeOff.setComment(rejectComment.trim());

        WriteOff saved = writeOffRepository.save(writeOff);

        log.info("WRITE_OFF REJECTED: id={}", id);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public WriteOffResponse cancelWriteOff(UUID id) {
        ensureAnyRole("STOREKEEPER", "MERCHANDISER");

        StorageLocation location = storageLocationContextService.getCurrentStorageLocation();

        WriteOff writeOff = writeOffRepository.findByIdAndLocation(id, location.getId())
                .orElseThrow(() -> new EntityNotFoundException("Акт списания не найден: " + id));

        String status = writeOff.getStatus() == null ? "" : writeOff.getStatus().toUpperCase(Locale.ROOT);

        if (!status.equals("DRAFT") && !status.equals("SUBMITTED")) {
            throw new IllegalArgumentException("Отменять можно только DRAFT или SUBMITTED акт");
        }

        writeOff.setStatus("CANCELLED");

        WriteOff saved = writeOffRepository.save(writeOff);

        log.info("WRITE_OFF CANCELLED: id={}", id);

        return toResponse(saved);
    }


    private String normalizeReason(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("reason обязателен");
        }
        String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        if (!ALLOWED_REASONS.contains(normalized)) {
            throw new IllegalArgumentException("Некорректная причина списания: " + raw);
        }
        return normalized;
    }

    private String normalizeStatus(String raw) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Некорректный статус списания: " + raw);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyWriteOffToBatchLocations(WriteOff writeOff) {
        UUID batchId = writeOff.getBatch().getId();
        UUID zoneId = writeOff.getStorageZone().getId();
        BigDecimal remaining = writeOff.getQuantity();

        List<BatchLocation> locations = batchLocationRepository
                .findByBatchIdAndStorageZoneId(batchId, zoneId)
                .stream()
                .sorted(Comparator.comparing(bl ->
                        Optional.ofNullable(bl.getUpdatedAt()).orElse(Instant.EPOCH)))
                .toList();

        if (locations.isEmpty()) {
            throw new IllegalStateException("Нет остатков в batch_locations для списания");
        }

        for (BatchLocation bl : locations) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal qty = bl.getQuantity() == null ? BigDecimal.ZERO : bl.getQuantity();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal toTake = qty.min(remaining);
            bl.setQuantity(qty.subtract(toTake));
            remaining = remaining.subtract(toTake);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("После перерасчёта остатков осталось несписанное количество: " + remaining);
        }

        batchLocationRepository.saveAll(locations);
    }

    private String buildOperationReason(WriteOff writeOff) {
        String base = writeOff.getReason();
        if (base == null) base = "";
        String comment = writeOff.getComment();
        if (comment == null || comment.isBlank()) {
            return base;
        }
        if (base.isBlank()) {
            return comment.trim();
        }
        return base + " | " + comment.trim();
    }

    private void ensureAnyRole(String... allowedRoles) {
        String roleCode = getCurrentRoleCode();
        for (String allowed : allowedRoles) {
            if (allowed.equalsIgnoreCase(roleCode)) {
                return;
            }
        }
        throw new AccessDeniedException("Недостаточно прав для операции");
    }

    private String getCurrentRoleCode() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Пользователь не аутентифицирован");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CurrentUserPrincipal cup) {
            return cup.role();
        }

        throw new AccessDeniedException("Некорректный principal в контексте безопасности");
    }

    private WriteOffResponse toResponse(WriteOff entity) {
        return WriteOffResponse.builder()
                .id(entity.getId())
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .productName(entity.getProduct() != null ? entity.getProduct().getName() : null)
                .batchId(entity.getBatch() != null ? entity.getBatch().getId() : null)
                .storageZoneId(entity.getStorageZone() != null ? entity.getStorageZone().getId() : null)
                .storageZoneName(entity.getStorageZone() != null ? entity.getStorageZone().getName() : null)
                .quantity(entity.getQuantity())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .dateWrittenOff(entity.getDateWrittenOff())
                .comment(entity.getComment())
                .documentNumber(entity.getDocumentNumber())
                .createdByEmployeeFullName(
                        entity.getEmployee() != null ? entity.getEmployee().getFullName() : null
                )
                .approvedByDirectorFullName(
                        entity.getApprovedByDirector() != null ? entity.getApprovedByDirector().getFullName() : null
                )
                .approvedAt(entity.getApprovedAt())
                .build();
    }
}
