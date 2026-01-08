package com.shop.system.service.impl;

import com.shop.system.domain.entity.Employee;
import com.shop.system.domain.entity.UserAccount;
import com.shop.system.domain.entity.Vacation;
import com.shop.system.dto.request.CreateVacationRequest;
import com.shop.system.dto.request.RejectRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.VacationListItemResponse;
import com.shop.system.exception.BusinessException;
import com.shop.system.repository.SickLeaveRepository;
import com.shop.system.repository.UserAccountRepository;
import com.shop.system.repository.VacationRepository;
import com.shop.system.service.VacationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VacationServiceImpl implements VacationService {

    private final VacationRepository vacationRepository;
    private final SickLeaveRepository sickLeaveRepository; // чтобы проверять пересечения с больничными
    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<VacationListItemResponse> list(UUID employeeId,
                                               String status,
                                               LocalDate dateFrom,
                                               LocalDate dateTo,
                                               int page,
                                               int size) {

        UserAccount ua = currentAccount();
        boolean canViewAny = isDirectorOrAdmin(ua);

        UUID effectiveEmployeeId;
        if (canViewAny) {
            // admin/director могут смотреть по выбранному сотруднику или всех (если employeeId null)
            effectiveEmployeeId = employeeId;
        } else {
            // обычный сотрудник смотрит только себя
            if (ua.getEmployee() == null) {
                throw new BusinessException("У пользователя нет привязанного сотрудника");
            }
            effectiveEmployeeId = ua.getEmployee().getId();
        }


        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return vacationRepository
                .findForList(
                        effectiveEmployeeId,
                        blankToNull(status),
                        dateFrom,
                        dateTo,
                        pageable
                )
                .map(this::toListItem);
    }

    @Override
    @Transactional
    public ApiResponse create(CreateVacationRequest request) {

        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("AUTH name=" + a.getName()
                + " principal=" + a.getPrincipal()
                + " principalClass=" + (a.getPrincipal() == null ? null : a.getPrincipal().getClass()));


        UserAccount ua = currentAccount();

        if (ua.getEmployee() == null) {
            throw new BusinessException("У пользователя нет привязанного сотрудника");
        }

        LocalDate start = request.getDateStart();
        LocalDate end = request.getDateEnd();

        validateDates(start, end);

        UUID employeeId = ua.getEmployee().getId();

        // 1) пересечение с отпуском запрещено
        long vacOverlaps = vacationRepository.countOverlaps(employeeId, start, end, null);
        if (vacOverlaps > 0) {
            throw new BusinessException("Пересечение с существующим отпуском запрещено");
        }

        // 2) пересечение с больничным запрещено
        long sickOverlaps = sickLeaveRepository.countOverlaps(employeeId, start, end, null);
        if (sickOverlaps > 0) {
            throw new BusinessException("Пересечение с больничным запрещено");
        }

        Vacation v = Vacation.builder()
                .employee(ua.getEmployee())
                .createdAt(LocalDateTime.now())
                .dateStart(start)
                .dateEnd(end)
                .status("pending")
                .build();

        vacationRepository.save(v);

        return new ApiResponse(true, "Заявление на отпуск отправлено");
    }

    @Override
    @Transactional
    public ApiResponse approve(UUID id) {

        UserAccount ua = currentAccount();
        if (!isDirectorOrAdmin(ua)) {
            throw new BusinessException("Нет прав: требуется роль director или admin");
        }

        Vacation v = vacationRepository.findWithDetails(id)
                .orElseThrow(() -> new BusinessException("Отпуск не найден"));

        if (!"pending".equals(v.getStatus())) {
            throw new BusinessException("Можно подписывать только заявки в статусе pending");
        }

        v.setStatus("approved");
        v.setApprovedAt(LocalDateTime.now());
        v.setApprovedByEmployee(ua.getEmployee());
        v.setRejectComment(null);

        vacationRepository.save(v);

        return new ApiResponse(true, "Отпуск подписан");
    }

    @Override
    @Transactional
    public ApiResponse reject(UUID id, RejectRequest request) {

        UserAccount ua = currentAccount();
        if (!isDirectorOrAdmin(ua)) {
            throw new BusinessException("Нет прав: требуется роль director или admin");
        }

        if (request == null || request.getRejectComment() == null || request.getRejectComment().isBlank()) {
            throw new BusinessException("rejectComment обязателен");
        }

        Vacation v = vacationRepository.findWithDetails(id)
                .orElseThrow(() -> new BusinessException("Отпуск не найден"));

        if (!"pending".equals(v.getStatus())) {
            throw new BusinessException("Можно отклонять только заявки в статусе pending");
        }

        v.setStatus("rejected");
        v.setRejectComment(request.getRejectComment().trim());
        v.setApprovedAt(LocalDateTime.now());
        v.setApprovedByEmployee(ua.getEmployee());

        vacationRepository.save(v);

        return new ApiResponse(true, "Отпуск отклонён");
    }

    // -------------------- mapping --------------------

    private VacationListItemResponse toListItem(Vacation v) {
        Employee e = v.getEmployee();

        return VacationListItemResponse.builder()
                .id(v.getId())
                .employeeId(e != null ? e.getId() : null)
                .employeeFullName(e != null ? e.getFullName() : null)
                .dateStart(v.getDateStart())
                .dateEnd(v.getDateEnd())
                .status(v.getStatus())
                .rejectComment(v.getRejectComment())
                .createdAt(v.getCreatedAt())
                .approvedAt(v.getApprovedAt())
                .build();
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

    private UserAccount currentAccount() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) {
            throw new BusinessException("Пользователь не авторизован");
        }

        Object principal = a.getPrincipal();

        String login;
        if (principal instanceof com.shop.system.security.CurrentUserPrincipal p) {
            login = p.login();
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            login = ud.getUsername();
        } else if (principal instanceof String s) {
            login = s;
        } else {
            login = a.getName(); // fallback
        }

        if (login == null || login.isBlank() || "anonymousUser".equalsIgnoreCase(login)) {
            throw new BusinessException("Пользователь не авторизован");
        }

        return userAccountRepository.findByLoginAndIsActiveTrue(login)
                .orElseThrow(() -> new BusinessException("Пользователь не найден или неактивен"));
    }

    private boolean isDirectorOrAdmin(UserAccount ua) {
        if (ua.getRole() == null || ua.getRole().getCode() == null) return false;
        String code = ua.getRole().getCode();
        return "DIRECTOR".equals(code) || "ADMIN".equals(code);
    }
}
