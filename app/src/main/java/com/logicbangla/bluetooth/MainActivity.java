package com.logicbangla.bluetooth;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static com.logicbangla.bluetooth.Main2Activity.EXTRA_DEVICE_ADDRESS;

public class MainActivity extends AppCompatActivity {

    // Debugging for LOGCAT
    private static final String TAG = "MainActivity";

    Button btnOn, btnOff;
    TextView textView1, textView2;

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnOn = findViewById(R.id.buttonON);
        btnOff = findViewById(R.id.buttonOFF);
        textView1 = findViewById(R.id.textConnect);
        textView2 = findViewById(R.id.textDevice);

        textView1.setText("Not Connected");

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();
        //Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        //startActivityForResult(turnOn, 0);

        // Set up onClick listeners for buttons to send 1 or 0 to turn on/off LED
        btnOff.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("0");    // Send "0" via Bluetooth
                Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
            }
        });

        btnOn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("1");    // Send "1" via Bluetooth
                Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        textView1 = findViewById(R.id.textConnect);

        //Get MAC address from DeviceListActivity via intent
        //Intent intent = getIntent();
        try {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                address = bundle.getString("id");
            }

            //Get the MAC address from the DeviceListActivty via EXTRA
            //address = intent.getStringExtra("id");

            //create device and set the MAC address
            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            try {
                btSocket = createBluetoothSocket(device);
                Toast.makeText(getBaseContext(), "Socket is created", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
            }

            // Establish the Bluetooth socket connection.
            try {
                btSocket.connect();
                Toast.makeText(getBaseContext(), "Connection established", Toast.LENGTH_SHORT).show();
                textView1.setText("Connected");
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    //insert code to deal with this
                }
            }
            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();

            //I send a character when resuming.beginning transmission to check device is connected
            //If it is not an exception will be thrown in the write method and finish() will be called
            //mConnectedThread.write("x");
        } catch (Exception e) {
            Log.i(TAG, "Error: " + e.getMessage());
            Toast.makeText(getBaseContext(), "Address is not found" + "\n" + "Connect to a paired device", Toast.LENGTH_LONG).show();
            textView1.setText("Not Connected");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Toast.makeText(getBaseContext(), "MainActivity is paused", Toast.LENGTH_SHORT).show();
        if (address != null) {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                btSocket.close();
            } catch (IOException e2) {
                //insert code to deal with this
            }
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        try {
            if (btAdapter == null) {
                Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
            } else {
                if (!btAdapter.isEnabled()) {

                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 0);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void btnConnect(View view) {
        Intent intent = new Intent(MainActivity.this, Main2Activity.class);
        startActivity(intent);
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    //String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    //bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }
}