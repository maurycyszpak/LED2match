package com.kiand.LED2match;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.kiand.LED2match.LightAdjustments.SHAREDPREFS_CONTROLLER_FILEIMAGE;


public class ReassignLamps extends Activity {

    final Context context = this;
    public static final String SHAREDPREFS_LAMP_ASSIGNMENTS = "lamp_button_assignments";

    Spinner spinner1, spinner2, spinner3, spinner4, spinner5, spinner6;
    ArrayAdapter arrayAdapterLampNames;
    public ArrayList<String> arrayListLampNames = new ArrayList<String>();

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

        spinner1 = (Spinner) findViewById(R.id.spinner1);
        spinner2 = (Spinner) findViewById(R.id.spinner2);
        spinner3 = (Spinner) findViewById(R.id.spinner3);
        spinner4 = (Spinner) findViewById(R.id.spinner4);
        spinner5 = (Spinner) findViewById(R.id.spinner5);
        spinner6 = (Spinner) findViewById(R.id.spinner6);

        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        arrayListLampNames.clear();

        for (int i=1; i<=iCounter; i++) {
            String sKey = "preset" + i + "_name";
            if (json_analyst.getJSONValue(sKey).length() > 0) {
                arrayListLampNames.add(json_analyst.getJSONValue(sKey));
            } else {
                //arrayListLampNames.add("BUTTON " + i);
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
        this.setTitle(getString(R.string.app_header_title) + " assign buttons");


    }

    public void store_assignment (View v) {
        if (validate_assignments()) {
            makeToast("Lamp presets assigned successfully.");

            SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_LAMP_ASSIGNMENTS, 0);
            SharedPreferences.Editor edit = spFile.edit();
            edit.clear();
            edit.commit();


            //makeToast("Spinner1 text size: " + spinner1.getSelectedItem().toString().length());
            //SystemClock.sleep(15000);
            if (spinner1.getSelectedItem().toString().length() > 0) { edit.putString("1", spinner1.getSelectedItem().toString()); }
            if (spinner2.getSelectedItem().toString().length() > 0) { edit.putString("2", spinner2.getSelectedItem().toString()); }
            if (spinner3.getSelectedItem().toString().length() > 0) { edit.putString("3", spinner3.getSelectedItem().toString()); }
            if (spinner4.getSelectedItem().toString().length() > 0) { edit.putString("4", spinner4.getSelectedItem().toString()); }
            if (spinner5.getSelectedItem().toString().length() > 0) { edit.putString("5", spinner5.getSelectedItem().toString()); }
            if (spinner6.getSelectedItem().toString().length() > 0) { edit.putString("6", spinner6.getSelectedItem().toString()); }
            edit.putString("666", String.valueOf(System.currentTimeMillis()));
            edit.commit();

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

        Set<String> deduped = new HashSet<String>(stringSpinnerValues);

        if (deduped.size() != stringSpinnerValues.size()) {
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
