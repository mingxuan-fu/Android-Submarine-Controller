package com.example.fit3146_controller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ImageButton forwardBtn;
    private ImageButton rLeftBtn;
    private ImageButton rRightBtn;
    private ImageButton floatBtn;
    private TextView statusTv;
    private BluetoothDevice selectedBtDevice;
    private BluetoothGatt selectedBtGatt;
    private final String uuidWrite = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private final String uuidService = "0000ffe0-0000-1000-8000-00805f9b34fb";

    void setControlEnabled(boolean status) {
        forwardBtn.setEnabled(status);
        rLeftBtn.setEnabled(status);
        rRightBtn.setEnabled(status);
        floatBtn.setEnabled(status);
    }

    boolean checkScanPerm() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    void reqScanPerm() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 0);
    }

    boolean checkLocPerm() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    void reqLocPerm() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        forwardBtn = findViewById(R.id.forwardBtn);
        rLeftBtn = findViewById(R.id.rLeftBtn);
        rRightBtn = findViewById(R.id.rRightBtn);
        floatBtn = findViewById(R.id.floatBtn);
        statusTv = findViewById(R.id.statusTv);
        setControlEnabled(false);

        BluetoothAdapter btAdapter = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        if (!checkScanPerm()) reqScanPerm();
        if (!checkLocPerm()) reqLocPerm();
        BluetoothLeScanner scanner = btAdapter.getBluetoothLeScanner();
        ScanSettings settings = (new ScanSettings.Builder()).setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            scanner.startScan(null, settings, new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    if (result.getDevice().getName() != null && result.getDevice().getName().contentEquals("ArduinoBT")) {
                        selectedBtDevice = result.getDevice();
                        statusTv.setText(R.string.foundInd);
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        selectedBtGatt = selectedBtDevice.connectGatt(getApplicationContext(), true, new BluetoothGattCallback() {
                            @Override
                            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                                        Log.w("BluetoothGattCallback", "Successfully connected to device");
                                        selectedBtGatt = gatt;
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                                return;
                                            }
                                            selectedBtGatt.discoverServices();
                                        });
                                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                        Log.w("BluetoothGattCallback", "Successfully disconnected from device");
                                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                            return;
                                        }
                                        gatt.close();
                                    }
                                } else {
                                    Log.w("BluetoothGattCallback", "Error encountered for device Disconnecting...");
                                }
                            }

                            @Override
                            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                                statusTv.setText(R.string.connectedInd);
                                setControlEnabled(true);
                                printGattTable();
                            }

                            private void printGattTable() {
                                List<BluetoothGattService> services = selectedBtGatt.getServices();
                                List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
                                services.forEach(service -> {
                                    Log.w("BluetoothGattCallback", service.getUuid().toString());
                                    characteristics.addAll(service.getCharacteristics());
                                });
                                List<String> uuids = new ArrayList<>();
                                characteristics.forEach(characteristic -> uuids.add(String.valueOf(characteristic.getUuid())));

                                String table = String.join("\n", uuids);
                                Log.w("BluetoothGattCallback", table);
                            }

                        });
                        selectedBtGatt.connect();
                        scanner.stopScan(this);
                    }
                }
            });
        }
        statusTv.setText(R.string.searchingInd);


        forwardBtn.setOnTouchListener((view, motionEvent) -> {
            BluetoothGattCharacteristic characteristic = selectedBtGatt.getService(UUID.fromString(uuidService)).getCharacteristic(UUID.fromString(uuidWrite));
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    characteristic.setValue("14");
                    selectedBtGatt.writeCharacteristic(characteristic);
                } else if (motionEvent.getActionMasked() == MotionEvent.ACTION_UP) {
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    characteristic.setValue("05");
                    selectedBtGatt.writeCharacteristic(characteristic);
                }
            }
            return false;
        });

        rLeftBtn.setOnTouchListener((view, motionEvent) -> {
            BluetoothGattCharacteristic characteristic = selectedBtGatt.getService(UUID.fromString(uuidService)).getCharacteristic(UUID.fromString(uuidWrite));
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    characteristic.setValue("1");
                    selectedBtGatt.writeCharacteristic(characteristic);
                } else if (motionEvent.getActionMasked() == MotionEvent.ACTION_UP) {
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    characteristic.setValue("0");
                    selectedBtGatt.writeCharacteristic(characteristic);
                }
            }
            return false;
        });

        rRightBtn.setOnTouchListener((view, motionEvent) -> {
            BluetoothGattCharacteristic characteristic = selectedBtGatt.getService(UUID.fromString(uuidService)).getCharacteristic(UUID.fromString(uuidWrite));
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    characteristic.setValue("4");
                    selectedBtGatt.writeCharacteristic(characteristic);
                } else if (motionEvent.getActionMasked() == MotionEvent.ACTION_UP) {
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    characteristic.setValue("5");
                    selectedBtGatt.writeCharacteristic(characteristic);
                }
            }
            return false;
        });

        floatBtn.setOnTouchListener((view, motionEvent) -> {
            BluetoothGattCharacteristic characteristic = selectedBtGatt.getService(UUID.fromString(uuidService)).getCharacteristic(UUID.fromString(uuidWrite));
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    characteristic.setValue("2");
                    selectedBtGatt.writeCharacteristic(characteristic);
                } else if (motionEvent.getActionMasked() == MotionEvent.ACTION_UP) {
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    characteristic.setValue("3");
                    selectedBtGatt.writeCharacteristic(characteristic);
                }
            }
            return false;
        });

    }

    @Override
    protected void onDestroy() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (selectedBtGatt != null) {
            selectedBtGatt.disconnect();
        }
        super.onDestroy();
    }
}