package com.fcfsdraw.wallet.controller;

import com.fcfsdraw.common.response.ApiResponse;
import com.fcfsdraw.wallet.dto.WalletBalanceRequest;
import com.fcfsdraw.wallet.dto.WalletResponse;
import com.fcfsdraw.wallet.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{userId}")
    public ApiResponse<WalletResponse> getBalance(
            @PathVariable @Positive(message = "사용자 ID는 0보다 커야 합니다.") Long userId
    ) {
        return ApiResponse.success(walletService.getBalance(userId));
    }

    @PutMapping("/{userId}")
    public ApiResponse<WalletResponse> setBalance(
            @PathVariable @Positive(message = "사용자 ID는 0보다 커야 합니다.") Long userId,
            @Valid @RequestBody WalletBalanceRequest request
    ) {
        return ApiResponse.success(walletService.setBalance(userId, request.balance()));
    }
}
