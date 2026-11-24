package com.emblemix.monitoring.website;

import com.emblemix.common.util.IpValidationUtil;
import com.emblemix.monitoring.website.dto.WebsiteTestResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class WebsiteCheckExecutor {

    private static final Logger log = LoggerFactory.getLogger(WebsiteCheckExecutor.class);

    private final HttpClient httpClient;

    public WebsiteCheckExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER) // handled safely to prevent SSRF through redirection
                .build();
    }

    public WebsiteTestResultDto executeCheck(
            String targetUrl,
            int timeoutMs,
            int expectedMinStatus,
            int expectedMaxStatus,
            String expectedContent,
            boolean checkSsl,
            int sslExpiryThresholdDays
    ) {
        URI uri;
        try {
            uri = IpValidationUtil.validateTargetUrl(targetUrl);
        } catch (Exception e) {
            return new WebsiteTestResultDto(false, null, null, false, null, "SSRF or URL validation failed", e.getMessage());
        }

        long startTime = System.currentTimeMillis();
        Integer statusCode = null;
        Long responseTimeMs = null;
        String errorMessage = null;
        boolean healthy = true;
        Boolean sslValid = null;
        Integer sslDaysRemaining = null;

        // Perform safe HTTP check
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("User-Agent", "Emblemix-Monitoring-Agent/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long endTime = System.currentTimeMillis();
            responseTimeMs = endTime - startTime;
            statusCode = response.statusCode();

            // Handle safe redirect check (up to 3 redirects, each re-validated for SSRF)
            int redirectCount = 0;
            HttpResponse<String> currentResponse = response;
            URI currentUri = uri;

            while ((currentResponse.statusCode() >= 300 && currentResponse.statusCode() < 400) && redirectCount < 3) {
                redirectCount++;
                String location = currentResponse.headers().firstValue("Location").orElse(null);
                if (location == null) {
                    break;
                }
                URI nextUri = currentUri.resolve(location);
                IpValidationUtil.validateTargetUrl(nextUri.toString());

                HttpRequest redirectRequest = HttpRequest.newBuilder()
                        .uri(nextUri)
                        .timeout(Duration.ofMillis(timeoutMs))
                        .header("User-Agent", "Emblemix-Monitoring-Agent/1.0")
                        .GET()
                        .build();

                currentResponse = httpClient.send(redirectRequest, HttpResponse.BodyHandlers.ofString());
                currentUri = nextUri;
                statusCode = currentResponse.statusCode();
            }

            if (statusCode < expectedMinStatus || statusCode > expectedMaxStatus) {
                healthy = false;
                errorMessage = String.format("HTTP status %d outside expected range [%d, %d]",
                        statusCode, expectedMinStatus, expectedMaxStatus);
            } else if (expectedContent != null && !expectedContent.isBlank()) {
                if (!currentResponse.body().contains(expectedContent)) {
                    healthy = false;
                    errorMessage = "Expected content string was not found in response body";
                }
            }

        } catch (Exception e) {
            healthy = false;
            errorMessage = "Check failed: " + e.getMessage();
            responseTimeMs = System.currentTimeMillis() - startTime;
        }

        // Check SSL certificate if HTTPS
        if ("https".equalsIgnoreCase(uri.getScheme()) && checkSsl) {
            try {
                int port = uri.getPort() > 0 ? uri.getPort() : 443;
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                try (SSLSocket socket = (SSLSocket) factory.createSocket(uri.getHost(), port)) {
                    socket.setSoTimeout(timeoutMs);
                    socket.startHandshake();

                    java.security.cert.Certificate[] certs = socket.getSession().getPeerCertificates();
                    if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate x509) {
                        Date notAfter = x509.getNotAfter();
                        Instant expiry = notAfter.toInstant();
                        long days = ChronoUnit.DAYS.between(Instant.now(), expiry);
                        sslDaysRemaining = (int) days;
                        sslValid = days > 0;

                        if (days <= sslExpiryThresholdDays) {
                            if (healthy) {
                                errorMessage = "SSL certificate expires in " + days + " days (threshold: " + sslExpiryThresholdDays + ")";
                            }
                        }
                    }
                }
            } catch (Exception e) {
                sslValid = false;
                log.debug("SSL check error for {}: {}", uri.getHost(), e.getMessage());
            }
        }

        String message = healthy ? "Website is healthy and reachable" : errorMessage;
        return new WebsiteTestResultDto(healthy, statusCode, responseTimeMs, sslValid, sslDaysRemaining, message, errorMessage);
    }
}
