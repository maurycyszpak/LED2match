package com.kiand.LED2match;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;


public class JSON_analyst {
    private final String TAG = "MORRIS-JSON-ANALYST";
    private final SharedPreferences spsFile;

    public JSON_analyst(SharedPreferences spsJsonFile) {

        this.spsFile = spsJsonFile;
    }

    String getJSONValue(String sKeyScanned) {
        String sReturn = "";
        String sJSONbody = getJsonContent(spsFile);
        //Log.d(TAG, "Contents of sJSONbody: " + sJSONbody);

        if (sJSONbody.length() == 0) {
            Log.d(TAG, "JSON content in SP file has length: 0. I would be concerned if I were you.");
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
                        //Log.d(TAG, "FOUND key = " + sKey + ", value = " + objValue.toString());
                        sReturn = objValue.toString();
                    } else {
                        //Log.d(TAG, "Checking key = " + sKey + " for \"" + sKeyScanned + "\"");
                    }
                } catch (JSONException e) {
                    Log.w(TAG, "JSONException when fetching JSON value: " + e.getMessage());
                    // Something went wrong!
                }
            }
        } catch (JSONException j) {
            Log.w(TAG, "Unable to parse string to JSON object");
        }
        return sReturn;
    }

    public String getRGBW(String sPresetName) {

        sPresetName = sPresetName.replaceAll("\"", "");
        String sResult = "";
        String sRGB = "";
        for (int i=1; i<11; i++) {
            sResult = getJSONValue("preset" + i + "_name");
            if (sResult.equals(sPresetName)) {
                sRGB = getJSONValue("preset" + i + "_rgbw");
            }
        }

        return sRGB;
    }

    Integer getPresetIndex(String sPresetName) {
        sPresetName = sPresetName.replaceAll("\"", "");
        String sResult = "";
        Integer iIndex = 0;
        for (int i=1; i<11; i++) {
            sResult = getJSONValue("preset" + i + "_name");
            Log.d("JSON_ANALYST", "Comparing '"+sPresetName +"' with '" + sResult +"'");
            if (sResult.equals(sPresetName)) {
                iIndex = i;
            }
        }
        return iIndex;
    }

    private String getJsonContent(SharedPreferences sSharedPrefsFile) {
        String sReturn = "";
        String sVal = "";

        //SharedPreferences spsValues = getSharedPreferences(sSharedPrefsFilename, MODE_PRIVATE);
        sReturn = sSharedPrefsFile.getString("JSON", "");

        return sReturn;
    }

    public String populatePresetsFromFILE() {

        ArrayList<String> presetNames = new ArrayList<>();
        for (int i=1; i<11; i++) {
            String sPresetName = getJSONValue("preset" + i + "_name");
            String sPresetRGB = getJSONValue("preset" + i + "_rgbw");
            if (sPresetName.length() > 0) {
                String sTemp = sPresetName + ":" + sPresetRGB;
                presetNames.add(sTemp);
            }
        }
        return TextUtils.join(";", presetNames);
    }
}
