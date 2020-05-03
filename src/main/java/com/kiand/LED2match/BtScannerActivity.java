package com.kiand.LED2match;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class BtScannerActivity extends ListActivity implements ServiceConnection
{
	private enum Connected { False, Pending, True }
	public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private Handler _handler = new Handler();
	private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();
	private static BluetoothSocket btSocket = null;
	private static boolean socketConnected = false;
	private Set<BluetoothDevice> pairedDevices;
	public static final String BT_PREFS = "bluetooth_status";
	private static OutputStream outStream = null;
	private static InputStream inStream = null;
	public static String TAG = "MORRIS-BTSCANNER";
	private String deviceAddress;
	private String newline = "\r\n";
	//private SerialSocket socket;
	//private SerialService service;
	private Connected connected = Connected.False;
	private boolean initialStart = true;


	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		setContentView(R.layout.discovery);

		Log.d(TAG, "onCreate");
		// BT isEnable 
		if (!_bluetooth.isEnabled())
		{
			Log.w("HSB-CONTR-SCAN", "BT disabled! Switching on.");
			Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(turnOn, 0);
			//Toast.makeText(getApplicationContext(), "BT turned on",Toast.LENGTH_LONG).show();
		} else {
			//Toast.makeText(getApplicationContext(), "BT already on", Toast.LENGTH_LONG).show();
		}

		pairedDevices = _bluetooth.getBondedDevices();
		//lv = (ListView)findViewById(R.id.listView);

		ArrayList list = new ArrayList();
		for(BluetoothDevice bt : pairedDevices) list.add(bt.getName() + System.lineSeparator() + bt.getAddress());
		Toast.makeText(getApplicationContext(), "Showing Paired Devices",Toast.LENGTH_SHORT).show();
		final ArrayAdapter adapter = new  ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
		setListAdapter(adapter);

	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "BtScannerActivity - onStart");

	}

	/* Show devices list */
	/*protected void showDevices()
	{
		List<String> list = new ArrayList<String>();
		for (int i = 0, size = _devices.size(); i < size; ++i)
		{
			StringBuilder b = new StringBuilder();
			BluetoothDevice d = _devices.get(i);
			b.append(d.getAddress());
			b.append('\n');
			b.append(d.getName());
			String s = b.toString();
			list.add(s);
		}
		Log.d("HSB-CONTR-SCAN", "showDevices");
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
		_handler.post(new Runnable() {
			public void run()
			{
				setListAdapter(adapter);
			}
		});
	}*/

	// Select device
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		Log.d("HSB-CONTR-SCAN", "Click device");
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

		Bundle args = new Bundle();
		args.putString("device", device.getAddress());

		/*connectDevice(device);

		try {
			//Toast.makeText(getApplicationContext(), device.getName().toString(), Toast.LENGTH_SHORT).show();
		} catch (NullPointerException e) {
			Log.d (TAG, "BT device is null");
		}

		connectDevice(device);*/
		//finish();
	}

	/*public void connectDevice(BluetoothDevice bd){
		try{
			//ProgressDialog pairDia = ProgressDialog.show(this, "", "Connecting...", true, true);
			BluetoothSocket bs = bd.createRfcommSocketToServiceRecord(MY_UUID);
			bs.connect();
			socket = new SerialSocket();
			service.connect(this, "Connected to " + bd.getName());
			socket.connect(getApplicationContext(), service, bd);

			byte[] data = ("N" + newline).getBytes();
			Log.d(TAG, "Socket is null: " + (socket == null));
			Log.d(TAG, "Writing N to socket.");
			socket.write(data);

			if (bs.isConnected()) {
				Toast.makeText(this, "Connected to " + bd.getName() + "(" + bd.getAddress() + ")", Toast.LENGTH_SHORT).show();
				String sMessage = "N" + System.lineSeparator();

				BtCore.setServerAddress(bd.getAddress());
				BtCore.createSocketBluetooth();
				BtCore.initBluetooth();
				BtCore.sendMessageBluetooth(sMessage);


				try {
					outStream = btSocket.getOutputStream();
					inStream = btSocket.getInputStream();

					Log.d(TAG, "Sending 'N'");
					sendMessageBluetooth(sMessage);
				} catch (IOException e) {
					Log.e(TAG, "RESUME: Output stream creation failed.", e);
					Log.e(TAG, "RESUME: or Input stream creation failed.", e);
				}

				//finish();
			}

		} catch(Exception e){
			e.printStackTrace();
			this.finish();
		}
	}*/

	public static boolean Connected() {
		try {
            socketConnected = btSocket.isConnected();
		} catch (NullPointerException e) {
			socketConnected = false;
		}
		return socketConnected;
	}

	public static void sendMessageBluetooth(String message) {

		if (outStream == null) {
			Log.e("MORRIS-BT", "BtScannerActivity stream is Null");
			return;
		}
		byte[] msgBuffer = message.getBytes();
		try {
			outStream.flush(); // Mauricio
			outStream.write(msgBuffer);
			LightSettings.BTCommsLog btLog = new LightSettings.BTCommsLog("IN", message);
			btLog.appendMessage("IN", message);
			//Log.d("MORRIS-SENDMSGBT", "SENT: " + message);
		} catch (IOException e) {
			Log.d("MORRIS", "Exception during write.", e);
			Log.d("MORRIS", "Exception during write.", e);
		}
	}

	private void receive(byte[] data) {

		//receiveText.append(new String(data));
	}

	private void status(String str) {
		SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
		spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		//receiveText.append(spn);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {

	}

	private void connect() {
		/*try {
			BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
			String deviceName = device.getName() != null ? device.getName() : device.getAddress();
			status("connecting...");
			connected = Connected.Pending;
			socket = new SerialSocket();
			service.connect(this, "Connected to " + deviceName);
			socket.connect(getApplicationContext(), service, device);
		} catch (Exception e) {
			onSerialConnectError(e);
		}*/
	}

	private void disconnect() {
		/*connected = Connected.False;
		service.disconnect();
		socket.disconnect();
		socket = null;*/
	}


}