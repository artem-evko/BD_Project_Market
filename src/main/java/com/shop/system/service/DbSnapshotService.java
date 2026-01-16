package com.shop.system.service;

import java.time.LocalDate;
import java.util.Map;

public interface DbSnapshotService {

    ExportedSnapshot exportSnapshot(LocalDate snapshotDate);

    record ExportedSnapshot(
            String fileName,
            byte[] bytes,
            Map<String, Object> meta
    ) {}
}
