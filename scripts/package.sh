#!/usr/bin/env bash
# ==============================================================================
# ZX Spectrum 128K Emulator - Standalone Packaging Script
# Uses jlink and jpackage to produce native installers and portable bundles.
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
DIST_DIR="${ROOT_DIR}/dist"
TARGET_DIR="${ROOT_DIR}/target"
RUNTIME_DIR="${TARGET_DIR}/zxspectrum-runtime"

APP_NAME="ZXSpectrum"
MAIN_MODULE="nl.invokedynamic.spectrum.emulator/nl.invokedynamic.spectrum.ui.SpectrumApp"
VENDOR="nl.invokedynamic"
DESCRIPTION="ZX Spectrum 128K Emulator in modern Java 26 with JavaFX"
MAC_CATEGORY="public.app-category.games"
MAC_BUNDLE_ID="nl.invokedynamic.spectrum.emulator"

# Extract version from pom.xml and normalize for jpackage (must be digits and dots)
RAW_VERSION=$(grep -m1 '<version>' "${ROOT_DIR}/pom.xml" | sed -E 's/.*<version>(.*)<\/version>.*/\1/')
APP_VERSION=$(echo "${RAW_VERSION}" | sed -E 's/-SNAPSHOT//' | grep -E -o '^[0-9]+(\.[0-9]+)*' || echo "0.1.0")

RUN_TESTS=true
BUILD_ALL=true
BUILD_DMG=false
BUILD_APP=false
BUILD_ZIP=false
BUILD_DEB=false

print_usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Builds standalone native installers and portable bundles for ZX Spectrum 128K Emulator.

Options:
  --all            Build all available formats for current OS (default)
  --dmg            Build macOS .dmg installer (macOS only)
  --app            Build macOS .app bundle (macOS only)
  --zip            Build portable zip archive
  --deb            Build Linux .deb package (Linux only)
  --no-test        Skip running unit tests before packaging
  -h, --help       Show this help message
EOF
}

# Parse command-line flags
SPECIFIC_TARGET=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --all)
            BUILD_ALL=true
            shift
            ;;
        --dmg)
            BUILD_DMG=true
            SPECIFIC_TARGET=true
            shift
            ;;
        --app)
            BUILD_APP=true
            SPECIFIC_TARGET=true
            shift
            ;;
        --zip)
            BUILD_ZIP=true
            SPECIFIC_TARGET=true
            shift
            ;;
        --deb)
            BUILD_DEB=true
            SPECIFIC_TARGET=true
            shift
            ;;
        --no-test)
            RUN_TESTS=false
            shift
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

if [ "${SPECIFIC_TARGET}" = false ]; then
    BUILD_ALL=true
fi

OS_NAME="$(uname -s)"

# Determine active targets if --all
if [ "${BUILD_ALL}" = true ]; then
    BUILD_ZIP=true
    if [[ "${OS_NAME}" == "Darwin" ]]; then
        BUILD_APP=true
        BUILD_DMG=true
    elif [[ "${OS_NAME}" == "Linux" ]]; then
        BUILD_DEB=true
    fi
fi

# Detect Maven command
MVN_CMD="mvn"
if [ -f "${ROOT_DIR}/mvnw" ]; then
    MVN_CMD="${ROOT_DIR}/mvnw"
fi

# Verify jpackage is available
if ! command -v jpackage &> /dev/null; then
    echo "ERROR: 'jpackage' command not found in PATH."
    echo "Please ensure JDK 26+ is installed and configured (e.g. via SDKMAN)."
    exit 1
fi

echo "============================================================"
echo " Packaging ${APP_NAME} v${APP_VERSION} (${RAW_VERSION})"
echo " OS: ${OS_NAME} | Runtime: ${RUNTIME_DIR}"
echo "============================================================"

mkdir -p "${DIST_DIR}"

# 1. Run tests if requested
if [ "${RUN_TESTS}" = true ]; then
    echo "--> Running unit test suite..."
    (cd "${ROOT_DIR}" && "${MVN_CMD}" test)
fi

# 2. Build trimmed jlink modular runtime image
echo "--> Generating trimmed jlink runtime image..."
(cd "${ROOT_DIR}" && "${MVN_CMD}" javafx:jlink)

if [ ! -d "${RUNTIME_DIR}" ]; then
    echo "ERROR: Runtime directory ${RUNTIME_DIR} not found!"
    exit 1
fi

# 3. macOS App Bundle (.app)
if [ "${BUILD_APP}" = true ] && [[ "${OS_NAME}" == "Darwin" ]]; then
    echo "--> Creating macOS Application Bundle (${APP_NAME}.app)..."
    rm -rf "${DIST_DIR}/${APP_NAME}.app"
    jpackage \
        --name "${APP_NAME}" \
        --app-version "${APP_VERSION}" \
        --vendor "${VENDOR}" \
        --description "${DESCRIPTION}" \
        --runtime-image "${RUNTIME_DIR}" \
        --module "${MAIN_MODULE}" \
        --java-options "--enable-native-access=javafx.graphics" \
        --mac-package-identifier "${MAC_BUNDLE_ID}" \
        --mac-app-category "${MAC_CATEGORY}" \
        --dest "${DIST_DIR}" \
        --type app-image
    echo "    Created: ${DIST_DIR}/${APP_NAME}.app"
fi

# 4. macOS DMG Installer (.dmg)
if [ "${BUILD_DMG}" = true ] && [[ "${OS_NAME}" == "Darwin" ]]; then
    echo "--> Creating macOS DMG Installer (${APP_NAME}-${APP_VERSION}.dmg)..."
    rm -f "${DIST_DIR}/${APP_NAME}-${APP_VERSION}.dmg"
    
    # Use pre-built app-image if present, otherwise direct from runtime-image
    if [ -d "${DIST_DIR}/${APP_NAME}.app" ]; then
        jpackage \
            --name "${APP_NAME}" \
            --app-version "${APP_VERSION}" \
            --app-image "${DIST_DIR}/${APP_NAME}.app" \
            --dest "${DIST_DIR}" \
            --type dmg
    else
        jpackage \
            --name "${APP_NAME}" \
            --app-version "${APP_VERSION}" \
            --vendor "${VENDOR}" \
            --description "${DESCRIPTION}" \
            --runtime-image "${RUNTIME_DIR}" \
            --module "${MAIN_MODULE}" \
            --java-options "--enable-native-access=javafx.graphics" \
            --mac-package-identifier "${MAC_BUNDLE_ID}" \
            --mac-app-category "${MAC_CATEGORY}" \
            --dest "${DIST_DIR}" \
            --type dmg
    fi
    echo "    Created: ${DIST_DIR}/${APP_NAME}-${APP_VERSION}.dmg"
fi

# 5. Linux DEB Package (.deb)
if [ "${BUILD_DEB}" = true ] && [[ "${OS_NAME}" == "Linux" ]]; then
    echo "--> Creating Linux DEB Package..."
    jpackage \
        --name "zxspectrum" \
        --app-version "${APP_VERSION}" \
        --vendor "${VENDOR}" \
        --description "${DESCRIPTION}" \
        --runtime-image "${RUNTIME_DIR}" \
        --module "${MAIN_MODULE}" \
        --dest "${DIST_DIR}" \
        --type deb \
        --linux-shortcut
    echo "    Created DEB in ${DIST_DIR}/"
fi

# 6. Portable Zip Bundle
if [ "${BUILD_ZIP}" = true ]; then
    OS_LOWER=$(echo "${OS_NAME}" | tr '[:upper:]' '[:lower:]')
    ZIP_NAME="${APP_NAME}-${APP_VERSION}-${OS_LOWER}"
    echo "--> Creating Portable Zip Archive (${ZIP_NAME}.zip)..."
    rm -f "${DIST_DIR}/${ZIP_NAME}.zip"
    
    # Copy zxspectrum-runtime into temporary staging dir for clean archiving
    TMP_STAGE=$(mktemp -d)
    cp -R "${RUNTIME_DIR}" "${TMP_STAGE}/${APP_NAME}"
    
    # Include README or quick run instructions
    cat << EOF > "${TMP_STAGE}/${APP_NAME}/README.txt"
ZX Spectrum 128K Emulator
=========================
A cycle-accurate ZX Spectrum 128K emulator written in modern Java with JavaFX.
This distribution includes its own self-contained Java runtime.
No installation or Java setup is required.

To run:
- macOS/Linux: ./bin/zxspectrum
- Windows: .\\bin\\zxspectrum.bat (or run java inside bin)
EOF

    (cd "${TMP_STAGE}" && zip -q -r "${DIST_DIR}/${ZIP_NAME}.zip" "${APP_NAME}")
    rm -rf "${TMP_STAGE}"
    echo "    Created: ${DIST_DIR}/${ZIP_NAME}.zip"
fi

echo "============================================================"
echo " Packaging Complete! Artifacts in dist/:"
ls -lh "${DIST_DIR}"
echo "============================================================"
