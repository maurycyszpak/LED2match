package com.kiand.LED2match;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class ReassignLamps extends Activity {

    final Context context = this;
    public static final String SHAREDPREFS_LAMP_ASSIGNMENTS = "lamp_button_assignments";
    public static final String TAG = "MORRIS_RSSGN";

    Spinner spinner1, spinner2, spinner3, spinner4, spinner5, spinner6, spinner7, spinner8, spinner9;
    ArrayAdapter arrayAdapterLampNames;
    public ArrayList<String> arrayListLampNames = new ArrayList<String>();
    public Boolean blDirty = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reassign_lamps);
        Intent intent = getIntent();
        String sTags = intent.getExtras().getString("tags");
        Integer iCounter = intent.getExtras().getInt("counter");

        //makeToast(sTags);
        String[] sValues = sTags.split(",");
        //makeToast("Number of items: "+ sValues.length);

        spinner1 = findViewById(R.id.spinner1);
        spinner2 = findViewById(R.id.spinner2);
        spinner3 = findViewById(R.id.spinner3);
        spinner4 = findViewById(R.id.spinner4);
        spinner5 = findViewById(R.id.spinner5);
        spinner6 = findViewById(R.id.spinner6);
        spinner7 = findViewById(R.id.spinner7);
        spinner8 = findViewById(R.id.spinner8);
        spinner9 = findViewById(R.id.spinner9);

        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        arrayListLampNames.clear();

        for (int i=1; i<=Constants.MAX_PRESET_NUM; i++) {
            String sKey = "preset" + i + "_name";
            if (json_analyst.getJSONValue(sKey).length() > 0) {
                arrayListLampNames.add(json_analyst.getJSONValue(sKey));
            } else {

            }
        }
        arrayListLampNames.add("");

        /*arrayListLampNames.add(json_analyst.getJSONValue("preset1_name"));
        arrayListLampNames.add(json_analyst.getJSONValue("preset2_name"));
        arrayListLampNames.add(json_analyst.getJSONValue("preset3_name"));
        arrayListLampNames.add(json_analyst.getJSONValue("preset4_name"));
        arrayListLampNames.add(json_analyst.getJSONValue("preset5_name"));
        arrayListLampNames.add(json_analyst.getJSONValue("preset6_name"));*/


        arrayAdapterLampNames = new ArrayAdapter<String>(this, R.layout.spinner_lamp_assignment, arrayListLampNames);
        spinner1.setAdapter(arrayAdapterLampNames);
        spinner2.setAdapter(arrayAdapterLampNames);
        spinner3.setAdapter(arrayAdapterLampNames);
        spinner4.setAdapter(arrayAdapterLampNames);
        spinner5.setAdapter(arrayAdapterLampNames);
        spinner6.setAdapter(arrayAdapterLampNames);
        spinner7.setAdapter(arrayAdapterLampNames);
        spinner8.setAdapter(arrayAdapterLampNames);
        spinner9.setAdapter(arrayAdapterLampNames);

        String sCurrValue = "";
        int spinnerPosition = 0;

        try {
            sCurrValue = sValues[0];
            spinnerPosition = arrayAdapterLampNames.getPosition(sCurrValue);
            spinner1.setSelection(spinnerPosition);
        } catch (ArrayIndexOutOfBoundsException e) {
            spinnerPosition = arrayAdapterLampNames.getPosition("");
            spinner1.setSelection(spinnerPosition);
        }

        try {
            sCurrValue = sValues[1];
            spinnerPosition = arrayAdapterLampNames.getPosition(sCurrValue);
            spinner2.setSelection(spinnerPosition);
        } catch (ArrayIndexOutOfBoundsException e) {
            spinnerPosition = arrayAdapterLampNames.getPosition("");
            spinner2.setSelection(spinnerPosition);
        }

        try {
            sCurrValue = sValues[2];
            spinnerPosition = arrayAdapterLampNames.getPosition(sCurrValue);
            spinner3.setSelection(spinnerPosition);
        } catch (ArrayIndexOutOfBoundsException e) {
            spinnerPosition = arrayAdapterLampNames.getPosition("");
            spinner3.setSelection(spinnerPosition);
        }

        try {
            sCurrValue = sValues[3];
            spinnerPosition = arrayAdapterLampNames.getPosition(sCurrValue);
            spinner4.setSelection(spinnerPosition);
        } catch (ArrayIndexOutOfBoundsException e) {
            spinnerPosition = arrayAdapterLampNames.getPosition("");
            spinner4.setSelection(spinnerPosition);
        }

        try {
            sCurrValue = sValues[4];
            spinnerPosition = arrayAdapterLampNames.getPosition(sCurrValue);
            spinner5.setSelection(spinnerPosition);
        } catch (ArrayIndexOutOfBoundsException e) {
            spinnerPosition = arrayAdapterLampNames.getPosition("");
            spinner5.setSelection(spinnerPosition);
        }

        try {
            sCurrValue = sValues[5];
            spinnerPosition = arrayAdapterLampNames.getPosition(sCurrValue);
            spinner6.setSelection(spinnerPosition);
        } catch (ArrayIndexOutOfBoundsException e) {
            spinnerPosition = arrayAdapterLampNames.getPosition("");
            spinner6.setSelection(spinnerPosition);
        }

        try {
            sCurrValue = sValues[6];
            spinnerPosition = arrayAdapterLampNames.getPosition(sCurrValue);
            spinner7.setSelection(spinnerPosition);
        } catch (ArrayIndexOutOfBoundsException e) {
            spinnerPosition = arrayAdapterLampNames.getPosition("");
            spinner7.setSelection(spinnerPosition);
        }

        try {
            sCurrValue = sValues[7];
            spinnerPosition = arrayAdapterLampNames.getPosition(sCurrValue);
            spinner8.setSelection(spinnerPosition);
        } catch (ArrayIndexOutOfBoundsException e) {
            spinnerPosition = arrayAdapterLampNames.getPosition("");
            spinner8.setSelection(spinnerPosition);
        }

        try {
            sCurrValue = sValues[8];
            spinnerPosition = arrayAdapterLampNames.getPosition(sCurrValue);
            spinner9.setSelection(spinnerPosition);
        } catch (ArrayIndexOutOfBoundsException e) {
            spinnerPosition = arrayAdapterLampNames.getPosition("");
            spinner9.setSelection(spinnerPosition);
        }
        this.setTitle(getString(R.string.app_header_title) + " assign buttons");


    }

    public int findGivenPresetSlot(String presetName) {
        int i = -1;
        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        String json = spFile.getString("JSON", "");
        json = json.replaceAll(";", ",");
        try {
            JSONObject jsonObject = new JSONObject(json);
            Iterator<String> keysIterator = jsonObject.keys();
            int j =0;
            while (keysIterator.hasNext()) {
                String key = keysIterator.next();
                String value = jsonObject.getString(key);
                if (key.contains("_name") && key.contains("preset")) {
                    j++;
                    if (value.equalsIgnoreCase(presetName)) {
                        return j;
                    }
                }
            }
        } catch (JSONException ioe) {
            ioe.printStackTrace();
            Log.e(TAG, "Unable to process JSON: " + json);
        }
        return i;
    }

    public void store_assignment (View v) {
        if (validate_assignments()) {
            makeToast("Lamp presets assigned successfully.");

            SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_LAMP_ASSIGNMENTS, 0);
            SharedPreferences prefs = getSharedPreferences(Constants.NEW_SHAREDPREFS_LAMP_ASSIGNMENTS, 0);
            SharedPreferences.Editor edit = spFile.edit();
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            edit.clear();
            edit.apply();


            //makeToast("Spinner1 text size: " + spinner1.getSelectedItem().toString().length());
            //SystemClock.sleep(15000);

            int slot1 = findGivenPresetSlot(spinner1.getSelectedItem().toString());
            if (spinner1.getSelectedItem().toString().length() > 0) { editor.putString("1", "preset" + slot1); }

            int slot2 = findGivenPresetSlot(spinner2.getSelectedItem().toString());
            if (spinner2.getSelectedItem().toString().length() > 0) { editor.putString("2", "preset" + slot2); }

            int slot3 = findGivenPresetSlot(spinner3.getSelectedItem().toString());
            if (spinner3.getSelectedItem().toString().length() > 0) { editor.putString("3", "preset" + slot3); }

            int slot4 = findGivenPresetSlot(spinner4.getSelectedItem().toString());
            if (spinner4.getSelectedItem().toString().length() > 0) { editor.putString("4", "preset" + slot4); }

            int slot5 = findGivenPresetSlot(spinner5.getSelectedItem().toString());
            if (spinner5.getSelectedItem().toString().length() > 0) { editor.putString("5", "preset" + slot5); }

            int slot6 = findGivenPresetSlot(spinner6.getSelectedItem().toString());
            if (spinner6.getSelectedItem().toString().length() > 0) { editor.putString("6", "preset" + slot6); }

            int slot7 = findGivenPresetSlot(spinner7.getSelectedItem().toString());
            if (spinner7.getSelectedItem().toString().length() > 0) { editor.putString("7", "preset" + slot7); }

            int slot8 = findGivenPresetSlot(spinner8.getSelectedItem().toString());
            if (spinner8.getSelectedItem().toString().length() > 0) { editor.putString("8", "preset" + slot8); }

            int slot9 = findGivenPresetSlot(spinner9.getSelectedItem().toString());
            if (spinner9.getSelectedItem().toString().length() > 0) { editor.putString("9", "preset" + slot9); }


            if (spinner1.getSelectedItem().toString().length() > 0) { edit.putString("1", spinner1.getSelectedItem().toString()); }
            if (spinner2.getSelectedItem().toString().length() > 0) { edit.putString("2", spinner2.getSelectedItem().toString()); }
            if (spinner3.getSelectedItem().toString().length() > 0) { edit.putString("3", spinner3.getSelectedItem().toString()); }
            if (spinner4.getSelectedItem().toString().length() > 0) { edit.putString("4", spinner4.getSelectedItem().toString()); }
            if (spinner5.getSelectedItem().toString().length() > 0) { edit.putString("5", spinner5.getSelectedItem().toString()); }
            if (spinner6.getSelectedItem().toString().length() > 0) { edit.putString("6", spinner6.getSelectedItem().toString()); }
            if (spinner7.getSelectedItem().toString().length() > 0) { edit.putString("21", spinner7.getSelectedItem().toString()); }
            if (spinner8.getSelectedItem().toString().length() > 0) { edit.putString("22", spinner8.getSelectedItem().toString()); }
            if (spinner9.getSelectedItem().toString().length() > 0) { edit.putString("23", spinner9.getSelectedItem().toString()); }
            edit.putString("666", String.valueOf(System.currentTimeMillis()));
            editor.putString("666", String.valueOf(System.currentTimeMillis()));
            edit.apply();
            editor.apply();

            finish();
        }
    }

    public void cancel_assignment (View v) {

        finish();
    }

    public boolean validate_assignments() {
        boolean blReturn = false;

        ArrayList<String> stringSpinnerValues= new ArrayList<String>();

        if (spinner1.getSelectedItem().toString().length() > 0) { stringSpinnerValues.add(spinner1.getSelectedItem().toString()); }
        if (spinner2.getSelectedItem().toString().length() > 0) { stringSpinnerValues.add(spinner2.getSelectedItem().toString()); }
        if (spinner3.getSelectedItem().toString().length() > 0) { stringSpinnerValues.add(spinner3.getSelectedItem().toString()); }
        if (spinner4.getSelectedItem().toString().length() > 0) { stringSpinnerValues.add(spinner4.getSelectedItem().toString()); }
        if (spinner5.getSelectedItem().toString().length() > 0) { stringSpinnerValues.add(spinner5.getSelectedItem().toString()); }
        if (spinner6.getSelectedItem().toString().length() > 0) { stringSpinnerValues.add(spinner6.getSelectedItem().toString()); }
        if (spinner7.getSelectedItem().toString().length() > 0) { stringSpinnerValues.add(spinner7.getSelectedItem().toString()); }
        if (spinner8.getSelectedItem().toString().length() > 0) { stringSpinnerValues.add(spinner8.getSelectedItem().toString()); }
        if (spinner9.getSelectedItem().toString().length() > 0) { stringSpinnerValues.add(spinner9.getSelectedItem().toString()); }

        Set<String> deduped = new HashSet<String>(stringSpinnerValues);

        if (deduped.size() == 0) {
            makeToast("No button assignments changed");
        } else if (deduped.size() != stringSpinnerValues.size()) {
            makeToast("Duplicate values found at assignments. Please correct");
        } else {
            blReturn = true;
        }

        return blReturn;
    }


    public void makeToast (String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

}
