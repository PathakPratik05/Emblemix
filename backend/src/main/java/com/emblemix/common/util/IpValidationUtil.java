package com.emblemix.common.util;

import com.emblemix.common.exception.SsrfProtectionException;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

public final class IpValidationUtil {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
            "localhost",
            "localhost.localdomain",
            "metadata.google.internal",
            "instance-data",
            "169.254.169.254"
    );

    private IpValidationUtil() {}

    /**
     * Validates a target URL against SSRF attack vectors.
     * Checks protocol, host, and all resolved DNS addresses.
     */
    public static URI validateTargetUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new SsrfProtectionException("Target URL cannot be empty");
        }

        URI uri;
        try {
            uri = URI.create(urlString.trim());
        } catch (IllegalArgumentException e) {
            throw new SsrfProtectionException("Malformed target URL: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new SsrfProtectionException("Invalid protocol: '" + scheme + "'. Only HTTP and HTTPS are permitted.");
        }

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new SsrfProtectionException("Target URL host cannot be empty");
        }

        String normalizedHost = host.toLowerCase().trim();
        if (BLOCKED_HOSTNAMES.contains(normalizedHost)) {
            throw new SsrfProtectionException("Access to internal/metadata hostname '" + normalizedHost + "' is forbidden.");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(normalizedHost);
            if (addresses == null || addresses.length == 0) {
                throw new SsrfProtectionException("Failed to resolve host: " + normalizedHost);
            }

            for (InetAddress address : addresses) {
                validateAddress(address);
            }
        } catch (UnknownHostException e) {
            throw new SsrfProtectionException("Host resolution failed for: " + normalizedHost);
        }

        return uri;
    }

    /**
     * Inspects an individual InetAddress for private, loopback, link-local, or cloud metadata ranges.
     */
    public static void validateAddress(InetAddress address) {
        if (address == null) {
            throw new SsrfProtectionException("Invalid null IP address encountered");
        }

        if (address.isLoopbackAddress()) {
            throw new SsrfProtectionException("Loopback addresses are blocked: " + address.getHostAddress());
        }

        if (address.isAnyLocalAddress()) {
            throw new SsrfProtectionException("Wildcard local addresses are blocked: " + address.getHostAddress());
        }

        if (address.isLinkLocalAddress()) {
            throw new SsrfProtectionException("Link-local addresses are blocked: " + address.getHostAddress());
        }

        if (address.isSiteLocalAddress()) {
            throw new SsrfProtectionException("Private/Internal network addresses are blocked: " + address.getHostAddress());
        }

        if (address.isMulticastAddress()) {
            throw new SsrfProtectionException("Multicast addresses are blocked: " + address.getHostAddress());
        }

        byte[] bytes = address.getAddress();

        // Check IPv4 specific ranges
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;

            // 10.0.0.0/8
            if (b0 == 10) {
                throw new SsrfProtectionException("Private IP range 10.0.0.0/8 is blocked: " + address.getHostAddress());
            }
            // 172.16.0.0/12
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) {
                throw new SsrfProtectionException("Private IP range 172.16.0.0/12 is blocked: " + address.getHostAddress());
            }
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) {
                throw new SsrfProtectionException("Private IP range 192.168.0.0/16 is blocked: " + address.getHostAddress());
            }
            // 169.254.0.0/16 (Link Local / Cloud Metadata)
            if (b0 == 169 && b1 == 254) {
                throw new SsrfProtectionException("Cloud metadata / link-local address is blocked: " + address.getHostAddress());
            }
            // 100.64.0.0/10 (Carrier-grade NAT)
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) {
                throw new SsrfProtectionException("Shared address space 100.64.0.0/10 is blocked: " + address.getHostAddress());
            }
            // 0.0.0.0/8
            if (b0 == 0) {
                throw new SsrfProtectionException("Current network address is blocked: " + address.getHostAddress());
            }
        }

        // Check IPv6 unique local (fc00::/7) or IPv4-mapped IPv6
        if (bytes.length == 16) {
            int b0 = bytes[0] & 0xFF;
            if ((b0 & 0xFE) == 0xFC) {
                throw new SsrfProtectionException("IPv6 unique local address is blocked: " + address.getHostAddress());
            }
            // IPv4-mapped IPv6 (::ffff:0:0/96)
            boolean isMapped = true;
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) {
                    isMapped = false;
                    break;
                }
            }
            if (isMapped && (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF) {
                try {
                    byte[] ipv4Bytes = new byte[4];
                    System.arraycopy(bytes, 12, ipv4Bytes, 0, 4);
                    validateAddress(InetAddress.getByAddress(ipv4Bytes));
                } catch (UnknownHostException e) {
                    throw new SsrfProtectionException("Invalid mapped IPv6 address");
                }
            }
        }
    }
}
