package com.kiand.LED2match;

import android.app.Application;
import android.content.Context;

/**
 * Created by Maurycy on 15.04.2017.
 */

public class MyApplication extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }
}
