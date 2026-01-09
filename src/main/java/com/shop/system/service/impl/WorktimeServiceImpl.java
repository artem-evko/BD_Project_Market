package com.shop.system.service.impl;

import com.shop.system.domain.entity.Employee;
import com.shop.system.domain.entity.EmployeeWorktime;
import com.shop.system.domain.entity.UserAccount;
import com.shop.system.dto.request.ConfirmWorktimeRequest;
import com.shop.system.dto.response.*;
import com.shop.system.exception.BusinessException;
import com.shop.system.repository.EmployeeWorktimeRepository;
import com.shop.system.repository.UserAccountRepository;
import com.shop.system.security.CurrentUserPrincipal;
import com.shop.system.service.WorktimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorktimeServiceImpl implements WorktimeService {

    private final EmployeeWorktimeRepository worktimeRepository;
    private final UserAccountRepository userAccountRepository;

    private static final Set<String> PRIVILEGED = Set.of("DIRECTOR", "ADMIN");

    @Override
    @Transactional(readOnly = true)
    public Page<WorktimeListItemResponse> list(UUID employeeId, LocalDate dateFrom, LocalDate dateTo, int page, int size) {
        UserAccount ua = currentAccount();

        boolean canSeeAll = hasAnyRole(ua, PRIVILEGED);

        UUID effectiveEmployeeId;
        if (canSeeAll) {
            effectiveEmployeeId = employeeId; // может быть null -> все
        } else {
            if (ua.getEmployee() == null) throw new BusinessException("У пользователя нет привязанного сотрудника");
            effectiveEmployeeId = ua.getEmployee().getId();
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "date")
        );

        return worktimeRepository.findForList(effectiveEmployeeId, dateFrom, dateTo, pageable)
                .map(this::toListItem);
    }

    @Override
    @Transactional
    public OpenWorktimeResponse open() {
        UserAccount ua = currentAccount();
        if (ua.getEmployee() == null) throw new BusinessException("У пользователя нет привязанного сотрудника");

        UUID employeeId = ua.getEmployee().getId();

        EmployeeWorktime wt = worktimeRepository.findOpenByEmployee(employeeId)
                .orElse(null);

        if (wt == null) {
            return OpenWorktimeResponse.builder().build(); // фронту проще: пусто значит нет open
        }

        // Логика "после 24:00": если сейчас уже новый день, а запись открыта за "вчера"
        // и confirmation_sent_at ещё нет — выставляем sent/deadline
        LocalDate today = LocalDate.now();
        boolean isYesterdayOpen = wt.getDate() != null && wt.getDate().isBefore(today);

        if (isYesterdayOpen && wt.getConfirmationSentAt() == null) {
            LocalDateTime now = LocalDateTime.now();
            wt.setConfirmationSentAt(now);
            wt.setConfirmationDeadlineAt(now.plusMinutes(5));
            worktimeRepository.save(wt);
        }

        boolean needsConfirmation = wt.getConfirmationSentAt() != null
                && wt.getConfirmationResponseAt() == null;

        return OpenWorktimeResponse.builder()
                .id(wt.getId())
                .date(wt.getDate())
                .scheduledStart(wt.getScheduledStart())
                .scheduledEnd(wt.getScheduledEnd())
                .actualLogin(wt.getActualLogin())
                .actualLogout(wt.getActualLogout())
                .confirmationSentAt(wt.getConfirmationSentAt())
                .confirmationDeadlineAt(wt.getConfirmationDeadlineAt())
                .needsConfirmation(needsConfirmation)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse confirm(UUID worktimeId, ConfirmWorktimeRequest request) {
        UserAccount ua = currentAccount();
        if (ua.getEmployee() == null) throw new BusinessException("У пользователя нет привязанного сотрудника");

        if (worktimeId == null) throw new BusinessException("worktimeId обязателен");
        if (request == null || request.getResult() == null || request.getResult().isBlank()) {
            throw new BusinessException("result обязателен");
        }

        String result = request.getResult().trim().toLowerCase();
        if (!result.equals("continue") && !result.equals("stop")) {
            throw new BusinessException("result должен быть: continue или stop");
        }

        EmployeeWorktime wt = worktimeRepository.findWithEmployee(worktimeId)
                .orElseThrow(() -> new BusinessException("Запись worktime не найдена"));

        // только владелец
        UUID myEmployeeId = ua.getEmployee().getId();
        if (wt.getEmployee() == null || wt.getEmployee().getId() == null || !wt.getEmployee().getId().equals(myEmployeeId)) {
            throw new BusinessException("Нет прав на подтверждение этой записи");
        }

        if (wt.getActualLogout() != null) {
            throw new BusinessException("Смена уже закрыта");
        }

        // подтверждение допускаем только если бэк его отправлял (или можно разрешить всегда — но лучше по ТЗ)
        if (wt.getConfirmationSentAt() == null) {
            throw new BusinessException("Подтверждение не требуется");
        }
        if (wt.getConfirmationResponseAt() != null) {
            throw new BusinessException("Подтверждение уже отправлено");
        }

        LocalDateTime now = LocalDateTime.now();
        wt.setConfirmationResult(result);
        wt.setConfirmationResponseAt(now);

        if (result.equals("stop")) {
            wt.setActualLogout(now);
            wt.setAutoClosed(false);
            wt.setAutoCloseReason(null);
        }

        worktimeRepository.save(wt);

        return new ApiResponse(true, "Подтверждение сохранено: " + result);
    }

    // ---------------- mapping ----------------

    private WorktimeListItemResponse toListItem(EmployeeWorktime wt) {
        Employee e = wt.getEmployee();
        return WorktimeListItemResponse.builder()
                .id(wt.getId())
                .employeeId(e != null ? e.getId() : null)
                .employeeFullName(e != null ? e.getFullName() : null)
                .date(wt.getDate())
                .scheduledStart(wt.getScheduledStart())
                .scheduledEnd(wt.getScheduledEnd())
                .actualLogin(wt.getActualLogin())
                .actualLogout(wt.getActualLogout())
                .status(wt.getStatus())
                .autoClosed(wt.getAutoClosed())
                .autoCloseReason(wt.getAutoCloseReason())
                .confirmationSentAt(wt.getConfirmationSentAt())
                .confirmationDeadlineAt(wt.getConfirmationDeadlineAt())
                .confirmationResult(wt.getConfirmationResult())
                .confirmationResponseAt(wt.getConfirmationResponseAt())
                .build();
    }

    // ---------------- auth helpers ----------------

    private boolean hasAnyRole(UserAccount ua, Set<String> codes) {
        return ua.getRole() != null && ua.getRole().getCode() != null && codes.contains(ua.getRole().getCode());
    }

    private UserAccount currentAccount() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) throw new BusinessException("Пользователь не авторизован");

        Object principal = a.getPrincipal();
        String login;

        if (principal instanceof CurrentUserPrincipal p) {
            login = p.login();
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            login = ud.getUsername();
        } else if (principal instanceof String s) {
            login = s;
        } else {
            login = a.getName();
        }

        if (login == null || login.isBlank() || "anonymousUser".equalsIgnoreCase(login)) {
            throw new BusinessException("Пользователь не авторизован");
        }

        return userAccountRepository.findByLoginAndIsActiveTrue(login)
                .orElseThrow(() -> new BusinessException("Пользователь не найден или неактивен"));
    }
}
