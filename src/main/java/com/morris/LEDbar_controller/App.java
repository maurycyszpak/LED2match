package com.morris.LEDbar_controller;

import android.app.Application;
import android.content.Context;

/**
 * Created by Mauricio on 20/02/2016.
 */
public class App extends Application {
    public static Context context;

    @Override public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }
}