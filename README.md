# Bitdreamit Serial Connector

RS-232/RS-485 source and destination connector for Mirth Connect 4.5+.

## Project Structure

```
bitdreamit-serial-connector/
├── .idea/                    ← IntelliJ project config (open this folder)
│   ├── modules.xml
│   ├── misc.xml
│   ├── vcs.xml
│   └── libraries/
│       ├── mirth_server.xml
│       ├── mirth_client.xml
│       └── jSerialComm.xml
├── shared/                   ← Shared module (property classes, config, port manager)
│   ├── shared.iml
│   ├── pom.xml
│   └── src/...
├── server/                   ← Server module (connectors, plugin, mode providers)
│   ├── server.iml
│   ├── pom.xml
│   └── src/...
├── client/                   ← Client module (settings panels, UI, client plugin)
│   ├── client.iml
│   ├── pom.xml
│   └── src/...
├── lib/
│   └── jSerialComm-2.10.4.jar
├── stubs/                    ← Compile-time fallback for Purgable
├── distribution/
│   └── build.sh              ← Command-line build script
├── plugin.xml
├── source.xml
├── destination.xml
├── pom.xml                   ← Root Maven POM
├── .gitignore
└── .gitattributes
```

## Prerequisites

1. **JDK 8+** (tested with OpenJDK 17)
2. **Mirth Connect 4.5+ jars** extracted to `../mirth-libs/`:
   ```
   mirth-libs/
   ├── server/
   │   ├── mirth-server.jar
   │   ├── donkey-server.jar
   │   ├── mirth-client-core.jar
   │   └── log4j-1.2-api-2.17.2.jar
   └── client/
       ├── mirth-client.jar
       ├── mirth-client-core.jar
       ├── miglayout-core-4.2.jar
       ├── miglayout-swing-4.2.jar
       └── log4j-1.2-api-2.17.2.jar
   ```
   Copy these from your Mirth Connect installation (`<mirth>/lib/`).

## Build with IntelliJ IDEA

1. Open the project folder in IntelliJ IDEA
2. The `.idea/` config is pre-configured — modules and libraries are auto-detected
3. `Build → Build Project` or `Build → Build Artifacts`
4. Output goes to `out/`

## Build with Maven

```bash
mvn clean package
```

## Build with command line (no IDE)

```bash
cd distribution
./build.sh           # build all jars
./build.sh clean     # clean output
./build.sh sign      # build + sign jars
./build.sh rebuild   # clean + build
```

Output: `out/bitdreamit-serial-connector-{shared,server,client}.jar`

## Deploy to Mirth

1. Stop Mirth Connect
2. Copy the 3 jars + XML files + lib/ to `<mirth>/extensions/bitdreamit-serial-connector/`
3. Delete `<mirth>/extensions/.cache/`
4. Start Mirth Connect
5. Restart Mirth Administrator

## Features

- RS-232/RS-485 source + destination connectors
- 5 transmission modes (RAW, LINE, FRAME, MLLP, ASTM) — dynamically loaded
- Dynamic transmission mode provider system (add new modes without modifying connectors)
- Auto-reconnect with exponential backoff
- Protocol traffic logging (hex + ASCII preview)
- Serial statistics (bytes, messages, errors, reconnects)
- Custom checksum algorithms for ASTM (STANDARD, MOD256, XOR, NONE)
- Response processing for destination
- NextGen-style message templates
- TCP-style UI with strict two-column grid alignment
- Idle/receive timeouts
- Port alias support
- Connection pooling
- Signal control (DTR/RTS/CTS/DSR/DCD)
