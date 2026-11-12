package com.borrowhub.repository;

import com.borrowhub.model.entity.BorrowingRequest;
import com.borrowhub.model.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowingRequestRepository extends JpaRepository<BorrowingRequest, Long> {
    Optional<BorrowingRequest> findByRequestNumber(String requestNumber);
    List<BorrowingRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<BorrowingRequest> findByStatus(RequestStatus status, Pageable pageable);
    Page<BorrowingRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<BorrowingRequest> findByStatus(RequestStatus status);
    List<BorrowingRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);
}
