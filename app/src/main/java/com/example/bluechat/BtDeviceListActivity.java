package com.example.bluechat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Set;

public class BtDeviceListActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    LinearLayout buttonContainerPaired, buttonContainerSearched;
    BluetoothAdapter mBluetoothAdapter;
    private static final String TAG = "BtDeviceListActivity";


    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private final BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    createDeviceButton(device,"searched");
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        // Initialiser les LinearLayouts
        buttonContainerPaired = findViewById(R.id.buttonContainerPaired);
        buttonContainerSearched = findViewById(R.id.buttonContainerSearched);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non supporté sur cet appareil", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth désactivé, veuillez l'activer", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkBTPermissions();

        // Enregistrer le BroadcastReceiver pour la découverte des appareils
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver3, filter);

        // Démarrer la découverte des appareils
        startDiscovery();
    }

    @SuppressLint("MissingPermission")
    private void startDiscovery() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
        if(buttonContainerPaired != null){
            buttonContainerPaired.removeAllViews();
        }
        if(buttonContainerSearched != null){
            buttonContainerSearched.removeAllViews();
        }
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                createDeviceButton(device, "paired");
            }
        }
    }





    // fonction qui crée un bouton pour un appareil Bluetooth
    //-------------------------------------------------------
    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void createDeviceButton(final BluetoothDevice device, String type) {
        // Create a new button
        Button deviceButton = new Button(BtDeviceListActivity.this);
        deviceButton.setText(device.getName());
        deviceButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Add the button to the container
        if(type.equals("paired")){
            buttonContainerPaired.addView(deviceButton);
        } else if(type.equals("searched")) {
            buttonContainerSearched.addView(deviceButton);
        } else{
            return;
        }

        // Set the BluetoothDevice as a tag for the button
        deviceButton.setTag(device);

        // Set an OnClickListener for the button
        deviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //BluetoothDevice device = (BluetoothDevice) v.getTag();
                mBluetoothAdapter.cancelDiscovery();
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result",device);
                setResult(BtDeviceListActivity.RESULT_OK,returnIntent);
                finish();
            }

        });

    }
    //-------------------------------------------------------

    private void checkBTPermissions() {
        //--------------------- PERMISSION ------------------------
        // Vérifiez et demandez les permissions Bluetooth si nécessaire
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_PRIVILEGED) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_PRIVILEGED,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    },
                    REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }
    //--------------------------------------------------------
}
