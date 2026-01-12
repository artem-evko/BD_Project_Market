package com.shop.system.repository;

import com.shop.system.domain.entity.ExchangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ExchangeLogRepository extends JpaRepository<ExchangeLog, UUID> {

    @Query(
            value = """
            select *
            from exchange_log el
            where (cast(:entity as text) is null or el.entity_name ilike concat('%', cast(:entity as text), '%'))
              and (cast(:direction as text) is null or el.direction ilike cast(:direction as text))
              and (cast(:status as text) is null or el.status ilike cast(:status as text))
              and (cast(:fromTs as timestamptz) is null or el."timestamp" >= cast(:fromTs as timestamptz))
              and (cast(:toTs as timestamptz) is null or el."timestamp" <= cast(:toTs as timestamptz))
            order by el."timestamp" desc
            """,
            countQuery = """
            select count(*)
            from exchange_log el
            where (cast(:entity as text) is null or el.entity_name ilike concat('%', cast(:entity as text), '%'))
              and (cast(:direction as text) is null or el.direction ilike cast(:direction as text))
              and (cast(:status as text) is null or el.status ilike cast(:status as text))
              and (cast(:fromTs as timestamptz) is null or el."timestamp" >= cast(:fromTs as timestamptz))
              and (cast(:toTs as timestamptz) is null or el."timestamp" <= cast(:toTs as timestamptz))
            """,
            nativeQuery = true
    )
    Page<ExchangeLog> search(
            @Param("entity") String entity,
            @Param("direction") String direction,
            @Param("status") String status,
            @Param("fromTs") Instant fromTs,
            @Param("toTs") Instant toTs,
            Pageable pageable
    );
}