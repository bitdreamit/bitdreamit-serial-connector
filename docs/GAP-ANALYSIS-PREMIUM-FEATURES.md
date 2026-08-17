# Bitdreamit Serial Connector — Gap Analysis & Premium Feature Roadmap

## Source References
- Mirth Official Serial Connector: https://docs.nextgen.com/en-US/mirthc2ae-connect-user-guide-3299192/serial-connector-14837
- ASTM E1381 Transmission Mode: https://docs.nextgen.com/en-US/mirthc2ae-connect-user-guide-3299192/astm-e1381-transmission-mode-14835
- Competitor (Meditecs ASTM): https://www.meditecs.com/astm-extension-for-mirth-connect

---

## CURRENT FEATURES (Already Implemented)

| Feature | Status |
|---|---|
| RS-232 serial port communication | ✅ Present |
| Source connector (Serial Reader) | ✅ Present |
| Destination connector (Serial Writer) | ✅ Present |
| Baud rate / data bits / stop bits / parity | ✅ Present |
| Flow control (None/RTS-CTS/XON-XOFF/DSR-DTR) | ✅ Present |
| Charset selection | ✅ Present |
| Binary (Base64) mode | ✅ Present |
| Read/Write timeouts | ✅ Present |
| Buffer size | ✅ Present |
| DTR/RTS signal control | ✅ Present |
| CTS/DSR/DCD signal waiting | ✅ Present |
| Send break before open | ✅ Present |
| Flush on open/close | ✅ Present |
| Auto-reconnect with health monitor | ✅ Present |
| RAW transmission mode | ✅ Present |
| LINE transmission mode | ✅ Present |
| FRAME transmission mode | ✅ Present |
| MLLP transmission mode | ✅ Present |
| ASTM transmission mode | ✅ Present |
| ACK/NAK bytes (MLLP/ASTM) | ✅ Present |
| MLLPv2 commit ACK | ✅ Present |
| Max retry count | ✅ Present |
| Connection pooling (destination) | ✅ Present |
| Wait for ACK after write (destination) | ✅ Present |
| TCP-style UI (two-column grid) | ✅ Present |
| Mirth 4.5.2 XStream compatibility | ✅ Present |

---

## MISSING FEATURES — Premium/Professional Additions

### 1. Connection Idle Timeout (PROFESSIONAL)
**Mirth TCP has this.** Close the connection after N milliseconds of inactivity.
- **Config field:** `idleTimeout` (int, default 0 = disabled)
- **Use case:** Free up ports when devices go offline
- **Priority:** HIGH

### 2. Receive Idle Timeout (PREMIUM)
Close the connection if no data received within N ms.
- **Config field:** `receiveIdleTimeout` (int, default 0 = disabled)
- **Use case:** Detect dead connections
- **Priority:** MEDIUM

### 3. Max Reconnect Delay / Backoff (PREMIUM)
Exponential backoff for reconnection attempts — don't hammer the port.
- **Config fields:** `maxReconnectDelay` (int), `backoffMultiplier` (double)
- **Use case:** Prevent port exhaustion during outages
- **Priority:** MEDIUM

### 4. Send Buffer Size / Receive Buffer Size (PROFESSIONAL)
Separate buffer sizes for send and receive (currently single `bufferSize`).
- **Config fields:** `sendBufferSize`, `receiveBufferSize`
- **Use case:** High-throughput scenarios
- **Priority:** LOW

### 5. SO_KEEPALIVE equivalent (PROFESSIONAL)
Periodic keepalive signal to maintain port state.
- **Config field:** `keepAlive` (boolean), `keepAliveInterval` (int)
- **Use case:** Long-lived idle connections
- **Priority:** LOW

### 6. Custom Baud Rate (PREMIUM)
Allow arbitrary baud rates (not just the dropdown list).
- **UI:** Editable baud rate field + "Custom" option
- **Use case:** Non-standard devices (e.g., 31250, 76800)
- **Priority:** MEDIUM

### 7. Port Aliases / Friendly Names (PREMIUM)
Display human-readable names alongside port identifiers.
- **Config field:** `portAlias` (String)
- **Use case:** "Analyzer 1" instead of "COM3"
- **Priority:** LOW

### 8. Statistics Tracking (PREMIUM — partially implemented)
`SerialStatistics` class exists but is NOT wired into connectors.
- **Missing:** bytes read/written counters, error count, reconnect count
- **UI:** Display live statistics in a status bar
- **Priority:** MEDIUM

### 9. Protocol Logging / Analyzer (PREMIUM — partially implemented)
`ProtocolLogEntry` class exists, `protocolLoggingEnabled` config exists, but NO actual logging.
- **Missing:** Write log entries to file/database
- **Missing:** UI viewer for protocol log
- **Priority:** MEDIUM

### 10. Transmission Mode: Delimited (PROFESSIONAL)
Mirth TCP has a "Delimited" transmission mode (delimiter-based framing).
- **Our LINE mode** already covers this — equivalent.
- **Status:** ✅ Already covered by LINE mode

### 11. Custom Checksum Algorithm for ASTM (PREMIUM)
Mirth's ASTM mode supports custom checksum algorithms.
- **Config fields:** `checksumAlgorithm` (enum: ASTM_STANDARD, MOD256, CUSTOM)
- **Use case:** Non-standard ASTM devices
- **Priority:** LOW

### 12. Bidirectional Mode (PREMIUM)
Source and destination share the same serial port simultaneously.
- **Use case:** ASTM devices that send and receive on the same port
- **Status:** Partially supported via `SerialPortManager` reference counting
- **Priority:** LOW

### 13. Response Generation / Processing (PROFESSIONAL)
For destination connectors — process the response after sending.
- **Config fields:** `responseDelimiter`, `processResponse` (boolean)
- **Use case:** Query-response protocols
- **Priority:** MEDIUM

### 14. Connection Test Button (PREMIUM UI)
"Test Connection" button in the UI that tries to open the port.
- **Use case:** Verify configuration before saving
- **Priority:** HIGH (easy to implement, high perceived value)

### 15. Port Status Indicator (PREMIUM UI)
Live status indicator showing if the port is open/closed/error.
- **Use case:** Visual feedback
- **Priority:** MEDIUM

### 16. Export/Import Configuration (PROFESSIONAL)
Save/load port configuration as a file.
- **Use case:** Reuse configs across channels
- **Priority:** LOW

### 17. Command-Line Configuration (PREMIUM)
Configure via VM properties or environment variables.
- **Use case:** Headless/server deployments
- **Priority:** LOW

### 18. Multi-Port Aggregation (ENTERPRISE)
Read from multiple serial ports simultaneously into one channel.
- **Use case:** Multiple analyzers feeding one channel
- **Priority:** LOW (complex)

### 19. SSL/TLS over Serial (ENTERPRISE)
Not applicable to RS-232 — skip.

### 20. Roche Elecsys/Cobas Frame Support (PREMIUM)
Competitor (Meditecs) specifically supports Roche frame structures.
- **Use case:** Roche analyzers
- **Priority:** LOW (niche)

---

## RECOMMENDED ADDITIONS (Priority Order)

### Phase 1 — Quick Wins (HIGH value, LOW effort)
1. **Connection Test button** in UI
2. **Idle timeout** config field
3. **Custom baud rate** (editable field)
4. **Wire up SerialStatistics** (counters already exist)
5. **Wire up ProtocolLogEntry** (logging framework exists)

### Phase 2 — Professional Polish (MEDIUM effort)
6. **Receive idle timeout**
7. **Exponential backoff reconnect**
8. **Response processing** for destination
9. **Port status indicator** in UI
10. **Statistics display** in UI

### Phase 3 — Premium Features (HIGHER effort)
11. **Custom checksum algorithm** for ASTM
12. **Protocol log viewer** UI
13. **Export/Import config**
14. **Port aliases**
15. **Bidirectional mode** improvements
