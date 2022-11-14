package com.kiand.LED2match;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import static com.kiand.LED2match.Constants.CONFIG_SETTINGS;
import static com.kiand.LED2match.Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE;
import static com.kiand.LED2match.Constants.sNewLine;

public class TRSMaintenancePage extends Activity {

    public static final String SHAREDPREFS_ONE_OFF_SEEKBARS = "one-off-seekbar-values.txt"; //Mauricio
    public static final String PREFS_DAC_VALUE = "dac-value";
    public static final String prefs_psu_value_tag = "psu_current";
    public static final String newLine = System.getProperty("line.separator");
    public static final String NO_PRESET_TEXT = "#n/a";
    public static final String TAG = "MORRIS-MNTNC";

    boolean blLamp1_ON, blLamp2_ON, blLamp3_ON, blLamp4_ON, blLamp5_ON, blLamp6_ON;
    Button btnL1, btnL2, btnL3, btnL4, btnL5, btnL6;
    EditText edit_psu;
    EditText edit_tl84_dim;
    EditText edit_tl84_full;
    EditText edit_no_of_panels;
    EditText edit_temp_corr_factor;
    Integer btn1Timer = 0;
    Integer btn2Timer = 0;
    Integer btn3Timer = 0;
    Integer btn4Timer = 0;
    Integer btn5Timer = 0;
    Integer btn6Timer = 0;

    Switch aSwitch;

    private Handler lclHandler;
    private UsbCOMMsService lclUsbServiceInstance;
    private BtCOMMsService lclBTServiceInstance;
    boolean mBound = false;
    boolean mBoundBT = false;
    final Context context = this;


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

    private final ServiceConnection btConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BtCOMMsService.MyBinder binder = (BtCOMMsService.MyBinder) service;
            lclBTServiceInstance = binder.getService();
            mBoundBT = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBoundBT = false;
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

        if (!mBoundBT) {
            Intent intent = new Intent(this, BtCOMMsService.class);
            bindService(intent, btConnection, Context.BIND_AUTO_CREATE);
            //Toast.makeText(this.getBaseContext(),"Service bound (onResume)", Toast.LENGTH_SHORT).show();

            SystemClock.sleep(50);
            mBoundBT = true;
        }

        //populateLampsState();
        populate_unit_config();
        //readDACvalue();
        //readPSUpower();
        if (shared_prefs_exists(Constants.NEW_SHAREDPREFS_LAMP_ASSIGNMENTS, "666")) {
            repopulate_button_assignments();
        } else {
            populateButtonNames();
        }
        populate_extended_lamps_switch_value();
        boolean connected = get_connection_status();
        if (connected) {
            license_check();
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
        setContentView(R.layout.trs_maintenance_page);
        edit_psu = findViewById(R.id.edit_PSU_current);
        edit_tl84_dim = findViewById(R.id.edit_DACvalue);
        edit_tl84_full = findViewById(R.id.edit_TL84_fullbright_value);
        edit_no_of_panels = findViewById(R.id.edit_no_of_panels);
        edit_temp_corr_factor = findViewById(R.id.edit_temp_corr_factor);
        lclHandler = new Handler();

        btnL1 = findViewById(R.id.btnL1);
        btnL2 = findViewById(R.id.btnL2);
        btnL3 = findViewById(R.id.btnL3);
        btnL4 = findViewById(R.id.btnL4);
        btnL5 = findViewById(R.id.btnL5);
        btnL6 = findViewById(R.id.btnL6);


        btnL1.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL1.setTextColor(Color.WHITE);

        btnL2.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL2.setTextColor(Color.WHITE);

        btnL3.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL3.setTextColor(Color.WHITE);

        btnL4.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL4.setTextColor(Color.WHITE);

        btnL5.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL5.setTextColor(Color.WHITE);

        btnL6.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL6.setTextColor(Color.WHITE);



        btnL1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                resetOperatingTime(view);
                return true;
            }
        });

        btnL2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                resetOperatingTime(view);
                return true;
            }
        });

        btnL3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                resetOperatingTime(view);
                return true;
            }
        });

        btnL4.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                resetOperatingTime(view);
                return true;
            }
        });

        btnL5.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                resetOperatingTime(view);
                return true;
            }
        });

        btnL6.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                resetOperatingTime(view);
                return true;
            }
        });
        aSwitch = findViewById(R.id.switch_extended_lamps_mode);
        aSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor spEditor = prefs.edit();

                        if (isChecked) {
                            Toast.makeText(TRSMaintenancePage.this,"Extended Lamps mode ON", Toast.LENGTH_SHORT).show();
                            spEditor.putBoolean(Constants.EXTENDED_LAMPS_MODE_TAG, true);
                        } else {
                            Toast.makeText(TRSMaintenancePage.this, "Extended Lamps mode OFF", Toast.LENGTH_SHORT).show();
                            spEditor.putBoolean(Constants.EXTENDED_LAMPS_MODE_TAG, false);
                        }
                        spEditor.apply();
                    }
                });
    }

    public void button_click(View v) {
        makeToast(v.getTag().toString());
    }

    private void populate_extended_lamps_switch_value() {
        SharedPreferences prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, Context.MODE_PRIVATE);
        Boolean bl_extended_mode = prefs.getBoolean(Constants.EXTENDED_LAMPS_MODE_TAG, false);

        aSwitch.setChecked(bl_extended_mode);
    }

    public void factoryReset(View v) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        View promptView = layoutInflater.inflate(R.layout.factory_reset_confirmation_view, null);

        // set prompts.xml to be the layout file of the alertdialog builder
        alertDialogBuilder.setView(promptView);


        // setup a dialog window
        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton("OK", (dialog, id) -> {
                    //brace for reset!
                    delete_all_shared_prefs();
                    String sCommand = "T" + newLine;
                    send_via_bt(sCommand);
                    restart_app();

                })
                .setNegativeButton("Cancel",
                        (dialog, id) -> dialog.cancel());

        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.setOnShowListener( new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alertD.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.dark_gray));
                //alertD.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(getResources().getColor(R.color.hts_gray));

                alertD.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.red_slider));
                //alertD.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(getResources().getColor(R.color.hts_gray));
            }
        });
        alertD.show();
    }

    public int get_tier() {
        int current_tier = 0;
        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        try {
            current_tier = Integer.parseInt(json_analyst.getJSONValue("license_tier"));
            Log.d(TAG, "get_tier_() - returning TIER " + current_tier);
            return current_tier;
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            Log.w(TAG, "Unable to parse tier '" + json_analyst.getJSONValue("license_tier") + "' as a number");
            return 0;
        }
    }

    public boolean get_connection_status() {
        SharedPreferences prefs_config = getSharedPreferences(Constants.BT_CONNECTED_PREFS, 0);
        boolean status= prefs_config.getBoolean("CONNECTED", false);

        return status;
    }

    private void license_check() {
        int current_tier = get_tier();

        if (current_tier < Constants.LICENSE_TIER_MAINTENANCE_PAGE) {
            Log.d(TAG, "Blocking page");
            block_current_page();
        } else {
            Log.d(TAG, "Not blocking page");
        }
    }

    private void block_current_page() {
        Intent intent = new Intent(TRSMaintenancePage.this, DisabledOverlayPage.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK) ;
        startActivity(intent);

    }

    private void send_via_bt(String command) {
        if (mBoundBT) {
            Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + command.replace("\n", "\\n").replace("\r", "\\r") + "'");
            lclBTServiceInstance.sendData(command);
        } else {
            Log.d(TAG, "Service btService not connected!");
        }
    }

    void delete_all_shared_prefs() {
        String sPath = "/data/data/"+ getPackageName()+ "/shared_prefs/";
        //makeToast(sPath);
        File sharedPreferenceFile = new File(sPath);
        File[] listFiles = sharedPreferenceFile.listFiles();
        for (File file : listFiles) {
            file.delete();
        }
    }

    void restart_app() {
        try{
            Intent mStartActivity = new Intent(context, TRSDigitalPanel.class);
            int mPendingIntentId = 123456;
            PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void populate_unit_config() {
        Log.d(TAG, "Populating page with unit config values");
        String ee_psucurrent_tag = "eeprom_PSU_current";
        String ee_tl84dim_tag = "eeprom_tl84_dim_value";
        String ee_tl84full_tag = "eeprom_tl84_full_value";
        String ee_noofpanels_tag = "eeprom_no_of_panels";
        String ee_tempcorrfactor_tag = "eeprom_temp_corr_factor";

        SharedPreferences spConfig = getSharedPreferences(CONFIG_SETTINGS, 0);
        String s_eeprom_PSU_current = spConfig.getString(ee_psucurrent_tag, "");
        String s_eeprom_tl84_dim_value = spConfig.getString(ee_tl84dim_tag, "");
        String s_eeprom_tl84_full_value = spConfig.getString(ee_tl84full_tag,"");
        String s_eeprom_no_of_panels = spConfig.getString(ee_noofpanels_tag, "1");
        String s_eeprom_temp_corr_factor = spConfig.getString(ee_tempcorrfactor_tag, "1");


        try {
            if (s_eeprom_PSU_current.length() > 0) {
                if (Integer.valueOf(s_eeprom_PSU_current) != 0) {
                    edit_psu.setText(s_eeprom_PSU_current);
                }
            }
        } catch (NullPointerException e) {
            makeToast("PSU current not yet defined for this unit");
        }

        try {
            if (s_eeprom_tl84_dim_value.length() > 0) {
                if (Integer.valueOf(s_eeprom_tl84_dim_value) != 0) {
                    edit_tl84_dim.setText(s_eeprom_tl84_dim_value);
                }
            }
        } catch (NullPointerException e) {
            makeToast("TL84 dim value not yet defined for this unit");
        }

        try {
            if (s_eeprom_tl84_full_value.length() > 0) {
                if (Integer.valueOf(s_eeprom_tl84_full_value) != 0) {
                    edit_tl84_full.setText(s_eeprom_tl84_full_value);
                }
            }
        } catch (NullPointerException e) {
            makeToast("Full brightness TL84 dim value not yet defined for this unit");
        }

        try {
            if (s_eeprom_no_of_panels.length() > 0) {
                if (Integer.parseInt(s_eeprom_no_of_panels) != 0) {
                    edit_no_of_panels.setText(s_eeprom_no_of_panels);
                }
            }
        } catch (NullPointerException e) {
            makeToast("Full brightness TL84 dim value not yet defined for this unit");
        }

        try {
            if (s_eeprom_temp_corr_factor.length() > 0) {
                if (Integer.parseInt(s_eeprom_temp_corr_factor) != 0) {
                    float f_factor = Integer.parseInt(s_eeprom_temp_corr_factor);
                    edit_temp_corr_factor.setText(Float.toString(f_factor / 100));
                }
            }
        } catch (NullPointerException e) {
            makeToast("Temperature correction factor not yet defined for this unit");
        }

    }

    public boolean shared_prefs_exists(String sFileName, String sKey) {
        SharedPreferences spFile = getSharedPreferences(sFileName, 0);
        return spFile.contains(sKey);
    }

    public void repopulate_button_assignments() {
        btnL1.setText(NO_PRESET_TEXT);
        btnL1.setTag(NO_PRESET_TEXT);
        btnL2.setText(NO_PRESET_TEXT);
        btnL2.setTag(NO_PRESET_TEXT);
        btnL3.setText(NO_PRESET_TEXT);
        btnL3.setTag(NO_PRESET_TEXT);
        btnL4.setText(NO_PRESET_TEXT);
        btnL4.setTag(NO_PRESET_TEXT);
        btnL5.setText(NO_PRESET_TEXT);
        btnL5.setTag(NO_PRESET_TEXT);
        btnL6.setText(NO_PRESET_TEXT);
        btnL6.setTag(NO_PRESET_TEXT);

        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        // now check which preset is currently mapped to a particular button
        // e.g.: key--> 1, value --> preset3
        SharedPreferences prefs = getSharedPreferences(Constants.NEW_SHAREDPREFS_LAMP_ASSIGNMENTS, MODE_PRIVATE);
        TreeMap<String, ?> keys = new TreeMap<String, Object>(prefs.getAll());

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            switch (entry.getKey()) {
                case "1":
                    // preset8. Get name of this preset
                    String preset_name = json_analyst.getJSONValue("p1_nm");
                    btnL1.setText(entry.getValue().toString());
                    btnL1.setText(preset_name);
                    btnL1.setTag(entry.getValue().toString());
                    //btnL1.setTag(preset_name);
                    break;

                case "2":
                    preset_name = json_analyst.getJSONValue("p2_nm");
                    btnL2.setText(entry.getValue().toString());
                    btnL2.setText(preset_name);
                    btnL2.setTag(entry.getValue().toString());
                    break;

                case "3":
                    preset_name = json_analyst.getJSONValue("p3_nm");
                    btnL3.setText(entry.getValue().toString());
                    btnL3.setText(preset_name);
                    btnL3.setTag(entry.getValue().toString());
                    break;

                case "4":
                    preset_name = json_analyst.getJSONValue("p4_nm");
                    btnL4.setText(entry.getValue().toString());
                    btnL4.setText(preset_name);
                    btnL4.setTag(entry.getValue().toString());
                    break;

                case "5":
                    preset_name = json_analyst.getJSONValue("p5_nm");
                    btnL5.setText(entry.getValue().toString());
                    btnL5.setText(preset_name);
                    btnL5.setTag(entry.getValue().toString());
                    break;

                case "6":
                    preset_name = json_analyst.getJSONValue("p6_nm");
                    btnL6.setText(entry.getValue().toString());
                    btnL6.setText(preset_name);
                    btnL6.setTag(entry.getValue().toString());
                    break;
            }
        }
    }


    public void resetOperatingTime(View v) {

        //placeholder
        if (!v.getTag().toString().equalsIgnoreCase("#N/A")) {
            Button b = (Button) v;
            String tag = v.getTag().toString();
            String button_text = b.getText().toString();
            String msg = "You are about to the reset operating hours of the lamp: '" + button_text + "'.\n\nClick OK to continue.";
            openDialog(v, msg, v.getTag().toString(), button_text);
        } else {
            makeToast("No lamp assigned to this button");
        }

    }

    public void openDialog(final View view, String message, String preset_indicator, String preset_name) {

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptView = layoutInflater.inflate(R.layout.okcancel, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set prompts.xml to be the layout file of the alertdialog builder
        alertDialogBuilder.setView(promptView);
        String preset = preset_indicator.replaceAll("preset", "");

        // setup a dialog window
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        makeToast("Resetting timer of preset: " + preset_name);
                        String sCommand = "X," + preset_name + "$" + sNewLine;
                        Log.d(TAG, "Resetting timer of preset: " + preset_name);
                        if (mBoundBT) {
                            Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                            lclBTServiceInstance.sendData(sCommand);
                        } else {
                            Log.d(TAG, "Service btService not connected when sending message: '" + sCommand + "'");
                        }
                        lclUsbServiceInstance.sendBytes(sCommand.getBytes());
                    }

                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
    }

    public void onClickBack (View v) {
        finish();
    }


    public void openLightPage(View v) {
        Intent intent = new Intent(TRSMaintenancePage.this, LightSettings.class);
        startActivity(intent);

    }

    public void onClickReassign(View v) {

        String sTags = "";
        if (!btnL1.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL1.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL2.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL2.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL3.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL3.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL4.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL4.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL5.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL5.getText().toString() + ","; } else { sTags = sTags + ","; }
        if (!btnL6.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL6.getText().toString() + ","; } else { sTags = sTags + ","; }
        //if (!btnL10.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL10.getText().toString() + ","; } else { sTags = sTags + ","; }
        //if (!btnL11.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL11.getText().toString() + ","; } else { sTags = sTags + ","; }
        //if (!btnL12.getText().toString().equalsIgnoreCase(NO_PRESET_TEXT)) { sTags = sTags + btnL12.getText().toString() + ","; } else { sTags = sTags + ","; }


        SharedPreferences spFile = getSharedPreferences(SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);
        String sPresetCounter = json_analyst.getJSONValue("preset_counter");

        int iCtr = ((!sPresetCounter.trim().equals("") ? Integer.valueOf(sPresetCounter) : 0));

        Intent intentLampAssignment = new Intent(TRSMaintenancePage.this, ReassignLamps.class);
        intentLampAssignment.putExtra("tags", sTags);
        intentLampAssignment.putExtra("counter", iCtr);
        startActivity(intentLampAssignment);
    }

    public void populateButtonNames() {
        //String sUnitName = "";
        SharedPreferences spFile = getSharedPreferences(Constants.SHAREDPREFS_CONTROLLER_FILEIMAGE, 0);
        JSON_analyst json_analyst = new JSON_analyst(spFile);

        //Log.d(TAG, "bluetoothAskReply(V1)");
        final String sLamp1Name = json_analyst.getJSONValue("p1_nm");
        final String sLamp2Name = json_analyst.getJSONValue("p2_nm");
        final String sLamp3Name = json_analyst.getJSONValue("p3_nm");
        final String sLamp4Name = json_analyst.getJSONValue("p4_nm");
        final String sLamp5Name = json_analyst.getJSONValue("p5_nm");
        final String sLamp6Name = json_analyst.getJSONValue("p6_nm");
        //final String sLamp4Name = extractJSONvalue("", "lamp4_name");

        setLampName(1, sLamp1Name);
        setLampName(2, sLamp2Name);
        setLampName(3, sLamp3Name);
        setLampName(4, sLamp4Name);
        setLampName(5, sLamp5Name);
        setLampName(6, sLamp6Name);
        //setLampName(4, sLamp4Name);
    }

    public void saveSettings(View v) {
        //We need to store the DAC Value if present
        boolean bl_valid_DAC_value;
        boolean bl_valid_psu_power = false;
        boolean bl_valid_temp_corr = false;
        float temp_corr_factor = 0.0f;
        String new_settings_value = "";
        String arduino_command_prefix = "X301,";
        String var_name;
        String var_value;



        String settings_value = "";
        if (!TextUtils.isEmpty(edit_tl84_full.getText().toString())) {
            settings_value += string_int_to_hex_4(edit_tl84_full.getText().toString());
            var_name = "TL84_FULL_VALUE";
            var_value = edit_tl84_full.getText().toString();
            new_settings_value += arduino_command_prefix + var_name + ":" + var_value + ";";
        } else {
            settings_value += "0000";
        }

        if (!TextUtils.isEmpty(edit_tl84_dim.getText().toString())) {
            settings_value += string_int_to_hex_4(edit_tl84_dim.getText().toString());
            var_name = "TL84_DIM_VALUE";
            var_value = edit_tl84_dim.getText().toString();
            new_settings_value += "," + var_name + ":" + var_value + ";";
        } else {
            settings_value += "0000";
        }

        if (!TextUtils.isEmpty(edit_psu.getText().toString())) {
            settings_value += string_int_to_hex_4(edit_psu.getText().toString());
            var_name = "MAX_PSU_CURRENT";
            var_value = edit_psu.getText().toString();
            new_settings_value += "," + var_name + ":" + var_value + ";";
        } else {
            settings_value += "0000";
        }

        if (!TextUtils.isEmpty(edit_no_of_panels.getText().toString())) {
            settings_value += string_int_to_hex_4(edit_no_of_panels.getText().toString());
            var_name = "NO_OF_PANELS";
            var_value = edit_no_of_panels.getText().toString();
            new_settings_value += "," + var_name + ":" + var_value + ";";
        } else {
            settings_value += "0000";
        }

        if (!TextUtils.isEmpty(edit_temp_corr_factor.getText().toString())) {
            String strFactor = edit_temp_corr_factor.getText().toString();
            float i = Float.parseFloat(strFactor);
            double d = (double) Math.round(i *100)/100;
            d *= 100;
            settings_value += string_int_to_hex_4(String.valueOf((int)d));
            var_name = "TEMP_CORR_FACTOR";
            var_value = String.valueOf(d);
            new_settings_value += "," + var_name + ":" + var_value + ";";
            Log.d(TAG, "Passing '" + i + "' to int_to_hex function");
        } else {
            settings_value += "0000";
        }

        Log.d(TAG, "Sending unit config string: " + settings_value);
        /*if (value < 600 || value > 800) {
            makeToast("DAC value should be between 600 and 800.");
            bl_valid_DAC_value = false;
            //return;
        } else {
            bl_valid_DAC_value = true;
        }*/

        /*Log.d(TAG, "DAC value of '" + value + "' stored in prefs file");
        makeToast( "DAC value of '" + value + "' successfully stored.");*/

        //validate temp correction
        try {
            temp_corr_factor = Float.parseFloat(edit_temp_corr_factor.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.d(TAG, "Unable to read number as float");
        }
        bl_valid_temp_corr = validate_temp_corr_factor(temp_corr_factor);

        if (bl_valid_temp_corr) {
            //String sCommand = "M1" + settings_value + "$" + newLine;
            String sCommand = new_settings_value + "$" + newLine;
            if (mBoundBT) {
                Log.d(TAG, "Service btService connected. Calling btService.sendData with message '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
                lclBTServiceInstance.sendData(sCommand);
            } else {
                Log.d(TAG, "Service btService not connected when sending message: '" + sCommand + "'");
            }
            lclUsbServiceInstance.sendBytes(sCommand.getBytes());
        } else {
            makeToast("Configuration settings not sent to controller. Please provide valid values");
        }



        Float ampValue = 0.0f;
        int milliAmpValue = 0;
        EditText ed_amps = findViewById(R.id.edit_PSU_current);
        if (!TextUtils.isEmpty(ed_amps.getText().toString())) {
            ampValue = Float.valueOf(ed_amps.getText().toString());
            try {
                milliAmpValue = Integer.valueOf(ed_amps.getText().toString());
                if (milliAmpValue/1000.0 < 1.0 || milliAmpValue/1000.0 > 120.0) {
                    makeToast("Power supply max current should be between 1 and 120 amps.");
                    bl_valid_psu_power = false;
                } else {
                    bl_valid_psu_power = true;
                }
            } catch (NumberFormatException e) {
                makeToast("PSU power value does not look like an integer value.");
            }
        }



        if (bl_valid_psu_power) {
            SharedPreferences spFile = getSharedPreferences(Constants.PREFS_PSU_CURRENT, 0);
            SharedPreferences.Editor editor = spFile.edit();
            editor.clear();
            //editor.putFloat(prefs_psu_value_tag, ampValue);
            editor.putInt(prefs_psu_value_tag, milliAmpValue);
            editor.apply();
            Log.d(TAG, "PSU current of '" + milliAmpValue + "' stored in prefs file");
            //int value = spFile.getInt("dac_value", 0);
        }
        makeToast( "Saving configuration data on Controller.");

    }

    private boolean validate_temp_corr_factor(float factor) {

        if (factor >= 0.1 && factor <= 2.0) {
            //makeToast("A valid temp correction factor");

            return true;
        }
        makeToast("Invalid temp correction factor. Value should be between 0.1 and 2.00");
        return false;
    }

    public String string_int_to_hex_4(String s) {
        String sValue = "";

        int iValue = Integer.parseInt(s);
        sValue += String.format("%04X", iValue);

        sValue = sValue.toUpperCase();
        return sValue;
    }

    public void setLampName(int i, String sName) {
        if (i == 1) {
            if (sName.length() > 0) {
                btnL1.setText(sName);
                btnL1.setTag("");
            } else {
                btnL1.setText(NO_PRESET_TEXT);
                btnL1.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 2) {
            if (sName.length() > 0) {
                btnL2.setText(sName);
                btnL2.setTag("");
            } else {
                btnL2.setText(NO_PRESET_TEXT);
                btnL2.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 3) {
            if (sName.length() > 0) {
                btnL3.setText(sName);
                btnL3.setTag("");
            } else {
                btnL3.setText(NO_PRESET_TEXT);
                btnL3.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 4) {
            if (sName.length() > 0) {
                btnL4.setText(sName);
                btnL4.setTag("");
            } else {
                btnL4.setText(NO_PRESET_TEXT);
                btnL4.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 5) {
            if (sName.length() > 0) {
                btnL5.setText(sName);
                btnL5.setTag("");
            } else {
                btnL5.setText(NO_PRESET_TEXT);
                btnL5.setTag(NO_PRESET_TEXT);
            }
        } else if (i == 6) {
            if (sName.length() > 0) {
                btnL6.setText(sName);
                btnL6.setTag("");
            } else {
                btnL6.setText(NO_PRESET_TEXT);
                btnL6.setTag(NO_PRESET_TEXT);
            }
        }

    }

    public void switch_all_off() {
        blLamp1_ON = false;
        btnL1.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL1.setTextColor(Color.WHITE);
        blLamp2_ON = false;
        btnL2.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL2.setTextColor(Color.WHITE);
        blLamp3_ON = false;
        btnL3.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL3.setTextColor(Color.WHITE);
        blLamp4_ON = false;
        btnL4.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL4.setTextColor(Color.WHITE);
        blLamp5_ON = false;
        btnL5.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL5.setTextColor(Color.WHITE);
        blLamp6_ON = false;
        btnL6.setBackgroundDrawable(getResources().getDrawable(R.drawable.buttonselector_main));
        btnL6.setTextColor(Color.WHITE);
    }

    public void populateLampsState() {
        SharedPreferences spsValues = getSharedPreferences(LightSettings.SHAREDPREFS_LAMP_STATE, MODE_PRIVATE);
        String sReturn = spsValues.getString("LAMPS", "");
        String[] sLampState = sReturn.split(",");

        //makeToast("Populating lamp states: " + sReturn);

        if (sLampState.length == 5) {
            if (sLampState[1].equals("1")) {
                blLamp1_ON = false;
                btnL1.performClick();
            }

            if (sLampState[2].equals("1")) {
                blLamp2_ON = false;
                btnL2.performClick();
            }

            if (sLampState[3].equals("1")) {
                blLamp3_ON = false;
                btnL3.performClick();
            }

            if (sLampState[4].equals("1")) {
                blLamp4_ON = false;
                btnL4.performClick();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(Menu.NONE, 0, 0, "Light Settings").setIcon(
                getResources().getDrawable(R.drawable.icon_scan));
        menu.add(Menu.NONE, 2, 2, "Operating Hours").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 3, 3, "Sequence Settings (PRG)").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 4, 4, "Settings").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 5, 5, "Manual").setIcon(
                getResources().getDrawable(R.drawable.icon_information));
        menu.add(Menu.NONE, 6, 6, "Digital Panel").setIcon(
                getResources().getDrawable(R.drawable.icon_information));


        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem item = menu.findItem(R.id.menu_color_picker);

        //populateButtonNames();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case 0:
                Intent intent0 = new Intent(TRSMaintenancePage.this, LightSettings.class);
                startActivity(intent0);
                break;

            case 2:
                Intent intent5 = new Intent(TRSMaintenancePage.this, TRSLightOperatingHours.class);
                startActivity(intent5);
                break;

            case 3:
                Intent intent6 = new Intent(TRSMaintenancePage.this, TRSSequence.class);
                startActivity(intent6);
                break;

            case 4:
                Intent intent7 = new Intent(TRSMaintenancePage.this, TRSSettings.class);
                startActivity(intent7);
                break;

            case 5:
                Intent intent8 = new Intent(TRSMaintenancePage.this, TRSManualPage.class);
                startActivity(intent8);
                break;

            case 6:
                Intent intent9 = new Intent(TRSMaintenancePage.this, TRSDigitalPanel.class);
                startActivity(intent9);
                break;

        }
        return true;
    }

    public void switch_preset_on () {

    }

    public void makeToast (String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
