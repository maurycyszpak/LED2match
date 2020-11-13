package com.kiand.LED2match;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import static com.kiand.LED2match.LightSettings.sNewLine;
import static com.kiand.LED2match.TRSDigitalPanel.SHAREDPREFS_LAMP_ASSIGNMENTS;

public class TRSLightOperatingHours extends Activity {

    public static final String SHAREDPREFS_ONE_OFF_SEEKBARS = "one-off-seekbar-values.txt"; //Mauricio
    public static final String SHAREDPREFS_LED_TIMERS = "led_timers"; //Mauricio
    public static final String newLine = System.getProperty("line.separator");
    public static final String NO_PRESET_TEXT = "#n/a";


    boolean blLamp1_ON, blLamp2_ON, blLamp3_ON, blLamp4_ON;
    Button btnL1, btnL2, btnL3, btnL4, btnL5, btnL6;
    String sEmptyTimerValue = "00:00";
    public final String TAG = "MORRIS-LED-HOURS";

    private Handler lclHandler;
    private UsbCOMMsService lclUsbServiceInstance;
    private BtCOMMsService lclBTServiceInstance;

    boolean mBound = false;
    boolean mBoundBT = false;
    final Context context = this;

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

    private final ServiceConnection btConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BtCOMMsService.MyBinder binder = (BtCOMMsService.MyBinder) service;
            lclBTServiceInstance = binder.getService();
            Log.d(TAG, "onServiceConnected_() - we should be bound now. BTServiceInstance null? " + (lclBTServiceInstance == null));
            mBoundBT = true;
            onClickTimers(null);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBoundBT = false;
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

        if (shared_prefs_exists(SHAREDPREFS_LAMP_ASSIGNMENTS, "666")) {
            repopulate_button_assignments();
            Log.d(TAG, "Repopulating button captions");
        } else {
            populateButtonNames();
        }
        //Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();
        if (!mBound) {
            Intent intent = new Intent(this, UsbCOMMsService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            //Toast.makeText(this.getBaseContext(),"Service bound (onResume)", Toast.LENGTH_SHORT).show();
        }
        if (!mBoundBT) {
            Intent intentBT = new Intent(this, BtCOMMsService.class);
            bindService(intentBT, btConnection, Context.BIND_AUTO_CREATE);
            SystemClock.sleep(100);
        }

        if (mBoundBT) {
            onClickTimers(null);
        }
        populate_button_timers();

        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        String sFWVersion = json_analyst.getJSONValue("firmware_version");
        TextView tvInfoBox = findViewById(R.id.infobox);
        String version_line = "FW Version: " + sFWVersion;
        tvInfoBox.setText(getString(R.string.system_footer) + "\n" + version_line);
        populateLampsState();
        license_check();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to UsbService
        //Toast.makeText(this.getBaseContext(),"Activity started", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, UsbCOMMsService.class);
        bindService (intent, mConnection, Context.BIND_AUTO_CREATE);
        show_splash_screen();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
        unbindService(btConnection);
        mBoundBT = false;
        mBound = false;
        //Toast.makeText(this.getBaseContext(),"Activity stopped", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"Service unbound", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trs_light_operating_hours);

        lclHandler = new Handler();

        btnL1 = findViewById(R.id.btnL1);
        btnL2 = findViewById(R.id.btnL2);
        btnL3 = findViewById(R.id.btnL3);
        btnL4 = findViewById(R.id.btnL4);
        btnL5 = findViewById(R.id.btnL5);
        btnL6 = findViewById(R.id.btnL6);


        btnL1.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL1.setTextColor(Color.WHITE);

        btnL2.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL2.setTextColor(Color.WHITE);

        btnL3.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL3.setTextColor(Color.WHITE);

        btnL4.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL4.setTextColor(Color.WHITE);

        btnL5.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL5.setTextColor(Color.WHITE);

        btnL6.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL6.setTextColor(Color.WHITE);

        //populateButtonNames();
        repopulate_button_assignments();
        populate_button_timers_empty();
        set_custom_address();
    }

    public int get_tier() {
        boolean connected = get_connection_status();

        SharedPreferences prefs_config = getSharedPreferences(Constants.CONFIG_SETTINGS, 0);
        int current_tier = prefs_config.getInt(Constants.LICENSE_TIER_TAG, 0);

        Log.d(TAG, "get_tier_() - CONNECTED status: " + connected);
        if (connected) {
            String licensed_mac = prefs_config.getString(Constants.LICENSE_MAC_ADDR_TAG, "NO DATA");
            SharedPreferences prefs_connection = getSharedPreferences(Constants.BT_CONNECTED_PREFS, 0);
            String connected_mac = prefs_connection.getString(Constants.SESSION_CONNECTED_MAC_TAG, "NO DATA");
            Log.d(TAG, "get_tier_() - Licensed MAC: " + licensed_mac);
            Log.d(TAG, "get_tier_() - Connected MAC: " + connected_mac);
            if (!licensed_mac.equalsIgnoreCase(connected_mac)) {
                Log.d(TAG, "Detected connection to a non-licensed MAC address");
                makeToast("Detected connection to a non-licensed MAC address");
                current_tier = 0;
            }
        }
        Log.d(TAG, "get_tier_() - returning TIER " + current_tier);
        return current_tier;
    }

    public boolean get_connection_status() {
        SharedPreferences prefs_config = getSharedPreferences(Constants.BT_CONNECTED_PREFS, 0);
        boolean status= prefs_config.getBoolean("CONNECTED", false);

        return status;
    }

    private void license_check() {
        int current_tier = get_tier();

        if (current_tier < Constants.LICENSE_TIER_OPERATING_HOURS_PAGE) {
            Log.d(TAG, "Blocking page");
            block_current_page();
        } else {
            Log.d(TAG, "Not blocking page");
        }
    }

    private void block_current_page() {
        Intent intent = new Intent(TRSLightOperatingHours.this, DisabledOverlayPage.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK) ;
        startActivity(intent);

    }

    public void populate_button_timers_empty() {
        Log.d(TAG, "Populating EMPTY button timers");
        if (!btnL1.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            btnL1.setText(btnL1.getTag().toString() + sNewLine + sEmptyTimerValue);
        }

        if (!btnL2.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            btnL2.setText(btnL2.getTag().toString() + sNewLine + sEmptyTimerValue);
        }

        if (!btnL3.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            btnL3.setText(btnL3.getTag().toString() + sNewLine + sEmptyTimerValue);
        }

        if (!btnL4.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            btnL4.setText(btnL4.getTag().toString() + sNewLine + sEmptyTimerValue);
        }

        if (!btnL5.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            btnL5.setText(btnL5.getTag().toString() + sNewLine + sEmptyTimerValue);
        }

        if (!btnL6.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            btnL6.setText(btnL6.getTag().toString() + sNewLine + sEmptyTimerValue);
        }
    }

    public void populate_button_timers() {

        Log.d(TAG, "Populating button timers");
        SharedPreferences prefs = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(prefs);
        String preset_name = "";
        String preset_timer = "";

        //Log.d(TAG, "Checking time elapsed for preset: " + btnL1.getTag().toString());
        //String time_elapsed = json_analyst.getJSONValue(btnL1.getTag().toString() + "_time_elapsed");

        Log.d(TAG, "Pulling name of preset: " + btnL1.getTag().toString() + "_name");
        if (!btnL1.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            Log.d(TAG, "button1 tag = " + btnL1.getTag().toString());
            preset_name = json_analyst.getJSONValue(btnL1.getTag().toString() + "_name");
            preset_timer = json_analyst.getJSONValue(btnL1.getTag().toString() + "_time_elapsed");
            btnL1.setText(preset_name + sNewLine + convert_secs_to_hhmm(preset_timer));
        }

        if (!btnL2.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            Log.d(TAG, "button2 tag = " + btnL2.getTag().toString());
            preset_name = json_analyst.getJSONValue(btnL2.getTag().toString() + "_name");
            preset_timer = json_analyst.getJSONValue(btnL2.getTag().toString() + "_time_elapsed");
            btnL2.setText(preset_name + sNewLine + convert_secs_to_hhmm(preset_timer));
        }

        if (!btnL3.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            Log.d(TAG, "button3 tag = " + btnL3.getTag().toString());
            preset_name = json_analyst.getJSONValue(btnL3.getTag().toString() + "_name");
            preset_timer = json_analyst.getJSONValue(btnL3.getTag().toString() + "_time_elapsed");
            btnL3.setText(preset_name + sNewLine + convert_secs_to_hhmm(preset_timer));
        }

        if (!btnL4.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            preset_name = json_analyst.getJSONValue(btnL4.getTag().toString() + "_name");
            preset_timer = json_analyst.getJSONValue(btnL4.getTag().toString() + "_time_elapsed");
            btnL4.setText(preset_name + sNewLine + convert_secs_to_hhmm(preset_timer));
        }

        if (!btnL5.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            preset_name = json_analyst.getJSONValue(btnL5.getTag().toString() + "_name");
            preset_timer = json_analyst.getJSONValue(btnL5.getTag().toString() + "_time_elapsed");
            btnL5.setText(preset_name + sNewLine + convert_secs_to_hhmm(preset_timer));
        }

        if (!btnL6.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            preset_name = json_analyst.getJSONValue(btnL6.getTag().toString() + "_name");
            preset_timer = json_analyst.getJSONValue(btnL6.getTag().toString() + "_time_elapsed");
            btnL6.setText(preset_name + sNewLine + convert_secs_to_hhmm(preset_timer));
        }
    }
    private String convert_secs_to_hhmm (String sSeconds) {
        if (sSeconds.length() > 0) {
            Long lSecs = Long.valueOf(sSeconds);
            Integer iHours = lSecs.intValue()/60/60;
            Integer iMinutes = (lSecs.intValue() - iHours*60*60)/60;

            return String.format("%02d", iHours) + ":" + String.format("%02d", iMinutes);
        } else {
            return "N/A";
        }

    }

    private void show_splash_screen() {
        Intent intent = new Intent(TRSLightOperatingHours.this, OverlayPage.class);
        startActivity(intent);
    }

    public void repopulate_button_assignments() {
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

        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        // now check which preset is currently mapped to a particular button
        // e.g.: key--> 1, value --> preset3
        SharedPreferences prefs = getSharedPreferences(Constants.NEW_SHAREDPREFS_LAMP_ASSIGNMENTS, MODE_PRIVATE);
        TreeMap<String, ?> keys = new TreeMap<String, Object>(prefs.getAll());

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            switch (entry.getKey()) {
                case "1":
                    // preset8. Get name of this preset
                    String preset_name = json_analyst.getJSONValue(entry.getValue() + "_name");
                    btnL1.setText(entry.getValue().toString());
                    btnL1.setText(preset_name);
                    btnL1.setTag(entry.getValue().toString());
                    //btnL1.setTag(preset_name);
                    break;

                case "2":
                    preset_name = json_analyst.getJSONValue(entry.getValue() + "_name");
                    btnL2.setText(entry.getValue().toString());
                    btnL2.setText(preset_name);
                    btnL2.setTag(entry.getValue().toString());
                    break;

                case "3":
                    preset_name = json_analyst.getJSONValue(entry.getValue() + "_name");
                    btnL3.setText(entry.getValue().toString());
                    btnL3.setText(preset_name);
                    btnL3.setTag(entry.getValue().toString());
                    break;

                case "4":
                    preset_name = json_analyst.getJSONValue(entry.getValue() + "_name");
                    btnL4.setText(entry.getValue().toString());
                    btnL4.setText(preset_name);
                    btnL4.setTag(entry.getValue().toString());
                    break;

                case "5":
                    preset_name = json_analyst.getJSONValue(entry.getValue() + "_name");
                    btnL5.setText(entry.getValue().toString());
                    btnL5.setText(preset_name);
                    btnL5.setTag(entry.getValue().toString());
                    break;

                case "6":
                    preset_name = json_analyst.getJSONValue(entry.getValue() + "_name");
                    btnL6.setText(entry.getValue().toString());
                    btnL6.setText(preset_name);
                    btnL6.setTag(entry.getValue().toString());
                    break;
            }
        }
    }

    public void onClickBack (View v) {
        finish();
    }

    public void onClickTimers (View v) {
        String sSequence = "F";
        try {
            sSequence = sSequence.concat(sNewLine);
            lclBTServiceInstance.sendData(sSequence);
            Log.d(TAG, "onClickTimers_() - Requesting timers via commmand '" + sSequence.replace(sNewLine, "") + "'.");
        } catch (NullPointerException e) {
            Log.d(TAG, "onClickTimers_() - NullPointerException when sending command '" + sSequence.replace(sNewLine, "") + "' via Bluetooth");
            //Log.e(TAG, Log.getStackTraceString(e));
        }
    }


    public boolean shared_prefs_exists(String sFileName, String sKey) {
        SharedPreferences spFile = getSharedPreferences(sFileName, 0);
        return spFile.contains(sKey);
    }

    public void populateButtonNames() {
        //String sUnitName = "";
        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        //Log.d(TAG, "bluetoothAskReply(V1)");
        final String sLamp1Counter = json_analyst.getJSONValue("preset1_time_elapsed");
        final String sLamp2Counter = json_analyst.getJSONValue("preset2_time_elapsed");
        final String sLamp3Counter = json_analyst.getJSONValue("preset3_time_elapsed");
        final String sLamp4Counter = json_analyst.getJSONValue("preset4_time_elapsed");
        final String sLamp5Counter = json_analyst.getJSONValue("preset5_time_elapsed");
        final String sLamp6Counter = json_analyst.getJSONValue("preset6_time_elapsed");

        final String sLamp1Name = json_analyst.getJSONValue("preset1_name");
        final String sLamp2Name = json_analyst.getJSONValue("preset2_name");
        final String sLamp3Name = json_analyst.getJSONValue("preset3_name");
        final String sLamp4Name = json_analyst.getJSONValue("preset4_name");
        final String sLamp5Name = json_analyst.getJSONValue("preset5_name");
        final String sLamp6Name = json_analyst.getJSONValue("preset6_name");

        //final String sLamp4Name = extractJSONvalue("", "lamp4_name");

        setLampName(1, sLamp1Name, sLamp1Counter);
        setLampName(2, sLamp2Name, sLamp2Counter);
        setLampName(3, sLamp3Name, sLamp3Counter);
        setLampName(4, sLamp4Name, sLamp4Counter);
        setLampName(5, sLamp5Name, sLamp5Counter);
        setLampName(6, sLamp6Name, sLamp6Counter);
    }

    public void setLampName(int i, String sLampName, String sCounter) {
        if (i == 1) {
            if (sLampName.length() > 0) {
                btnL1.setText(sLampName + sNewLine + sCounter);
                btnL1.setTag(sLampName);
            } else {
                btnL1.setText(NO_PRESET_TEXT);
                btnL1.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 2) {
            if (sLampName.length() > 0) {
                btnL2.setText(sLampName + sNewLine + sCounter);
                btnL2.setTag(sLampName);
            } else {
                btnL2.setText(NO_PRESET_TEXT);
                btnL2.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 3) {
            if (sLampName.length() > 0) {
                btnL3.setText(sLampName + sNewLine + sCounter);
                btnL3.setTag(sLampName);
            } else {
                btnL3.setText(NO_PRESET_TEXT);
                btnL3.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 4) {
            if (sLampName.length() > 0) {
                btnL4.setText(sLampName + sNewLine + sCounter);
                btnL4.setTag(sLampName);
            } else {
                btnL4.setText(NO_PRESET_TEXT);
                btnL4.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 5) {
            if (sLampName.length() > 0) {
                btnL5.setText(sLampName + sNewLine + sCounter);
                btnL5.setTag(sLampName);
            } else {
                btnL5.setText(NO_PRESET_TEXT);
                btnL5.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 6) {
            if (sLampName.length() > 0) {
                btnL6.setText(sLampName + sNewLine + sCounter);
                btnL6.setTag(sLampName);
            } else {
                btnL6.setText(NO_PRESET_TEXT);
                btnL6.setTag(NO_PRESET_TEXT);
            }
        }

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

    public boolean display_custom_data() {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, Context.MODE_PRIVATE);
        Boolean bl_custom_data = prefs.getBoolean(Constants.CUSTOMER_DATA_FLAG, false);
        Log.d(TAG, "display_custom_data_(): USE CUSTOMER DATA = " + bl_custom_data);
        return bl_custom_data;
    }

    private void set_custom_address() {
        FileUtilities fileUtilities = new FileUtilities(get_path_to_customer_datafile(), get_path_to_customer_logofile());

        if (display_custom_data()) {
            String footer_address = fileUtilities.get_value_from_customer_data(Constants.CUSTOMER_DATA_ABOUT_PAGE_LINE_2_TAG);

            footer_address = footer_address.replace("\\n", System.lineSeparator());
            TextView text_footer_address = findViewById(R.id.text_customer_address);
            text_footer_address.setText(footer_address);
        }
    }

    public void resetOperatingTime(View v) {

        //placeholder
        openDialog(v);
    }

    public void openDialog(final View view) {

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptView = layoutInflater.inflate(R.layout.okcancel, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set prompts.xml to be the layout file of the alertdialog builder
        alertDialogBuilder.setView(promptView);

        // setup a dialog window
        alertDialogBuilder
            .setCancelable(true)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    makeToast("Clicked OK");
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

    public void makeToast (String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
