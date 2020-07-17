package com.kiand.LED2match;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.kiand.LED2match.BtCOMMsService.SHAREDPREFS_UNITNAME;
import static com.kiand.LED2match.BtCOMMsService.BT_CONNECTED_PREFS;
import static com.kiand.LED2match.Constants.CONFIG_SETTINGS;
import static com.kiand.LED2match.Constants.DEFAULT_PSU_POWER;
import static com.kiand.LED2match.Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE;
import static com.kiand.LED2match.Constants.sNewLine;
import static com.kiand.LED2match.TRSSequence_old.SP_LAMP_TIMERS;
import static com.kiand.LED2match.TRSSettings.TL84_DELAY_KEY;

public class TRSDigitalPanel extends Activity {

    public static final String TAG = "morris-TRSDigitalPanel";
    private static final int TOAST_MESSAGE = 1;

    public String S_CURRENT_SEQ_ITEM = "";
    public boolean BL_LOW_MODE = false;
    public boolean BL_UV_MODE = false;
    public boolean BL_COMMAND_SENT = false;
    public static final String NO_PRESET_TEXT = "#n/a";
    public static final String TL84_TAG = "TL84";
    private static final String password = "hokus";
    private static final int MSG_SHOW_TOAST = 1;
    private static final int ICON_HEIGHT = 80;

    boolean blLamp1_ON, blLamp2_ON, blLamp3_ON, blLamp4_ON, blLamp5_ON, blLamp6_ON, blLamp7_ON, blTL84_ON;
    Button btnL1, btnL2, btnL3, btnL4, btnL5, btnL6, btnL7, btnLOW, btnL9, btnL10;
    ImageView usb_conn_indicator;
    ImageView bt_conn_indicator;
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

    private Handler messageHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SHOW_TOAST) {
                String message = (String)msg.obj;

                switch (msg.what) {
                    case TOAST_MESSAGE:
                        Toast.makeText(MyApplication.getAppContext(), message.getBytes().toString() , Toast.LENGTH_SHORT).show();
                        break;

                    //handle the result here

                    default:
                        //super.handleMessage(msg);
                        break;
                }

            }
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            if (intent == null) {
                return;
            }
            if (intent.getAction().equals("temperature_reading_event")) {
                String message = intent.getStringExtra("temperature");
                Log.d(TAG, "Got message: " + message);
                TextView textTemperature = findViewById(R.id.temperature_textview);
                message += "\u2103";
                textTemperature.setText(message);

            } else if (intent.getAction().equals("button_highlight_event")) {
                String index = intent.getStringExtra("button_index");
                //Log.d(TAG, "Got index of button to highlight: " + index);
                if (TextUtils.isEmpty(index)) {
                    return;
                }
                switchButtonON(Integer.valueOf(index));
                //Log.d(TAG, "Button: " + index + " highlighted");
            } else if (intent.getAction().equals("controller_data_refreshed_event")) {
                SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
                JSON_analyst json_analyst = new JSON_analyst(spFile);
                String sFWVersion = json_analyst.getJSONValue("firmware_version");
                TextView tv_firmware_version = findViewById(R.id.fw_version);
                tv_firmware_version.setText(sFWVersion);
                //makeToast("fw_version intent received");
            } else if (intent.getAction().equals("button_highlight_extra")) {
                String name = intent.getStringExtra("button_name");
                //Log.d(TAG, "Got additional button to highlight: " + name);
                if (TextUtils.isEmpty(name)) {
                    return;
                }

                if (name.equalsIgnoreCase("PRG")){
                    switchButtonON(7);
                   //Log.d(TAG, "PRG Button highlighted");
                }

                if (name.equalsIgnoreCase("LOW")){
                    if (!BL_LOW_MODE) {
                        switchButtonON(8);
                        //Log.d(TAG, "LOW Button highlighted");
                    } else {
                        switchButtonOFF(8);
                       // Log.d(TAG, "LOW Button deactivated");
                    }
                }

                if (name.equalsIgnoreCase("OFF")){
                    switchButtonON(9);
                    //Log.d(TAG, "OFF Button highlighted");
                }
            }

        }
    };


    protected void onResume()
    {
        super.onResume();

        usb_conn_indicator.getLayoutParams().height= ICON_HEIGHT;
        usb_conn_indicator.requestLayout();

        bt_conn_indicator.getLayoutParams().height= ICON_HEIGHT;
        bt_conn_indicator.requestLayout();

        IntentFilter filter = new IntentFilter();
        filter.addAction("temperature_reading_event");
        filter.addAction("controller_data_refreshed_event");
        filter.addAction("button_highlight_event");
        filter.addAction("button_highlight_extra");

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
        //Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();
        if (!mBound) {
            Intent intent = new Intent(this, UsbCOMMsService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            //Toast.makeText(this.getBaseContext(),"Service bound (onResume)", Toast.LENGTH_SHORT).show();

            SystemClock.sleep(50);
            if (mBound) {
                String sCommand = "F" + Constants.sNewLine;
                lclUsbServiceInstance.sendBytes(sCommand.getBytes());
            }
            mBound = true;
        }

        Log.d(TAG, "starting Bluetooth connection service");

        if (!mBoundBT) {
            Intent intent = new Intent(this, BtCOMMsService.class);
            //startService(intent);
            bindService(intent, btConnection, Context.BIND_AUTO_CREATE);
            //Toast.makeText(this.getBaseContext(),"Service bound (onResume)", Toast.LENGTH_SHORT).show();

            SystemClock.sleep(50);
            mBoundBT = true;

            if (mBoundBT) {
                if (bluetooth_connected()) {
                    try {
                        String sSequence = "F";
                        sSequence = sSequence.concat(System.lineSeparator());

                        lclBTServiceInstance.sendData(sSequence);
                    } catch (NullPointerException e) {
                        Log.e(TAG, "NullPointerException when sending command via Bluetooth");
                    }
                    SystemClock.sleep(1500);
                }
                if (bluetooth_connected()) {
                    try {
                        String sSequence = "G";
                        sSequence = sSequence.concat(System.lineSeparator());
                        lclBTServiceInstance.sendData(sSequence);
                    } catch (NullPointerException e) {
                        Log.e(TAG, "NullPointerException when sending command via Bluetooth");
                    }
                    SystemClock.sleep(50);
                }
                if (bluetooth_connected()) {
                    try {
                        String sSequence = "J";
                        sSequence = sSequence.concat(System.lineSeparator());
                        lclBTServiceInstance.sendData(sSequence);
                    } catch (NullPointerException e) {
                        Log.e(TAG, "NullPointerException when sending command via Bluetooth");
                    }
                }
            }
        }


        Float versionName = BuildConfig.VERSION_CODE / 1000.0f;
        TextView tvInfoBox = findViewById(R.id.infobox);
        String version_line = "APP version: " + versionName;
        tvInfoBox.setText(version_line);

        if (shared_prefs_exists(SHAREDPREFS_LAMP_ASSIGNMENTS, "666")) {
            repopulate_button_assignments();
            Log.d(TAG, "Repopulating button captions");
        } else {
            Log.d(TAG, "Key 666 not found. Populating button names from JSON");
            populateButtonNames();
        }


        populate_bluetooth_indicator();
        populateLampsState();
        setUnitName();
        makeToast("onresume");
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
        if (mBound) {
            unbindService(mConnection);
        }
        if (mBoundBT) {
            unbindService(btConnection);
        }
        mBoundBT = false;
        mBound = false;

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
        //mark_BT_disconnected();


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
        btnL10 = findViewById(R.id.btnL10); //reassign
        usb_conn_indicator = findViewById(R.id.usb_connection_image);
        bt_conn_indicator = findViewById(R.id.bt_connection_image);

        populateButtonNames();

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

        btnL10.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_special));
        btnL10.setTextColor(Color.BLACK);

        if (shared_prefs_exists(SHAREDPREFS_LAMP_ASSIGNMENTS, "1")) {
            repopulate_button_assignments();
        } else {
            populateButtonNames();
        }

    }

    public void reassign_lamps(View v) {

        String sTags = "";
        if (!btnL1.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL1.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL2.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL2.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL3.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL3.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL4.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL4.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL5.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL5.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL6.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL6.getText().toString() + ","; } else { sTags = sTags + ","; }


        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        String sPresetCounter = json_analyst.getJSONValue("preset_counter");

        int iCtr = ((!sPresetCounter.trim().equals("") ? Integer.valueOf(sPresetCounter) : 0));

        Intent intentLampAssignment = new Intent(TRSDigitalPanel.this, ReassignLamps.class);
        intentLampAssignment.putExtra("tags", sTags);
        intentLampAssignment.putExtra("counter", iCtr);
        startActivity(intentLampAssignment);

    }

    public void mark_BT_connnected() {
        SharedPreferences prefs = getSharedPreferences(BT_CONNECTED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = prefs.edit();

        spEditor.clear();
        spEditor.putBoolean("CONNECTED", true);
        spEditor.apply();
        Log.d(TAG, "BT connection marked as true in the sp file");
    }

    public void mark_BT_disconnected() {
        makeToast("Entering mark BT disconnected");
        SharedPreferences prefs = getSharedPreferences(BT_CONNECTED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.clear();
        spEditor.putBoolean("CONNECTED", false);
        spEditor.apply();
        Log.d(TAG, "BT connection marked as false in the sp file");
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

    public boolean shared_prefs_exists(String sFileName, String sKey) {
        SharedPreferences spFile = getSharedPreferences(sFileName, 0);
        Log.d(TAG, "Shared_prefs_exists (" + sFileName+ "): " + spFile.contains(sKey));
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

                //Log.d(TAG, entry.getKey() + ":" + entry.getValue());
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
                makeToast("LOW mode switched off");

                break;

            case 11:
                if (blTL84_ON) {
                    blTL84_ON = false;
                }

        }

    }

    public void switchButtonON(int index) {
        switch(index) {
            case 1:
                if (!blLamp1_ON) {
                    mark_all_buttons_off_on_mobile();
                    blLamp1_ON = true;
                    btnL1.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL1.setTextColor(Color.BLACK);
                    makeToast("Light switched on");
                }
                break;

            case 2:
                if (!blLamp2_ON) {
                    mark_all_buttons_off_on_mobile();
                    blLamp2_ON = true;
                    btnL2.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL2.setTextColor(Color.BLACK);
                    makeToast("Light switched on");
                }
                break;

            case 3:
                if (!blLamp3_ON) {
                    mark_all_buttons_off_on_mobile();
                    blLamp3_ON = true;
                    btnL3.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL3.setTextColor(Color.BLACK);
                    makeToast("Light switched on");
                }
                break;

            case 4:
                if (!blLamp4_ON) {
                    mark_all_buttons_off_on_mobile();
                    blLamp4_ON = true;
                    btnL4.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL4.setTextColor(Color.BLACK);
                    makeToast("Light switched on");
                }
                break;

            case 5:
                if (!blLamp5_ON) {
                    mark_all_buttons_off_on_mobile();
                    blLamp5_ON = true;
                    btnL5.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL5.setTextColor(Color.BLACK);
                    makeToast("Light switched on");
                }
                break;

            case 6:
                if (!blLamp6_ON) {
                    mark_all_buttons_off_on_mobile();
                    blLamp6_ON = true;
                    btnL6.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL6.setTextColor(Color.BLACK);
                    makeToast("Light switched on");
                }
                break;

            case 7: //PRG
                mark_all_buttons_off_on_mobile();
                btnL7.setBackgroundResource(R.drawable.buttonselector_active);
                btnL7.setTextColor(Color.BLACK);
                makeToast("SEQUENCE switched on");
                break;

            case 8: //LOW
                btnLOW.setBackgroundResource(R.drawable.buttonselector_active);
                btnLOW.setTextColor(Color.BLACK);
                makeToast("LOW mode switched on");
                break;

            case 9: //OFF
                mark_all_buttons_off_on_mobile();
                btnL9.setBackgroundResource(R.drawable.buttonselector_active);
                btnL9.setTextColor(Color.BLACK);
                makeToast("All lights switched off");
                break;

            case 11:
                if (!blTL84_ON) {
                    mark_all_buttons_off_on_mobile();
                    blTL84_ON = true;
                }

        }

    }

    private void setUnitName() {
        SharedPreferences spUnitName = getSharedPreferences(SHAREDPREFS_UNITNAME, 0);
        String sUnitName = spUnitName.getString("UNIT_NAME", "not_loaded");
        messageHandler.post(new Runnable() {
            @Override
            public void run() {
                setTitle(getResources().getString(R.string.app_header_title) + " " + sUnitName);
            }
        });
    }

    public void populateButtonNames() {
        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        //Log.d(TAG, "bluetoothAskReply(V1)");
        final String sLamp1Name = json_analyst.getJSONValue("preset1_name");
        final String sLamp2Name = json_analyst.getJSONValue("preset2_name");
        final String sLamp3Name = json_analyst.getJSONValue("preset3_name");
        final String sLamp4Name = json_analyst.getJSONValue("preset4_name");
        final String sLamp5Name = json_analyst.getJSONValue("preset5_name");
        final String sLamp6Name = json_analyst.getJSONValue("preset6_name");
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
        }

    }
    public String get_tl84_delay() {
        SharedPreferences config_prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, 0);
        Log.d(TAG, " ** TL84_delay from file: " + String.format(Locale.US, "%04d", config_prefs.getInt(TL84_DELAY_KEY, 0)));
        return String.format(Locale.US, "%04d", config_prefs.getInt(TL84_DELAY_KEY, 0));

    }

    public void btnClicked(View v) {

        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        SharedPreferences spLampDefinitions = getSharedPreferences(Constants.SHAREDPREFS_LAMP_DEFINITIONS, 0);
        String sPresetRGBValues = "000,000,000,000,000,000,000,000,000,000";
        String sCommand = "";
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        String buttonCaption = v.getTag().toString();
        if (!TextUtils.isEmpty(buttonCaption)) {
            if (!buttonCaption.equalsIgnoreCase("LOW") && !buttonCaption.equalsIgnoreCase("UV")) {
                mark_all_buttons_off_on_mobile();
            }
        }

        BL_LOW_MODE = false;
        Log.d(TAG, "switching off LOW mode - shouldn't happen, this function is redundant");

        switch (v.getId()) {
            case R.id.btnL1:
                if (spLampDefinitions.contains(btnL1.getText().toString())) {
                    sPresetRGBValues = spLampDefinitions.getString(btnL1.getText().toString(), null);
                    Log.d(TAG, "sending " + sPresetRGBValues + " to controller");
                    if (sPresetRGBValues != null) {

                        if (btnL1.getText().toString().equalsIgnoreCase("UV")) {
                            //complementary light - keep current on and add UV definition for non-zeros - when switching ON UV mode. Otherwise switch off additional lamps
                            sCommand = "P" + convertRGB2complementaryLight(sPresetRGBValues, (!BL_UV_MODE));
                            sCommand += "$" + sNewLine;
                            if (mBoundBT) {
                                Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                                lclBTServiceInstance.sendData(sCommand);
                            } else {
                                Log.d(TAG, "Service btService not connected!");
                            }
                            lclUsbServiceInstance.sendBytes(sCommand.getBytes());

                            sCommand = "B," + btnL1.getTag().toString() + (BL_UV_MODE ? 0 : 1) + "$" + sNewLine;
                            if (mBoundBT) {
                                Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                                lclBTServiceInstance.sendData(sCommand);
                            } else {
                                Log.d(TAG, "Service btService not connected!");
                            }
                            lclUsbServiceInstance.sendBytes(sCommand.getBytes());

                            BL_UV_MODE = !BL_UV_MODE;
                            String s = (BL_UV_MODE ? "active" : "inactive");
                            Log.d(TAG, "UV mode - " + s);
                            if (BL_UV_MODE) {
                                btnL1.setBackgroundResource(R.drawable.buttonselector_active);
                                btnL1.setTextColor(Color.BLACK);
                            } else {
                                btnL1.setBackgroundResource(R.drawable.buttonselector_main);
                                btnL1.setTextColor(Color.WHITE);
                            }
                        } else {

                            sCommand = "S" + convertRGBwithCommasToHexString(sPresetRGBValues);
                            sCommand += "$" + sNewLine;
                            //if (btService.connected) {
                            if (mBoundBT) {
                                Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                                lclBTServiceInstance.sendData(sCommand);
                            } else {
                                Log.d(TAG, "Service btService not connected!");
                            }
                            lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                            sCommand = "B," + btnL1.getTag().toString() + "1$" + sNewLine;
                            if (mBoundBT) {
                                Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                                lclBTServiceInstance.sendData(sCommand);
                            } else {
                                Log.d(TAG, "Service btService not connected!");
                            }
                            lclUsbServiceInstance.sendBytes(sCommand.getBytes());

                            if (btnL1.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                                if (BL_LOW_MODE) {
                                    sCommand = "S11050" + sNewLine;
                                } else {
                                    sCommand = "S11100" + sNewLine;
                                }
                                if (mBoundBT) {
                                    //Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                                    lclBTServiceInstance.sendData(sCommand);
                                } else {
                                    Log.d(TAG, "Service btService not connected!");
                                }
                                lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                                blTL84_ON = true;
                            } else {
                                sCommand = "S11000" + sNewLine;
                                blTL84_ON = false;
                                if (mBoundBT) {
                                    lclBTServiceInstance.sendData(sCommand);
                                }
                            }
                            btnL1.setBackgroundResource(R.drawable.buttonselector_active);
                            btnL1.setTextColor(Color.BLACK);
                        }
                        blLamp1_ON = true;
                        updateLampValue(sPresetRGBValues);
                    }
                } else {
                    makeToast("No lamp preset assigned to this button!");
                }
                break;

            case R.id.btnL2:

                if (spLampDefinitions.contains(btnL2.getText().toString())) {
                    sPresetRGBValues = spLampDefinitions.getString(btnL2.getText().toString(), null);
                    sCommand = "S" + convertRGBwithCommasToHexString(sPresetRGBValues);
                    sCommand += "$" + sNewLine;
                    //if (btService.connected) {
                    if (mBoundBT) {
                        Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                        lclBTServiceInstance.sendData(sCommand);
                    } else {
                        Log.d(TAG, "Service btService not connected!");
                    }
                    lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    sCommand = "B," + btnL2.getTag().toString() + "1$" + sNewLine;
                    if (mBoundBT) {
                        Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                        lclBTServiceInstance.sendData(sCommand);
                    } else {
                        Log.d(TAG, "Service btService not connected!");
                    }
                    lclUsbServiceInstance.sendBytes(sCommand.getBytes());

                    if (btnL2.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                        if (BL_LOW_MODE) {
                            sCommand = "S11050" + sNewLine;
                        } else {
                            sCommand = "S11100" + sNewLine;
                        }
                        if (mBoundBT) {
                            Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                            lclBTServiceInstance.sendData(sCommand);

                        } else {
                            Log.d(TAG, "Service btService not connected!");
                        }
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        blTL84_ON = true;
                    } else {
                        sCommand = "S11000" + sNewLine;
                        blTL84_ON = false;
                        if (mBoundBT) {
                            lclBTServiceInstance.sendData(sCommand);
                        }
                    }
                    blLamp2_ON = true;
                    btnL2.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL2.setTextColor(Color.BLACK);
                    updateLampValue(sPresetRGBValues);
                } else {
                    makeToast("No lamp preset assign to this button!");
                }
                break;

            case R.id.btnL3:

                if (spLampDefinitions.contains(btnL3.getText().toString())) {
                    sPresetRGBValues = spLampDefinitions.getString(btnL3.getText().toString(), null);
                    sCommand = "S" + convertRGBwithCommasToHexString(sPresetRGBValues);
                    sCommand += "$" + sNewLine;
                    //if (btService.connected) {
                    if (mBoundBT) {
                        Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                        lclBTServiceInstance.sendData(sCommand);
                    } else {
                        Log.d(TAG, "Service btService not connected!");
                    }
                    lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    sCommand = "B," + btnL3.getTag().toString() + "1$" + sNewLine;
                    if (mBoundBT) {
                        Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                        lclBTServiceInstance.sendData(sCommand);
                    } else {
                        Log.d(TAG, "Service btService not connected!");
                    }
                    lclUsbServiceInstance.sendBytes(sCommand.getBytes());

                    if (btnL3.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                        if (BL_LOW_MODE) {
                            sCommand = "S11050" + sNewLine;
                        } else {
                            sCommand = "S11100" + sNewLine;
                        }
                        if (mBoundBT) {
                            Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                            lclBTServiceInstance.sendData(sCommand);

                        } else {
                            Log.d(TAG, "Service btService not connected!");
                        }
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        blTL84_ON = true;
                    } else {
                        sCommand = "S11000" + sNewLine;
                        blTL84_ON = false;
                        if (mBoundBT) {
                            lclBTServiceInstance.sendData(sCommand);
                        }
                    }
                    blLamp3_ON = true;
                    btnL3.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL3.setTextColor(Color.BLACK);
                    updateLampValue(sPresetRGBValues);
                } else {
                    makeToast("No lamp preset assign to this button!");
                }
                break;
            case R.id.btnL4:

                if (spLampDefinitions.contains(btnL4.getText().toString())) {
                    sPresetRGBValues = spLampDefinitions.getString(btnL4.getText().toString(), null);
                    sCommand = "S" + convertRGBwithCommasToHexString(sPresetRGBValues);
                    sCommand += "$" + sNewLine;
                    //if (btService.connected) {
                    if (mBoundBT) {
                        Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                        lclBTServiceInstance.sendData(sCommand);
                    } else {
                        Log.d(TAG, "Service btService not connected!");
                    }
                    lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    sCommand = "B," + btnL4.getTag().toString() + "1$" + sNewLine;
                    if (mBoundBT) {
                        Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                        lclBTServiceInstance.sendData(sCommand);
                    } else {
                        Log.d(TAG, "Service btService not connected!");
                    }
                    lclUsbServiceInstance.sendBytes(sCommand.getBytes());

                    if (btnL4.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                        if (BL_LOW_MODE) {
                            sCommand = "S11050" + sNewLine;
                        } else {
                            sCommand = "S11100" + sNewLine;
                        }
                        if (mBoundBT) {
                            Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                            lclBTServiceInstance.sendData(sCommand);

                        } else {
                            Log.d(TAG, "Service btService not connected!");
                        }
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        blTL84_ON = true;
                    } else {
                        sCommand = "S11000" + sNewLine;
                        blTL84_ON = false;
                        if (mBoundBT) {
                            lclBTServiceInstance.sendData(sCommand);
                        }
                    }
                    blLamp4_ON = true;
                    btnL4.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL4.setTextColor(Color.BLACK);
                    updateLampValue(sPresetRGBValues);
                } else {
                    makeToast("No lamp preset assign to this button!");
                }
                break;
            case R.id.btnL5:

                if (spLampDefinitions.contains(btnL5.getText().toString())) {
                    sPresetRGBValues = spLampDefinitions.getString(btnL5.getText().toString(), null);
                    sCommand = "S" + convertRGBwithCommasToHexString(sPresetRGBValues);
                    sCommand += "$" + sNewLine;
                    //if (btService.connected) {
                    if (mBoundBT) {
                        Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                        lclBTServiceInstance.sendData(sCommand);
                    } else {
                        Log.d(TAG, "Service btService not connected!");
                    }
                    lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    sCommand = "B," + btnL5.getTag().toString() + "1$" + sNewLine;
                    if (mBoundBT) {
                        Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                        lclBTServiceInstance.sendData(sCommand);
                    } else {
                        Log.d(TAG, "Service btService not connected!");
                    }
                    lclUsbServiceInstance.sendBytes(sCommand.getBytes());

                    if (btnL5.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                        if (BL_LOW_MODE) {
                            sCommand = "S11050" + sNewLine;
                        } else {
                            sCommand = "S11100" + sNewLine;
                        }
                        if (mBoundBT) {
                            Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                            lclBTServiceInstance.sendData(sCommand);

                        } else {
                            Log.d(TAG, "Service btService not connected!");
                        }
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        blTL84_ON = true;
                    } else {
                        sCommand = "S11000" + sNewLine;
                        blTL84_ON = false;
                        if (mBoundBT) {
                            //lclBTServiceInstance.sendData(sCommand);
                        }
                    }
                    blLamp5_ON = true;
                    btnL5.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL5.setTextColor(Color.BLACK);
                    updateLampValue(sPresetRGBValues);
                } else {
                    makeToast("No lamp preset assign to this button!");
                }
                break;
            case R.id.btnL6:

                if (spLampDefinitions.contains(btnL6.getText().toString())) {
                    sPresetRGBValues = spLampDefinitions.getString(btnL6.getText().toString(), null);
                    sCommand = "S" + convertRGBwithCommasToHexString(sPresetRGBValues);
                    sCommand += "$" + sNewLine;
                    //if (btService.connected) {
                    if (mBoundBT) {
                        Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                        lclBTServiceInstance.sendData(sCommand);
                    } else {
                        Log.d(TAG, "Service btService not connected!");
                    }
                    lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    sCommand = "B," + btnL6.getTag().toString() + "1$" + sNewLine;
                    if (mBoundBT) {
                        Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                        lclBTServiceInstance.sendData(sCommand);
                    } else {
                        Log.d(TAG, "Service btService not connected!");
                    }
                    lclUsbServiceInstance.sendBytes(sCommand.getBytes());

                    if (btnL6.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                        if (BL_LOW_MODE) {
                            sCommand = "S11050" + sNewLine;
                        } else {
                            sCommand = "S11100" + sNewLine;
                        }
                        if (mBoundBT) {
                            Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                            lclBTServiceInstance.sendData(sCommand);

                        } else {
                            Log.d(TAG, "Service btService not connected!");
                        }
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        blTL84_ON = true;
                    } else {
                        sCommand = "S11000" + sNewLine;
                        blTL84_ON = false;
                        if (mBoundBT) {
                            lclBTServiceInstance.sendData(sCommand);
                        }
                    }
                    blLamp6_ON = true;
                    btnL6.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL6.setTextColor(Color.BLACK);
                    updateLampValue(sPresetRGBValues);
                } else {
                    makeToast("No lamp preset assign to this button!");
                }
                break;

            case R.id.btnL7:
                blLamp7_ON = true;
                btnL7.setBackgroundResource(R.drawable.buttonselector_active);
                btnL7.setTextColor(Color.BLACK);

                break;
        }
    }

    private void send_via_bt(String command) {
        if (mBoundBT) {
            Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + command.replace("\n", "\\n").replace("\r", "\\r") + "'");
            lclBTServiceInstance.sendData(command);
        } else {
            Log.d(TAG, "Service btService not connected!");
        }
    }

    public void btnClicked2(View v) {
        SharedPreferences spLampDefinitions = getSharedPreferences(Constants.SHAREDPREFS_LAMP_DEFINITIONS, 0);
        String sPresetRGBValues;
        String sCommand;

        String buttonCaption = v.getTag().toString();
        if (!TextUtils.isEmpty(buttonCaption)) {
            if (!buttonCaption.equalsIgnoreCase("LOW") && !buttonCaption.equalsIgnoreCase("UV")) {
                Log.d(TAG, "switching off all lamps");
                mark_all_buttons_off_on_mobile();
            }
        }

        int buttonID = v.getId();
        Button button = findViewById(buttonID);

        /* scenarios:
        1. "clean" execution - switch on lamp
        2. "clean" execution - special lamp - UV
        3. "clean" execution - special lamp - TL84
        4. "clean" execution - special lamp - LOW
        5. a lamp is already on - special lamp - UV
        6. a lamp is already on - special lamp - TL84
        7. a lamp is already on - special lamp - LOW on
        8. a lamp is already on - special lamp - LOW off
         */


        if (spLampDefinitions.contains(button.getText().toString())) {
            sPresetRGBValues = spLampDefinitions.getString(button.getText().toString(), null);

            if (!power_drain_check(sPresetRGBValues)) {
                return;
            }
            Log.d(TAG, "sending " + sPresetRGBValues + " to controller");
            if (sPresetRGBValues != null) {

                if (button.getText().toString().equalsIgnoreCase("UV")) {
                    //complementary light - keep current on and add UV definition for non-zeros - when switching ON UV mode. Otherwise switch off additional lamps
                    sCommand = "P" + convertRGB2complementaryLight(sPresetRGBValues, (!BL_UV_MODE));
                    sCommand += "$" + sNewLine;
                    send_via_bt(sCommand);
                    lclUsbServiceInstance.sendBytes(sCommand.getBytes());

                    sCommand = "B," + button.getTag().toString() + (BL_UV_MODE ? 0 : 1) + "$" + sNewLine;
                    Log.d(TAG, "sendviabt ** B command with tag: "+ button.getTag().toString() + " text: " + button.getText());
                    send_via_bt(sCommand);
                    lclUsbServiceInstance.sendBytes(sCommand.getBytes());

                    BL_UV_MODE = !BL_UV_MODE;
                    String s = (BL_UV_MODE ? "active" : "inactive");
                    Log.d(TAG, "UV mode - " + s);
                    if (BL_UV_MODE) {
                        button.setBackgroundResource(R.drawable.buttonselector_active);
                        button.setTextColor(Color.BLACK);
                    } else {
                        button.setBackgroundResource(R.drawable.buttonselector_main);
                        button.setTextColor(Color.WHITE);
                    }


                } else {
                    if (!BL_LOW_MODE) {
                        Log.d(TAG, "I'm not in LOW mode");

                        if (button.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                            get_tl84_delay();
                            sCommand = "S11100" + convertRGBwithCommasToHexString(sPresetRGBValues) + get_tl84_delay() + "$" + sNewLine;
                            Log.d(TAG, " *** NEW TL84 command (ON): " + sCommand);
                            send_via_bt(sCommand);
                            lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                            blTL84_ON = true;
                        } else {
                            //sCommand = "S11000$" + sNewLine; //actually we want to send a S11000^ffffffff(...)
                            sCommand  = "S11000" + convertRGBwithCommasToHexString(sPresetRGBValues) + "$" + sNewLine;
                            blTL84_ON = false;
                            send_via_bt(sCommand);

                            BL_COMMAND_SENT = true;
                            if (!BL_COMMAND_SENT) {
                                sCommand = "S" + convertRGBwithCommasToHexString(sPresetRGBValues);
                                sCommand += "$" + sNewLine;
                                Log.d(TAG, " *** NEW TL84 command (OFF): " + sCommand);
                                send_via_bt(sCommand);
                                lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                            }
                            sCommand = "B," + button.getTag().toString() + "1$" + sNewLine;
                            send_via_bt(sCommand);
                            lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        }
                    } else {
                        //LOW MODE!
                        /*SharedPreferences prefsLamps = getSharedPreferences(SHAREDPREFS_CURRENT_LAMPS, 0);
                        Map<String, ?> keys = prefsLamps.getAll(); */
                        Log.d (TAG, "LOW mode! Changing " + sPresetRGBValues + " to LOW values.");


                        if (button.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                            sCommand = "S11050" + convertRGBwithCommasToHexString(sPresetRGBValues) + get_tl84_delay() + "$" + sNewLine;
                            Log.d(TAG, " *** NEW TL84 command (LOW): " + sCommand);
                            send_via_bt(sCommand);
                            lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                            blTL84_ON = true;
                        } else {
                            sCommand = "S11000$" + sNewLine;
                            blTL84_ON = false;
                            Log.d(TAG, " *** NEW TL84 command (OFF): " + sCommand);
                            send_via_bt(sCommand);
                            String[] sRGB_in = sPresetRGBValues.split(",");
                            String[] sRGB_out = sRGB_in;
                            for (int i = 0; i < sRGB_in.length; i++) {
                                int iRGB = Integer.valueOf(sRGB_in[i]);
                                iRGB /= (BL_LOW_MODE ? 2 : 1);
                                sRGB_out[i] = String.valueOf(iRGB);
                            }
                            String concatValues = TextUtils.join(",", sRGB_out);
                            String sHex = convertRGBwithCommasToHexString(concatValues);
                            sCommand = "S" + sHex + "$" + sNewLine;
                            send_via_bt(sCommand);
                            lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                            Log.d(TAG, "I'm in LOW mode");
                            Log.d(TAG, "Illuminating lamps with: " + sCommand);
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
                }
                updateLampValue(sPresetRGBValues);
            }
        } else {
            makeToast("No lamp preset assigned to this button!");
        }
    }

    public void btnClicked_UV_normal(View v) {
        SharedPreferences spLampDefinitions = getSharedPreferences(Constants.SHAREDPREFS_LAMP_DEFINITIONS, 0);
        String sPresetRGBValues;
        String sCommand;

        String buttonCaption = v.getTag().toString();
        if (!TextUtils.isEmpty(buttonCaption)) {
            if (!buttonCaption.equalsIgnoreCase("LOW")) {
                Log.d(TAG, "switching off all lamps");
                mark_all_buttons_off_on_mobile();
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


        if (spLampDefinitions.contains(button.getText().toString())) {
            sPresetRGBValues = spLampDefinitions.getString(button.getText().toString(), null);

            if (!power_drain_check(sPresetRGBValues)) {
                return;
            }
            Log.d(TAG, "sending " + sPresetRGBValues + " to controller");
            if (sPresetRGBValues != null) {
                if (!BL_LOW_MODE) {
                    Log.d(TAG, "I'm not in LOW mode");

                    if (button.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                        get_tl84_delay();
                        sCommand = "S11100" + convertRGBwithCommasToHexString(sPresetRGBValues) + get_tl84_delay() + "$" + sNewLine;
                        Log.d(TAG, " *** NEW TL84 command (ON): " + sCommand);
                        send_via_bt(sCommand);
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        blTL84_ON = true;
                    } else {
                        //sCommand = "S11000$" + sNewLine; //actually we want to send a S11000^ffffffff(...)
                        sCommand  = "S11000" + convertRGBwithCommasToHexString(sPresetRGBValues) + "$" + sNewLine;
                        blTL84_ON = false;
                        send_via_bt(sCommand);

                        BL_COMMAND_SENT = true;
                        if (!BL_COMMAND_SENT) {
                            sCommand = "S" + convertRGBwithCommasToHexString(sPresetRGBValues);
                            sCommand += "$" + sNewLine;
                            Log.d(TAG, " *** NEW TL84 command (OFF): " + sCommand);
                            send_via_bt(sCommand);
                            lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        }
                        sCommand = "B," + button.getTag().toString() + "1$" + sNewLine;
                        send_via_bt(sCommand);
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    }
                } else {
                    //LOW MODE!
                    /*SharedPreferences prefsLamps = getSharedPreferences(SHAREDPREFS_CURRENT_LAMPS, 0);
                    Map<String, ?> keys = prefsLamps.getAll(); */
                    Log.d (TAG, "LOW mode! Changing " + sPresetRGBValues + " to LOW values.");


                    if (button.getText().toString().equalsIgnoreCase(TL84_TAG)) {
                        sCommand = "S11050" + convertRGBwithCommasToHexString(sPresetRGBValues) + get_tl84_delay() + "$" + sNewLine;
                        Log.d(TAG, " *** NEW TL84 command (LOW): " + sCommand);
                        send_via_bt(sCommand);
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        blTL84_ON = true;
                    } else {
                        //sCommand = "S11000$" + sNewLine;
                        sCommand  = "S11000" + convertRGBwithCommasToHexString(sPresetRGBValues) + "$" + sNewLine;
                        blTL84_ON = false;
                        Log.d(TAG, " *** NEW TL84 command (OFF): " + sCommand);
                        send_via_bt(sCommand);
                        String[] sRGB_in = sPresetRGBValues.split(",");
                        String[] sRGB_out = sRGB_in;
                        for (int i = 0; i < sRGB_in.length; i++) {
                            int iRGB = Integer.valueOf(sRGB_in[i]);
                            iRGB /= (BL_LOW_MODE ? 2 : 1);
                            sRGB_out[i] = String.valueOf(iRGB);
                        }
                        String concatValues = TextUtils.join(",", sRGB_out);
                        String sHex = convertRGBwithCommasToHexString(concatValues);
                        sCommand = "S" + sHex + "$" + sNewLine;
                        send_via_bt(sCommand);
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        Log.d(TAG, "I'm in LOW mode");
                        Log.d(TAG, "Illuminating lamps with: " + sCommand);
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
            }
            updateLampValue(sPresetRGBValues);

        } else {
            makeToast("No lamp preset assigned to this button!");
        }
    }

    private void TL84_OFF() {
        //String sCommand = "S11000$" + sNewLine;
        String sCommand  = "S11000" + "00000000000000000000" + "$" + sNewLine;

        blTL84_ON = false;
        send_via_bt(sCommand);
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
        Integer light_power = check_light_power(convertRGBwithCommasToHexString(sPresetRGBValues));
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

    public void mark_all_buttons_off_on_mobile() {
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

        /*btnLOW.setBackgroundResource(R.drawable.buttonselector_low);
        btnLOW.setTextColor(Color.WHITE);*/

        btnL9.setBackgroundResource(R.drawable.buttonselector_main);
        btnL9.setTextColor(Color.WHITE);
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

        sCommand= "B,OFF1$" + sNewLine;
        send_via_bt(sCommand);
        lclUsbServiceInstance.sendBytes(sCommand.getBytes());

        //blLamp9_ON = true;
        if (!BL_LOW_MODE) {
            btnL9.setBackgroundResource(R.drawable.buttonselector_active);
            btnL9.setTextColor(Color.BLACK);

            btnLOW.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
            btnLOW.setTextColor(Color.WHITE);

        }

        blTL84_ON = false;
        TL84_OFF();
        sCommand = "S00000000000000000000$" + sNewLine;
        send_via_bt(sCommand);

        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
        BL_UV_MODE = false;


        updateLampValue("000,000,000,000,000,000,000,000,000,000");
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

        menu.add(Menu.NONE, 0, 0, "Light Settings").setIcon(
                getResources().getDrawable(R.drawable.icon_scan));

        /*menu.add(Menu.NONE, 1, 1, "Home / Light sources").setIcon(
                getResources().getDrawable(R.drawable.icon_information));*/
        menu.add(Menu.NONE, 2, 2, "Operating Hours").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 3, 3, "Sequence Settings (PRG)").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 4, 4, "Settings").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 5, 5, "Manual").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 6, 6, "Maintenance page").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        /*menu.add(Menu.NONE, 7, 7, "ListView page").setIcon(
                getResources().getDrawable(R.drawable.icon_information));*/


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
            case 0:
                Intent intent0 = new Intent(TRSDigitalPanel.this, LightSettings.class);
                startActivity(intent0);
                break;

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

            /*case 7:
                Intent intent9 = new Intent(TRSDigitalPanel.this, TRSSequence.class);
                startActivity(intent9);
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

                            //getJSONFile();
                            //String sCommand = "J" + sNewLine;

                            /*int iResult = usbService.sendBytes(sCommand.getBytes());
                            if (iResult < 0) {
                                Toast.makeText(context, "Stream was null, no request sent", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "I think I've sent " + iResult + " bytes.", Toast.LENGTH_SHORT).show();
                            }*/

                            Intent intent = new Intent(TRSDigitalPanel.this, TRSMaintenancePage.class);
                            startActivity(intent);
                            // Intent sequencerIntent = new Intent(this, BtSequencerActivity.class);
                            // startActivity(sequencerIntent);
                        } else {
                            Message msg = new Message();
                            msg.what = MSG_SHOW_TOAST;
                            msg.obj = "Password incorrect";
                            messageHandler.sendMessage(msg);
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

    public void openPRG (final View v) {
        Intent intent = new Intent(TRSDigitalPanel.this, TRSSequence_old.class);
        startActivity(intent);
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
                    Log.d(TAG, "Executing preset #" + sPresetName + "# for " + iTimer + " seconds via reverseTimer()");
                    S_CURRENT_SEQ_ITEM = sPresetName;
                    reverseTimer(iTimer, sPresetName);
                    Log.d(TAG, "Removing preset with key="+key+".");
                    blSeqGreenLight = false;
                }
            }
        //} else {
            //Log.d(TAG, "Sequence execution finished");
       // }
    }

    public void executePRG (final View v) {

        Long lTime;
        Long lNextStopTime;
        String sSequence = "D";

        duplicateSPFile();
        makeToast("Executing sequence ... ");
        Log.d(TAG, "Executing sequence, kicking off function processSequenceFile()");

        //processSequenceFile();
        SharedPreferences spLampTimers = getSharedPreferences(SP_LAMP_TIMERS, 0);
        TreeMap<String, ?> allEntries = new TreeMap<String, Object>(spLampTimers.getAll());
        int i = 1;
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String sVal = entry.getValue().toString();
            String[] sBuffer = sVal.split(",");
            sSequence = sSequence.concat("0" + i);
            sSequence = sSequence.concat(sBuffer[1]);
            Log.d(TAG, "Currently sSequence: " + entry.getKey() + ": " + sSequence);
            i++;
        }

        sSequence = sSequence.concat("^").concat((check_sequence_for_loop()) ? "1" : "0");
        sSequence = sSequence.concat(System.lineSeparator());
        lclBTServiceInstance.sendData(sSequence);
        lclUsbServiceInstance.sendBytes(sSequence.getBytes());

        Log.d(TAG, "Sending Bytes: " + sSequence);
        //makeToast(sSequence);

        SystemClock.sleep(200);
        String sExecute = "E" + System.lineSeparator();
        lclBTServiceInstance.sendData(sExecute);
        lclUsbServiceInstance.sendBytes(sExecute.getBytes());
        Log.d(TAG, "Sending Bytes: " + sExecute);
        //switch_all_off();
    }

    boolean check_sequence_for_loop() {
        boolean flag = false;
        SharedPreferences sp = getSharedPreferences(CONFIG_SETTINGS, 0);
        flag = sp.getBoolean("LOOP_SEQUENCE", false);

        return flag;
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
                Log.d(TAG, "TICK: " + seconds + " left for preset #" + sPresetName +"#");
                switch_preset_on(sPresetName);
                blSeqGreenLight = false;
            }

            public void onFinish() {
                trim_sequence_file(sPresetName);
                boolean blNextStep = (getPrefsFileSize(SP_SEQUENCE_COMMAND_EXECUTED) > 0);
                Log.d(TAG, "reverseTimer finished. Preset #" + sPresetName +"# removed from EXECUTE file. More presets left to execute: " + getPrefsFileSize(SP_SEQUENCE_COMMAND_EXECUTED));
                //makeToast("Executing trim sequence file. S_NEXT_SEQ_ITEM=" + S_NEXT_SEQ_ITEM);
                blSeqGreenLight = true;
                Log.d(TAG, "blSeqGreenLight = " + blSeqGreenLight);

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

    public void updateLampValue (String sCommand) {
        SharedPreferences prefsCurrentState = getSharedPreferences(Constants.SHAREDPREFS_CURRENT_LAMPS, 0);
        SharedPreferences.Editor editor = prefsCurrentState.edit();
        String[] RGBValues = sCommand.split(",");
        Map<String, String> map = new HashMap<>();
        int i = 1;
        for (String RGB : RGBValues) {
            String sKey = "S" + String.format("%02d", i);
            map.put(sKey, RGB);
            i++;
        }
        TreeMap<String, String> sorted = new TreeMap<>();
        // Copy all data from hashMap into TreeMap
        sorted.putAll(map);

        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            editor.putString(entry.getKey(), entry.getValue());
            //Log.d(TAG, "sorted: " + entry.getKey() + ", " + entry.getValue());
        }
        editor.apply();
    }

    public void btnLOW(View v) {
        SharedPreferences spLampDefinitions = getSharedPreferences(Constants.SHAREDPREFS_LAMP_DEFINITIONS, 0);
        if (blTL84_ON) {
            String sPresetRGBValues = spLampDefinitions.getString(Constants.TL84_TAG, null);
            if (!BL_LOW_MODE) {

                String sCommand = "S11050" + convertRGBwithCommasToHexString(sPresetRGBValues) + get_tl84_delay() + "$" + sNewLine;
                if (mBoundBT) {
                    Log.d(TAG, "LOW function - Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                    lclBTServiceInstance.sendData(sCommand);
                } else {
                    Log.d(TAG, "Service btService not connected!");
                }
                btnLOW.setBackgroundResource(R.drawable.buttonselector_active);
                btnLOW.setTextColor(Color.BLACK);
                lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                blTL84_ON = true;
            } else {
                String sCommand = "S11100" + convertRGBwithCommasToHexString(sPresetRGBValues) + get_tl84_delay() + "$" + sNewLine;
                if (mBoundBT) {
                    Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                    lclBTServiceInstance.sendData(sCommand);
                } else {
                    Log.d(TAG, "Service btService not connected!");
                }
                btnLOW.setBackgroundResource(R.drawable.buttonselector_low);
                btnLOW.setTextColor(Color.WHITE);
                lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                blTL84_ON = true;
            }
        }

        if (BL_LOW_MODE) {
            btnLOW.setBackgroundResource(R.drawable.buttonselector_low);
            btnLOW.setTextColor(Color.WHITE);
        } else {
            btnLOW.setBackgroundResource(R.drawable.buttonselector_active);
            btnLOW.setTextColor(Color.BLACK);
        }


        SharedPreferences prefsLamps = getSharedPreferences(Constants.SHAREDPREFS_CURRENT_LAMPS, 0);
        Map<String, ?> keys = prefsLamps.getAll();
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            int iRGB = Integer.valueOf(entry.getValue().toString());
            iRGB /= (BL_LOW_MODE ? 1 : 2);
            sorted.put(entry.getKey(), String.valueOf(iRGB));
        }
        ArrayList<String> concatValues = new ArrayList<>();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            concatValues.add(entry.getValue());
        }

        String sHex = convertRGBwithCommasToHexString(TextUtils.join(",", concatValues));
        String sCommand = "S" + sHex + "$" + sNewLine;
        Log.d("MORRIS-TRSDIGITAL", "btnLOW: sending command:" + sCommand);
        lclBTServiceInstance.sendData(sCommand);
        lclUsbServiceInstance.sendBytes(sCommand.getBytes());

        sCommand= "B,LOW" + (BL_LOW_MODE ? 0 : 1) + "$" + sNewLine;
        lclBTServiceInstance.sendData(sCommand);
        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
        BL_LOW_MODE = !BL_LOW_MODE;
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
