package com.ocbc.bankdemo.dto;

import com.ocbc.bankdemo.entity.Account;
import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String accountNumber,
        String ownerName,
        BigDecimal balance,
        Instant createdAt,
        long version
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getBalance(),
                account.getCreatedAt(),
                account.getVersion()
        );
    }
}
