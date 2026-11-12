package com.borrowhub.util;

import org.erdtman.jcs.JsonCanonicalizer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility for RFC 8785 Canonical JSON Serialization and Request Hashing.
 * Enforces C23 / C26 specifications:
 * - RFC 8785 JSON Canonicalization Scheme (JCS)
 * - SHA-256(endpoint + ":" + userId + ":" + canonicalPayload)
 */
public final class CanonicalJsonUtil {

    private CanonicalJsonUtil() {}

    /**
     * Canonicalizes raw JSON string according to RFC 8785 (JCS).
     */
    public static String canonicalize(String json) {
        if (json == null || json.isBlank()) {
            return "{}";
        }
        try {
            JsonCanonicalizer jc = new JsonCanonicalizer(json);
            return jc.getEncodedString();
        } catch (Exception e) {
            // If raw string is not JSON object/array (e.g., empty or scalar), normalize trim
            return json.trim();
        }
    }

    /**
     * Computes the SHA-256 fingerprint for idempotency validation:
     * H = SHA-256(endpoint + ":" + userId + ":" + canonicalJson)
     */
    public static String computeRequestHash(String endpoint, Long userId, String rawBody) {
        String canonicalBody = canonicalize(rawBody);
        String inputToHash = endpoint + ":" + (userId != null ? userId : "ANONYMOUS") + ":" + canonicalBody;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(inputToHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
