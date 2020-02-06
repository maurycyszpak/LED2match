package com.kiand.LED2match;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import static com.kiand.LED2match.LightAdjustments.SHAREDPREFS_CONTROLLER_FILEIMAGE;
import static com.kiand.LED2match.LightAdjustments.sNewLine;
import static com.kiand.LED2match.TRSDigitalPanel.NO_PRESET_TEXT;
import static com.kiand.LED2match.TRSDigitalPanel.SHAREDPREFS_LAMP_ASSIGNMENTS;

public class TRSLightOperatingHours extends Activity {

    public static final String SHAREDPREFS_ONE_OFF_SEEKBARS = "one-off-seekbar-values.txt"; //Mauricio
    public static final String SHAREDPREFS_LED_TIMERS = "led_timers"; //Mauricio
    public static final String newLine = System.getProperty("line.separator");
    public static final String NO_PRESET_TEXT = "#n/a";
    private BtCOMMsService lclBTServiceInstance;

    boolean blLamp1_ON, blLamp2_ON, blLamp3_ON, blLamp4_ON, blLamp5_ON, blLamp6_ON;
    Button btnL1, btnL2, btnL3, btnL4, btnL5, btnL6;
    Integer btn1Timer = 0;
    Integer btn2Timer = 0;
    Integer btn3Timer = 0;
    Integer btn4Timer = 0;
    Integer btn5Timer = 0;
    Integer btn6Timer = 0;
    String sEmptyTimerValue = "00:00";
    public final String TAG = "MORRIS-LED-HOURS";

    private Handler lclHandler;
    private UsbCOMMsService lclUsbServiceInstance;
    boolean mBound = false;
    boolean mBoundBT = false;
    final Context context = this;

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

    public String getSystemTime() {
        Date currentTime = Calendar.getInstance().getTime();
        String sTimeNow = DateFormat.format("HH:mm:ss", currentTime).toString();
        return sTimeNow;

    }

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
        if (!mBoundBT) {
            Intent intent = new Intent(this, BtCOMMsService.class);
            bindService(intent, btConnection, Context.BIND_AUTO_CREATE);
            //Toast.makeText(this.getBaseContext(),"Service bound (onResume)", Toast.LENGTH_SHORT).show();
            mBoundBT = true;
            Log.d(TAG, "BT service bound");
        }

        if (mBoundBT) {
            try {
                String sSequence = "J";
                sSequence = sSequence.concat(System.lineSeparator());
                lclBTServiceInstance.sendData(sSequence);
                Log.d(TAG, "Requesting timers via commmand J");
            } catch (NullPointerException e) {
                Log.e(TAG, "NullPointerException when sending command via Bluetooth");
            }
        }
        SystemClock.sleep(1500);
        populate_button_timers();

        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        String sFWVersion = json_analyst.getJSONValue("firmware_version");
        TextView tvInfoBox = findViewById(R.id.infobox);
        String version_line = "FW Version: " + sFWVersion;
        tvInfoBox.setText(getString(R.string.system_footer) + "\n" + version_line);
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
        setContentView(R.layout.trs_operatingtime_page);

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
    }

    public void populate_button_timers_empty() {
        if (!btnL1.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            btnL1.setText(btnL1.getTag().toString() + System.lineSeparator() + sEmptyTimerValue);
        }

        if (!btnL2.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            btnL2.setText(btnL2.getTag().toString() + System.lineSeparator() + sEmptyTimerValue);
        }

        if (!btnL3.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            btnL3.setText(btnL3.getTag().toString() + System.lineSeparator() + sEmptyTimerValue);
        }

        if (!btnL4.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            btnL4.setText(btnL4.getTag().toString() + System.lineSeparator() + sEmptyTimerValue);
        }

        if (!btnL5.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            btnL5.setText(btnL5.getTag().toString() + System.lineSeparator() + sEmptyTimerValue);
        }

        if (!btnL6.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
            btnL6.setText(btnL6.getTag().toString() + System.lineSeparator() + sEmptyTimerValue);
        }
    }

    public void populate_button_timers() {

        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_LED_TIMERS, 0);
        String sCounters = spFile.getString("currTimers", "");
        if (sCounters.length() > 0) {
            String sArray[] = sCounters.split("\\|");
            for (String sPiece: sArray) {
                if (sPiece.contains(",")) {
                    String sTimers[] = sPiece.split(",");

                    try {

                        if (!btnL1.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
                            btnL1.setText(btnL1.getTag().toString() + System.lineSeparator() + convert_secs_to_hhmm(sTimers[0]));
                        }

                        if (!btnL2.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
                            btnL2.setText(btnL2.getTag().toString() + System.lineSeparator() + convert_secs_to_hhmm(sTimers[1]));
                        }

                        if (!btnL3.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
                            btnL3.setText(btnL3.getTag().toString() + System.lineSeparator() + convert_secs_to_hhmm(sTimers[2]));
                        }

                        if (!btnL4.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
                            btnL4.setText(btnL4.getTag().toString() + System.lineSeparator() + convert_secs_to_hhmm(sTimers[3]));
                        }

                        if (!btnL5.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
                            btnL5.setText(btnL5.getTag().toString() + System.lineSeparator() + convert_secs_to_hhmm(sTimers[4]));
                        }

                        if (!btnL6.getTag().toString().equalsIgnoreCase(NO_PRESET_TEXT)) {
                            btnL6.setText(btnL6.getTag().toString() + System.lineSeparator() + convert_secs_to_hhmm(sTimers[5]));
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        makeToast("Unable to read the LED timers from file");
                        Log.d(TAG, "Unable to split by comma: '" + sPiece + "'.");
                    }
                }
            }
        }
    }
    private String convert_secs_to_hhmm (String sSeconds) {
        Long lSecs = Long.valueOf(sSeconds);
        Integer iHours = lSecs.intValue()/60/60;
        Integer iMinutes = (lSecs.intValue() - iHours*60*60)/60;

        String sResult = String.format("%02d", iHours) + ":" + String.format("%02d", iMinutes);
        return sResult;
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

    public void onClickBack (View v) {
        finish();
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

    public void populateButtonNames() {
        //String sUnitName = "";
        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        //Log.d(TAG, "bluetoothAskReply(V1)");

        final String sLamp1Name = json_analyst.getJSONValue("preset1_name") + sNewLine + json_analyst.getJSONValue("counter1");
        final String sLamp2Name = json_analyst.getJSONValue("preset2_name") + sNewLine + json_analyst.getJSONValue("counter2");
        final String sLamp3Name = json_analyst.getJSONValue("preset3_name") + sNewLine + json_analyst.getJSONValue("counter3");
        final String sLamp4Name = json_analyst.getJSONValue("preset4_name") + sNewLine + json_analyst.getJSONValue("counter4");
        final String sLamp5Name = json_analyst.getJSONValue("preset5_name") + sNewLine + json_analyst.getJSONValue("counter5");
        final String sLamp6Name = json_analyst.getJSONValue("preset6_name") + sNewLine + json_analyst.getJSONValue("counter6");

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
            } else {
                btnL1.setText(NO_PRESET_TEXT);
            }
        } else if (i == 2) {
            if (sName.length() > 0) {
                btnL2.setText(sName);
            } else {
                btnL2.setText(NO_PRESET_TEXT);
            }
        } else if (i == 3) {
            if (sName.length() > 0) {
                btnL3.setText(sName);
            } else {
                btnL3.setText(NO_PRESET_TEXT);
            }
        } else if (i == 4) {
            if (sName.length() > 0) {
                btnL4.setText(sName);
            } else {
                btnL4.setText(NO_PRESET_TEXT);
            }
        } else if (i == 5) {
            if (sName.length() > 0) {
                btnL5.setText(sName);
            } else {
                btnL5.setText(NO_PRESET_TEXT);
            }
        } else if (i == 6) {
            if (sName.length() > 0) {
                btnL6.setText(sName);
            } else {
                btnL6.setText(NO_PRESET_TEXT);
            }
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




    /*public void btnClicked(View v) {

        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        String sPresetRGBValues = "0,0,0,0,0,0,0,0,0,0";
        String sCommand = "";
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        switch_all_off();

        switch (v.getId()) {
            case R.id.btnL1:

                btn1Timer++;
                blLamp1_ON = true;
                btnL1.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL1.setTextColor(Color.BLACK);
//                String sButtonCaption = btnL1.getTag().toString();
//                sButtonCaption = sButtonCaption + sNewLine + btn1Timer*5 + " sec";
//                btnL1.setText(sButtonCaption);

                sPresetRGBValues = json_analyst.getJSONValue("preset1_rgbw");
                String[] sRGB = sPresetRGBValues.split(",");
                for (int i=0; i < sRGB.length; i++) {
                    if (i < 9) {
                        sCommand = "S0" + (i+1) + sRGB[i] + newLine;
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    } else {
                        sCommand = "S" + (i+1) + sRGB[i] + newLine;
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    }
                    //Toast.makeText(context, sCommand, Toast.LENGTH_LONG).show();
                }

                break;

            case R.id.btnL2:

                btn2Timer++;
                blLamp2_ON = true;
                btnL2.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL2.setTextColor(Color.BLACK);

//                sButtonCaption = btnL2.getTag().toString();
//                sButtonCaption = sButtonCaption + sNewLine + btn2Timer*5 + " sec";
//                btnL2.setText(sButtonCaption);


                sPresetRGBValues = json_analyst.getJSONValue("preset2_rgbw");
                sRGB = sPresetRGBValues.split(",");
                for (int i=0; i < sRGB.length; i++) {
                    if (i < 9) {
                        sCommand = "S0" + (i+1) + sRGB[i] + newLine;
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    } else {
                        sCommand = "S" + (i+1) + sRGB[i] + newLine;
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    }
                    //Toast.makeText(context, sCommand, Toast.LENGTH_LONG).show();
                }

                break;

            case R.id.btnL3:

                btn3Timer++;
                blLamp3_ON = true;
                btnL3.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL3.setTextColor(Color.BLACK);

//                sButtonCaption = btnL3.getTag().toString();
//                sButtonCaption = sButtonCaption + sNewLine + btn3Timer*5 + " sec";
//                btnL3.setText(sButtonCaption);

                sPresetRGBValues = json_analyst.getJSONValue("preset3_rgbw");
                sRGB = sPresetRGBValues.split(",");
                for (int i=0; i < sRGB.length; i++) {
                    if (i < 9) {
                        sCommand = "S0" + (i+1) + sRGB[i] + newLine;
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    } else {
                        sCommand = "S" + (i+1) + sRGB[i] + newLine;
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    }
                    //Toast.makeText(context, sCommand, Toast.LENGTH_LONG).show();
                }

                break;

            case R.id.btnL4:

                btn4Timer++;
                blLamp4_ON = true;
                btnL4.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL4.setTextColor(Color.BLACK);

//                sButtonCaption = btnL4.getTag().toString();
//                sButtonCaption = sButtonCaption + sNewLine + btn4Timer*5 + " sec";
//                btnL4.setText(sButtonCaption);

                sPresetRGBValues = json_analyst.getJSONValue("preset4_rgbw");
                sRGB = sPresetRGBValues.split(",");
                for (int i=0; i < sRGB.length; i++) {
                    if (i < 9) {
                        sCommand = "S0" + (i+1) + sRGB[i] + newLine;
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    } else {
                        sCommand = "S" + (i+1) + sRGB[i] + newLine;
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    }
                    //Toast.makeText(context, sCommand, Toast.LENGTH_LONG).show();
                }

                break;

            case R.id.btnL5:

                btn5Timer++;
                blLamp5_ON = true;
                btnL5.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL5.setTextColor(Color.BLACK);

//                sButtonCaption = btnL5.getTag().toString();
//                sButtonCaption = sButtonCaption + sNewLine + btn5Timer*5 + " sec";
//                btnL5.setText(sButtonCaption);

                sPresetRGBValues = json_analyst.getJSONValue("preset5_rgbw");
                sRGB = sPresetRGBValues.split(",");
                for (int i=0; i < sRGB.length; i++) {
                    if (i < 9) {
                        sCommand = "S0" + (i+1) + sRGB[i] + newLine;
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    } else {
                        sCommand = "S" + (i+1) + sRGB[i] + newLine;
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    }
                    //Toast.makeText(context, sCommand, Toast.LENGTH_LONG).show();
                }

                break;

            case R.id.btnL6:

                btn6Timer++;
                blLamp6_ON = true;
                btnL6.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL6.setTextColor(Color.BLACK);

//                sButtonCaption = btnL6.getTag().toString();
//                sButtonCaption = sButtonCaption + sNewLine + btn6Timer*5 + " sec";
//                btnL6.setText(sButtonCaption);

                sPresetRGBValues = json_analyst.getJSONValue("preset6_rgbw");
                sRGB = sPresetRGBValues.split(",");
                for (int i=0; i < sRGB.length; i++) {
                    if (i < 9) {
                        sCommand = "S0" + (i+1) + sRGB[i] + newLine;
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    } else {
                        sCommand = "S" + (i+1) + sRGB[i] + newLine;
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    }
                    //Toast.makeText(context, sCommand, Toast.LENGTH_LONG).show();
                }

                break;
        }
    }*/

    /*public void switch_all_off() {
        blLamp1_ON = false;
        btnL1.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL1.setTextColor(Color.WHITE);
        blLamp2_ON = false;
        btnL2.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL2.setTextColor(Color.WHITE);
        blLamp3_ON = false;
        btnL3.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL3.setTextColor(Color.WHITE);
        blLamp4_ON = false;
        btnL4.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL4.setTextColor(Color.WHITE);
        blLamp5_ON = false;
        btnL5.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL5.setTextColor(Color.WHITE);
        blLamp6_ON = false;
        btnL6.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL6.setTextColor(Color.WHITE);
    }*/

    public void populateLampsState() {
        SharedPreferences spsValues = getSharedPreferences(LightAdjustments.SHAREDPREFS_LAMP_STATE, MODE_PRIVATE);
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
