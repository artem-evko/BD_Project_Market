package com.shop.system.controller;

import com.shop.system.dto.ExchangeFileDto;
import com.shop.system.service.DataExchangeService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange")
public class DataExchangeController {

    private final DataExchangeService dataExchangeService;

    // --- TAB: Download справочников ---

    @GetMapping("/download/reference-pack")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Resource> downloadReferencePack() {
        ExchangeFileDto file = dataExchangeService.buildReferencePack();
        return asAttachment(file.filename(), file.bytes());
    }

    @PostMapping(value = "/import/reference-pack", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> importReferencePack(
            @RequestPart("file") @NotNull MultipartFile file,
            @RequestParam(name = "overwrite", defaultValue = "false") boolean overwrite
    ) {
        return ResponseEntity.ok(dataExchangeService.importReferencePack(file, overwrite));
    }

    // --- TAB: Upload / Export в ГК ---

    @GetMapping("/export/sales")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Resource> exportSales(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam("dateTo") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        ExchangeFileDto file = dataExchangeService.exportSales(dateFrom, dateTo);
        return asAttachment(file.filename(), file.bytes());
    }

    @GetMapping("/export/stock")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Resource> exportStock(
            @RequestParam("snapshotDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate
    ) {
        ExchangeFileDto file = dataExchangeService.exportStock(snapshotDate);
        return asAttachment(file.filename(), file.bytes());
    }

    @GetMapping("/export/stop-list")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Resource> exportStopList(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        ExchangeFileDto file = dataExchangeService.exportStopList(date);
        return asAttachment(file.filename(), file.bytes());
    }

    // --- TAB: Слепок базы (в UI есть) ---
    @GetMapping("/export/snapshot")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> exportSnapshot(
            @RequestParam("snapshotDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate
    ) {
        ExchangeFileDto file = dataExchangeService.exportSnapshot(snapshotDate);
        return asAttachment(file.filename(), file.bytes());
    }

    private ResponseEntity<Resource> asAttachment(String filename, byte[] bytes) {
        var resource = new ByteArrayResource(bytes);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentLength(bytes.length)
                .body(resource);
    }
}