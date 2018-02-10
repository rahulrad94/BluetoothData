package com.example.rahul.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.logging.Handler;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_ENABLE_BT = 1;
    private final UUID rp_uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter mBluetoothAdapter;
    String TAG = "Bluetooth";
    ConnectedThread mConnectedThread;

   private Handler mHandler;

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
    }

    //UI
    private Button scanButton,readButton;
    private TextView s,t,h,g5,g3,g2,l,g9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanButton = (Button) findViewById(R.id.scan);
        readButton = (Button) findViewById(R.id.read);
        s = (TextView) findViewById(R.id.sound);
        t = (TextView) findViewById(R.id.temp);
        h = (TextView) findViewById(R.id.hum);
        g5 = (TextView) findViewById(R.id.gas5);
        g3 = (TextView) findViewById(R.id.gas3);
        g2 = (TextView) findViewById(R.id.gas2);
        l = (TextView) findViewById(R.id.light);
        g9 = (TextView) findViewById(R.id.gas9);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter==null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_NAME_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    // Create a BroadcastReceiver for ACTION_NAME_CHANGED.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                String str = deviceName + "   " + deviceHardwareAddress;

                Log.d(TAG, str);

                if(deviceName!=null && deviceName.equals("raspberrypi")){
                    //new ConnectThread(device).start();
                    // write start here to connect thread run
                    Log.d(TAG, "RaspBerry Pie Data Transfer Started");
                    new ConnectThread(device).start();
                }
            }
        }
    };

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(rp_uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            //manageMyConnectedSocket(mmSocket);

            // my code
            Log.d(TAG,"ConnectThread Success");
            //ConnectedThread mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private  final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.d("Bluetooth", "Error occurred when creating input stream");
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d("Bluetooth", "Error occurred when creating output stream");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    String TOKEN_ = new String(mmBuffer, "UTF-8");
                    final String inComing = TOKEN_.substring(0, numBytes);
                    Log.d("Data coming:  ", inComing);

                    try {

                        final JSONObject mainObject = new JSONObject(inComing);
                        final int o1 = mainObject.getInt("sound");
                        final double o2 = mainObject.getDouble("temp");
                        final double o3 = mainObject.getDouble("hum");
                        final int o4 = mainObject.getInt("gas_MQ5");
                        final int o5 = mainObject.getInt("gas_MQ3");
                        final int o6 = mainObject.getInt("gas_MQ2");
                        final double o7 = mainObject.getDouble("light");
                        final int o8 = mainObject.getInt("gas_MQ9");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                s.setText("" + o1);
                                t.setText("" + o2);
                                h.setText("" + o3);
                                g5.setText("" + o4);
                                g3.setText("" + o5);
                                g2.setText("" + o6);
                                l.setText("" + o7);
                                g9.setText("" + o8);
                            }
                        });

                    } catch (final JSONException e) {
                        Log.e(TAG, "Json parsing error: " + e.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        "Json parsing error: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                    /*
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1,
                            mmBuffer);


                    readMsg.sendToTarget();
                    */

                } catch (IOException e) {
                    Log.d("Bluetooth", "Input stream was disconnected");
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                /*
                Message writtenMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
                */

            } catch (IOException e) {
                Log.d("Bluetooth", "Error occurred when sending data");

                // Send a failure message back to the activity.
                /*
                Message writeErrorMsg =
                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
                */
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.i("Bluetooth", "Could not close the connect socket");
            }
        }

    }

    public void scanHandler(View v){
        if(mBluetoothAdapter.startDiscovery()){
            Log.d(TAG, "Discovery Started!!");
        }
    }

    public void readHandler(View v){
        mConnectedThread.write("data".getBytes());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }
}
// runOnUIThread -- problems when trying to print it to UI