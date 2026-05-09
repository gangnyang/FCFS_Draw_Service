package com.fcfsdraw.wallet.service;

import com.fcfsdraw.common.exception.BusinessException;
import com.fcfsdraw.common.exception.ErrorCode;
import com.fcfsdraw.wallet.domain.Wallet;
import com.fcfsdraw.wallet.dto.WalletResponse;
import com.fcfsdraw.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public WalletResponse getBalance(Long userId) {
        Wallet wallet = findByUserId(userId);
        return WalletResponse.from(wallet);
    }

    @Transactional
    public WalletResponse setBalance(Long userId, long balance) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> new Wallet(userId, balance));
        wallet.changeBalance(balance);

        return WalletResponse.from(walletRepository.save(wallet));
    }

    @Transactional
    public Wallet deduct(Long userId, long price) {
        Wallet wallet = findByUserId(userId);
        wallet.deduct(price);
        return wallet;
    }

    private Wallet findByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
    }
}
