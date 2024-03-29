package com.morris.LEDbar_controller;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import static com.morris.LEDbar_controller.LightSettings.sNewLine;
import static com.morris.LEDbar_controller.TRSDigitalPanel.NO_PRESET_TEXT;
import static com.morris.LEDbar_controller.TRSDigitalPanel.SHAREDPREFS_LAMP_ASSIGNMENTS;
import static com.morris.LEDbar_controller.TRSDigitalPanel.TL84_TAG;
import static com.morris.LEDbar_controller.TRSSettings.TL84_DELAY_KEY;


public class TRSSequence_old extends Activity {

    public static final String SP_LAMP_TIMERS = "sequence_lamp_timers"; //Mauricio
    public static final String SP_SEQUENCE_COMMAND_GENERATE = "sequence_command_generated"; //Mauricio
    public static final String PRESETS_DEFINITION = "presets_definition"; //Mauricio
    public static final String TAG = "MORRIS-TRSSEQ";
    boolean blLamp1_ON, blLamp2_ON, blLamp3_ON, blLamp4_ON, blLamp5_ON, blLamp6_ON;
    Button btnL1, btnL2, btnL3, btnL4, btnL5, btnL6;
    Integer btn1Timer = 0;
    Integer btn2Timer = 0;
    Integer btn3Timer = 0;
    Integer btn4Timer = 0;
    Integer btn5Timer = 0;
    Integer btn6Timer = 0;

    boolean mBound = false;
    final Context context = this;


    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            UsbCOMMsService.UsbBinder binder = (UsbCOMMsService.UsbBinder) service;
            UsbCOMMsService lclUsbServiceInstance = binder.getService();
            mBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    protected void onResume()
    {
        super.onResume();
        //Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();
        if (!mBound) {
            Intent intent = new Intent(this, UsbCOMMsService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            //Toast.makeText(this.getBaseContext(),"Service bound (onResume)", Toast.LENGTH_SHORT).show();
            mBound = true;
        }

        populateLampsState();
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
        unbindService(mConnection);
        mBound = false;
        //Toast.makeText(this.getBaseContext(),"Activity stopped", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"Service unbound", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();
        finish();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.trs_sequence_page_old);
        setContentView(R.layout.trs_sequence_page_ordered);

        //Handler lclHandler = new Handler();

        btnL1 = findViewById(R.id.btnL1);
        btnL2 = findViewById(R.id.btnL2);
        btnL3 = findViewById(R.id.btnL3);
        btnL4 = findViewById(R.id.btnL4);
        btnL5 = findViewById(R.id.btnL5);
        btnL6 = findViewById(R.id.btnL6);


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


        final CheckedTextView checkedTextRead = findViewById(R.id.checkedTextRead);
        if (checkedTextRead != null) {
            checkedTextRead.setChecked(false);
            //checkedTextRead.setCheckMarkDrawable(android.R.drawable.checkbox_off_background);

            checkedTextRead.setOnClickListener(v -> {
                checkedTextRead.setChecked(!checkedTextRead.isChecked());
                //checkedTextRead.setCheckMarkDrawable(checkedTextRead.isChecked() ? android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background);

                String msg = getString(R.string.pre_msg) + " " + (checkedTextRead.isChecked() ? getString(R.string.checked) : getString(R.string.unchecked));
                Toast.makeText(TRSSequence_old.this, msg, Toast.LENGTH_SHORT).show();
            });
        }

        SharedPreferences prefs = getSharedPreferences(SP_SEQUENCE_COMMAND_GENERATE, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        /*prefs = getSharedPreferences(SP_LAMP_TIMERS, 0);
        editor = prefs.edit();
        editor.clear();
        editor.commit();*/

        final CheckedTextView checkedTextSend = findViewById(R.id.checkedTextSend);
        if (checkedTextSend != null) {
            checkedTextSend.setChecked(false);
            //checkedTextSend.setCheckMarkDrawable(android.R.drawable.checkbox_off_background);

            checkedTextSend.setOnClickListener(v -> {
                checkedTextSend.setChecked(!checkedTextSend.isChecked());
                //checkedTextSend.setCheckMarkDrawable(checkedTextSend.isChecked() ? android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background);

                String msg = getString(R.string.pre_msg) + " " + (checkedTextSend.isChecked() ? getString(R.string.checked) : getString(R.string.unchecked));
                Toast.makeText(TRSSequence_old.this, msg, Toast.LENGTH_SHORT).show();
            });
        }


        btnL1.setOnLongClickListener(arg0 -> {

            btn1Timer = 0;
            btnL1.setBackgroundResource(R.drawable.buttonselector_main);
            btnL1.setTextColor(Color.WHITE);
            String sButtonCaption = btnL1.getTag().toString();
            btnL1.setText(sButtonCaption);
            SharedPreferences prefs1 = getSharedPreferences(SP_LAMP_TIMERS, 0);
            SharedPreferences.Editor editor1 = prefs1.edit();
            editor1.remove(sButtonCaption);
            makeToast("resetting timer of " + sButtonCaption);
            editor1.apply();
            return true;
        });

        btnL2.setOnLongClickListener(arg0 -> {

            btn2Timer = 0;
            btnL2.setBackgroundResource(R.drawable.buttonselector_main);
            btnL2.setTextColor(Color.WHITE);
            String sButtonCaption = btnL2.getTag().toString();
            btnL2.setText(sButtonCaption);
            SharedPreferences prefs12 = getSharedPreferences(SP_LAMP_TIMERS, 0);
            SharedPreferences.Editor editor12 = prefs12.edit();
            editor12.remove(sButtonCaption);
            makeToast("resetting timer of " + sButtonCaption);
            editor12.apply();

            return true;
        });

        btnL3.setOnLongClickListener(arg0 -> {

            btn3Timer = 0;
            btnL3.setBackgroundResource(R.drawable.buttonselector_main);
            btnL3.setTextColor(Color.WHITE);
            String sButtonCaption = btnL3.getTag().toString();
            //sButtonCaption = sButtonCaption + sNewLine + btn3Timer*5 + " sec";
            btnL3.setText(sButtonCaption);
            SharedPreferences prefs13 = getSharedPreferences(SP_LAMP_TIMERS, 0);
            SharedPreferences.Editor editor13 = prefs13.edit();
            editor13.remove(sButtonCaption);
            makeToast("resetting timer of " + sButtonCaption);
            editor13.apply();

            return true;
        });

        btnL4.setOnLongClickListener(arg0 -> {

            btn4Timer = 0;
            btnL4.setBackgroundResource(R.drawable.buttonselector_main);
            btnL4.setTextColor(Color.WHITE);
            String sButtonCaption = btnL4.getTag().toString();
            //sButtonCaption = sButtonCaption + sNewLine + btn4Timer*5 + " sec";
            btnL4.setText(sButtonCaption);
            SharedPreferences prefs14 = getSharedPreferences(SP_LAMP_TIMERS, 0);
            SharedPreferences.Editor editor14 = prefs14.edit();
            editor14.remove(sButtonCaption);
            makeToast("resetting timer of " + sButtonCaption);
            editor14.apply();

            return true;
        });

        btnL5.setOnLongClickListener(arg0 -> {

            btn5Timer = 0;
            btnL5.setBackgroundResource(R.drawable.buttonselector_main);
            btnL5.setTextColor(Color.WHITE);
            String sButtonCaption = btnL5.getTag().toString();
            //sButtonCaption = sButtonCaption + sNewLine + btn5Timer*5 + " sec";
            btnL5.setText(sButtonCaption);
            SharedPreferences prefs15 = getSharedPreferences(SP_LAMP_TIMERS, 0);
            SharedPreferences.Editor editor15 = prefs15.edit();
            editor15.remove(sButtonCaption);
            makeToast("resetting timer of " + sButtonCaption);
            editor15.apply();

            return true;
        });

        btnL6.setOnLongClickListener(arg0 -> {

            btn6Timer = 0;
            btnL6.setBackgroundResource(R.drawable.buttonselector_main);
            btnL6.setTextColor(Color.WHITE);
            String sButtonCaption = btnL6.getTag().toString();
            //sButtonCaption = sButtonCaption + sNewLine + btn6Timer*5 + " sec";
            btnL6.setText(sButtonCaption);
            SharedPreferences prefs16 = getSharedPreferences(SP_LAMP_TIMERS, 0);
            SharedPreferences.Editor editor16 = prefs16.edit();
            editor16.remove(sButtonCaption);
            makeToast("resetting timer of " + sButtonCaption);
            editor16.apply();

            return true;
        });

        if (shared_prefs_exists(SHAREDPREFS_LAMP_ASSIGNMENTS, "666")) {
            repopulate_button_assignments();
            Log.d(TAG, "Repopulating button captions");
        } else {
            Log.d(TAG, "Key 666 not found. Populating button names from JSON");
            populateButtonNames();
        }
        invokeLampSequence();
    }

    public boolean shared_prefs_exists(String sFileName, String sKey) {
        SharedPreferences spFile = getSharedPreferences(sFileName, 0);
        Log.d(TAG, "Shared_prefs_exists (" + sFileName+ "): " + spFile.contains(sKey));
        return spFile.contains(sKey);
    }

    public void writeSequenceToFile(String sLampName, Integer iTimeToDisplay, String seq_id) {

        SharedPreferences prefs = getSharedPreferences(SP_SEQUENCE_COMMAND_GENERATE, 0);
        SharedPreferences.Editor editor = prefs.edit();

        JSONArray jsonArray = new JSONArray();
        jsonArray.put(sLampName);
        jsonArray.put(iTimeToDisplay);
        Log.d(TAG, "Writing '" + jsonArray + "' to Sequence file under id: '" + seq_id + "'");
        editor.putString(seq_id, jsonArray.toString());
        editor.apply();
    }

    /*public void readSequenceFromFile() {
        SharedPreferences prefs = getSharedPreferences(SP_SEQUENCE_COMMAND_GENERATE, 0);

        try {
            JSONArray jsonArray2 = new JSONArray(prefs.getString("seq_id", "[]"));
            for (int i = 0; i < jsonArray2.length(); i++) {
                Log.d("your JSON Array", jsonArray2.getInt(i)+"");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public void resetSequenceCommand(View v) {
        SharedPreferences spLampTimers = getSharedPreferences(SP_LAMP_TIMERS, 0);
        SharedPreferences.Editor editor = spLampTimers.edit();
        editor.clear();
        editor.apply();

        btnL1.setBackgroundResource(R.drawable.buttonselector_main);
        btnL1.setTextColor(Color.WHITE);
        btnL1.setText(btnL1.getTag().toString());

        btnL2.setBackgroundResource(R.drawable.buttonselector_main);
        btnL2.setTextColor(Color.WHITE);
        btnL2.setText(btnL2.getTag().toString());

        btnL3.setBackgroundResource(R.drawable.buttonselector_main);
        btnL3.setTextColor(Color.WHITE);
        btnL3.setText(btnL3.getTag().toString());

        btnL4.setBackgroundResource(R.drawable.buttonselector_main);
        btnL4.setTextColor(Color.WHITE);
        btnL4.setText(btnL4.getTag().toString());

        btnL5.setBackgroundResource(R.drawable.buttonselector_main);
        btnL5.setTextColor(Color.WHITE);
        btnL5.setText(btnL5.getTag().toString());

        btnL6.setBackgroundResource(R.drawable.buttonselector_main);
        btnL6.setTextColor(Color.WHITE);
        btnL6.setText(btnL6.getTag().toString());

        btn1Timer = btn2Timer = btn3Timer = btn4Timer = btn5Timer = btn6Timer = 0;
    }
    public void generateSequenceCommand(View v) {

        Date dteNow = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String sTimeStamp = sdf.format(dteNow);
        String sSequence = "D";
        String sPresetName = "";

        SharedPreferences spControlerFileimage = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        SharedPreferences spLampTimers = getSharedPreferences(SP_LAMP_TIMERS, 0);

        String sLamp1Timers = spLampTimers.getString("lamp1_timer", "0");
        String sLamp2Timers = spLampTimers.getString("lamp2_timer", "0");
        String sLamp3Timers = spLampTimers.getString("lamp3_timer", "0");
        String sLamp4Timers = spLampTimers.getString("lamp4_timer", "0");
        String sLamp5Timers = spLampTimers.getString("lamp5_timer", "0");
        String sLamp6Timers = spLampTimers.getString("lamp6_timer", "0");

        JSON_analyst json_analyst = new JSON_analyst(spControlerFileimage);
        if (Integer.valueOf(sLamp1Timers) > 0) {
            sPresetName = json_analyst.getJSONValue("preset1_name");
            writeSequenceToFile(sPresetName, Integer.valueOf(sLamp1Timers), "1");
        }

        //sPresetRGBValues = json_analyst.getJSONValue("preset2_rgbw");
        if (Integer.valueOf(sLamp2Timers) > 0) {
            sPresetName = json_analyst.getJSONValue("preset2_name");
            writeSequenceToFile(sPresetName, Integer.valueOf(sLamp2Timers), "2");
        }

        if (Integer.valueOf(sLamp3Timers) > 0) {
            sPresetName = json_analyst.getJSONValue("preset3_name");
            writeSequenceToFile(sPresetName, Integer.valueOf(sLamp3Timers), "3");
        }

        if (Integer.valueOf(sLamp4Timers) > 0) {
            sPresetName = json_analyst.getJSONValue("preset4_name");
            writeSequenceToFile(sPresetName, Integer.valueOf(sLamp4Timers), "4");
        }

        if (Integer.valueOf(sLamp5Timers) > 0) {
            sPresetName = json_analyst.getJSONValue("preset5_name");
            writeSequenceToFile(sPresetName, Integer.valueOf(sLamp5Timers), "5");
        }

        if (Integer.valueOf(sLamp6Timers) > 0) {
            sPresetName = json_analyst.getJSONValue("preset6_name");
            writeSequenceToFile(sPresetName, Integer.valueOf(sLamp6Timers), "6");
        }

        if (get_prefs_contents_size(SP_LAMP_TIMERS) > 0) {
            Toast.makeText(this, "Sequence data stored on mobile device.\nPress PRG on main panel to run", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Nothing to be saved", Toast.LENGTH_SHORT).show();
        }
    }

    private int get_prefs_contents_size(String prefs_file_name) {
        int iSize=0;
        SharedPreferences prefs = getSharedPreferences(prefs_file_name, MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            iSize++;
        }
        return iSize;
    }

    public void populateButtonNames() {
        //String sUnitName = "";
        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        final String sLamp1Name = json_analyst.getJSONValue("preset1_name");
        final String sLamp2Name = json_analyst.getJSONValue("preset2_name");
        final String sLamp3Name = json_analyst.getJSONValue("preset3_name");
        final String sLamp4Name = json_analyst.getJSONValue("preset4_name");
        final String sLamp5Name = json_analyst.getJSONValue("preset5_name");
        final String sLamp6Name = json_analyst.getJSONValue("preset6_name");

        setLampName(1, sLamp1Name);
        setLampName(2, sLamp2Name);
        setLampName(3, sLamp3Name);
        setLampName(4, sLamp4Name);
        setLampName(5, sLamp5Name);
        setLampName(6, sLamp6Name);
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

    public void updateLampHEXsequence (String sKey, String sSequence) {
        SharedPreferences spLampTimers = getSharedPreferences(SP_LAMP_TIMERS, 0);
        SharedPreferences.Editor editorLampTimer = spLampTimers.edit();

        editorLampTimer.remove(sKey);
        editorLampTimer.putString(sKey, sSequence);
        editorLampTimer.commit();
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

    public String getRGBValues(String sPresetName) {
        String sPresetRGB = "";

        SharedPreferences spsValues = getSharedPreferences(PRESETS_DEFINITION, 0);
        Map<String, ?> allEntries = spsValues.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String sVal = entry.getKey();
            if (sVal.equalsIgnoreCase(sPresetName)) {
                String sValue = entry.getValue().toString();
                sPresetRGB = convertRGBwithCommasToHexString(sValue);
            }
         }
        return sPresetRGB;
    }

    private void invokeLampSequence() {
        String sLampTag, sTimer, sBuffer;
        SharedPreferences spFile = getSharedPreferences(SP_LAMP_TIMERS, 0);


        sLampTag = btnL1.getTag().toString();
        sBuffer = spFile.getString(sLampTag, "");
        if (!sBuffer.isEmpty()) {
            String[] value = sBuffer.split(",");
            sBuffer = value[1];
            sTimer = sBuffer.substring(sBuffer.length() - 8, sBuffer.length() - 6);
            sTimer = sTimer.replaceFirst("^0+(?!$)", "");
            btn1Timer = Integer.valueOf(sTimer)/5;
            sTimer += " SEC";
            btnL1.setText(btnL1.getTag().toString() + "\n" + sTimer);
        }

        sLampTag = btnL2.getTag().toString();
        sBuffer = spFile.getString(sLampTag, "");
        if (!sBuffer.isEmpty()) {
            String[] value = sBuffer.split(",");
            sBuffer = value[1];
            sTimer = sBuffer.substring(sBuffer.length() - 8, sBuffer.length() - 6);
            sTimer = sTimer.replaceFirst("^0+(?!$)", "");
            btn2Timer = Integer.valueOf(sTimer)/5;
            sTimer += " SEC";
            btnL2.setText(btnL2.getTag().toString() + "\n" + sTimer);
        }

        sLampTag = btnL3.getTag().toString();
        sBuffer = spFile.getString(sLampTag, "");
        if (!sBuffer.isEmpty()) {
            String[] value = sBuffer.split(",");
            sBuffer = value[1];
            sTimer = sBuffer.substring(sBuffer.length() - 8, sBuffer.length() - 6);
            sTimer = sTimer.replaceFirst("^0+(?!$)", "");
            btn3Timer = Integer.valueOf(sTimer)/5;
            sTimer += " SEC";
            btnL3.setText(btnL3.getTag().toString() + "\n" + sTimer);
        }

        sLampTag = btnL4.getTag().toString();
        sBuffer = spFile.getString(sLampTag, "");
        if (!sBuffer.isEmpty()) {
            String[] value = sBuffer.split(",");
            sBuffer = value[1];
            sTimer = sBuffer.substring(sBuffer.length() - 8, sBuffer.length() - 6);
            sTimer = sTimer.replaceFirst("^0+(?!$)", "");
            btn4Timer = Integer.valueOf(sTimer)/5;
            sTimer += " SEC";
            btnL4.setText(btnL4.getTag().toString() + "\n" + sTimer);
        }

        sLampTag = btnL5.getTag().toString();
        sBuffer = spFile.getString(sLampTag, "");
        if (!sBuffer.isEmpty()) {
            String[] value = sBuffer.split(",");
            sBuffer = value[1];
            sTimer = sBuffer.substring(sBuffer.length() - 8, sBuffer.length() - 6);
            sTimer = sTimer.replaceFirst("^0+(?!$)", "");
            btn5Timer = Integer.valueOf(sTimer)/5;
            sTimer += " SEC";
            btnL5.setText(btnL5.getTag().toString() + "\n" + sTimer);
        }

        sLampTag = btnL6.getTag().toString();
        sBuffer = spFile.getString(sLampTag, "");
        if (!sBuffer.isEmpty()) {
            String[] value = sBuffer.split(",");
            sBuffer = value[1];
            sTimer = sBuffer.substring(sBuffer.length() - 8, sBuffer.length() - 6);
            sTimer = sTimer.replaceFirst("^0+(?!$)", "");
            btn6Timer = Integer.valueOf(sTimer)/5;
            sTimer += " SEC";
            btnL6.setText(btnL6.getTag().toString() + "\n" + sTimer);
        }
    }

    public String get_tl84_delay() {
        SharedPreferences config_prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, 0);
        Log.d(TAG, " ** TL84_delay from file: " + String.format(Locale.US, "%04d", config_prefs.getInt(TL84_DELAY_KEY, 0)));
        return String.format(Locale.US, "%04d", config_prefs.getInt(TL84_DELAY_KEY, 0));

    }


    public void btnClicked(View v) {

        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        String sHEX = "";
        String sButtonCaption = "";

        switch_all_off();
        String sDelay = "0000";

        switch (v.getId()) {
            case R.id.btnL1:

                if (!btnL1.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
                    btn1Timer++;
                    blLamp1_ON = true;
                    btnL1.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL1.setTextColor(Color.BLACK);
                    sButtonCaption = btnL1.getTag().toString();
                    sButtonCaption = sButtonCaption + sNewLine + btn1Timer*5 + " sec";
                    sHEX = getRGBValues(btnL1.getTag().toString());
                    sHEX = sHEX.concat(String.format("%04d", (btn1Timer*5)));
                    int i_flag_TL84 = 0;
                    if (btnL1.getTag().toString().equalsIgnoreCase(TL84_TAG)) {
                        i_flag_TL84 = 1;
                        sDelay = get_tl84_delay();

                    }

                    //makeToast(sHEX);
                    updateLampHEXsequence(btnL1.getTag().toString(), System.currentTimeMillis() + "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00") + sDelay);
                    //updateLampHEXsequence("lamp1_timer", System.currentTimeMillis() + "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00"));

                    btnL1.setText(sButtonCaption);
                }
                break;

            case R.id.btnL2:

                if (!btnL2.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
                    btn2Timer++;
                    blLamp2_ON = true;
                    btnL2.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL2.setTextColor(Color.BLACK);

                    sButtonCaption = btnL2.getTag().toString();
                    sButtonCaption = sButtonCaption + sNewLine + btn2Timer * 5 + " sec";

                    sHEX = getRGBValues(btnL2.getTag().toString());
                    sHEX = sHEX.concat(String.format("%04d", (btn2Timer * 5)));
                    int i_flag_TL84 = 0;
                    if (btnL2.getTag().toString().equalsIgnoreCase(TL84_TAG)) {
                        i_flag_TL84 = 1;
                        sDelay = get_tl84_delay();
                    }
                    updateLampHEXsequence(btnL2.getTag().toString(), System.currentTimeMillis() + "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00") + sDelay);
                    //updateLampHEXsequence("lamp2_timer", System.currentTimeMillis() + "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00"));

                    btnL2.setText(sButtonCaption);
                }
                break;

            case R.id.btnL3:

                if (!btnL3.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
                    btn3Timer++;
                    blLamp3_ON = true;
                    btnL3.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL3.setTextColor(Color.BLACK);

                    sHEX = getRGBValues(btnL3.getTag().toString());
                    sHEX = sHEX.concat(String.format("%04d", (btn3Timer * 5)));
                    int i_flag_TL84 = 0;
                    if (btnL3.getTag().toString().equalsIgnoreCase(TL84_TAG)) {
                        i_flag_TL84 = 1;
                        sDelay = get_tl84_delay();
                    }
                    updateLampHEXsequence(btnL3.getTag().toString(), System.currentTimeMillis() + "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00") + sDelay);
                    //updateLampHEXsequence("lamp3_timer", System.currentTimeMillis() + "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00"));

                    sButtonCaption = btnL3.getTag().toString();
                    sButtonCaption = sButtonCaption + sNewLine + btn3Timer * 5 + " sec";

                    btnL3.setText(sButtonCaption);
                }
                break;

            case R.id.btnL4:

                if (!btnL4.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
                    btn4Timer++;
                    blLamp4_ON = true;
                    btnL4.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL4.setTextColor(Color.BLACK);

                    sHEX = getRGBValues(btnL4.getTag().toString());
                    sHEX = sHEX.concat(String.format("%04d", (btn4Timer * 5)));
                    int i_flag_TL84 = 0;
                    if (btnL4.getTag().toString().equalsIgnoreCase(TL84_TAG)) {
                        i_flag_TL84 = 1;
                        sDelay = get_tl84_delay();
                    }
                    updateLampHEXsequence(btnL4.getTag().toString(), System.currentTimeMillis() + "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00") + sDelay);
                    //updateLampHEXsequence("lamp4_timer", System.currentTimeMillis() + "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00"));

                    sButtonCaption = btnL4.getTag().toString();
                    sButtonCaption = sButtonCaption + sNewLine + btn4Timer * 5 + " sec";

                    btnL4.setText(sButtonCaption);
                }
                break;

            case R.id.btnL5:

                if (!btnL5.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
                    btn5Timer++;
                    blLamp5_ON = true;
                    btnL5.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL5.setTextColor(Color.BLACK);

                    sHEX = getRGBValues(btnL5.getTag().toString());
                    sHEX = sHEX.concat(String.format("%04d", (btn5Timer * 5)));
                    int i_flag_TL84 = 0;
                    if (btnL5.getTag().toString().equalsIgnoreCase(TL84_TAG)) {
                        i_flag_TL84 = 1;
                        sDelay = get_tl84_delay();
                    }
                    updateLampHEXsequence(btnL5.getTag().toString(), System.currentTimeMillis() + "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00") + sDelay);
                    //updateLampHEXsequence("lamp5_timer", System.currentTimeMillis() + "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00"));

                    sButtonCaption = btnL5.getTag().toString();
                    sButtonCaption = sButtonCaption + sNewLine + btn5Timer * 5 + " sec";

                    btnL5.setText(sButtonCaption);
                }
                break;

            case R.id.btnL6:

                if (!btnL6.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {

                    btn6Timer++;
                    blLamp6_ON = true;
                    btnL6.setBackgroundResource(R.drawable.buttonselector_active);
                    btnL6.setTextColor(Color.BLACK);

                    sHEX = getRGBValues(btnL6.getTag().toString());
                    sHEX = sHEX.concat(String.format("%04d", (btn6Timer * 5)));
                    int i_flag_TL84 = 0;
                    if (btnL6.getTag().toString().equalsIgnoreCase(TL84_TAG)) {
                        i_flag_TL84 = 1;
                        sDelay = get_tl84_delay();
                    }
                    updateLampHEXsequence(btnL6.getTag().toString(), System.currentTimeMillis() + "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00") + sDelay);
                    //updateLampHEXsequence("lamp6_timer", System.currentTimeMillis() + "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00"));

                    sButtonCaption = btnL6.getTag().toString();
                    sButtonCaption = sButtonCaption + sNewLine + btn6Timer * 5 + " sec";

                    btnL6.setText(sButtonCaption);
                }

                break;
        }
    }

    public void switch_all_off() {

        blLamp1_ON = false;
        btnL1.setBackgroundResource(R.drawable.buttonselector_main);
        btnL1.setTextColor(Color.WHITE);
        blLamp2_ON = false;
        btnL2.setBackgroundResource(R.drawable.buttonselector_main);
        //btnL2.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL2.setTextColor(Color.WHITE);
        blLamp3_ON = false;
        btnL3.setBackgroundResource(R.drawable.buttonselector_main);
        //btnL3.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL3.setTextColor(Color.WHITE);
        blLamp4_ON = false;
        btnL4.setBackgroundResource(R.drawable.buttonselector_main);
        //btnL4.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL4.setTextColor(Color.WHITE);
        blLamp5_ON = false;
        btnL5.setBackgroundResource(R.drawable.buttonselector_main);
        //btnL5.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL5.setTextColor(Color.WHITE);
        blLamp6_ON = false;
        btnL6.setBackgroundResource(R.drawable.buttonselector_main);
        //btnL6.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL6.setTextColor(Color.WHITE);
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
