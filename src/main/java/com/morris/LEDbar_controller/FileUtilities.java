package com.morris.LEDbar_controller;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileUtilities extends Activity {
    final Context context = this;
    private final String TAG = "MORRIS-APPUTILS";
    private String dataFilePath = "";
    private String logoFilePath = "";

    public FileUtilities(String dataFilePath, String logoFilePath) {

        this.dataFilePath = dataFilePath;
        this.logoFilePath = logoFilePath;
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

    public String get_value_from_customer_data(String tag) {
        if (!dataFileExists()) {
            Log.d(TAG, "File '" + Constants.CUSTOMER_DATA_FILENAME + "' couldn't be found!");
            return "";
        } else {
            Log.d(TAG, "Customer data XML file found");
            try {
                Log.d(TAG, "Opening JSON file for parsing");
                JSONObject jsonObject = new JSONObject(get_string_from_file(dataFilePath));
                String item = jsonObject.getString(tag);
                item.replace("\n", "\\n").replace("\r", "\\r");
                //Log.d(TAG, tag +": " + item);
                //makeToast(item);
                return item;

            } catch (Exception e) {
                Log.w(TAG, "Parsing JSON failed. Check format.");

                e.printStackTrace();
            }
            Log.d(TAG, "Customer data processed");
        }
        return "";
    }

    public boolean dataFileExists() {
        File file = new File(dataFilePath);
        if (file.exists()) {
            Log.d(TAG, "FILE EXISTS: " + dataFilePath);
            return true;
        } else {
            return false;
        }
    }

    public boolean logoFileExists() {
        File file = new File(logoFilePath);
        return file.exists();
    }
}
