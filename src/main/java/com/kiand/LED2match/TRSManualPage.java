package com.kiand.LED2match;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class TRSManualPage extends Activity {

    public static final String SHAREDPREFS_ONE_OFF_SEEKBARS = "one-off-seekbar-values.txt"; //Mauricio
    public static final String newLine = System.getProperty("line.separator");

    boolean blLamp1_ON, blLamp2_ON, blLamp3_ON, blLamp4_ON, blLamp5_ON, blLamp6_ON;
    Button btnBack;

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
        TextView tv = findViewById(R.id.textview_manual);
        tv.setMovementMethod(new ScrollingMovementMethod());

        //populateButtonNames();
    }

    public void goBack(View v){
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

    public void switch_preset_on () {

    }

    public void makeToast (String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
