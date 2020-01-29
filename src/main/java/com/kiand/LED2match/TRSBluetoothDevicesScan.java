package com.kiand.LED2match;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

public class TRSBluetoothDevicesScan extends ListActivity {

    private Handler lclHandler;
    private UsbCOMMsService lclUsbServiceInstance;
    boolean mBound = false;
    final Context context = this;
    public static final String BT_PREFS = "bluetooth_status";
    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();
    private String deviceAddress;
    private Set<BluetoothDevice> pairedDevices;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            UsbCOMMsService.UsbBinder binder = (UsbCOMMsService.UsbBinder) service;
            lclUsbServiceInstance = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public String getSystemTime() {
        Date currentTime = Calendar.getInstance().getTime();
        String sTimeNow = DateFormat.format("HH:mm:ss", currentTime).toString();
        return sTimeNow;
    }

    protected void onResume()
    {
        super.onResume();
        //Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();
        if (!mBound) {
            Intent intent = new Intent(this, UsbCOMMsService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            //Toast.makeText(this.getBaseContext(),"Service bound (onResume)", Toast.LENGTH_SHORT).show();
            mBound = true;
        }
        String sNow = getSystemTime();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to UsbService
        //Toast.makeText(this.getBaseContext(),"Activity started", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, UsbCOMMsService.class);
        bindService (intent, mConnection, Context.BIND_AUTO_CREATE);
        //Toast.makeText(this.getBaseContext(),"Service bound (onStart)", Toast.LENGTH_SHORT).show();
        /*String sCommand = "J" + LightAdjustments.sNewLine; */
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
        mBound = false;
        //Toast.makeText(this.getBaseContext(),"Activity stopped", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"Service unbound", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_devices);
        lclHandler = new Handler();

        pairedDevices = _bluetooth.getBondedDevices();
        //lv = (ListView)findViewById(R.id.listView);

        ArrayList list = new ArrayList();
        for(BluetoothDevice bt : pairedDevices) list.add(bt.getName() + System.lineSeparator() + bt.getAddress());
        Toast.makeText(getApplicationContext(), "Showing Paired Devices",Toast.LENGTH_SHORT).show();
        final ArrayAdapter adapter = new  ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        setListAdapter(adapter);

    }
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        if (_bluetooth.isDiscovering()) {
            _bluetooth.cancelDiscovery();
        }

        String selectedItem = (String) getListView().getItemAtPosition(position);
        String[] arrSelectedItem = selectedItem.split(System.lineSeparator());
        BluetoothDevice device = _bluetooth.getRemoteDevice(arrSelectedItem[1]);

        Intent result = new Intent();
        result.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        setResult(RESULT_OK, result);
        deviceAddress = device.getAddress();

        SharedPreferences prefs = getSharedPreferences(BT_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("device_btaddress", deviceAddress);
        editor.putBoolean("connected", true);
        editor.commit();

        //connectDevice(device);
        finish();
    }

    public void onClickBack (View v) {
        finish();
    }


    public void makeToast (String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }


}
