package com.fcfsdraw.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WalletBalanceRequest(
        @NotNull(message = "잔액은 필수입니다.")
        @Min(value = 0, message = "잔액은 0 이상이어야 합니다.")
        Long balance
) {
}
