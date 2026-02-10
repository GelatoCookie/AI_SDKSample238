
# Zebra RFID SDK Sample Application

**Release Version: 1.0.0**  
Branch: `master`  
Tag: `1.0.0`


This sample application demonstrates how to integrate and use the Zebra RFID API3 SDK for Android. It provides a basic implementation for connecting to Zebra RFID readers, performing inventory operations, and scanning barcodes.

---
**Changelog**

- **1.0.0 (2026-02-10):**
	- Major code cleanup and refactoring for maintainability and style compliance.
	- Reduced method complexity and improved naming conventions in all main Java classes.
	- Removed unused fields, improved exception handling, and modernized code style.
	- Ready for commit and further development.

New Remote:
https://github.com/GelatoCookie/AI_SDKSample238

Old Remote: https://github.com/GelatoCookie/AI_Java_SDKSample

mac:AI_Java_SDKSample-version1-new chucklin$ pwd
/Users/chucklin/myprojects/MS_Code/Java_AI/AI_Java_SDKSample-version1-new

git tag --list
Your current tag is v1. This tag marks your initial release in the repository.

git status
On branch master

---
**Changelog**

	- Initial public release (1.0.0):
		- Full source code, build scripts, and documentation
		- All Zebra .aar libraries included in `app/libs`
		- Complete RFID and barcode sample logic
		- Javadoc and code comments updated for all main classes
		- Git history and release tagging established
---

## Features

- **RFID Reader Connection:** Connect and disconnect from Zebra RFID readers via Bluetooth.
- **RFID Inventory:** Perform real-time inventory to discover RFID tags.
- **Tag Data Display:** View unique tag IDs along with their peak RSSI values in a list.
- **Barcode Scanning:** Utilize the reader's scanner to capture barcode data.
- **Hardware Trigger Support:** Handle hardware trigger presses for starting/stopping inventory or scanning barcodes.
- **Settings Configuration:** Basic demonstration of modifying antenna settings and singulation control.

## Project Structure

- `MainActivity.java`: Handles the UI logic and user interactions.
- `RFIDHandler.java`: Manages the lifecycle and operations of the Zebra RFID reader.
- `ScannerHandler.java`: Handles barcode scanning functionality.
- `MainUIHandler.java`: Helper for UI-related updates.

## Getting Started

### Prerequisites

- Android Studio Flamingo or newer.
- Android device running API level 26 (Oreo) or higher.
- A compatible Zebra RFID Reader (e.g., RFD8500, RFD40, RFD90).
- Zebra RFID SDK for Android (ensure `.aar` or `.jar` files are placed in `app/libs`).

### Installation

1. Clone or download this repository.
2. Open the project in Android Studio.
3. Sync project with Gradle files.
4. Build and deploy the application to your Android device.

## Usage

1. **Connect:** Launch the app and tap the status text at the top to search and connect to an available Zebra RFID reader.
2. **Inventory:** Once connected, tap **Start Inventory** to begin reading tags. Tap **Stop Inventory** to end the session.
3. **Barcode Scan:** Tap the **Scan** button or use the hardware trigger (if configured) to scan barcodes.
4. **View Tags:** Discovered tags will appear in the list with their EPC and RSSI.

## Permissions

The application requires the following permissions:
- `BLUETOOTH`
- `BLUETOOTH_ADMIN`
- `BLUETOOTH_SCAN` (Android 12+)
- `BLUETOOTH_CONNECT` (Android 12+)
- `ACCESS_FINE_LOCATION` (Required for Bluetooth scanning on some Android versions)

## License

This project is for demonstration purposes. See the [LICENSE](LICENSE) file for details.
