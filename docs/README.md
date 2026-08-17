# Bitdreamit Serial Connector — Final Source (v1.3.0)

This is the **complete, fixed source tree** that solves the channel-enable silent failure.

## What was fixed

1. **Duplicate class issue (root cause #1)** — `SerialReceiverProperties` and `SerialDispatcherProperties` now exist ONLY in `serial-shared/`. The `build.sh` script verifies this and fails the build if duplicates appear.

2. **Silent XStream permission failure (root cause #2)** — All logging now goes through log4j (mirth.log). The `findXStream()` method has 4 fallback strategies. On failure, `logger.error()` is called — no more silent failures.

3. **Hardcoded log file path** — Removed. All diagnostic output goes to mirth.log via log4j.

4. **Shallow clone()** — Property classes now deep-clone their nested objects.

5. **`plugin.xml` now includes `<serverClasses>`** — Ensures `SerialServerPlugin` is loaded by Mirth so XStream permission registration runs at startup.

## File structure

```
final-source/
├── build.sh                    ← Clean build script (prevents duplicate classes)
├── plugin.xml                  ← Plugin metadata (with serverClasses)
├── source.xml                  ← Source connector metadata
├── destination.xml             ← Destination connector metadata
├── lib/                        ← Put jSerialComm-2.10.4.jar here
├── serial-shared/              ← Property classes + port manager (SHARED jar)
│   └── com/bitdreamit/mirth/labextensions/serialconnector/
│       ├── SerialPortConfig.java
│       ├── SerialReceiverProperties.java    ← implements SourceConnectorPropertiesInterface
│       ├── SerialDispatcherProperties.java  ← implements DestinationConnectorPropertiesInterface
│       ├── SerialPortManager.java
│       ├── ProtocolLogEntry.java
│       └── SerialStatistics.java
├── serial-server/              ← Server connectors (SERVER jar)
│   └── com/bitdreamit/mirth/labextensions/serialconnector/
│       ├── SerialServerPlugin.java          ← XStream permission registration (fixed)
│       ├── SerialSourceConnector.java       ← Source connector (fixed)
│       ├── SerialDestinationConnector.java  ← Destination connector (fixed)
│       └── SerialFrameAssembler.java
├── serial-client/              ← Client UI panels (CLIENT jar)
│   └── com/bitdreamit/mirth/labextensions/serialconnector/
│       ├── SerialSourceSettingsPanel.java
│       ├── SerialDestinationSettingsPanel.java
│       ├── SerialConnectorSettingsPanel.java
│       └── SerialTransmissionModeDialog.java
└── docs/
    └── README.md               ← This file
```

## How to build

### Prerequisites
- JDK 1.8+ (1.8 recommended for Mirth compatibility)
- Mirth Connect installed (for the lib jars)
- `jSerialComm-2.10.4.jar` placed in `lib/`

### Steps

1. Set the Mirth lib path:
   ```bash
   # Linux
   export MIRTH_LIBS=/opt/mirthconnect/lib
   # Windows (Git Bash)
   export MIRTH_LIBS="C:/Program Files/Mirth Connect/lib"
   ```

2. (Optional) Set signing keystore:
   ```bash
   export KEYSTORE_PATH=/path/to/your-keystore.jks
   export KEYSTORE_ALIAS=bitdreamit
   export KEYSTORE_PASS=changeit
   ```

3. Run the build:
   ```bash
   chmod +x build.sh
   ./build.sh
   ```

4. The built plugin will be in `build/bitdreamit-serial-connector/`

5. The script automatically verifies:
   - serial-server.jar has NO duplicate property classes
   - serial-shared.jar HAS both property classes
   - serial-client.jar has NO duplicate property classes

## How to install

1. Stop Mirth Connect service
2. Delete old plugin: `rm -rf <mirth>/extensions/bitdreamit-serial-connector`
3. Delete extension cache: `rm -rf <mirth>/extensions/.cache`
4. Copy new plugin: `cp -r build/bitdreamit-serial-connector <mirth>/extensions/`
5. Start Mirth Connect service

## How to verify

Check `mirth.log` at startup. You should see:

```
SerialServerPlugin: XStream permission registered + annotations processed for 6 classes.
SerialServerPlugin: start() called — plugin is active.
```

If you see instead:
```
SerialServerPlugin: XStream instance is NULL — channel enable WILL FAIL!
```
...then `findXStream()` cannot find the XStream instance on your Mirth version. This means the `ObjectXMLSerializer` internal layout is different. Check the Mirth version and report the issue.

## Channel enable should now work

After installing this fixed plugin:
1. Open Mirth Administrator
2. Right-click your serial channel → Enable
3. Channel should transition to STARTED within 2 seconds
4. Check mirth.log for `Opened serial port: COMx @ <baudrate>`

If enable still fails, mirth.log will now have a CLEAR error message (not a silent one).
