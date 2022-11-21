package com.morris.LEDbar_controller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class DisabledOverlayPage extends Activity {
    public static String TAG = "MORRIS-blockedpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.disabled_overlay_page);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed");
        Intent setIntent = new Intent(DisabledOverlayPage.this, TRSDigitalPanel.class);
        finish();
        startActivity(setIntent);
    }

    public void makeToast (String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}