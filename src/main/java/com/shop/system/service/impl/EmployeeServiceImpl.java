package com.shop.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.system.domain.entity.*;
import com.shop.system.dto.PassportDataDto;
import com.shop.system.dto.request.CreateEmployeeRequest;
import com.shop.system.dto.request.UpdateEmployeeRequest;
import com.shop.system.dto.response.*;
import com.shop.system.exception.BusinessException;
import com.shop.system.repository.*;
import com.shop.system.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeListItemResponse> getEmployees(String search,
                                                       UUID departmentId,
                                                       UUID positionId,
                                                       String status,
                                                       int page,
                                                       int size) {

        log.info("search class = {}, value = {}",
                search == null ? "null" : search.getClass().getName(),
                search);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.ASC, "fullName")
        );

        String s = blankToNull(search);
        String searchPattern = (s == null) ? null : "%" + s + "%";

        return employeeRepository
                .findEmployees(searchPattern, departmentId, positionId, blankToNull(status), pageable)
                .map(this::toListItem);

    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDetailsResponse getEmployee(UUID id) {

        Employee e = employeeRepository.findWithDetailsById(id)
                .orElseThrow(() -> new BusinessException("Сотрудник не найден"));

        return toDetails(e);
    }

    @Override
    @Transactional
    public ApiResponse createEmployee(CreateEmployeeRequest request) {

        if (userAccountRepository.existsByLogin(request.getLogin())) {
            throw new BusinessException("Логин уже занят");
        }

        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new BusinessException("Должность не найдена"));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new BusinessException("Отдел не найден"));

        Role role = roleRepository.findByCode(request.getRoleCode())
                .orElseThrow(() -> new BusinessException("Роль не найдена"));

        String passportJson = serializeAndValidatePassport(request.getPassportData());

        Employee employee = Employee.builder()
                .fullName(request.getFullName())
                .passportData(passportJson)
                .position(position)
                .department(department)
                .employmentDate(request.getEmploymentDate())
                .workPhone(request.getWorkPhone())
                .personalPhone(request.getPersonalPhone())
                .email(request.getEmail())
                .employmentStatus("active")
                .build();

        employee = employeeRepository.save(employee);

        UserAccount account = UserAccount.builder()
                .employee(employee)
                .login(request.getLogin())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isActive(true)
                .build();

        userAccountRepository.save(account);

        return new ApiResponse(true, "Сотрудник успешно создан");
    }

    @Override
    @Transactional
    public ApiResponse updateEmployee(UUID id, UpdateEmployeeRequest request) {

        Employee employee = employeeRepository.findWithDetailsById(id)
                .orElseThrow(() -> new BusinessException("Сотрудник не найден"));

        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new BusinessException("Должность не найдена"));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new BusinessException("Отдел не найден"));

        Role role = roleRepository.findByCode(request.getRoleCode())
                .orElseThrow(() -> new BusinessException("Роль не найдена"));

        String passportJson = serializeAndValidatePassport(request.getPassportData());

        employee.setFullName(request.getFullName());
        employee.setPassportData(passportJson);
        employee.setPosition(position);
        employee.setDepartment(department);
        employee.setEmploymentDate(request.getEmploymentDate());
        employee.setWorkPhone(request.getWorkPhone());
        employee.setPersonalPhone(request.getPersonalPhone());
        employee.setEmail(request.getEmail());

        // логин не меняем, пароль не меняем; роль можно обновлять
        if (employee.getUserAccount() != null) {
            employee.getUserAccount().setRole(role);
        }

        employeeRepository.save(employee);

        return new ApiResponse(true, "Данные сотрудника обновлены");
    }

    @Override
    @Transactional
    public ApiResponse terminateEmployee(UUID id) {

        Employee employee = employeeRepository.findWithDetailsById(id)
                .orElseThrow(() -> new BusinessException("Сотрудник не найден"));

        employee.setEmploymentStatus("terminated");
        employee.setTerminationDate(LocalDate.now());

        if (employee.getUserAccount() != null) {
            employee.getUserAccount().setIsActive(false);
        }

        employeeRepository.save(employee);

        return new ApiResponse(true, "Сотрудник уволен");
    }

    @Override
    @Transactional
    public ApiResponse deleteEmployee(UUID id) {

        Employee employee = employeeRepository.findWithDetailsById(id)
                .orElseThrow(() -> new BusinessException("Сотрудник не найден"));

        if (employee.getUserAccount() != null) {
            userAccountRepository.delete(employee.getUserAccount());
        }

        employeeRepository.delete(employee);

        return new ApiResponse(true, "Сотрудник удалён");
    }

    // -------------------- mapping --------------------

    private EmployeeListItemResponse toListItem(Employee e) {
        return EmployeeListItemResponse.builder()
                .id(e.getId())
                .fullName(e.getFullName())
                .position(e.getPosition() != null ? e.getPosition().getName() : null)
                .department(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .role(e.getUserAccount() != null && e.getUserAccount().getRole() != null
                        ? e.getUserAccount().getRole().getName()
                        : null)
                .status(e.getEmploymentStatus())
                .build();
    }

    private EmployeeDetailsResponse toDetails(Employee e) {
        return EmployeeDetailsResponse.builder()
                .id(e.getId())
                .fullName(e.getFullName())
                .status(e.getEmploymentStatus())
                .position(e.getPosition() != null ? e.getPosition().getName() : null)
                .department(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .role(e.getUserAccount() != null && e.getUserAccount().getRole() != null
                        ? e.getUserAccount().getRole().getName()
                        : null)
                .workPhone(e.getWorkPhone())
                .personalPhone(e.getPersonalPhone())
                .email(e.getEmail())
                .employmentDate(e.getEmploymentDate())
                .terminationDate(e.getTerminationDate())
                .passportData(deserializePassport(e.getPassportData()))
                .build();
    }

    // -------------------- passport helpers --------------------

    private PassportDataDto deserializePassport(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, PassportDataDto.class);
        } catch (Exception e) {
            // если в БД уже лежит не то — лучше явно увидеть проблему
            throw new BusinessException("Некорректные паспортные данные в базе");
        }
    }

    private String serializeAndValidatePassport(PassportDataDto dto) {
        validatePassport(dto);
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Не удалось сохранить паспортные данные");
        }
    }

    private void validatePassport(PassportDataDto p) {
        if (p == null) throw new BusinessException("passportData обязателен");

        String type = p.getType() != null ? p.getType().trim() : null;
        if (type == null || type.isBlank()) {
            throw new BusinessException("passportData.type обязателен");
        }

        if (!type.equals("internal_rf") && !type.equals("international_rf")) {
            throw new BusinessException("passportData.type должен быть internal_rf или international_rf");
        }

        LocalDate issueDate = p.getIssueDate();
        if (issueDate == null) {
            throw new BusinessException("passportData.issueDate обязателен");
        }
        if (issueDate.isAfter(LocalDate.now())) {
            throw new BusinessException("passportData.issueDate не может быть будущей датой");
        }

        if (type.equals("internal_rf")) {
            if (isBlank(p.getSeries()) || !p.getSeries().matches("^\\d{4}$")) {
                throw new BusinessException("passportData.series: должно быть 4 цифры");
            }
            if (isBlank(p.getNumber()) || !p.getNumber().matches("^\\d{6}$")) {
                throw new BusinessException("passportData.number: должно быть 6 цифр");
            }
            if (isBlank(p.getRegistrationAddress())) {
                throw new BusinessException("passportData.registrationAddress обязателен");
            }

            // поля загранника можно игнорировать

        } else { // international_rf
            if (isBlank(p.getPassportNumber()) || !p.getPassportNumber().matches("^[A-Za-z0-9]{8,10}$")) {
                throw new BusinessException("passportData.passportNumber: 8-10 букв/цифр");
            }
            if (p.getValidUntil() == null) {
                throw new BusinessException("passportData.validUntil обязателен");
            }
            if (!p.getValidUntil().isAfter(issueDate)) {
                throw new BusinessException("passportData.validUntil должен быть позже issueDate");
            }

            // поля внутреннего можно игнорировать
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
