package com.example.smartgardencontrol;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class gardenControl extends AppCompatActivity {

    Button on, off;
    TextView txtString;
    String address = null;
    BluetoothAdapter myBluetooth;
    BluetoothSocket btSocket;
    Handler bluetoothIn;

    private boolean isConnected = false;
    private ProgressDialog progress;
    private StringBuilder recDataString = new StringBuilder();
    private AsyncTask mConnectBT;
    private ConnectedThread mConnectedThread;
    private BluetoothAdapter btAdapter = null;


    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    final int handlerState = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garden_control);

        on = (Button)findViewById(R.id.on_btn);
        off = (Button)findViewById(R.id.off_btn);
        txtString = (TextView)findViewById(R.id.textView2);


        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg){
                if (msg.what == handlerState){
                    String readMessage  = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("~");
                    if (endOfLineIndex >0){
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);
                        txtString.setText("Data Received =  "+ dataInPrint);
                    }
                }
            }

        };


        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        on.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                turnOnPump();
            }
        });
        off.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                turnOffPump();
            }
        });

    }


    private void turnOffPump(){
        mConnectedThread.write("abc");
        Toast.makeText(getApplicationContext(), "Pump Turned Off", Toast.LENGTH_LONG).show();
    }


    private void turnOnPump(){
        mConnectedThread.write("def");
        Toast.makeText(getApplicationContext(), "Pump Turned On", Toast.LENGTH_LONG).show();
    }

    private void updateDisplay(){
        if (btSocket != null){
            try{
                int bytes = btSocket.getInputStream().read();

            }
            catch (IOException e){
                Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
            }
        }
    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(myUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume(){
        super.onResume();
        Intent newInt = getIntent();
        address = ((Intent) newInt).getStringExtra("ADDRESS");
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        mConnectedThread.write("x");

    }

    @Override
    public void onPause() {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }


    private class ConnectedThread extends Thread{

        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //Creating of the connect thread
        public ConnectedThread(BluetoothSocket socket){
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e){

                Log.e("Error in ConnectedThd", "Related to getInputStream()");

            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[256];
            int bytes;
            //Looping to keep searching for new messages from the garden!
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(String input){
            byte[] msgBuffer  = input.getBytes();
            try{
                mmOutStream.write(msgBuffer);
            }
            catch (IOException e){
                Toast.makeText(getApplicationContext(), "Failed to write to garden", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
