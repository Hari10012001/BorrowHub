package com.borrowhub.scheduler;

import com.borrowhub.model.entity.Reservation;
import com.borrowhub.repository.ReservationRepository;
import com.borrowhub.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sweeper job to expire unfulfilled HELD reservations after their claim deadline.
 * Enforces C21: Expiry transition is committed durably.
 */
@Component
public class ReservationExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public ReservationExpiryScheduler(ReservationRepository reservationRepository,
                                       ReservationService reservationService) {
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${borrowhub.reservation.expiry-check-delay-ms:60000}")
    public void sweepExpiredReservations() {
        List<Reservation> expiredReservations = reservationRepository.findExpiredHeldReservations();
        if (!expiredReservations.isEmpty()) {
            logger.info("Found {} expired held reservations to process", expiredReservations.size());
            for (Reservation res : expiredReservations) {
                try {
                    reservationService.expireReservationDurable(res.getId());
                } catch (Exception e) {
                    logger.error("Failed to expire reservation id {}: {}", res.getId(), e.getMessage());
                }
            }
        }
    }
}
