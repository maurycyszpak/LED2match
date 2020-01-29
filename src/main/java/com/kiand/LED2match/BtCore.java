package com.kiand.LED2match;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class BtCore extends Thread {

	public static final int MSG_LINE_READ = 21;
	public static String TAG = "MORRIS-BTCORE";

	private static BluetoothAdapter mBluetoothAdapter = null;
	private static BluetoothSocket btSocket = null;
	private static OutputStream outStream = null;
	private static InputStream inStream = null;
	private static InputStreamReader isr = null;
	private static byte[] buffer;
	private static boolean socketConnected = false;
	private ProgressDialog dlgProgress;

	public static final int REQUEST_DISCOVERY = 0x1;
	public static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// hardcode your server's MAC address here
	public static String serverAddress = "0A:17:07:0B:25:16";
	public static String serverAddressAfterScan = "";
	public static String remoteDeviceName = "";

	// accessors
	public static String getRemoteDeviceName() {
		return remoteDeviceName;
	}

	public static int initBluetooth() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		return 0;
	}

	private Runnable btCommsThread = new Runnable() {
		public void run() {

			executeBTCommand("r");
			dlgProgress.dismiss();
		}

		public void executeBTCommand(String sCommand) {
			String sReply = "";
			sReply = LightAdjustments.bluetoothAskReply(sCommand);

			//return sReply;
		}
	};

	public static void setServerAddress(String serverAddres) {
		serverAddressAfterScan = serverAddres;
	}

	public static int createSocketBluetooth() {
		if (serverAddressAfterScan != "") {
			serverAddress = serverAddressAfterScan;
			//Log.v(TAG, "Use server Address after Scan:" + serverAddress);
		}

		BluetoothDevice device = mBluetoothAdapter
				.getRemoteDevice(serverAddress);
		try {
			btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
			remoteDeviceName = device.getName();

		} catch (IOException e) {
			Log.e(TAG, "RESUME: Socket creation failed.", e);
			return -1;
		}

		// Mauricio

		InputStream tmpIn = null;
		OutputStream tmpOut = null;
		try {
			tmpOut = btSocket.getOutputStream();
			tmpIn = btSocket.getInputStream();
			buffer = new byte[1024];
		} catch (IOException e) {
			e.printStackTrace();
		}
		outStream = tmpOut;
		inStream = tmpIn;


		// mBluetoothAdapter.cancelDiscovery(); // rallenterebbe
		return 0;
	}

	public static boolean Connected() {
		try {
			if (btSocket.isConnected()) {
				socketConnected = true;
			} else {
				socketConnected = false;
			}
		} catch (NullPointerException e) {
			socketConnected = false;
		}
		return socketConnected;
	}

	public static int connectBluetooth() {
		try {
			btSocket.connect();
			Log.d(TAG,"RESUME: BT connection established, data transfer link open.");
		} catch (IOException e) {
			try {
				btSocket.close();
			} catch (IOException e2) {
				Log.d(TAG, "RESUME: Unable to close socket during connection failure", e2);
			}
			return -1;
		}

		try {
			if (BtCore.Connected() ) {
			//	BtClientActivity.read_EE_presets_write_prefs();
			}
			Toast.makeText(App.context, "Connected to Controller over BT", Toast.LENGTH_LONG).show();


		} catch (NullPointerException e3) {
			Log.d(TAG, "RESUME: BTCore socket is a null object", e3);
		}

		try {
			outStream = btSocket.getOutputStream();
			inStream = btSocket.getInputStream();
		} catch (IOException e) {
			Log.e(TAG, "RESUME: Output stream creation failed.", e);
			Log.e(TAG, "RESUME: or Input stream creation failed.", e);
			return -2;
		}
		return 0;
	}

	public static void sendMessageBluetooth(String message) {

		if (outStream == null) {
			Log.e("MORRIS-BT", "BtCore stream is Null when trying to send '" + message + "'.");
			return;
		}
		byte[] msgBuffer = message.getBytes();
		try {
			outStream.flush(); // Mauricio
			outStream.write(msgBuffer);
			LightAdjustments.BTCommsLog btLog = new LightAdjustments.BTCommsLog("IN", message);
			btLog.appendMessage("IN", message);
			//Log.d("MORRIS-SENDMSGBT", "SENT: " + message);
		} catch (IOException e) {
			Log.d("MORRIS", "Exception during write.", e);
			Log.d("MORRIS", "Exception during write.", e);
		}
	}
	private static final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_LINE_READ:
					//if (paused) break;
					String line = (String) msg.obj;
					//if (D) Log.d(TAG, line);
					//mConversationArrayAdapter.add(line);

					break;
			}
		}
	};

	private static void sendLineRead(String line) {
		mHandler.obtainMessage(MSG_LINE_READ, -1, -1, line).sendToTarget();
		Log.d(TAG, "READ: " + line);
	}


	public static String receiveMessageBluetooth() {
		int iRead = 0;
		if (inStream == null) {
			Log.d(TAG, "InputStream is Null");
			return "-5";
		}
		String response = "";
		//byte[] msgBuffer = new byte[1024];
		BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
		boolean stop = false;

		//Log.d (TAG, "rcvMsgBt: entering function");
		SystemClock.sleep(100);
		while (!stop) {
			try {
				//Log.d(TAG, "Trying to execute: inStream.read()");
				String sConcat = "";
				//int iData = 0;

				int iData = in.read();
				//Log.d(TAG, "in.read():" + iData + "("+ (char)iData+")");
				while(iData != -1 && iData != 10) {
					sConcat += Character.toString((char)iData);
					//Log.d("INREAD", "in.read():" + iData + "("+ (char)iData+")");
					//Log.d(TAG, "concat():" + sConcat);
					iData = in.read();
				}
				//String line = in.readLine();
				String line = sConcat;
				//Log.d(TAG, "in.readLine() executed.");
				if (line != null) {
					sendLineRead(line);
					stop = true;
					response = line;
				} else {
					Log.d(TAG, "in.readLine() returned null");
				}
			} catch (IOException e) {
				Log.e(TAG, "disconnected", e);
				//connectionLost();
				e.printStackTrace();
			//} finally {
			//	if (inStream != null) {
			//		inStream.close();
			//	}
			}
		}
		return response;
	}

	public static void closeBluetooth() {
		if (outStream != null) {
			try {
				outStream.flush();
			} catch (IOException e) {
				//Log.e(TAG, "ON PAUSE: Couldn't flush output stream.", e);
			}
		}

		try {
			btSocket.close();
		} catch (IOException e2) {
			//Log.e(TAG, "ON PAUSE: Unable to close socket.", e2);
		}
	}

}
