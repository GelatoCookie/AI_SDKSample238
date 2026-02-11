package com.zebra.rfid.demo.sdksample;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.IRFIDLogger;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.TagData;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.SDKHandler;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Handler class for RFID operations, connection management, and event processing.
 * <p>
 * This class manages the lifecycle of the RFID reader, handles inventory and scanner operations,
 * and provides callback interfaces for UI updates.
 */
class RFIDHandler implements Readers.RFIDReaderEventHandler {
        // This method is intentionally left empty for future extension or testing purposes.
    private static final String CONNECTION_FAILED = "Connection failed";
    // This method is intentionally left empty for future extension or testing purposes.

    private static final String TAG = "RFID_SAMPLE";
    private Readers readers;
    private ArrayList<ReaderDevice> availableRFIDReaderList;
    private RFIDReader reader;
    private EventHandler eventHandler;
    private MainActivity context;
    private SDKHandler sdkHandler;
    private ScannerHandler scannerHandler;
    private ArrayList<DCSScannerInfo> scannerList;
    private int scannerID;
    private static final int MAX_POWER = 270;
    private static final String READER_NAME = "RFD4031-G10B700-WR";

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private int connectionTimer = 0;
    private volatile boolean bRfidBusy = false;
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (context != null) {
                context.updateReaderStatus(context.getString(R.string.connecting) + "... " + connectionTimer++ + "s", false);
                uiHandler.postDelayed(this, 1000);
            }
        }
    };
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Initializes the handler and SDK with the provided activity context.
     * @param activity The MainActivity context for UI and resource access.
     */
    void onCreate(MainActivity activity) {
        context = activity;
        scannerList = new ArrayList<>();
        scannerHandler = new ScannerHandler(activity);
        initSdk();
    }

    /**
     * Placeholder for test function 1.
     * @return String indicating not implemented.
     */
    public String Test1() { return "TO DO"; }

    /**
     * Placeholder for test function 2.
     * @return String indicating not implemented.
     */
    public String Test2() { return "TODO2"; }

    /**
     * Applies default antenna and singulation settings to the RFID reader.
     * @return Status message indicating result.
     */
    public String Defaults() {
        if (!isReaderConnected() || context == null) return context != null ? context.getString(R.string.not_connected) : "Not connected";
        try {
            Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(MAX_POWER);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);

            Antennas.SingulationControl singulationControl = reader.Config.Antennas.getSingulationControl(1);
            singulationControl.setSession(SESSION.SESSION_S0);
            singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
            singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
            reader.Config.Antennas.setSingulationControl(1, singulationControl);
        } catch (InvalidUsageException | OperationFailureException e) {
            Log.e(TAG, "Error in Defaults", e);
            return e.getMessage();
        }
        return context.getString(R.string.default_settings_applied);
    }

    /**
     * Checks if the RFID reader is currently connected.
     * @return True if connected, false otherwise.
     */
    private boolean isReaderConnected() {
        return reader != null && reader.isConnected();
    }

    /**
     * Toggles the connection state of the RFID reader (connect/disconnect).
     */
    public void toggleConnection() {
        if (isReaderConnected()) {
            executor.execute(this::disconnect);
        } else {
            connectReader();
        }
    }

    /**
     * Handles resume event for the activity, reconnecting the reader if needed.
     */
    void onResume() {
        executor.execute(() -> {
            String result = connect();
            if (context != null) {
                context.updateReaderStatus(result, isReaderConnected());
            }
        });
    }

    /**
     * Handles pause event for the activity, disconnecting the reader.
     */
    void onPause() {
        executor.execute(this::disconnect);
    }

    /**
     * Handles destroy event for the activity, disposing resources and shutting down executors.
     */
    void onDestroy() {
        executor.execute(() -> {
            dispose();
            context = null;
        });
        executor.shutdown();
    }

    private void initSdk() {
        Log.d(TAG, "initSdk");
        if (readers == null) {
            executor.execute(this::findAndHandleAvailableReaders);
        } else {
            connectReader();
        }
    }

    private void findAndHandleAvailableReaders() {
        InvalidUsageException exception = null;
        try {
            availableRFIDReaderList = findAvailableReadersAcrossTransports();
        } catch (InvalidUsageException e) {
            exception = e;
        }
        if (context != null) {
            final InvalidUsageException finalException = exception;
            context.runOnUiThread(() -> handleAvailableReadersResult(finalException));
        }
    }

    private void handleAvailableReadersResult(InvalidUsageException exception) {
        if (context == null) return;
        if (exception != null) {
            handleReaderInitializationFailure(context.getString(R.string.failed_to_get_available_readers) + "\n" + exception.getInfo(), context.getString(R.string.failed_to_get_readers));
        } else if (availableRFIDReaderList.isEmpty()) {
            handleReaderInitializationFailure(context.getString(R.string.no_available_readers_to_proceed), context.getString(R.string.no_readers_found));
        } else {
            connectReader();
        }
    
    }

    private ArrayList<ReaderDevice> findAvailableReadersAcrossTransports() throws InvalidUsageException {
        ENUM_TRANSPORT[] transports = {
                ENUM_TRANSPORT.SERVICE_USB,
                ENUM_TRANSPORT.RE_SERIAL,
                ENUM_TRANSPORT.RE_USB,
                ENUM_TRANSPORT.BLUETOOTH,
                ENUM_TRANSPORT.ALL
        };
        ArrayList<ReaderDevice> foundReaders = new ArrayList<>();
        for (int i = 0; i < transports.length; i++) {
            ENUM_TRANSPORT transport = transports[i];
            if (i == 0) {
                readers = new Readers(context, transport);
            } else {
                readers.setTransport(transport);
            }
            Log.d(TAG, "ECRT: #" + (i + 1) + " Getting Available Readers in " + transport.name());
            ArrayList<ReaderDevice> list = readers.GetAvailableRFIDReaderList();
            if (list != null && !list.isEmpty()) {
                foundReaders = new ArrayList<>(list);
                break;
            }
        }
        return foundReaders;
    }

    private void handleReaderInitializationFailure(String toastMessage, String statusMessage) {
        if (context != null) {
            context.sendToast(toastMessage);
            context.updateReaderStatus(statusMessage, false);
        }
        if (readers != null) {
            readers.Dispose();
            readers = null;
        }
    }

    /**
     * Placeholder for a test function. Throws UnsupportedOperationException if called.
     */
    public void testFunction() {
        // This method is intentionally left empty for future extension or testing purposes.
        throw new UnsupportedOperationException("testFunction() is not implemented yet.");
    }

    private void connectReader() {
        executor.execute(() -> {
            if (context != null) {
                context.updateReaderStatus(context.getString(R.string.connecting) + "...", false);
            }
            synchronized (RFIDHandler.this) {
                handleConnectionStatus();
            }
        });
    }

    private void handleConnectionStatus() {
        if (!isReaderConnected()) {
            getAvailableReader();
            String result = getConnectionResultString();
            if (context != null) {
                context.updateReaderStatus(result, isReaderConnected());
            }
        } else {
            if (context != null && reader != null) {
                context.updateReaderStatus(context.getString(R.string.connected) + ": " + reader.getHostName(), true);
            }
        }
    }

    private String getConnectionResultString() {
        if (reader != null) {
            return connect();
        } else if (context != null) {
            return context.getString(R.string.failed_to_find_reader);
        } else {
            return "Failed to find reader";
        }
    }

    private synchronized void getAvailableReader() {
        if (readers != null) {
            Readers.attach(this);
            try {
                ArrayList<ReaderDevice> availableReaders = readers.GetAvailableRFIDReaderList();
                if (availableReaders != null && !availableReaders.isEmpty()) {
                    availableRFIDReaderList = new ArrayList<>(availableReaders);
                    reader = selectReaderFromList(availableRFIDReaderList);
                }
            } catch (InvalidUsageException e) {
                Log.e(TAG, "Error getting available readers", e);
            }
        }
    }

    private RFIDReader selectReaderFromList(ArrayList<ReaderDevice> deviceList) {
        if (deviceList.size() == 1) {
            return deviceList.get(0).getRFIDReader();
        }
        for (ReaderDevice device : deviceList) {
            if (device != null && device.getName() != null && device.getName().startsWith(READER_NAME)) {
                return device.getRFIDReader();
            }
        }
        return null;
    }

    @Override
    public void RFIDReaderAppeared(ReaderDevice readerDevice) {
        connectReader();
    }

    @Override
    public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
        if (context != null && readerDevice != null) context.sendToast(context.getString(R.string.rfid_reader_disappeared, readerDevice.getName()));
        synchronized (RFIDHandler.this) {
            if (reader != null && readerDevice != null && readerDevice.getName().equals(reader.getHostName())) {
                executor.execute(this::disconnect);
            }
        }
    }

    private synchronized String connect() {
        if (reader == null) {
            return context != null ? context.getString(R.string.disconnected) : "Disconnected";
        }
        try {
            if (!reader.isConnected()) {
                return connectAndConfigureReader();
            } else {
                return getConnectedStatus();
            }
        } catch (InvalidUsageException e) {
            Log.e(TAG, CONNECTION_FAILED, e);
            return context != null ? context.getString(R.string.connection_failed, e.getMessage()) : CONNECTION_FAILED;
        } catch (OperationFailureException e) {
            Log.e(TAG, CONNECTION_FAILED, e);
            return context != null ? context.getString(R.string.connection_failed, e.getStatusDescription()) : CONNECTION_FAILED;
        }
    }

    private String connectAndConfigureReader() throws InvalidUsageException, OperationFailureException {
        connectionTimer = 0;
        bRfidBusy = false;
        uiHandler.post(timerRunnable);
        long startTime = System.currentTimeMillis();
        try {
            reader.connect();
        } finally {
            uiHandler.removeCallbacks(timerRunnable);
        }
        long duration = System.currentTimeMillis() - startTime;
        configureReader();
        if (reader.isConnected()) {
            return context != null ? context.getString(R.string.connected) + ": " + reader.getHostName() + " (" + duration + " ms)" : "Connected";
        }
        return context != null ? context.getString(R.string.disconnected) : "Disconnected";
    }

    private String getConnectedStatus() {
        return context != null ? context.getString(R.string.connected) + ": " + reader.getHostName() : "Connected";
    }

    private void configureReader() {
        if (reader != null && reader.isConnected()) {
            IRFIDLogger.getLogger("SDKSampleApp").EnableDebugLogs(true);
            try {
                if (eventHandler == null) eventHandler = new EventHandler();
                reader.Events.addEventsListener(eventHandler);
                reader.Events.setHandheldEvent(true);
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(false);
                reader.Events.setReaderDisconnectEvent(true);
                reader.Events.setInventoryStartEvent(true);
                reader.Events.setInventoryStopEvent(true);
                reader.Events.setOperationEndSummaryEvent(true);
            } catch (InvalidUsageException | OperationFailureException e) {
                Log.e(TAG, "Configuration failed", e);
            }
            Log.d(TAG, "ECRT: Configuration successful, RFID SDK Version = " + com.zebra.rfid.api3.BuildConfig.VERSION_NAME);
        }
    }

    public void setupScannerSdk() {
        if (context == null) return;
        initializeSdkHandlerIfNeeded();
        refreshScannerList();
        establishScannerSessionIfNeeded();
    }

    private void initializeSdkHandlerIfNeeded() {
        if (sdkHandler == null) {
            sdkHandler = new SDKHandler(context);
            sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC);
            sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);
            sdkHandler.dcssdkSetDelegate(scannerHandler);
            int notificationsMask = DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value |
                    DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value |
                    DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value |
                    DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value |
                    DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value;
            sdkHandler.dcssdkSubsribeForEvents(notificationsMask);
        }
    }

    private void refreshScannerList() {
        ArrayList<DCSScannerInfo> availableScanners = (ArrayList<DCSScannerInfo>) sdkHandler.dcssdkGetAvailableScannersList();
        if (scannerList != null) {
            scannerList.clear();
        } else {
            scannerList = new ArrayList<>();
        }
        if (availableScanners != null) {
            scannerList.addAll(availableScanners);
        }
    }

    private void establishScannerSessionIfNeeded() {
        if (reader != null && reader.isConnected()) {
            String hostName = reader.getHostName();
            for (DCSScannerInfo device : scannerList) {
                if (device != null && device.getScannerName() != null && hostName != null && device.getScannerName().contains(hostName)) {
                    try {
                        sdkHandler.dcssdkEstablishCommunicationSession(device.getScannerID());
                        scannerID = device.getScannerID();
                    } catch (Exception e) {
                        Log.e(TAG, "Error establishing scanner session", e);
                    }
                }
            }
        }
    }

    private synchronized void disconnect() {
        try {
            if (reader != null) {
                if (eventHandler != null) reader.Events.removeEventsListener(eventHandler);
                if (sdkHandler != null) {
                    sdkHandler.dcssdkTerminateCommunicationSession(scannerID);
                }
                reader.disconnect();
                if (context != null)
                    context.updateReaderStatus(context.getString(R.string.disconnected), false);
                reader.Dispose();
                reader = null;
                sdkHandler = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during disconnect", e);
        }
    }

    private synchronized void dispose() {
        disconnect();
        try {
            if (readers != null) {
                readers.Dispose();
                readers = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during dispose", e);
        }
    }

    /**
     * Starts RFID inventory operation if the reader is not busy.
     */
    synchronized void performInventory() {
        if(bRfidBusy) {
            Log.d(TAG, "RFID is busy, inventory request ignored.");
            return;
        }
        try {
            if (reader != null && reader.isConnected()) reader.Actions.Inventory.perform();
        } catch (InvalidUsageException | OperationFailureException e) {
            Log.e(TAG, "Error performing inventory", e);
        }
    }

    /**
     * Stops RFID inventory operation if the reader is connected.
     */
    synchronized void stopInventory() {
        try {
            if (reader != null && reader.isConnected()) reader.Actions.Inventory.stop();
        } catch (InvalidUsageException | OperationFailureException e) {
            Log.e(TAG, "Error stopping inventory", e);
            Log.e(TAG, CONNECTION_FAILED, e);
        }
    }

    /**
     * Initiates a scan code operation using the scanner SDK.
     */
    public void scanCode() {
        String inXml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        executor.execute(() -> executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_PULL_TRIGGER, inXml, scannerID));
    }

    private void executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE opCode, String inXML, int scannerID) {
        if (sdkHandler != null) {
            sdkHandler.dcssdkExecuteCommandOpCodeInXMLForScanner(opCode, inXML, new StringBuilder(), scannerID);
        }
    }

    /**
     * Event handler for RFID read and status events.
     */
    public class EventHandler implements RfidEventsListener {
        @Override
        public void eventReadNotify(RfidReadEvents e) {
            RFIDReader localReader = reader;
            if (localReader == null) return;
            try {
                TagData[] myTags = localReader.Actions.getReadTags(100);
                if (myTags != null && context != null) {
                    executor.execute(() -> {
                        if (context != null) context.handleTagdata(myTags);
                    });
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error in eventReadNotify", ex);
            }
        }

        @Override
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            if (rfidStatusEvents == null || rfidStatusEvents.StatusEventData == null) return;
            STATUS_EVENT_TYPE eventType = rfidStatusEvents.StatusEventData.getStatusEventType();
            if (eventType == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData != null) {
                    HANDHELD_TRIGGER_EVENT_TYPE triggerEvent = rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent();
                    boolean pressed = (triggerEvent == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED);
                    if (context != null) {
                        executor.execute(() -> {
                            if (context != null) context.handleTriggerPress(pressed);
                        });
                    }
                }
            } else if (eventType == STATUS_EVENT_TYPE.DISCONNECTION_EVENT) {
                executor.execute(() -> {
                    disconnect();
                    dispose();
                });
            } else if (eventType == STATUS_EVENT_TYPE.INVENTORY_START_EVENT) {
                bRfidBusy = true;
            } else if (eventType == STATUS_EVENT_TYPE.INVENTORY_STOP_EVENT) {
                bRfidBusy = false;
          } else if (eventType == STATUS_EVENT_TYPE.OPERATION_END_SUMMARY_EVENT) {
                Log.d(TAG, "Operation End Summary Event");
            }
            else {
                Log.d(TAG, "Unhandled status event: " + eventType);
            }
        }
    }

    /**
     * Interface for UI callback methods to handle tag data, trigger events, barcode data, and toasts.
     */
    interface ResponseHandlerInterface {
        void handleTagdata(TagData[] tagData);
        void handleTriggerPress(boolean pressed);
        void barcodeData(String val);
        void sendToast(String val);
    }
}
