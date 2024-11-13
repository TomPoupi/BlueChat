package com.example.bluechat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;


import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;
    public static final int BT_DEVICE_LIST_ACTIVITY_REQUEST_CODE = 42;
    public static final int BT_REQUEST_FOR_CONNECTION = 1;
    public static final int BT_REQUEST_FOR_SOCKET = 2;
    private static final String TAG = "MainActivity";

    public static boolean is_connected = false;

    public static Toolbar toolbar;
    public static MenuItem itemBleutooth;


    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mBTDevice;

    //private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");




    BluetoothConnectionService mBluetoothConnection;

    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<String> messages;

    EditText etSend;
    Button btnSend;



    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        if(itemBleutooth != null){
                            itemBleutooth.setIcon(R.drawable.ic_connection);
                        }
                        is_connected = false;
                        if(mBluetoothConnection != null){
                            mBluetoothConnection.cancel();
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    mBTDevice = mDevice;
                    onActivityResult(BT_REQUEST_FOR_SOCKET, RESULT_OK, null);
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }


            }
        }
    };

    // onActivity Result
    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BT_REQUEST_FOR_CONNECTION && resultCode == RESULT_OK) {

            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);

            Intent BtDeviceActivity = new Intent(MainActivity.this, BtDeviceListActivity.class);
            startActivityForResult(BtDeviceActivity, BT_DEVICE_LIST_ACTIVITY_REQUEST_CODE);
        }

        if (BT_DEVICE_LIST_ACTIVITY_REQUEST_CODE == requestCode && resultCode == RESULT_OK) {
            BluetoothDevice device = data.getParcelableExtra("result");
            String deviceName = "";
            String deviceAddress= "";
            if (device != null) {
                // Utilisez l'objet BluetoothDevice ici
                deviceName = device.getName();
                deviceAddress = device.getAddress();
                // Faites quelque chose avec le device
            }
            // Show a toast with the device name
            Toast.makeText(this, "Device selected: " + deviceName + " - " + deviceAddress, Toast.LENGTH_SHORT).show();

            // Start the connection with the device
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
                if(device != null) {
                    Log.d(TAG, "Trying to pair with " + device.getName());
                    device.createBond();
                    mBTDevice = device;
                    if(device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        Log.d(TAG, "Device already bonded");
                        onActivityResult(BT_REQUEST_FOR_SOCKET, RESULT_OK, null);
                    }
                }
            }

        }

        if(requestCode == BT_REQUEST_FOR_SOCKET && resultCode == RESULT_OK){
            // Start the socket connection
            if(mBTDevice != null){
                Log.d(TAG, "Starting the socket connection with " + mBTDevice.getName());
                mBluetoothConnection.startClient(mBTDevice,MY_UUID_INSECURE);
            }

            itemBleutooth.setIcon(R.drawable.ic_deconnection);
            is_connected = true;

        }

    }





    @SuppressLint({"MissingPermission", "MissingInflatedId"})
    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //--------------------- PERMISSION ------------------------
        checkBTPermissions();
        //--------------------------------------------------------

        //----------------- INIT bluetooth adapter --------------
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        //-------------------------------------------------------

        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        btnSend = (Button) findViewById(R.id.btnSend);
        etSend = (EditText) findViewById(R.id.editText);

        if(!mBluetoothAdapter.isEnabled()){
            // turn on bluetooth
            Toast.makeText(this, "Bluetooth turn on", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,BT_REQUEST_FOR_CONNECTION);
        }

        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, BTIntent);

        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        if(mBluetoothAdapter.isEnabled()){
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }


        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = etSend.getText().toString().getBytes();
                mBluetoothConnection.write(bytes);
                addMessage("Me: " + etSend.getText().toString());
                etSend.setText("");
            }
        });

    }

    void addMessage(String message) {
        Log.d(TAG, "addMessage: " + message);
        messages.add(message);
        messageAdapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);
    }


    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu_item,menu);
        itemBleutooth = menu.findItem(R.id.action_connection);
        return true;

    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @SuppressLint({"NonConstantResourceId", "MissingPermission"})
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connection:
                if(!is_connected){
                    if(!mBluetoothAdapter.isEnabled()) {
                        // turn on bluetooth
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent,BT_REQUEST_FOR_CONNECTION);

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(mBroadcastReceiver1, BTIntent);
                    } else {
                        Intent BtDeviceActivity = new Intent(MainActivity.this, BtDeviceListActivity.class);
                        startActivityForResult(BtDeviceActivity, BT_DEVICE_LIST_ACTIVITY_REQUEST_CODE);
                    }
                } else{
                    Toast.makeText(this, "Deconnection", Toast.LENGTH_SHORT).show();
                    item.setIcon(R.drawable.ic_connection);
                    is_connected = false;
                    mBluetoothConnection.cancel();
                }


                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

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
        ){
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
        //--------------------------------------------------------
    }
}