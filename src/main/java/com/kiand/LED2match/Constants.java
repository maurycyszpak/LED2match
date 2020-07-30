package com.kiand.LED2match;

class Constants {

    // values have to be globally unique
    //static final String INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect";    // static final String NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel";
    //static final String INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity";
    public static final String TL84_TAG = "TL84";
    public static final String SHAREDPREFS_CURRENT_LAMPS= "lamps_current_values"; //Mauricio
    public static final String SHAREDPREFS_LAMP_DEFINITIONS = "presets_definition"; //Mauricio
    public static final String CONFIG_SETTINGS = "config_settings";
    public static final String SHAREDPREFS_LAMP_ASSIGNMENTS = "lamp_button_assignments";
    public static final String PRESETS_DEFINITION = "presets_definition";
    public static final String SHAREDPREFS_CONTROLLER_FILEIMAGE = "LEDbar.json"; //Mauricio
    public static final String PREFS_PSU_CURRENT = "psu_max_power";
    public static final String SP_LAMP_TIMERS = "sequence_lamp_timers"; //Mauricio
    public static final String SP_SEQUENCE_COMMAND_GENERATE = "sequence_command_generated"; //Mauricio
    public static final String EXTENDED_LAMPS_MODE_TAG = "XTNDD_MODE"; //Mauricio




    public static final int DEFAULT_PSU_POWER = 2200;
    public static final String sNewLine = System.getProperty("line.separator");

    // values have to be unique within each app
    static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;

    private Constants() {}
}
