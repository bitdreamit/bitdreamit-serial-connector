#!/usr/bin/env bash
# ============================================================================
# Bitdreamit Serial Connector — CLEAN BUILD SCRIPT
# ============================================================================
# Builds serial-shared.jar, serial-server.jar, serial-client.jar from source.
#
# This script GUARANTEES no duplicate classes by:
#   - Compiling serial-shared FIRST (has property classes)
#   - Compiling serial-server with serial-shared on classpath
#     (so it does NOT recompile property classes into serial-server.jar)
#   - Cleaning output directories before each build
#
# Usage:
#   1. Set MIRTH_LIBS to point at your Mirth Connect lib folder
#   2. Set KEYSTORE_PATH / KEYSTORE_ALIAS / KEYSTORE_PASS if you want signing
#   3. Run: ./build.sh
# ============================================================================

set -euo pipefail

# ---- CONFIG ----
# Path to Mirth Connect lib folder (contains donkey-*.jar, mirth-*.jar, xstream-*.jar, log4j-*.jar)
MIRTH_LIBS="${MIRTH_LIBS:-/opt/mirthconnect/lib}"

# jSerialComm jar (must be present in lib/ folder)
JSERIALCOMM_JAR="$(pwd)/lib/jSerialComm-2.10.4.jar"

# Signing (optional — leave empty to skip)
KEYSTORE_PATH="${KEYSTORE_PATH:-}"
KEYSTORE_ALIAS="${KEYSTORE_ALIAS:-bitdreamit}"
KEYSTORE_PASS="${KEYSTORE_PASS:-changeit}"

# Output directory
OUTPUT_DIR="$(pwd)/build/bitdreamit-serial-connector"

# Java version
JAVA_SOURCE="${JAVA_SOURCE:-1.8}"
JAVA_TARGET="${JAVA_TARGET:-1.8}"

# ---- VALIDATION ----
if [ ! -d "$MIRTH_LIBS" ]; then
    echo "ERROR: MIRTH_LIBS '$MIRTH_LIBS' does not exist."
    echo "Set it to your Mirth Connect lib folder, e.g.:"
    echo "  export MIRTH_LIBS=/opt/mirthconnect/lib"
    echo "  export MIRTH_LIBS='C:/Program Files/Mirth Connect/lib'"
    exit 1
fi

if [ ! -f "$JSERIALCOMM_JAR" ]; then
    echo "ERROR: jSerialComm jar not found at $JSERIALCOMM_JAR"
    echo "Place jSerialComm-2.10.4.jar in the lib/ folder."
    exit 1
fi

# Build classpath from Mirth libs + jSerialComm
MIRTH_CP=""
for jar in "$MIRTH_LIBS"/*.jar; do
    if [ -n "$MIRTH_CP" ]; then MIRTH_CP="$MIRTH_CP:"; fi
    MIRTH_CP="$MIRTH_CP$jar"
done
MIRTH_CP="$MIRTH_CP:$JSERIALCOMM_JAR"

echo "================================================================"
echo " Bitdreamit Serial Connector — Clean Build"
echo "================================================================"
echo ""
echo "Mirth libs:       $MIRTH_LIBS"
echo "jSerialComm jar:  $JSERIALCOMM_JAR"
echo "Output dir:       $OUTPUT_DIR"
echo "Java source/target: $JAVA_SOURCE / $JAVA_TARGET"
echo ""

# ---- CLEAN ----
echo "==> [1/5] Cleaning output directories"
rm -rf "$(pwd)/build/classes-shared"
rm -rf "$(pwd)/build/classes-server"
rm -rf "$(pwd)/build/classes-client"
rm -rf "$OUTPUT_DIR"
mkdir -p "$(pwd)/build/classes-shared"
mkdir -p "$(pwd)/build/classes-server"
mkdir -p "$(pwd)/build/classes-client"
mkdir -p "$OUTPUT_DIR/lib"
echo "    ✓ Cleaned"
echo ""

# ---- BUILD serial-shared.jar ----
echo "==> [2/5] Building serial-shared.jar"
find serial-shared -name '*.java' > /tmp/src-shared.txt
echo "    Compiling $(wc -l < /tmp/src-shared.txt) source files..."
javac -source "$JAVA_SOURCE" -target "$JAVA_TARGET" \
      -d "$(pwd)/build/classes-shared" \
      @/tmp/src-shared.txt \
      -classpath "$MIRTH_CP" 2>&1 | head -30 || true

# Verify property classes exist in shared
if [ ! -f "$(pwd)/build/classes-shared/com/bitdreamit/mirth/labextensions/serialconnector/SerialReceiverProperties.class" ]; then
    echo "    ❌ FAIL: SerialReceiverProperties.class not in serial-shared"
    exit 1
fi
if [ ! -f "$(pwd)/build/classes-shared/com/bitdreamit/mirth/labextensions/serialconnector/SerialDispatcherProperties.class" ]; then
    echo "    ❌ FAIL: SerialDispatcherProperties.class not in serial-shared"
    exit 1
fi
echo "    ✓ Property classes compiled into serial-shared"

# Create jar
( cd "$(pwd)/build/classes-shared" && jar cf "$OUTPUT_DIR/serial-shared.jar" . )
echo "    ✓ serial-shared.jar created"
echo ""

# ---- BUILD serial-server.jar ----
echo "==> [3/5] Building serial-server.jar"
find serial-server -name '*.java' > /tmp/src-server.txt
echo "    Compiling $(wc -l < /tmp/src-server.txt) source files..."
# serial-server depends on serial-shared.jar
javac -source "$JAVA_SOURCE" -target "$JAVA_TARGET" \
      -d "$(pwd)/build/classes-server" \
      @/tmp/src-server.txt \
      -classpath "$OUTPUT_DIR/serial-shared.jar:$MIRTH_CP" 2>&1 | head -30 || true

# CRITICAL: verify NO property classes leaked into server jar
SERVER_RECEIVER=$(find "$(pwd)/build/classes-server" -name "SerialReceiverProperties.class" 2>/dev/null | wc -l)
SERVER_DISPATCHER=$(find "$(pwd)/build/classes-server" -name "SerialDispatcherProperties.class" 2>/dev/null | wc -l)
if [ "$SERVER_RECEIVER" -gt 0 ] || [ "$SERVER_DISPATCHER" -gt 0 ]; then
    echo "    ❌ FAIL: Property classes leaked into serial-server build output!"
    echo "    This means serial-server source tree contains duplicate .java files."
    echo "    Delete these files from serial-server/:"
    echo "      serial-server/com/bitdreamit/mirth/labextensions/serialconnector/SerialReceiverProperties.java"
    echo "      serial-server/com/bitdreamit/mirth/labextensions/serialconnector/SerialDispatcherProperties.java"
    exit 1
fi
echo "    ✓ No duplicate property classes (clean)"

# Create jar
( cd "$(pwd)/build/classes-server" && jar cf "$OUTPUT_DIR/serial-server.jar" . )
echo "    ✓ serial-server.jar created"
echo ""

# ---- BUILD serial-client.jar ----
echo "==> [4/5] Building serial-client.jar"
find serial-client -name '*.java' > /tmp/src-client.txt
echo "    Compiling $(wc -l < /tmp/src-client.txt) source files..."
# serial-client depends on serial-shared.jar
javac -source "$JAVA_SOURCE" -target "$JAVA_TARGET" \
      -d "$(pwd)/build/classes-client" \
      @/tmp/src-client.txt \
      -classpath "$OUTPUT_DIR/serial-shared.jar:$MIRTH_CP" 2>&1 | head -30 || true

# CRITICAL: verify NO property classes leaked into client jar
CLIENT_RECEIVER=$(find "$(pwd)/build/classes-client" -name "SerialReceiverProperties.class" 2>/dev/null | wc -l)
CLIENT_DISPATCHER=$(find "$(pwd)/build/classes-client" -name "SerialDispatcherProperties.class" 2>/dev/null | wc -l)
if [ "$CLIENT_RECEIVER" -gt 0 ] || [ "$CLIENT_DISPATCHER" -gt 0 ]; then
    echo "    ❌ FAIL: Property classes leaked into serial-client build output!"
    exit 1
fi
echo "    ✓ No duplicate property classes (clean)"

# Create jar
( cd "$(pwd)/build/classes-client" && jar cf "$OUTPUT_DIR/serial-client.jar" . )
echo "    ✓ serial-client.jar created"
echo ""

# ---- COPY XML AND LIB ----
echo "==> [5/5] Copying plugin metadata and dependencies"
cp plugin.xml      "$OUTPUT_DIR/plugin.xml"
cp source.xml      "$OUTPUT_DIR/source.xml"
cp destination.xml "$OUTPUT_DIR/destination.xml"
cp "$JSERIALCOMM_JAR" "$OUTPUT_DIR/lib/jSerialComm-2.10.4.jar"
echo "    ✓ Copied plugin.xml, source.xml, destination.xml, lib/jSerialComm-2.10.4.jar"
echo ""

# ---- OPTIONAL SIGNING ----
if [ -n "$KEYSTORE_PATH" ] && [ -f "$KEYSTORE_PATH" ]; then
    echo "==> Signing jars"
    for j in serial-shared.jar serial-server.jar serial-client.jar; do
        echo "    Signing $j..."
        jarsigner -keystore "$KEYSTORE_PATH" -storepass "$KEYSTORE_PASS" \
                  "$OUTPUT_DIR/$j" "$KEYSTORE_ALIAS" 2>&1 | tail -3
    done
    echo ""
else
    echo "==> Skipping signing (set KEYSTORE_PATH to enable)"
    echo ""
fi

# ---- VERIFY ----
echo "================================================================"
echo " BUILD COMPLETE — VERIFICATION"
echo "================================================================"
echo ""
echo "Output folder: $OUTPUT_DIR"
echo ""
echo "Contents:"
ls -la "$OUTPUT_DIR" | grep -vE "^total|^d"
ls -la "$OUTPUT_DIR/lib" | grep -vE "^total|^d"
echo ""

echo "Jar contents (filtered):"
echo ""
echo "--- serial-shared.jar ---"
unzip -l "$OUTPUT_DIR/serial-shared.jar" | grep -E "Serial|bitdreamit|\.class" | grep -vE "META-INF"
echo ""
echo "--- serial-server.jar ---"
unzip -l "$OUTPUT_DIR/serial-server.jar" | grep -E "Serial|bitdreamit|\.class" | grep -vE "META-INF"
echo ""
echo "--- serial-client.jar ---"
unzip -l "$OUTPUT_DIR/serial-client.jar" | grep -E "Serial|bitdreamit|\.class" | grep -vE "META-INF"
echo ""

echo "================================================================"
echo " CRITICAL CHECKS"
echo "================================================================"
echo ""

# Check 1: serial-server.jar must NOT have property classes
SR_HAS_R=$(unzip -l "$OUTPUT_DIR/serial-server.jar" | grep -c "SerialReceiverProperties.class" || true)
SR_HAS_D=$(unzip -l "$OUTPUT_DIR/serial-server.jar" | grep -c "SerialDispatcherProperties.class" || true)
if [ "$SR_HAS_R" -gt 0 ] || [ "$SR_HAS_D" -gt 0 ]; then
    echo "❌ FAIL: serial-server.jar contains duplicate property classes"
else
    echo "✓ PASS: serial-server.jar has NO duplicate property classes"
fi

# Check 2: serial-shared.jar MUST have property classes
SH_HAS_R=$(unzip -l "$OUTPUT_DIR/serial-shared.jar" | grep -c "SerialReceiverProperties.class" || true)
SH_HAS_D=$(unzip -l "$OUTPUT_DIR/serial-shared.jar" | grep -c "SerialDispatcherProperties.class" || true)
if [ "$SH_HAS_R" -gt 0 ] && [ "$SH_HAS_D" -gt 0 ]; then
    echo "✓ PASS: serial-shared.jar has both property classes"
else
    echo "❌ FAIL: serial-shared.jar is missing property classes"
fi

# Check 3: serial-client.jar must NOT have property classes
CL_HAS_R=$(unzip -l "$OUTPUT_DIR/serial-client.jar" | grep -c "SerialReceiverProperties.class" || true)
CL_HAS_D=$(unzip -l "$OUTPUT_DIR/serial-client.jar" | grep -c "SerialDispatcherProperties.class" || true)
if [ "$CL_HAS_R" -gt 0 ] || [ "$CL_HAS_D" -gt 0 ]; then
    echo "❌ FAIL: serial-client.jar contains duplicate property classes"
else
    echo "✓ PASS: serial-client.jar has NO duplicate property classes"
fi

echo ""
echo "================================================================"
echo " INSTALLATION"
echo "================================================================"
echo ""
echo "1. Stop Mirth Connect service"
echo "2. Delete old plugin:"
echo "   rm -rf <mirth>/extensions/bitdreamit-serial-connector"
echo "3. Delete extension cache:"
echo "   rm -rf <mirth>/extensions/.cache"
echo "4. Copy new plugin:"
echo "   cp -r $OUTPUT_DIR <mirth>/extensions/bitdreamit-serial-connector"
echo "5. Start Mirth Connect service"
echo "6. Check mirth.log for:"
echo "   'SerialServerPlugin: XStream permission registered + annotations processed'"
echo "7. Enable your channel — it should now work!"
echo ""
