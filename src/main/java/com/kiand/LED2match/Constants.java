package com.kiand.LED2match;

class Constants {

    // values have to be globally unique
    static final String INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect";
    static final String NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel";
    static final String INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity";
    static final String TL84_TAG = "TL84";
    static final String SHAREDPREFS_CURRENT_LAMPS= "lamps_current_values"; //Mauricio
    static final String SHAREDPREFS_LAMP_DEFINITIONS = "presets_definition"; //Mauricio
    static final String CONFIG_SETTINGS = "config_settings";
    static final String SHAREDPREFS_LAMP_ASSIGNMENTS = "lamp_button_assignments";
    public static final String SHAREDPREFS_CONTROLLER_FILEIMAGE = "LEDbar.json"; //Mauricio

    // values have to be unique within each app
    static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;

    private Constants() {}
}
