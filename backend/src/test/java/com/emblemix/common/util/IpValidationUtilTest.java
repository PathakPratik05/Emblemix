package com.emblemix.common.util;

import com.emblemix.common.exception.SsrfProtectionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class IpValidationUtilTest {

    @Test
    @DisplayName("Should accept valid public HTTP/HTTPS URLs")
    void testValidPublicUrls() {
        assertDoesNotThrow(() -> {
            URI uri = IpValidationUtil.validateTargetUrl("https://httpbin.org/status/200");
            assertEquals("https", uri.getScheme());
            assertEquals("httpbin.org", uri.getHost());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/file",
            "file:///etc/passwd",
            "gopher://example.com",
            "javascript:alert(1)"
    })
    @DisplayName("Should block non-HTTP/HTTPS protocols")
    void testDisallowedProtocols(String targetUrl) {
        assertThrows(SsrfProtectionException.class, () -> IpValidationUtil.validateTargetUrl(targetUrl));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:8080",
            "http://127.0.0.1",
            "http://127.0.0.2:9000",
            "http://metadata.google.internal",
            "http://169.254.169.254/latest/meta-data/"
    })
    @DisplayName("Should block loopback and cloud metadata targets")
    void testBlockedHostnames(String blockedUrl) {
        assertThrows(SsrfProtectionException.class, () -> IpValidationUtil.validateTargetUrl(blockedUrl));
    }

    @Test
    @DisplayName("Should block private IPv4 address ranges")
    void testPrivateIpRanges() throws Exception {
        // 10.0.0.0/8
        assertThrows(SsrfProtectionException.class, () ->
                IpValidationUtil.validateAddress(InetAddress.getByName("10.0.0.5")));

        // 172.16.0.0/12
        assertThrows(SsrfProtectionException.class, () ->
                IpValidationUtil.validateAddress(InetAddress.getByName("172.20.1.1")));

        // 192.168.0.0/16
        assertThrows(SsrfProtectionException.class, () ->
                IpValidationUtil.validateAddress(InetAddress.getByName("192.168.1.100")));

        // 169.254.169.254 (link local / metadata)
        assertThrows(SsrfProtectionException.class, () ->
                IpValidationUtil.validateAddress(InetAddress.getByName("169.254.169.254")));
    }
}
