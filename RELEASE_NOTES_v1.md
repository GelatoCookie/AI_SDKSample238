# Release Notes - v1 (2026-02-10)

## Overview
This is the initial release (v1) of the Zebra RFID SDK Sample Application, providing a working Android app for RFID inventory and barcode scanning using Zebra's SDKs.

## Major Features
- RFID inventory and tag reading with unique tag count
- Barcode scanning integration
- UI for connecting/disconnecting RFID reader
- Start/Stop inventory controls
- Real-time tag and barcode display
- Device connection status and error handling
- Automated build, deploy, and launch script (auto.sh)
- Automated build, deploy, and launch script (build_deploy_launch.sh)

## Improvements
- Refactored UI logic for button enable/disable
- Consistent naming for UI elements (StartButton, btnStop)
- Added/updated Javadoc for all major classes and methods
- Improved error handling and user feedback
- Cleaned up and organized codebase

## Files Changed
- MainActivity.java: UI logic, Javadoc, button wiring
- RFIDHandler.java: Javadoc, event handling, logic improvements
- activity_main.xml: Button IDs, onClick wiring, layout updates
- build_deploy_launch.sh: New script for build/deploy/launch automation

## Known Issues
- None reported for this release

## Getting Started
See README.md for build and usage instructions.

---
