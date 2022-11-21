package com.morris.LEDbar_controller;

import android.app.Application;
import android.content.Context;

/**
 * Created by Maurycy on 15.04.2017.
 */

public class MainApplication extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        MainApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return MainApplication.context;
    }
}
