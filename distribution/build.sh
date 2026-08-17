#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Build script for bitdreamit-serial-connector
# -----------------------------------------------------------------------------
# Produces three jars + XML files ready for Mirth extension folder:
#   out/bitdreamit-serial-connector-shared.jar
#   out/bitdreamit-serial-connector-server.jar
#   out/bitdreamit-serial-connector-client.jar
#   out/plugin.xml
#   out/source.xml
#   out/destination.xml
#   out/lib/jSerialComm-2.10.4.jar
#
# Requirements:
#   - JDK 8+ (tested with OpenJDK 17)
#   - Mirth Connect 4.5+ jars unpacked at ../mirth-libs/{server,client}/
#
# Usage:
#   cd distribution && ./build.sh            # build all jars
#   cd distribution && ./build.sh clean      # remove out/ folder
#   cd distribution && ./build.sh sign       # build + sign jars
# -----------------------------------------------------------------------------
set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$PROJECT_DIR/out"

MIRTH_LIBS_DIR="${MIRTH_LIBS_DIR:-$PROJECT_DIR/../mirth-libs}"
SERVER_LIB="$MIRTH_LIBS_DIR/server"
CLIENT_LIB="$MIRTH_LIBS_DIR/client"

SHARED_MODEL_JAR="$CLIENT_LIB/mirth-client-core.jar"
DONKEY_SERVER_JAR="$SERVER_LIB/donkey-server.jar"
LOG4J_API_JAR="$SERVER_LIB/log4j-1.2-api-2.17.2.jar"
MIGLAYOUT_CORE_JAR="$CLIENT_LIB/miglayout-core-4.2.jar"
MIGLAYOUT_SWING_JAR="$CLIENT_LIB/miglayout-swing-4.2.jar"
JSERIALCOMM_JAR="$PROJECT_DIR/lib/jSerialComm-2.10.4.jar"

SHARED_CP="$SHARED_MODEL_JAR:$DONKEY_SERVER_JAR:$LOG4J_API_JAR"
SERVER_CP="$SERVER_LIB/mirth-server.jar:$DONKEY_SERVER_JAR:$SHARED_MODEL_JAR:$LOG4J_API_JAR:$JSERIALCOMM_JAR"
CLIENT_CP="$CLIENT_LIB/mirth-client.jar:$DONKEY_SERVER_JAR:$SHARED_MODEL_JAR:$MIGLAYOUT_CORE_JAR:$MIGLAYOUT_SWING_JAR:$LOG4J_API_JAR"

# Stub fallback
if [ ! -f "$DONKEY_SERVER_JAR" ]; then
    if [ -d "$PROJECT_DIR/stubs" ]; then
        echo "[build] WARNING: $DONKEY_SERVER_JAR not found — using stubs/"
        STUBS_SOURCEPATH="$PROJECT_DIR/stubs"
    else
        echo "[build] ERROR: $DONKEY_SERVER_JAR not found and no stubs/ directory."
        exit 2
    fi
fi

clean() { rm -rf "$OUT_DIR"; }

build() {
    echo "[build] project dir: $PROJECT_DIR"
    echo "[build] mirth libs:  $MIRTH_LIBS_DIR"
    mkdir -p "$OUT_DIR/shared" "$OUT_DIR/server" "$OUT_DIR/client" "$OUT_DIR/lib"

    echo "[build] compiling shared..."
    javac -d "$OUT_DIR/shared" -cp "$SHARED_CP" \
        -sourcepath "$PROJECT_DIR/shared/src${STUBS_SOURCEPATH:+:$STUBS_SOURCEPATH}" \
        $(find "$PROJECT_DIR/shared/src" -name "*.java")

    echo "[build] compiling server..."
    javac -cp "$OUT_DIR/shared:$SERVER_CP" -d "$OUT_DIR/server" \
        -sourcepath "$PROJECT_DIR/server/src${STUBS_SOURCEPATH:+:$STUBS_SOURCEPATH}" \
        $(find "$PROJECT_DIR/server/src" -name "*.java")

    echo "[build] compiling client..."
    javac -cp "$OUT_DIR/shared:$CLIENT_CP" -d "$OUT_DIR/client" \
        -sourcepath "$PROJECT_DIR/client/src${STUBS_SOURCEPATH:+:$STUBS_SOURCEPATH}" \
        $(find "$PROJECT_DIR/client/src" -name "*.java")

    echo "[build] packaging shared jar (with manifest)..."
    # Create MANIFEST.MF — Mirth Launcher requires it in EVERY jar
    mkdir -p "$OUT_DIR/shared/META-INF"
    cat > "$OUT_DIR/shared/META-INF/MANIFEST.MF" << 'EOF'
Manifest-Version: 1.0
Created-By: Bitdreamit Serial Connector Build 1.3.0
Implementation-Title: bitdreamit-serial-connector-shared
Implementation-Version: 1.3.0
Implementation-Vendor: Bit Dream IT
EOF
    jar cfm "$OUT_DIR/bitdreamit-serial-connector-shared.jar" \
        "$OUT_DIR/shared/META-INF/MANIFEST.MF" -C "$OUT_DIR/shared" .

    echo "[build] packaging server jar (shared + server merged, with manifest)..."
    rm -rf "$OUT_DIR/server-jar"; mkdir -p "$OUT_DIR/server-jar"
    cp -r "$OUT_DIR/shared/." "$OUT_DIR/server-jar/"
    cp -r "$OUT_DIR/server/." "$OUT_DIR/server-jar/"
    # Create manifest for server jar
    mkdir -p "$OUT_DIR/server-jar/META-INF"
    cat > "$OUT_DIR/server-jar/META-INF/MANIFEST.MF" << 'EOF'
Manifest-Version: 1.0
Created-By: Bitdreamit Serial Connector Build 1.3.0
Implementation-Title: bitdreamit-serial-connector-server
Implementation-Version: 1.3.0
Implementation-Vendor: Bit Dream IT
EOF
    jar cfm "$OUT_DIR/bitdreamit-serial-connector-server.jar" \
        "$OUT_DIR/server-jar/META-INF/MANIFEST.MF" -C "$OUT_DIR/server-jar" .

    echo "[build] packaging client jar (shared + client merged, with manifest)..."
    rm -rf "$OUT_DIR/client-jar"; mkdir -p "$OUT_DIR/client-jar"
    cp -r "$OUT_DIR/shared/." "$OUT_DIR/client-jar/"
    cp -r "$OUT_DIR/client/." "$OUT_DIR/client-jar/"
    # Create manifest for client jar
    mkdir -p "$OUT_DIR/client-jar/META-INF"
    cat > "$OUT_DIR/client-jar/META-INF/MANIFEST.MF" << 'EOF'
Manifest-Version: 1.0
Created-By: Bitdreamit Serial Connector Build 1.3.0
Implementation-Title: bitdreamit-serial-connector-client
Implementation-Version: 1.3.0
Implementation-Vendor: Bit Dream IT
EOF
    jar cfm "$OUT_DIR/bitdreamit-serial-connector-client.jar" \
        "$OUT_DIR/client-jar/META-INF/MANIFEST.MF" -C "$OUT_DIR/client-jar" .

    echo "[build] copying XML + lib..."
    cp "$PROJECT_DIR/plugin.xml"       "$OUT_DIR/plugin.xml"
    cp "$PROJECT_DIR/source.xml"       "$OUT_DIR/source.xml"
    cp "$PROJECT_DIR/destination.xml"  "$OUT_DIR/destination.xml"
    cp "$JSERIALCOMM_JAR"             "$OUT_DIR/lib/jSerialComm-2.10.4.jar"

    echo ""
    echo "[build] complete. Artifacts:"
    ls -la "$OUT_DIR"/*.jar "$OUT_DIR"/*.xml
    ls -la "$OUT_DIR/lib/"
}

sign() {
    build
    KEYSTORE="${KEYSTORE:-$PROJECT_DIR/../keystore.jks}"
    ALIAS="${ALIAS:-bitdreamit}"
    STOREPASS="${STOREPASS:-changeit}"
    echo "[sign] signing jars with $KEYSTORE..."
    for jar in shared server client; do
        jarsigner -keystore "$KEYSTORE" -storepass "$STOREPASS" \
            "$OUT_DIR/bitdreamit-serial-connector-$jar.jar" "$ALIAS"
    done
    echo "[sign] done."
}

case "${1:-build}" in
    clean)   clean ;;
    build)   build ;;
    sign)    sign ;;
    rebuild) clean && build ;;
    *) echo "Usage: $0 {clean|build|sign|rebuild}"; exit 1 ;;
esac
