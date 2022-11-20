package com.kiand.LED2match;

import android.app.Activity;
import android.app.ActivityManager;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import static com.kiand.LED2match.Constants.BT_CONNECTED_PREFS;
import static com.kiand.LED2match.Constants.CONFIG_SETTINGS;
import static com.kiand.LED2match.Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE;

public class TRSSettings extends Activity {

    public static final String SHAREDPREFS_ONE_OFF_SEEKBARS = "one-off-seekbar-values.txt"; //Mauricio
    public static final String TIME_OFF_STORAGE = "shutdown_timer"; //Mauricio
    public static final String CONFIG_SETTINGS = "config_settings";
    public static final String newLine = System.getProperty("line.separator");

    public static final String TL84_DELAY_KEY = "TL84_delay";
    public static final int TL84_DELAY_DEFAULT = 600;

    private static final String password = "hokus";
    private static final int MSG_SHOW_TOAST = 1;
    private static final int TOAST_MESSAGE = 1;
    private static final String NO_PRESET_TEXT = "#n/a";
    private FileUtilities fileUtilities;


    Button btnSave;
    Switch aSwitch;

    public String TAG = "MORRIS-SETTINGS";
    private BtCOMMsService lclBTServiceInstance;
    private UsbCOMMsService lclUsbServiceInstance;
    boolean mBound = false;
    boolean mBoundBT = false;
    final Context context = this;
    public final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private boolean bl_bluetooth_forced_on;
    private CountDownTimer shutdownTimer;
    private final int iHours_idle_shutoff = 0;
    private int iMinutes_idle_shutoff = 0;
    Button btnL1, btnL2, btnL3, btnL4, btnL5, btnL6;

    EditText editOff_m;
    EditText edit_TL84_delay;
    EditText edit_emergency_delay;

    Float versionName = BuildConfig.VERSION_CODE / 1000.0f;
    public String apkVersion = "v." + versionName;



    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();

            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (bluetoothState) {
                    case BluetoothAdapter.STATE_ON:
                        if (bl_bluetooth_forced_on) {
                            Log.d(TAG, " *** Calling start BT Service");
                            startBluetoothService();
                        }
                        break;
                }
            }

        }
    };
    private final BroadcastReceiver btReceiverBTdevice = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action){
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.d(TAG, "Broadcast receiver: ACL_CONNECTED");
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.d(TAG, "Broadcast receiver: ACL_DISCONNECTED");
                    break;
            }
            Bundle bundle = intent.getExtras();

        }
    };

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            UsbCOMMsService.UsbBinder binder = (UsbCOMMsService.UsbBinder) service;
            lclUsbServiceInstance = binder.getService();
            mBound = true;
            Log.d(TAG, "mBound = " + mBound + ", mBoundBT = " + mBoundBT);
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
            Log.d(TAG, "BT Comms service connection established, btService communication interface established.");
            mBoundBT = true;
            Log.d(TAG, "mBound = " + mBound + ", mBoundBT = " + mBoundBT);
            trigger_temp_reading();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            lclBTServiceInstance = null;
            mBoundBT = false;
            Log.d(TAG, "BT Comms service connection not available, probably remote service has been closed.");
        }
    };

    protected void onResume()
    {
        super.onResume();
        populate_unit_config();
        populate_multiplier_values();
        Log.d(TAG, "mBound = " + mBound + ", mBoundBT = " + mBoundBT);
        Log.d(TAG, "Executing license check");
        boolean connected = get_connection_status();
        if (connected) {
            license_check();
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

    public boolean get_connection_status() {
        SharedPreferences prefs_config = getSharedPreferences(Constants.BT_CONNECTED_PREFS, 0);
        boolean status= prefs_config.getBoolean("CONNECTED", false);

        return status;
    }

    private void license_check() {
        int current_tier = get_tier();

        if (current_tier < Constants.LICENSE_TIER_SETTINGS_PAGE) {
            Log.d(TAG, "Blocking page");
            block_current_page();
        } else {
            Log.d(TAG, "Not blocking page");
            unblock_slave_controls();
        }
    }

    private void block_current_page() {
        Intent intent = new Intent(TRSSettings.this, DisabledOverlayPage.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK) ;
        startActivity(intent);

    }
    public void check_for_edits(View v) {
        if (!v.isFocusableInTouchMode()) {
            makeToast("To edit value please make sure you are connected to the controller via BT and have appropriate license tier");
        }


    }

    private void unblock_slave_controls() {
        EditText et;
        et = findViewById(R.id.edit_group1_LED_full);
        et.setFocusableInTouchMode(true);
        et = findViewById(R.id.edit_group1_LED_dim);
        et.setFocusableInTouchMode(true);
        et = findViewById(R.id.edit_group1_TL84_full);
        et.setFocusableInTouchMode(true);
        et = findViewById(R.id.edit_group1_TL84_dim);
        et.setFocusableInTouchMode(true);

        et = findViewById(R.id.edit_group2_LED_full);
        et.setFocusableInTouchMode(true);
        et = findViewById(R.id.edit_group2_LED_dim);
        et.setFocusableInTouchMode(true);
        et = findViewById(R.id.edit_group2_TL84_full);
        et.setFocusableInTouchMode(true);
        et = findViewById(R.id.edit_group2_TL84_dim);
        et.setFocusableInTouchMode(true);

        et = findViewById(R.id.edit_group3_LED_full);
        et.setFocusableInTouchMode(true);
        et = findViewById(R.id.edit_group3_LED_dim);
        et.setFocusableInTouchMode(true);
        et = findViewById(R.id.edit_group3_TL84_full);
        et.setFocusableInTouchMode(true);
        et = findViewById(R.id.edit_group3_TL84_dim);
        et.setFocusableInTouchMode(true);


    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
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
                message += " \u2103";
                textTemperature.setText(message);
            }
        }
    };

    private void trigger_temp_reading() {
        String sCommand = "X303" + Constants.sNewLine;
        if (mBound) {
            lclUsbServiceInstance.sendBytes(sCommand.getBytes());
        }

        if (mBoundBT) {
            SystemClock.sleep(100);
            try {
                Log.d(TAG, "Requesting temp reading from controller");
                lclBTServiceInstance.sendData(sCommand);
            } catch (NullPointerException e) {
                Log.d(TAG, "NullPointerException when sending command via Bluetooth");
            }
        }
    }

    private void populate_unit_config() {
        Log.d(TAG, "Populating page with unit config values");
        String ee_autoshutoff_tag = "eeprom_auto_shutoff";
        String ee_tl84delay_tag = "eeprom_tl84_delay";
        String ee_psucurrent_tag = "eeprom_PSU_current";
        String ee_tl84dim_tag = "eeprom_tl84_dim_value";
        String ee_tl84masterdim_tag = "eeprom_tl84_master_dim_value";
        String ee_emergency_tag = "eeprom_emergency_light_delay";

        SharedPreferences spConfig = getSharedPreferences(CONFIG_SETTINGS, 0);
        SharedPreferences.Editor spConfigEditor = spConfig.edit();
        String s_eeprom_auto_shutoff  = spConfig.getString(ee_autoshutoff_tag, "");
        String s_eeprom_tl84_delay = spConfig.getString(ee_tl84delay_tag, "");
        String s_eeprom_PSU_current = spConfig.getString(ee_psucurrent_tag, "");
        String s_eeprom_tl84_dim_value = spConfig.getString(ee_tl84dim_tag, "");
        String s_eeprom_tl84_master_dim_value = spConfig.getString(ee_tl84masterdim_tag,"");
        String s_eeprom_emergency_light_delay = spConfig.getString(ee_emergency_tag,"");

        Log.d(TAG, "emergency light delay: " + s_eeprom_emergency_light_delay);

        try {
            if (s_eeprom_tl84_delay.length() > 0) {
                edit_TL84_delay.setText(s_eeprom_tl84_delay);
            }
        } catch (NullPointerException e) {
            makeToast("TL84 delay not yet defined for this unit");
        }

        try {
            if (s_eeprom_auto_shutoff.length() > 0) {
                int iMinutes = Integer.parseInt(s_eeprom_auto_shutoff) / 60;
                editOff_m.setText(String.valueOf(iMinutes));
            }
        } catch (NullPointerException e) {
            makeToast("TL84 dim value not yet defined for this unit");
        }

        try {
            if (s_eeprom_emergency_light_delay.length() > 0) {
                int iMinutes = Integer.parseInt(s_eeprom_emergency_light_delay);
                edit_emergency_delay.setText(String.valueOf(iMinutes));
            }
        } catch (NullPointerException e) {
            makeToast("Emergency light delay value not yet defined for this unit");
        }
    }

    private void populate_multiplier_values() {
        Log.d(TAG, "Populating page with multipliers");
        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json = new JSON_analyst(spFile);

        String ee_autoshutoff_tag = "eeprom_auto_shutoff";
        String ee_tl84delay_tag = "eeprom_tl84_delay";
        String ee_psucurrent_tag = "eeprom_PSU_current";
        String ee_tl84dim_tag = "eeprom_tl84_dim_value";
        String ee_tl84masterdim_tag = "eeprom_tl84_master_dim_value";
        EditText ed;

        //Group 1
        try {
            String s_value = json.getJSONValue("slave_group1_led_full");
            if (s_value.length() > 0) {
                ed = findViewById(R.id.edit_group1_LED_full);
                int value = s_value.charAt(0);
                ed.setText(String.valueOf(value));
            }
        } catch (NullPointerException e) {
            //makeToast("TL84 delay not yet defined for this unit");
        }

        try {
            String s_value = json.getJSONValue("slave_group1_led_dim");
            if (s_value.length() > 0) {
                ed = findViewById(R.id.edit_group1_LED_dim);
                int value = s_value.charAt(0);
                ed.setText(String.valueOf(value));
            }
        } catch (NullPointerException e) {
            //makeToast("TL84 delay not yet defined for this unit");
        }

        try {
            String s_value = json.getJSONValue("slave_group1_tl84_full");
            if (s_value.length() > 0) {
                ed = findViewById(R.id.edit_group1_TL84_full);
                int value = s_value.charAt(0);
                ed.setText(String.valueOf(value));
            }
        } catch (NullPointerException e) {
            //makeToast("TL84 delay not yet defined for this unit");
        }

        try {
            String s_value = json.getJSONValue("slave_group1_tl84_dim");
            if (s_value.length() > 0) {
                ed = findViewById(R.id.edit_group1_TL84_dim);
                int value = s_value.charAt(0);
                ed.setText(String.valueOf(value));
            }
        } catch (NullPointerException e) {
            //makeToast("TL84 delay not yet defined for this unit");
        }

        //Group 2
        try {
            String s_value = json.getJSONValue("slave_group2_led_full");
            if (s_value.length() > 0) {
                ed = findViewById(R.id.edit_group2_LED_full);
                int value = s_value.charAt(0);
                ed.setText(String.valueOf(value));
            }
        } catch (NullPointerException e) {
            //makeToast("TL84 delay not yet defined for this unit");
        }

        try {
            String s_value = json.getJSONValue("slave_group2_led_dim");
            if (s_value.length() > 0) {
                ed = findViewById(R.id.edit_group2_LED_dim);
                int value = s_value.charAt(0);
                ed.setText(String.valueOf(value));
            }
        } catch (NullPointerException e) {
            //makeToast("TL84 delay not yet defined for this unit");
        }

        try {
            String s_value = json.getJSONValue("slave_group2_tl84_full");
            if (s_value.length() > 0) {
                ed = findViewById(R.id.edit_group2_TL84_full);
                int value = s_value.charAt(0);
                ed.setText(String.valueOf(value));
            }
        } catch (NullPointerException e) {
            //makeToast("TL84 delay not yet defined for this unit");
        }

        try {
            String s_value = json.getJSONValue("slave_group2_tl84_dim");
            if (s_value.length() > 0) {
                ed = findViewById(R.id.edit_group2_TL84_dim);
                int value = s_value.charAt(0);
                ed.setText(String.valueOf(value));
            }
        } catch (NullPointerException e) {
            //makeToast("TL84 delay not yet defined for this unit");
        }

        //Group 3
        try {
            String s_value = json.getJSONValue("slave_group3_led_full");
            if (s_value.length() > 0) {
                ed = findViewById(R.id.edit_group3_LED_full);
                int value = s_value.charAt(0);
                ed.setText(String.valueOf(value));
            }
        } catch (NullPointerException e) {
            //makeToast("TL84 delay not yet defined for this unit");
        }

        try {
            String s_value = json.getJSONValue("slave_group3_led_dim");
            if (s_value.length() > 0) {
                ed = findViewById(R.id.edit_group3_LED_dim);
                int value = s_value.charAt(0);
                ed.setText(String.valueOf(value));
            }
        } catch (NullPointerException e) {
            //makeToast("TL84 delay not yet defined for this unit");
        }

        try {
            String s_value = json.getJSONValue("slave_group3_tl84_full");
            if (s_value.length() > 0) {
                ed = findViewById(R.id.edit_group3_TL84_full);
                int value = s_value.charAt(0);
                ed.setText(String.valueOf(value));
            }
        } catch (NullPointerException e) {
            //makeToast("TL84 delay not yet defined for this unit");
        }

        try {
            String s_value = json.getJSONValue("slave_group3_tl84_dim");
            if (s_value.length() > 0) {
                ed = findViewById(R.id.edit_group3_TL84_dim);
                int value = s_value.charAt(0);
                ed.setText(String.valueOf(value));
            }
        } catch (NullPointerException e) {
            //makeToast("TL84 delay not yet defined for this unit");
        }
    }


    int check_for_TL84_delay() {
        SharedPreferences prefs = getSharedPreferences(CONFIG_SETTINGS, 0);
        return prefs.getInt(TL84_DELAY_KEY, 0);
    }


    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction("temperature_reading_event");
        filter.addAction("controller_data_refreshed_event");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);

        Intent intentUSB = new Intent(this, UsbCOMMsService.class);
        bindService (intentUSB, mConnection, Context.BIND_AUTO_CREATE);

        Log.d(TAG, "Creating IntentBT and binding it with BtCOMMsService");
        Intent intentBT = new Intent(this, BtCOMMsService.class);
        bindService(intentBT, btConnection, Context.BIND_AUTO_CREATE);


        //Toast.makeText(this.getBaseContext(),"Service bound (onStart)", Toast.LENGTH_SHORT).show();
        /*String sCommand = "J" + LightAdjustments.sNewLine; */
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
        unbindService(btConnection);
        mBound = false;
        mBoundBT = false;
        Log.d(TAG, "onSTOP - unbinding services");
        //Toast.makeText(this.getBaseContext(),"Activity stopped", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"Service unbound", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isMyServiceRunning(BtCOMMsService.class)) {
            Log.d(TAG, "BtCOMMS srvc is running. Unregistering and unbinding from btService");
            try {
                unregisterReceiver(btReceiver);
                unregisterReceiver(btReceiverBTdevice);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "IllegalArgumentException");
                e.printStackTrace();
            }

            if (mBoundBT) {
                try {
                    unbindService(btConnection);
                } catch (IllegalArgumentException e) {
                    Log.d(TAG, "Illegal Argument Exception");
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trs_settings);

        editOff_m = findViewById(R.id.editAutoShutOFF_m);
        edit_TL84_delay = findViewById(R.id.edit_TL84_delay);
        edit_emergency_delay = findViewById(R.id.editEmergency_m);
        btnL1 = findViewById(R.id.btnL1);
        btnL2 = findViewById(R.id.btnL2);
        btnL3 = findViewById(R.id.btnL3);
        btnL4 = findViewById(R.id.btnL4);
        btnL5 = findViewById(R.id.btnL5);
        btnL6 = findViewById(R.id.btnL6);

        btnSave = findViewById(R.id.btnSave);
        btnSave.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnSave.setTextColor(Color.WHITE);
        fileUtilities = new FileUtilities(get_path_to_customer_datafile(), get_path_to_customer_logofile());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        //menu.add(Menu.NONE, 0, 0, "Light Settings").setIcon(getResources().getDrawable(R.drawable.icon_scan));

        //menu.add(Menu.NONE, 2, 2, "Operating Hours").setIcon(
                //getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 3, 3, "Sequence Settings (PRG)").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        /*menu.add(Menu.NONE, 4, 4, "Settings").setIcon(
                getResources().getDrawable(R.drawable.icon_information));*/
        menu.add(Menu.NONE, 5, 5, "Manual").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 6, 6, "Maintenance page").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 7, 7, "Digital Panel").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 8, 8, "About").setIcon(
                getResources().getDrawable(R.drawable.icon_information));

        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem item = menu.findItem(R.id.menu_color_picker);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case 0:
                Intent intent0 = new Intent(TRSSettings.this, LightSettings.class);
                startActivity(intent0);
                break;

            case 2:
                Intent intent5 = new Intent(TRSSettings.this, TRSLightOperatingHours.class);
                startActivity(intent5);
                break;

            case 3:
                Intent intent6 = new Intent(TRSSettings.this, TRSSequence.class);
                startActivity(intent6);
                break;

            case 4:
                Intent intent7 = new Intent(TRSSettings.this, TRSSettings.class);
                startActivity(intent7);
                break;

            case 5:
                Intent intent8 = new Intent(TRSSettings.this, TRSManualPage.class);
                startActivity(intent8);
                break;

            case 6:
                //Recertification page
                goto_maintenance(null);
                //startActivity(intent9);
                break;

            case 7:
                Intent intent9 = new Intent(TRSSettings.this, TRSDigitalPanel.class);
                intent9.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent9);
                break;

            case 8:
                openAboutDialog();
                break;

        }
        return true;
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

    public boolean display_custom_data() {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, Context.MODE_PRIVATE);
        Boolean bl_custom_data = prefs.getBoolean(Constants.CUSTOMER_DATA_FLAG, false);
        Log.d(TAG, "display_custom_data_(): USE CUSTOMER DATA = " + bl_custom_data);
        return bl_custom_data;
    }

    private String getFWver_JSON() {
        String sReturn = "";
        sReturn = extractJSONvalue("", "firmware_version");

        return sReturn;
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

    public String getJsonBody(String sSharedPrefsFilename) {
        SharedPreferences spsValues = getSharedPreferences(sSharedPrefsFilename, 0);
        return spsValues.getString("JSON", "");

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

    public void onClickReassign(View v) {

        String sTags = "";
        if (!btnL1.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL1.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL2.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL2.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL3.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL3.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL4.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL4.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL5.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL5.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL6.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL6.getText().toString() + ","; } else { sTags = sTags + ","; }
        //if (!btnL10.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL10.getText().toString() + ","; } else { sTags = sTags + ","; }
        //if (!btnL11.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL11.getText().toString() + ","; } else { sTags = sTags + ","; }
        //if (!btnL12.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL12.getText().toString() + ","; } else { sTags = sTags + ","; }


        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        String sPresetCounter = json_analyst.getJSONValue("preset_counter");

        int iCtr = ((!sPresetCounter.trim().equals("") ? Integer.valueOf(sPresetCounter) : 0));

        Intent intentLampAssignment = new Intent(TRSSettings.this, ReassignLamps.class);
        intentLampAssignment.putExtra("tags", sTags);
        intentLampAssignment.putExtra("counter", iCtr);
        startActivity(intentLampAssignment);
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

                            Intent intent = new Intent(TRSSettings.this, TRSMaintenancePage.class);
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

    private final Handler messageHandler = new Handler(Looper.getMainLooper()) {
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

    private boolean check_for_BT_connection() {
        SharedPreferences prefs = getSharedPreferences(BT_CONNECTED_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean("CONNECTED", false);
    }

    private void startBluetoothService() {
        /*setFiltersBT();
        setFiltersBTdevice();*/
        //Log.d(TAG, "BtCOMMS srvc is not running. Setting filters and starting ");
        Intent intent = new Intent(this, BtCOMMsService.class);
        //this.startService(new Intent(this, BtCOMMsService.class));
        this.startService(intent);
        //bindService(intent, btConnection, Context.BIND_AUTO_CREATE);
        aSwitch.setChecked(true);
        //startService(BtCOMMsService.class, btConnection, null); // Start BtCOMMsService(if it was not started before) and Bind it
    }

    /*private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
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
    }*/

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

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    boolean validate_tl84_delay(Integer value) {
        return value >= 100 && value <= 1200;
    }
    public void onClickBack (View v) {
        finish();
    }

    public void saveSettings(View v) {
        boolean bl_edit_tl84delay = false;
        Log.d(TAG, "Entering function saveSettings.");

        String settings_value = "";
        String new_settings_value = "";
        String arduino_command_prefix = "X301,";
        String var_name;
        String var_value;

        if (editOff_m.getText().toString().isEmpty()) { iMinutes_idle_shutoff = 0; } else { iMinutes_idle_shutoff = Integer.valueOf(editOff_m.getText().toString()); }

        Integer iTimeToOFF = iMinutes_idle_shutoff * 60;
        var_name = "AUTO_SHUTOFF";
        var_value = String.valueOf(iTimeToOFF);
        settings_value += string_int_to_hex_4(String.valueOf(iTimeToOFF));

        new_settings_value = arduino_command_prefix + var_name + ":" + var_value + ";";


        if (!TextUtils.isEmpty(edit_TL84_delay.getText().toString())) {
            //makeToast("NO BUENO");
            bl_edit_tl84delay = validate_tl84_delay(Integer.valueOf(edit_TL84_delay.getText().toString()));
        }

        if (TextUtils.isEmpty(edit_emergency_delay.getText().toString())) {
            //makeToast("NO BUENO");
            edit_emergency_delay.setText("0");
        }

        if (bl_edit_tl84delay) {
            settings_value += string_int_to_hex_4(edit_TL84_delay.getText().toString());
            //makeToast("TL84 delay of " + edit_TL84_delay.getText().toString() + "ms stored in the config file.");
            var_name = "TL84_DELAY";
            var_value = edit_TL84_delay.getText().toString();
            new_settings_value += "," + var_name + ":" + var_value  + ";";
            makeToast("Config settings stored.");
        } else {
            settings_value += string_int_to_hex_4("0000");
        }

        var_name = "EMERGENCY_LIGHT_DELAY";
        var_value = edit_emergency_delay.getText().toString();
        new_settings_value += "," + var_name + ":" + var_value  + ";";

        String sCommand = new_settings_value + "$" + newLine;
        if (mBoundBT) {
            lclBTServiceInstance.sendData(sCommand);
        } else {
            Log.d(TAG, "Service btService not connected!");
        }
        Log.d(TAG, "*** ALLOFF *** Sending 'shutdown' intent to BtComms");

        if (shutdownTimer != null) {
            shutdownTimer.cancel();
        }
    }

    public void saveMultipliers(View v) {
        //We need to store the DAC Value if present
        boolean bl_valid_DAC_value;
        boolean bl_valid_psu_power = false;


        String settings_value = "";
        EditText ed;

        //group 1
        ed = findViewById(R.id.edit_group1_LED_full);
        if (!TextUtils.isEmpty(ed.getText().toString())) {
            settings_value += string_int_to_hex_2(ed.getText().toString());
        } else {
            settings_value += "00";
        }

        ed = findViewById(R.id.edit_group1_LED_dim);
        if (!TextUtils.isEmpty(ed.getText().toString())) {
            settings_value += string_int_to_hex_2(ed.getText().toString());
        } else {
            settings_value += "00";
        }

        ed = findViewById(R.id.edit_group1_TL84_full);
        if (!TextUtils.isEmpty(ed.getText().toString())) {
            settings_value += string_int_to_hex_2(ed.getText().toString());
        } else {
            settings_value += "00";
        }

        ed = findViewById(R.id.edit_group1_TL84_dim);
        if (!TextUtils.isEmpty(ed.getText().toString())) {
            settings_value += string_int_to_hex_2(ed.getText().toString());
        } else {
            settings_value += "00";
        }

        //group 2
        ed = findViewById(R.id.edit_group2_LED_full);
        if (!TextUtils.isEmpty(ed.getText().toString())) {
            settings_value += string_int_to_hex_2(ed.getText().toString());
        } else {
            settings_value += "00";
        }

        ed = findViewById(R.id.edit_group2_LED_dim);
        if (!TextUtils.isEmpty(ed.getText().toString())) {
            settings_value += string_int_to_hex_2(ed.getText().toString());
        } else {
            settings_value += "00";
        }

        ed = findViewById(R.id.edit_group2_TL84_full);
        if (!TextUtils.isEmpty(ed.getText().toString())) {
            settings_value += string_int_to_hex_2(ed.getText().toString());
        } else {
            settings_value += "00";
        }

        ed = findViewById(R.id.edit_group2_TL84_dim);
        if (!TextUtils.isEmpty(ed.getText().toString())) {
            settings_value += string_int_to_hex_2(ed.getText().toString());
        } else {
            settings_value += "00";
        }

        //group 3
        ed = findViewById(R.id.edit_group3_LED_full);
        if (!TextUtils.isEmpty(ed.getText().toString())) {
            settings_value += string_int_to_hex_2(ed.getText().toString());
        } else {
            settings_value += "00";
        }

        ed = findViewById(R.id.edit_group3_LED_dim);
        if (!TextUtils.isEmpty(ed.getText().toString())) {
            settings_value += string_int_to_hex_2(ed.getText().toString());
        } else {
            settings_value += "00";
        }

        ed = findViewById(R.id.edit_group3_TL84_full);
        if (!TextUtils.isEmpty(ed.getText().toString())) {
            settings_value += string_int_to_hex_2(ed.getText().toString());
        } else {
            settings_value += "00";
        }

        ed = findViewById(R.id.edit_group3_TL84_dim);
        if (!TextUtils.isEmpty(ed.getText().toString())) {
            settings_value += string_int_to_hex_2(ed.getText().toString());
        } else {
            settings_value += "00";
        }

        Log.d(TAG, "Sending unit config string: " + settings_value);
        String sCommand = "X302," + settings_value + "$" + newLine;
        if (mBoundBT) {
            Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
            lclBTServiceInstance.sendData(sCommand);
        } else {
            Log.d(TAG, "Service btService not connected when sending message: '" + sCommand + "'");
        }
        lclUsbServiceInstance.sendBytes(sCommand.getBytes());

        makeToast( "Saving multiplier data on Controller.");

    }

    public void allOFF() {
        String sCommand = "S00000000000000000000";
        sCommand += "$" + newLine;
        //if (btService.connected) {
        if (mBoundBT) {
            //Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
            lclBTServiceInstance.sendData(sCommand);
        } else {
            Log.d(TAG, "Service btService not connected!");
        }
        Log.d(TAG, "*** ALLOFF *** Sending 'shutdown' intent to BtComms");
        Intent serviceIntent= new Intent(TRSSettings.this,BtCOMMsService.class);
        serviceIntent.putExtra("shutdown", "1");
        startService(serviceIntent);

        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
        /*SharedPreferences prefs = getSharedPreferences(TIME_OFF_STORAGE, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();*/
    }

    public String string_int_to_hex_4(String s) {
        String sValue = "";

        int iValue = Integer.parseInt(s);
        sValue += String.format("%04X", iValue);

        sValue = sValue.toUpperCase();
        return sValue;
    }

    public String string_int_to_hex_2(String s) {
        String sValue = "";

        int iValue = Integer.parseInt(s);
        sValue += String.format("%02X", iValue);

        sValue = sValue.toUpperCase();
        return sValue;
    }

    public void switchOFFAfterX(int hours, int minutes) {

            shutdownTimer = new CountDownTimer(hours*60*60*1000 + minutes*60*1000 + 30, 10000) { //30ms is for shits and giggles
                public void onTick(long millisUntilFinished) {
                    Log.d(TAG, " *** TICK *** " + millisUntilFinished/1000 + " secs left until shutdown");
                }

                public void onFinish() {
                    Log.d(TAG, "*** TICKING OVER *** Time is up - switching all lamps off");
                    allOFF();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(TRSSettings.this, "Countdown timer is up - switching off all lamps.", Toast.LENGTH_LONG).show();
                        }
                    });

                }
            }.start();
    }

    private void get_shutdown_delay() {
        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        String s_shutoff = json_analyst.getJSONValue("eeprom_auto_shutoff");
        makeToast("SHUTOFF reading: " + s_shutoff);

    }

    private void get_TL84_delay() {
        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        String s_shutoff = json_analyst.getJSONValue("eeprom_tl84_delay");
        makeToast("TL84_DELAY reading: " + s_shutoff);

    }
    public void updateUIView() {
        Log.d("morris-sender", "Broadcasting message");
        Intent mIntent = new Intent("custom-event-name");
        mIntent.putExtra("iMessage", 0);
        LocalBroadcastManager.getInstance(this).sendBroadcast(mIntent);



        SharedPreferences spsFile = getSharedPreferences(SHAREDPREFS_ONE_OFF_SEEKBARS, 0);
        SharedPreferences.Editor spsEditor = spsFile.edit();
        spsEditor.clear();
        spsEditor.commit();

        spsEditor.putInt ("seekBar1", 0);
        spsEditor.putInt ("seekBar2", 0);
        spsEditor.putInt ("seekBar3", 0);
        spsEditor.putInt ("seekBar4", 0);
        spsEditor.putInt ("seekBar5", 0);
        spsEditor.putInt ("seekBar6", 0);
        spsEditor.putInt ("seekBar7", 0);
        spsEditor.putInt ("seekBar8", 0);
        spsEditor.putInt ("seekBar9", 0);
        spsEditor.putInt ("seekBar10", 0);

        spsEditor.commit();
    }

    public void switch_preset_on () {

    }

    public void makeToast (String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
