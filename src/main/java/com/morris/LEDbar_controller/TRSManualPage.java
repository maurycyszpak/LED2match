package com.morris.LEDbar_controller;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class TRSManualPage extends Activity {

    public static final String SHAREDPREFS_ONE_OFF_SEEKBARS = "one-off-seekbar-values.txt"; //Mauricio
    public static final String newLine = System.getProperty("line.separator");
    public static final String TAG = "MORRIS-MANUAL-PG";


    boolean blLamp1_ON, blLamp2_ON, blLamp3_ON, blLamp4_ON, blLamp5_ON, blLamp6_ON;
    Button btnBack;

    private Handler lclHandler;
    private UsbCOMMsService lclUsbServiceInstance;
    boolean mBound = false;
    final Context context = this;
    private FileUtilities fileUtilities;


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

        Log.d(TAG, "onResume_() - kicking off manual content check");
        set_manual_content();
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
        setContentView(R.layout.trs_manual_page);

        lclHandler = new Handler();
        btnBack = findViewById(R.id.btnBack);
        //TextView tv = findViewById(R.id.textview_manual);
        //tv.setMovementMethod(new ScrollingMovementMethod());
        //populateButtonNames();
        fileUtilities = new FileUtilities(get_path_to_customer_datafile(), get_path_to_customer_logofile());
    }

    public void goBack(View v){
        finish();
    }

    private void set_manual_content() {
        if (display_custom_manual()) {
            // get the content from XML file
            String custom_manual_content = fileUtilities.get_value_from_customer_data(Constants.CUSTOMER_DATA_MANUAL_CONTENT);
            Log.d(TAG, "Setting customer manual content. Custom manual content length: " + custom_manual_content.length());
            if (custom_manual_content.length() > 0) {
                WebView wv = findViewById(R.id.manual_webview);

                String wv_mimetype = "text/html";
                String wv_charset = "UTF-8";
                wv.loadData(custom_manual_content, wv_mimetype, wv_charset);
            }

        } else {
            WebView wv = findViewById(R.id.manual_webview);

            String wv_mimetype = "text/html";
            String wv_charset = "UTF-8";
            String manual_content = getString(R.string.ledbar_manual);
            manual_content.replace("\n", "\\n").replace("\r", "\\r");
            wv.loadData(manual_content, wv_mimetype, wv_charset);
        }
    }

    private boolean display_custom_manual() {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, Context.MODE_PRIVATE);
        Boolean bl_custom_manual = prefs.getBoolean(Constants.CUSTOMER_DATA_FLAG, false);
        Log.d(TAG, "display_custom_manual_(): USE CUSTOMER DATA = " + bl_custom_manual);
        return bl_custom_manual;
    }

    private String get_path_to_customer_datafile() {
        PackageManager m = getPackageManager();
        String s = getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return s + "/" + Constants.CUSTOMER_DATA_FILENAME;
    }

    private String get_path_to_customer_logofile() {
        PackageManager m = getPackageManager();
        String s = getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return s + "/" + Constants.CUSTOMER_LOGO_FILENAME;
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
