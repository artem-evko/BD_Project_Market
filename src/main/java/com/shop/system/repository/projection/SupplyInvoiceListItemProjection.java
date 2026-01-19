package com.shop.system.repository.projection;

import java.time.LocalDate;
import java.util.UUID;

public interface SupplyInvoiceListItemProjection {
    UUID getId();
    String getInvoiceNumber();
    String getStatus();
    LocalDate getExpectedDate();
    LocalDate getActualDate();
    UUID getStorageLocationId();
    UUID getContractId();
    String getContractNumber();
}
