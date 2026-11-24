package com.emblemix.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FingerprintUtilTest {

    @Test
    @DisplayName("Should generate deterministic SHA-256 fingerprint for identical input components")
    void testDeterministicFingerprint() {
        String orgId = UUID.randomUUID().toString();
        String sysId = UUID.randomUUID().toString();

        String fp1 = FingerprintUtil.generateFingerprint(orgId, sysId, "WEBSITE", "WEBSITE_DOWN");
        String fp2 = FingerprintUtil.generateFingerprint(orgId, sysId, "WEBSITE", "WEBSITE_DOWN");

        assertEquals(fp1, fp2);
        assertEquals(64, fp1.length()); // SHA-256 hex length
    }

    @Test
    @DisplayName("Should generate distinct fingerprints for different systems or event types")
    void testDistinctFingerprints() {
        String orgId = UUID.randomUUID().toString();
        String sysId1 = UUID.randomUUID().toString();
        String sysId2 = UUID.randomUUID().toString();

        String fp1 = FingerprintUtil.generateFingerprint(orgId, sysId1, "WEBSITE", "WEBSITE_DOWN");
        String fp2 = FingerprintUtil.generateFingerprint(orgId, sysId2, "WEBSITE", "WEBSITE_DOWN");

        assertNotEquals(fp1, fp2);
    }
}
