package com.shop.system.service.impl;

import com.shop.system.domain.entity.*;
import com.shop.system.dto.request.*;
import com.shop.system.dto.response.*;
import com.shop.system.exception.BusinessException;
import com.shop.system.repository.*;
import com.shop.system.security.CurrentUserPrincipal;
import com.shop.system.service.WorkReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkReportServiceImpl implements WorkReportService {

    private final WorkReportHeaderRepository headerRepository;
    private final WorkReportLineRepository lineRepository;
    private final EmployeeWorktimeRepository worktimeRepository;

    private final UserAccountRepository userAccountRepository; // чтобы получить employee + storageLocation + role

    @Override
    @Transactional(readOnly = true)
    public Page<WorkReportHeaderListItemResponse> list(LocalDate weekStart, String status, int page, int size) {

        UserAccount ua = currentAccount();
        UUID storageLocationId = currentStorageLocationId(ua);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "weekStart").and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return headerRepository.findForList(storageLocationId, weekStart, blankToNull(status), pageable)
                .map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkReportDetailsResponse get(UUID id) {

        UserAccount ua = currentAccount();
        UUID storageLocationId = currentStorageLocationId(ua);

        WorkReportHeader h = headerRepository.findWithDetails(id)
                .orElseThrow(() -> new BusinessException("Отчёт не найден"));

        if (!h.getStorageLocation().getId().equals(storageLocationId)) {
            throw new BusinessException("Нет доступа к отчёту другой торговой точки");
        }

        List<WorkReportLine> lines = lineRepository.findByReportIdWithEmployee(id);

        return toDetails(h, lines);
    }

    @Override
    @Transactional
    public ApiResponse generate(GenerateWorkReportRequest request) {

        UserAccount ua = currentAccount();
        requireDirector(ua);

        LocalDate weekStart = requireWeekStart(request);

        UUID storageLocationId = currentStorageLocationId(ua);

        // если отчёт уже есть — перегенерим строки (удобно на разработке)
        WorkReportHeader header = headerRepository
                .findByStorageLocation_IdAndWeekStart(storageLocationId, weekStart)
                .orElseGet(() -> {
                    WorkReportHeader h = WorkReportHeader.builder()
                            .weekStart(weekStart)
                            .storageLocation(ua.getEmployee().getStorageLocation())
                            .director(ua.getEmployee())
                            .status("draft")
                            .createdAt(LocalDateTime.now())
                            .build();
                    return headerRepository.save(h);
                });

        // draft можно перегенерировать, confirmed/sent — лучше запретить
        if (!"draft".equals(header.getStatus())) {
            throw new BusinessException("Генерация доступна только для отчёта в статусе draft");
        }

        // удаляем старые строки и строим новые
        lineRepository.deleteByReport_Id(header.getId());

        LocalDate dateFrom = weekStart;
        LocalDate dateTo = weekStart.plusDays(6);

        List<EmployeeWorktime> weekWorktimes = worktimeRepository.findForReport(storageLocationId, dateFrom, dateTo);

        // группируем по сотруднику
        Map<UUID, List<EmployeeWorktime>> byEmployee = weekWorktimes.stream()
                .collect(Collectors.groupingBy(w -> w.getEmployee().getId()));

        List<WorkReportLine> lines = new ArrayList<>();

        for (Map.Entry<UUID, List<EmployeeWorktime>> entry : byEmployee.entrySet()) {
            Employee employee = entry.getValue().get(0).getEmployee();
            List<EmployeeWorktime> times = entry.getValue();

            BigDecimal computed = sumComputedHours(times);
            BigDecimal norm = sumNormHours(times);
            BigDecimal delta = computed.subtract(norm);

            WorkReportLine line = WorkReportLine.builder()
                    .report(header)
                    .employee(employee)
                    .computedHours(computed)
                    .normHours(norm)
                    .deltaHours(delta)
                    .directorOverrideDelta(null)
                    .overrideReason(null)
                    .build();

            lines.add(line);
        }

        lineRepository.saveAll(lines);

        return new ApiResponse(true, "Отчёт сформирован: " + weekStart);
    }

    @Override
    @Transactional
    public ApiResponse confirm(UUID id, ConfirmWorkReportRequest request) {

        UserAccount ua = currentAccount();
        requireDirector(ua);

        WorkReportHeader header = headerRepository.findWithDetails(id)
                .orElseThrow(() -> new BusinessException("Отчёт не найден"));

        UUID storageLocationId = currentStorageLocationId(ua);
        if (!header.getStorageLocation().getId().equals(storageLocationId)) {
            throw new BusinessException("Нет доступа к отчёту другой торговой точки");
        }

        if (!"draft".equals(header.getStatus())) {
            throw new BusinessException("Подтвердить можно только отчёт в статусе draft");
        }

        if (request != null && request.getOverrides() != null) {
            for (WorkReportLineOverrideRequest ov : request.getOverrides()) {
                WorkReportLine line = lineRepository.findByIdAndReportId(ov.getLineId(), id)
                        .orElseThrow(() -> new BusinessException("Строка отчёта не найдена: " + ov.getLineId()));

                if (ov.getDirectorOverrideDelta() != null) {
                    String reason = ov.getOverrideReason();
                    if (reason == null || reason.isBlank()) {
                        throw new BusinessException("overrideReason обязателен, если задан directorOverrideDelta");
                    }
                    line.setDirectorOverrideDelta(scale2(ov.getDirectorOverrideDelta()));
                    line.setOverrideReason(reason.trim());
                } else {
                    // снять правку
                    line.setDirectorOverrideDelta(null);
                    line.setOverrideReason(null);
                }
                lineRepository.save(line);
            }
        }

        header.setStatus("confirmed");
        header.setConfirmedAt(LocalDateTime.now());
        header.setDirector(ua.getEmployee());

        headerRepository.save(header);

        return new ApiResponse(true, "Отчёт подтверждён");
    }

    @Override
    @Transactional
    public ApiResponse send(UUID id) {

        UserAccount ua = currentAccount();
        requireDirector(ua);

        WorkReportHeader header = headerRepository.findWithDetails(id)
                .orElseThrow(() -> new BusinessException("Отчёт не найден"));

        UUID storageLocationId = currentStorageLocationId(ua);
        if (!header.getStorageLocation().getId().equals(storageLocationId)) {
            throw new BusinessException("Нет доступа к отчёту другой торговой точки");
        }

        if (!"confirmed".equals(header.getStatus())) {
            throw new BusinessException("Отправить в ГК можно только отчёт в статусе confirmed");
        }

        header.setStatus("sent");
        header.setSentToHqAt(LocalDateTime.now());
        headerRepository.save(header);

        // тут потом подключишь реальную отправку в ГК
        return new ApiResponse(true, "Отчёт отправлен в ГК");
    }

    // -------------------- calculations --------------------

    /**
     * computed_hours правило:
     * - если confirmation_result = 'no_response' => effective_logout = scheduled_end (переработка после scheduled_end не учитывается)
     * - иначе effective_logout = actual_logout
     */
    private BigDecimal sumComputedHours(List<EmployeeWorktime> times) {
        long totalMinutes = 0;

        for (EmployeeWorktime w : times) {
            LocalDateTime start = w.getActualLogin() != null ? w.getActualLogin() : w.getScheduledStart();

            LocalDateTime effectiveLogout;
            if ("no_response".equals(w.getConfirmationResult())) {
                effectiveLogout = w.getScheduledEnd();
            } else {
                effectiveLogout = w.getActualLogout();
            }

            if (start == null || effectiveLogout == null) continue;

            long minutes = ChronoUnit.MINUTES.between(start, effectiveLogout);
            if (minutes < 0) minutes = 0;
            totalMinutes += minutes;
        }

        return scale2(BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
    }

    /**
     * norm_hours — по плану: scheduled_end - scheduled_start (за каждый день)
     */
    private BigDecimal sumNormHours(List<EmployeeWorktime> times) {
        long totalMinutes = 0;

        for (EmployeeWorktime w : times) {
            if (w.getScheduledStart() == null || w.getScheduledEnd() == null) continue;
            long minutes = ChronoUnit.MINUTES.between(w.getScheduledStart(), w.getScheduledEnd());
            if (minutes < 0) minutes = 0;
            totalMinutes += minutes;
        }

        return scale2(BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal scale2(BigDecimal v) {
        if (v == null) return null;
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    // -------------------- mapping --------------------

    private WorkReportHeaderListItemResponse toListItem(WorkReportHeader h) {
        return WorkReportHeaderListItemResponse.builder()
                .id(h.getId())
                .weekStart(h.getWeekStart())
                .status(h.getStatus())
                .confirmedAt(h.getConfirmedAt())
                .sentToHqAt(h.getSentToHqAt())
                .createdAt(h.getCreatedAt())
                .build();
    }

    private WorkReportDetailsResponse toDetails(WorkReportHeader h, List<WorkReportLine> lines) {
        return WorkReportDetailsResponse.builder()
                .id(h.getId())
                .weekStart(h.getWeekStart())
                .status(h.getStatus())
                .confirmedAt(h.getConfirmedAt())
                .sentToHqAt(h.getSentToHqAt())
                .createdAt(h.getCreatedAt())
                .lines(lines.stream().map(this::toLine).toList())
                .build();
    }

    private WorkReportLineResponse toLine(WorkReportLine l) {
        Employee e = l.getEmployee();
        return WorkReportLineResponse.builder()
                .id(l.getId())
                .employeeId(e != null ? e.getId() : null)
                .employeeFullName(e != null ? e.getFullName() : null)
                .computedHours(l.getComputedHours())
                .normHours(l.getNormHours())
                .deltaHours(l.getDeltaHours())
                .directorOverrideDelta(l.getDirectorOverrideDelta())
                .overrideReason(l.getOverrideReason())
                .build();
    }

    // -------------------- auth/helpers --------------------

    private LocalDate requireWeekStart(GenerateWorkReportRequest request) {
        if (request == null || request.getWeekStart() == null) {
            throw new BusinessException("weekStart обязателен");
        }
        // не заставляю проверять "понедельник", но можно:
        // if (request.getWeekStart().getDayOfWeek() != DayOfWeek.MONDAY) throw ...
        return request.getWeekStart();
    }

    private void requireDirector(UserAccount ua) {
        String code = ua.getRole() != null ? ua.getRole().getCode() : null;
        if (!"DIRECTOR".equals(code) && !"ADMIN".equals(code)) {
            throw new BusinessException("Нет прав: требуется роль DIRECTOR");
        }
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private UUID currentStorageLocationId(UserAccount ua) {
        if (ua.getEmployee() == null || ua.getEmployee().getStorageLocation() == null) {
            throw new BusinessException("У пользователя нет привязанной торговой точки (storage_location)");
        }
        return ua.getEmployee().getStorageLocation().getId();
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

        // важно: тут employee/storageLocation должны быть доступны (обычно в UserAccount entity employee -> storageLocation)
        return userAccountRepository.findByLoginAndIsActiveTrue(login)
                .orElseThrow(() -> new BusinessException("Пользователь не найден или неактивен"));
    }
}
