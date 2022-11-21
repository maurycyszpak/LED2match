package com.morris.LEDbar_controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import static com.morris.LEDbar_controller.LightSettings.SHAREDPREFS_LED_TIMERS;
import static com.morris.LEDbar_controller.LightSettings.bluetoothAskReply;
//import static com.kiand.LED2match.LightAdjustments.sendDataOverSerialAsync;
//import static com.kiand.LED2match.LightAdjustments.extractJSONvalue;
import static com.morris.LEDbar_controller.LightSettings.sNewLine;
import static com.morris.LEDbar_controller.LightSettings.sUnitName;
import static com.morris.LEDbar_controller.R.id.textCounterLED1;

public class ServicePageActivity extends Activity implements Serializable {

    final Context context2 = this;
    public static String TAG = "Morris-Service";
    private EditText edtUnitName;
    TextView textViewDate;
    TextView textLampCell;
    TextView textCounter1, textCounter2, textCounter3, textCounter4, textCounter5, textCounter6, textCounter7, textCounter8, textCounter9, textCounter10;
    public Spinner spinLamps;
    public ArrayAdapter<String> spinLampsAdapter;
    public ArrayList<String> spinLampsArrList = new ArrayList<String>(8);
    public HashMap<String ,String> hmLamps = new HashMap<String,String>();
    private int iSpinLampCount = 1;
    public static final String LAMPBAR_SHAREDPREFS = "LAMPButtons_visibility"; //Mauricio
    public SharedPreferences spsFile;
    final Context context = this;

    //private UsbService usbService;
    private LightSettings.MyHandler mHandler;
    private Handler lclHandler;

/*    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };*/
    private UsbCOMMsService lclUsbServiceInstance;
    private BtCOMMsService lclBTServiceInstance;
    boolean mBound = false;
    boolean mBoundBT = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        lclHandler = new Handler();

        textViewDate = findViewById(R.id.textViewDate);
        edtUnitName = findViewById(R.id.editTextUnitName);
        spsFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, MODE_PRIVATE);

        textCounter1 = findViewById(R.id.textCounterLED1);
        textCounter2 = findViewById(R.id.textCounterLED2);
        textCounter3 = findViewById(R.id.textCounterLED3);
        textCounter4 = findViewById(R.id.textCounterLED4);
        textCounter5 = findViewById(R.id.textCounterLED5);
        textCounter6 = findViewById(R.id.textCounterLED6);
        textCounter7 = findViewById(R.id.textCounterLED7);
        textCounter8 = findViewById(R.id.textCounterLED8);
        textCounter9 = findViewById(R.id.textCounterLED9);
        textCounter10 = findViewById(R.id.textCounterLED10);


        spinLamps = findViewById(R.id.spinnerLamps); // Mauricio
        spinLampsAdapter = new ArrayAdapter<String>(ServicePageActivity.this, R.layout.spinner_row, spinLampsArrList); // Mauricio
        spinLampsArrList.clear();
        hmLamps.clear();

        spinLamps.setAdapter(spinLampsAdapter); // Mauricio

        spinLamps.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //spinLamps = parent.getItemAtPosition(position).toString();
                iSpinLampCount = position;
                if (position == 0) {
                    showMiddleColumn(view);
                    textCounter1.setVisibility(View.VISIBLE);
                    textCounter2.setVisibility(View.VISIBLE);
                    textCounter3.setVisibility(View.VISIBLE);
                    textCounter4.setVisibility(View.VISIBLE);
                    textCounter5.setVisibility(View.VISIBLE);
                    textCounter6.setVisibility(View.VISIBLE);
                    textCounter7.setVisibility(View.VISIBLE);
                    textCounter8.setVisibility(View.VISIBLE);
                    textCounter9.setVisibility(View.VISIBLE);
                    textCounter10.setVisibility(View.VISIBLE);
                    //refreshCounters(findViewById(R.id.wrap_content));
                } else {
                    hideMiddleColumn(view);
                    textCounter1.setVisibility(View.VISIBLE);
                    textCounter2.setVisibility(View.GONE);
                    textCounter3.setVisibility(View.GONE);
                    textCounter4.setVisibility(View.GONE);
                    textCounter5.setVisibility(View.GONE);
                    textCounter6.setVisibility(View.GONE);
                    textCounter7.setVisibility(View.GONE);
                    textCounter8.setVisibility(View.GONE);
                    textCounter9.setVisibility(View.GONE);
                    textCounter10.setVisibility(View.GONE);

                    //String sLampNo = "0" + Integer.toString(position);
                    String sLampNo = Integer.toString(position);
                    JSON_analyst jSON = new JSON_analyst(spsFile);

                    String sLampDate = jSON.getJSONValue("lamp" + sLampNo + "_installed_date");
                    setLampInstalledDate(sLampDate);

                    //String sReply = bluetoothAskReply("S" + sLampNo);
                    //Log.d(TAG, "Timer of lamp " + (position) + " has increased by " + sReply + "ms.");
                    //formatTextCounter(textCounter1, sReply);

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        if (BtCore.Connected()) {
            checkForNewUnitName();
            //checkForNewLampName();
            checkForLampInstallDate();
            refreshCounters(findViewById(R.id.wrap_content));
            populateLampNames();
        }
    }

    public void checkForNewUnitName() {
        String sUnitName = "";

        Log.d(TAG, "bluetoothAskReply(V1)");
        sUnitName = bluetoothAskReply("V1");
        //Toast.makeText(this, "UNITNAME:" + sUnitName, Toast.LENGTH_LONG).show();
        setNewUnitName(sUnitName);
    }

    public void checkForNewUnitName_JSON() {
        String sUnitName = "";

        //Log.d(TAG, "bluetoothAskReply(V1)");
        JSON_analyst jsonAnalyst = new JSON_analyst(spsFile);

        //String sJsonBody = getJsonBody(SHAREDPREFS_CONTROLLER_FILEIMAGE);
        sUnitName = jsonAnalyst.getJSONValue("unit_name");
        //Toast.makeText(this, "UNITNAME:" + sUnitName, Toast.LENGTH_LONG).show();
        setNewUnitName(sUnitName);
    }

    public void checkForLampInstallDate() {
        Log.d(TAG, "bluetoothAskReply(V21)");
        String sLampDate = bluetoothAskReply("V21");
        setLampInstalledDate(sLampDate);
    }

    public void checkForNewLampName() {
        Log.d(TAG, "bluetoothAskReply(V2)");
        String sLampName = "";
        sLampName = bluetoothAskReply("V2");
        //Toast.makeText(this, "LAMPNAME:" + sLAMPNAME, Toast.LENGTH_LONG).show();
        //writeLampName(findViewById(R.id.wrap_content), sLampName);
    }

    public void setNewUnitName(String unitName) {

        if (unitName.length() > 0) {
            setTitle("HTS-LED2match " + unitName);
            Toast.makeText(this.getBaseContext(),"Application title '" + unitName + "' set.", Toast.LENGTH_SHORT).show();
        } else {
            setTitle("HTS-LED2match");
        }
    }

    public void setLampInstalledDate(String sDate) {
        //DateFormat format = new SimpleDateFormat("yyyyMMdd");
        //String parsedDate = sDate.substring(1, 4) + "-" +sDate.substring(5, 2) + "-" + sDate.substring(7, 2);
        //textViewDate.setText("Lamp installed: " + "20"+sDate);
        textViewDate.setText("Lamp installed: " + sDate);
        //BtCore.sendMessageBluetooth("Z7," + sDate + "\n");
    }

    protected void onPause() {
        super.onPause();
        //Toast.makeText(this.getBaseContext(),"Activity Paused", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();
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
            //startService(intent);
            bindService(intent, btConnection, Context.BIND_AUTO_CREATE);
            //Toast.makeText(this.getBaseContext(),"Service bound (onResume)", Toast.LENGTH_SHORT).show();

            SystemClock.sleep(100);
            mBoundBT = true;
        }

        /*if (mBound) {
            checkForNewUnitName_JSON();
            populateLampNames();
        }*/
        //setUnitNameOverSerial();
    }

    public void resetChrono(View view) {
        if (spinLamps.getSelectedItemPosition() == 0) {
            makeToast("Unable to replace LED bar");
            return;
        }

        int iLampNo = 10 + spinLamps.getSelectedItemPosition();
        String timeStamp = new SimpleDateFormat("yyyyMMdd").format(new Date());

        //makeToast("Select item: " + msg + "," + timeStamp);

        String msg = "Z" + iLampNo + "," + timeStamp;
        if (BtCore.Connected() || mBound) {
            try {
                //BtCore.sendMessageBluetooth("X\n");
                String sCommand = msg + "\n";
                int iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                if (iResult < 0) {
                    return;
                }
                //BtCore.sendMessageBluetooth("Z7," + timeStamp.substring(2) + "\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
            textViewDate.setText("Lamp installed: " + timeStamp); //culprit?
            msg = "F\n";
            int iResult = lclUsbServiceInstance.sendBytes(msg.getBytes());
            makeToast("Lamp replaced. Installed date set to: " + timeStamp);
        } else {
            Toast.makeText(this, "Please connect to the RGB LED first", Toast.LENGTH_SHORT).show();
        }
    }

    private String readData(View view, String fileName) {
        String output = "";
        BufferedReader bufferedReader = null;
        StringBuilder result = new StringBuilder();
        try {
            FileInputStream fileInputStream = openFileInput(fileName);
            bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line + "|");
                output = line.trim();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return output;
    }

    public void writeData(View view, String fileName, String sData) {
        BufferedWriter bufferWriter = null;
        try {
            FileOutputStream fileWrite = openFileOutput(fileName, Context.MODE_PRIVATE);
            bufferWriter = new BufferedWriter(new OutputStreamWriter(fileWrite));
            bufferWriter.write(sData);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setUnitName(View view) {

        String unitName = "";

        if (edtUnitName.getText().length() > 0) {
            unitName = edtUnitName.getText().toString();
            setTitle("HTS-LED2match " + unitName);
            //BtCore.sendMessageBluetooth("Z1," + unitName + "\n");
            String sCommand = "Z1," + unitName + "\n";
            try {
                int iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                lclBTServiceInstance.sendData(sCommand);
            } catch (NullPointerException e) {
                Toast.makeText(this, "NullPointerException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(this, "Unit name '" + unitName + "' applied.", Toast.LENGTH_SHORT).show();
            requestFileImage();
        }
    }

    public void requestFileImage() {
        String sCommand = "F\n";
        try {
            lclUsbServiceInstance.sendBytes(sCommand.getBytes());
            lclBTServiceInstance.sendData(sCommand);
        } catch (NullPointerException e) {
            Toast.makeText(this, "NullPointerException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(this, "Refreshing data from controller.", Toast.LENGTH_SHORT).show();
    }

    public void formatTextCounter(TextView cArg, String counter) {

        if (counter.isEmpty()) {
            counter = "0";
        }
       //Toast.makeText(context2, "Setting value '" + counter + "' to control: " + cArg.toString(), Toast.LENGTH_SHORT).show();
        long time = Long.parseLong(counter);

        int h = (int) (time / 3600);
        int m = (int) (time - h * 3600) / 60;
        int s = (int) (time - h * 3600 - m * 60);
        String hh = h < 10 ? "0" + h : h + "";
        String mm = m < 10 ? "0" + m : m + "";
        String ss = s < 10 ? "0" + s : s + "";
        cArg.setText(hh + ":" + mm);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_, menu);
        return true;
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

    /** Defines callbacks for service binding, passed to bindService() */
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
            mBoundBT = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBoundBT = false;
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void changeLNameDialog(final View view) {
        // get changelampname.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(context2);
        final View lampNameView = layoutInflater.inflate(R.layout.changelampname, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context2);

        // set changelampname.xml to be the layout file of the alertdialog builder
        alertDialogBuilder.setView(lampNameView);
        final EditText edtInput = lampNameView.findViewById(R.id.newLampName);


        // setup a dialog window
        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //get user input and set it to result

                        if (edtInput.getText().toString().length() > 0) {
                            String newLampName = edtInput.getText().toString();
                            //newLampName = newLampName.substring(0, 8);
                            updateLampNameSerial(view, newLampName);
                            /*new Timer().schedule(new TimerTask() {

                                @Override
                                public void run() {
                                    // this code will be executed after 200 mseconds
                                    lclHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            populateLampNames();
                                        }
                                    });
                                    return;
                                }
                            }, 1500);*/
                            //dialog.cancel();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
    }

    /*public void writeLampName(View v, String newLampName) {

        textLampName.setText(newLampName);
        File fLampName = getApplicationContext().getFileStreamPath("hts-chrono1-lampname");
        if (fLampName.exists()) {
            fLampName.delete();
        }
        writeData(findViewById(R.id.wrap_content), "hts-chrono1-lampname", newLampName);
    }*/

    public void updateLampName(View view, String newLampName) {
        //writeLampName(view, newLampName);
        //checkForNewLampName();
        Log.d(TAG, "changing lamp no " + iSpinLampCount + " to: " + newLampName);
        String sLampNo = "0" + iSpinLampCount;

        String sReply = bluetoothAskReply("M" + sLampNo + "," + newLampName);
        Toast.makeText(this, "Lamp " + iSpinLampCount + " renamed to '" + newLampName + "'.", Toast.LENGTH_SHORT).show();
    }

    public void updateLampNameSerial(View view, String newLampName) {
        int iResult = -1;
        Log.d(TAG, "changing lamp no " + iSpinLampCount + " to: " + newLampName);
        String sLampNo = "0" + iSpinLampCount;

        String sCommand = "M" + sLampNo + "," + newLampName + sNewLine;
        //String sReply = bluetoothAskReply("M" + sLampNo + "," + newLampName);

        try {
            iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());
        }
        catch (RuntimeException e) {
            Toast.makeText(this.getBaseContext(), "Exception: " + e, Toast.LENGTH_SHORT).show();
        }
        if (iResult < 0) {
            Toast.makeText(this.getBaseContext(), "Stream was null, no request sent", Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(this, "Lamp " + iSpinLampCount + " renamed to '" + newLampName + "'.", Toast.LENGTH_SHORT).show();
        SystemClock.sleep(250);

        String sCommand2 = "F" + sNewLine;
        int iBytes = lclUsbServiceInstance.sendBytes(sCommand2.getBytes());

        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                // this code will be executed after 200 mseconds
                lclHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        populateLampNames();
                    }
                });
                return;
            }
        }, 1500);

    }

    public void refreshCounters(View v) {
        String sRed = "";
        String sGreen = "";
        String sBlue = "";
        String sWhite = "";
        sRed = bluetoothAskReply("r");
        //SystemClock.sleep(50);
        sGreen = bluetoothAskReply("g");
        //SystemClock.sleep(50);
        sBlue = bluetoothAskReply("b");
        //SystemClock.sleep(50);
        sWhite = bluetoothAskReply("w");

        TextView txtCtrRED = findViewById(textCounterLED1);
        TextView txtCtrGREEN = findViewById(R.id.textCounterLED2);
        TextView txtCtrBLUE = findViewById(R.id.textCounterLED3);
        TextView txtCtrWHITE = findViewById(R.id.textCounterLED4);

        formatTextCounter(txtCtrRED, sRed);
        formatTextCounter(txtCtrGREEN, sGreen);
        formatTextCounter(txtCtrBLUE, sBlue);
        formatTextCounter(txtCtrWHITE, sWhite);

        //Toast.makeText(this, "RED:" + sRed + ", GREEN: " + sGreen + ", BLUE: " + sBlue + ", WHITE: " + sWhite, Toast.LENGTH_LONG).show();
    }

    public void resetCountersOverSerial (View v) {
        if (mBound) {
            String sCommand = "X" + sNewLine;
            //Toast.makeText(this.getBaseContext(),"Service bound. Executing sendBytes", Toast.LENGTH_SHORT).show();

            int iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());
            if (iResult < 0) {
                Toast.makeText(this.getBaseContext(), "Stream was null, no request sent", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(this.getBaseContext(), "Sending " + iResult + " bytes.", Toast.LENGTH_SHORT).show();
            }
        }

        //read in new timer values (should be 0)
        refreshCountersOverSerial(v);
    }


    public void refreshCountersOverSerial(View v) {
        int iResult = -1;

        //checkForNewUnitName_JSON();
        /*Toast.makeText(this.getBaseContext(),"Trying to send J", Toast.LENGTH_SHORT).show();
        Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();*/
        if (mBound) {
            String sCommand = "J" + sNewLine;
            /*Toast.makeText(this.getBaseContext(),"Service bound. Executing sendBytes", Toast.LENGTH_SHORT).show();*/

            try { iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes()); }
            catch (RuntimeException e) {
                Toast.makeText(this.getBaseContext(), "Exception: " + e, Toast.LENGTH_SHORT).show();
            }
            if (iResult < 0) {
                Toast.makeText(this.getBaseContext(), "Stream was null, no request sent", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(this.getBaseContext(), "Sending " + iResult + " bytes.", Toast.LENGTH_SHORT).show();
            }

        }

        new Timer().schedule(new TimerTask() {

     /*       private Handler updateUI = new Handler(){
                @Override
                public void dispatchMessage(Message msg) {
                    super.dispatchMessage(msg);

                }
            };*/

            @Override
            public void run() {
                // this code will be executed after 750 mseconds


               /* lclHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context2,"0. 750 milisecond delay over. Executing", Toast.LENGTH_SHORT).show();
                    }
                });*/


                SharedPreferences spsLEDtimers = getSharedPreferences(SHAREDPREFS_LED_TIMERS, 0);
                //SharedPreferences.Editor spsEditor = spsLEDtimers.edit();


                String sCurrTimers = spsLEDtimers.getString("currTimers", null);
                if (sCurrTimers == null ) {

                    lclHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context2,"1. No timer values found in the file", Toast.LENGTH_SHORT).show();
                            TextView txtCounterLED1 = findViewById(R.id.textCounterLED1);
                            //formatTextCounter(txtCounterLED1, "1131");
                        }
                    });


                    return;
                } else {
                    /*lclHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context2,"2. Timer values: " + sCurrTimers, Toast.LENGTH_SHORT).show();
                        }
                    });*/

                    String[] arrTimerValues = sCurrTimers.split("\\|");
                    if (arrTimerValues.length == 2) {
                        Long lTimeWritten = Long.parseLong(arrTimerValues[1]);
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        Long lMillisEpoch = timestamp.getTime();
                        int iCounter = 1;

                        lclHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                //Toast.makeText(context2,"3. Data written: " + (lTimeWritten/1000) + ". Current time: " + (lMillisEpoch/1000) + ". Time diff: " + (lMillisEpoch - lTimeWritten) / 1000, Toast.LENGTH_SHORT).show();
                            }
                        });

                        if (iCounter <= 3) {
                            /*lclHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context2,"5. Checking diff between millisEpoch and timeWritten", Toast.LENGTH_SHORT).show();
                                }
                            });*/
                            if ((lMillisEpoch - lTimeWritten) < 60000) {

                                /*lclHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context2,"4. Less than 60 seconds passed.", Toast.LENGTH_SHORT).show();
                                    }
                                });*/


                                String[] arrTimers = arrTimerValues[0].split(",");
                                if (arrTimers.length == 14) {

                                    lclHandler.post(new Runnable() {
                                        @Override
                                        public void run() {

                                            /*lclHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(context2,"5. Executing run in arrTimers.", Toast.LENGTH_SHORT).show();
                                                }
                                            });*/

                                            TextView txtCounterLED1 = findViewById(R.id.textCounterLED1);
                                            TextView txtCounterLED2 = findViewById(R.id.textCounterLED2);
                                            TextView txtCounterLED3 = findViewById(R.id.textCounterLED3);
                                            TextView txtCounterLED4 = findViewById(R.id.textCounterLED4);
                                            TextView txtCounterLED5 = findViewById(R.id.textCounterLED5);
                                            TextView txtCounterLED6 = findViewById(R.id.textCounterLED6);
                                            TextView txtCounterLED7 = findViewById(R.id.textCounterLED7);
                                            TextView txtCounterLED8 = findViewById(R.id.textCounterLED8);
                                            TextView txtCounterLED9 = findViewById(R.id.textCounterLED9);
                                            TextView txtCounterLED10 = findViewById(R.id.textCounterLED10);

                                            formatTextCounter(txtCounterLED1, arrTimers[0]);
                                            formatTextCounter(txtCounterLED2, arrTimers[1]);
                                            formatTextCounter(txtCounterLED3, arrTimers[2]);
                                            formatTextCounter(txtCounterLED4, arrTimers[3]);
                                            formatTextCounter(txtCounterLED5, arrTimers[4]);
                                            formatTextCounter(txtCounterLED6, arrTimers[5]);
                                            formatTextCounter(txtCounterLED7, arrTimers[6]);
                                            formatTextCounter(txtCounterLED8, arrTimers[7]);
                                            formatTextCounter(txtCounterLED9, arrTimers[8]);
                                            formatTextCounter(txtCounterLED10, arrTimers[9]);

                                            lclHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(context2,"Timers reported.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    });

                                }
                            } else {
                                /*lclHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context2,"7. No update done.more than 60 secs passed.", Toast.LENGTH_SHORT).show();
                                    }
                                });*/
                                //iCounter++; // try three times
                            }
                        }
                    } else {
                        lclHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context2,"8. Arrtimeslength not 2.", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }
            }
        }, 1000);
        //Toast.makeText(context2,"9. End Of Function.", Toast.LENGTH_SHORT).show();
    }

    public void setUnitNameOverSerial() {
        int iResult = -1;

        //Toast.makeText(this.getBaseContext(),"Service bound. Executing sendBytes", Toast.LENGTH_SHORT).show();

        if (mBound) {
            String sCommand = "V01" + sNewLine;
            //Toast.makeText(this.getBaseContext(),"Service bound. Executing sendBytes '" + sCommand+ "'", Toast.LENGTH_SHORT).show();

            iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());

            if (iResult < 0) {
                Toast.makeText(this.getBaseContext(), "Stream was null, no request sent", Toast.LENGTH_SHORT).show();
                return;

            }
        }

        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                // this code will be executed after 1000 mseconds
                lclHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(context2,"UnitName = " + sUnitName, Toast.LENGTH_SHORT).show();
                    }
                });

                if (LightSettings.sUnitName.length() > 0) {

                    lclHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setNewUnitName(sUnitName);
                        }
                    });
                    return;

                }
            }
        }, 1000);
        //Toast.makeText(context2,"9. End Of Function.", Toast.LENGTH_SHORT).show();
    }

    public void readEEPROMdata(View v) {

        if (BtCore.Connected()) {
            String sUnitName = "";
            String sLampName = "";
            String sLampDate = "";

            Log.d(TAG, "bluetoothAskReply(V1)");
            sUnitName = bluetoothAskReply("V1");
            //Toast.makeText(this, "UNITNAME:" + sUnitName, Toast.LENGTH_LONG).show();
            setNewUnitName(sUnitName);

            //SystemClock.sleep(1000);
            Log.d(TAG, "bluetoothAskReply(V2)");
            sLampName = bluetoothAskReply("V2");
            //Toast.makeText(this, "LAMPNAME:" + sLAMPNAME, Toast.LENGTH_LONG).show();
            //writeLampName(findViewById(R.id.wrap_content), sLampName);

            //SystemClock.sleep(1000);
            Log.d(TAG, "bluetoothAskReply(V21)");
            sLampDate = "20" + bluetoothAskReply("V21");
            setLampInstalledDate(sLampDate);
        } else {
            Toast.makeText(this, "Please connect to the RGB LED first", Toast.LENGTH_SHORT).show();
        }
    }

    public void populateLampNames() {
        //hmLamps.clear();
        String sReply = "";

        JSON_analyst jSON = new JSON_analyst(spsFile);
        sReply = jSON.getJSONValue("lamp1_name");
        sReply = sReply.concat(",");
        sReply = sReply.concat(jSON.getJSONValue("lamp2_name"));
        sReply = sReply.concat(",");
        sReply = sReply.concat(jSON.getJSONValue("lamp3_name"));
        //sReply = sReply.concat(",");
        //sReply = sReply.concat(jSON.getJSONValue("lamp4_name"));

        //Toast.makeText(this, sReply, Toast.LENGTH_SHORT).show();
        //String sReply = bluetoothAskReply("P");
        sReply.trim();
        spinLampsArrList.clear();
        spinLampsAdapter.notifyDataSetChanged();
        spinLampsArrList.add(0,"LED-BAR");
        //Log.d(TAG, "sReply = " + sReply);
        if (!((sReply.lastIndexOf(",")>-1))) {
            return;
        }
        String[] sLamps = sReply.split(",");

        if (sLamps.length == 3) {
            for (int i = 1; i <= 3; i++) {
                //hmLamps.put("0" + Integer.toString(i+1), sLamps[i]);
                //Log.d(TAG, "Adding '" + sLamps[i] + "' on position " + Integer.toString(i));
                spinLampsArrList.add(i, sLamps[i-1]);
                //spinLampsArrList.add()
                //Toast.makeText(this, "Lamp " + Integer.toString(i+1) + "added: " + spinLampsArrList.get(i), Toast.LENGTH_SHORT).show();
            }
            spinLampsAdapter.notifyDataSetChanged();
        }
    }

    public void renameLamp(View v) {

        if (spinLamps.getSelectedItemPosition() != 0) {
            changeLNameDialog(v);
            Log.d(TAG, "repopulating lamp names spinner after renaming");
        } else {
            Toast.makeText(this, "Unable to change the name of LED-BAR. This name is fixed", Toast.LENGTH_SHORT).show();
        }
    }

    public void defaultEEPROM(View w) {
        if (BtCore.Connected()) {
            String sReply = "";
            Log.d(TAG, "Clearing EEPROM");
            sReply = bluetoothAskReply("C");
            Log.d(TAG, "EEPROM wiped clean: " + sReply);
            SystemClock.sleep(250);
            Toast.makeText(getBaseContext(), "Controller's EEPROM cleared:" + sReply, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please connect to the RGB LED first", Toast.LENGTH_SHORT).show();
        }
    }

    public void factoryReset(View v) {
        int iResult = -1;
        String sCommand = "T" + sNewLine;
        iResult = lclUsbServiceInstance.sendBytes(sCommand.getBytes());

        if (iResult < 0) {
            Toast.makeText(this.getBaseContext(), "Stream was null, no request sent", Toast.LENGTH_SHORT).show();
            return;
        }
        SystemClock.sleep(250);
        Toast.makeText(getBaseContext(), "Controller reset to factory settings. Please close the app and reconnect the LED2match.", Toast.LENGTH_LONG).show();
    }

    public void hideMiddleColumn(View view ) {
        textLampCell = findViewById(R.id.textViewCOL2Header);
        textLampCell.setVisibility(View.GONE);

        textLampCell = findViewById(R.id.textViewRED);
        textLampCell.setVisibility(View.GONE);

        textLampCell = findViewById(R.id.textViewGREEN);
        textLampCell.setVisibility(View.GONE);

        textLampCell = findViewById(R.id.textViewBLUE);
        textLampCell.setVisibility(View.GONE);

        textLampCell = findViewById(R.id.textViewWHITE);
        textLampCell.setVisibility(View.GONE);

        textLampCell = findViewById(R.id.textViewLED5);
        textLampCell.setVisibility(View.GONE);
        textLampCell = findViewById(R.id.textViewLED6);
        textLampCell.setVisibility(View.GONE);
        textLampCell = findViewById(R.id.textViewLED7);
        textLampCell.setVisibility(View.GONE);
        textLampCell = findViewById(R.id.textViewLED8);
        textLampCell.setVisibility(View.GONE);
        textLampCell = findViewById(R.id.textViewLED9);
        textLampCell.setVisibility(View.GONE);
        textLampCell = findViewById(R.id.textViewLED10);
        textLampCell.setVisibility(View.GONE);
    }

    public void showMiddleColumn(View view ) {
        textLampCell = findViewById(R.id.textViewCOL2Header);
        textLampCell.setVisibility(View.VISIBLE);

        textLampCell = findViewById(R.id.textViewRED);
        textLampCell.setVisibility(View.VISIBLE);

        textLampCell = findViewById(R.id.textViewGREEN);
        textLampCell.setVisibility(View.VISIBLE);

        textLampCell = findViewById(R.id.textViewBLUE);
        textLampCell.setVisibility(View.VISIBLE);

        textLampCell = findViewById(R.id.textViewWHITE);
        textLampCell.setVisibility(View.VISIBLE);

        textLampCell = findViewById(R.id.textViewLED5);
        textLampCell.setVisibility(View.VISIBLE);
        textLampCell = findViewById(R.id.textViewLED6);
        textLampCell.setVisibility(View.VISIBLE);
        textLampCell = findViewById(R.id.textViewLED7);
        textLampCell.setVisibility(View.VISIBLE);
        textLampCell = findViewById(R.id.textViewLED8);
        textLampCell.setVisibility(View.VISIBLE);
        textLampCell = findViewById(R.id.textViewLED9);
        textLampCell.setVisibility(View.VISIBLE);
        textLampCell = findViewById(R.id.textViewLED10);
        textLampCell.setVisibility(View.VISIBLE);
    }

    private void check_LampButton_stage() {
        Log.d(TAG, "Checking Lamp Button layout state");
        SharedPreferences spsValues = getSharedPreferences(LAMPBAR_SHAREDPREFS, 0);
        Button btn = findViewById(R.id.buttonLampLayoutOnOff);
        int iVisFlag = spsValues.getInt("LAMP_LAYOUT", -1);
        if (iVisFlag == 1) {
            Log.d(TAG, "Lamp Button state: 1, trying to switch on the button");
            switchButtonOn(btn);
            btn.setText ("LAMP buttons: ON");
        } else if (iVisFlag == 0){
            Log.d(TAG, "Lamp Button state: 1, trying to switch off the button");
            switchButtonOff(btn);
            btn.setText ("LAMP buttons: OFF");
        }
    }

    public void switchLampLayoutOnOff(View v) {
        SharedPreferences spsValues = getSharedPreferences(LAMPBAR_SHAREDPREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor spsEditor = spsValues.edit();
        if(!spsValues.contains("LAMP_LAYOUT")) {
            spsEditor.putInt("LAMP_LAYOUT", 1);
            spsEditor.commit();
            Log.d(TAG, "LAMPBAR_SHAREDPREFS doesn't exist, creating");
        }
        int iVisibility = spsValues.getInt("LAMP_LAYOUT", -1);
        if (iVisibility == 1) {
            Log.d(TAG, "Currently LAMP layout visible, switching off=0");
            spsEditor.clear();
            spsEditor.commit();
            spsEditor.putInt("LAMP_LAYOUT", 0);
            spsEditor.commit();
            Button btn = findViewById(R.id.buttonLampLayoutOnOff);
            btn.setText ("LAMP buttons: OFF");
            switchButtonOff(btn);
        } else if (iVisibility == 0) {
            Log.d(TAG, "Currently LAMP layout INvisible, switching on=1");
            spsEditor.clear();
            spsEditor.commit();
            spsEditor.putInt("LAMP_LAYOUT", 1);
            spsEditor.commit();
            Button btn = findViewById(R.id.buttonLampLayoutOnOff);
            btn.setText ("LAMP buttons: ON");
            switchButtonOn(btn);
        }
        //LightAdjustments.switchOffAllLamps();
    }
    private void switchButtonOn (View v) {
        v.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_active));

    }

    private void switchButtonOff (View v) {
        v.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_2nd));
    }
    public void makeToast (String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
