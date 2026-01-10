package com.shop.system.controller;

import com.shop.system.dto.request.WriteOffCreateRequest;
import com.shop.system.dto.request.WriteOffRejectRequest;
import com.shop.system.dto.response.WriteOffResponse;
import com.shop.system.service.WriteOffService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/write-offs")
@RequiredArgsConstructor
public class WriteOffController {

    private final WriteOffService writeOffService;

    @GetMapping
    public Page<WriteOffResponse> getWriteOffs(
            @RequestParam(name = "status", defaultValue = "all") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return writeOffService.getWriteOffs(status, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WriteOffResponse createWriteOff(@RequestBody WriteOffCreateRequest request) {
        return writeOffService.createWriteOff(request);
    }

    @PostMapping("/{id}/submit")
    public WriteOffResponse submitWriteOff(@PathVariable("id") UUID id) {
        return writeOffService.submitWriteOff(id);
    }

    @PostMapping("/{id}/approve")
    public WriteOffResponse approveWriteOff(@PathVariable("id") UUID id) {
        return writeOffService.approveWriteOff(id);
    }

    @PostMapping("/{id}/reject")
    public WriteOffResponse rejectWriteOff(
            @PathVariable("id") UUID id,
            @RequestBody WriteOffRejectRequest request
    ) {
        return writeOffService.rejectWriteOff(id, request.getRejectComment());
    }

    @PostMapping("/{id}/cancel")
    public WriteOffResponse cancelWriteOff(@PathVariable("id") UUID id) {
        return writeOffService.cancelWriteOff(id);
    }
}
