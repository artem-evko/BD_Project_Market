package com.shop.system.repository;

import com.shop.system.domain.entity.SupplyInvoice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SupplyInvoiceRepository extends JpaRepository<SupplyInvoice, UUID> {

    Optional<SupplyInvoice> findByInvoiceNumberIgnoreCase(String invoiceNumber);

    /**
     * Для экрана "Приёмка": сразу подтягиваем строки, продукты и несоответствия,
     * чтобы не словить N+1 при сериализации/маппинге.
     */
    @Query("""
           select si
           from SupplyInvoice si
           left join fetch si.items i
           left join fetch i.product p
           left join fetch i.discrepancyRequest dr
           where si.id = :id
           """)
    Optional<SupplyInvoice> findForReceiptById(@Param("id") UUID id);

    /**
     * Иногда удобно искать сразу накладную "для приёмки" по номеру.
     */
    @Query("""
           select si
           from SupplyInvoice si
           left join fetch si.items i
           left join fetch i.product p
           left join fetch i.discrepancyRequest dr
           where lower(si.invoiceNumber) = lower(:invoiceNumber)
           """)
    Optional<SupplyInvoice> findForReceiptByNumber(@Param("invoiceNumber") String invoiceNumber);
}
