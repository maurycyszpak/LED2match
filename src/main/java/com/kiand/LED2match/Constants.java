package com.kiand.LED2match;

class Constants {

    public static final int MAX_PRESET_NUM = 10;
    public static final String TL84_TAG = "TL84";
    public static final String SHAREDPREFS_CURRENT_LAMPS= "lamps_current_values"; //Mauricio
    public static final String SHAREDPREFS_LAMP_DEFINITIONS = "presets_definition"; //Mauricio
    public static final String SHAREDPREFS_DIAGNOSTIC_DATA = "LED2match.debug.txt"; //Mauricio
    public static final String CONFIG_SETTINGS = "config_settings";
    public static final String SHAREDPREFS_LAMP_ASSIGNMENTS = "lamp_button_assignments";
    public static final String NEW_SHAREDPREFS_LAMP_ASSIGNMENTS = "button_lamp_mapping";
    public static final String PRESETS_DEFINITION = "presets_definition";
    public static final String PRESETS_DEFINITION_JSONFILE = "presets_definition.json";
    public static final String SHAREDPREFS_CONTROLLER_FILEIMAGE = "LEDbar.json"; //Mauricio
    public static final String PREFS_PSU_CURRENT = "psu_max_power";
    public static final String SP_LAMP_TIMERS = "sequence_lamp_timers"; //Mauricio
    public static final String SP_SEQUENCE_COMMAND_GENERATE = "sequence_command_generated"; //Mauricio
    public static final String EXTENDED_LAMPS_MODE_TAG = "XTNDD_MODE"; //Mauricio
    public static final String CUSTOMER_DATA_ARCHIVE_FILENAME = "LED2match_customer_data.zip"; //Mauricio
    public static final String CUSTOMER_LOGO_FILENAME = "customer_logo.png"; //Mauricio
    public static final String CUSTOMER_DATA_FILENAME = "customer_data.xml"; //Mauricio
    public static final String CUSTOMER_DATA_ABOUT_PAGE_LINE_1_TAG = "about_page_line_1";
    public static final String CUSTOMER_DATA_ABOUT_PAGE_LINE_2_TAG = "about_page_line_2";
    public static final String CUSTOMER_DATA_ABOUT_PAGE_LINE_3_TAG = "about_page_line_3";
    public static final String CUSTOMER_DATA_ABOUT_PAGE_LINE_4_TAG = "about_page_line_4";
    public static final String CUSTOMER_DATA_ABOUT_PAGE_HYPERLINK = "about_page_hyperlink";
    public static final String CUSTOMER_DATA_MANUAL_CONTENT = "manual_content";
    public static final String CUSTOMER_DATA_FLAG = "USE_CUSTOMER_DATA";
    public static final String CUSTOMER_LOGO_FLAG = "USE_CUSTOMER_LOGO";
    public static final String CUSTOMER_ZIPFILE_PROCESSED = "CUSTOMER_ZIPFILE_PROCESSED";

    public static final String BT_CONNECTED_PREFS = "bluetooth_connection_status";

    public static final String SESSION_TIER_TAG = "SESSION_TIER";
    public static final String SESSION_CONNECTED_MAC_TAG = "CONNECTED_MAC_ADDRESS";
    public static final String MAC_ADDRESS_PREFERENCES_TAG = SESSION_CONNECTED_MAC_TAG;
    public static final String LICENSE_TIER_TAG = "LICENSE_TIER";
    public static final String LICENSE_MAC_ADDR_TAG = "LICENSE_MAC_ADDRESS";
    public static final String LICENSE_DATE_TAG = "LICENSE_DATE";

    public static final int LICENSE_TIER_DIGITAL_PANEL_PAGE = 0;
    public static final int LICENSE_TIER_MANUAL_PAGE = 0;
    public static final int LICENSE_TIER_OPERATING_HOURS_PAGE  = 0;
    public static final int LICENSE_TIER_LICENSE_PAGE  = 0;
    public static final int LICENSE_TIER_SEQUENCE_SETTINGS_PAGE  = 1;
    public static final int LICENSE_TIER_SETTINGS_PAGE  = 1;
    public static final int LICENSE_TIER_MAINTENANCE_PAGE = 2;
    public static final int LICENSE_TIER_LIGHT_SETTINGS_PAGE  = 2;


    public static final int DEFAULT_PSU_POWER = 2200;
    public static final String sNewLine = System.getProperty("line.separator");

    // values have to be unique within each app
    static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;

    private Constants() {}
}
