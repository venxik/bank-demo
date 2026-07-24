package com.ocbc.bankdemo.service;

import com.ocbc.bankdemo.dto.AccountResponse;
import com.ocbc.bankdemo.dto.CreateAccountRequest;
import com.ocbc.bankdemo.dto.TransferRequest;
import com.ocbc.bankdemo.exception.InsufficientFundsException;
import com.ocbc.bankdemo.exception.SelfTransferException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class AccountServiceTransferTest {

    @org.springframework.beans.factory.annotation.Autowired
    private AccountService accountService;

    @Test
    void transferMovesBalanceBetweenAccounts() {
        AccountResponse from = accountService.createAccount(new CreateAccountRequest("T-FROM-1", "From", new BigDecimal("100.00")));
        AccountResponse to = accountService.createAccount(new CreateAccountRequest("T-TO-1", "To", new BigDecimal("50.00")));

        var result = accountService.transfer(from.id(), new TransferRequest(to.id(), new BigDecimal("30.00")), null);

        assertEquals(new BigDecimal("70.00"), result.from().balance());
        assertEquals(new BigDecimal("80.00"), result.to().balance());
    }

    @Test
    void transferToSelfIsRejected() {
        AccountResponse account = accountService.createAccount(new CreateAccountRequest("T-SELF-1", "Self", new BigDecimal("100.00")));

        assertThatThrownBy(() -> accountService.transfer(account.id(), new TransferRequest(account.id(), BigDecimal.TEN), null))
                .isInstanceOf(SelfTransferException.class);
    }

    @Test
    void transferBeyondBalanceIsRejected() {
        AccountResponse from = accountService.createAccount(new CreateAccountRequest("T-FROM-2", "From", new BigDecimal("10.00")));
        AccountResponse to = accountService.createAccount(new CreateAccountRequest("T-TO-2", "To", new BigDecimal("0.00")));

        assertThatThrownBy(() -> accountService.transfer(from.id(), new TransferRequest(to.id(), new BigDecimal("50.00")), null))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void replayingIdempotencyKeyDoesNotDoubleDebit() {
        AccountResponse from = accountService.createAccount(new CreateAccountRequest("T-FROM-3", "From", new BigDecimal("100.00")));
        AccountResponse to = accountService.createAccount(new CreateAccountRequest("T-TO-3", "To", new BigDecimal("0.00")));
        TransferRequest request = new TransferRequest(to.id(), new BigDecimal("40.00"));

        var first = accountService.transfer(from.id(), request, "replay-key-1");
        var second = accountService.transfer(from.id(), request, "replay-key-1");

        assertThat(second.from().balance()).isEqualByComparingTo(first.from().balance());
        assertThat(accountService.getAccount(from.id()).balance()).isEqualByComparingTo("60.00");
    }
}
