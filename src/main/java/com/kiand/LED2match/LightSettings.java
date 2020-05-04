package com.kiand.LED2match;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.TimerTask;
import java.text.ParseException;
import java.util.regex.PatternSyntaxException;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import static com.kiand.LED2match.BtScannerActivity.BT_PREFS;
import static com.kiand.LED2match.Constants.DEFAULT_PSU_POWER;
import static com.kiand.LED2match.TRSSettings.TL84_DELAY_KEY;

public class LightSettings extends Activity implements ServiceConnection {

	private TextView display;

	public static final int TOAST_MESSAGE = 1000;
	public static final int MSG_HIDE_PROGRESS_DIALOG = 1001;
	public static final String MSG_SET_SEEKBAR_PROGRESS = "custom-event-name";
    public static final String PRESETS_DEFINITION = "presets_definition"; //Mauricio
    public static final String SHAREDPREFS_UNITNAME = "unit_name";
	//public static final String TL84_TAG = "TL84";

    Button startButton, stopButton;
	private static final int MSG_SHOW_TOAST = 1;
	private static final String password = "hokus";
	final Context context = this;
	public static String TAG = "MORRIS-BTCLIENT";
	public boolean blUSBConnected = false;
	public String sFWver = "N/A";
	public static String sUnitName = "";

	public static final String sNewLine = System.getProperty("line.separator");
	public ProgressDialog pDialog;
	Boolean blLamp1_ON, blLamp2_ON, blLamp3_ON;


	private Handler messageHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_SHOW_TOAST) {
				String message = (String)msg.obj;

				switch (msg.what) {
					case TOAST_MESSAGE:
						Toast.makeText(MyApplication.getAppContext(), Arrays.toString(message.getBytes()), Toast.LENGTH_SHORT).show();
						break;

					case MSG_HIDE_PROGRESS_DIALOG:
						if (pDialog.isShowing()) {
							pDialog.dismiss();
						}
						break;
						//handle the result here

					default:
						//super.handleMessage(msg);
						break;
				}

			}
		}
	};

	public static final String MY_PREFS = "MyPrefsFile"; //Mauricio
	public static final String APP_SHAREDPREFS_READ = "EEPROM_PRESETS_read"; //Mauricio
	public static final String APP_SHAREDPREFS_WRITE = "EEPROM_PRESETS_write"; //Mauricio
	public static final String SHAREDPREFS_LED_TIMERS = "led_timers"; //Mauricio
	public static final String SHAREDPREFS_LAMP_STATE = "current_lamps_state"; //Mauricio
	public static final String SHAREDPREFS_COMMS_LOG = "serial_communication.txt"; //Mauricio
	public static final String BTCOMMSLOG_SHAREDPREFS = "Controller-communication-log"; //Mauricio
	public static final String SERIAL_COMMS_LOG = "serial_controller-communication-log"; //Mauricio

	public static final String SHAREDPREFS_ONE_OFF_SEEKBARS = "one-off-seekbar-values.txt"; //Mauricio

	public Boolean blSilent = false;
	public static final int NUM_PRESETS_MAX = 10;
	public static final int MIN_PRESET_NAME_LEN = 1;

	String check = "RGB";
	String ha = "0";

	private static final boolean Debug = true;
    private EditText edRed, edGreen, edBlue, edWhite, edLED65, edLEDUVA, edLED50, edLED27, edLED395, edLED420;
    private SeekBar barWhite, barRed, barGreen, barBlue, barLED65, barLEDUVA, barLED50, barLED27, barLED395, barLED660;

	RelativeLayout rgbLay;
	RelativeLayout bottomButtonsLayout;

	static boolean updateText = false;
	public static String sUSBResponse ="";


    /*UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                sUSBResponse.concat(data);
                if (sUSBResponse.length() > 10) {
					makeToast("UsbReadCallback: " + sUSBResponse);
					decodeBTResponse(sUSBResponse);
					sUSBResponse = null;
				}

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };*/

	private ProgressDialog dlgProgressSplash;
	public Spinner spinner_control_preset_list; // Mauricio
	public Button btnStore, btnRemove, btnRead; // Mauricio
	private Button btnL1, btnL2, btnL3;
	Button btnSend;
	public EditText edtPresetName;

	public ArrayList<String> spnPresetsArrayList = new ArrayList<>(); // Mauricio
	public ArrayAdapter<String> spnPresetsAdapter; // Mauricio

    public UsbCOMMsService usbService;
    private BtCOMMsService lclBTServiceInstance;
    boolean mBound = false;
    boolean mBoundBT = false;

    //public CdcAcmSerialDriver cdcDriver;
    private MyHandler mHandler;


    public final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbCOMMsService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    private ServiceConnection btConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BtCOMMsService.MyBinder binder = (BtCOMMsService.MyBinder) service;
            lclBTServiceInstance = binder.getService();
            mBoundBT = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBoundBT = false;
        }
    };

	/*public final ServiceConnection btConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			btService = ((BtCOMMsService.MyBinder) arg1).getService();
			btService.setHandler(mHandler);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			btService = null;
		}
	};*/

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		//BtCOMMsService.MyBinder b = (BtCOMMsService.MyBinder) binder;
		//btService = b.getService();
		//Toast.makeText(LightAdjustments.this, "666 Connected 666", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		//btService = null;
	}

    /*UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                //data.concat("/n");
                //makeToast("USB RCVD:" + data);
                //sUSBResponse = data;
				tvAppend(data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };*/

	private BroadcastReceiver btReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			//Bundle bundle = intent.getExtras();

		}
	};
	private BroadcastReceiver btReceiverBTdevice = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (action != null) {
				switch (action){
					case BluetoothDevice.ACTION_ACL_CONNECTED:
						Log.d(TAG, "Broadcast receiver: ACL_CONNECTED");
						break;
					case BluetoothDevice.ACTION_ACL_DISCONNECTED:
						Log.d(TAG, "Broadcast receiver: ACL_DISCONNECTED");
						break;
				}
			}
		}
	};


	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("morris-receiver", "Checking message: " + intent.getAction());
			switch (Objects.requireNonNull(intent.getAction())) {
				case UsbCOMMsService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
					//Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
					//blUSBConnected = true;
					break;
				case UsbCOMMsService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
					Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
					break;
				case UsbCOMMsService.ACTION_NO_USB: // NO USB CONNECTED
					//Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
					break;
				case UsbCOMMsService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
					Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
					blUSBConnected = false;
					//delete temp. SP files:
					//clearSharedPrefsFile(SHAREDPREFS_LED_TIMERS);
					//clearSharedPrefsFile(APP_SHAREDPREFS_READ);
					//clearSharedPrefsFile(APP_SHAREDPREFS_WRITE);

					break;
				case UsbCOMMsService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
					Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
					break;
				case UsbCOMMsService.ACTION_USB_READY:
					makeToast("USB Device is ready");
					blUSBConnected = true;

					JSONFileRefresh asyncTask = new JSONFileRefresh(context);
					asyncTask.execute();
					//getJSONFile();
					//getUnitName();
					//populateLampNames();
					//readEE_presets_click(null);
					break;
				case UsbCOMMsService.ACTION_USB_ATTACHED:
					makeToast("USB Device has been attached!");
					//blUSBConnected = true;
					makeToast("Now the blUSBConnected  = " + blUSBConnected);
					break;

                case MSG_SET_SEEKBAR_PROGRESS:
                    String message = intent.getStringExtra("iMessage");
                    makeToast("Trying to set seekbar to: " + message);
					Log.d("morris-receiver", "Got message: " + message);
                    barLEDUVA.setProgress(Integer.valueOf(message));
                    break;

			}
		}
	};

	public void getJSONFile() {
		String sCommand = "F" + sNewLine;
		lclBTServiceInstance.sendData(sCommand);
		sendDataOverSerial(sCommand);
		SystemClock.sleep(200);
	}

	private void getUnitName() {
        SharedPreferences spUnitName = getSharedPreferences(SHAREDPREFS_UNITNAME, 0);
        String sUnitName = spUnitName.getString("UNIT_NAME", "not_loaded");
        messageHandler.post(() -> setTitle("Set Lights" + " " + sUnitName));
    }

    /*private void populateLampNames() {
        //String sUnitName = "";

        //Log.d(TAG, "bluetoothAskReply(V1)");
        final String sLamp1Name = extractJSONvalue("", "lamp1_name");
        final String sLamp2Name = extractJSONvalue("", "lamp2_name");
        final String sLamp3Name = extractJSONvalue("", "lamp3_name");
        //final String sLamp4Name = extractJSONvalue("", "lamp4_name");

        messageHandler.post(new Runnable() {
            @Override
            public void run() {
                setLampName(1, sLamp1Name);
                setLampName(2, sLamp2Name);
                setLampName(3, sLamp3Name);
                //setLampName(4, sLamp4Name);
            }
        });

    }*/

	public void colorizeLayout214(String sPresetKey, String sFileName) {
		SharedPreferences spsValues = getSharedPreferences(sFileName, 0);
		String strRGB = spsValues.getString(sPresetKey, "1,0,0,0,0,0,0,0,0,0,0"); //214
		//String strRGB = spsValues.getString(sPresetKey, "0,0,0,0");

		Log.d(TAG, "File: " + sFileName + ", Key: " + sPresetKey + ", Value: " + strRGB);
		String[] arrColour = strRGB.split(",");

		//int ll = Integer.parseInt(arrColour[0]); //214
		if (arrColour.length != 10) {
			//makeToast("Preset definition might be corrupted - expecting 10 values, reading: " + arrColour.length);
			return;
		}

		int led_65 = Integer.parseInt(arrColour[0]);
		int led_uva = Integer.parseInt(arrColour[1]);
		int led_50 = Integer.parseInt(arrColour[2]);
		int led_27 = Integer.parseInt(arrColour[3]);
		int led_w = Integer.parseInt(arrColour[4]);
		int led_r = Integer.parseInt(arrColour[5]);
		int led_g = Integer.parseInt(arrColour[6]);
		int led_b = Integer.parseInt(arrColour[7]);
		int led_395 = Integer.parseInt(arrColour[8]);
		int led_660 = Integer.parseInt(arrColour[9]);

		rgbLay.setBackgroundColor(Color.rgb(led_r, led_g, led_b));

		edLED65.setText(String.valueOf(led_65));
		edLEDUVA.setText(String.valueOf(led_uva));
		edLED50.setText(String.valueOf(led_50));
		edLED27.setText(String.valueOf(led_27));
		edRed.setText(String.valueOf(led_r));
		edGreen.setText(String.valueOf(led_g));
		edBlue.setText(String.valueOf(led_b));
		edWhite.setText(String.valueOf(led_w));
		edLED395.setText(String.valueOf(led_395));
		edLED420.setText(String.valueOf(led_660));

		barLED65.setProgress(led_65);
		barLEDUVA.setProgress(led_uva);
		barLED50.setProgress(led_50);
		barLED27.setProgress(led_27);
		barWhite.setProgress(led_w);
		barRed.setProgress(led_r);
		barGreen.setProgress(led_g);
		barBlue.setProgress(led_b);
		barLED395.setProgress(led_395);
		barLED660.setProgress(led_660);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		/*menu.add(Menu.NONE, 0, 0, "Scan").setIcon(
				getResources().getDrawable(R.drawable.icon_scan));*/

		menu.add(Menu.NONE, 1, 1, "Service page").setIcon(
				getResources().getDrawable(R.drawable.icon_scan));

		menu.add(Menu.NONE, 2, 2, "About").setIcon(
				getResources().getDrawable(R.drawable.icon_information));


		menu.add(Menu.NONE, 3, 3, "View firmware data").setIcon(getResources().getDrawable(R.drawable.icon_information));

		menu.add(Menu.NONE, 4, 4, "Digital panel").setIcon(
		        getResources().getDrawable(R.drawable.icon_information));

		/*menu.add(Menu.NONE, 5, 5, "Torso Sequence").setIcon(
				getResources().getDrawable(R.drawable.icon_information));

		menu.add(Menu.NONE, 6, 6, "Blower settings").setIcon(
				getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 7, 7, "Operating Hours").setIcon(
                getResources().getDrawable(R.drawable.icon_information));*/

		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	public void makeToast (String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	public void switch_all_lights_off(View v) {
		edRed.setText("0");
		edGreen.setText("0");
		edBlue.setText("0");
		edWhite.setText("0");
		edLED65.setText("0");
		edLEDUVA.setText("0");
		edLED50.setText("0");
		edLED27.setText("0");
		edLED395.setText("0");
		edLED420.setText("0");

		barBlue.setProgress(0);
		barGreen.setProgress(0);
		barRed.setProgress(0);
		barWhite.setProgress(0);
		barLED65.setProgress(0);
		barLEDUVA.setProgress(0);
		barLED50.setProgress(0);
		barLED27.setProgress(0);
		barLED395.setProgress(0);
		barLED660.setProgress(0);

        String sCommand = "S00000000000000000000";
        sCommand += "$" + sNewLine;
        //if (btService.connected) {
        if (mBoundBT) {
            Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
            lclBTServiceInstance.sendData(sCommand);
        } else {
            Log.d(TAG, "Service btService not connected!");
        }
        usbService.write(sCommand.getBytes());

        //switch off TL84
		sCommand = "S11000";
		//if (btService.connected) {
		if (mBoundBT) {
			Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
			lclBTServiceInstance.sendData(sCommand);
		} else {
			Log.d(TAG, "Service btService not connected!");
		}
		usbService.write(sCommand.getBytes());

    }

	public void startScannerActivity() {
		Log.v(TAG, "Start Scan");

		Intent btScanIntent = new Intent(this, BtScannerActivity.class);
		//startActivityForResult(btScanIntent, BtCore.REQUEST_DISCOVERY);
		startActivityForResult(btScanIntent, BtCore.REQUEST_DISCOVERY);
	}

	// after select, connect to device
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != BtCore.REQUEST_DISCOVERY) {
			return;
		}
		if (resultCode != RESULT_OK) {
			return;
		}
		BluetoothDevice device = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		String newMacAddress = device.getAddress();
		Log.d(TAG, "mac " + newMacAddress);
		SharedPreferences prefsBT = getSharedPreferences(BT_PREFS, Context.MODE_PRIVATE);
		SharedPreferences.Editor editorBT = prefsBT.edit();
		editorBT.putString("device_btaddress", newMacAddress);
		editorBT.putBoolean("connected", true);
		editorBT.apply();

		String newServerName = device.getName();
		BtCore.serverAddressAfterScan = newMacAddress;

		// save in preference
		SharedPreferences prefs = getSharedPreferences(MY_PREFS, Context.MODE_PRIVATE);

		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("KEY_BTMACADDRESS", newMacAddress);
		editor.putString("KEY_BTSERVERNAME", newServerName);
		editor.apply();

		//if (BtCore.Connected() ) {
		//	read_EE_presets_write_prefs();
		//}
		// update title bar
		setTitle("Light2match (" + newMacAddress + " - " + newServerName + ")");
		Toast.makeText(this, "Bluetooth connected!", Toast.LENGTH_LONG).show();
	}

	public String sAppVersionDate = "2020/01/16";
	public String apkVersion = "v3.01";//rc" + sAppVersionDate.replace("/", "");


	Handler h = new Handler();
	int delay = 300*1000; //1 second=1000 millisecond, 15*1000=15seconds
	Runnable runnable;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		ArrayList<String> RESERVED_PRESET_NAMES_LIST = new ArrayList <> (Arrays.asList("LOW", "PRG", "OFF"));
		mHandler = new MyHandler(this);
		setContentView(R.layout.light_adjustments);// main

		display = findViewById(R.id.textTestControl);
        display.setMovementMethod(new ScrollingMovementMethod());

		spinner_control_preset_list = findViewById(R.id.spinner2); // Mauricio
		btnStore = findViewById(R.id.btnstore); // Mauricio
		btnSend = findViewById(R.id.btnSend); // Mauricio
		edtPresetName = findViewById(R.id.edtpresetname); //Mauricio
		btnRemove = findViewById(R.id.btnRemove); // Mauricio
		btnL1 = findViewById(R.id.btnL1);
		btnL2 = findViewById(R.id.btnL2);
		btnL3 = findViewById(R.id.btnL3);
		//btnL4 = findViewById(R.id.btnL4);
		rgbLay = findViewById(R.id.RGBlayout);
		bottomButtonsLayout = findViewById(R.id.bottom_buttons_layout);

		edRed = findViewById(R.id.edRed);
		edGreen = findViewById(R.id.edGreen);
		edBlue = findViewById(R.id.edBlue);
		edWhite = findViewById(R.id.edWhite);
		edLED65 = findViewById(R.id.edLED65);
		edLEDUVA = findViewById(R.id.edLEDUVA);
		edLED50 = findViewById(R.id.edLED50);
		edLED27 = findViewById(R.id.edLED27);
		edLED395 = findViewById(R.id.edLED395);
		edLED420 = findViewById(R.id.edLED420);

		barWhite = findViewById(R.id.barWhite);
		barRed = findViewById(R.id.barRed);
		barGreen = findViewById(R.id.barGreen);
		barBlue = findViewById(R.id.barBlue);
		barLED65 = findViewById(R.id.barLED65);
		barLEDUVA = findViewById(R.id.barLEDUVA);
		barLED50 = findViewById(R.id.barLED50);
		barLED27 = findViewById(R.id.barLED27);
		barLED395 = findViewById(R.id.barLED395);
		barLED660 = findViewById(R.id.barLED420);
		//dlgProgressSplash = new ProgressDialog(this);

		edRed.setText("0");
		edGreen.setText("0");
		edBlue.setText("0");
		edWhite.setText("0");
		edLED65.setText("0");
		edLEDUVA.setText("0");
		edLED50.setText("0");
		edLED27.setText("0");
		edLED395.setText("0");
		edLED420.setText("0");

		barBlue.setProgress(0);
		barGreen.setProgress(0);
		barRed.setProgress(0);
		barWhite.setProgress(0);
		barLED65.setProgress(0);
		barLEDUVA.setProgress(0);
		barLED50.setProgress(0);
		barLED27.setProgress(0);
		barLED395.setProgress(0);
		barLED660.setProgress(0);

		barBlue.setMax(255);
		barGreen.setMax(255);
		barRed.setMax(255);
		barWhite.setMax(255);
		barLED65.setMax(255);
		barLEDUVA.setMax(255);
		barLED50.setMax(255);
		barLED27.setMax(255);
		barLED395.setMax(255);
		barLED660.setMax(255);
		//setUiEnabled(false);
		/*IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);*/
		
		spnPresetsAdapter = new ArrayAdapter<>(this, R.layout.spinner_row, spnPresetsArrayList); // Mauricio
		spinner_control_preset_list.setAdapter(spnPresetsAdapter); // Mauricio
		spinner_control_preset_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // Mauricio
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view, int pos, long id) { // Mauricio
				InputMethodManager inputManager = (InputMethodManager)
						getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);
				Log.d (TAG, "Executing spinnerPresets onItemSelectedListener. spnPresetsArrayList: " + spnPresetsArrayList.size());

				if (spnPresetsArrayList.size() > 0) {
					colorizeLayout214(spinner_control_preset_list.getSelectedItem().toString() , PRESETS_DEFINITION);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) { // Mauricio
				// DO NOTHING

			}
		});
		
		rgbLay.setVisibility(View.VISIBLE); //Mauricio - let's load the RGB layout straight away

		barRed.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				edRed.setText("" + arg1);
				fncRefreshRGB();
			}
		});

		barGreen.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				edGreen.setText("" + arg1);
				fncRefreshRGB();
			}
		});

		barBlue.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				edBlue.setText("" + arg1);
				fncRefreshRGB();
			}
		});

		barWhite.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				edWhite.setText("" + arg1);
				fncRefreshRGB();
			}
		});

		barLED65.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				edLED65.setText("" + arg1);
				fncRefreshRGB();
			}
		});

		barLEDUVA.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				edLEDUVA.setText("" + arg1);
			}
		});

		barLED50.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				edLED50.setText("" + arg1);
			}
		});

		barLED27.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				edLED27.setText("" + arg1);
			}
		});

		barLED395.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				edLED395.setText("" + arg1);
			}
		});

		barLED660.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				edLED420.setText("" + arg1);
			}
		});
		
		edWhite.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

				String strEnteredVal = arg0.toString();
				if (!strEnteredVal.equals("")) {
					int num = Integer.parseInt(strEnteredVal);
					if (num > 255) {
						edWhite.setText("255");
						barWhite.setProgress(num);
					} else {
						barWhite.setProgress(num);
					}
				}
				edWhite.setSelection(edWhite.getText().length());
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
										  int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {

			}
		});

		edRed.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				String strEnteredVal = arg0.toString();

				if (!strEnteredVal.equals("")) {
					int num = Integer.parseInt(strEnteredVal);

					//System.out.println("Hue value is " + num);
//					System.out.println("num" + num);

					if (check.equals("RGB")) {
						if (num > 255) {
							edRed.setText("255");
							barRed.setProgress(255);
							ha = "255";
						} else {
							barRed.setProgress(num);
						}
						fncRefreshRGB();
					}
				}
				// if(updateText==true)
				{
					updateText = false;
				}
				// else
				edRed.setSelection(edRed.getText().length()); //To prevent moving the cursor inside the text to the beginning
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {

			}
		});

		edGreen.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				String strEnteredVal = arg0.toString();

				if (!strEnteredVal.equals("")) {
					int num = Integer.parseInt(strEnteredVal);

					//System.out.println("Hue value is    " + num);

					if (check.equals("RGB")) {
						if (num > 255) {
							edGreen.setText("255");
							ha = "255";
							barGreen.setProgress(255);
						} else {
							barGreen.setProgress(num);
						}
						fncRefreshRGB();
					}
				}
				// if(updateText==true)
				{
					updateText = false;
				}
				// else
				edGreen.setSelection(edGreen.getText().length());
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {

			}
		});

		edBlue.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				
				String strEnteredVal = arg0.toString();

				if (!strEnteredVal.equals("")) {
					
					int num = Integer.parseInt(strEnteredVal);

					//System.out.println("Hue value is    " + num);

					if (check.equals("RGB")) {
						if (num > 255) {
							edBlue.setText("255");
							ha = "255";
							barBlue.setProgress(255);
						} else {
							barBlue.setProgress(num);
						}
						fncRefreshRGB();
					}
				}
				// if(updateText==true)
				{
					updateText = false;
				}
				// else
				edBlue.setSelection(edBlue.getText().length());
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {

			}
		});

		edLED65.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				String strEnteredVal = arg0.toString();

				if (!strEnteredVal.equals("")) {
					int num = Integer.parseInt(strEnteredVal);
					if (num > 255) {
						edLED65.setText("255");
						barLED65.setProgress(255);
					} else {
						barLED65.setProgress(num);
					}
				}
				edLED65.setSelection(edLED65.getText().length()); //To prevent moving the cursor inside the text to the beginning
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {

			}
		});

		edLEDUVA.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				String strEnteredVal = arg0.toString();

				if (!strEnteredVal.equals("")) {
					int num = Integer.parseInt(strEnteredVal);
					if (num > 255) {
						edLEDUVA.setText("255");
						barLEDUVA.setProgress(255);
					} else {
						barLEDUVA.setProgress(num);
					}
				}
				edLEDUVA.setSelection(edLEDUVA.getText().length()); //To prevent moving the cursor inside the text to the beginning
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {

			}
		});

		edLED50.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				String strEnteredVal = arg0.toString();

				if (!strEnteredVal.equals("")) {
					int num = Integer.parseInt(strEnteredVal);
					if (num > 255) {
						edLED50.setText("255");
						barLED50.setProgress(255);
					} else {
						barLED50.setProgress(num);
					}
				}
				edLED50.setSelection(edLED50.getText().length()); //To prevent moving the cursor inside the text to the beginning
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {

			}
		});

		edLED27.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				String strEnteredVal = arg0.toString();

				if (!strEnteredVal.equals("")) {
					int num = Integer.parseInt(strEnteredVal);
					if (num > 255) {
						edLED27.setText("255");
						barLED27.setProgress(255);
					} else {
						barLED27.setProgress(num);
					}
				}
				edLED27.setSelection(edLED27.getText().length()); //To prevent moving the cursor inside the text to the beginning
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {

			}
		});

		edLED395.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				String strEnteredVal = arg0.toString();

				if (!strEnteredVal.equals("")) {
					int num = Integer.parseInt(strEnteredVal);
					if (num > 255) {
						edLED395.setText("255");
						barLED395.setProgress(255);
					} else {
						barLED395.setProgress(num);
					}
				}
				edLED395.setSelection(edLED395.getText().length()); //To prevent moving the cursor inside the text to the beginning
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {

			}
		});

		edLED420.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				String strEnteredVal = arg0.toString();

				if (!strEnteredVal.equals("")) {
					int num = Integer.parseInt(strEnteredVal);
					if (num > 255) {
						edLED420.setText("255");
						barLED660.setProgress(255);
					} else {
						barLED660.setProgress(num);
					}
				}
				edLED420.setSelection(edLED420.getText().length()); //To prevent moving the cursor inside the text to the beginning
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {

			}
		});

		btnStore.setOnClickListener(view -> {

			//popupWaitDialog();

			InputMethodManager inputManager = (InputMethodManager)
					getSystemService(Context.INPUT_METHOD_SERVICE);

			inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);

			//copy APP_SHAREDPREFS_READ to _WRITE and add the new one on top
			if (edtPresetName.getText().toString().length() >= MIN_PRESET_NAME_LEN && !(RESERVED_PRESET_NAMES_LIST.contains(edtPresetName.getText().toString()))) {

				SharedPreferences prefs_presets = getSharedPreferences(PRESETS_DEFINITION, 0);
				SharedPreferences spsValues_write = getSharedPreferences(APP_SHAREDPREFS_WRITE, 0);
				SharedPreferences.Editor spsEditor_write = spsValues_write.edit();

				spsEditor_write.clear();
				spsEditor_write.apply();

				Map<String,?> keys = prefs_presets.getAll();

				for (Map.Entry<String,?> entry : keys.entrySet()) {
					spsEditor_write.putString(entry.getKey(), entry.getValue().toString());
				}

				int iCtr = Integer.valueOf(extractJSONvalue("", "preset_counter"));
				if (iCtr < NUM_PRESETS_MAX) {
					//Cycle through spsEditor_read first and add key + value to _write
					//For each pair of key-> value add item to the list
					//refresh the list and the spinner
					//spsEditor_write.clear();
					//spsEditor_write.commit();

					String strComaDel = "";
					strComaDel += edLED65.getText().toString() + "," +
							edLEDUVA.getText().toString() + "," + edLED50.getText().toString() + "," + edLED27.getText().toString() + "," +
							edWhite.getText().toString() + "," + edRed.getText().toString() + "," + edGreen.getText().toString() + "," +
							edBlue.getText().toString() + "," + edLED395.getText().toString() + "," + edLED420.getText().toString();

					//spinnerPresets.setEnabled(false);

					//Toast.makeText(getBaseContext(), edtPresetName.getText().toString().toUpperCase() + " preset saved.\nValues: " + strComaDel, Toast.LENGTH_SHORT).show();
					spsEditor_write.putString(edtPresetName.getText().toString().toUpperCase(), strComaDel);
					spsEditor_write.commit();

					sendPresetsOverBT();
					sendPresetsOverSerial();
					//SystemClock.sleep(1000);
					getJSONFile();
					SystemClock.sleep(1000);
					extractPresetsFromJson();
					spnPresetsAdapter.notifyDataSetChanged();

					blSilent = true;
					if (spnPresetsArrayList.size() > 0) {
						spinner_control_preset_list.setSelection(0);
						colorizeLayout214(spinner_control_preset_list.getSelectedItem().toString(), PRESETS_DEFINITION);
					}
					blSilent = false;

				} else {
					Toast.makeText(getBaseContext(), "Only " + NUM_PRESETS_MAX + " presets allowed. Please remove a preset to store a new one.\nCurrent size: " + spnPresetsArrayList.size(), Toast.LENGTH_SHORT).show();
				}
				//sendPresetsOverBT();
				//SystemClock.sleep(250);
				//read_EE_presets_write_prefs();

				edtPresetName.setText("");
			} else {
				Toast.makeText(getBaseContext(), "Preset not saved - the name should be at least " + MIN_PRESET_NAME_LEN  + " character(s) long and should not contain reserved names ('LOW')", Toast.LENGTH_SHORT).show();
			}

			/*PopupAsyncTask asyncTask = new PopupAsyncTask(context, "");
			asyncTask.execute();*/
		});

		btnRemove.setOnClickListener(w -> {

			//showSplash();
			InputMethodManager inputManager = (InputMethodManager)
					getSystemService(Context.INPUT_METHOD_SERVICE);

			inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);

			String sPrefsKeyToBeDeleted = "";
			if (spinner_control_preset_list.getSelectedItem() == null) {
				return;
			}
			sPrefsKeyToBeDeleted = spinner_control_preset_list.getSelectedItem().toString();

			SharedPreferences prefs_presets = getSharedPreferences(PRESETS_DEFINITION, 0);
			SharedPreferences spsValues_write = getSharedPreferences(APP_SHAREDPREFS_WRITE, 0);
			SharedPreferences.Editor spsEditor_write = spsValues_write.edit();

			spsEditor_write.clear();
			spsEditor_write.apply();

			Map<String,?> keys = prefs_presets.getAll();

			for (Map.Entry<String,?> entry : keys.entrySet()) {
				spsEditor_write.putString(entry.getKey(), entry.getValue().toString());
			}
			spsEditor_write.remove(sPrefsKeyToBeDeleted);
			spsEditor_write.commit();

			if (spnPresetsArrayList.size() > 0) {


				sendPresetsOverSerial();
				sendPresetsOverBT();
				SystemClock.sleep(100);

				getJSONFile();
				SystemClock.sleep(2000);
				extractPresetsFromJson();

				//spnPresetsAdapter.addAll(spnPresetsArrayList);
				spnPresetsAdapter.notifyDataSetChanged();

				blSilent = true;
				if (spnPresetsArrayList.size()>0) {
					spinner_control_preset_list.setSelection(0);
					colorizeLayout214(spinner_control_preset_list.getSelectedItem().toString(), APP_SHAREDPREFS_READ);
					//spnPresetsAdapter.notifyDataSetChanged(); // Mauricio - this might not be needed.
				}
				blSilent = false;
			}
			//hideSplash();
		});


		// load savedPreference _KEY_MACADDRESS
		SharedPreferences prefs = getSharedPreferences(MY_PREFS, Context.MODE_PRIVATE);

		btnSend.setOnClickListener(view -> {
			onClickSend(view);
			Toast.makeText(getBaseContext(), "Color applied!", Toast.LENGTH_SHORT).show();

			File file = getApplicationContext().getFileStreamPath("hts-chrono1");
			if (!file.exists()) {
				writeData(findViewById(R.id.wrap_content), "hts-chrono1", String.valueOf(SystemClock.elapsedRealtime()));
			}

			String factorFileName = "hts-chrono1.REDfactor";
			file = getApplicationContext().getFileStreamPath(factorFileName);
			if (file.exists()) {
				String currentFileContent = readData(findViewById(R.id.wrap_content), factorFileName);
				currentFileContent += "|" + SystemClock.elapsedRealtime() + "-" + edRed.getText().toString();
				writeData(findViewById(R.id.wrap_content), factorFileName, currentFileContent);
			} else {
				writeData(findViewById(R.id.wrap_content), factorFileName, SystemClock.elapsedRealtime() + "-" + edRed.getText().toString());
			}

			factorFileName = "hts-chrono1.GREENfactor";
			file = getApplicationContext().getFileStreamPath(factorFileName);
			if (file.exists()) {
				String currentFileContent = readData(findViewById(R.id.wrap_content), factorFileName);
				currentFileContent += "|" + SystemClock.elapsedRealtime() + "-" + edGreen.getText().toString();
				writeData(findViewById(R.id.wrap_content), factorFileName, currentFileContent);
			} else {
				writeData(findViewById(R.id.wrap_content), factorFileName, SystemClock.elapsedRealtime() + "-" + edGreen.getText().toString());
			}

			factorFileName = "hts-chrono1.BLUEfactor";
			file = getApplicationContext().getFileStreamPath(factorFileName);
			if (file.exists()) {
				String currentFileContent = readData(findViewById(R.id.wrap_content), factorFileName);
				currentFileContent += "|" + SystemClock.elapsedRealtime() + "-" + edBlue.getText().toString();
				writeData(findViewById(R.id.wrap_content), factorFileName, currentFileContent);
				currentFileContent = readData(findViewById(R.id.wrap_content), factorFileName);
			} else {
				writeData(findViewById(R.id.wrap_content), factorFileName, SystemClock.elapsedRealtime() + "-" + edBlue.getText().toString());
			}

			factorFileName = "hts-chrono1.WHITEfactor";
			file = getApplicationContext().getFileStreamPath(factorFileName);
			if (file.exists()) {
				String currentFileContent = readData(findViewById(R.id.wrap_content), factorFileName);
				currentFileContent += "|" + SystemClock.elapsedRealtime() + "-" + edWhite.getText().toString();
				writeData(findViewById(R.id.wrap_content), factorFileName, currentFileContent);
				currentFileContent = readData(findViewById(R.id.wrap_content), factorFileName);
			} else {
				writeData(findViewById(R.id.wrap_content), factorFileName, SystemClock.elapsedRealtime() + "-" + edWhite.getText().toString());
			}
		});
	}

	public void extractPresetsFromJson() {
        SharedPreferences prefs_presets = getSharedPreferences(PRESETS_DEFINITION, 0);

		spnPresetsArrayList.clear();
        Map<String,?> keys = prefs_presets.getAll();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            spnPresetsArrayList.add(entry.getKey());
            //Log.d("map values",entry.getKey() + ": " + entry.getValue().toString());
        }
		Collections.sort(spnPresetsArrayList);
		spnPresetsAdapter.notifyDataSetChanged();
	}

	public void logIncomingData(String sBuffer) {
		SharedPreferences sps = getSharedPreferences(SHAREDPREFS_COMMS_LOG, 0);
		SharedPreferences.Editor spsEditor = sps.edit();

		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		long lMillisEpoch = timestamp.getTime();

		spsEditor.putString(String.valueOf(lMillisEpoch), sBuffer);
		spsEditor.apply();

	}

	public void decodeUSBResponse (String sReply) {
		//makeToast("Entering decodeBTResponse: '" + sReply + "'");
		//Log.e(TAG, sReply);

		//String sDecodedReply = sReply;
        logIncomingData(sReply);

        if (!sReply.contains(",")) {
            return;
        }

        String sPrefix = "";

		try {
			sPrefix = sReply.substring(0, sReply.indexOf(",")); // expecting "RGBW"
			//makeToast("sPrefix:" + sPrefix);
		} catch (StringIndexOutOfBoundsException e) {
			makeToast("Unable to find ',' in string: " + sReply);
		}
		String sDataPart = sReply.substring(sReply.indexOf(",")+1); // expecting eg "J,255,0,234,123,..."
		if (sPrefix.equals("RGBW")) {
			//makeToast("Decoding response, 'RGBW' found");

			String[] sDataArray = sDataPart.split(",");
            if ((sDataArray[0]).equals("A")) {
                //process the reply for 'A'
                //makeToast("Firmware reports having " + sDataArray[1] + " presets currently stored in flash memory.");
				requestPresets(Integer.valueOf(sDataArray[1]));

            } else if ((sDataArray[0]).equals("V")) {
                //process the reply for 'V'
				//makeToast("Processing preset response " + sReplyDataPart + ". Length: " + sDataArray.length);
				if (sDataArray.length == 12) {
					populatePresetItem(sDataPart);
					Collections.sort(spnPresetsArrayList);
                    spnPresetsAdapter.notifyDataSetChanged();
				}
            } else if ((sDataArray[0]).equals("FW")) {
				//process the reply for 'FW'
				//makeToast("Processing preset response " + sReplyDataPart + ". Length: " + sDataArray.length);
				sFWver = sDataArray[1];

			} else if ((sDataArray[0]).equals("U")) {
				//makeToast("Processing data part: '" + sDataPart + ". Length: " + sDataPart.length());
				String[] arrCurrentLEDstate = sDataPart.split(",",11);
				SharedPreferences spsControllerData = getSharedPreferences(SHAREDPREFS_ONE_OFF_SEEKBARS, 0);
				SharedPreferences.Editor spsEditor = spsControllerData.edit();
				spsEditor.clear();
				spsEditor.apply();

				spsEditor.putInt("seekBar1", Integer.valueOf(arrCurrentLEDstate[1]));
				spsEditor.putInt("seekBar2", Integer.valueOf(arrCurrentLEDstate[2]));
				spsEditor.putInt("seekBar3", Integer.valueOf(arrCurrentLEDstate[3]));
				spsEditor.putInt("seekBar4", Integer.valueOf(arrCurrentLEDstate[4]));
				spsEditor.putInt("seekBar5", Integer.valueOf(arrCurrentLEDstate[5]));
				spsEditor.putInt("seekBar6", Integer.valueOf(arrCurrentLEDstate[6]));
				spsEditor.putInt("seekBar7", Integer.valueOf(arrCurrentLEDstate[7]));
				spsEditor.putInt("seekBar8", Integer.valueOf(arrCurrentLEDstate[8]));
				spsEditor.putInt("seekBar9", Integer.valueOf(arrCurrentLEDstate[9]));
				spsEditor.putInt("seekBar10", Integer.valueOf(arrCurrentLEDstate[10]));
				spsEditor.apply();
				refreshSeekBars();
				
			} else if ((sDataArray[0]).equals("LAMPS")) {
				//process the reply for 'LAMPS'
				//this will return e.g. "RGBW,LAMPS,0,1,0,1" this means lamps 2 and 4 are on
				SharedPreferences spsControllerData = getSharedPreferences(SHAREDPREFS_LAMP_STATE, 0);
				SharedPreferences.Editor spsEditor = spsControllerData.edit();
				spsEditor.clear(); //Delete previous presets
				spsEditor.apply();

				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				long lMillisEpoch = timestamp.getTime();

				String sTemp = sDataPart;

				spsEditor.putString("LAMPS", sTemp);
				spsEditor.putLong("timestamp", lMillisEpoch);
				spsEditor.apply();

				makeToast("Current lamp state read.\nTimestamp: " + timestamp);

			} else if ((sDataArray[0]).equals("FILE")) {
				//process the reply for 'FILE'
				//makeToast("Processing preset response " + sReplyDataPart + ". Length: " + sDataArray.length);
				SharedPreferences spsControllerData = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
				SharedPreferences.Editor spsEditor = spsControllerData.edit();
				spsEditor.clear(); //Delete previous presets
				spsEditor.apply();

				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				long lMillisEpoch = timestamp.getTime();

				String sTemp = sDataPart;

				//makeToast("Filesize: " + sTemp.length());
				if (sTemp.length() < 10) {
					factoryReset();
					Toast.makeText(getBaseContext(), "Controller reset to factory settings. Please close the app and reconnect the LED2match.", Toast.LENGTH_LONG).show();
				}

				//sTemp = sTemp.substring(0, sTemp.lastIndexOf(","));
				try {
					sTemp = sTemp.replaceAll(";\\}", "\\}");
				} catch ( PatternSyntaxException e) {
					makeToast("Unable to find ';}' in string: " + sTemp);
				}

				try {
					sTemp = sTemp.substring(sTemp.indexOf(",")+1); // expecting "RGBW"
				} catch (StringIndexOutOfBoundsException e) {
					makeToast("Unable to find ',' in string: " + sTemp);
				}

				spsEditor.putString("JSON", sTemp);
				spsEditor.putLong("timestamp", lMillisEpoch);
				spsEditor.apply();
				makeToast("Data from controller refreshed.\nTimestamp: " + timestamp);

			} else if ((sDataArray[0]).equals("J")) {

				String sValues = sDataPart.substring(sDataPart.indexOf(",")+1); // expecting eg "255,0,234,123,..."

            	// Save the values to SharedPrefs File together with a current timetstamp
				SharedPreferences spsLEDtimers = getSharedPreferences(SHAREDPREFS_LED_TIMERS, 0);
				SharedPreferences.Editor spsEditor = spsLEDtimers.edit();
				spsEditor.clear(); //Delete previous presets
				spsEditor.apply();

				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				long lMillisEpoch = timestamp.getTime();

				spsEditor.putString("currTimers", sValues + "|" + lMillisEpoch );
				spsEditor.apply();

			}
        }
		//PopupAsyncTask asyncTask = new PopupAsyncTask(context, "");
		//asyncTask.execute();
        //arrReply.add(i, sLamps[i]);
    }

    public String extractJSONvalue(String sJSONbody_ref, String sKeyScanned) {
		String sReturn = "";
		String sJSONbody = getJsonBody(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE);
		if (sJSONbody.length() == 0) {
			return sReturn;
		}

		try {
			JSONObject jsonStructure = new JSONObject(sJSONbody);
			Iterator<String> iter = jsonStructure.keys();
			while (iter.hasNext()) {
				String sKey = iter.next();
				try {
					if (sKey.equals(sKeyScanned)) {
						Object objValue = jsonStructure.get(sKey);
						//makeToast("key = " + key + ", value = " + value.toString());
						sReturn = objValue.toString();
					}
				} catch (JSONException e) {
					// Something went wrong!
				}
			}
		} catch (JSONException j) {
			makeToast("Unable to parse string to JSON object");
		}
		return sReturn;
	}

    public void populatePresetItem(String sPresetLine) {
		String[] arrColour = sPresetLine.split(",",3);

		if (arrColour.length > 1) {
			String sCommand = arrColour[0]; //'V'
			String sPresetName = arrColour[1]; //e.g. GTI+
			String sColours = arrColour[2]; //e.g. 255,255,1,100,100,0,255,200,100,10
			SharedPreferences spsValues = getSharedPreferences(APP_SHAREDPREFS_READ, 0);
			SharedPreferences.Editor spsEditor = spsValues.edit();
			spsEditor.putString(sPresetName.toUpperCase(), sColours);
			spsEditor.commit();
			Log.d(TAG, "Populating preset in APP_SHAREDPREFS_READ:" + sPresetName.toUpperCase() + ": " + sColours);
			//makeToast("Populating preset in APP_SHAREDPREFS_READ:" + sPresetName.toUpperCase() + ": " + sColours);
			spnPresetsArrayList.add(sPresetName.toUpperCase());
		} else {
			Log.d(TAG, "populatePresetItem: Unable to split controller reply by comma - no comma in the response");
		}

	}

    public void requestPresets(int iCounter) {
		Log.d(TAG, "Executing function requestPresets");
		spnPresetsArrayList.clear();
		SharedPreferences spsValues = getSharedPreferences(APP_SHAREDPREFS_READ, 0);
		SharedPreferences.Editor spsEditor = spsValues.edit();
		spsEditor.clear(); //Delete previous presets
		spsEditor.commit();

		Log.d(TAG, "APP_SHAREDPREFS_READ file cleared");

		//For each pair of key-> value add item to the list
		//refresh the list and the spinner
		for (int i = 11; (i <= 10 + iCounter); i++) {
			Log.d(TAG, "Sending 'V" + i + "'");
			sendDataOverSerial("V" + i + sNewLine);
			SystemClock.sleep(100);
		}
		Log.d(TAG, "APP_SHAREDPREFS_READ populated");
		Collections.sort(spnPresetsArrayList);
		spnPresetsAdapter.notifyDataSetChanged();
	}


    public void refresh_controller_data(View view) {

        //popupWaitDialog();
		//sendDataOverSerialAsync(sNewLine);
		String sCommand = "F" + sNewLine;
		lclBTServiceInstance.sendData(sCommand);
		sendDataOverSerial(sCommand);
		SystemClock.sleep(700);
		getLampsState();
		getUnitName();
		onClickRead(null);
		refreshSeekBars();
    }

    public void getLampsState() {
		String sCommand = "U" + sNewLine;
		lclBTServiceInstance.sendData(sCommand);
		sendDataOverSerial(sCommand);
		SystemClock.sleep(200);
	}

	public void popupWaitDialog() {
		pDialog = new ProgressDialog(context); // this = YourActivity
		pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pDialog.setTitle("Loading");
		pDialog.setMessage("Loading. Please wait...");
		pDialog.setIndeterminate(true);
		pDialog.setCanceledOnTouchOutside(false);
		pDialog.show();
	}


	public String getJsonBody(String sSharedPrefsFilename) {
		SharedPreferences spsValues = getSharedPreferences(sSharedPrefsFilename, 0);
		return spsValues.getString("JSON", "");

	}

	public long getJsonBodyTimestamp(String sSharedPrefsFilename) {
	    SharedPreferences spsValues = getSharedPreferences(sSharedPrefsFilename, 0);
		return spsValues.getLong("timestamp", 0);

	}

    public void onClickRead(View v) {
		/*String sCommand = "F" + sNewLine;
		sendDataOverSerial(sCommand);


		getLampsState();
		SystemClock.sleep(100);



		if (sJsonBody.length() == 0) {
			makeToast("No data found in '" + SHAREDPREFS_CONTROLLER_FILEIMAGE + "' file.");
			return;
		}*/

		SharedPreferences spsPresets = getSharedPreferences(APP_SHAREDPREFS_READ, 0);
		SharedPreferences.Editor spsEditor = spsPresets.edit();

		spsEditor.clear();
		String sJsonBody = getJsonBody(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE);
		String sValue = extractJSONvalue(sJsonBody, "preset_counter");
		makeToast("preset_counter: " + sValue);

		int iPresetCount = 0;

		if (sValue.length() != 0) {
			if (Integer.valueOf(sValue) == 0) {
				makeToast("No presets stored in the controller's memory.");
			}
			iPresetCount = Integer.valueOf(sValue);
		} else {
			makeToast("No presets stored in the controller's memory.");
		}

		for (int i=0; i<iPresetCount;i++) {
			int j = i+1;
			String sKeyName = "preset" + j + "_name";
			String sPresetName = extractJSONvalue(sJsonBody, sKeyName); //eg. "GTI+"
			sKeyName = "preset" + j + "_rgbw";
			String sPresetValue = extractJSONvalue(sJsonBody, sKeyName); //eg. "123,123,255,000,000,064,006,255,100,001"
			spsEditor.putString(sPresetName, sPresetValue);
			spnPresetsArrayList.add(sPresetName.toUpperCase());
		}

		spsEditor.commit();
		refreshItemsOnSpinner();
		SystemClock.sleep(50);
		//messageHandler.sendEmptyMessage(MSG_HIDE_PROGRESS_DIALOG);
	}

	public void sendDataOverSerial(String data) {
		Log.d (TAG, "sendDataOverSerial: '" + data.replaceAll("\r", "").replaceAll("\n", "") + "'");
		if (usbService != null) {
			//display.append(" >> " + data);
			usbService.write(data.getBytes());
		} else {
			makeToast("Serial port is not open. Unable to send.");
		}
	}

	/*public void sendDataOverBluetooth(String data) {
		if (btService != null) {
			try {
				serialSocket.write(data.getBytes());
			} catch (java.io.IOException e) {
				Log.d (TAG, "IOexception: ");
				e.printStackTrace();
			}

		} else {
			makeToast("Bluetooth connection is not open. Unable to send.");
		}
	}*/

    public String getRGBValues(String sPresetName) {
        String sPresetRGB = "";

        SharedPreferences spsValues = getSharedPreferences(PRESETS_DEFINITION, 0);
        Map<String, ?> allEntries = spsValues.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String sVal = entry.getKey();
            if (sVal.equalsIgnoreCase(sPresetName)) {
                String sValue = entry.getValue().toString();
                sPresetRGB = convertRGBwithCommasToHexString(sValue);
            }
        }
        return sPresetRGB;
    }

    public String convertRGBwithCommasToHexString(String sRGB) {
        String sValue = "";

        String[] stringArray = sRGB.split(",");
        for (String numberAsString : stringArray) {
            int iValue = Integer.parseInt(numberAsString);
            sValue += String.format("%02X", iValue);
        }
        sValue = sValue.toUpperCase();
        return sValue;
    }

	private boolean isMyServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	public void factoryReset() {
		int iResult = -1;
		String sCommand = "T" + sNewLine;
		sendDataOverSerial(sCommand);
		SystemClock.sleep(250);
	}

	public void setUiEnabled(boolean bool) {
		if (bool) {
			btnSend.setBackgroundDrawable(getApplicationContext().getResources().getDrawable(R.drawable.buttonselector_main));
			stopButton.setBackgroundDrawable(getApplicationContext().getResources().getDrawable(R.drawable.buttonselector_main));
			startButton.setBackgroundDrawable(getApplicationContext().getResources().getDrawable(R.drawable.button_idle_disabled));
		} else {
			btnSend.setBackgroundDrawable(getApplicationContext().getResources().getDrawable(R.drawable.button_idle_disabled));
			stopButton.setBackgroundDrawable(getApplicationContext().getResources().getDrawable(R.drawable.button_idle_disabled));
			startButton.setBackgroundDrawable(getApplicationContext().getResources().getDrawable(R.drawable.buttonselector_main));
		}
		startButton.setEnabled(!bool);
		btnSend.setEnabled(bool);
		stopButton.setEnabled(bool);
	}

	public void onClickSend(View view) {
		Log.d (TAG, "CLICKSENDING");
		String sCommand = "S";
		String sColour = String.format("%02X", Integer.valueOf(edLED65.getText().toString()));
		sColour += String.format("%02X", Integer.valueOf(edLEDUVA.getText().toString()));
		sColour += String.format("%02X", Integer.valueOf(edLED50.getText().toString()));
		sColour += String.format("%02X", Integer.valueOf(edLED27.getText().toString()));
		sColour += String.format("%02X", Integer.valueOf(edWhite.getText().toString()));
		sColour += String.format("%02X", Integer.valueOf(edRed.getText().toString()));
		sColour += String.format("%02X", Integer.valueOf(edGreen.getText().toString()));
		sColour += String.format("%02X", Integer.valueOf(edBlue.getText().toString()));
		sColour += String.format("%02X", Integer.valueOf(edLED395.getText().toString()));
		sColour += String.format("%02X", Integer.valueOf(edLED420.getText().toString()));
		sCommand += sColour + "$" + sNewLine;

		if (!power_drain_check(sColour)) {
			return;
		}

		if (lclBTServiceInstance.connected) {
			Log.d(TAG, "Sending RGBW combination: '" + sColour.replace("\n", "\\n").replace("\r", "\\r") + "'");
			lclBTServiceInstance.sendData(sCommand);
		} else {
			Log.d(TAG, "Service btService not connected!");
		}
		usbService.write(sCommand.getBytes());

		//Check if the TL84 preset has been selected - if yes, switch on additional lamp (S11 command)
		try {
			if (spinner_control_preset_list.getSelectedItem().toString().equalsIgnoreCase(Constants.TL84_TAG)) {
				sCommand = "S11100";
				sCommand = "S11100" + get_tl84_delay() + "$" + sNewLine;
				//if (btService.connected) {
				if (mBoundBT) {
					Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
					lclBTServiceInstance.sendData(sCommand);
				} else {
					Log.d(TAG, "Service btService not connected!");
				}
				usbService.write(sCommand.getBytes());
			}
		} catch (NullPointerException e) {
			Log.i (TAG, "Pressing SEND on a null SpinnerPreset");
			e.printStackTrace();
		}


		Toast.makeText(getBaseContext(), "Color applied!", Toast.LENGTH_SHORT).show();
	}



	@Override
	public void onStart() {
		super.onStart();
	}

    private Boolean power_drain_check(String sPresetRGBValues) {
        Integer light_power = check_light_power(sPresetRGBValues);
        //Float max_power = get_max_power();
        int max_power = get_max_power();

        if (max_power == 0) {

            String msg = "No PSU definition found!\nUsing a default value of " + Constants.DEFAULT_PSU_POWER/1000.0 + "A";
            makeToast(msg);
            max_power = DEFAULT_PSU_POWER;
        }

        if (light_power/100 > max_power/100) {
            String toast = getString(R.string.light_power_warning);
            toast = toast.replace("%light_power%", String.format(Locale.US, "%.1f", light_power/1000.0));
            toast = toast.replace("%psu_current%", String.format(Locale.US, "%.1f", max_power/1000.0));
            //makeToast(toast);
            display_popup_message("Power drain warning!", toast);
            return false;
        } else {
            return true;
        }
    }

	private Integer check_light_power(String sPresetRGBValues) {
		Integer iPower = 0;
		Integer i = 0;
        /*"Assign specific power drain to indidual LEDs:
        Each Count 255:

        1.LED65 1.44A
        2.LED50 1.44A
        3.LED27 1.48A
        4.Red 1.0A
        5.Green 0.88A
        6.Blue  0.86A
        7.White 1.04A
        8.UVA 0.98A
        9.385 0.83A
        10.420 0.78A"*/

		while (!TextUtils.isEmpty(sPresetRGBValues)) {
			//Log.d (TAG, "checking light power of: " + sPresetRGBValues + ". Power so far: " + iPower);
			int iDecimal = Integer.parseInt(sPresetRGBValues.substring(0, 2), 16);
			i++;

			if (i==1 || i ==2) {
				int iFullPower = 1440;
				iPower += (int)Math.round((1.0 * iDecimal / 255)*iFullPower);
			} else if (i==3) {
				int iFullPower = 1480;
				iPower += (int)Math.round((1.0 * iDecimal / 255)*iFullPower);
			} else if (i==4) {
				int iFullPower = 1000;
				iPower += (int)Math.round((1.0 * iDecimal / 255)*iFullPower);
			} else if (i==5) {
				int iFullPower = 880;
				iPower += (int)Math.round((1.0 * iDecimal / 255)*iFullPower);
			} else if (i==6) {
				int iFullPower = 860;
				iPower += (int)Math.round((1.0 * iDecimal / 255)*iFullPower);
			} else if (i==7) {
				int iFullPower = 1040;
				iPower += (int)Math.round((1.0 * iDecimal / 255)*iFullPower);
			} else if (i==8) {
				int iFullPower = 980;
				iPower += (int)Math.round((1.0 * iDecimal / 255)*iFullPower);
			} else if (i==9) {
				int iFullPower = 830;
				iPower += (int)Math.round((1.0 * iDecimal / 255)*iFullPower);
			} else if (i==10) {
				int iFullPower = 780;
				iPower += (int)Math.round((1.0 * iDecimal / 255)*iFullPower);
			}
			//Log.d (TAG, iDecimal + " / 210 = " + (int)Math.round(1000 * iDecimal / 210) + " mA");
			sPresetRGBValues = sPresetRGBValues.substring(2);
		}
		Log.d (TAG, "No more preset light to check. Overall light power is: " + iPower);
		return iPower;
	}

	public String get_tl84_delay() {
		SharedPreferences config_prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, 0);
		Log.d(TAG, " ** TL84_delay from file: " + String.format(Locale.US, "%04d", config_prefs.getInt(TL84_DELAY_KEY, 0)));
		return String.format(Locale.US, "%04d", config_prefs.getInt(TL84_DELAY_KEY, 0));

	}

	/*private Float get_max_power() {
		SharedPreferences spFile = getSharedPreferences(Constants.PREFS_PSU_CURRENT, 0);
		Float fPower = spFile.getFloat("psu_current", 0.0f) * 1000;
		Log.d (TAG, "Max power for this PSU is " + fPower);
		return fPower;
	}*/

	private int get_max_power() {
		SharedPreferences spFile = getSharedPreferences(Constants.PREFS_PSU_CURRENT, 0);
		//Float fPower = spFile.getFloat("psu_current", 0.0f) * 1000;
		int power = 0;
		try {
			power = spFile.getInt("psu_current", 0);
		} catch (NumberFormatException e) {
			makeToast("Unable to get the stored PSU power value");
		}
		return power;
	}

	private void display_popup_message(String title, String message) {

		AlertDialog dlg = new AlertDialog.Builder(this).create();
		dlg.setTitle(title);
		dlg.setMessage(message);
		dlg.setIcon(R.drawable.icon_main);
		dlg.show();
	}

	public void refreshItemsOnSpinner() {

		SharedPreferences spsValues = getSharedPreferences(APP_SHAREDPREFS_READ, 0);
		String sVal;

		Log.d(TAG, "Reading APP_SHAREDPREFS_READ and refreshing spnPresetsArrayList and spinnerPresets");
		spnPresetsArrayList.clear();
		//read in APP_SHAREDPREFS_READ;
		//For each pair of key-> value add item to the list
		//refresh the list and the spinner
		Map<String, ?> allEntries = spsValues.getAll();
		for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
			sVal = entry.getKey();
			spnPresetsArrayList.add(sVal);
		}
		Collections.sort(spnPresetsArrayList);
		spnPresetsAdapter.notifyDataSetChanged();
	}

	public static boolean check_eeprom_populated() {
		boolean blEEPROM_populated = false;
		String sEEPROMbyte = LightSettings.bluetoothAskReply("Y");
		//Toast.makeText(this.getBaseContext(),"Application title '" + unitName + "' set.", Toast.LENGTH_SHORT).show();
		Log.d(TAG, "EEPROM first byte:" + sEEPROMbyte);
		if (sEEPROMbyte.equals("")) sEEPROMbyte = Integer.toString(255);
		if (Integer.parseInt(sEEPROMbyte) < 255) {
			blEEPROM_populated = true;
		}
		return blEEPROM_populated;
	}

	@Override
	public void onRestart() {
		super.onRestart();

		//makeToast("onRestart()");
		getJSONFile();
		getLampsState();

		/*new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				// this code will be executed after 750 mseconds

				getUnitName();
				populateLampNames();
			}
		}, 1000);*/
	}

	@Override
	public void onResume() {

		h.postDelayed( runnable = () -> {
			getJSONFile();
			getLampsState();
			h.postDelayed(runnable, delay);
		}, delay);

		extractPresetsFromJson();
		getUnitName();
		super.onResume();
        setFilters();  // Start listening notifications from UsbService
		Log.d(TAG, "starting USB connection service");
        startService(UsbCOMMsService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("request_preset_event"));

        if (!mBoundBT) {
            Intent intent = new Intent(this, BtCOMMsService.class);
            //startService(intent);
            bindService(intent, btConnection, Context.BIND_AUTO_CREATE);
            //Toast.makeText(this.getBaseContext(),"Service bound (onResume)", Toast.LENGTH_SHORT).show();

            SystemClock.sleep(100);
            mBoundBT = true;
        }
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Extract data included in the Intent
			String message = intent.getStringExtra("counter");
			Log.d(TAG, "Got message: " + message);
		}
	};

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        //if (!UsbCOMMsService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            //startService.putExtra("USB_connection", usbConnection);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        //}
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }



    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbCOMMsService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbCOMMsService.ACTION_NO_USB);
        filter.addAction(UsbCOMMsService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbCOMMsService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbCOMMsService.ACTION_USB_PERMISSION_NOT_GRANTED);
        filter.addAction(UsbCOMMsService.ACTION_USB_READY);
		filter.addAction(UsbCOMMsService.ACTION_USB_ATTACHED);
		filter.addAction(MSG_SET_SEEKBAR_PROGRESS);
        registerReceiver(mUsbReceiver, filter);
    }

    private void refreshSeekBars() {
		SharedPreferences spsFile = getSharedPreferences(SHAREDPREFS_ONE_OFF_SEEKBARS, 0);
		//SharedPreferences.Editor spsEditor = spsFile.edit();

		if (spsFile.contains("seekBar1")) {
			barLED65.setProgress(spsFile.getInt("seekBar1", 0));
			edLED65.setText(Integer.toString(spsFile.getInt("seekBar1", 0)));
		}
		
		if(spsFile.contains("seekBar2")) {
			barLEDUVA.setProgress(spsFile.getInt("seekBar2", 0));
			edLEDUVA.setText(Integer.toString(spsFile.getInt("seekBar2", 0)));
		}

		if(spsFile.contains("seekBar3")) {
			barLED50.setProgress(spsFile.getInt("seekBar3", 0));
			edLED50.setText(Integer.toString(spsFile.getInt("seekBar3", 0)));
		}

		if(spsFile.contains("seekBar4")) {
			barLED27.setProgress(spsFile.getInt("seekBar4", 0));
			edLED27.setText(Integer.toString(spsFile.getInt("seekBar4", 0)));
		}

		if(spsFile.contains("seekBar5")) {
			barWhite.setProgress(spsFile.getInt("seekBar5", 0));
			edWhite.setText(Integer.toString(spsFile.getInt("seekBar5", 0)));
		}

		if(spsFile.contains("seekBar6")) {
			barRed.setProgress(spsFile.getInt("seekBar6", 0));
			edRed.setText(Integer.toString(spsFile.getInt("seekBar6", 0)));
		}

		if(spsFile.contains("seekBar7")) {
			barGreen.setProgress(spsFile.getInt("seekBar7", 0));
			edGreen.setText(Integer.toString(spsFile.getInt("seekBar7", 0)));
		}

		if(spsFile.contains("seekBar8")) {
			barBlue.setProgress(spsFile.getInt("seekBar8", 0));
			edBlue.setText(Integer.toString(spsFile.getInt("seekBar8", 0)));
		}

		if(spsFile.contains("seekBar9")) {
			barLED395.setProgress(spsFile.getInt("seekBar9", 0));
			edLED395.setText(Integer.toString(spsFile.getInt("seekBar9", 0)));
		}

		if(spsFile.contains("seekBar10")) {
			barLED660.setProgress(spsFile.getInt("seekBar10", 0));
			edLED420.setText(Integer.toString(spsFile.getInt("seekBar10", 0)));
		}

		//spsEditor.clear();
		//spsEditor.commit();

		/*if (blUSBConnected) {
			onClickSend(null);

		}*/

	}

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    public static class MyHandler extends Handler {
        private final WeakReference<LightSettings> mActivity;

        public MyHandler(LightSettings activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbCOMMsService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    mActivity.get().display.append(data);
                    String sVal = data.getBytes().toString();
					//Toast.makeText(mActivity.get(), "Data: " + data, Toast.LENGTH_SHORT).show();
					mActivity.get().USBconcatResponse(data);
                    break;
				//case BtCOMMsService.
				case MSG_HIDE_PROGRESS_DIALOG:
					Toast.makeText(mActivity.get(), "Trying to hide dialog", Toast.LENGTH_SHORT).show();
					break;
//				case UsbService.ACTION_USB_ATTACHED:
//					Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
//					break;
            }
        }
    }

	@Override
	public void onPause() {
		h.removeCallbacks(runnable); //stop handler when activity not visible

        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);

        if (isMyServiceRunning(BtCOMMsService.class)) {
        	//Log.d(TAG, "BtCOMMS srvc is running. Unregistering and unbinding from btService");
			//unregisterReceiver(btReceiver);
			//unbindService(btConnection);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//clearSharedPrefsFile(SHAREDPREFS_LED_TIMERS);
        if (isMyServiceRunning(BtCOMMsService.class)) {
            Log.d(TAG, "BtCOMMS srvc is running. Unregistering and unbinding from btService");
            try {
				unregisterReceiver(btReceiver);
				unregisterReceiver(btReceiverBTdevice);
			} catch (IllegalArgumentException e) {
            	Log.d (TAG, "IllegalArgumentException");
            	e.printStackTrace();
			}

            unbindService(btConnection);
        }
		if (Debug) {
			// Log.e(TAG, "--- ON DESTROY ---");
		}
	}

	public void openSeqLayout (final View view) {
		Intent intent = new Intent(LightSettings.this, SequenceProgramming.class);
		startActivity(intent);
	}

	public void openDialog(final View view) {

		if (BtCore.Connected() || blUSBConnected || true) {
			LayoutInflater layoutInflater = LayoutInflater.from(context);
			View promptView = layoutInflater.inflate(R.layout.prompts, null);
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

			// set prompts.xml to be the layout file of the alertdialog builder
			alertDialogBuilder.setView(promptView);
			final EditText edtInput = promptView.findViewById(R.id.passInput);

			// setup a dialog window
			alertDialogBuilder
					.setCancelable(false)
					.setPositiveButton("OK", (dialog, id) -> {
						//get user input and set it to result

						if (edtInput.getText().toString().equals(password)) {

							getJSONFile();
							//String sCommand = "J" + sNewLine;
							
							/*int iResult = usbService.sendBytes(sCommand.getBytes());
							if (iResult < 0) {
							Toast.makeText(context, "Stream was null, no request sent", Toast.LENGTH_SHORT).show();
							} else {
							Toast.makeText(context, "I think I've sent " + iResult + " bytes.", Toast.LENGTH_SHORT).show();
							}*/

							Intent intent = new Intent(LightSettings.this, ServicePageActivity.class);
							startActivity(intent);
							// Intent sequencerIntent = new Intent(this, BtSequencerActivity.class);
							// startActivity(sequencerIntent);
						} else {
							Message msg = new Message();
							msg.what = MSG_SHOW_TOAST;
							msg.obj = "Password incorrect";
							messageHandler.sendMessage(msg);
						}
					})
					.setNegativeButton("Cancel",
							(dialog, id) -> dialog.cancel());

			// create an alert dialog
			AlertDialog alertD = alertDialogBuilder.create();
			alertD.show();
		} else {
			Toast.makeText(this, "goto_recertification: Please connect to the RGB LED first.", Toast.LENGTH_SHORT).show();
		}
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case 0:
			startScannerActivity();
			break;

		 case 1:
			 openDialog(findViewById(R.id.wrap_content));
			 break;

		case 2:
			openAboutDialog();
			break;

		case 3:
			openJSONReport();
			break;

		case 4:
			Intent intent4 = new Intent(LightSettings.this, TRSDigitalPanel.class);
			startActivity(intent4);
			break;

		case 5:
			Intent intent5 = new Intent(LightSettings.this, TRSSequence.class);
			startActivity(intent5);
			break;

		case 6:
			Intent intent6 = new Intent(LightSettings.this, TRSSettings.class);
			startActivity(intent6);
			break;

		case 7:
			Intent intent7 = new Intent(LightSettings.this, TRSLightOperatingHours.class);
			startActivity(intent7);
			break;

		}
		return true;
	}



	//
	// private void openSequencerView()
	// {
	// Intent sequencerIntent = new Intent(this, BtSequencerActivity.class);
	// startActivity(sequencerIntent);
	// }

	private void openJSONReport() {

		String sJSONbody = getJsonBody(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE);
		long lJSONFileTimestamp = getJsonBodyTimestamp(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE);

		Date date = new Date(lJSONFileTimestamp); // *1000 is to convert seconds to milliseconds
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm"); // the format of your date
		String formattedDate = sdf.format(date);

		AlertDialog dlg = new AlertDialog.Builder(this).create();
		String sFWver = extractJSONvalue("", "firmware_version");
		dlg.setTitle("FW v" + sFWver + " (" + sJSONbody.length() + " bytes)\n" + formattedDate);
		dlg.setMessage(sJSONbody);
		dlg.setIcon(R.drawable.icon_main);
		dlg.show();
	}

	private String getFWver_JSON() {
		String sReturn = "";
		sReturn = extractJSONvalue("", "firmware_version");

		return sReturn;
	}

	public void btnClicked(View v) {
		SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
		String sPresetRGBValues = "000";
		String sCommand = "";
		JSON_analyst json_analyst = new JSON_analyst(spFile);

		switch_all_off();

		switch (v.getId()) {
			case R.id.btnL1:
				blLamp1_ON = true;
				btnL1.setBackgroundResource(R.drawable.buttonselector_active);
				btnL1.setTextColor(Color.BLACK);

				sPresetRGBValues = json_analyst.getJSONValue("preset1_rgbw");
				String[] sRGB = sPresetRGBValues.split(",");
				for (int i=0; i < sRGB.length; i++) {
					if (i < 9) {
						sCommand = "S0" + (i+1) + sRGB[i] + sNewLine;
						usbService.write(sCommand.getBytes());
					} else {
						sCommand = "S" + (i+1) + sRGB[i] + sNewLine;
						usbService.write(sCommand.getBytes());
					}
					//Toast.makeText(context, sCommand, Toast.LENGTH_LONG).show();
				}

				barLED65.setProgress(Integer.valueOf(sRGB[0]));
				barLEDUVA.setProgress(Integer.valueOf(sRGB[1]));
				barLED50.setProgress(Integer.valueOf(sRGB[2]));
				barLED27.setProgress(Integer.valueOf(sRGB[3]));
				barWhite.setProgress(Integer.valueOf(sRGB[4]));
				barRed.setProgress(Integer.valueOf(sRGB[5]));
				barGreen.setProgress(Integer.valueOf(sRGB[6]));
				barBlue.setProgress(Integer.valueOf(sRGB[7]));
				barLED395.setProgress(Integer.valueOf(sRGB[8]));
				barLED660.setProgress(Integer.valueOf(sRGB[9]));

				break;

			case R.id.btnL2:
				blLamp2_ON = true;
				btnL2.setBackgroundResource(R.drawable.buttonselector_active);
				btnL2.setTextColor(Color.BLACK);

				sPresetRGBValues = json_analyst.getJSONValue("preset2_rgbw");
				sRGB = sPresetRGBValues.split(",");
				for (int i=0; i < sRGB.length; i++) {
					if (i < 9) {
						sCommand = "S0" + (i+1) + sRGB[i] + sNewLine;
						usbService.write(sCommand.getBytes());
					} else {
						sCommand = "S" + (i+1) + sRGB[i] + sNewLine;
						usbService.write(sCommand.getBytes());
					}
					//Toast.makeText(context, sCommand, Toast.LENGTH_LONG).show();
				}

				barLED65.setProgress(Integer.valueOf(sRGB[0]));
				barLEDUVA.setProgress(Integer.valueOf(sRGB[1]));
				barLED50.setProgress(Integer.valueOf(sRGB[2]));
				barLED27.setProgress(Integer.valueOf(sRGB[3]));
				barWhite.setProgress(Integer.valueOf(sRGB[4]));
				barRed.setProgress(Integer.valueOf(sRGB[5]));
				barGreen.setProgress(Integer.valueOf(sRGB[6]));
				barBlue.setProgress(Integer.valueOf(sRGB[7]));
				barLED395.setProgress(Integer.valueOf(sRGB[8]));
				barLED660.setProgress(Integer.valueOf(sRGB[9]));

				break;

			case R.id.btnL3:
				blLamp3_ON = true;
				btnL3.setBackgroundResource(R.drawable.buttonselector_active);
				btnL3.setTextColor(Color.BLACK);

				sPresetRGBValues = json_analyst.getJSONValue("preset3_rgbw");
				sRGB = sPresetRGBValues.split(",");
				for (int i=0; i < sRGB.length; i++) {
					if (i < 9) {
						sCommand = "S0" + (i+1) + sRGB[i] + sNewLine;
						usbService.write(sCommand.getBytes());
					} else {
						sCommand = "S" + (i+1) + sRGB[i] + sNewLine;
						usbService.write(sCommand.getBytes());
					}
					//Toast.makeText(context, sCommand, Toast.LENGTH_LONG).show();
				}

				barLED65.setProgress(Integer.valueOf(sRGB[0]));
				barLEDUVA.setProgress(Integer.valueOf(sRGB[1]));
				barLED50.setProgress(Integer.valueOf(sRGB[2]));
				barLED27.setProgress(Integer.valueOf(sRGB[3]));
				barWhite.setProgress(Integer.valueOf(sRGB[4]));
				barRed.setProgress(Integer.valueOf(sRGB[5]));
				barGreen.setProgress(Integer.valueOf(sRGB[6]));
				barBlue.setProgress(Integer.valueOf(sRGB[7]));
				barLED395.setProgress(Integer.valueOf(sRGB[8]));
				barLED660.setProgress(Integer.valueOf(sRGB[9]));

				break;
		}
	}

	public void switch_all_off() {
		blLamp1_ON = false;
		btnL1.setBackgroundResource(R.drawable.buttonselector_main);
		btnL1.setTextColor(Color.WHITE);
		blLamp2_ON = false;
		btnL2.setBackgroundResource(R.drawable.buttonselector_main);
		btnL2.setTextColor(Color.WHITE);
		blLamp3_ON = false;
		btnL3.setBackgroundResource(R.drawable.buttonselector_main);
		btnL3.setTextColor(Color.WHITE);
	}

	private void openAboutDialog() {
		//String sFWver = "N/A";
		//getFWver();
		String sFWverLcl = getFWver_JSON();
		String sFreeRam = "N/A";

		if (BtCore.Connected()) {
			Log.d(TAG, "bluetoothAskReply(V09)");
			sFWver = bluetoothAskReply("V09");
			sFreeRam = bluetoothAskReply("I");
		}

		String sFormattedDate = "";
		try {

			SimpleDateFormat sdfSource = new SimpleDateFormat("yyyy/MM/dd");
			Date date = sdfSource.parse(sAppVersionDate);
			SimpleDateFormat sdfDest = new SimpleDateFormat("dd MMMM yy");
			sFormattedDate = sdfDest.format(date);

		} catch (ParseException pe) {
			System.out.println("Parse exception:" + pe);
		}
		AlertDialog dlg = new AlertDialog.Builder(this).create();
		dlg.setTitle("LED2Match app " + apkVersion);
		dlg.setMessage("Hi-Tec-Support GmbH\nCopyright (R) 2018\nVersion " + sFormattedDate + "\nFirmware version: " + sFWverLcl);
		dlg.setIcon(R.drawable.icon_main);
		dlg.show();
	}

	public void fncRefreshRGB() {
		int rr, gg, bb, ww;

		rr = Integer.parseInt(edRed.getText().toString());
		gg = Integer.parseInt(edGreen.getText().toString());
		bb = Integer.parseInt(edBlue.getText().toString());
		ww = Integer.parseInt(edWhite.getText().toString());

		//Toast.makeText(this, (ww/2+rr/2)+","+(ww/2+gg/2)+","+(ww/2+bb/2), Toast.LENGTH_LONG).show();
		//botLay.setBackgroundColor(Color.rgb(rr, gg, bb));
		rgbLay.setBackgroundColor(Color.rgb((ww/2+rr/2), (ww/2+gg/2), (ww/2+bb/2)));
	}

	int rgbColor = 0;
	private void writeData(View view, String fileName, String sData) {
		BufferedWriter bufferWriter = null;

		//writeToFile(timeStamp, lampRecord);
		try {
			FileOutputStream fileWrite = openFileOutput(fileName, Context.MODE_PRIVATE);
			bufferWriter = new BufferedWriter(new OutputStreamWriter(fileWrite));
			bufferWriter.write(sData);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				assert bufferWriter != null;
				bufferWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	public static boolean blHasNewLine(String sBuffer) {
		String newline = "\n";
		boolean hasNewLine = sBuffer.contains(newline);
		if (sBuffer.length() > 0) {
			//makeToast("Buffer '" + sBuffer + "', hasnewline: " + hasNewLine);
		}
		return hasNewLine;
	}

	public static String bluetoothAskReply(String sCommand) {
		String[] arrResponse;

		String newline = System.getProperty("line.separator");
		boolean hasNewLine = sCommand.contains(newline);

		BtCore.sendMessageBluetooth(sCommand + '\n');
		BTCommsLog btLog = new BTCommsLog("IN", sCommand);
		btLog.appendMessage("IN<<", sCommand);
		String response = BtCore.receiveMessageBluetooth();
		String sLog = response.replaceAll("(\\r|\\n)", "");
		btLog.appendMessage("OUT>", sLog);
		String sReply = "";
		//Log.d(TAG, "bluetoothAskReply() - Received response:" + response);
		String[] lines = response.split("\\r?\\n");
		for (String value : lines) {
			if (value.lastIndexOf(",") > -1 ) {
				arrResponse = response.split(",", 2);
				if (arrResponse[0].equals("RGBW")) {
					sReply = arrResponse[1];
					sReply = sReply.replaceAll("(\\r|\\n)", "");
				}
			}
		}
		return sReply.trim();
	}

	private String readData(View view, String fileName) {
		String output = "";
		BufferedReader bufferedReader = null;
		StringBuilder result = new StringBuilder();
		try {
			FileInputStream fileInputStream = openFileInput(fileName);
			bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				result.append(line);// + "\r\n");
				output = line.trim();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bufferedReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return output;
	}

	private void sendPresetsOverBT () {
		SharedPreferences spsValues = getSharedPreferences(APP_SHAREDPREFS_WRITE, 0);
		//SharedPreferences spsValues = getSharedPreferences(PRESETS_DEFINITION, 0);
		String sVal;
		String sKey;
		ArrayList<String> encodedPresetNames = new ArrayList<>();
		ArrayList<String> encodedPresetRGBs = new ArrayList<>();

		//For each pair of key-> value add item to the list
		//refresh the list and the spinner
		Log.d(TAG, "BT Sending: O - to clear all presets on Controller\n");

		if (lclBTServiceInstance.connected) {
			String string = "O" + sNewLine;
			Log.d(TAG, "Service btService connected. Calling lclBTServiceInstance.sendData with message '" + string.replace("\n", "\\n").replace("\r", "\\r") + "'");
			lclBTServiceInstance.sendData(string);
		} else {
			Log.d(TAG, "Service btService not connected!");
		}
		SystemClock.sleep(50);

		Map<String, ?> allEntries = spsValues.getAll();
		int i = 1;
		for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
			sKey = entry.getKey();
			sVal = entry.getValue().toString();

			Log.d(TAG, "Sending '" + sVal + "' to convert to HEX");
			encodedPresetRGBs.add(convertRGBwithCommasToHexString(sVal));
			encodedPresetNames.add(sKey);

			/*String sNum = Integer.toString(i);
			if (sNum.length() == 1) { sNum = "0"+sNum; }
			Log.d(TAG, "Splitting entry from spsValues: sKey=" + sKey + ", sVal="+ sVal);
			Log.d(TAG, "Sending: Q" + sNum + "," + sKey + "," +sVal + "\n");
			String string  = "Q" + sNum + "," + sKey + "," +sVal + sNewLine;

			if (lclBTServiceInstance.connected) {
				Log.d(TAG, "Service btService connected. Calling lclBTServiceInstance.sendData with message '" + string.replace("\n", "\\n").replace("\r", "\\r") + "'");
				//lclBTServiceInstance.sendData(string);
			} else {
				Log.d(TAG, "Service btService not connected!");
			}

			i++;*/
		}

		String sSize;
		if (encodedPresetNames.size() < 10) {
			sSize =  "0" + encodedPresetNames.size();
		} else {
			sSize = String.valueOf(encodedPresetNames.size());
		}

		String sCommand = "Q" + sSize + TextUtils.join("", encodedPresetRGBs) + TextUtils.join(",", encodedPresetNames) + "$\n";
		Log.d(TAG, sCommand);
		if (lclBTServiceInstance.connected) {
			Log.d(TAG, "New way to send the presets: '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
			lclBTServiceInstance.sendData(sCommand);
		} else {
			Log.d(TAG, "Service btService not connected!");
		}
	}

	private void sendPresetsOverSerial () {
		SharedPreferences spsValues = getSharedPreferences(APP_SHAREDPREFS_WRITE, 0);
		String sReply = "";
		String sVal = "";
		String sKey = "";

		//For each pair of key-> value add item to the list
		//refresh the list and the spinner
		Log.d(TAG, "Sending: O - to clear all presets on Controller\n");
		//sendDataOverSerial("O" + sNewLine);
		sendDataOverSerial("O" + sNewLine);
		//sReply = bluetoothAskReply("O"); //clear presets before sending fresh ones
		SystemClock.sleep(50);

		Map<String, ?> allEntries = spsValues.getAll();
		int i = 1;
		for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
			sKey = entry.getKey();
			sVal = entry.getValue().toString();

			//get rid of initial number
			//sVal = sVal.substring(sVal.indexOf(",") +1 , sVal.length() - sVal.indexOf(",")+1);

			String sNum = Integer.toString(i);
			if (sNum.length() == 1) { sNum = "0"+sNum; }
			Log.d(TAG, "Splitting entry from spsValues: sKey=" + sKey + ", sVal="+ sVal);
			Log.d(TAG, "Sending: Q" + sNum + "," + sKey + "," +sVal + "\n");
			//sendDataOverSerial("Q" + sNum + "," + sKey + "," +sVal + sNewLine);
			sendDataOverSerial("Q" + sNum + "," + sKey + "," +sVal + sNewLine);
			//sReply = bluetoothAskReply("Q" + sNum + "," + sKey + "," +sVal);
			//sReply = USBAskReply("Q" + sNum + "," + sKey + "," +sVal);

			//BtCore.sendMessageBluetooth("Q1" + sNum + "," + sKey + "," +sVal + "\n");
			Log.d(TAG, "Receiving: " + sReply + "\n");
			i++;
		}
		Log.d(TAG, "Sending: Q - to move presets from flash to EEPROM on Controller\n");
		//makeToast("sendPresetsOverSerial - sent: " + (i-1) + " presets.");
		//BtCore.sendMessageBluetooth("Q" + "\n");

	}


	private void showSplash() {
		dlgProgressSplash = new ProgressDialog(this);
		dlgProgressSplash.setCancelable(false);

		dlgProgressSplash.setTitle("HTS");
		dlgProgressSplash.setMessage("Communicating...");
		dlgProgressSplash.show();
	}

	private void hideSplash() {
		dlgProgressSplash.dismiss();
	}

	public void USBconcatResponse(String sBuffer) {

		//Toast.makeText(this, "USBConcat:" + sBuffer, Toast.LENGTH_SHORT).show();
        if (sBuffer.length() > 0) {
            sUSBResponse = sUSBResponse.concat(sBuffer);

            if (blHasNewLine(sUSBResponse)){
                sUSBResponse = sUSBResponse.replaceAll("(\\r|\\n)", "");
                SerialCommsLog SC_log = new SerialCommsLog("IN << ", sUSBResponse);
                SC_log.appendMessage("IN << ", sUSBResponse);
                //makeToast("USBconcatResponse: " + sUSBResponse);
				//Toast.makeText(this, "CRLF found. String terminated:" + sUSBResponse, Toast.LENGTH_SHORT).show();
                decodeUSBResponse(sUSBResponse);

                sUSBResponse = "";
            }
        }
	}

	static public class MyAsyncTask extends AsyncTask<Void, Void, String> {

		private Context mContext;
		private UsbCOMMsService mUsbService;

		private String sBuffer;

		public MyAsyncTask(Context context, UsbCOMMsService usbService, String buffer) {
			mContext = context;
			mUsbService = usbService;
			sBuffer = buffer;
		}
		@Override protected String doInBackground(Void... params) {
			String sResult = "";

			if (mUsbService != null) {
				SerialCommsLog SC_log = new SerialCommsLog("OUT", sBuffer);
				SC_log.appendMessage("OUT >> ", sBuffer);
				mUsbService.write(sBuffer.getBytes());
			} else {
				//makeToast("Serial port is not open. Unable to send.");
			}

			return sResult;

		}
		@Override protected void onPostExecute(String result) {

		}
	}

	public class JSONFileRefresh extends AsyncTask<Void, Void, String> {

		private Context mContext;
		private String sBuffer;

		JSONFileRefresh(Context context) {
			mContext = context;
		}

		protected void onPreExecute() {
			super.onPreExecute();
			sendDataOverSerial("F" + sNewLine);
			//alertDialog = new AlertDialog.Builder(YourClasss.this);
		}


		@Override
		protected String doInBackground(Void... params) {
			String sResult = "";
			return sResult;

		}
		@Override protected void onPostExecute(String result) {
			super.onPostExecute(result);
            getLampsState();
			//mHandler.sendEmptyMessage(MSG_HIDE_PROGRESS_DIALOG);

		}
	}

	public class PopupAsyncTask extends AsyncTask<Void, Void, String> {

		private Context mContext;
		private String sBuffer;

		public PopupAsyncTask( Context context, String buffer) {
			mContext = context;
			//sBuffer = buffer;
		}

		protected void onPreExecute() {
			super.onPreExecute();

			//alertDialog = new AlertDialog.Builder(YourClasss.this);
		}


		@Override
		protected String doInBackground(Void... params) {
			return "";

		}
		@Override protected void onPostExecute(String result) {
			super.onPostExecute(result);
			//mHandler.sendEmptyMessage(MSG_HIDE_PROGRESS_DIALOG);

		}
	}


	class firstTask extends TimerTask {
		@Override
		public void run(){
			LightSettings.this.runOnUiThread(() -> {
				long starttime = 0;
				long millis = System.currentTimeMillis() - starttime;
				int seconds = (int) (millis/1000);
			});
		}
	}

    public class MyRunnable implements Runnable {
		public MyRunnable(String parameter) {
		}



		public void run () {
			Log.d(TAG, "showing splash screen");
			dlgProgressSplash.show();

		}

		public String getResponse(String sCommand) {
			String sThreadedReply = "";

			sThreadedReply = bluetoothAskReply(sCommand);
			Log.d(TAG, "hiding dialogbox");
			dlgProgressSplash.dismiss();
			return sThreadedReply;
		}
	}


	public static class BTCommsLog {
		private String sMessage;
		private String sKey;


		BTCommsLog(String sKey, String sMessage) {this.sKey = sKey; this.sMessage = sMessage; }

		//public static SharedPreferences getSharedPreferences (Context ctxt) {
//			return ctxt.getSharedPreferences(BTCOMMSLOG_SHAREDPREFS, MODE_APPEND);
		//}


		void appendMessage(String sKey, String sMessage) {
			SharedPreferences spLogFile = MyApplication.getAppContext().getSharedPreferences(BTCOMMSLOG_SHAREDPREFS, 0);
			SharedPreferences.Editor spsEditor = spLogFile.edit();
			Date dteNow = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			String sTimeStamp = sdf.format(dteNow);

			spsEditor.putString(sKey +"|"+sTimeStamp, sTimeStamp + " # " + sMessage);
			spsEditor.apply();
		}


	}

	public static class SerialCommsLog {

		SerialCommsLog(String sKey, String sMessage) {

		}

		void appendMessage(String sKey, String sMessage) {
			SharedPreferences spLogFile = MyApplication.getAppContext().getSharedPreferences(SERIAL_COMMS_LOG, 0);
			SharedPreferences.Editor spsEditor = spLogFile.edit();
			Date dteNow = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			String sTimeStamp = sdf.format(dteNow);

			spsEditor.putString(sKey +"|"+sTimeStamp, sTimeStamp + " # " + sMessage);
			spsEditor.apply();
		}


	}

	private class PopupThread extends Thread {
        Context cntxtParent;

        public PopupThread(Context context) {
            this.cntxtParent = context;
        }

		@Override
		public void run() {
			pDialog = new ProgressDialog(cntxtParent); // this = YourActivity
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setTitle("Loading");
			pDialog.setMessage("Loading. Please wait...");
			pDialog.setIndeterminate(true);
			pDialog.setCanceledOnTouchOutside(false);
			pDialog.show();
		}
	}
}