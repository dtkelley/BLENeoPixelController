package com.mascorp.bleneopixelcontroller;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.*;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.ColorListener;
import java.nio.charset.Charset;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private TextView messages;
    private TextView textView;
    private LinearLayout linearLayout;
    private SeekBar slider;
    private int[] colors;
    private ColorPickerView colorPickerView;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private BluetoothAdapter adapter;
    private BluetoothManager manager;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;

    // Initialize layout and prompt BLE scanning
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize bluetooth properties
        textView = findViewById(R.id.textView);
        linearLayout = findViewById(R.id.linearLayout);
        messages = findViewById(R.id.textViewScroll);
        slider = findViewById(R.id.brightnessSlider);
        manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();
        bluetoothLeScanner = adapter.getBluetoothLeScanner();

        if (adapter != null && !adapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        // Prompt user to enable location access
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        // Set up color wheel for user interaction
        colorPickerView = findViewById(R.id.colorPickerView);
        colorPickerView.setPaletteDrawable(ContextCompat.getDrawable(this, R.drawable.palette));
        colorPickerView.setColorListener(new ColorListener() {
            // Get RGB colors of selection and send data via String to BLE device
            @Override
            public void onColorSelected(int color) {
                linearLayout.setBackgroundColor(color);

                String colorHtml = "#" + colorPickerView.getColorHtml();
                textView.setText(colorHtml);
                colors = colorPickerView.getColorRGB();
                if (colors != null) {
                    sendMessage("c," + colors[0] + "," + colors[1] + "," + colors[2]);
                }
            }
        });

        // Get progress bar location to send to BLE device for NeoPixel brightness
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sendMessage("b," + i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    // Begin scanning for BLE device after Activity initialized
    @Override
    protected void onResume() {
        super.onResume();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.startScan(callback);
            }
        });
        writeLine("Scanning for devices...");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
            tx = null;
            rx = null;
        }
    }

    // Send string to Arduino via BLE
    private void sendMessage(String currentColor) {
        if (tx == null || currentColor == null || currentColor.isEmpty()) {
            return;
        }

        // Convert string to be sent over BLE
        tx.setValue(currentColor.getBytes(Charset.forName("UTF-8")));

        if (gatt.writeCharacteristic(tx)) {
            writeLine("Sent: " + currentColor);
        } else {
            writeLine("Couldn't write TX characteristic!");
        }

    }

    // Turn lights on/off
    public void toggleLights(View view) {
        sendMessage("toggle");
    }

    // Get values of already-turned-on NeoPixel strip and re-initialize app values
    // Re-initialization happens in updateValues function
    public void recoverValues(View view) {
        sendMessage("get");
    }

    // Control which BLE device to connect to
    private ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            writeLine("Device Name: " + result.getDevice().getName());

            // Connect to device if the device name is 'Arduino'
            // Can be changed to connect to a particular address or UUID
            if (gatt == null && result.getDevice().getName() != null && result.getDevice().getName().equals("Arduino")) {
                gatt = result.getDevice().connectGatt(getApplicationContext(), false, gattCallback);
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        bluetoothLeScanner.stopScan(callback);
                    }
                });
            }
        }
    };

    // Handle outcome of coarse location access prompt
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    // Write messages to screen
    private void writeLine(final CharSequence text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messages.append(text);
                messages.append("\n");
            }
        });
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            // Discover services once the Android and BLE device(s) are connected
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                writeLine("Connected!");
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                writeLine("Disconnected!");
            } else {
                writeLine("New State: " + newState);
            }
        }

        // Initialize BluetoothGattCharacteristic for reading/writing
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeLine("Service discovery completed");
            } else {
                writeLine("Service discovery failed with status: " + status);
            }

            tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
            rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);

            if (!gatt.setCharacteristicNotification(rx, true)) {
                writeLine("Couldn't set notifications for RX characteristic!");
            }
            if (rx.getDescriptor(CLIENT_UUID) != null) {
                BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(desc)) {
                    writeLine("Couldn't write RX client descriptor value!");
                }
            } else {
                writeLine("Couldn't get RX client descriptor!");
            }
        }

        // On message received from BLE Device
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            updateValues(characteristic.getStringValue(0).split(","));
        }
    };

    // Get RGB & Brightness values from BLE Device and re-initialize app values
    private void updateValues(String[] values) {
        int red = Integer.parseInt(values[0]);
        int green = Integer.parseInt(values[1]);
        int blue = Integer.parseInt(values[2]);
        int brightness = Integer.parseInt(values[3]);
        slider.setProgress(brightness);
        final String colorHtml = String.format("#%02X%02X%02X", red, green, blue);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(colorHtml);
                linearLayout.setBackgroundColor(Color.parseColor(colorHtml));
            }
        });
    }
}
