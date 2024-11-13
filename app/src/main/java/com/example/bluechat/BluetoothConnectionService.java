package com.example.bluechat;

import static com.example.bluechat.MainActivity.itemBleutooth;
import static com.example.bluechat.MainActivity.toolbar;
import static com.example.bluechat.MainActivity.is_connected;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import java.io.InputStream;
import java.io.OutputStream;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

//import com.github.pires.obd.commands.protocol.EchoOffCommand;
//import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
//import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
//import com.github.pires.obd.commands.protocol.TimeoutCommand;
//import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
//import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";

    private static final String appName = "MYAPP";

    //private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;



    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context){
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    // This thread runs while listening for incoming connections. It behaves like a server-side client. It runs until a connection is accepted (or until cancelled).
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);

            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }
            mmServerSocket = tmp;
        }

        @SuppressLint("MissingPermission")
        public void run(){
            Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;
            // This is a blocking call and will only return on a successful connection or an exception
            try {
                Log.d(TAG, "run: RFCOM server socket start....");
                socket = mmServerSocket.accept();
                Log.d(TAG, "run: RFCOM server socket accepted connection.");

            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }

            if(socket != null){
                // A connection was accepted. Perform work associated with the connection in a separate thread.
                mmDevice = socket.getRemoteDevice();
                ((MainActivity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toolbar.setTitle("BlueChat - " + mmDevice.getName());
                        if(itemBleutooth != null) {
                            itemBleutooth.setIcon(R.drawable.ic_deconnection);
                        }
                        is_connected = true;
                    }
                });
                connected(socket,mmDevice);
            }
            Log.i(TAG, "END mAcceptThread ");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
            ((MainActivity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toolbar.setTitle("BlueChat - Not Connected");
                    if(itemBleutooth != null) {
                        itemBleutooth.setIcon(R.drawable.ic_connection);
                    }
                    is_connected = false;
                }
            });
        }

    }

    // This thread runs while attempting to make an outgoing connection with a device. It runs straight through; the connection either succeeds or fails.
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        // take the BluetoothDevice and UUID from the device that we want to connect to
        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
//            deviceUUID = UUID
//                    .fromString("00001101-0000-1000-8000-00805F9B34FB");
            deviceUUID = uuid;
        }

        @SuppressLint("MissingPermission")
        public void run(){
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectThread ");


            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            // given BluetoothDevice, MY_UUID_INSECURE

            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: " + MY_UUID_INSECURE);
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            // Always cancel discovery because it will slow down a connection
            //BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

            // Make a connection to the BluetoothSocket
            // successful connection or an exception
            try {
                Log.d(TAG, "run: ConnectThread trying to connect. :" + mmSocket);
                mmSocket.connect();
                Log.d(TAG, "run: ConnectThread connected.");
            } catch (IOException e) {
                try {
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                Log.e(TAG, "ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE + e.getMessage());

                // Dismiss the progressdialog when connection is established
                try{
                    mProgressDialog.dismiss();
                }catch (NullPointerException e2){
                    Log.e(TAG, "ConnectedThread: NullPointerException: " + e.getMessage());
                }
                BluetoothConnectionService.this.cancel();
                return;
            }

            ((MainActivity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toolbar.setTitle("BlueChat - " + mmDevice.getName());
                    if(itemBleutooth != null) {
                        itemBleutooth.setIcon(R.drawable.ic_deconnection);
                    }
                    is_connected = true;
                }
            });
            connected(mmSocket,mmDevice);

        }

        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in ConnectThread failed. " + e.getMessage());
            }
            ((MainActivity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toolbar.setTitle("BlueChat - Not Connected");
                    if(itemBleutooth != null) {
                        itemBleutooth.setIcon(R.drawable.ic_connection);
                    }
                    is_connected = false;
                }
            });
        }
    }

    // Start the chat service. Specifically start AcceptThread to begin a session in listening (server) mode. Called by the Activity onResume()
    public synchronized void start(){
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start(); // start from the thread class
        }

    }

    // AcceptThread starts and sits waiting for a connection.
    // Then ConnectThread starts and attempts to make a connection with the other devices AcceptThread.
    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: Started.");

        //initprogress dialog
        mProgressDialog = ProgressDialog.show(mContext,"Connecting Bluetooth","Please Wait...",true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start(); // start from the thread class
    }




    // ConnectedThread is responsible for maintaining the BTConnection, sending the data, and receiving incoming data through input/output streams respectively.
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Dismiss the progressdialog when connection is established
            try{
                mProgressDialog.dismiss();
            }catch (NullPointerException e){
                Log.e(TAG, "ConnectedThread: NullPointerException: " + e.getMessage());
            }

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: err on get input/output stream" + e.getMessage());
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }

        public void run(){
            byte[] buffer = new byte[1024]; // buffer store for the stream
            int bytes; // bytes returned from read()

            // keep listening to the InputStream until an exception occurs
            while(true){
                // Read from the InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);
                    // Utilisez la fonction addMessage
                    ((MainActivity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((MainActivity) mContext).addMessage(incomingMessage);
                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage());
                    if(Objects.equals(e.getMessage(), "bt socket closed, read return: -1")){
                        BluetoothConnectionService.this.cancel();
                    }
                    break;
                }

            }
        }

        public void write(byte[] bytes){
            String text = new String(bytes, 0, bytes.length);
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
            }
        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in ConnectedThread failed. " + e.getMessage());
            }
            ((MainActivity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toolbar.setTitle("BlueChat - Not Connected");
                    if(itemBleutooth != null) {
                        itemBleutooth.setIcon(R.drawable.ic_connection);
                    }
                    is_connected = false;
                }
            });
        }
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    // Write to the ConnectedThread in an unsynchronized manner
    public void write(byte[] out){
        // Create temporary object
        ConnectedThread r;

        // Synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write Called.");
        r = mConnectedThread;
        // Perform the write
        r.write(out);
    }

    public void cancel(){
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        start();
    }

}
