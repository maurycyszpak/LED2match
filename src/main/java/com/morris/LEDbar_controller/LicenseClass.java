package com.morris.LEDbar_controller;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.morris.LEDbar_controller.Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE;

public class LicenseClass extends Activity {

    public static final String TAG = "morris-License";
    EditText editLicense;
    public static final int MAGIC_KEY = 0x1F;
    public static final int MAGIC_KEY2 = 0x65;
    TextView textCurrentTier;
    private BtCOMMsService lclBTServiceInstance;
    private LightSettings.MyHandler mHandler;
    boolean mBoundBT = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.license_page_layout);
        editLicense = findViewById(R.id.editLicenseCode);
        TextView tv = findViewById(R.id.textMACconnected);
        tv.setText(mackme());
        set_custom_address();
    }

    @Override
        protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("controller_data_refreshed_event");
        Log.d(TAG, "mBoundBT = " + mBoundBT);
        if (!mBoundBT) {
            Intent intent = new Intent(this, BtCOMMsService.class);
            bindService(intent, btConnection, Context.BIND_AUTO_CREATE);
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
    }

    private final ServiceConnection btConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BtCOMMsService.MyBinder binder = (BtCOMMsService.MyBinder) service;
            lclBTServiceInstance = binder.getService();
            lclBTServiceInstance.setHandler(mHandler);
            mBoundBT = true;
            Log.d(TAG, "Connected to BT service instance, requesting firmware file (F)");
            request_license_data();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            lclBTServiceInstance = null;
            mBoundBT = false;
        }
    };


    public void validateLicense(View v) {
        if (editLicense.getText().equals(null)) {
            makeToast("No value provided in License Code field");
        } else {
            Log.d(TAG, "License code is not null");
            String license_code = "";
            String mac = mackme().replaceAll(":", "");
            Log.d(TAG, "MAC: " + mac);
            String sCommand = "X401,";
            sCommand += mac;

            String value = editLicense.getText().toString().trim();
            if (check_BT_connected()) {
                if (value.length() == 0) {
                    makeToast("No value provided in License Code field");
                } else {
                    //VALIDATE PROPA!
                    double ret = parse_validation_string(value);
                    if (ret != (int) ret) {
                        makeToast("Invalid license code");
                    } else {
                        //Validated
                        show_splash_screen();
                        makeToast("License code correct - current tier: " + (int) ret);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");
                        SimpleDateFormat dateserial = new SimpleDateFormat("yyyyMMdd");
                        String currentDate = sdf.format(new Date());
                        long lDate = Long.parseLong(dateserial.format(new Date()));
                        sCommand += Long.toHexString(lDate);
                        int digits = value.replaceAll("-", "").replaceAll("[Ff]", "").length();
                        sCommand += digits;
                        sCommand += Integer.parseInt(value.replaceAll("-", "").replaceAll("[Ff]", ""));
                        sCommand += (int)ret; //tier
                        sCommand = sCommand.concat("$").concat(System.lineSeparator());
                        Log.d(TAG, "Will send code: " + sCommand);
                        lclBTServiceInstance.sendData(sCommand);
                        //save_license_data(currentDate, (int)ret, mackme());

                        request_license_data();
                    }
                }
            } else {
                makeToast("To proceed with validation, please connect to the controller");
                Log.d(TAG, "validateLicense_() - unable to proceed with validation - not connected to controller");
            }
        }
    }

    private void save_license_data(String currentDate, int tier, String MAC) {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.LICENSE_DATE_TAG, currentDate);
        editor.putInt(Constants.LICENSE_TIER_TAG, tier);
        editor.putString(Constants.LICENSE_MAC_ADDR_TAG, MAC);
        editor.apply();
    }

    private void request_license_data() {
        show_splash_screen();
        makeToast("Please wait - communicating with the controller.");
        String sCommand = "X600,";
        sCommand = sCommand.concat(System.lineSeparator());
        Log.d(TAG, "requesting: " + sCommand);
        lclBTServiceInstance.sendData(sCommand);
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            if (intent == null) {
                return;
            }

            if (intent.getAction().equals("controller_data_refreshed_event")) {
                SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
                JSON_analyst json_analyst = new JSON_analyst(spFile);
                String licensed_date = json_analyst.getJSONValue("date_license_activated");

                TextView tv_date = findViewById(R.id.textLicensedDate);
                tv_date.setText(licensed_date);

                String licensed_tier = json_analyst.getJSONValue("license_tier");
                TextView tv_tier = findViewById(R.id.textLicensedTier);
                tv_tier.setText(String.valueOf(licensed_tier));
                Log.d(TAG, "Completed populating license data");
            }
        }
    };

    private void show_splash_screen() {
        Intent intent = new Intent(LicenseClass.this, OverlayPage.class);
        startActivity(intent);
    }

    private String convert_to_hex(long value) {
        String val = String.valueOf(value);
        String buf = "";
        int rest = val.length() % 4;
        for (int i=1; i<=(4-rest); i++) {
            val += "F";
        }
        for (int i=1; i<=val.length()/4; i++) {
            buf += val.substring(i*4-4, i*4) + "-";
        }
        buf = buf.substring(0, buf.length()-1);
        return buf;
    }


    private double parse_validation_string(String license_code) {
        String MAC_ADDR = mackme();
        double temp1 = 0.0;
        double temp2;
        MAC_ADDR = MAC_ADDR.replaceAll(":", "");
        Log.d(TAG, "parse_validation_string_() - " + MAC_ADDR + ", Length: " + MAC_ADDR.length());
        for (int i = 1; i <= MAC_ADDR.length() / 2; i++) {
            String ret = MAC_ADDR.substring(i*2-2, i*2);
            Integer val = Integer.parseInt(ret, 16);
            temp1 += (val *MAGIC_KEY);
            //Log.d(TAG, "parse_validation_string_() - token = " + ret + ", int = " + val + ", growing: " + temp);
        }
        temp2 = temp1*MAGIC_KEY2;
        temp1 *= MAGIC_KEY;
        //Log.d(TAG, "CALC_STEP_1: " + temp1);
        //Log.d(TAG, "CALC_STEP_2: " + temp2);
        double temp = temp1+temp2;
        //Log.d(TAG, "CALC_STEP_SUM: " + temp);
        license_code = license_code.replaceAll("[Ff]", "").replaceAll("-", "").replaceAll(" ", "");
        double tier = 0.0;
        try {
            long value2 = Long.parseLong(license_code);
            //Log.d(TAG, "comparing " + value2 + " with " +temp);
            tier = value2 / temp;
            //Log.d(TAG, "Decoded tier: " + tier);
        } catch (NumberFormatException nfe) {
            Log.d(TAG, "Number format exception: " + license_code);
            tier = 0;
            nfe.printStackTrace();
        }
        return tier;
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

    private boolean check_BT_connected() {
        SharedPreferences prefs = getSharedPreferences(Constants.BT_CONNECTED_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean("CONNECTED", false);
    }
    private static double customLog(double base, double logNumber) {
        return Math.log(logNumber) / Math.log(base);
    }

    private String mackme() {
        SharedPreferences prefs = getSharedPreferences(Constants.BT_CONNECTED_PREFS, Context.MODE_PRIVATE);
        Log.d(TAG, "mackme_() - " + prefs.getString(Constants.MAC_ADDRESS_PREFERENCES_TAG, ""));
        return prefs.getString(Constants.MAC_ADDRESS_PREFERENCES_TAG, "");
    }

    public void onClickBack(View v) {
        finish();

    }

    public void makeToast (String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}