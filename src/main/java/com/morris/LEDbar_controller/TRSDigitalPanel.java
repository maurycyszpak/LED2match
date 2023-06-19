package com.morris.LEDbar_controller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
//import android.support.multidex.BuildConfig;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.morris.LEDbar_controller.BtCOMMsService.SHAREDPREFS_UNITNAME;
import static com.morris.LEDbar_controller.Constants.BT_CONNECTED_PREFS;
import static com.morris.LEDbar_controller.Constants.CONFIG_SETTINGS;
import static com.morris.LEDbar_controller.Constants.DEFAULT_PSU_POWER;
import static com.morris.LEDbar_controller.Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE;
import static com.morris.LEDbar_controller.Constants.S_COMMAND_SEPARATOR;
import static com.morris.LEDbar_controller.Constants.sNewLine;
import static com.morris.LEDbar_controller.TRSSettings.TL84_DELAY_KEY;

public class TRSDigitalPanel extends Activity {

    public static final String TAG = "morris-TRSDigitalPanel";
    private static final int TOAST_MESSAGE = 1;

    public String S_CURRENT_SEQ_ITEM = "";
    public boolean BL_LOW_MODE = false;
    public boolean BL_UV_MODE = false;
    public boolean BL_COMMAND_SENT = false;
    private final boolean BL_USE_CUSTOM_LOGO = false;
    public static final String NO_PRESET_TEXT = "#n/a";
    public static final String TL84_TAG = "TL84";
    private static final String password = "hokus";
    private static final int MSG_SHOW_TOAST = 1;
    private static final int ICON_HEIGHT = 80;
    private boolean bl_bluetooth_forced_on;
    private LightSettings.MyHandler mHandler;
    public final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private FileUtilities fileUtilities;

    boolean blLamp1_ON, blLamp2_ON, blLamp3_ON, blLamp4_ON, blLamp5_ON, blLamp6_ON, blLamp7_ON, BL_TL84_ON, blLamp10_ON, blLamp11_ON, blLamp12_ON, blPRG_ON;
    Button btnL1, btnL2, btnL3, btnL4, btnL5, btnL6, btnL7, btnLOW, btnL9, btnReassign, btnL10, btnL11, btnL12;
    ImageView usb_conn_indicator;
    ImageView bt_conn_indicator;
    ImageView img_logo;
    public Integer iGlobalIndex = 1;

    private Handler lclHandler;
    private UsbCOMMsService lclUsbServiceInstance;
    private BtCOMMsService lclBTServiceInstance;
    boolean mBound = false;
    boolean mBoundBT = false;
    final Context context = this;
    public static final String SP_SEQUENCE_COMMAND_GENERATED = "sequence_command_generated"; //Mauricio
    public static final String SP_SEQUENCE_COMMAND_EXECUTED = "sequence_command_executed"; //Mauricio
    public static final String SHAREDPREFS_LAMP_ASSIGNMENTS = "lamp_button_assignments"; //Mauricio

    public boolean blSeqGreenLight = true;

    private TransparentProgressDialog pd;
    private Handler h;
    private Runnable r;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;

    //Float versionName = com.kiand.LED2match.BuildConfig.VERSION_CODE / 1000.0f;
    String versionName = BuildConfig.VERSION_NAME;
    public String apkVersion = "v" + versionName;
    private final boolean BL_1ST_SCAN = true;


    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();

            String action = intent.getAction();
            logme(TAG, "btReceiver received some intent");
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (bluetoothState) {
                    case BluetoothAdapter.STATE_ON:
                        if (bl_bluetooth_forced_on) {
                            logme(TAG, " *** Calling start BT Service");
                            startBluetoothService();
                            Intent new_intent = new Intent(TRSDigitalPanel.this, TRSBluetoothDevicesScan.class);
                            startActivity(new_intent); //or start activity for result? this should be "modal"
                        }
                        break;

                    case BluetoothAdapter.STATE_OFF:
                        bl_bluetooth_forced_on = false;
                        mark_BT_disconnected();
                        toggle_bt_icon_OFF();

                        break;
                }
            }

        }
    };


    private final ServiceConnection mConnection = new ServiceConnection() {

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
    public final ServiceConnection btConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            lclBTServiceInstance = ((BtCOMMsService.MyBinder) arg1).getService();
            lclBTServiceInstance.setHandler(mHandler);
            mBoundBT = true;
            String sCommand = "X600" + Constants.sNewLine;
            lclBTServiceInstance.sendData(sCommand);
            //Log.d(TAG, "Command: '" + sCommand + "' sent.");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            lclBTServiceInstance = null;
            mBoundBT = false;
        }
    };

    private final Handler messageHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SHOW_TOAST) {
                String message = (String)msg.obj;

                switch (msg.what) {
                    case TOAST_MESSAGE:
                        Toast.makeText(MainApplication.getAppContext(), message.getBytes().toString() , Toast.LENGTH_SHORT).show();
                        break;

                    //handle the result here

                    default:
                        //super.handleMessage(msg);
                        break;
                }

            }
        }
    };

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            if (intent == null) {
                return;
            }
            if (intent.getAction().equals("button_highlight_event")) {
                String name = intent.getStringExtra("button_name");
                logme(TAG, "Received intent 'button_highlight_event'. Name of button to highlight: " + name);
                if (TextUtils.isEmpty(name)) {
                    return;
                }
                //switchButtonON(Integer.valueOf(index), false);
                switchButton_by_name_OFF("OFF");
                switchButton_by_name_ON(name, false);
                //logme(TAG, "Button: " + index + " highlighted");
            } else if (intent.getAction().equals("button_dehighlight_event")) {
                String name = intent.getStringExtra("button_name");
                logme(TAG, "Got button to dehighlight: " + name);
                if (TextUtils.isEmpty(name)) {
                    return;
                }
                switchButton_by_name_OFF(name);
                //logme(TAG, "Button: " + index + " highlighted");
            } else if (intent.getAction().equals("controller_data_refreshed_event")) {
                logme(TAG, "controller data refreshed - intent received");
                refresh_unit_config();
            } else if (intent.getAction().equals("button_highlight_extra")) {
                String name = intent.getStringExtra("button_name");
                logme(TAG, "Got additional button to highlight: " + name);
                if (TextUtils.isEmpty(name)) {
                    return;
                }
                if (name.equalsIgnoreCase("PRG")){
                    switchButtonON(7, false);
                   //logme(TAG, "PRG Button highlighted");
                } else if (name.equalsIgnoreCase("LOW")){
                    switchButtonON(8, false);
                } else if (name.equalsIgnoreCase("OFF")){
                    //allOFF(null);
                    switchButtonON(9, false);
                    BL_LOW_MODE = false;
                } else {

                    logme(TAG, "Calling switch button by name: " + name);
                    switchButton_by_name_ON(name, true);
                }
            }

        }
    };

    public String getJsonBody(String sSharedPrefsFilename) {
        SharedPreferences spsValues = getSharedPreferences(sSharedPrefsFilename, 0);
        return spsValues.getString("JSON", "");

    }

    public String extractJSONvalue(String sJSONbody_ref, String sKeyScanned) {
        String sReturn = "";
        String sJSONbody = getJsonBody(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE);
        if (sJSONbody.length() == 0) {
            return sReturn;
        }

        try {
            sJSONbody = "{" + sJSONbody + "}";
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

    private void switchButton_by_name_ON(String button_name, boolean bl_additional) {
        Log.d(TAG, "switchButton_by_name_ON_() - Trying to highlight button: " + button_name);
        //logme(TAG, "Marking TL84 as active");
        //logme(TAG, "Marking TL84 as inactive");
        BL_TL84_ON = button_name.equalsIgnoreCase("TL84");

        LinearLayout layout = findViewById(R.id.lLayout1);
        int count = layout.getChildCount();
        //logme(TAG, "layout_main has " + count + " children");
        for (int i = 0; i < count; i++) {
            View view = layout.getChildAt(i);
            if (view instanceof Button) {
                //logme(TAG, "Iterating over button name: " + ((Button) view).getText().toString());
                if (((Button) view).getText().toString().equals(button_name)) {
                    if (!bl_additional) {
                        boolean flag = !BL_LOW_MODE;
                        mark_all_buttons_off_on_mobile(flag);
                    }
                    view.setBackgroundResource(R.drawable.buttonselector_active);
                    ((Button) view).setTextColor(Color.BLACK);
                    logme(TAG, "switchButton_by_name_ON_() - Button name found - highlighting: " + ((Button) view).getText().toString());

                }
            }
        }

        layout = findViewById(R.id.lLayout2);
        count = layout.getChildCount();
        //logme(TAG, "layout_main has " + count + " children");
        for (int i = 0; i < count; i++) {
            View view = layout.getChildAt(i);
            if (view instanceof Button) {
                //logme(TAG, "Iterating over button name: " + ((Button) view).getText().toString());
                if (((Button) view).getText().toString().equals(button_name)) {
                    if (!bl_additional) {
                        boolean flag = !BL_LOW_MODE;
                        mark_all_buttons_off_on_mobile(flag);
                    }
                    view.setBackgroundResource(R.drawable.buttonselector_active);
                    ((Button) view).setTextColor(Color.BLACK);
                    logme(TAG, "switchButton_by_name_ON_() - Button name found - highlighting: " + ((Button) view).getText().toString());

                }
            }
        }

        layout = findViewById(R.id.lLayout3);
        count = layout.getChildCount();
        //logme(TAG, "layout_main has " + count + " children");
        for (int i = 0; i < count; i++) {
            View view = layout.getChildAt(i);
            if (view instanceof Button) {
                //logme(TAG, "Iterating over button name: " + ((Button) view).getText().toString());
                if (((Button) view).getText().toString().equals(button_name)) {
                    if (!bl_additional) {
                        boolean flag = !BL_LOW_MODE;
                        mark_all_buttons_off_on_mobile(flag);
                    }
                    view.setBackgroundResource(R.drawable.buttonselector_active);
                    ((Button) view).setTextColor(Color.BLACK);
                    logme(TAG, "switchButton_by_name_ON_() - Button name found - highlighting: " + ((Button) view).getText().toString());

                }
            }
        }

        //if it was a real preset - update lamps value
        String sJsonBody = getJsonBody(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE);

        String sValue = extractJSONvalue(sJsonBody, "preset_counter");
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
            if (sPresetName.equalsIgnoreCase(button_name)) {
                logme(TAG, "switchButton_by_name_ON_() - marking '" + button_name + "' as current lamp in lamps_current_values");
                //updateLampValue(sPresetValue);
            }
        }

    }

    private void switchButton_by_name_OFF(String button_name) {
        logme(TAG, "switchButton_by_name_OFF_() - dehighlighting button: " + button_name);
        LinearLayout layout = findViewById(R.id.lLayout1);
        int count = layout.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = layout.getChildAt(i);
            if (view instanceof Button) {
                if (((Button) view).getText().toString().equals(button_name)) {
                    view.setBackgroundResource(R.drawable.buttonselector_main);
                    ((Button) view).setTextColor(Color.WHITE);
                    logme(TAG, "switchButton_by_name_OFF_() - button: " + button_name + " dehighlighted.");
                }
            }
        }

        layout = findViewById(R.id.lLayout2);
        count = layout.getChildCount();
        //logme(TAG, "layout_main has " + count + " children");
        for (int i = 0; i < count; i++) {
            View view = layout.getChildAt(i);
            if (view instanceof Button) {
                if (((Button) view).getText().toString().equals(button_name)) {
                    view.setBackgroundResource(R.drawable.buttonselector_main);
                    ((Button) view).setTextColor(Color.WHITE);
                    logme(TAG, "switchButton_by_name_OFF_() - button: " + button_name + " dehighlighted.");
                }
            }
        }

        layout = findViewById(R.id.lLayout3);
        count = layout.getChildCount();
        //logme(TAG, "layout_main has " + count + " children");
        for (int i = 0; i < count; i++) {
            View view = layout.getChildAt(i);
            if (view instanceof Button) {
                if (((Button) view).getText().toString().equals(button_name)) {
                    view.setBackgroundResource(R.drawable.buttonselector_main);
                    ((Button) view).setTextColor(Color.WHITE);
                    logme(TAG, "switchButton_by_name_OFF_() - button: " + button_name + " dehighlighted.");
                }
            }
        }
    }


    @SuppressLint("NewApi")
    protected void onResume()
    {
        super.onResume();

        //try suto-connecting to the controller via BT:
//        if (BL_1ST_SCAN) {
//            int value = BT_autoconnect();
//            BL_1ST_SCAN = false;
//        }

        //makeToast("onResume");
        usb_conn_indicator.getLayoutParams().height= ICON_HEIGHT;
        usb_conn_indicator.requestLayout();

        bt_conn_indicator.getLayoutParams().height= ICON_HEIGHT;
        bt_conn_indicator.requestLayout();

        ImageView img = findViewById(R.id.unitycolor_logo);
        img.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("http://www.unitycolor.com"));
                startActivity(intent);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction("controller_data_refreshed_event");
        filter.addAction("button_highlight_event");
        filter.addAction("button_dehighlight_event");
        filter.addAction("button_highlight_extra");

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
        if (!mBound) {
            Intent intent = new Intent(this, UsbCOMMsService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            //Toast.makeText(this.getBaseContext(),"Service bound (onResume)", Toast.LENGTH_SHORT).show();

            SystemClock.sleep(50);
            mBound = true;
        }

//        logme(TAG, "starting Bluetooth connection service.. Wait 3s");
//        SystemClock.sleep(3000);
        Log.d(TAG, "mBoundBT = " + mBoundBT);
        if (!mBoundBT) {
            Log.d(TAG, "localBTservice null? = " + (lclBTServiceInstance == null));
            Intent intent = new Intent(this, BtCOMMsService.class);
            bindService(intent, btConnection, Context.BIND_AUTO_CREATE);

            SystemClock.sleep(50);

        }


        Float versionName = BuildConfig.VERSION_CODE / 1000.0f;

        if (shared_prefs_exists(SHAREDPREFS_LAMP_ASSIGNMENTS, "666")) {
            repopulate_button_assignments();
            logme(TAG, "Using repopulate_button to get captions");
        } else {
            logme(TAG, "Key 666 not found. Using populate_button from JSON");
            populateButtonNames();
        }

        check_extended_mode();
        populate_bluetooth_indicator();
        populateLampsState();
        setUnitName();

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to UsbService
        //Toast.makeText(this.getBaseContext(),"Activity started", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, UsbCOMMsService.class);
        bindService (intent, mConnection, Context.BIND_AUTO_CREATE);

        //makeToast("onStart");
        /*String sCommand = "J" + LightAdjustments.sNewLine; */
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        //unregisterReceiver(btReceiverBTdevice);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
        }
        if (mBoundBT) {
            unbindService(btConnection);
        }
        mBoundBT = false;
        mBound = false;
        logme(TAG, "onStop_() - onStop");

        /*SharedPreferences prefs = getSharedPreferences(BT_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("device_btaddress");
        editor.putBoolean("connected", false);
        editor.commit();*/

        //Toast.makeText(this.getBaseContext(),"Main activity stopped", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"Service unbound", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();
        //finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Toast.makeText(this.getBaseContext(),"Main activity destroyed", Toast.LENGTH_SHORT).show();
        if (lclBTServiceInstance == null) {
            toggle_bt_icon_OFF();
            mark_BT_disconnected();
        }

        //mark_customer_data_use_in_config(false);
        //mark_customer_logo_use_in_config(false);
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        logme(TAG, "Back button pressed");
        finish();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trs_digital_panel);

        lclHandler = new Handler();

        btnL1 = findViewById(R.id.btnL1);
        btnL2 = findViewById(R.id.btnL2);
        btnL3 = findViewById(R.id.btnL3);
        btnL4 = findViewById(R.id.btnL4);
        btnL5 = findViewById(R.id.btnL5);
        btnL6 = findViewById(R.id.btnL6);
        btnL7 = findViewById(R.id.btnL7);
        btnLOW = findViewById(R.id.btnLOW);
        btnL9 = findViewById(R.id.btnL9);

        btnL10 = findViewById(R.id.btnL10);
        btnL11 = findViewById(R.id.btnL11);
        btnL12 = findViewById(R.id.btnL12);
        btnReassign = findViewById(R.id.btnReassign); //reassign
        usb_conn_indicator = findViewById(R.id.usb_connection_image);
        bt_conn_indicator = findViewById(R.id.bt_connection_image);



        populateButtonNames();
        setFiltersBT();
        setFiltersBTdevice();
        fileUtilities = new FileUtilities(get_path_to_customer_datafile(), get_path_to_customer_logofile());
        check_for_customer_data_use();


        btnReassign.setOnLongClickListener(arg0 -> {

            mark_customer_zipfile_processed_in_config(false);
            makeToast("ZIP file processed marked as false");

            return true;
        });


        bt_conn_indicator.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //your stuff

                //makeToast("Long click detected");

                int value = BT_autoconnect();
                return true;
            }
        });

        btnL1.setOnLongClickListener(arg0 -> {
            if (btnL1.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                switchButtonOFF(11);
            }
            switchButtonOFF(1);
            allOFF(null);
            return true;
        });
        btnL2.setOnLongClickListener(arg0 -> {
            switchButtonOFF(2);
            allOFF(null);
            return true;
        });
        btnL3.setOnLongClickListener(arg0 -> {
            switchButtonOFF(3);
            allOFF(null);
            return true;
        });
        btnL4.setOnLongClickListener(arg0 -> {
            switchButtonOFF(4);
            allOFF(null);
            return true;
        });
        btnL5.setOnLongClickListener(arg0 -> {
            switchButtonOFF(5);
            allOFF(null);
            return true;
        });
        btnL6.setOnLongClickListener(arg0 -> {
            switchButtonOFF(6);
            allOFF(null);
            return true;
        });
        btnL10.setOnLongClickListener(arg0 -> {
            switchButtonOFF(7);
            allOFF(null);
            return true;
        });
        btnL11.setOnLongClickListener(arg0 -> {
            switchButtonOFF(8);
            allOFF(null);
            return true;
        });
        btnL12.setOnLongClickListener(arg0 -> {
            switchButtonOFF(9);
            allOFF(null);
            return true;
        });


        btnL1.setBackgroundResource(R.drawable.buttonselector_main);
        btnL1.setTextColor(Color.WHITE);

        btnL2.setBackgroundResource(R.drawable.buttonselector_main);
        btnL2.setTextColor(Color.WHITE);

        btnL3.setBackgroundResource(R.drawable.buttonselector_main);
        btnL3.setTextColor(Color.WHITE);

        btnL4.setBackgroundResource(R.drawable.buttonselector_main);
        btnL4.setTextColor(Color.WHITE);

        btnL5.setBackgroundResource(R.drawable.buttonselector_main);
        btnL5.setTextColor(Color.WHITE);

        btnL6.setBackgroundResource(R.drawable.buttonselector_main);
        btnL6.setTextColor(Color.WHITE);

        btnL7.setBackgroundResource(R.drawable.buttonselector_main);
        btnL7.setTextColor(Color.WHITE);

        btnL9.setBackgroundResource(R.drawable.buttonselector_main);
        btnL9.setTextColor(Color.WHITE);

        btnLOW.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnLOW.setTextColor(Color.WHITE);

        btnReassign.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_special));
        btnReassign.setTextColor(Color.BLACK);

        btnL10.setBackgroundResource(R.drawable.buttonselector_main);
        btnL10.setTextColor(Color.WHITE);

        btnL11.setBackgroundResource(R.drawable.buttonselector_main);
        btnL11.setTextColor(Color.WHITE);

        btnL12.setBackgroundResource(R.drawable.buttonselector_main);
        btnL12.setTextColor(Color.WHITE);

        if (shared_prefs_exists(SHAREDPREFS_LAMP_ASSIGNMENTS, "1")) {
            repopulate_button_assignments();
        } else {
            populateButtonNames();
        }

        Log.d(TAG, "Requesting storage permission");
        check_and_request_permissions(STORAGE_PERMISSION_CODE);

    }
    private int BT_autoconnect() {

        int retVal = 0;
        if (btAdapter == null) {
            logme(TAG, "BT adapter is null");
            retVal = 1;
        }

        if (btAdapter.isEnabled()) {
            logme(TAG, "BT_autoconnect_() - BT enabled on the mobile");
            if (!bl_bluetooth_forced_on) {
                logme(TAG, " **** starting Bluetooth connection service");
                startBluetoothService();
            }
            if (!check_for_BT_connection()) {
                Intent intent = new Intent(TRSDigitalPanel.this, TRSBluetoothDevicesScan.class);
                startActivity(intent); //or start activity for result? this should be "modal"
            } else {
                Intent intent = new Intent(TRSDigitalPanel.this, TRSBluetoothDevicesScan.class);
                startActivity(intent); //or start activity for result? this should be "modal"
            }
        } else {
            logme(TAG, "Requesting to enable BT on mobile");
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
            bl_bluetooth_forced_on = true;
        }

        return retVal;
    }

    private void check_and_request_permissions(int requestCode) {
        // Function to check and request permission


                String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            // Checking if permission is not granted
            if (ContextCompat.checkSelfPermission(
                    TRSDigitalPanel.this,
                    permission)
                    == PackageManager.PERMISSION_DENIED) {
                ActivityCompat
                        .requestPermissions(
                                TRSDigitalPanel.this,
                                new String[] { permission },
                                requestCode);
            }
            else {
                /*Toast
                        .makeText(TRSDigitalPanel.this,
                                "Permission already granted",
                                Toast.LENGTH_SHORT)
                        .show();*/
                Log.d(TAG, "Storage permission already granted");
            }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super
                .onRequestPermissionsResult(requestCode,
                        permissions,
                        grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(TRSDigitalPanel.this,
                        "Storage Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
            } else {
                Toast.makeText(TRSDigitalPanel.this,
                        "Storage Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void check_extended_mode() {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, Context.MODE_PRIVATE);

        Boolean bl_extended_mode = prefs.getBoolean(Constants.EXTENDED_LAMPS_MODE_TAG, false);
        View layout_extended = findViewById(R.id.lLayout5);
        if (bl_extended_mode) {
            layout_extended.setVisibility(View.VISIBLE);
        } else {
            layout_extended.setVisibility(View.INVISIBLE);
        }
    }

    private void mark_customer_logo_use_in_config(boolean flag) {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.CUSTOMER_LOGO_FLAG, flag);
        editor.apply();
    }

    private void mark_customer_data_use_in_config(boolean flag) {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        logme(TAG, "Setting USE CUSTOMER DATA to: " + flag);
        editor.putBoolean(Constants.CUSTOMER_DATA_FLAG, flag);
        editor.apply();
    }

    private void mark_customer_zipfile_processed_in_config(boolean flag) {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        logme(TAG, "Setting USE CUSTOMER ZIPFILE PROCESSED to: " + flag);
        editor.putBoolean(Constants.CUSTOMER_ZIPFILE_PROCESSED, flag);
        editor.apply();
    }

    private void startBluetoothService() {
        Intent intent = new Intent(this, BtCOMMsService.class);
        this.startService(intent);
    }

    private boolean check_for_BT_connection() {
        SharedPreferences prefs = getSharedPreferences(BT_CONNECTED_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean("CONNECTED", false);
    }

    public void reassign_lamps(View v) {

        String sTags = "";
        if (!btnL1.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL1.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL2.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL2.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL3.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL3.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL4.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL4.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL5.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL5.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL6.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL6.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL10.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL10.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL11.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL11.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL12.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL12.getText().toString() + ","; } else { sTags = sTags + ","; }


        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        String sPresetCounter = json_analyst.getJSONValue("preset_counter");

        int iCtr = ((!sPresetCounter.trim().equals("") ? Integer.valueOf(sPresetCounter) : 0));
        Intent intentLampAssignment = new Intent(TRSDigitalPanel.this, ReassignLamps.class);
        intentLampAssignment.putExtra("tags", sTags);
        intentLampAssignment.putExtra("counter", iCtr);
        startActivity(intentLampAssignment);
    }

    private void refresh_unit_config() {
        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        String ee_autoshutoff_tag = "eeprom_auto_shutoff";
        String ee_tl84delay_tag = "eeprom_tl84_delay";
        String ee_psucurrent_tag = "eeprom_PSU_current";
        String ee_tl84dim_tag = "eeprom_tl84_dim_value";
        String ee_tl84full_tag = "eeprom_tl84_full_value";
        String ee_noofpanels_tag = "eeprom_no_of_panels";
        String ee_temp_corr_factor_tag = "eeprom_temp_corr_factor";
        String ee_emergency_tag = "eeprom_emergency_light_delay";

        //logme(TAG, "bluetoothAskReply(V1)");
        String s_eeprom_auto_shutoff = json_analyst.getJSONValue(ee_autoshutoff_tag);
        String s_eeprom_tl84_delay = json_analyst.getJSONValue(ee_tl84delay_tag);
        String s_eeprom_PSU_current = json_analyst.getJSONValue(ee_psucurrent_tag);
        String s_eeprom_tl84_dim_value = json_analyst.getJSONValue(ee_tl84dim_tag);
        String s_eeprom_tl84_full_value = json_analyst.getJSONValue(ee_tl84full_tag);
        String s_eeprom_no_of_panels = json_analyst.getJSONValue(ee_noofpanels_tag);
        String s_eeprom_temp_corr_factor = json_analyst.getJSONValue(ee_temp_corr_factor_tag);
        String s_eeprom_emergency_light_delay = json_analyst.getJSONValue(ee_emergency_tag);

        SharedPreferences spConfig = getSharedPreferences(CONFIG_SETTINGS, 0);
        SharedPreferences.Editor spConfigEditor = spConfig.edit();
        spConfigEditor.putString(ee_autoshutoff_tag, s_eeprom_auto_shutoff);
        spConfigEditor.putString(ee_tl84delay_tag, s_eeprom_tl84_delay);
        spConfigEditor.putString(ee_psucurrent_tag, s_eeprom_PSU_current);
        spConfigEditor.putString(ee_tl84dim_tag, s_eeprom_tl84_dim_value);
        spConfigEditor.putString(ee_tl84full_tag, s_eeprom_tl84_full_value);
        spConfigEditor.putString(ee_noofpanels_tag, s_eeprom_no_of_panels);
        spConfigEditor.putString(ee_temp_corr_factor_tag, s_eeprom_temp_corr_factor);
        spConfigEditor.putString(ee_emergency_tag, s_eeprom_emergency_light_delay);
        spConfigEditor.apply();

    }

    private void comms_dialog() {

        Intent intent = new Intent(TRSDigitalPanel.this, OverlayPage.class);
        startActivity(intent);
    }

    public void splash_screen(View v) {
        comms_dialog();

    }

    public void mark_BT_connnected() {
        SharedPreferences prefs = getSharedPreferences(BT_CONNECTED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = prefs.edit();

        spEditor.clear();
        spEditor.putBoolean("CONNECTED", true);
        spEditor.apply();
        logme(TAG, "BT connection marked as true in the sp file");
    }

    public void mark_BT_disconnected() {
        //makeToast("Entering mark BT disconnected");
        SharedPreferences prefs = getSharedPreferences(BT_CONNECTED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.clear();
        spEditor.putBoolean("CONNECTED", false);
        spEditor.apply();
        logme(TAG, "BT connection marked as false in the sp file");
        toggle_bt_icon_OFF();
    }

    public void toggle_usb_icon(View view) {
        usb_conn_indicator.getLayoutParams().height= ICON_HEIGHT;
        usb_conn_indicator.requestLayout();
        usb_conn_indicator.setImageLevel(1);
    }

    public void toggle_bt_icon_ON() {
        bt_conn_indicator.getLayoutParams().height= ICON_HEIGHT;
        bt_conn_indicator.requestLayout();
        bt_conn_indicator.setImageLevel(1);
    }
    public void toggle_bt_icon_OFF() {
        bt_conn_indicator.getLayoutParams().height= ICON_HEIGHT;
        bt_conn_indicator.requestLayout();
        bt_conn_indicator.setImageLevel(0);
    }

    public void populate_bluetooth_indicator() {

        if (bluetooth_connected()) {
            toggle_bt_icon_ON();
        } else {
            toggle_bt_icon_OFF();
        }

    }

    private final BroadcastReceiver btReceiverBTdevice = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action){
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    logme(TAG, "Broadcast receiver: ACL_CONNECTED");
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    logme(TAG, "Broadcast receiver: ACL_DISCONNECTED");
                    makeToast("Bluetooth communication has been disconnected.");
                    mark_BT_disconnected();
                    break;
            }
            Bundle bundle = intent.getExtras();

        }
    };

    private void setFiltersBT() {
        IntentFilter filterBT = new IntentFilter();
        filterBT.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filterBT.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filterBT.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filterBT.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filterBT.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btReceiver, filterBT);
    }

    private void setFiltersBTdevice() {
        IntentFilter filterBTdevice = new IntentFilter();
        filterBTdevice.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filterBTdevice.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(btReceiverBTdevice, filterBTdevice);
    }

    public boolean shared_prefs_exists(String sFileName, String sKey) {
        SharedPreferences spFile = getSharedPreferences(sFileName, 0);
        logme(TAG, "Shared_prefs_exists (" + sFileName+ "): " + spFile.contains(sKey));
        return spFile.contains(sKey);
    }

    public void repopulate_button_assignments() {
        SharedPreferences myPrefs = this.getSharedPreferences(SHAREDPREFS_LAMP_ASSIGNMENTS, 0);
        TreeMap<String, ?> keys = new TreeMap<String, Object>(myPrefs.getAll());

        btnL1.setText(NO_PRESET_TEXT);
        btnL1.setTag(NO_PRESET_TEXT);
        btnL2.setText(NO_PRESET_TEXT);
        btnL2.setTag(NO_PRESET_TEXT);
        btnL3.setText(NO_PRESET_TEXT);
        btnL3.setTag(NO_PRESET_TEXT);
        btnL4.setText(NO_PRESET_TEXT);
        btnL4.setTag(NO_PRESET_TEXT);
        btnL5.setText(NO_PRESET_TEXT);
        btnL5.setTag(NO_PRESET_TEXT);
        btnL6.setText(NO_PRESET_TEXT);
        btnL6.setTag(NO_PRESET_TEXT);

        btnL10.setText(NO_PRESET_TEXT);
        btnL10.setTag(NO_PRESET_TEXT);
        btnL11.setText(NO_PRESET_TEXT);
        btnL11.setTag(NO_PRESET_TEXT);
        btnL12.setText(NO_PRESET_TEXT);
        btnL12.setTag(NO_PRESET_TEXT);


        for (Map.Entry<String, ?> entry : keys.entrySet()) {

            switch (entry.getKey()) {
                case "1":
                    btnL1.setText(entry.getValue().toString());
                    btnL1.setTag(entry.getValue().toString());
                    break;

                case "2":
                    btnL2.setText(entry.getValue().toString());
                    btnL2.setTag(entry.getValue().toString());
                    break;

                case "3":
                    btnL3.setText(entry.getValue().toString());
                    btnL3.setTag(entry.getValue().toString());
                    break;

                case "4":
                    btnL4.setText(entry.getValue().toString());
                    btnL4.setTag(entry.getValue().toString());
                    break;

                case "5":
                    btnL5.setText(entry.getValue().toString());
                    btnL5.setTag(entry.getValue().toString());
                    break;

                case "6":
                    btnL6.setText(entry.getValue().toString());
                    btnL6.setTag(entry.getValue().toString());
                    break;

                case "21":
                    btnL10.setText(entry.getValue().toString());
                    btnL10.setTag(entry.getValue().toString());
                    break;

                case "22":
                    btnL11.setText(entry.getValue().toString());
                    btnL11.setTag(entry.getValue().toString());
                    break;

                case "23":
                    btnL12.setText(entry.getValue().toString());
                    btnL12.setTag(entry.getValue().toString());
                    break;

                //logme(TAG, entry.getKey() + ":" + entry.getValue());
                //some code
            }
        }
    }

    public void switchButtonOFF(int index) {
        switch(index) {
            case 1:
                if (blLamp1_ON) {
                    blLamp1_ON = false;
                    btnL1.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL1.setTextColor(Color.WHITE);
                    makeToast("Light switched off");
                }
                break;

            case 2:
                if (blLamp2_ON) {
                    blLamp2_ON = false;
                    btnL2.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL2.setTextColor(Color.WHITE);
                    makeToast("Light switched off");
                }
                break;

            case 3:
                if (blLamp3_ON) {
                    blLamp3_ON = false;
                    btnL3.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL3.setTextColor(Color.WHITE);
                    makeToast("Light switched off");
                }
                break;

            case 4:
                if (blLamp4_ON) {
                    blLamp4_ON = false;
                    btnL4.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL4.setTextColor(Color.WHITE);
                    makeToast("Light switched off");
                }
                break;

            case 5:
                if (blLamp5_ON) {
                    blLamp5_ON = false;
                    btnL5.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL5.setTextColor(Color.WHITE);
                    makeToast("Light switched off");
                }
                break;

            case 6:
                if (blLamp6_ON) {
                    blLamp6_ON = false;
                    btnL6.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL6.setTextColor(Color.WHITE);
                    makeToast("Light switched off");
                }
                break;

            case 8:

                btnLOW.setBackgroundResource(R.drawable.buttonselector_main);
                btnLOW.setTextColor(Color.WHITE);
                //makeToast("LOW mode switched off");

                break;

            case 11:
                if (BL_TL84_ON) {
                    BL_TL84_ON = false;
                }
                break;

            case 21:
                if (blLamp10_ON) {
                    blLamp10_ON = false;
                    btnL10.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL10.setTextColor(Color.WHITE);
                    makeToast("Light switched off");
                }
                break;

            case 22:
                if (blLamp11_ON) {
                    blLamp11_ON = false;
                    btnL11.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL11.setTextColor(Color.WHITE);
                    makeToast("Light switched off");
                }
                break;

            case 23:
                if (blLamp12_ON) {
                    blLamp12_ON = false;
                    btnL12.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL12.setTextColor(Color.WHITE);
                    makeToast("Light switched off");
                }
                break;

        }

    }

    public void switchButtonON(int index, boolean overwrite_OFF) {
        switch(index) {
            case 1:
                if (!blLamp1_ON) {
                    mark_all_buttons_off_on_mobile(false);
                    blLamp1_ON = true;
                    btnL1.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL1.setTextColor(Color.BLACK);
                    //makeToast("Light switched on");
                }
                break;

            case 2:
                if (!blLamp2_ON) {
                    mark_all_buttons_off_on_mobile(false);
                    blLamp2_ON = true;
                    btnL2.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL2.setTextColor(Color.BLACK);
                    //makeToast("Light switched on");
                }
                break;

            case 3:
                if (!blLamp3_ON) {
                    mark_all_buttons_off_on_mobile(false);
                    blLamp3_ON = true;
                    btnL3.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL3.setTextColor(Color.BLACK);
                    //makeToast("Light switched on");
                }
                break;

            case 4:
                if (!blLamp4_ON) {
                    mark_all_buttons_off_on_mobile(false);
                    blLamp4_ON = true;
                    btnL4.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL4.setTextColor(Color.BLACK);
                    //makeToast("Light switched on");
                }
                break;

            case 5:
                if (!blLamp5_ON) {
                    mark_all_buttons_off_on_mobile(false);
                    blLamp5_ON = true;
                    btnL5.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL5.setTextColor(Color.BLACK);
                    //makeToast("Light switched on");
                }
                break;

            case 6:
                if (!blLamp6_ON) {
                    mark_all_buttons_off_on_mobile(false);
                    blLamp6_ON = true;
                    btnL6.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL6.setTextColor(Color.BLACK);
                    //makeToast("Light switched on");
                }
                break;

            case 7: //PRG
                mark_all_buttons_off_on_mobile(true);
                btnL7.setBackgroundResource(R.drawable.buttonselector_active);
                btnL7.setTextColor(Color.BLACK);
                logme(TAG, "switchButtonON_() - only PRG should be highlighted");
                break;

            case 8: //LOW
                btnLOW.setBackgroundResource(R.drawable.buttonselector_active);
                btnLOW.setTextColor(Color.BLACK);
                //makeToast("LOW mode switched on");
                BL_LOW_MODE = true;
                break;

            case 9: //OFF
                mark_all_buttons_off_on_mobile(true);
                btnL9.setBackgroundResource(R.drawable.buttonselector_active);
                btnL9.setTextColor(Color.BLACK);
                //makeToast("All lights switched off");
                break;

            case 11:
                if (!BL_TL84_ON) {
                    mark_all_buttons_off_on_mobile(false);
                    BL_TL84_ON = true;
                }
                break;

            case 21:
                if (!blLamp10_ON) {
                    mark_all_buttons_off_on_mobile(false);
                    blLamp10_ON = true;
                    btnL10.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL10.setTextColor(Color.BLACK);
                    //makeToast("Light switched on");
                }
                break;

            case 22:
                if (!blLamp11_ON) {
                    mark_all_buttons_off_on_mobile(false);
                    blLamp11_ON = true;
                    btnL11.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL11.setTextColor(Color.BLACK);
                    //makeToast("Light switched on");
                }
                break;

            case 23:
                if (!blLamp12_ON) {
                    mark_all_buttons_off_on_mobile(false);
                    blLamp12_ON = true;
                    btnL12.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL12.setTextColor(Color.BLACK);
                    //makeToast("Light switched on");
                }
                break;

        }

    }

    private void setUnitName() {
        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        final String sUnitName = json_analyst.getJSONValue("unit_name");
        messageHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d (TAG, "UNIT NAME of '"  + sUnitName + "' set!");
                setTitle(getResources().getString(R.string.app_header_title) + " " + sUnitName);
            }
        });
    }

    public void populateButtonNames() {
        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        //logme(TAG, "bluetoothAskReply(V1)");
        final String sLamp1Name = json_analyst.getJSONValue("p1_nm");
        final String sLamp2Name = json_analyst.getJSONValue("p2_nm");
        final String sLamp3Name = json_analyst.getJSONValue("p3_nm");
        final String sLamp4Name = json_analyst.getJSONValue("p4_nm");
        final String sLamp5Name = json_analyst.getJSONValue("p5_nm");
        final String sLamp6Name = json_analyst.getJSONValue("p6_nm");
        Log.d(TAG, "Populating Lamp1Name = " + sLamp1Name);
        //final String sLamp4Name = extractJSONvalue("", "lamp4_name");

        setLampName(1, sLamp1Name);
        setLampName(2, sLamp2Name);
        setLampName(3, sLamp3Name);
        setLampName(4, sLamp4Name);
        setLampName(5, sLamp5Name);
        setLampName(6, sLamp6Name);
        //setLampName(4, sLamp4Name);
    }

    public void setLampName(int i, String sName) {
        if (i == 1) {
            if (sName.length() > 0) {
                btnL1.setText(sName);
                btnL1.setTag(sName);
            } else {
                btnL1.setText(NO_PRESET_TEXT);
                btnL1.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 2) {
            if (sName.length() > 0) {
                btnL2.setText(sName);
                btnL2.setTag(sName);
            } else {
                btnL2.setText(NO_PRESET_TEXT);
                btnL2.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 3) {
            if (sName.length() > 0) {
                btnL3.setText(sName);
                btnL3.setTag(sName);
            } else {
                btnL3.setText(NO_PRESET_TEXT);
                btnL3.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 4) {
            if (sName.length() > 0) {
                btnL4.setText(sName);
                btnL4.setTag(sName);
            } else {
                btnL4.setText(NO_PRESET_TEXT);
                btnL4.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 5) {
            if (sName.length() > 0) {
                btnL5.setText(sName);
                btnL5.setTag(sName);
            } else {
                btnL5.setText(NO_PRESET_TEXT);
                btnL5.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 6) {
            if (sName.length() > 0) {
                btnL6.setText(sName);
                btnL6.setTag(sName);
            } else {
                btnL6.setText(NO_PRESET_TEXT);
                btnL6.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 21) {
            if (sName.length() > 0) {
                btnL10.setText(sName);
                btnL10.setTag(sName);
            } else {
                btnL10.setText(NO_PRESET_TEXT);
                btnL10.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 22) {
            if (sName.length() > 0) {
                btnL11.setText(sName);
                btnL11.setTag(sName);
            } else {
                btnL11.setText(NO_PRESET_TEXT);
                btnL11.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 23) {
            if (sName.length() > 0) {
                btnL12.setText(sName);
                btnL12.setTag(sName);
            } else {
                btnL12.setText(NO_PRESET_TEXT);
                btnL12.setTag(NO_PRESET_TEXT);
            }
        }

    }
    public String get_tl84_delay() {
        SharedPreferences config_prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, 0);
        logme(TAG, " ** TL84_delay from file: " + String.format(Locale.US, "%04d", config_prefs.getInt(TL84_DELAY_KEY, 0)));
        return String.format(Locale.US, "%04d", config_prefs.getInt(TL84_DELAY_KEY, 0));

    }

    private void send_via_bt(String command) {
        if (mBoundBT) {
            logme(TAG, "Service btService connected. Calling btService.sendData with message '" + command.replace("\n", "\\n").replace("\r", "\\r") + "'");
            lclBTServiceInstance.sendData(command);
        } else {
            logme(TAG, "Service btService not connected!");
        }
    }

    private String getBodyOfJSONPresetsfile() { // checks file /files/presets_definition.json
        String readString = "";
        try {

            PackageManager m = getPackageManager();
            String s = getPackageName();
            try {
                PackageInfo p = m.getPackageInfo(s, 0);
                s = p.applicationInfo.dataDir;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "PATH: " + s);
            File file = new File(s + "/files/" + Constants.PRESETS_DEFINITION_JSONFILE);
            FileInputStream fin = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            readString = new String(sb);
            reader.close();
            fin.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return readString;
    }

    public int findGivenPresetSlot(String presetName, String jsonString) {
        int i = -1;
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            Iterator<String> keysIterator = jsonObject.keys();
            int j =0;
            while (keysIterator.hasNext()) {
                String key = keysIterator.next();
                String value = jsonObject.getString(key);

                if (key.contains("_nm")) {
                    j++;
                    if (value.equalsIgnoreCase(presetName)) {
                        return j;
                    }
                }
            }
        } catch (JSONException ioe) {
            ioe.printStackTrace();
        }
        return i;
    }

    public boolean get_connection_status() {
        SharedPreferences prefs_config = getSharedPreferences(Constants.BT_CONNECTED_PREFS, 0);
        boolean status= prefs_config.getBoolean("CONNECTED", false);

        return status;
    }

    public void btnClicked(View v) {
        boolean connected = get_connection_status();
        if (connected) {
            //check license
            if (!licensed()) {
                //v.setEnabled(false);
                makeToast("No license detected - unable to switch on lamp");
            } else {
                //v.setEnabled(true);
                btnClicked_UV_normal(v);
            }
        }
    }

    public boolean licensed() {
        int current_tier = get_tier();

        if (current_tier == 0) {
            Log.d(TAG, "licensed_() - unlicensed copy - disabling buttons");
            return false;
        } else {
            Log.d(TAG, "licensed_() - licensed copy. Buttons active");
            return true;
        }
    }

    public int get_tier() {
        int current_tier = 0;
        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        try {
            current_tier = Integer.parseInt(json_analyst.getJSONValue("license_tier"));
            Log.d(TAG, "get_tier_() - returning TIER " + current_tier);
            return current_tier;
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            Log.w(TAG, "Unable to parse tier '" + json_analyst.getJSONValue("license_tier") + "' as a number");
            return 0;
        }
    }


    public void btnClicked_UV_normal(View v) {
        //SharedPreferences spLampDefinitions = getSharedPreferences(Constants.SHAREDPREFS_LAMP_DEFINITIONS, 0);
        String sPresetDefinitions = getBodyOfJSONPresetsfile();
        String sPresetRGBValues = null;
        String sCommand;

        String buttonCaption = v.getTag().toString();
        if (!TextUtils.isEmpty(buttonCaption)) {
            if (!buttonCaption.equalsIgnoreCase("LOW")) {
                logme(TAG, "switching off all lamps");
                mark_all_buttons_off_on_mobile(false);
            }
        }

        int buttonID = v.getId();
        Button button = findViewById(buttonID);

        /* scenarios:
        1. "clean" execution - switch on lamp
        2. "clean" execution - special lamp - TL84
        3. "clean" execution - special lamp - LOW
        4. a lamp is already on - special lamp - TL84
        5. a lamp is already on - special lamp - LOW on
        6. a lamp is already on - special lamp - LOW off
         */


        if (sPresetDefinitions.contains(button.getText().toString())) {
            JSONObject jsonPresets = null;
            try {
                jsonPresets = new JSONObject(sPresetDefinitions);

            } catch (JSONException je) {
                je.printStackTrace();
            }
            try {
                int slot = findGivenPresetSlot(button.getText().toString(), sPresetDefinitions);
                if (slot == -1) {
                    makeToast("Unable to find preset slot for: " + button.getText().toString());
                    return;
                }
                sPresetRGBValues = jsonPresets.getString("p" + slot + "_def");
                Log.d(TAG, "btnClicked_UV_normal_() - Found RGB values of preset '" + button.getText().toString() + "': " + sPresetRGBValues);
            } catch (NullPointerException | JSONException npe) {
                npe.printStackTrace();
                makeToast("ERROR: Unable to build JSON object with presets definition. Does the correct file exist in the correct path?");
                Log.e(TAG, "JSON Object of presets definition is null or JSON exception encountered.");
            }


            if (!power_drain_check(sPresetRGBValues)) {
                return;
            }
            logme(TAG, "sending " + sPresetRGBValues + " to controller");
            if (sPresetRGBValues != null) {
                if (!BL_LOW_MODE) {
                    logme(TAG, "I'm not in LOW mode");

                    if (button.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                        get_tl84_delay();
                        sCommand = sCommand = "X101" + S_COMMAND_SEPARATOR + button.getTag().toString() + S_COMMAND_SEPARATOR + "1$" + sNewLine;
                        logme(TAG, " *** NEW TL84 command (ON): " + sCommand);
                        send_via_bt(sCommand);
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        BL_TL84_ON = true;
                    } else {
                        BL_TL84_ON = false;
                        sCommand = "X101" + S_COMMAND_SEPARATOR + button.getTag().toString() + S_COMMAND_SEPARATOR + "1$" + sNewLine;
                        send_via_bt(sCommand);

//                        BL_COMMAND_SENT = true;
//                        if (!BL_COMMAND_SENT) {
//                            sCommand = "S" + convertRGBwithCommasToHexString(sPresetRGBValues);
//                            sCommand += "$" + sNewLine;
//                            logme(TAG, " *** NEW TL84 command (OFF): " + sCommand);
//                            send_via_bt(sCommand);
//                            lclUsbServiceInstance.sendBytes(sCommand.getBytes());
//                        }
                    }
                } else {
                    //LOW MODE!
                    //19.11.2022 We send full led factors, let the controller do the dimming
                    Log.d (TAG, "LOW mode! Changing " + sPresetRGBValues + " to LOW values.");


                    if (button.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                        sCommand = sCommand = "X101" + S_COMMAND_SEPARATOR + button.getTag().toString() + S_COMMAND_SEPARATOR + "1$" + sNewLine;
                        logme(TAG, " *** NEW TL84 command (LOW): " + sCommand);
                        send_via_bt(sCommand);
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        sCommand = "B," + button.getTag().toString() + "1$" + sNewLine;
                        send_via_bt(sCommand);
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        BL_TL84_ON = true;
                    } else {
                        BL_TL84_ON = false;
                        logme(TAG, " *** Not a TL84 light.");
                        //send_via_bt(sCommand);
                        sCommand = "X101" + S_COMMAND_SEPARATOR + button.getTag().toString() + S_COMMAND_SEPARATOR + "1$" + sNewLine;
                        send_via_bt(sCommand);
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        logme(TAG, "I'm in LOW mode");
                    }
                }
                button.setBackgroundResource(R.drawable.buttonselector_active);
                button.setTextColor(Color.BLACK);
            }
            if (buttonID == R.id.btnL1) {
                blLamp1_ON = true;
            } else if (buttonID == R.id.btnL2) {
                blLamp2_ON = true;
            } else if (buttonID == R.id.btnL3) {
                blLamp3_ON = true;
            } else if (buttonID == R.id.btnL4) {
                blLamp4_ON = true;
            } else if (buttonID == R.id.btnL5) {
                blLamp5_ON = true;
            } else if (buttonID == R.id.btnL6) {
                blLamp6_ON = true;
            } else if (buttonID == R.id.btnL10) {
                blLamp10_ON = true;
            } else if (buttonID == R.id.btnL11) {
                blLamp11_ON = true;
            }  else if (buttonID == R.id.btnL12) {
                blLamp12_ON = true;
            }
            //updateLampValue(sPresetRGBValues);

        } else {
            Log.d(TAG, "No lamp preset found for '" + button.getText().toString());
            makeToast("No lamp preset assigned to this button!");
        }
    }

    public String convertRGB2complementaryLight(String sRGB, boolean ON) {
        String sValue = "";

        String[] stringArray = sRGB.split(",");
        for (int i = 0; i < stringArray.length; i++) {
            String numberAsString = stringArray[i];
            int iValue = Integer.parseInt(numberAsString);
            if (iValue > 0) {
                if (ON) {
                    sValue += String.format(Locale.US, "%02d", i + 1) + String.format("%02X", iValue);
                } else {
                    sValue += String.format(Locale.US, "%02d", i+1) + String.format("%02X", 0);
                }
            }
        }
        sValue = sValue.toUpperCase();
        return sValue;
    }

    public String convertRGBwithCommasToHexString(String sRGB) {
        String sValue = "";

        String[] stringArray = sRGB.split(",");
        for (int i = 0; i < stringArray.length; i++) {
            String numberAsString = stringArray[i];
            int iValue = Integer.parseInt(numberAsString);
            sValue += String.format("%02X", iValue);
        }
        sValue = sValue.toUpperCase();
        return sValue;
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
        int iPanels = 1;
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
        Log.d (TAG, "check_light_power_() - light power of 1 panel: " + iPower);


        String ee_noofpanels_tag = "eeprom_no_of_panels";

        SharedPreferences spConfig = getSharedPreferences(CONFIG_SETTINGS, 0);
        String s_eeprom_no_of_panels = spConfig.getString(ee_noofpanels_tag, "1");

        try {
            if (s_eeprom_no_of_panels.length() > 0) {
                if (Integer.parseInt(s_eeprom_no_of_panels) != 0) {
                    iPanels = Integer.parseInt(s_eeprom_no_of_panels);
                }
            }
        } catch (NullPointerException e) {
            makeToast("No of panels not defined for this unit");
        }
        logme(TAG, "check_light_power_() - multiplying power by '" + iPanels + "' panels.");
        iPower *= iPanels;
        Log.d (TAG, "check_light_power_() - Overall light power for all panels: " + iPower);
        return iPower;
    }

    /*private int get_max_power() {
        SharedPreferences spFile = getSharedPreferences(Constants.PREFS_PSU_CURRENT, 0);
        //Float fPower = spFile.getFloat("psu_current", 0.0f) * 1000;
        int power = 0;
        try {
             power = spFile.getInt("psu_current", 0);
        } catch (NumberFormatException e) {
            makeToast("Unable to get the stored PSU power value");
        }
        return power;
    }*/

    private int get_max_power() {
        int power = 0;
        logme(TAG, "get_max_power_() - checking max PSU power from " + CONFIG_SETTINGS);
        String ee_psucurrent_tag = "eeprom_PSU_current";

        SharedPreferences spConfig = getSharedPreferences(CONFIG_SETTINGS, 0);
        String s_eeprom_PSU_current = spConfig.getString(ee_psucurrent_tag, "");

        try {
            if (s_eeprom_PSU_current.length() > 0) {
                if (Integer.parseInt(s_eeprom_PSU_current) != 0) {
                    power = Integer.parseInt(s_eeprom_PSU_current);
                }
            }
        } catch (NullPointerException e) {
            makeToast("PSU current not yet defined for this unit");
        }
        logme(TAG, "get_max_power_() - max PSU current defined for this unit is: " + power);
        return power;
    }

    private void display_popup_message(String title, String message) {

        AlertDialog dlg = new AlertDialog.Builder(this).create();
        dlg.setTitle(title);
        dlg.setMessage(message);
        dlg.setIcon(R.drawable.icon_main);
        dlg.show();
    }

    public void mark_all_buttons_off_on_mobile(boolean bl_force_LOW_off) {
        blLamp1_ON = false;
        btnL1.setBackgroundResource(R.drawable.buttonselector_main);
        btnL1.setTextColor(Color.WHITE);
        blLamp2_ON = false;
        btnL2.setBackgroundResource(R.drawable.buttonselector_main);
        btnL2.setTextColor(Color.WHITE);
        blLamp3_ON = false;
        btnL3.setBackgroundResource(R.drawable.buttonselector_main);
        btnL3.setTextColor(Color.WHITE);
        blLamp4_ON = false;
        btnL4.setBackgroundResource(R.drawable.buttonselector_main);
        btnL4.setTextColor(Color.WHITE);
        blLamp5_ON = false;
        btnL5.setBackgroundResource(R.drawable.buttonselector_main);
        btnL5.setTextColor(Color.WHITE);
        blLamp6_ON = false;
        btnL6.setBackgroundResource(R.drawable.buttonselector_main);
        btnL6.setTextColor(Color.WHITE);
        blLamp7_ON = false;
        btnL7.setBackgroundResource(R.drawable.buttonselector_main);
        btnL7.setTextColor(Color.WHITE);

        if (bl_force_LOW_off) {
            btnLOW.setBackgroundResource(R.drawable.buttonselector_main);
            btnLOW.setTextColor(Color.WHITE);
            Log.d(TAG, "Forcing LOW button OFF on Digital Panel");
        }


        btnL9.setBackgroundResource(R.drawable.buttonselector_main);
        btnL9.setTextColor(Color.WHITE);

        blLamp10_ON = false;
        btnL10.setBackgroundResource(R.drawable.buttonselector_main);
        btnL10.setTextColor(Color.WHITE);
        blLamp11_ON = false;
        btnL11.setBackgroundResource(R.drawable.buttonselector_main);
        btnL11.setTextColor(Color.WHITE);
        blLamp12_ON = false;
        btnL12.setBackgroundResource(R.drawable.buttonselector_main);
        btnL12.setTextColor(Color.WHITE);
    }

    public void buttonOFF(View v) {
        BL_LOW_MODE = false;
        allOFF(v);
    }

    public void allOFF(View v) {

        String sCommand = "";
        blLamp1_ON = false;
        btnL1.setBackgroundResource(R.drawable.buttonselector_main);
        btnL1.setTextColor(Color.WHITE);
        blLamp2_ON = false;
        btnL2.setBackgroundResource(R.drawable.buttonselector_main);
        btnL2.setTextColor(Color.WHITE);
        blLamp3_ON = false;
        btnL3.setBackgroundResource(R.drawable.buttonselector_main);
        btnL3.setTextColor(Color.WHITE);
        blLamp4_ON = false;
        btnL4.setBackgroundResource(R.drawable.buttonselector_main);
        btnL4.setTextColor(Color.WHITE);
        blLamp5_ON = false;
        btnL5.setBackgroundResource(R.drawable.buttonselector_main);
        btnL5.setTextColor(Color.WHITE);
        blLamp6_ON = false;
        btnL6.setBackgroundResource(R.drawable.buttonselector_main);
        btnL6.setTextColor(Color.WHITE);

        blPRG_ON = false;
        btnL7.setBackgroundResource(R.drawable.buttonselector_main);
        btnL7.setTextColor(Color.WHITE);


        blLamp10_ON = false;
        btnL10.setBackgroundResource(R.drawable.buttonselector_main);
        btnL10.setTextColor(Color.WHITE);
        blLamp11_ON = false;
        btnL11.setBackgroundResource(R.drawable.buttonselector_main);
        btnL11.setTextColor(Color.WHITE);
        blLamp12_ON = false;
        btnL12.setBackgroundResource(R.drawable.buttonselector_main);
        btnL12.setTextColor(Color.WHITE);

//        sCommand= "B,OFF1$" + sNewLine;
//        send_via_bt(sCommand);
//        lclUsbServiceInstance.sendBytes(sCommand.getBytes());

        sCommand = "X101" + S_COMMAND_SEPARATOR + "OFF" + S_COMMAND_SEPARATOR + "1$" + sNewLine;
        send_via_bt(sCommand);

        //blLamp9_ON = true;
        if (!BL_LOW_MODE) {
            btnL9.setBackgroundResource(R.drawable.buttonselector_active);
            btnL9.setTextColor(Color.BLACK);

            btnLOW.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
            btnLOW.setTextColor(Color.WHITE);

        }

        BL_TL84_ON = false;
        BL_UV_MODE = false;
    }

    public void populateLampsState() {
        SharedPreferences spsValues = getSharedPreferences(LightSettings.SHAREDPREFS_LAMP_STATE, MODE_PRIVATE);
        String sReturn = spsValues.getString("LAMPS", "");
        String[] sLampState = sReturn.split(",");

        //makeToast("Populating lamp states: " + sReturn);

        if (sLampState.length == 5) {
            if (sLampState[1].equals("1")) {
                blLamp1_ON = false;
                btnL1.performClick();
            }

            if (sLampState[2].equals("1")) {
                blLamp2_ON = false;
                btnL2.performClick();
            }

            if (sLampState[3].equals("1")) {
                blLamp3_ON = false;
                btnL3.performClick();
            }

            if (sLampState[4].equals("1")) {
                blLamp4_ON = false;
                btnL4.performClick();
            }
        }
    }

    public boolean bluetooth_connected() {
        SharedPreferences spFile = getSharedPreferences(BT_CONNECTED_PREFS, 0);
        return spFile.getBoolean("CONNECTED", false);
    }

    public void switch_preset_on (String sPresetName) {
        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        String sPresetRGBValues;
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        String sCommand;
        iGlobalIndex=1;

        iGlobalIndex = json_analyst.getPresetIndex(sPresetName);
        if (iGlobalIndex > 0) {
            //makeToast("FOUND. Switching on lamp preset:" + sPresetName + " (index = " + iGlobalIndex + ")");
            lclHandler.post(new Runnable() {
                @Override
                public void run() {
                    switchOnLamp(iGlobalIndex);
                    //formatTextCounter(txtCounterLED1, "1131");
                }
            });
        } else {
            makeToast("getPresetIndex returned 0 for preset:" + sPresetName);
        }

        /*Map<String,?> keys = spFile.getAll();

        makeToast("entering loop to find index of preset '" + sPresetName + "'.");
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if (entry.getKey() != null) {
                makeToast("Comparing value of '"+ entry.getValue().toString() + "'.");
                if (entry.getValue().toString().equals(sPresetName)) {
                    makeToast("FOUND. Switching on lamp preset:" + entry.getValue().toString() + " (index = " + iGlobalIndex + ")");
                    lclHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            switchOnLamp(iGlobalIndex);
                            //formatTextCounter(txtCounterLED1, "1131");
                        }
                    });

                }
            }
            iGlobalIndex++;
        }*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(Menu.NONE, 2, 2, "Operating Hours").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 3, 3, "Sequence Settings (PRG)").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 4, 4, "Settings").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 5, 5, "Manual").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 6, 6, "Maintenance").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 7, 7, "License Page").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 8, 8, "About").setIcon(
                getResources().getDrawable(R.drawable.icon_information));

//        menu.add(Menu.NONE, 9, 9, "TEST PRESETS").setIcon(
//                getResources().getDrawable(R.drawable.icon_information));

        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem item = menu.findItem(R.id.menu_color_picker);


        return true;
    }

    public String getSystemTime() {
        Date currentTime = Calendar.getInstance().getTime();
        String sTimeNow = DateFormat.format("HH:mm:ss", currentTime).toString();
        return sTimeNow;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {

            case 1:
                Intent intent4 = new Intent(TRSDigitalPanel.this, TRSDigitalPanel.class);
                startActivity(intent4);
                break;

            case 2:
                Intent intent5 = new Intent(TRSDigitalPanel.this, TRSLightOperatingHours.class);
                startActivity(intent5);
                break;

            case 3:
                Intent intent6 = new Intent(TRSDigitalPanel.this, TRSSequence.class);
                startActivity(intent6);
                break;

            case 4:
                Intent intent7 = new Intent(TRSDigitalPanel.this, TRSSettings.class);
                startActivity(intent7);
                break;

            case 5:
                Intent intent8 = new Intent(TRSDigitalPanel.this, TRSManualPage.class);
                startActivity(intent8);
                break;

            case 6:
                //Recertification page
                goto_maintenance(null);
                //startActivity(intent9);
                break;

            case 7:
                Intent intent = new Intent(TRSDigitalPanel.this, LicenseClass.class);
                startActivity(intent);
                break;

            case 8:
                openAboutDialog();
                break;

//            case 9:
//                Intent intent_test = new Intent(TRSDigitalPanel.this, LightSettings.class);
//                startActivity(intent_test);
//                break;
            /*case 99:
                Intent intent99 = new Intent(TRSDigitalPanel.this, TstVrly.class);
                startActivity(intent99);
                break;*/

        }
        return true;
    }

    public void goto_maintenance(final View view) {

        if (BtCore.Connected() || true) {
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

                            Intent intent = new Intent(TRSDigitalPanel.this, TRSMaintenancePage.class);
                            startActivity(intent);
                        } else {
                            makeToast("Password incorrect");
                        }
                    })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            // create an alert dialog
            AlertDialog alertD = alertDialogBuilder.create();
            alertD.show();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(edtInput, InputMethodManager.SHOW_IMPLICIT);
        } else {
            Toast.makeText(this, "goto_recertification: Please connect to the RGB LED first.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFWver_JSON() {
        String sReturn = "";
        sReturn = extractJSONvalue("", "fw_vrsn");

        return sReturn;
    }

    public boolean display_custom_data() {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, Context.MODE_PRIVATE);
        Boolean bl_custom_data = prefs.getBoolean(Constants.CUSTOMER_DATA_FLAG, false);
        Log.d(TAG, "display_custom_data_(): USE CUSTOMER DATA = " + bl_custom_data);
        return bl_custom_data;
    }

    private void openAboutDialog() {
        String sFWverLcl = getFWver_JSON();

        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.about_popup_view, null);
        dlgBuilder.setView(dialogView);
        dlgBuilder.setTitle(getString(R.string.app_header_title) + " App " + apkVersion);

        TextView tv_fwversion = dialogView.findViewById(R.id.FWversion_value);
        tv_fwversion.setText(sFWverLcl);

        if (display_custom_data()) {
            String about_dialog_line1 = fileUtilities.get_value_from_customer_data(Constants.CUSTOMER_DATA_ABOUT_PAGE_LINE_1_TAG);
            String about_dialog_line2 = fileUtilities.get_value_from_customer_data(Constants.CUSTOMER_DATA_ABOUT_PAGE_LINE_2_TAG);
            String about_dialog_line3 = fileUtilities.get_value_from_customer_data(Constants.CUSTOMER_DATA_ABOUT_PAGE_LINE_3_TAG);
            String about_dialog_line4 = fileUtilities.get_value_from_customer_data(Constants.CUSTOMER_DATA_ABOUT_PAGE_LINE_4_TAG);
            String about_dialog_hyperlink = fileUtilities.get_value_from_customer_data(Constants.CUSTOMER_DATA_ABOUT_PAGE_HYPERLINK);

            about_dialog_line1 = about_dialog_line1.replace("\\n", System.lineSeparator());
            about_dialog_line2 = about_dialog_line2.replace("\\n", System.lineSeparator());
            about_dialog_line3 = about_dialog_line3.replace("\\n", System.lineSeparator());
            about_dialog_line4 = about_dialog_line4.replace("\\n", System.lineSeparator());

            Log.d(TAG, "openAboutDialog_() - setting custom data in line 1: " + about_dialog_line1);
            TextView view = dialogView.findViewById(R.id.about_line_1);
            view.setText(about_dialog_line1);

            view = dialogView.findViewById(R.id.about_line_2);
            view.setText(about_dialog_line2);

            view = dialogView.findViewById(R.id.about_hyperlink);
            view.setText(about_dialog_hyperlink);


        }


        dlgBuilder.setIcon(R.drawable.icon_main);
        AlertDialog alertDialog = dlgBuilder.create();
        dlgBuilder.show();
    }

    public void switchOnLamp (int iIndex) {

        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        String sPresetRGBValues;
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        String sCommand;

        sPresetRGBValues = json_analyst.getJSONValue("preset" + iIndex + "_rgbw");
        //makeToast("SWitching on lamp index "+iIndex);
        String[] sRGB = sPresetRGBValues.split(",");
        for (int i=0; i < sRGB.length; i++) {
            if (i < 9) {
                sCommand = "S0" + (i+1) + sRGB[i] + sNewLine;
                lclUsbServiceInstance.sendBytes(sCommand.getBytes());
            } else {
                sCommand = "S" + (i+1) + sRGB[i] + sNewLine;
                lclUsbServiceInstance.sendBytes(sCommand.getBytes());
            }
            //Toast.makeText(context, sCommand, Toast.LENGTH_LONG).show();
        }
    }

    public void duplicateSPFile() {
        //sp1 is the shared pref to copy to

        SharedPreferences prefsFrom = getSharedPreferences(SP_SEQUENCE_COMMAND_GENERATED, 0);
        SharedPreferences prefsTo = getSharedPreferences(SP_SEQUENCE_COMMAND_EXECUTED, 0);
        SharedPreferences.Editor ed = prefsTo.edit();
        //SharedPreferences sp = prefsTo; //The shared preferences to copy from
        ed.clear(); // This clears the one we are copying to, but you don't necessarily need to do that.

        for(Map.Entry<String,?> entry : prefsFrom.getAll().entrySet()){
            Object v = entry.getValue();
            String key = entry.getKey();
            //Now we just figure out what type it is, so we can copy it.
            // Note that i am using Boolean and Integer instead of boolean and int.
            // That's because the Entry class can only hold objects and int and boolean are primatives.
            if(v instanceof Boolean)
                // Also note that i have to cast the object to a Boolean
                // and then use .booleanValue to get the boolean
                ed.putBoolean(key, ((Boolean)v).booleanValue());
            else if(v instanceof Float)
                ed.putFloat(key, ((Float)v).floatValue());
            else if(v instanceof Integer)
                ed.putInt(key, ((Integer)v).intValue());
            else if(v instanceof Long)
                ed.putLong(key, ((Long)v).longValue());
            else if(v instanceof String)
                ed.putString(key, ((String)v));
        }
        ed.commit(); //save it.
    }

    public int getPrefsFileSize(String sFileName) {
        SharedPreferences prefs = getSharedPreferences(SP_SEQUENCE_COMMAND_EXECUTED, 0);
        Map<String,?> mapSequence = prefs.getAll();
        SharedPreferences.Editor ed = prefs.edit();

        int i = mapSequence.size();

        return i;
    }

    public void processSequenceFile() {

        //if (blSeqGreenLight) {
            Log.d (TAG, getPrefsFileSize(SP_SEQUENCE_COMMAND_EXECUTED) + " iterations left.");
            SharedPreferences prefs = getSharedPreferences(SP_SEQUENCE_COMMAND_EXECUTED, 0);
            SharedPreferences.Editor ed = prefs.edit();
            Map<String,?> mapSequence = prefs.getAll();
            SortedSet<String> keys = new TreeSet<>(mapSequence.keySet());
            for (String key : keys) {
                if (blSeqGreenLight) {
                    String value = mapSequence.get(key).toString();
                    // do something

                    List<String> presetString = Arrays.asList(value.split(","));
                    String sPresetName = presetString.get(0);
                    String sTimer = presetString.get(1);
                    sTimer = sTimer.replaceAll("\\[", "").replaceAll("\\]","");
                    sPresetName = sPresetName.replaceAll("\\[", "").replaceAll("\\]","");

                    Integer iTimer = Integer.valueOf(sTimer);

                    //makeToast("Switching on timed preset '" + sPresetName +"'.");
                    logme(TAG, "Executing preset #" + sPresetName + "# for " + iTimer + " seconds via reverseTimer()");
                    S_CURRENT_SEQ_ITEM = sPresetName;
                    reverseTimer(iTimer, sPresetName);
                    logme(TAG, "Removing preset with key="+key+".");
                    blSeqGreenLight = false;
                }
            }
        //} else {
            //logme(TAG, "Sequence execution finished");
       // }
    }

    public void executePRG (final View v) {

        makeToast("Executing sequence ... ");

        blPRG_ON = true;
        btnL7.setBackgroundResource(R.drawable.buttonselector_active);
        btnL7.setTextColor(Color.BLACK);

        String sCommand = "X101" + S_COMMAND_SEPARATOR + "PRG" + S_COMMAND_SEPARATOR + "1$" + sNewLine;
        send_via_bt(sCommand);
        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
        logme(TAG, "Sending Bytes: " + sCommand);
    }


    private void check_for_customer_data_use() {

        Log.d(TAG, "check_for_customer_data_use_() - executing function");
        PackageManager m = getPackageManager();
        String s = getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        
        if (customer_zipfile_processed()) {
            logme(TAG, "check_for_customer_data_use_() - customer zipfile already processd.");
            if (use_custom_logo()) {
                logme(TAG, "check_for_customer_data_use_() - changing logo to customer's.");
                change_logo(s + "/" + Constants.CUSTOMER_LOGO_FILENAME);
            }
            return;
        }
        
        logme(TAG, "check_for_customer_data_use_() - looking for customer ZIP file");
        
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        if (fileExists(path + "/" + Constants.CUSTOMER_DATA_ARCHIVE_FILENAME)) {
            logme(TAG, "check_for_customer_data_use_() - ZIP file '" + Constants.CUSTOMER_DATA_ARCHIVE_FILENAME + "' found in path: " + path);
            unzip(path + "/" + Constants.CUSTOMER_DATA_ARCHIVE_FILENAME, s);
            logme(TAG, "check_for_customer_data_use_() - ZIP file '" + Constants.CUSTOMER_DATA_ARCHIVE_FILENAME + "' unzipped to path: " + s);
            mark_customer_zipfile_processed_in_config(true);

            fileUtilities = new FileUtilities(get_path_to_customer_datafile(), get_path_to_customer_logofile());
            if (fileUtilities.logoFileExists()) {
                logme(TAG, "check_for_customer_data_use_() - LOGO file '" + Constants.CUSTOMER_LOGO_FILENAME + "' found in path: " + s+ ". Using new logo.");
                mark_customer_data_use_in_config(true);
                change_logo(s + "/" + Constants.CUSTOMER_LOGO_FILENAME);
            }
        } else {
            logme(TAG, "check_for_customer_data_use_() - ZIP file '" + Constants.CUSTOMER_DATA_ARCHIVE_FILENAME + "' not found. Stock execution");
        }
        //process_customer_data(s + "/" + Constants.CUSTOMER_DATA_FILENAME);
    }

    public boolean fileExists(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            logme(TAG, "FILE EXISTS: " + filePath);
            return true;
        } else {
            return false;
        }
    }
    
    private boolean customer_zipfile_processed() {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, Context.MODE_PRIVATE);
        Boolean ret = prefs.getBoolean(Constants.CUSTOMER_ZIPFILE_PROCESSED, false);

        logme(TAG, "TAG: " + Constants.CUSTOMER_ZIPFILE_PROCESSED + ": " + ret);
        return ret;
    }

    private boolean use_custom_logo() {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, Context.MODE_PRIVATE);

        Boolean ret = prefs.getBoolean(Constants.CUSTOMER_LOGO_FLAG, false);
        return ret;
    }


    public boolean trim_sequence_file(String sCurrentItem) {
        /*  This function accepts a String preset name and looks to find it in the SP_SEQUENCE_COMMAND_EXECUTED file
            If it finds it, it removes the item, therefore decreasing the number of lines left to be executed in sequence
         */
        boolean blReturnStatus = false;

        SharedPreferences prefsFile = getSharedPreferences(SP_SEQUENCE_COMMAND_EXECUTED, 0);
        SharedPreferences.Editor editor = prefsFile.edit();

        Map<String,?> mapSequence = prefsFile.getAll();

        //makeToast("trim_sequence_file(): Looking for '" + sCurrentItem + "'");
        SortedSet<String> keys = new TreeSet<>(mapSequence.keySet());
        for (String key : keys) {
            //String value = mapSequence.get(key).toString();
            // do something
                String value = mapSequence.get(key).toString();
                List<String> presetString = Arrays.asList(value.split(","));
                String sPresetName = presetString.get(0);
                String sTimer = presetString.get(1);
                sTimer = sTimer.replaceAll("\\[", "").replaceAll("\\]","");
                sPresetName = sPresetName.replaceAll("\\[", "").replaceAll("\\]","");

                //makeToast("trim_sequence_file(): Comparing :" + sCurrentItem + ": against :" + sPresetName + ":");
                if (sPresetName.equals(sCurrentItem)) {
                    //makeToast("trim_sequence_file(): found match! :" + sCurrentItem + ": against :" + sPresetName + ":");
                    editor.remove(key);
                    editor.apply();
                    blReturnStatus = true;

                } else {
                    //makeToast("NO value found + blReturnStatus = " + blReturnStatus);
                }
            }
        return blReturnStatus;
    }

    public void reverseTimer(int Seconds, String sPresetName){

        new CountDownTimer(Seconds* 1000+1000, 2000) {

            public void onTick(long millisUntilFinished) {
                //makeToast("TICK " + sPresetName + " " + Seconds);
                int seconds = (int) (millisUntilFinished / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                logme(TAG, "TICK: " + seconds + " left for preset #" + sPresetName +"#");
                switch_preset_on(sPresetName);
                blSeqGreenLight = false;
            }

            public void onFinish() {
                trim_sequence_file(sPresetName);
                boolean blNextStep = (getPrefsFileSize(SP_SEQUENCE_COMMAND_EXECUTED) > 0);
                logme(TAG, "reverseTimer finished. Preset #" + sPresetName +"# removed from EXECUTE file. More presets left to execute: " + getPrefsFileSize(SP_SEQUENCE_COMMAND_EXECUTED));
                //makeToast("Executing trim sequence file. S_NEXT_SEQ_ITEM=" + S_NEXT_SEQ_ITEM);
                blSeqGreenLight = true;
                logme(TAG, "blSeqGreenLight = " + blSeqGreenLight);

                if (blNextStep) {
                    makeToast("NEXT ITEM IN SEQUENCE");
                    processSequenceFile();

                } else {
                    makeToast("SEQUENCE FINISHED");
                    allOFF(null);
                }
            }
        }.start();
    }

//    public void updateLampValue (String sCommand) {
//        SharedPreferences prefsCurrentState = getSharedPreferences(Constants.SHAREDPREFS_CURRENT_LAMPS, 0);
//        SharedPreferences.Editor editor = prefsCurrentState.edit();
//        String[] RGBValues = sCommand.split(",");
//        Map<String, String> map = new HashMap<>();
//        int i = 1;
//        for (String RGB : RGBValues) {
//            String sKey = "S" + String.format("%02d", i);
//            map.put(sKey, RGB);
//            i++;
//        }
//        TreeMap<String, String> sorted = new TreeMap<>();
//        // Copy all data from hashMap into TreeMap
//        sorted.putAll(map);
//
//        for (Map.Entry<String, String> entry : sorted.entrySet()) {
//            editor.putString(entry.getKey(), entry.getValue());
//            //logme(TAG, "sorted: " + entry.getKey() + ", " + entry.getValue());
//        }
//        editor.apply();
//    }

    public void btnLOW(View v) {
        SharedPreferences spLampDefinitions = getSharedPreferences(Constants.SHAREDPREFS_LAMP_DEFINITIONS, 0);

        if (BL_LOW_MODE) {
            btnLOW.setBackgroundResource(R.drawable.buttonselector_main);
            btnLOW.setTextColor(Color.WHITE);
        } else {
            btnLOW.setBackgroundResource(R.drawable.buttonselector_active);
            btnLOW.setTextColor(Color.BLACK);
        }

        String sCommand= "X101,LOW," + (BL_LOW_MODE ? 0 : 1) + "$" + sNewLine;
        logme("MORRIS-TRSDIGITAL", "btnLOW(): sending: " + sCommand);
        lclBTServiceInstance.sendData(sCommand);
        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
        BL_LOW_MODE = !BL_LOW_MODE;
    }

    private void move_custom_data_to_app_dir() {

    }

    private void clear_custom_data_to_app_dir() {

    }

    public void unzip (String _zipFile, String _targetLocation) {
        try {
            FileInputStream fin = new FileInputStream(_zipFile);
            ZipInputStream zin = new ZipInputStream(fin);

            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                FileOutputStream fout = new FileOutputStream(_targetLocation + "/" + ze.getName());

                for (int c = zin.read(); c != -1; c = zin.read()) {
                    fout.write(c);
                }
                zin.closeEntry();
                fout.close();
            }
            zin.close();
            //makeToast("Unzip successful");
        } catch (Exception e) {
            System.out.println(e);
            makeToast("Error when unzipping customer data");
            Log.e(TAG, "Error when unzipping: " + e.getMessage());
        }

    }

    public void change_logo(String path_to_logo) {
        if (!fileUtilities.logoFileExists()) {
            makeToast("Expecting file '" + Constants.CUSTOMER_LOGO_FILENAME + "' , but can't find it!");
            return;
        } else {
            logme(TAG, "change_logo_() - customer logo file found");
            File imgFile = new File(path_to_logo);
            Bitmap myBitmap = BitmapFactory.decodeFile(String.valueOf(imgFile));
            ImageView myImage = findViewById(R.id.unitycolor_logo);
            logme(TAG, "Setting logo with '" + path_to_logo + "'");
            myImage.setImageBitmap(myBitmap);
            logme(TAG, "Logo changed");
            mark_customer_logo_use_in_config(true);
        }
    }

    public void logme(String tag, String message) {
        SharedPreferences log_file = getSharedPreferences(Constants.SHAREDPREFS_DIAGNOSTIC_DATA, MODE_PRIVATE);
        SharedPreferences.Editor editor = log_file.edit();
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();

        editor.putString(ts, tag + " | " + message);
        editor.apply();

        Log.d(tag, message);
    }

    public void process_customer_data(String path_to_datafile) {
        if (!fileUtilities.dataFileExists()) {
            makeToast("File '" + Constants.CUSTOMER_DATA_FILENAME + "' couldn't be found!");
            return;
        } else {
            logme(TAG, "Customer data XML file found");
            try {
                logme(TAG, "Opening JSON file for parsing");
                JSONObject jsonObject = new JSONObject(get_string_from_file(path_to_datafile));
                String item = jsonObject.getString("manual_content");
                item.replace("\n", "\\n").replace("\r", "\\r");
                //logme(TAG, "manual content:" + item);
                //makeToast(item);
                mark_customer_data_use_in_config(true);
                makeToast("Customer data found");

            } catch (Exception e) {
                Log.w(TAG, "Parsing JSON failed. Check format.");

                e.printStackTrace();
            }
            logme(TAG, "Customer data processed");
        }
    }

    public static String get_string_from_file(String filePath) throws Exception {
        File file = new File(filePath);
        FileInputStream fin = new FileInputStream(file);
        String ret = convertStreamToString(fin);

        fin.close();
        return ret;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append(System.lineSeparator());
        }
        reader.close();
        return sb.toString();
    }

    private String get_path_to_customer_datafile() {
        PackageManager m = getPackageManager();
        String s = getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return s + "/" + Constants.CUSTOMER_DATA_FILENAME;
    }

    private String get_path_to_customer_logofile() {
        PackageManager m = getPackageManager();
        String s = getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return s + "/" + Constants.CUSTOMER_LOGO_FILENAME;
    }

    public void makeToast (String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /*static public class AsyncLampControl extends AsyncTask<Void, Void, String> {

        private Context mContext;
        private UsbCOMMsService mUsbService;


        private String sBuffer;
        private String sPresetName;
        private int index;


        public AsyncLampControl(Context context, String sPresetName, int index) {
            mContext = context;
            this.sPresetName = sPresetName;
            this.index = index;
        }
        @Override protected String doInBackground(Void... params) {
            String sResult = "";

            return sResult;

        }

        @Override protected void onPostExecute(String result) {

        }
    }*/
}
