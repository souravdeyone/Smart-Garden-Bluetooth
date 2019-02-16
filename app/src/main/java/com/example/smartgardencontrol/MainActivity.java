package com.example.smartgardencontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Button btnPaired;
    ListView deviceList;

    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    private static String EXTRA_ADDRESS = "device address";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnPaired = (Button)findViewById(R.id.button);
        deviceList = (ListView)findViewById(R.id.listView);

        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (myBluetooth ==null){

            Toast.makeText(getApplicationContext(), "Device Bluetooth not available", Toast.LENGTH_LONG).show();
            finish();

        }
        else if(myBluetooth.isEnabled() == false){

            //Ask to enable bluetooth
            Intent turnOnBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnBT, 1);
        }

        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pairedDevicesList();
            }
        });
    }

    private void pairedDevicesList(){

        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size()>0){
            for (BluetoothDevice bt : pairedDevices){
                list.add(bt.getName() +"\n"+ bt.getAddress());
            }
        }

        else{
            Toast.makeText(getApplicationContext(), "No paired bluetooth devices found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        deviceList.setAdapter(adapter);

        //deviceList.setOnItemClickListener(myListClickListener);
        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
                String info = ((TextView) v).getText().toString();
                String address = info.substring(info.length()-17);
                Intent i = new Intent(MainActivity.this, gardenControl.class);
                i.putExtra(EXTRA_ADDRESS,  address);
                startActivity(i);

            }
        });
    }

}
