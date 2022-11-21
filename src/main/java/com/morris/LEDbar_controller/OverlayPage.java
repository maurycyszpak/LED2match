package com.morris.LEDbar_controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

public class OverlayPage extends Activity {
    public static String TAG = "MORRIS-splash";
    private static boolean active = false;
    private Handler handler;
    private Runnable my_runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.overlay_page);
        register_filters();
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        animate_image();
        Log.d(TAG, "Starting force_finish() in 10 seconds");
        force_finish(10);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        Log.d(TAG, "Unregistering received when STOPPED activity");
        handler.removeCallbacks(my_runnable);
        active = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        Log.d(TAG, "Unregistering received when PAUSED activity");
    }



    private void animate_image() {
        ImageView iv = findViewById(R.id.image_comms_splash);
        RotateAnimation anim = new RotateAnimation(0.0f, 360.0f , Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatCount(Animation.INFINITE);
        anim.setDuration(1500);
        iv.setAnimation(anim);
        iv.startAnimation(anim);
    }

    private void force_finish(int time_s) {
        my_runnable = new Runnable() {
            public void run() {
                no_connection_popup();
                //OverlayPage.this.finish();
            }
        };
        handler = new Handler();
        handler.postDelayed(my_runnable, time_s * 1000);
    }

    private void no_connection_popup() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to be the layout file of the alertdialog builder
        alertDialogBuilder.setMessage(R.string.overlay_no_connection_msg);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> {
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
                    OverlayPage.this.finish();
                });

        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
    }


    private void register_filters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("controller_data_refreshed_event");
        filter.addAction("request_preset_event");
        filter.addAction("timers_received_event");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            if (intent == null) {
                return;
            }
            if (intent.getAction().equals("request_preset_event")) {
                String message = intent.getStringExtra("counter");
                Log.d(TAG, "Request_preset_event received - extra message (counter): " + message);
            } else if (intent.getAction().equals("controller_data_refreshed_event")) {
                Log.d(TAG, "controller data refreshed - intent received. Closing page.");
                handler.removeCallbacks(my_runnable);
                finish();

            } else if (intent.getAction().equals("timers_received_event")) {
                Log.d(TAG, "Timers refreshed - intent received. Closing page.");
                handler.removeCallbacks(my_runnable);
                finish();
            }

        }
    };


    public void makeToast (String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}