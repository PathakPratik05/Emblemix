# Website Monitoring Engine

The Emblemix Website Monitoring Engine performs scheduled, high-concurrency availability and performance checks while strictly mitigating Server-Side Request Forgery (SSRF) threats.

---

## 1. Capabilities

1. **Availability & Status Code Verification**: Asserts that target HTTP response codes fall within the configurable range (e.g. 200–299).
2. **Response Time & Latency Metrics**: Tracks probe duration in milliseconds.
3. **SSL Certificate Expiration**: Connects over TLS to extract peer certificate expiration (`notAfter`) and triggers alerts when days remaining falls below threshold (default 30 days).
4. **Content Validation**: Verifies that specific expected substrings are present in the response body.
5. **Consecutive Failure Detection**: Avoids spurious flapping by requiring consecutive check failures (default: 2) before elevating an event to an incident.
6. **Automatic Recovery & Auto-Resolution**: When a monitored website resumes healthy status, a `WEBSITE_RECOVERED` event is dispatched that automatically transitions open incidents to `RESOLVED`.

---

## 2. SSRF Protection Architecture

Website monitoring inherently creates attack surface for SSRF. The `IpValidationUtil` and `WebsiteCheckExecutor` enforce defense-in-depth:

- **Protocol Restriction**: Only `http` and `https` schemes are accepted. Protocols like `file`, `gopher`, `ftp`, and `dict` are rejected immediately.
- **Loopback Blocking**: All loopback addresses (`127.0.0.0/8`, `::1`) and hostnames (`localhost`) are blocked.
- **Private IP Blocking**: RFC 1918 private subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) and carrier-grade NAT (`100.64.0.0/10`) are blocked.
- **Cloud Metadata Blocking**: Link-local address space (`169.254.0.0/16`), `169.254.169.254`, and cloud metadata hostnames (`metadata.google.internal`) are blocked.
- **DNS Resolution Validation**: Resolves all A and AAAA records for the target hostname before initiating connections.
- **Safe Redirection Policy**: HTTP redirects are not followed blindly. Each redirection destination (`Location` header) is parsed and passed through full SSRF re-validation before being fetched, capped at 3 redirects.
