package com.zebra.rfid.demo.sdksample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zebra.rfid.api3.TagData;

import java.util.ArrayList;
import java.util.HashSet;


/**
 * Main Activity for the RFID Sample application.
 */
public class MainActivity extends AppCompatActivity implements RFIDHandler.ResponseHandlerInterface {

    /**
     * List of tag IDs detected by the RFID reader.
     */
    private final ArrayList<String> tagList = new ArrayList<>();

    /**
     * Set of unique tag IDs detected by the RFID reader.
     */
    private final HashSet<String> tagSet = new HashSet<>();

    /**
     * Handler for RFID operations and responses.
     */
    private RFIDHandler rfidHandler;

    /**
     * Request code for Bluetooth permission.
     */
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;

    /**
     * Progress dialog for showing connection status.
     */
    private ProgressDialog progressDialog;

    // UI Components
    private TextView statusTextViewRFID;
    private ListView tagListView;
    private ArrayAdapter<String> tagAdapter;
    private Button btnStart;
    private Button btnStop;
    private Button btnScan;
    private TextView scanResultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * Called when the activity is starting. Initializes UI and RFID handler.
         * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this contains the data it most recently supplied.
         */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupUI();

        rfidHandler = new RFIDHandler();
        checkPermissionsAndInit();
    }

    /**
     * Consolidates UI initialization and setup.
     */
    private void setupUI() {
        String appName = getString(R.string.app_name);
        try {
            setTitle(appName + " (" + com.zebra.rfid.api3.BuildConfig.VERSION_NAME + ")");
        } catch (Exception e) {
            /* Exception intentionally ignored for compatibility with Java 8. */
            setTitle(appName);
        }

        statusTextViewRFID = findViewById(R.id.textViewStatusrfid);
        if (statusTextViewRFID != null) {
            statusTextViewRFID.setOnClickListener(v -> {
                if (rfidHandler != null) {
                    rfidHandler.toggleConnection();
                }
            });
        }

        tagListView = findViewById(R.id.tag_list);
        tagAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tagList);
        if (tagListView != null) {
            tagListView.setAdapter(tagAdapter);
        }

        btnStart = findViewById(R.id.StartButton);
        btnStop = findViewById(R.id.btnStop);
        btnScan = findViewById(R.id.scan);
        scanResultText = findViewById(R.id.scanResult);

        if (btnStart != null) btnStart.setEnabled(false);
        if (btnStop != null) btnStop.setEnabled(false);
        if (btnScan != null) btnScan.setEnabled(false);
    }

    public void updateReaderStatus(String status, boolean isConnected) {
        /**
         * Updates the reader status on the UI.
         * @param status The status message to display.
         * @param isConnected Whether the reader is connected.
         */
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || status == null) return;
            if (statusTextViewRFID != null) {
                statusTextViewRFID.setText(status);
                int color = isConnected ? R.color.status_connected : R.color.status_disconnected;
                statusTextViewRFID.setTextColor(ContextCompat.getColor(this, color));
            }
            if (btnStart != null) {
                btnStart.setEnabled(isConnected);
            }
            if (status.contains(getString(R.string.connecting))) {
                showProgressDialog(status);
            } else {
                dismissProgressDialog();
            }
        });
    }

    private void showProgressDialog(String message) {
        if (isFinishing() || isDestroyed()) return;
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void checkPermissionsAndInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                        BLUETOOTH_PERMISSION_REQUEST_CODE);
            } else {
                rfidHandler.onCreate(this);
            }
        } else {
            rfidHandler.onCreate(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (rfidHandler != null) rfidHandler.onCreate(this);
            } else {
                Toast.makeText(this, getString(R.string.bluetooth_permissions_not_granted), Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (rfidHandler == null) return super.onOptionsItemSelected(item);
        
        String result;
        if (id == R.id.antenna_settings) {
            result = rfidHandler.Test1();
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.Singulation_control) {
            result = rfidHandler.Test2();
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.Default) {
            result = rfidHandler.Defaults();
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (rfidHandler != null) {
            rfidHandler.onPause();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (rfidHandler != null) rfidHandler.onResume();
    }

    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        if (rfidHandler != null) {
            rfidHandler.onDestroy();
        }
        super.onDestroy();
    }

    private void toggleInventoryButtons(boolean isRunning) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (btnStart != null) btnStart.setEnabled(!isRunning);
            if (btnStop != null) btnStop.setEnabled(isRunning);
        });
    }

    public void setScanButtonEnabled(boolean enabled) {
        /**
         * Enables or disables the scan button.
         * @param enabled True to enable, false to disable.
         */
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (btnScan != null) {
                btnScan.setEnabled(enabled);
            }
        });
    }

    public void StartInventory(View view) {
        /**
         * Starts RFID inventory when the start button is pressed.
         * @param view The view that triggered this method.
         */
        toggleInventoryButtons(true);
        clearTagData();
        if (rfidHandler != null) rfidHandler.performInventory();
    }

    private void clearTagData() {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            tagSet.clear();
            tagList.clear();
            if (tagAdapter != null) {
                tagAdapter.notifyDataSetChanged();
            }
        });
    }

    public void scanCode(View view) {
        /**
         * Initiates barcode scanning when the scan button is pressed.
         * @param view The view that triggered this method.
         */
        if (rfidHandler != null) rfidHandler.scanCode();
    }

    public void testFunction(View view) {
        /**
         * Runs a test function when the test button is pressed.
         * @param view The view that triggered this method.
         */
        if (rfidHandler != null) rfidHandler.testFunction();
    }

    public void StopInventory(View view) {
        /**
         * Stops RFID inventory when the stop button is pressed.
         * @param view The view that triggered this method.
         */
        toggleInventoryButtons(false);
        if (rfidHandler != null) rfidHandler.stopInventory();
    }

    @SuppressLint("SetTextI18n")
    public void handleTagdata(TagData[] tagData) {
        /**
         * Handles new tag data received from the RFID reader.
         * @param tagData Array of TagData objects.
         */
        if (tagData == null || tagData.length == 0) return;

        final ArrayList<String> newTags = collectNewTags(tagData);
        if (!newTags.isEmpty()) {
            final int totalUniqueTags = tagSet.size();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                updateTagListUI(newTags);
                updateStatusTextWithUniqueTags(totalUniqueTags);
            });
        }
    }

    private ArrayList<String> collectNewTags(TagData[] tagData) {
        ArrayList<String> newTags = new ArrayList<>();
        for (TagData tag : tagData) {
            if (tag == null) continue;
            String tagId = tag.getTagID();
            if (tagId != null && !tagSet.contains(tagId)) {
                tagSet.add(tagId);
                newTags.add(tagId + " (RSSI: " + tag.getPeakRSSI() + ")");
            }
        }
        return newTags;
    }

    private void updateTagListUI(ArrayList<String> newTags) {
        tagList.addAll(0, newTags);
        if (tagAdapter != null) {
            tagAdapter.notifyDataSetChanged();
        }
    }

    private void updateStatusTextWithUniqueTags(int totalUniqueTags) {
        if (statusTextViewRFID != null && statusTextViewRFID.getText() != null) {
            String statusStr = statusTextViewRFID.getText().toString();
            if (statusStr.contains(getString(R.string.connected))) {
                String[] parts = statusStr.split("\n");
                String currentStatus = parts.length > 0 ? parts[0] : statusStr;
                statusTextViewRFID.setText(currentStatus + "\n" + getString(R.string.unique_tags, totalUniqueTags));
            }
        }
    }

    public void handleTriggerPress(boolean pressed) {
        /**
         * Handles trigger press events from the RFID reader.
         * @param pressed True if trigger is pressed, false otherwise.
         */
        toggleInventoryButtons(pressed);
        if (pressed) {
            clearTagData();
            if (rfidHandler != null) rfidHandler.performInventory();
        } else {
            if (rfidHandler != null) rfidHandler.stopInventory();
        }
    }

    @Override
    public void barcodeData(String val) {
        /**
         * Displays barcode data received from the scanner.
         * @param val The barcode value.
         */
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (scanResultText != null) {
                scanResultText.setText(getString(R.string.scan_result_label, val != null ? val : ""));
            }
        });
    }

    @Override
    public void sendToast(String val) {
        /**
         * Shows a toast message on the UI.
         * @param val The message to display.
         */
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            Toast.makeText(MainActivity.this, val, Toast.LENGTH_SHORT).show();
        });
    }
}
