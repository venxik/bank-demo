package com.ocbc.bankdemo.dto;

public record TransferResponse(AccountResponse from, AccountResponse to) {
}
