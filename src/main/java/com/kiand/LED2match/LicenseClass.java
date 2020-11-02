package com.kiand.LED2match;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LicenseClass extends Activity {

    public static final String TAG = "morris-License";
    EditText editLicense;
    public static final int MAGIC_KEY = 0x0295;
    public static final int MAGIC_KEY2 = 0x06F1;
    TextView textCurrentTier;

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
        read_license_data();
    }


    public void validateLicense(View v) {
        if (editLicense.getText().equals(null)) {
            makeToast("No value provided in License Code field");
        } else {
            Log.d(TAG, "License code is not null");
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
                        makeToast("License code correct - current tier: " + (int) ret);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");
                        String currentDate = sdf.format(new Date());
                        save_license_data(currentDate, (int)ret, mackme());

                        read_license_data();
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

    private void read_license_data() {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, MODE_PRIVATE);
        String licensed_date = prefs.getString(Constants.LICENSE_DATE_TAG, "NO DATA");
        int licensed_tier = prefs.getInt(Constants.LICENSE_TIER_TAG, 0);
        String licensed_MAC = prefs.getString(Constants.LICENSE_MAC_ADDR_TAG, "NO DATA");

        TextView tv_date = findViewById(R.id.textLicensedDate);
        tv_date.setText(licensed_date);

        TextView tv_tier = findViewById(R.id.textLicensedTier);
        tv_tier.setText(String.valueOf(licensed_tier));

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


    private double parse_validation_string(String value) {
        String MAC_ADDR = mackme();
        long temp = 0;
        MAC_ADDR = MAC_ADDR.replaceAll(":", "");
        Log.d(TAG, "parse_validation_string_() - " + MAC_ADDR + ", Length: " + MAC_ADDR.length());
        for (int i = 1; i <= MAC_ADDR.length() / 2; i++) {
            String ret = MAC_ADDR.substring(i*2-2, i*2);
            Integer val = Integer.parseInt(ret, 16);
            temp += (val *MAGIC_KEY);
            //Log.d(TAG, "parse_validation_string_() - token = " + ret + ", int = " + val + ", growing: " + temp);
        }
        //Log.d(TAG, temp + " * " + MAGIC_KEY + " = " + temp * MAGIC_KEY);
        temp *= MAGIC_KEY;

        value = value.replaceAll("[Ff]", "").replaceAll("-", "").replaceAll(" ", "");
        long value2 = Long.parseLong(value) - MAGIC_KEY2;
        long result = value2 - temp;
        result = result / MAGIC_KEY2;
        double tier = customLog(MAGIC_KEY, result);
        Log.d(TAG, "Decoding license key to: " + result);
        Log.d(TAG, "Decoded tier: " + tier);

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