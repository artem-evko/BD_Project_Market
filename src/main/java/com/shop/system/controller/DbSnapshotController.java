package com.shop.system.controller;

import com.shop.system.dto.response.ApiResponse;
import com.shop.system.service.DbSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/export")
public class DbSnapshotController {

    private final DbSnapshotService dbSnapshotService;

    /**
     * GET /api/export/snapshot?snapshotDate=YYYY-MM-DD
     * Скачивание JSON слепка базы на заданную дату + запись в exchange_log
     */
    @GetMapping("/snapshot")
    @PreAuthorize("hasRole('ADMIN')") // при необходимости поменяй на свою модель ролей
    public ResponseEntity<byte[]> exportSnapshot(
            @RequestParam("snapshotDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate snapshotDate
    ) {
        DbSnapshotService.ExportedSnapshot exported = dbSnapshotService.exportSnapshot(snapshotDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(exported.fileName(), StandardCharsets.UTF_8)
                        .build()
        );
        headers.setCacheControl("no-store, no-cache, must-revalidate, max-age=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(exported.bytes());
    }
}
