package com.shop.system.service.impl;

import com.shop.system.domain.entity.*;
import com.shop.system.dto.request.GenerateScheduleFromTemplateRequest;
import com.shop.system.dto.request.UpdateScheduleRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.ScheduleItemResponse;
import com.shop.system.exception.BusinessException;
import com.shop.system.repository.*;
import com.shop.system.security.CurrentUserPrincipal;
import com.shop.system.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final EmployeeScheduleRepository scheduleRepository;
    private final WorkScheduleTemplateRepository templateRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository; // нужен список сотрудников ТТ

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleItemResponse> list(LocalDate dateFrom, LocalDate dateTo, UUID employeeId) {

        if (dateFrom == null || dateTo == null) {
            throw new BusinessException("dateFrom и dateTo обязательны");
        }
        if (dateTo.isBefore(dateFrom)) {
            throw new BusinessException("dateTo не может быть раньше dateFrom");
        }

        UserAccount ua = currentAccount();
        UUID storageLocationId = currentStorageLocationId(ua);

        boolean directorOrAdmin = isDirectorOrAdmin(ua);

        // не директор/админ -> всегда “моё”
        if (!directorOrAdmin) {
            UUID myEmployeeId = ua.getEmployee().getId();
            return scheduleRepository.findByEmployee_IdAndDateBetweenOrderByDateAsc(myEmployeeId, dateFrom, dateTo)
                    .stream().map(this::toResponse).toList();
        }

        // директор/админ
        if (employeeId != null) {
            // важно: не даём смотреть чужую ТТ
            Employee emp = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new BusinessException("Сотрудник не найден"));

            if (emp.getStorageLocation() == null || !storageLocationId.equals(emp.getStorageLocation().getId())) {
                throw new BusinessException("Нет доступа к сотруднику другой торговой точки");
            }

            return scheduleRepository.findByEmployee_IdAndDateBetweenOrderByDateAsc(employeeId, dateFrom, dateTo)
                    .stream().map(this::toResponse).toList();
        }

        // “все сотрудники”
        return scheduleRepository
                .findByEmployee_StorageLocation_IdAndDateBetweenOrderByEmployee_FullNameAscDateAsc(storageLocationId, dateFrom, dateTo)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ApiResponse generateFromTemplate(GenerateScheduleFromTemplateRequest request) {

        if (request == null || request.getDateFrom() == null || request.getDateTo() == null) {
            throw new BusinessException("dateFrom/dateTo обязательны");
        }
        if (request.getDateTo().isBefore(request.getDateFrom())) {
            throw new BusinessException("dateTo не может быть раньше dateFrom");
        }

        UserAccount ua = currentAccount();
        requireDirectorOrAdmin(ua);

        UUID storageLocationId = currentStorageLocationId(ua);
        boolean overwrite = Boolean.TRUE.equals(request.getOverwrite());

        LocalDate from = request.getDateFrom();
        LocalDate to = request.getDateTo();

        // 1) сотрудники торговой точки
        List<Employee> employees = employeeRepository.findByStorageLocation_IdAndEmploymentStatus(storageLocationId, "active");
        if (employees.isEmpty()) {
            return new ApiResponse(true, "Нет активных сотрудников для генерации");
        }

        // 2) все шаблоны по ТТ (одним запросом)
        List<WorkScheduleTemplate> templates = templateRepository.findByEmployee_StorageLocation_Id(storageLocationId);

        // сгруппируем: employeeId -> weekday(1..7) -> template
        Map<UUID, Map<Integer, WorkScheduleTemplate>> templateMap = templates.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getEmployee().getId(),
                        Collectors.toMap(
                                WorkScheduleTemplate::getWeekday,
                                Function.identity(),
                                (a, b) -> a // если случайно дубль — берём первый
                        )
                ));

        // 3) существующие записи schedule в диапазоне (одним запросом)
        List<EmployeeSchedule> existing = scheduleRepository.findByEmployee_StorageLocation_IdAndDateBetween(storageLocationId, from, to);
        Map<String, EmployeeSchedule> existingMap = existing.stream()
                .collect(Collectors.toMap(
                        s -> key(s.getEmployee().getId(), s.getDate()),
                        Function.identity(),
                        (a, b) -> a
                ));

        int created = 0;
        int updated = 0;
        int skippedManual = 0;

        for (Employee e : employees) {
            UUID eid = e.getId();
            Map<Integer, WorkScheduleTemplate> byWeekday = templateMap.getOrDefault(eid, Collections.emptyMap());

            LocalDate d = from;
            while (!d.isAfter(to)) {
                int wd = d.getDayOfWeek().getValue(); // 1..7

                WorkScheduleTemplate tpl = byWeekday.get(wd);

                String k = key(eid, d);
                EmployeeSchedule sch = existingMap.get(k);

                // если запись есть и её уже вручную правили — не трогаем
                if (sch != null && sch.getCorrectionReason() != null && !sch.getCorrectionReason().isBlank()) {
                    skippedManual++;
                    d = d.plusDays(1);
                    continue;
                }

                // если запись есть и overwrite=false — пропускаем
                if (sch != null && !overwrite) {
                    d = d.plusDays(1);
                    continue;
                }

                if (sch == null) {
                    sch = EmployeeSchedule.builder()
                            .employee(e)
                            .date(d)
                            .build();
                    created++;
                } else {
                    updated++;
                }

                sch.setTemplate(tpl);

                if (tpl == null || Boolean.TRUE.equals(tpl.getIsDayOff())) {
                    sch.setScheduleType("day_off");
                    sch.setPlannedStart(null);
                    sch.setPlannedEnd(null);
                } else {
                    sch.setScheduleType("regular");
                    sch.setPlannedStart(tpl.getStartTime());
                    sch.setPlannedEnd(tpl.getEndTime());
                }

                // это генерация — не ручная правка
                sch.setCorrectionReason(null);
                sch.setCorrectedBy(null);

                scheduleRepository.save(sch);

                d = d.plusDays(1);
            }
        }

        return new ApiResponse(true,
                "Генерация завершена. created=" + created + ", updated=" + updated + ", skippedManual=" + skippedManual);
    }

    @Override
    @Transactional
    public ScheduleItemResponse update(UUID id, UpdateScheduleRequest request) {

        if (id == null) throw new BusinessException("id обязателен");
        if (request == null) throw new BusinessException("body обязателен");

        UserAccount ua = currentAccount();
        requireDirectorOrAdmin(ua);

        EmployeeSchedule sch = scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Запись расписания не найдена"));

        UUID storageLocationId = currentStorageLocationId(ua);

        Employee targetEmployee = sch.getEmployee();
        if (targetEmployee == null || targetEmployee.getStorageLocation() == null ||
                !storageLocationId.equals(targetEmployee.getStorageLocation().getId())) {
            throw new BusinessException("Нет доступа к расписанию другой торговой точки");
        }

        String reason = request.getCorrectionReason();
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("correctionReason обязателен");
        }

        String type = (request.getScheduleType() == null || request.getScheduleType().isBlank())
                ? sch.getScheduleType()
                : request.getScheduleType().trim();

        LocalTime ps = request.getPlannedStart();
        LocalTime pe = request.getPlannedEnd();

        // правила
        if ("day_off".equalsIgnoreCase(type)) {
            ps = null;
            pe = null;
        } else {
            if (ps == null || pe == null) {
                throw new BusinessException("plannedStart и plannedEnd обязательны (кроме day_off)");
            }
            if (!pe.isAfter(ps)) {
                throw new BusinessException("plannedEnd должен быть позже plannedStart");
            }
        }

        sch.setScheduleType(type);
        sch.setPlannedStart(ps);
        sch.setPlannedEnd(pe);

        sch.setCorrectionReason(reason.trim());
        sch.setCorrectedBy(ua.getEmployee());

        EmployeeSchedule saved = scheduleRepository.save(sch);
        return toResponse(saved);
    }

    private String key(UUID employeeId, LocalDate date) {
        return employeeId + "|" + date;
    }

    private ScheduleItemResponse toResponse(EmployeeSchedule s) {
        Employee e = s.getEmployee();
        Employee cb = s.getCorrectedBy();
        WorkScheduleTemplate tpl = s.getTemplate();

        return ScheduleItemResponse.builder()
                .id(s.getId())
                .employeeId(e != null ? e.getId() : null)
                .employeeFullName(e != null ? e.getFullName() : null)
                .date(s.getDate())
                .plannedStart(s.getPlannedStart())
                .plannedEnd(s.getPlannedEnd())
                .scheduleType(s.getScheduleType())
                .correctionReason(s.getCorrectionReason())
                .correctedById(cb != null ? cb.getId() : null)
                .correctedByFullName(cb != null ? cb.getFullName() : null)
                .templateId(tpl != null ? tpl.getId() : null)
                .build();
    }

    // -------- auth/helpers --------

    private void requireDirectorOrAdmin(UserAccount ua) {
        String code = ua.getRole() != null ? ua.getRole().getCode() : null;
        if (!"DIRECTOR".equals(code) && !"ADMIN".equals(code)) {
            throw new BusinessException("Нет прав: требуется роль DIRECTOR или ADMIN");
        }
    }

    private boolean isDirectorOrAdmin(UserAccount ua) {
        String code = ua.getRole() != null ? ua.getRole().getCode() : null;
        return "DIRECTOR".equals(code) || "ADMIN".equals(code);
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

        return userAccountRepository.findByLoginAndIsActiveTrue(login)
                .orElseThrow(() -> new BusinessException("Пользователь не найден или неактивен"));
    }
}
