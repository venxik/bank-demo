package com.ocbc.bankdemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocbc.bankdemo.dto.AccountResponse;
import com.ocbc.bankdemo.dto.AmountRequest;
import com.ocbc.bankdemo.dto.CreateAccountRequest;
import com.ocbc.bankdemo.dto.TransferRequest;
import com.ocbc.bankdemo.dto.TransferResponse;
import com.ocbc.bankdemo.entity.Account;
import com.ocbc.bankdemo.entity.IdempotencyRecord;
import com.ocbc.bankdemo.exception.AccountNotFoundException;
import com.ocbc.bankdemo.exception.DuplicateAccountNumberException;
import com.ocbc.bankdemo.exception.InsufficientFundsException;
import com.ocbc.bankdemo.exception.SelfTransferException;
import com.ocbc.bankdemo.notification.NotificationService;
import com.ocbc.bankdemo.repository.AccountRepository;
import com.ocbc.bankdemo.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;
    // Spring injects every @Component implementing NotificationService here —
    // ConsoleNotificationService and SmsNotificationService today, a future
    // PushNotificationService tomorrow with zero edits to this class (OCP/DIP).
    private final List<NotificationService> notificationServices;

    public AccountService(AccountRepository accountRepository,
                           IdempotencyRecordRepository idempotencyRecordRepository,
                           ObjectMapper objectMapper,
                           List<NotificationService> notificationServices) {
        this.accountRepository = accountRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
        this.notificationServices = notificationServices;
    }

    private void notifyAll(Account account, String message) {
        notificationServices.forEach(service -> service.notify(account, message));
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByAccountNumber(request.accountNumber())) {
            throw new DuplicateAccountNumberException(request.accountNumber());
        }
        Account account = new Account(request.accountNumber(), request.ownerName(), request.initialBalance());
        return AccountResponse.from(accountRepository.save(account));
    }

    public AccountResponse getAccount(Long id) {
        return AccountResponse.from(findAccountOrThrow(id));
    }

    public List<AccountResponse> listAccounts() {
        return accountRepository.findAll().stream().map(AccountResponse::from).toList();
    }

    @Transactional
    public AccountResponse deposit(Long id, AmountRequest request, String idempotencyKey) {
        return executeIdempotent(idempotencyKey, id, AccountResponse.class, () -> {
            Account account = findAccountOrThrow(id);
            account.credit(request.amount());
            Account saved = accountRepository.save(account);
            notifyAll(saved, "Deposit of " + request.amount() + " received");
            return AccountResponse.from(saved);
        });
    }

    @Transactional
    public AccountResponse withdraw(Long id, AmountRequest request, String idempotencyKey) {
        return executeIdempotent(idempotencyKey, id, AccountResponse.class, () -> {
            Account account = findAccountOrThrow(id);
            if (account.getBalance().compareTo(request.amount()) < 0) {
                throw new InsufficientFundsException(id);
            }
            account.debit(request.amount());
            Account saved = accountRepository.save(account);
            notifyAll(saved, "Withdrawal of " + request.amount() + " processed");
            return AccountResponse.from(saved);
        });
    }

    /**
     * Debit + credit in one local @Transactional boundary — fine because both
     * accounts live in the same database. A real transfer between accounts owned
     * by different services (or different banks) can't take a single DB
     * transaction across a network boundary; that's what pushes you toward a
     * Saga (orchestrated or choreographed compensating steps) instead of ACID.
     *
     * Accounts are always locked in ascending-id order (not "from then to") so
     * two transfers racing in opposite directions (A->B and B->A) can't each
     * hold one row and wait on the other — a classic deadlock shape under
     * pessimistic locking. This demo has no explicit lock mode, so it's instead
     * exposed to a lost-update race, which is what the @Version field (see
     * Account) guards against.
     */
    @Transactional
    public TransferResponse transfer(Long fromId, TransferRequest request, String idempotencyKey) {
        Long toId = request.toAccountId();
        if (fromId.equals(toId)) {
            throw new SelfTransferException(fromId);
        }

        return executeIdempotent(idempotencyKey, fromId, TransferResponse.class, () -> {
            Long firstId = fromId.compareTo(toId) <= 0 ? fromId : toId;
            Long secondId = fromId.compareTo(toId) <= 0 ? toId : fromId;
            Account first = findAccountOrThrow(firstId);
            Account second = findAccountOrThrow(secondId);
            Account from = fromId.equals(firstId) ? first : second;
            Account to = fromId.equals(firstId) ? second : first;

            if (from.getBalance().compareTo(request.amount()) < 0) {
                throw new InsufficientFundsException(fromId);
            }
            from.debit(request.amount());
            to.credit(request.amount());
            accountRepository.save(from);
            accountRepository.save(to);
            notifyAll(from, "Transfer of " + request.amount() + " sent to account " + toId);
            notifyAll(to, "Transfer of " + request.amount() + " received from account " + fromId);

            return new TransferResponse(AccountResponse.from(from), AccountResponse.from(to));
        });
    }

    /**
     * Idempotency-key mechanism (Banking Playbook, Scenario 2): a key not seen before
     * runs the action and stores its result; a key already seen returns the stored
     * result instead of reprocessing the mutation.
     */
    private <T> T executeIdempotent(String idempotencyKey, Long accountId, Class<T> responseType, Supplier<T> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        return idempotencyRecordRepository.findById(idempotencyKey)
                .map(record -> deserialize(record, responseType))
                .orElseGet(() -> {
                    T response = action.get();
                    idempotencyRecordRepository.save(new IdempotencyRecord(
                            idempotencyKey, accountId, serialize(response), 200));
                    return response;
                });
    }

    private Account findAccountOrThrow(Long id) {
        return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    private <T> String serialize(T response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize idempotent response", e);
        }
    }

    private <T> T deserialize(IdempotencyRecord record, Class<T> responseType) {
        try {
            return objectMapper.readValue(record.getResponseBody(), responseType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize stored idempotent response", e);
        }
    }
}
