package com.shop.system.service.impl;

import com.shop.system.domain.entity.Employee;
import com.shop.system.domain.entity.SickLeave;
import com.shop.system.dto.request.CreateSickLeaveRequest;
import com.shop.system.dto.request.RejectRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.SickLeaveListItemResponse;
import com.shop.system.exception.BusinessException;
import com.shop.system.repository.EmployeeRepository;
import com.shop.system.repository.SickLeaveRepository;
import com.shop.system.repository.VacationRepository;
import com.shop.system.security.CurrentUserPrincipal;
import com.shop.system.service.SickLeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SickLeaveServiceImpl implements SickLeaveService {

    private final SickLeaveRepository sickLeaveRepository;
    private final VacationRepository vacationRepository; // чтобы проверять пересечения с отпусками
    private final EmployeeRepository employeeRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional(readOnly = true)
    public Page<SickLeaveListItemResponse> list(UUID employeeId,
                                                String status,
                                                String docStatus,
                                                LocalDate dateFrom,
                                                LocalDate dateTo,
                                                int page,
                                                int size) {

        CurrentUserPrincipal u = currentUser();
        boolean canSeeAll = isDirectorOrAdmin(u);

        UUID effectiveEmployeeId;
        if (canSeeAll) {
            // director/admin: можно employeeId или всех (null)
            effectiveEmployeeId = employeeId;
        } else {
            // сотрудник: всегда только себя
            if (u.employeeId() == null) {
                throw new BusinessException("У пользователя нет employeeId");
            }
            effectiveEmployeeId = u.employeeId();
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return sickLeaveRepository.findForList(
                        effectiveEmployeeId,
                        blankToNull(status),
                        normalizeDocStatus(blankToNull(docStatus)),
                        dateFrom,
                        dateTo,
                        pageable
                )
                .map(this::toListItem);
    }

    @Override
    @Transactional
    public ApiResponse create(CreateSickLeaveRequest request) {

        CurrentUserPrincipal u = currentUser();
        if (u.employeeId() == null) {
            throw new BusinessException("У пользователя нет employeeId");
        }

        LocalDate start = request.getDateStart();
        LocalDate end = request.getDateEnd();
        validateDates(start, end);

        UUID employeeId = u.employeeId();

        // 1) пересечение с больничным запрещено
        long sickOverlaps = sickLeaveRepository.countOverlaps(employeeId, start, end, null);
        if (sickOverlaps > 0) {
            throw new BusinessException("Пересечение с другим больничным запрещено");
        }

        // 2) пересечение с отпуском запрещено
        long vacOverlaps = vacationRepository.countOverlaps(employeeId, start, end, null);
        if (vacOverlaps > 0) {
            throw new BusinessException("Пересечение с отпуском запрещено");
        }

        Employee employeeRef = employeeRepository.getReferenceById(employeeId);

        SickLeave sl = SickLeave.builder()
                .employee(employeeRef)
                .createdAt(LocalDateTime.now())
                .dateStart(start)
                .dateEnd(end)
                .status("pending")
                .docRequired(true)
                .docDueDate(end.plusDays(7))
                .docStatus("pending")
                .docReceivedAt(null)
                .build();

        sickLeaveRepository.save(sl);

        return new ApiResponse(true, "Заявление на больничный отправлено");
    }

    @Override
    @Transactional
    public ApiResponse approve(UUID id) {

        CurrentUserPrincipal u = currentUser();
        if (!isDirectorOrAdmin(u)) {
            throw new BusinessException("Нет прав: требуется роль DIRECTOR или ADMIN");
        }
        if (u.employeeId() == null) {
            throw new BusinessException("У пользователя нет employeeId");
        }

        SickLeave sl = sickLeaveRepository.findWithDetails(id)
                .orElseThrow(() -> new BusinessException("Больничный не найден"));

        if (!"pending".equals(sl.getStatus())) {
            throw new BusinessException("Можно подписывать только заявки в статусе pending");
        }

        sl.setStatus("approved");
        sl.setApprovedAt(LocalDateTime.now());
        sl.setApprovedByEmployee(employeeRepository.getReferenceById(u.employeeId()));
        sl.setRejectComment(null);

        sickLeaveRepository.save(sl);

        return new ApiResponse(true, "Больничный подписан");
    }

    @Override
    @Transactional
    public ApiResponse reject(UUID id, RejectRequest request) {

        CurrentUserPrincipal u = currentUser();
        if (!isDirectorOrAdmin(u)) {
            throw new BusinessException("Нет прав: требуется роль DIRECTOR или ADMIN");
        }
        if (u.employeeId() == null) {
            throw new BusinessException("У пользователя нет employeeId");
        }

        if (request == null || request.getRejectComment() == null || request.getRejectComment().isBlank()) {
            throw new BusinessException("rejectComment обязателен");
        }

        SickLeave sl = sickLeaveRepository.findWithDetails(id)
                .orElseThrow(() -> new BusinessException("Больничный не найден"));

        if (!"pending".equals(sl.getStatus())) {
            throw new BusinessException("Можно отклонять только заявки в статусе pending");
        }

        sl.setStatus("rejected");
        sl.setRejectComment(request.getRejectComment().trim());
        sl.setApprovedAt(LocalDateTime.now());
        sl.setApprovedByEmployee(employeeRepository.getReferenceById(u.employeeId()));

        sickLeaveRepository.save(sl);

        return new ApiResponse(true, "Больничный отклонён");
    }

    @Override
    @Transactional
    public ApiResponse uploadDocument(UUID id, MultipartFile file) {

        CurrentUserPrincipal u = currentUser();
        if (u.employeeId() == null) {
            throw new BusinessException("У пользователя нет employeeId");
        }

        SickLeave sl = sickLeaveRepository.findWithDetails(id)
                .orElseThrow(() -> new BusinessException("Больничный не найден"));

        // по ТЗ: прикреплять может только владелец записи (сотрудник)
        boolean isOwner = sl.getEmployee() != null && u.employeeId().equals(sl.getEmployee().getId());
        if (!isOwner) {
            throw new BusinessException("Нет прав: можно прикреплять документ только к своему больничному");
        }

        if ("rejected".equals(sl.getStatus())) {
            throw new BusinessException("Нельзя прикреплять документ к отклонённой заявке");
        }

        if (sl.getDocRequired() == null || !sl.getDocRequired()) {
            throw new BusinessException("Документ не требуется");
        }

        if (sl.getDocReceivedAt() != null) {
            throw new BusinessException("Документ уже был загружен");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Файл обязателен");
        }

        validateFile(file);

        // сохраняем локально (демо-реализация, без БД-колонок под путь)
        saveToDisk(sl.getId(), file);

        sl.setDocStatus("received");
        sl.setDocReceivedAt(LocalDateTime.now());

        sickLeaveRepository.save(sl);

        return new ApiResponse(true, "Документ загружен");
    }

    // -------------------- mapping --------------------

    private SickLeaveListItemResponse toListItem(SickLeave sl) {
        String approvedBy = sl.getApprovedByEmployee() != null ? sl.getApprovedByEmployee().getFullName() : null;

        return SickLeaveListItemResponse.builder()
                .id(sl.getId())
                .employeeId(sl.getEmployee() != null ? sl.getEmployee().getId() : null)
                .employeeFullName(sl.getEmployee() != null ? sl.getEmployee().getFullName() : null)
                .dateStart(sl.getDateStart())
                .dateEnd(sl.getDateEnd())
                .status(sl.getStatus())
                .rejectComment(sl.getRejectComment())
                .createdAt(sl.getCreatedAt())
                .approvedAt(sl.getApprovedAt())
                .approvedByFullName(approvedBy)
                .docRequired(sl.getDocRequired())
                .docDueDate(sl.getDocDueDate())
                .docReceivedAt(sl.getDocReceivedAt())
                .docUiStatus(calcDocUiStatus(sl))
                .build();
    }

    private String calcDocUiStatus(SickLeave sl) {
        if (sl.getDocReceivedAt() != null) return "received";
        if (sl.getDocDueDate() != null && sl.getDocDueDate().isBefore(LocalDate.now())) return "overdue";
        return "pending";
    }

    // -------------------- helpers --------------------

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null) throw new BusinessException("dateStart обязателен");
        if (end == null) throw new BusinessException("dateEnd обязателен");
        if (end.isBefore(start)) throw new BusinessException("dateEnd должен быть >= dateStart");
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private String normalizeDocStatus(String v) {
        if (v == null) return null;
        String x = v.trim().toLowerCase(Locale.ROOT);
        return switch (x) {
            case "pending", "received", "overdue" -> x;
            default -> throw new BusinessException("docStatus должен быть: pending | received | overdue");
        };
    }

    private CurrentUserPrincipal currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) {
            throw new BusinessException("Пользователь не авторизован");
        }
        Object principal = a.getPrincipal();
        if (principal instanceof CurrentUserPrincipal p) return p;
        throw new BusinessException("Некорректный principal в SecurityContext");
    }

    private boolean isDirectorOrAdmin(CurrentUserPrincipal u) {
        return u.role() != null && ("DIRECTOR".equals(u.role()) || "ADMIN".equals(u.role()));
    }

    private void validateFile(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null) ct = "";

        boolean okType =
                ct.equalsIgnoreCase("application/pdf")
                        || ct.equalsIgnoreCase("image/jpeg")
                        || ct.equalsIgnoreCase("image/png");

        if (!okType) {
            throw new BusinessException("Разрешены файлы: PDF/JPG/PNG");
        }

        long maxBytes = 10L * 1024 * 1024; // 10MB
        if (file.getSize() > maxBytes) {
            throw new BusinessException("Файл слишком большой (макс 10MB)");
        }
    }

    private void saveToDisk(UUID sickLeaveId, MultipartFile file) {
        try {
            Path dir = Paths.get(uploadDir, "sick-leaves", sickLeaveId.toString());
            Files.createDirectories(dir);

            String original = file.getOriginalFilename();
            if (original == null || original.isBlank()) original = "document";

            // простая “санитизация”
            original = original.replaceAll("[\\\\/\\n\\r\\t]", "_");

            String targetName = UUID.randomUUID() + "_" + original;
            Path target = dir.resolve(targetName);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new BusinessException("Не удалось сохранить файл");
        }
    }
}
