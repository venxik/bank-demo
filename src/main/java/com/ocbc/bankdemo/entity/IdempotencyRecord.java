package com.ocbc.bankdemo.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Stores the result of a previously-processed request keyed by client-supplied
 * idempotency key, so a retried request returns the original result instead of
 * reprocessing (Banking Playbook, Scenario 2).
 */
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    private String idempotencyKey;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 2000)
    private String responseBody;

    @Column(nullable = false)
    private int statusCode;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, Long accountId, String responseBody, int statusCode) {
        this.idempotencyKey = idempotencyKey;
        this.accountId = accountId;
        this.responseBody = responseBody;
        this.statusCode = statusCode;
        this.createdAt = Instant.now();
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
