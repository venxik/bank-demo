package com.ocbc.bankdemo.repository;

import com.ocbc.bankdemo.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
}
