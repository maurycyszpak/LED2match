    package com.morris.LEDbar_controller;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;

import static com.morris.LEDbar_controller.Constants.BT_CONNECTED_PREFS;
import static com.morris.LEDbar_controller.Constants.PRESETS_DEFINITION_JSONFILE;
import static com.morris.LEDbar_controller.LightSettings.SHAREDPREFS_LAMP_STATE;
import static com.morris.LEDbar_controller.LightSettings.SHAREDPREFS_LED_TIMERS;
import static com.morris.LEDbar_controller.TRSDigitalPanel.SHAREDPREFS_LAMP_ASSIGNMENTS;
import static com.morris.LEDbar_controller.Constants.sNewLine;
import static com.morris.LEDbar_controller.Constants.PRESETS_DEFINITION;

public class BtCOMMsService extends Service {

    final boolean APP_DEBUG_MODE = false;
    //final boolean APP_DEBUG_MODE = true;
    final int handlerState = 0;                        //used to identify handler message
    Handler bluetoothIn = new Handler();
    private BluetoothAdapter btAdapter = null;
    public boolean connected = false;

    public static final String BT_PREFS = "bluetooth_status";

    public static final String SHAREDPREFS_UNITNAME = "unit_name";
    public static final String BT_COMMS_LOG = "bluetooth_controller-communication-log"; //Mauricio
    private ConnectingThread mConnectingThread;
    private ConnectedThread mConnectedThread;

    private boolean stopThread;
    public final String TAG = "MORRIS-BTCMS";
    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String MAC_ADDRESS = null;
    private final StringBuilder recDataString = new StringBuilder();
    private final IBinder mBinder = new MyBinder();
    private Handler mHandler;
    private final String newline = "\r\n";
    private BluetoothDevice device;
    public static String sBTResponse ="";
    public AsyncTask<?, ?, ?> running_task;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SERVICE CREATED");
        stopThread = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SERVICE STARTED");
        SharedPreferences prefs = getSharedPreferences(BT_PREFS, Context.MODE_PRIVATE);
        //Log.d(TAG,intent.getStringExtra("shutdown"));
        if (intent != null) {
            try {
                Log.d(TAG, "Received some intent");
                Log.d(TAG, "Intent get string extra: " + intent.getStringExtra("shutdown"));
                int flag = Integer.valueOf(intent.getStringExtra("shutdown"));
                if (flag == 1) {
                    Log.d(TAG, "Received signal - shutting down lamps");
                    String sCommand = "S00000000000000000000";
                    sCommand += "$" + sNewLine;
                    sendData(sCommand);
                }
            } catch (NumberFormatException e) { e.printStackTrace(); }
        }

        MAC_ADDRESS = prefs.getString("device_btaddress", "");

        if (MAC_ADDRESS.length() <= 1 || MAC_ADDRESS == null) {
            Log.d(TAG, "MAC_ADDRESS in the sharedprefs file is empty. Stopping service");
            stopSelf();
        }

        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg) {
                //  Log.d(TAG, "handleMessage");
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);

                    //Log.d(TAG + " RECORDED", recDataString.toString().replace("\r", "\\r").replace("\n", "\\n"));
                    // Do stuff here with your data, like adding it to the database
                    BTconcatResponse(readMessage);
                }
                recDataString.delete(0, recDataString.length());                    //clear all string data
            }
        };


        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        Log.d(TAG, "Executing ckeckBTState");
        checkBTState();
        return START_STICKY;
        //return super.onStartCommand(intent, flags, startId);
    }

    public void BTconcatResponse(String sBuffer) {

        //Toast.makeText(this, "USBConcat:" + sBuffer, Toast.LENGTH_SHORT).show();
        if (sBuffer.length() > 0) {
            sBTResponse = sBTResponse.concat(sBuffer);
            if (blHasNewLine(sBTResponse)) {
                //Log.d(TAG, "Received a newline. BTconcatresponse so far: " + sBTResponse);
                String[] sBufferRows = sBTResponse.split("(\\r|\\n)");

                for (String sBufferRow: sBufferRows) {
                    //Log.d (TAG, "New approach: will call decodeBTResponse on: '" + sBufferRow + "'.");
                    sBTResponse = sBufferRow.replaceAll("(\\r|\\n)", "");
                    decodeBTResponse(sBTResponse);
                }

                BluetoothCommsLog BT_log = new BluetoothCommsLog("IN << ", sBTResponse);
                BT_log.appendMessage("IN << ", sBTResponse);
                //makeToast("BTconcatResponse: " + sBTResponse);
                //Toast.makeText(this, "CRLF found. String terminated:" + sBTResponse, Toast.LENGTH_SHORT).show();
                //decodeBTResponse(sBTResponse);

                sBTResponse = "";
            }
        }
    }
    public void makeToast (String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }


    public void decodeBTResponse(String sDecodedReply) {
        //makeToast("Entering decodeBTResponse: '" + sReply + "'");
        Log.d(TAG, "decodeBTResponse() - sDecodedReply = " + sDecodedReply);

        //String sDecodedReply = sReply;
        //logIncomingData(sDecodedReply);

        if (!sDecodedReply.contains(",")) {
            return;
        }

        String sPrefix = "";
        try {
            sPrefix = sDecodedReply.substring(0, sDecodedReply.indexOf(",")); // expecting "RGBW"
            //makeToast("sPrefix:" + sPrefix);
        } catch (StringIndexOutOfBoundsException e) {
            makeToast("Unable to find ',' in string: " + sDecodedReply);
        }
        String sDataPart = sDecodedReply.substring(sDecodedReply.indexOf(",")+1); // expecting eg "J,255,0,234,123,..."
        //Log.d(TAG, "Data part of the payload: " + sDataPart + ", prefix: " + sPrefix + ", sPrefix = RGBW: " + sPrefix.equals("RGBW"));

        if (sPrefix.equals("RGBW")) {
            //Log.d(TAG, "Decoding response, 'RGBW' found");
            if (APP_DEBUG_MODE) {
                //Log.d(TAG, "Decoding response, RGBW found");
                Log.d(TAG, "PAYLOAD: " + sDataPart);
            }

            String[] sDataArray = sDataPart.split(",");

            if ((sDataArray[0]).equals("A")) {
                //process the reply for 'A'
                Log.d( TAG, "Firmware reports having " + sDataArray[1] + " presets currently stored in flash memory.");
                request_preset_intent(sDataArray[1]);

            } else if ((sDataArray[0]).equals("G")) {
                //process the reply for 'G'
                //This reports the temperature
                try {
                    Log.d(TAG, "Temperature reported Celsius: " + sDataArray[1]);
                    String sTemp = sDataArray[1];
                    Intent intent = new Intent("temperature_reading_event");
                    intent.putExtra("temperature", sTemp);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(TAG, "Corrupt temperature reading: '" + sDataPart + "'.");
                }

            } else if ((sDataArray[0]).equals("FW")) {
                //process the reply for 'FW'
                //makeToast("Processing preset response " + sReplyDataPart + ". Length: " + sDataArray.length);
                //String sFWver = sDataArray[1];

            } else if ((sDataArray[0]).equals("LAMPS")) {
                //process the reply for 'LAMPS'
                //this will return e.g. "RGBW,LAMPS,0,1,0,1" this means lamps 2 and 4 are on
                SharedPreferences spsControllerData = getSharedPreferences(SHAREDPREFS_LAMP_STATE, 0);
                SharedPreferences.Editor spsEditor = spsControllerData.edit();
                spsEditor.clear(); //Delete previous presets
                spsEditor.apply();

                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                long lMillisEpoch = timestamp.getTime();

                //String sTemp = sDataPart;

                spsEditor.putString("LAMPS", sDataPart);
                spsEditor.putLong("timestamp", lMillisEpoch);
                spsEditor.apply();
                makeToast("Current lamp state read.\nTimestamp: " + timestamp);

            } else if ((sDataArray[0]).equals("FILE")) {
                //process the reply for 'FILE'
                //makeToast("Processing preset response " + sReplyDataPart + ". Length: " + sDataArray.length);
                SharedPreferences spsControllerData = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
                SharedPreferences.Editor spsEditor = spsControllerData.edit();
                spsEditor.clear(); //Delete previous presets
                spsEditor.apply();

                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                long lMillisEpoch = timestamp.getTime();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm.ss", Locale.US);

                String sTemp = sDataPart;
                Log.d(TAG, "FILE json contents received: " + sDataPart);
                //makeToast("Filesize: " + sTemp.length());
                if (sTemp.length() < 10) {
                    //factoryReset(); TBC
                    Toast.makeText(getBaseContext(), "Controller reset to factory settings. Please close the app and reconnect the LED2match.", Toast.LENGTH_LONG).show();
                }

                try {
                    sTemp = sTemp.replaceAll(";$", "");
                    sTemp = sTemp.replace("\\", "\\\\");
                    sTemp = sTemp.replaceAll("\";\"", "\",\"");
                    //Log.d(TAG, "Replacing semicolons to commas?");
                } catch ( PatternSyntaxException e) {
                    makeToast("Unable to find ';}' in string: " + sTemp);
                }

                try {
                    sTemp = sTemp.substring(sTemp.indexOf(",")+1);
                } catch (StringIndexOutOfBoundsException e) {
                    makeToast("Unable to find ',' in string: " + sTemp);
                }

                Log.d(TAG, "Storing contents to JSON file: '" + sTemp + "'");
                spsEditor.putString("JSON", sTemp);
                spsEditor.putLong("timestamp", lMillisEpoch);
                spsEditor.commit();
                //makeToast("Data from controller refreshed.\nTimestamp: " + String.valueOf(timestamp).substring(0, String.valueOf()));
                makeToast("Data from controller refreshed.\nTimestamp: " + sdf.format(timestamp));
                SystemClock.sleep(100);

                SharedPreferences spsPresetFile = getSharedPreferences(PRESETS_DEFINITION, 0);
                SharedPreferences.Editor spEditorPresets = spsPresetFile.edit();
                spEditorPresets.clear();
                spEditorPresets.apply();

                JSON_analyst json_analyst = new JSON_analyst(spsControllerData);
                String sPresets = json_analyst.populatePresetsFromFILE();
                Log.d(TAG, "received presets. Will execute loop if " + sPresets.length() + " > 0");
                if (sPresets.length() > 0)  {
                    //wez jeden dżejson i zrob drugi
                    JSONObject jsonObject = null;
                    try {
                        sTemp = "{" + sTemp + "}";
                        Log.d(TAG, "Trying to create JSON Object on: " + sTemp);
                        jsonObject = new JSONObject(sTemp);
                        Log.d(TAG, "Trying to parse: " + sTemp);
                        JSONObject jsonPresets = new JSONObject();
                        for (int i=1; i<11; i++) {
                            String iterKey1 = "p" + i + "_nm";
                            String iterKey2 = "p" + i + "_def";
                            if (jsonObject.getString(iterKey1).length() > 0) {
                                jsonPresets.put(iterKey1, jsonObject.getString(iterKey1));
                                jsonPresets.put(iterKey2, jsonObject.getString(iterKey2));
                            } else {
                                Log.d(TAG, "Not storing preset " + i + ", " + iterKey1 + " = '" + jsonObject.getString(iterKey1) + "'");
                            }

                        }
                        Log.d(TAG, "Formatted JSON object will present: " + jsonPresets);
                        store_presets_file(jsonPresets.toString());
                    } catch (JSONException e) {
                        Log.d(TAG, "JSON exception when trying to iterate over presets definition");
                        e.printStackTrace();
                    }

                }
                Intent intent = new Intent("controller_data_refreshed_event");
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                Log.d (TAG, "controller_data_refreshed_event intent sent!");


            } else if ((sDataArray[0]).equals("J")) {

                String sValues = sDataPart.substring(sDataPart.indexOf(",")+1); // expecting eg "255,0,234,123,..."
                // Save the values to SharedPrefs File together with a current timetstamp
                SharedPreferences spsLEDtimers = getSharedPreferences(SHAREDPREFS_LED_TIMERS, MODE_PRIVATE);
                SharedPreferences.Editor spsEditor = spsLEDtimers.edit();
                spsEditor.clear(); //Delete previous presets
                //spsEditor.apply();
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                long lMillisEpoch = timestamp.getTime();
                spsEditor.putString("currTimers", sValues + "|" + lMillisEpoch );
                spsEditor.apply();
                Intent intent = new Intent("timers_received_event");
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            } else if ((sDataArray[0]).equals("BUTTON")) {
                //this means we should check which button has a TAG equal to the returned value and illuminate it
                //check in the file assignment order:
                Log.d(TAG, "Received BUTTON command: " + sDecodedReply);
                try {
                    //Log.d (TAG, "Locating preset '" + sDataArray[1] + "' in the lamp_button_assignments.xml");
                    int iButtonIndex;
                    SharedPreferences myPrefs = this.getSharedPreferences(SHAREDPREFS_LAMP_ASSIGNMENTS, 0);
                    TreeMap<String, ?> keys = new TreeMap<String, Object>(myPrefs.getAll());
                    if (sDataArray[1].equalsIgnoreCase("PRG")) {
                        Intent intent = new Intent("button_highlight_event");
                        intent.putExtra("button_name", sDataArray[1]);
                        Log.d(TAG, "Sending intent to highlight PRG in APP");
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    } else {
                        //Log.d(TAG, "*** NEW debug info - 0:" + sDataArray[0] + ",1:" + sDataArray[1]);
                        if (sDataArray[1].indexOf("^") > 0) {
                            Log.d(TAG, "Found '^' in the name of button to be highlighted!");
                            sDataArray[1] = sDataArray[1].replace("^","");

                            Intent intent = new Intent("button_highlight_extra");
                            intent.putExtra("button_name", sDataArray[1]);
                            Log.d (TAG, "Additional button to highlight: " + sDataArray[1]);
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                        } else if (sDataArray[1].indexOf("#") > 0) {
                            Log.d(TAG, "Found '#' in the name of button to be de-highlighted!");
                            sDataArray[1] = sDataArray[1].replace("#","");
                            Intent intent = new Intent("button_dehighlight_event");
                            intent.putExtra("button_name", sDataArray[1]);
                            Log.d (TAG, "Button to dehighlight: " + sDataArray[1]);
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                        } else {
                            Log.d(TAG, "BUTTON data received with value: " + sDataArray[1]);
                            for (Map.Entry<String, ?> entry : keys.entrySet()) {
                                if (entry.getValue().toString().equalsIgnoreCase(sDataArray[1])) {
                                    iButtonIndex = Integer.valueOf(entry.getKey());
                                    Intent intent = new Intent("button_highlight_event");
                                    intent.putExtra("button_index", String.valueOf(iButtonIndex));
                                    intent.putExtra("button_name", sDataArray[1]);
                                    Log.d(TAG, "Found preset '" + sDataArray[1] + "' under index: " + iButtonIndex);
                                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                                }
                            }
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException i) {
                    Log.d(TAG, "Array Index Out Of Bounds: " + sDecodedReply);
                }

                //also check for modifiers / special buttons (LOW, UV, OFF, PRG)
                List<String> extra = Arrays.asList("OFF");
                for (String entry: extra) {
                    try {
                        if (sDataArray[1].equalsIgnoreCase(entry)) {
                            Intent intent = new Intent("button_highlight_extra");
                            intent.putExtra("button_name", entry);
                            //Log.d (TAG, "Additional button to highlight: " + sDataArray[1]);
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        //PopupAsyncTask asyncTask = new PopupAsyncTask(context, "");
        //asyncTask.execute();
        //arrReply.add(i, sLamps[i]);
    }

    private void store_presets_file(String content) {
        try {
            FileOutputStream fOut = openFileOutput(Constants.PRESETS_DEFINITION_JSONFILE, MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            osw.write(content);

            osw.flush();
            osw.close();
            Log.d(TAG, "File '" + PRESETS_DEFINITION_JSONFILE + "' saved.");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void request_preset_intent(String sCounter) {
        Intent intent = new Intent("request_preset_event");
        intent.putExtra("counter", sCounter);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }



    public static boolean blHasNewLine(String sBuffer) {
        String newline = System.getProperty("line.separator");
        newline = "\n";
        boolean hasNewLine = sBuffer.contains(newline);
        if (sBuffer.length() > 0) {
            //makeToast("Buffer '" + sBuffer + "', hasnewline: " + hasNewLine);
        }
        return hasNewLine;
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bluetoothIn.removeCallbacksAndMessages(null);
        stopThread = true;
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
        }
        if (mConnectingThread != null) {
            mConnectingThread.closeSocket();

        }
        mark_BT_disconnected();
        Log.d(TAG, "onDestroy");


    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Task removed - stopping service");
        super.onTaskRemoved(rootIntent);
        //do something you want
        mark_BT_disconnected();
        //stop service
        this.stopSelf();
    }

    //@Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class MyBinder extends Binder {
        BtCOMMsService getService() {
            return BtCOMMsService.this;
        }
    }

    public boolean bluetooth_connected() {
        SharedPreferences spFile = getSharedPreferences(BT_CONNECTED_PREFS, 0);
        return spFile.getBoolean("CONNECTED", false);
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if (!bluetooth_connected()) {
            if (btAdapter == null) {
                Log.d(TAG, "BLUETOOTH NOT SUPPORTED BY DEVICE, STOPPING SERVICE");
                stopSelf();
            } else {
                if (btAdapter.isEnabled()) {
                    Log.d(TAG, "BT ENABLED! BT ADDRESS : " + btAdapter.getAddress() + " , BT NAME : " + btAdapter.getName());
                    try {
                        //BluetoothDevice device = btAdapter.getRemoteDevice(MAC_ADDRESS);
                        device = btAdapter.getRemoteDevice(MAC_ADDRESS);
                        Log.d(TAG, "ATTEMPTING TO CONNECT TO REMOTE DEVICE : " + MAC_ADDRESS);
                        mConnectingThread = new ConnectingThread(device);
                        mConnectingThread.start();
                    } catch (IllegalArgumentException e) {
                        Log.d(TAG, "PROBLEM WITH MAC ADDRESS : " + e);
                        Log.d(TAG, "ILLEGAL MAC ADDRESS, STOPPING SERVICE");
                        stopSelf();
                    }
                } else {
                    Log.d(TAG, "BLUETOOTH NOT ON, STOPPING SERVICE");
                    stopSelf();
                }
            }
        }
    }

    public static class BluetoothCommsLog {
        private final String sMessage;
        private final String sKey;


        BluetoothCommsLog(String sKey, String sMessage) {this.sKey = sKey; this.sMessage = sMessage; }

        //public static SharedPreferences getSharedPreferences (Context ctxt) {
//			return ctxt.getSharedPreferences(BTCOMMSLOG_SHAREDPREFS, MODE_APPEND);
        //}


        void appendMessage(String sKey, String sMessage) {
            SharedPreferences spLogFile = MainApplication.getAppContext().getSharedPreferences(BT_COMMS_LOG, 0);
            SharedPreferences.Editor spsEditor = spLogFile.edit();
            Date dteNow = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String sTimeStamp = sdf.format(dteNow);

            spsEditor.putString(sKey +"|"+sTimeStamp, sTimeStamp + " # " + sMessage);
            spsEditor.apply();
        }


    }

    public void mark_BT_connnected() {
        SharedPreferences prefs = getSharedPreferences(BT_CONNECTED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = prefs.edit();

        spEditor.clear();
        spEditor.putBoolean("CONNECTED", true);
        spEditor.putString(Constants.SESSION_CONNECTED_MAC_TAG, MAC_ADDRESS);
        spEditor.apply();
        Log.d(TAG, "BT connection marked as true in the sp file");


    }

    public void mark_BT_disconnected() {
        SharedPreferences prefs = getSharedPreferences(BT_CONNECTED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.clear();
        spEditor.putBoolean("CONNECTED", false);
        spEditor.apply();
        Log.d(TAG, "BT connection marked as false in the sp file");
    }

    public void disconnect() {

        Log.d(TAG, "Asynctask execution...");
        AsyncTask<?, ?, ?> runningTask = new AsyncTaskBTOff();
        runningTask.execute();
        if (mConnectedThread != null) {
            mConnectedThread.resetConnection();
        }
        this.stopSelf();
    }

    private void xxx() {

        if (btAdapter == null) {
            Log.d(TAG, "BLUETOOTH NOT SUPPORTED BY DEVICE, STOPPING SERVICE");
            stopSelf();
        } else {
            if (btAdapter.isEnabled()) {
                //Log.d(TAG, "BT ENABLED! BT ADDRESS : " + btAdapter.getAddress() + " , BT NAME : " + btAdapter.getName());
                try {
                    BluetoothDevice device = btAdapter.getRemoteDevice(MAC_ADDRESS);
                    Log.d(TAG, "ATTEMPTING TO CONNECT TO REMOTE DEVICE : " + MAC_ADDRESS);
                    mConnectingThread = new ConnectingThread(device);
                    mConnectingThread.start();
                } catch (IllegalArgumentException e) {
                    Log.d(TAG, "PROBLEM WITH MAC ADDRESS : " + e);
                    Log.d(TAG, "ILLEGAL MAC ADDRESS, STOPPING SERVICE");
                    stopSelf();
                }
            } else {
                Log.d(TAG, "BLUETOOTH NOT ON, STOPPING SERVICE");
                stopSelf();
            }
        }
    }

    // New Class for Connecting Thread
    private class ConnectingThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        ConnectingThread(BluetoothDevice device) {
            Log.d(TAG, "IN CONNECTING THREAD");
            mmDevice = device;
            BluetoothSocket temp = null;
            Log.d(TAG, "MAC ADDRESS : " + MAC_ADDRESS);
            Log.d(TAG, "BT UUID : " + BTMODULEUUID);
            try {
                temp = mmDevice.createRfcommSocketToServiceRecord(BTMODULEUUID);
                temp = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", int.class).invoke(device,1);
                Log.d(TAG, "SOCKET CREATED : " + temp.toString());
            } catch (IOException  | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                Log.d(TAG, "SOCKET CREATION FAILED :" + e);
                Log.d(TAG, "SOCKET CREATION FAILED, STOPPING SERVICE");
                stopSelf();
            }
            mmSocket = temp;
        }

        @Override
        public void run() {
            super.run();
            Log.d(TAG, "IN CONNECTING THREAD RUN");
            // Establish the Bluetooth socket connection.
            // Cancelling discovery as it may slow down connection
            btAdapter.cancelDiscovery();
            try {
                //mmSocket =(BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                mmSocket.connect();
                Log.d(TAG, "BT SOCKET CONNECTED");
                mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();
                Log.d(TAG, "CONNECTED THREAD STARTED");
                connected = true;
                //I send a character when resuming.beginning transmission to check device is connected
                //If it is not an exception will be thrown in the write method and finish() will be called
                //mConnectedThread.write("S07030" + newline);
            } catch (IOException e) {
                try {
                    Log.d(TAG, "SOCKET CONNECTION FAILED : " + e);
                    Log.d(TAG, "SOCKET CONNECTION FAILED, STOPPING SERVICE");

                    e.printStackTrace();
                    mmSocket.close();
                    stopSelf();
                } catch (IOException e2) {
                    Log.d(TAG, "SOCKET CLOSING FAILED :" + e2);
                    Log.d(TAG, "SOCKET CLOSING FAILED, STOPPING SERVICE");
                    stopSelf();
                    //insert code to deal with this
                }
            } catch (IllegalStateException e) {
                Log.d(TAG, "CONNECTED THREAD START FAILED : " + e);
                Log.d(TAG, "CONNECTED THREAD START FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }

        public void closeSocket() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                mmSocket.close();
                connected = false;
            } catch (IOException e2) {
                //insert code to deal with this
                Log.d(TAG, e2.toString());
                Log.d(TAG, "SOCKET CLOSING FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }
    }

    public void sendData(String str) {
        byte[] buffer = str.getBytes();
        write(buffer);
        //write(str);
    }

    //private void write(String data) {
        private void write(byte[] data) {
        if (mConnectingThread != null) {
            try {
                mConnectingThread.mmSocket.getOutputStream().write(data);
            } catch (IOException e) {
                Log.d (TAG, "EXCEPTION: unable to write data to btSocket");
                mark_BT_disconnected();
                e.printStackTrace();
            }
        } else {
            Log.d (TAG, "UNABLE to get mConnectingThread");
        }

        /*if (mConnectedThread != null) {
            Log.d (TAG, "Sending string in mConnectedThread");
            mConnectedThread.write(data + newline);
        } else {
            Log.d (TAG, "NEW ERROR: UNABLE to get mConnectedThread");

        }*/
    }

    // New Class for Connected Thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private BluetoothSocket btSocket;

        //creation of the connect thread
        ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "IN CONNECTED THREAD");
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, e.toString());
                Log.d(TAG, "UNABLE TO READ/WRITE, STOPPING SERVICE");
                mark_BT_disconnected();
                stopSelf();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            btSocket = socket;
        }

        public void run() {
            Log.d(TAG, "IN CONNECTED THREAD RUN");
            byte[] buffer = new byte[256];
            int bytes;

            mark_BT_connnected();

            // Keep looping to listen for received messages
            while (true && !stopThread) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    //Log.d("BT-DEBUG", "DEBUG_BT_PART: CONNECTED THREAD " + readMessage);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                    Log.d(TAG, "UNABLE TO READ/WRITE, STOPPING SERVICE");
                    stopSelf();
                    break;
                }
            }
        }

        private void resetConnection() {
            Log.d (TAG, "Trying to reset connections - disconnecting...");
            if (mmInStream != null) {
                try {mmInStream.close();} catch (Exception e) {}
                //mmInStream = null;
            }

            if (mmOutStream != null) {
                try {mmOutStream.close();} catch (Exception e) {}
                //mBTOutputStream = null;
            }

            closeStreams();
            try {
                btSocket.close();
                btSocket = null;
            } catch (IOException e) {
                Log.d(TAG, "IOException when trying to close socket.");
            }
            //btSocket.disconnect();

        }


        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Log.d(TAG, "UNABLE TO READ/WRITE " + e);
                Log.d(TAG, "UNABLE TO READ/WRITE, STOPPING SERVICE");
                stopSelf();
            }
        }

        public void closeStreams() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                mmInStream.close();
                mmOutStream.close();
            } catch (IOException e2) {
                //insert code to deal with this
                Log.d(TAG, e2.toString());
                Log.d(TAG, "STREAM CLOSING FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }
    }

    private class AsyncTaskBTOff extends AsyncTask<Object, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            /*p = new ProgressDialog(MainActivity.this);
            p.setMessage("Please wait...It is downloading");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();*/
        }
        @Override
        protected String doInBackground(Object... params) {
            try {
                SharedPreferences prefs = getSharedPreferences(BT_CONNECTED_PREFS, Context.MODE_PRIVATE);
                SharedPreferences.Editor spEditor = prefs.edit();
                spEditor.clear();
                spEditor.putBoolean("CONNECTED", false);
                spEditor.apply();
                Log.d(TAG, "BT connection marked as false in the sp file");

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String results) {

        }
    }
}
