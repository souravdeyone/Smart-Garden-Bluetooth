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

    Button on, off, discnt, abt;
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

    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    final int handlerState = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garden_control);

        Intent newInt = getIntent();
        address = ((Intent) newInt).getStringExtra("ADDRESS");
        on = (Button)findViewById(R.id.on_btn);
        off = (Button)findViewById(R.id.off_btn);
        discnt = (Button)findViewById(R.id.dis_btn);
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

        mConnectBT = new ConnectBT().execute();
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

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
        discnt.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //disconnect();
            }
        });
    }


    private void turnOffPump(){
        mConnectedThread.write("0");
        Toast.makeText(getApplicationContext(), "Pump Turned Off", Toast.LENGTH_LONG).show();
    }


    private void turnOnPump(){
        mConnectedThread.write("1");
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


    private class ConnectBT extends AsyncTask<Void, Void, Void> {

        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute(){
            progress = ProgressDialog.show(getApplicationContext(), "Connecting...", "Please wait");

        }

        @Override
        protected Void doInBackground(Void... devices) {

            try{
                if (btSocket==null || !isConnected){
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            }
            catch(IOException e){
                ConnectSuccess = false;
            }

            return null;

        }

        @Override
        protected void onPostExecute(Void result){
            super.onPostExecute(result);
            if (!ConnectSuccess){
                Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_LONG).show();
                finish();
            }
            else{
                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
                isConnected = true;
            }
            progress.dismiss();
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

            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[256];
            int bytes;
            //Looping to keep searhing for new messages from the garden!
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
