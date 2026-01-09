package com.shop.system.repository;

import com.shop.system.domain.entity.WorkReportLine;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface WorkReportLineRepository extends JpaRepository<WorkReportLine, UUID> {

    @Query("""
      select l from WorkReportLine l
      join fetch l.employee e
      where l.report.id = :reportId
      order by e.fullName asc
    """)
    List<WorkReportLine> findByReportIdWithEmployee(@Param("reportId") UUID reportId);

    @Query("""
      select l from WorkReportLine l
      join l.report h
      where l.id = :lineId and h.id = :reportId
    """)
    Optional<WorkReportLine> findByIdAndReportId(@Param("lineId") UUID lineId, @Param("reportId") UUID reportId);

    void deleteByReport_Id(UUID reportId);
}
