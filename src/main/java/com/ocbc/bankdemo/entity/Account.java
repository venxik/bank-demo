package com.ocbc.bankdemo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Optimistic lock: JPA bumps this on every UPDATE and includes it in the
     * WHERE clause. Two transactions that both read version 3 and both try to
     * write version 4 — the second UPDATE affects zero rows, Hibernate detects
     * that and throws OptimisticLockException instead of silently letting the
     * second writer clobber the first (lost update). No row lock is held
     * between read and write, so it's cheap, but the caller must retry on
     * conflict — see GlobalExceptionHandler.
     */
    @Version
    private long version;

    protected Account() {
        // required by JPA
    }

    public Account(String accountNumber, String ownerName, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = balance;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }
}
