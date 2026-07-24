package com.ocbc.bankdemo.controller;

import com.ocbc.bankdemo.dto.AccountResponse;
import com.ocbc.bankdemo.dto.AmountRequest;
import com.ocbc.bankdemo.dto.CreateAccountRequest;
import com.ocbc.bankdemo.dto.TransferRequest;
import com.ocbc.bankdemo.dto.TransferResponse;
import com.ocbc.bankdemo.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<AccountResponse> listAccounts() {
        return accountService.listAccounts();
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        return accountService.getAccount(id);
    }

    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable Long id,
                                    @Valid @RequestBody AmountRequest request,
                                    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return accountService.deposit(id, request, idempotencyKey);
    }

    @PostMapping("/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable Long id,
                                     @Valid @RequestBody AmountRequest request,
                                     @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return accountService.withdraw(id, request, idempotencyKey);
    }

    @PostMapping("/{id}/transfer")
    public TransferResponse transfer(@PathVariable Long id,
                                      @Valid @RequestBody TransferRequest request,
                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return accountService.transfer(id, request, idempotencyKey);
    }
}
