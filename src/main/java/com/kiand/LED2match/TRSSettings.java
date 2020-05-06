package com.kiand.LED2match;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import static com.kiand.LED2match.BtCOMMsService.BT_CONNECTED_PREFS;

public class TRSSettings extends Activity implements ServiceConnection {

    public static final String SHAREDPREFS_ONE_OFF_SEEKBARS = "one-off-seekbar-values.txt"; //Mauricio
    public static final String TIME_OFF_STORAGE = "shutdown__timer"; //Mauricio
    public static final String CONFIG_SETTINGS = "config_settings";
    public static final String newLine = System.getProperty("line.separator");

    public static final String TL84_DELAY_KEY = "TL84_delay";
    public static final int TL84_DELAY_DEFAULT = 600;

    Button btnSave;
    Switch aSwitch;

    private BtCOMMsService lclBTServiceInstance;
    public String TAG = "MORRIS-BLOW";
    private Handler lclHandler;
    private UsbCOMMsService lclUsbServiceInstance;
    private BtCOMMsService btService;
    boolean mBound = false;
    boolean mBoundBT = false;
    final Context context = this;
    private LightSettings.MyHandler mHandler;
    public final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private boolean bl_bluetooth_forced_on;
    private CountDownTimer shutdownTimer;

    EditText editOff_h;
    EditText editOff_m;
    EditText edit_TL84_delay;



    private BroadcastReceiver btReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();

            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (bluetoothState) {
                    case BluetoothAdapter.STATE_ON:
                        if (bl_bluetooth_forced_on) {
                            Log.d(TAG, " *** Calling start BT Service");
                            startBluetoothService();
                        }
                        break;
                }
            }

        }
    };
    private BroadcastReceiver btReceiverBTdevice = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action){
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.d(TAG, "Broadcast receiver: ACL_CONNECTED");
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.d(TAG, "Broadcast receiver: ACL_DISCONNECTED");
                    break;
            }
            Bundle bundle = intent.getExtras();

        }
    };

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

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        //BtCOMMsService.MyBinder b = (BtCOMMsService.MyBinder) binder;
        //btService = b.getService();
        //Toast.makeText(LightAdjustments.this, "666 Connected 666", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        btService = null;
    }

    public final ServiceConnection btConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            btService = ((BtCOMMsService.MyBinder) arg1).getService();
            btService.setHandler(mHandler);
            mBoundBT = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            btService = null;
            mBoundBT = false;
        }
    };

    protected void onResume()
    {
        super.onResume();
        //Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();

        editOff_h = findViewById(R.id.editAutoShutOFF_h);
        editOff_m = findViewById(R.id.editAutoShutOFF_m);
        edit_TL84_delay = findViewById(R.id.edit_TL84_delay);

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
        }

        if (check_for_BT_connection()) {
            aSwitch.setChecked(true);
        }

        if (check_for_shutdown_timer_h() > 0) {
            editOff_h.setText(String.valueOf(check_for_shutdown_timer_h()));
        }
        if (check_for_shutdown_timer_m() > 0) {
            editOff_m.setText(String.valueOf(check_for_shutdown_timer_m()));
        }

        int delay = check_for_TL84_delay();
        if (delay > 0) {
            edit_TL84_delay.setText(String.valueOf(check_for_TL84_delay()));
        } else {
            edit_TL84_delay.setText(String.valueOf(TL84_DELAY_DEFAULT));
        }
    }


    int check_for_shutdown_timer_h() {
        SharedPreferences prefs = getSharedPreferences(TIME_OFF_STORAGE, 0);
        return prefs.getInt("hours", 0);
    }

    int check_for_shutdown_timer_m() {
        SharedPreferences prefs = getSharedPreferences(TIME_OFF_STORAGE, 0);
        return prefs.getInt("minutes", 0);
    }

    int check_for_TL84_delay() {
        SharedPreferences prefs = getSharedPreferences(CONFIG_SETTINGS, 0);
        return prefs.getInt(TL84_DELAY_KEY, 0);
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
        mBound = false;
        mBoundBT = false;
        Log.d(TAG, "onSTOP - unbinding services");
        //Toast.makeText(this.getBaseContext(),"Activity stopped", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"Service unbound", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this.getBaseContext(),"mBound = " + mBound, Toast.LENGTH_SHORT).show();
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isMyServiceRunning(BtCOMMsService.class)) {
            Log.d(TAG, "BtCOMMS srvc is running. Unregistering and unbinding from btService");
            try {
                unregisterReceiver(btReceiver);
                unregisterReceiver(btReceiverBTdevice);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "IllegalArgumentException");
                e.printStackTrace();
            }

            if (mBoundBT) {
                try {
                    unbindService(btConnection);
                } catch (IllegalArgumentException e) {
                    Log.d(TAG, "Illegal Argument Exception");
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trs_settings);

        //editOff_h = findViewById(R.id.editAutoShutOFF_h);
        //editOff_m = findViewById(R.id.editAutoShutOFF_m);

        lclHandler = new Handler();

        btnSave = findViewById(R.id.btnSave);
        btnSave.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnSave.setTextColor(Color.WHITE);

        aSwitch = findViewById(R.id.switch_one);
        aSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            if (btAdapter.isEnabled()) {
                                Toast.makeText(TRSSettings.this,
                                        "Switching On...", Toast.LENGTH_SHORT).show();



                                Log.d(TAG, " **** starting Bluetooth connection service");
                                //if (!isMyServiceRunning(BtCOMMsService.class)) {
                                if (!bl_bluetooth_forced_on) {
                                    startBluetoothService();
                                }

                                //check if we're already connected
                                if (!check_for_BT_connection()) {
                                    Intent intent = new Intent(TRSSettings.this, TRSBluetoothDevicesScan.class);
                                    startActivity(intent); //or start activity for result? this should be "modal"
                                }

                            } else {
                                aSwitch.setChecked(false);
                                bl_bluetooth_forced_on = true;
                                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
                            }

                        } else {
                            Toast.makeText(TRSSettings.this,
                                    "Switch Off", Toast.LENGTH_SHORT).show();

                            //Switching off
                            SharedPreferences prefs = getSharedPreferences(BT_CONNECTED_PREFS, Context.MODE_PRIVATE);
                            SharedPreferences.Editor spEditor = prefs.edit();

                            spEditor.clear();
                            spEditor.putBoolean("CONNECTED", false);
                            spEditor.commit();
                            Log.d(TAG, "BT connection marked as false in the sp file - calling service disconnect.");
                            //sendBroadcast(new Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED));
                            btService.disconnect();
                            //Log.d(TAG, "Sending broadcast to disconnect");

                        }
                    }
                });

        setFiltersBT();
        setFiltersBTdevice();
    }

    private boolean check_for_BT_connection() {
        SharedPreferences prefs = getSharedPreferences(BT_CONNECTED_PREFS, Context.MODE_PRIVATE);

        //aSwitch.setChecked(true);
        return prefs.getBoolean("CONNECTED", false);
    }

    private void startBluetoothService() {
        /*setFiltersBT();
        setFiltersBTdevice();*/
        //Log.d(TAG, "BtCOMMS srvc is not running. Setting filters and starting ");
        Intent intent = new Intent(this, BtCOMMsService.class);
        //this.startService(new Intent(this, BtCOMMsService.class));
        this.startService(intent);
        //bindService(intent, btConnection, Context.BIND_AUTO_CREATE);
        aSwitch.setChecked(true);
        //startService(BtCOMMsService.class, btConnection, null); // Start BtCOMMsService(if it was not started before) and Bind it
    }

    /*private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        //if (!UsbCOMMsService.SERVICE_CONNECTED) {
        Intent startService = new Intent(this, service);
        //startService.putExtra("USB_connection", usbConnection);
        if (extras != null && !extras.isEmpty()) {
            Set<String> keys = extras.keySet();
            for (String key : keys) {
                String extra = extras.getString(key);
                startService.putExtra(key, extra);
            }
        }
        startService(startService);
        //}
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }*/

    private void setFiltersBT() {
        IntentFilter filterBT = new IntentFilter();
        filterBT.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filterBT.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filterBT.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filterBT.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filterBT.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btReceiver, filterBT);
    }

    private void setFiltersBTdevice() {
        IntentFilter filterBTdevice = new IntentFilter();
        filterBTdevice.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filterBTdevice.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(btReceiverBTdevice, filterBTdevice);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    boolean validate_tl84_delay(Integer value) {
        return value >= 100 && value <= 1200;
    }

    public void saveSettings(View v) {
        EditText edit_bltl84delay = findViewById(R.id.edit_TL84_delay);
        boolean bl_edit_tl84delay = false;
        Log.d(TAG, "Entering function saveSettings.");
        int iHours = 0, iMinutes = 0;
        if (editOff_h.getText().toString().isEmpty()) { iHours = 0; } else { iHours = Integer.valueOf(editOff_h.getText().toString()); }
        if (editOff_m.getText().toString().isEmpty()) { iMinutes = 0; } else { iMinutes = Integer.valueOf(editOff_m.getText().toString()); }

        SharedPreferences prefs_config = getSharedPreferences(CONFIG_SETTINGS, 0);
        SharedPreferences prefs = getSharedPreferences(TIME_OFF_STORAGE, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putInt("hours", iHours);
        editor.putInt("minutes", iMinutes);
        editor.apply();

        if (!TextUtils.isEmpty(edit_bltl84delay.getText().toString())) {
            if (!validate_tl84_delay(Integer.valueOf(edit_TL84_delay.getText().toString()))) {
                makeToast("NO BUENO");
                bl_edit_tl84delay = false;
            } else {
                bl_edit_tl84delay = true;
            }
        }




        if (bl_edit_tl84delay) {
            SharedPreferences.Editor edit_config = prefs_config.edit();
            edit_config.remove(TL84_DELAY_KEY);
            edit_config.putInt(TL84_DELAY_KEY, Integer.valueOf(edit_bltl84delay.getText().toString()));
            edit_config.apply();
            makeToast("TL84 delay of " + edit_bltl84delay.getText().toString() + "ms stored in the config file.");
        }

        Long lTimeToOFF = (long) (iHours * 60 * 60 + iMinutes * 60);
        if (lTimeToOFF > 0) {
            Log.d(TAG, "Setting timer on - switching OFF all lamps in " + iHours + " hours and " + iMinutes + " minutes (=" + lTimeToOFF + " seconds).");
            Toast.makeText(TRSSettings.this, "Timer ON\nAll lamps off in " + iHours + " hours and " + iMinutes + " minutes.", Toast.LENGTH_LONG).show();

            String sCommand = "I" + String.format("%04X", lTimeToOFF) + "$" + newLine;
            Log.d(TAG, "*** strtol 4 HEX " + sCommand);
            sCommand = "I" + String.format("%02X", lTimeToOFF) + "$" + newLine;
            Log.d(TAG, "*** strtol 2 HEX " + sCommand);
            if (mBoundBT) {
                btService.sendData(sCommand);
            } else {
                Log.d(TAG, "Service btService not connected!");
            }
            Log.d(TAG, "*** ALLOFF *** Sending 'shutdown' intent to BtComms");

            if (shutdownTimer != null) {
                shutdownTimer.cancel();
            }
            switchOFFAfterX(iHours, iMinutes);
            /*new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Log.d(TAG, "Time is up - switching all lamps off");
                            Toast.makeText(TRSSettings.this, "Time is up - switching all lamps off", Toast.LENGTH_LONG).show();
                        }
                    });
                    allOFF();
                }
            }, lTimeToOFF);*/
        }
    }

    public void allOFF() {
        String sCommand = "S00000000000000000000";
        sCommand += "$" + newLine;
        //if (btService.connected) {
        if (mBoundBT) {
            //Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
            btService.sendData(sCommand);
        } else {
            Log.d(TAG, "Service btService not connected!");
        }
        Log.d(TAG, "*** ALLOFF *** Sending 'shutdown' intent to BtComms");
        Intent serviceIntent= new Intent(TRSSettings.this,BtCOMMsService.class);
        serviceIntent.putExtra("shutdown", "1");
        startService(serviceIntent);

        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
        SharedPreferences prefs = getSharedPreferences(TIME_OFF_STORAGE, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    public void switchOFFAfterX(int hours, int minutes) {

            shutdownTimer = new CountDownTimer(hours*60*60*1000 + minutes*60*1000 + 30, 10000) { //30ms is for shits and giggles
                public void onTick(long millisUntilFinished) {
                    Log.d(TAG, " *** TICK *** " + millisUntilFinished/1000 + " secs left until shutdown");
                }

                public void onFinish() {
                    Log.d(TAG, "*** TICKING OVER *** Time is up - switching all lamps off");
                    allOFF();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(TRSSettings.this, "Countdown timer is up - switching off all lamps.", Toast.LENGTH_LONG).show();
                        }
                    });

                }
            }.start();
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

    public void switch_preset_on () {

    }

    public void makeToast (String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
