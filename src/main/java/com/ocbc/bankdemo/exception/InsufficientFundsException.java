package com.ocbc.bankdemo.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long accountId) {
        super("Account " + accountId + " has insufficient funds for this withdrawal");
    }
}
