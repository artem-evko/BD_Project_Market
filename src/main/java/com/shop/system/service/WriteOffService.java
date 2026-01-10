package com.shop.system.service;

import com.shop.system.dto.request.WriteOffCreateRequest;
import com.shop.system.dto.response.WriteOffResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface WriteOffService {

    Page<WriteOffResponse> getWriteOffs(String status, int page, int size);

    WriteOffResponse createWriteOff(WriteOffCreateRequest request);

    WriteOffResponse submitWriteOff(UUID id);

    WriteOffResponse approveWriteOff(UUID id);

    WriteOffResponse rejectWriteOff(UUID id, String rejectComment);

    WriteOffResponse cancelWriteOff(UUID id);
}
