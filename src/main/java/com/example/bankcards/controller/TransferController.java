package com.example.bankcards.controller;

import com.example.bankcards.dto.response.TransferDTOResponse;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.service.TransferService;
import com.example.bankcards.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Secured("ADMIN")
public class TransferController {

    private final TransferService transferService;
    private final SecurityUtils securityUtils;
    private final TransferMapper transferMapper;

    // Получить перевод по ID
    @GetMapping("/{transferId}")
    public ResponseEntity<TransferDTOResponse> getTransfer(@PathVariable Long transferId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Transfer transfer = transferService.getTransferById(transferId, currentUserId);
        TransferDTOResponse transferDTO = transferMapper.transferToTransferDTOResponse(transfer);
        return ResponseEntity.ok(transferDTO);
    }

    // Все переводы
    @GetMapping("/admin/all")
    public ResponseEntity<Page<TransferDTOResponse>> getAllTransfers(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Transfer> transfers = transferService.getAllTransfers(pageable);
        Page<TransferDTOResponse> transferDTOs = transfers.map(transferMapper::transferToTransferDTOResponse);
        return ResponseEntity.ok(transferDTOs);
    }
}
