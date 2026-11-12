package com.borrowhub.scheduler;

import com.borrowhub.model.entity.BorrowingRecord;
import com.borrowhub.model.enums.BorrowingStatus;
import com.borrowhub.repository.BorrowingRecordRepository;
import com.borrowhub.service.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Daily midnight job to transition loans whose due date has passed into OVERDUE.
 * Evaluates date purely against MySQL CURRENT_DATE() (C40/C42).
 */
@Component
public class OverdueDetectionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OverdueDetectionScheduler.class);

    private final BorrowingRecordRepository borrowingRecordRepository;
    private final OutboxService outboxService;

    public OverdueDetectionScheduler(BorrowingRecordRepository borrowingRecordRepository,
                                     OutboxService outboxService) {
        this.borrowingRecordRepository = borrowingRecordRepository;
        this.outboxService = outboxService;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void sweepOverdueLoans() {
        logger.info("Executing midnight overdue loan detection sweep...");
        List<BorrowingRecord> becomingOverdue = borrowingRecordRepository.findRecordsBecomingOverdue();

        for (BorrowingRecord record : becomingOverdue) {
            record.setPreviousStatus(record.getStatus());
            record.setStatus(BorrowingStatus.OVERDUE);
            borrowingRecordRepository.save(record);

            outboxService.publishEvent(
                    "BORROWING_OVERDUE",
                    "BORROWING_RECORD",
                    record.getId(),
                    String.format("{\"borrowingRecordId\":%d,\"userId\":%d,\"resourceId\":%d,\"dueDate\":\"%s\"}",
                            record.getId(), record.getUser().getId(), record.getResource().getId(), record.getDueDate())
            );
        }

        if (!becomingOverdue.isEmpty()) {
            logger.info("Transitioned {} borrowing records to OVERDUE status", becomingOverdue.size());
        }
    }
}
