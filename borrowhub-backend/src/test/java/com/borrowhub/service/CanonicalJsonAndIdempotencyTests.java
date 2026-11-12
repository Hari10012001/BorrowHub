package com.borrowhub.service;

import com.borrowhub.exception.BadRequestException;
import com.borrowhub.exception.ConflictException;
import com.borrowhub.repository.IdempotencyRecordRepository;
import com.borrowhub.util.CanonicalJsonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
public class CanonicalJsonAndIdempotencyTests {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Test
    @DisplayName("C26: RFC 8785 Canonical JSON key ordering and whitespace normalization")
    void testCanonicalJsonOrdering() {
        String json1 = "{\n  \"banana\": 2,\n  \"apple\": 1,\n  \"cherry\": [1, 2, 3]\n}";
        String json2 = "{\"cherry\":[1,2,3],\"banana\":2,\"apple\":1}";

        String canonical1 = CanonicalJsonUtil.canonicalize(json1);
        String canonical2 = CanonicalJsonUtil.canonicalize(json2);

        assertEquals("{\"apple\":1,\"banana\":2,\"cherry\":[1,2,3]}", canonical1);
        assertEquals(canonical1, canonical2, "Different key orders and formatting must produce identical canonical output");
    }

    @Test
    @DisplayName("C23/C26: SHA-256 fingerprinting guarantees payload and endpoint isolation")
    void testComputeRequestHash() {
        String endpoint = "/api/circulation/checkout";
        Long userId = 101L;
        String rawBody1 = "{\"resourceId\": 5, \"quantity\": 1}";
        String rawBody2 = "{\n  \"quantity\": 1,\n  \"resourceId\": 5\n}";

        String hash1 = CanonicalJsonUtil.computeRequestHash(endpoint, userId, rawBody1);
        String hash2 = CanonicalJsonUtil.computeRequestHash(endpoint, userId, rawBody2);

        assertEquals(hash1, hash2, "Reordered JSON attributes must compute to identical SHA-256 hash");

        String diffUserHash = CanonicalJsonUtil.computeRequestHash(endpoint, 102L, rawBody1);
        assertNotEquals(hash1, diffUserHash, "Different users must produce different request hashes");

        String diffEndpointHash = CanonicalJsonUtil.computeRequestHash("/api/reservations", userId, rawBody1);
        assertNotEquals(hash1, diffEndpointHash, "Different endpoints must produce different request hashes");
    }

    @Test
    @DisplayName("C23: Cross-user idempotency key reuse must be rejected with BadRequestException")
    void testCrossUserRejection() {
        String key = "idem-cross-user-" + UUID.randomUUID();
        String endpoint = "/api/circulation/checkout";
        String body = "{\"resourceId\": 1, \"quantity\": 1}";

        Long user1Id = 1L;
        Long user2Id = 2L;

        // User 1 acquires key: (key, userId, endpoint, body)
        IdempotencyService.IdempotencyContext ctx1 = idempotencyService.acquireOrReplay(key, user1Id, endpoint, body);
        assertNotNull(ctx1);
        assertFalse(ctx1.isReplayed());

        // User 2 attempts to use identical key
        assertThrows(BadRequestException.class, () -> {
            idempotencyService.acquireOrReplay(key, user2Id, endpoint, body);
        }, "Reusing key across different users must be rejected");
    }

    @Test
    @DisplayName("C23: Payload mismatch with same idempotency key must be rejected")
    void testPayloadMismatchRejection() {
        String key = "idem-mismatch-" + UUID.randomUUID();
        String endpoint = "/api/circulation/checkout";
        Long userId = 1L;

        String body1 = "{\"resourceId\": 1, \"quantity\": 1}";
        String body2 = "{\"resourceId\": 1, \"quantity\": 2}";

        // Acquire with body1
        IdempotencyService.IdempotencyContext ctx = idempotencyService.acquireOrReplay(key, userId, endpoint, body1);
        assertNotNull(ctx);

        // Attempt reuse with body2
        assertThrows(ConflictException.class, () -> {
            idempotencyService.acquireOrReplay(key, userId, endpoint, body2);
        }, "Altered payload with existing key must trigger ConflictException");
    }

    @Test
    @DisplayName("C23: Successful completion allows safe idempotent replay")
    void testSuccessfulReplay() {
        String key = "idem-replay-" + UUID.randomUUID();
        String endpoint = "/api/circulation/checkout";
        Long userId = 1L;
        String body = "{\"resourceId\": 2, \"quantity\": 1}";

        // First attempt: acquire
        IdempotencyService.IdempotencyContext ctx = idempotencyService.acquireOrReplay(key, userId, endpoint, body);
        assertFalse(ctx.isReplayed());

        // Complete the operation
        idempotencyService.complete(ctx, 200, "{\"success\":true,\"borrowingId\":99}");

        // Second attempt: replay
        IdempotencyService.IdempotencyContext replayCtx = idempotencyService.acquireOrReplay(key, userId, endpoint, body);
        assertTrue(replayCtx.isReplayed());
        assertEquals(200, replayCtx.getCachedStatus());
        assertTrue(replayCtx.getCachedBody().contains("borrowingId"));
        assertTrue(replayCtx.getCachedBody().contains("99"));
    }
}
