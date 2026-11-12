package com.borrowhub.repository;

import com.borrowhub.model.entity.ReturnTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnTransactionRepository extends JpaRepository<ReturnTransaction, Long> {
    List<ReturnTransaction> findByBorrowingRecordIdOrderByReturnedAtDesc(Long borrowingRecordId);
}
