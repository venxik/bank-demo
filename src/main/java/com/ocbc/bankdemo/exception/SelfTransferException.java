package com.ocbc.bankdemo.exception;

public class SelfTransferException extends RuntimeException {
    public SelfTransferException(Long accountId) {
        super("Cannot transfer from account " + accountId + " to itself");
    }
}
