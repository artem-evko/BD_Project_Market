package com.shop.system.service.context;

import com.shop.system.domain.entity.Employee;
import com.shop.system.domain.entity.StorageLocation;
import com.shop.system.repository.EmployeeRepository;
import com.shop.system.security.CurrentUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

@Service
@RequestScope // чтобы кэш был на один HTTP-запрос
@RequiredArgsConstructor
@Slf4j
public class StorageLocationContextService {

    private final EmployeeRepository employeeRepository;

    private StorageLocation cachedLocation;

    public StorageLocation getCurrentStorageLocation() {
        if (cachedLocation != null) {
            return cachedLocation;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Пользователь не аутентифицирован");
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof CurrentUserPrincipal cup)) {
            throw new IllegalStateException("Ожидался CurrentUserPrincipal, а пришло: " + principal);
        }

        UUID employeeId = cup.employeeId();
        if (employeeId == null) {
            throw new IllegalStateException("В токене отсутствует employeeId");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalStateException(
                        "Сотрудник с id=" + employeeId + " не найден"
                ));

        StorageLocation storageLocation = employee.getStorageLocation();
        if (storageLocation == null) {
            throw new IllegalStateException(
                    "Для сотрудника '%s' не указана торговая точка / склад"
                            .formatted(employee.getFullName())
            );
        }

        log.info("STORAGE LOCATION: id={}, name={}", storageLocation.getId(), storageLocation.getName());
        this.cachedLocation = storageLocation;
        return storageLocation;
    }
}
