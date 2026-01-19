package com.shop.system.repository;

import com.shop.system.domain.entity.SupplyInvoice;
import com.shop.system.repository.projection.SupplyInvoiceListItemProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    @Query(
            value = """
    select si.*
    from supply_invoices si
    where (:search is null or cast(si.invoice_number as text) ilike concat('%', :search, '%'))
      and (:status is null or si.status = :status)
      and (:storageLocationId is null or si.storage_location_id = :storageLocationId)
      and (:expectedFrom is null or si.expected_date >= :expectedFrom)
      and (:expectedTo is null or si.expected_date <= :expectedTo)
    order by si.expected_date desc
    """,
            countQuery = """
    select count(*)
    from supply_invoices si
    where (:search is null or cast(si.invoice_number as text) ilike concat('%', :search, '%'))
      and (:status is null or si.status = :status)
      and (:storageLocationId is null or si.storage_location_id = :storageLocationId)
      and (:expectedFrom is null or si.expected_date >= :expectedFrom)
      and (:expectedTo is null or si.expected_date <= :expectedTo)
    """,
            nativeQuery = true
    )
    Page<SupplyInvoice> findSupplyInvoicesNative(
            @Param("search") String search,
            @Param("status") String status,
            @Param("storageLocationId") UUID storageLocationId,
            @Param("expectedFrom") LocalDate expectedFrom,
            @Param("expectedTo") LocalDate expectedTo,
            Pageable pageable
    );

    @Query(
            value = """
        select
            si.id as id,
            si.invoice_number as invoiceNumber,
            si.status as status,
            si.expected_date as expectedDate,
            si.actual_date as actualDate,
            si.storage_location_id as storageLocationId,
            si.contract_id as contractId,
            c.contract_number as contractNumber
        from supply_invoices si
        left join contracts c on c.id = si.contract_id
        where (:search is null or si.invoice_number ilike ('%' || :search || '%'))
          and (:status is null or si.status = :status)
          and (:storageLocationId is null or si.storage_location_id = :storageLocationId)
          and (:expectedFrom is null or si.expected_date >= :expectedFrom)
          and (:expectedTo is null or si.expected_date <= :expectedTo)
        order by si.expected_date desc
        """,
            countQuery = """
        select count(*)
        from supply_invoices si
        where (:search is null or si.invoice_number ilike ('%' || :search || '%'))
          and (:status is null or si.status = :status)
          and (:storageLocationId is null or si.storage_location_id = :storageLocationId)
          and (:expectedFrom is null or si.expected_date >= :expectedFrom)
          and (:expectedTo is null or si.expected_date <= :expectedTo)
        """,
            nativeQuery = true
    )
    Page<SupplyInvoiceListItemProjection> findSupplyInvoicesList(
            @Param("search") String search,
            @Param("status") String status,
            @Param("storageLocationId") UUID storageLocationId,
            @Param("expectedFrom") LocalDate expectedFrom,
            @Param("expectedTo") LocalDate expectedTo,
            Pageable pageable
    );
}
