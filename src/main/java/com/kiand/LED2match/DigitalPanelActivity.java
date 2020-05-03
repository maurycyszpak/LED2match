package com.kiand.LED2match;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class DigitalPanelActivity extends Activity {

    public static final String SHAREDPREFS_ONE_OFF_SEEKBARS = "one-off-seekbar-values.txt"; //Mauricio
    public static final String newLine = System.getProperty("line.separator");

    boolean blLamp1_ON, blLamp2_ON, blLamp3_ON, blLamp4_ON, blLamp5_ON, blLamp6_ON, blLamp7_ON, blLamp8_ON;
    Button btnL1, btnL2, btnL3, btnL4, btnL5, btnL6, btnL7, btnL8;

    private Handler lclHandler;
    private UsbCOMMsService lclUsbServiceInstance;
    boolean mBound = false;
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
        setContentView(R.layout.digital_panel);

        lclHandler = new Handler();

        btnL1 = findViewById(R.id.btnL1);
        btnL2 = findViewById(R.id.btnL2);
        btnL3 = findViewById(R.id.btnL3);
        btnL4 = findViewById(R.id.btnL4);
        btnL5 = findViewById(R.id.btnL5);
        btnL6 = findViewById(R.id.btnL6);
        btnL7 = findViewById(R.id.btnL7);
        btnL8 = findViewById(R.id.btnL8);

        btnL8.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {

                Intent intent = new Intent(DigitalPanelActivity.this, SequenceProgramming.class);
                startActivity(intent);
                return true;

            }
        });

        btnL8.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                updateUIView();

                String sCommand = "N" + newLine;
                lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                //switch all off;
                switch_all_off();
            }
        });

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

        btnL7.setBackgroundResource(R.drawable.buttonselector_main);
        btnL7.setTextColor(Color.WHITE);

        btnL8.setBackgroundResource(R.drawable.buttonselector_main);
        btnL8.setTextColor(Color.WHITE);

        populateButtonNames();
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
        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        //Log.d(TAG, "bluetoothAskReply(V1)");
        final String sLamp1Name = json_analyst.getJSONValue("preset1_name");
        final String sLamp2Name = json_analyst.getJSONValue("preset2_name");
        final String sLamp3Name = json_analyst.getJSONValue("preset3_name");
        final String sLamp4Name = json_analyst.getJSONValue("preset4_name");
        final String sLamp5Name = json_analyst.getJSONValue("preset5_name");
        final String sLamp6Name = json_analyst.getJSONValue("preset6_name");
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
                btnL1.setText("Button1");
            }
        } else if (i == 2) {
            if (sName.length() > 0) {
                btnL2.setText(sName);
            } else {
                btnL2.setText("Button2");
            }
        } else if (i == 3) {
            if (sName.length() > 0) {
                btnL3.setText(sName);
            } else {
                btnL3.setText("Button3");
            }
        } else if (i == 4) {
            if (sName.length() > 0) {
                btnL4.setText(sName);
            } else {
                btnL4.setText("Button4");
            }
        } else if (i == 5) {
            if (sName.length() > 0) {
                btnL5.setText(sName);
            } else {
                btnL5.setText("Button5");
            }
        } else if (i == 6) {
            if (sName.length() > 0) {
                btnL6.setText(sName);
            } else {
                btnL6.setText("Button6");
            }
        }

    }


    public void btnClicked(View v) {

        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        String sPresetRGBValues = "0,0,0,0,0,0,0,0,0,0";
        String sCommand = "";
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        switch_all_off();

        switch (v.getId()) {
            case R.id.btnL1:
                blLamp1_ON = true;
                btnL1.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL1.setTextColor(Color.BLACK);

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
                blLamp2_ON = true;
                btnL2.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL2.setTextColor(Color.BLACK);

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
                blLamp3_ON = true;
                btnL3.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL3.setTextColor(Color.BLACK);

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
                blLamp4_ON = true;
                btnL4.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL4.setTextColor(Color.BLACK);

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
                blLamp5_ON = true;
                btnL5.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL5.setTextColor(Color.BLACK);

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
                blLamp6_ON = true;
                btnL6.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL6.setTextColor(Color.BLACK);

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

            case R.id.btnL7:
                blLamp7_ON = true;
                btnL7.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                btnL7.setTextColor(Color.BLACK);


                break;
        }

        /*switch (v.getId()) {
            case R.id.btnL1:
                if (blLamp1_ON == true) {
                    //switch it off
                    blLamp1_ON = false;

                    try {
                        String sCommand = "L01,0\n";
                        int iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        Toast.makeText(getBaseContext(), "Lamp switched off!", Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException e) {
                        //Toast.makeText(getBaseContext(), "Unable to locate button in current view", Toast.LENGTH_SHORT).show();
                    }
                    btnL1.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL1.setTextColor(Color.WHITE);
                } else if (blLamp1_ON == false) {

                    blLamp1_ON = true;

                    //switch it on
                    try {
                        String sCommand = "L01,1\n";
                        int iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        Toast.makeText(getBaseContext(), "Lamp switched on!", Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException e) {
                        //Toast.makeText(getBaseContext(), "Unable to locate button in current view", Toast.LENGTH_SHORT).show();
                    }
                    btnL1.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                    btnL1.setTextColor(Color.BLACK);
                }
                break;

            case R.id.btnL2:
                if (blLamp2_ON == true) {
                    //switch it off
                    blLamp2_ON = false;

                    try {
                        String sCommand = "L02,0\n";
                        int iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        Toast.makeText(getBaseContext(), "Lamp switched off!", Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException e) {
                        //Toast.makeText(getBaseContext(), "Unable to locate button in current view", Toast.LENGTH_SHORT).show();
                    }
                    btnL2.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL2.setTextColor(Color.WHITE);
                } else if (blLamp2_ON == false) {
                    //switch it on
                    blLamp2_ON = true;

                    try {
                        String sCommand = "L02,1\n";
                        int iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        Toast.makeText(getBaseContext(), "Lamp switched on!", Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException e) {
                        //Toast.makeText(getBaseContext(), "Unable to locate button in current view", Toast.LENGTH_SHORT).show();
                    }
                    btnL2.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                    btnL2.setTextColor(Color.BLACK);
                }
                break;

            case R.id.btnL3:
                if (blLamp3_ON == true) {
                    //switch it off
                    blLamp3_ON = false;

                    try {
                        String sCommand = "L03,0\n";
                        int iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        Toast.makeText(getBaseContext(), "Lamp switched off!", Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException e) {
                        //Toast.makeText(getBaseContext(), "Unable to locate button in current view", Toast.LENGTH_SHORT).show();
                    }
                    btnL3.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL3.setTextColor(Color.WHITE);
                } else if (blLamp3_ON == false) {
                    //switch it on
                    blLamp3_ON = true;

                    try {
                        String sCommand = "L03,1\n";
                        int iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        Toast.makeText(getBaseContext(), "Lamp switched on!", Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException e) {
                        //Toast.makeText(getBaseContext(), "Unable to locate button in current view", Toast.LENGTH_SHORT).show();
                    }
                    btnL3.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                    btnL3.setTextColor(Color.BLACK);
                }
                break;

            case R.id.btnL4:
                if (blLamp4_ON == true) {
                    //switch it off
                    blLamp4_ON = false;

                    try {
                        String sCommand = "L04,0\n";
                        int iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        Toast.makeText(getBaseContext(), "Lamp switched off!", Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException e) {
                        //Toast.makeText(getBaseContext(), "Unable to locate button in current view", Toast.LENGTH_SHORT).show();
                    }

                    btnL4.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL4.setTextColor(Color.WHITE);
                } else if (blLamp4_ON == false) {
                    //switch it on
                    blLamp4_ON = true;

                    try {
                        String sCommand = "L04,1\n";
                        int iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                        Toast.makeText(getBaseContext(), "Lamp switched on!", Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException e) {
                        //Toast.makeText(getBaseContext(), "Unable to locate button in current view", Toast.LENGTH_SHORT).show();
                    }
                    btnL4.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                    btnL4.setTextColor(Color.BLACK);
                }
                break;

            case R.id.btnL5:
                if (blLamp5_ON == true) {
                    //switch it off

                    blLamp5_ON = false;
                    btnL5.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL5.setTextColor(Color.WHITE);
                } else if (blLamp5_ON == false) {
                    //switch it on

                    blLamp5_ON = true;
                    btnL5.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                    btnL5.setTextColor(Color.BLACK);
                }
                break;

            case R.id.btnL6:
                if (blLamp6_ON == true) {
                    //switch it off

                    blLamp6_ON = false;
                    btnL6.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL6.setTextColor(Color.WHITE);
                } else if (blLamp6_ON == false) {
                    //switch it on

                    blLamp6_ON = true;
                    btnL6.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                    btnL6.setTextColor(Color.BLACK);
                }
                break;

            case R.id.btnL7:
                if (blLamp7_ON == true) {
                    //switch it off

                    blLamp7_ON = false;
                    btnL7.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL7.setTextColor(Color.WHITE);
                } else if (blLamp7_ON == false) {
                    //switch it on

                    blLamp7_ON = true;
                    btnL7.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                    btnL7.setTextColor(Color.BLACK);
                }
                break;

            case R.id.btnL8:
                if (blLamp8_ON == true) {
                    //switch it off

                    blLamp8_ON = false;
                    btnL8.setBackgroundResource(R.drawable.buttonselector_main);
                    btnL8.setTextColor(Color.WHITE);
                } else if (blLamp8_ON == false) {
                    //switch it on

                    blLamp8_ON = true;
                    btnL8.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));
                    btnL8.setTextColor(Color.BLACK);
                }
                break;
        }*/
    }

    public void switch_all_off() {
        blLamp1_ON = false;
        btnL1.setBackgroundResource(R.drawable.buttonselector_main);
        btnL1.setTextColor(Color.WHITE);
        blLamp2_ON = false;
        btnL2.setBackgroundResource(R.drawable.buttonselector_main);
        btnL2.setTextColor(Color.WHITE);
        blLamp3_ON = false;
        btnL3.setBackgroundResource(R.drawable.buttonselector_main);
        btnL3.setTextColor(Color.WHITE);
        blLamp4_ON = false;
        btnL4.setBackgroundResource(R.drawable.buttonselector_main);
        btnL4.setTextColor(Color.WHITE);
        blLamp5_ON = false;
        btnL5.setBackgroundResource(R.drawable.buttonselector_main);
        btnL5.setTextColor(Color.WHITE);
        blLamp6_ON = false;
        btnL6.setBackgroundResource(R.drawable.buttonselector_main);
        btnL6.setTextColor(Color.WHITE);
        blLamp7_ON = false;
        btnL7.setBackgroundResource(R.drawable.buttonselector_main);
        btnL7.setTextColor(Color.WHITE);
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

    public void switch_preset_on () {

    }

    public void makeToast (String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
