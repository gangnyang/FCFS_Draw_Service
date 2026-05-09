package com.fcfsdraw.wallet.dto;

import com.fcfsdraw.wallet.domain.Wallet;

public record WalletResponse(
        Long userId,
        long balance
) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getUserId(), wallet.getBalance());
    }
}
