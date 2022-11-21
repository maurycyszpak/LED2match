package com.morris.LEDbar_controller;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import static com.morris.LEDbar_controller.Constants.CONFIG_SETTINGS;
import static com.morris.LEDbar_controller.Constants.SP_LAMP_TIMERS;
import static com.morris.LEDbar_controller.Constants.SP_SEQUENCE_COMMAND_LOCAL;
import static com.morris.LEDbar_controller.TRSDigitalPanel.SP_SEQUENCE_COMMAND_EXECUTED;
import static com.morris.LEDbar_controller.TRSDigitalPanel.SP_SEQUENCE_COMMAND_GENERATED;


public class TRSSequence extends ListActivity {
    ArrayList<String> listItems = new ArrayList<String>();
    private BaseAdapter adapter;
    final Context context = this;
    Switch aSwitch;
    private UsbCOMMsService lclUsbServiceInstance;
    private BtCOMMsService lclBTServiceInstance;
    boolean mBound = false;
    boolean mBoundBT = false;
    public ArrayList<String> spnPresetsArrayList = new ArrayList<>(); // Mauricio
    public static final String TAG = "MORRIS-SQNC";

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
    public final ServiceConnection btConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            lclBTServiceInstance = ((BtCOMMsService.MyBinder) arg1).getService();
            //lclBTServiceInstance.setHandler(mHandler);
            mBoundBT = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            lclBTServiceInstance = null;
            mBoundBT = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listItems.clear();
        read_previous_sequence();
        //clear_lamp_timers_sp_file();

        if (!mBoundBT) {
            Intent intent = new Intent(this, BtCOMMsService.class);
            //startService(intent);
            bindService(intent, btConnection, Context.BIND_AUTO_CREATE);
            //Toast.makeText(this.getBaseContext(),"Service bound (onResume)", Toast.LENGTH_SHORT).show();

            SystemClock.sleep(50);
            mBoundBT = true;
        }

        Log.d(TAG, "Executing license check");
        boolean connected = get_connection_status();
        if (connected) {
            license_check();
        }
    }

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trs_sequence_page);

        ArrayList<HashMap<String,String>> list = new ArrayList<>();
        adapter = new ArrayAdapter<String>(
                this,
                R.layout.prg_sequence_listitem_view,
                R.id.title1,
                listItems);
        setListAdapter(adapter);

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {

                //makeToast("essa: " + pos);
                //resetList();
                edit_or_remove(pos, id);
                return true;
            }
        });
        aSwitch = findViewById(R.id.switch_infinite_loop);
        aSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        SharedPreferences sp = getSharedPreferences(CONFIG_SETTINGS, 0);
                        SharedPreferences.Editor sp_editor = sp.edit();
                        sp_editor.clear();
                        sp_editor.apply();

                        if (isChecked) {
                            sp_editor.putBoolean("LOOP_SEQUENCE", true);
                            sp_editor.apply();

                        } else {
                            sp_editor.putBoolean("LOOP_SEQUENCE", false);
                            sp_editor.apply();
                        }
                    }
                });
    }


    @Override
    protected void onListItemClick(ListView list, View v, int position, long id) {
        super.onListItemClick(list, v, position, id);
        String itemValue = (String) list.getItemAtPosition(position);
        add_seconds_to_step(position, 5);
        //
        // makeToast("Click : \n  Position :"+position+"  \n  ListItem : " +itemValue);
    }

    private void recalculate_list() {
        clear_local_sequence_file();
        for (String seq_step_entry: listItems) {
            int index = listItems.indexOf(seq_step_entry);
            String sLampName = seq_step_entry.substring(seq_step_entry.indexOf(".") + 2, seq_step_entry.indexOf(":"));
            String sLampDelay = seq_step_entry.substring(seq_step_entry.indexOf(":") + 2, seq_step_entry.length() - 1);
            String concat_value = (index +1) + ". " + sLampName + ": " + sLampDelay + "s";
            listItems.set(index, concat_value);
            Log.d(TAG, "Step: " + index+1 + ", preset: " + sLampName + ", Timer: " + sLampDelay);
            writeSequenceToFile(sLampName, Integer.valueOf(sLampDelay), String.valueOf(index+1));
        }
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

        if (current_tier < Constants.LICENSE_TIER_SEQUENCE_SETTINGS_PAGE) {
            Log.d(TAG, "Blocking page");
            block_current_page();
        } else {
            Log.d(TAG, "Not blocking page");
        }
    }

    private void block_current_page() {
        Intent intent = new Intent(TRSSequence.this, DisabledOverlayPage.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK) ;
        startActivity(intent);

    }

    private void edit_or_remove(int seq_item_position, long seq_item_id) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set prompts.xml to be the layout file of the alertdialog builder
        alertDialogBuilder.setMessage(R.string.prg_sequence_longpress_prompt);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("EDIT", (dialog, id) -> {
                    //makeToast("I will EDIT item from position: " + seq_item_position + "(id: " + seq_item_id + ")");
                    edit_sequence_step(seq_item_position);
                })

                .setNeutralButton("REMOVE", (dialog, id) -> {
                    //makeToast("I will REMOVE item from position: " + seq_item_position + "(id: " + seq_item_id + ")");
                    listItems.remove(seq_item_position);
                    recalculate_list();
                    adapter.notifyDataSetChanged();

                })
                .setNegativeButton("Cancel",
                        (dialog, id) -> dialog.cancel());

        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
        alertD.show();
    }

    public void open_dialog_add_item(View v) {
        open_add_item_dialog(v);
    }

    public void add_item(String value) {
        int count = listItems.size();
        value = String.valueOf(++count).concat(". ").concat(value);
        listItems.add(value);
        Log.d (TAG, "add_item()_ - Item added. Now the list has : " + listItems.size() + " items");
        adapter.notifyDataSetChanged();
    }

    public void resetList() {
        listItems.clear();
        adapter.notifyDataSetChanged();
    }

    public void updateLampHEXsequence (String sKey, String sSequence) {
        SharedPreferences spLampTimers = getSharedPreferences(SP_LAMP_TIMERS, 0);
        SharedPreferences.Editor editorLampTimer = spLampTimers.edit();

        editorLampTimer.remove(sKey);
        editorLampTimer.putString(sKey, sSequence);
        Log.d(TAG , "Updating sp file with key: " + sKey + " and sequence: " + sSequence);
        editorLampTimer.commit();
    }

    public String convertRGBwithCommasToHexString(String sRGB) {
        String sValue = "";

        String[] stringArray = sRGB.split(",");
        for (int i = 0; i < stringArray.length; i++) {
            String numberAsString = stringArray[i];
            int iValue = Integer.parseInt(numberAsString);
            sValue += String.format("%02X", iValue);
        }
        sValue = sValue.toUpperCase();
        return sValue;
    }

    private void edit_sequence_step(int step_id) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View add_item_view = layoutInflater.inflate(R.layout.prg_sequence_add_item_layout, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set prompts.xml to be the layout file of the alertdialog builder
        alertDialogBuilder.setView(add_item_view);
        final Spinner spinner_control_preset_list = add_item_view.findViewById(R.id.spinner_new_lamp);
        final Spinner spinner_delay = add_item_view.findViewById(R.id.spinner_lamp_delay);


        // setup a dialog window
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("SAVE", (dialog, id) -> {
                    String concat_value = (step_id + 1) + ". " + spinner_control_preset_list.getSelectedItem().toString();
                    concat_value += ": " + spinner_delay.getSelectedItem().toString() + "s";
                    listItems.set(step_id, concat_value);
                    adapter.notifyDataSetChanged();
                    recalculate_list();

                })
                .setNegativeButton("Cancel",
                        (dialog, id) -> dialog.cancel());

        extractPresetsFromJSONfile();
        if (spnPresetsArrayList.size() > 0) {

            String[] delays = {"5", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "60"};

            ArrayAdapter spnPresetsAdapter = new ArrayAdapter<String>(this, R.layout.sequence_add_item_spinner_text, spnPresetsArrayList); // Mauricio
            spnPresetsAdapter.setDropDownViewResource(R.layout.sequence_add_item_spinner_dropdown);
            spinner_control_preset_list.setAdapter(spnPresetsAdapter); // Mauricio

            ArrayAdapter spnDelaysAdapter = new ArrayAdapter<String>(this, R.layout.sequence_add_item_spinner_text, delays); // Mauricio
            spnDelaysAdapter.setDropDownViewResource(R.layout.sequence_add_item_spinner_dropdown);
            spinner_delay.setAdapter(spnDelaysAdapter); // Mauricio

            String sLampName = listItems.get(step_id).substring(listItems.get(step_id).indexOf(".") + 2, listItems.get(step_id).indexOf(":"));
            String sLampDelay = listItems.get(step_id).substring(listItems.get(step_id).indexOf(":") + 2, listItems.get(step_id).length() - 1);
            //makeToast("Will look for delay: '" + sLampDelay + "'");
            //makeToast("Will look for lamp name: '" + sLampName + "'");

            spinner_control_preset_list.setSelection(getSpinnerIndex(spinner_control_preset_list, sLampName), true);
            spinner_delay.setSelection(getSpinnerIndex(spinner_delay, sLampDelay), true);

            spnPresetsAdapter.notifyDataSetChanged();
            spnDelaysAdapter.notifyDataSetChanged();
        }




        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
    }

    private int getSpinnerIndex(Spinner spinner, String compareString) {
        for (int i=0; i<spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(compareString)) {
                return i;
            }
        }
        return 0;
    }

    private void open_add_item_dialog(final View view) {

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View add_item_view = layoutInflater.inflate(R.layout.prg_sequence_add_item_layout, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set prompts.xml to be the layout file of the alertdialog builder
        alertDialogBuilder.setView(add_item_view);
        final Spinner spinner_control_preset_list = add_item_view.findViewById(R.id.spinner_new_lamp);
        final Spinner spinner_delay = add_item_view.findViewById(R.id.spinner_lamp_delay);


        // setup a dialog window
        alertDialogBuilder
            .setCancelable(false)
            .setPositiveButton("ADD", (dialog, id) -> {
                String concat_value = spinner_control_preset_list.getSelectedItem().toString();
                concat_value += ": " + spinner_delay.getSelectedItem().toString() + "s";

                add_item(concat_value);
                writeSequenceToFile(spinner_control_preset_list.getSelectedItem().toString(),Integer.valueOf(spinner_delay.getSelectedItem().toString()), String.valueOf(listItems.size()));
                //listItems.add(concat_value);
                adapter.notifyDataSetChanged();

            })
            .setNegativeButton("Cancel",
                    (dialog, id) -> dialog.cancel());

        extractPresetsFromJSONfile();
        if (spnPresetsArrayList.size() > 0) {

            String[] delays = {"5", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "60"};

            ArrayAdapter spnPresetsAdapter = new ArrayAdapter<String>(this, R.layout.sequence_add_item_spinner_text, spnPresetsArrayList); // Mauricio
            spnPresetsAdapter.setDropDownViewResource(R.layout.sequence_add_item_spinner_dropdown);
            spinner_control_preset_list.setAdapter(spnPresetsAdapter); // Mauricio


            ArrayAdapter spnDelaysAdapter = new ArrayAdapter<String>(this, R.layout.sequence_add_item_spinner_text, delays); // Mauricio
            spnDelaysAdapter.setDropDownViewResource(R.layout.sequence_add_item_spinner_dropdown);
            spinner_delay.setAdapter(spnDelaysAdapter); // Mauricio

            spnPresetsAdapter.notifyDataSetChanged();
            spnDelaysAdapter.notifyDataSetChanged();
        }




        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
    }

    public void extractPresetsFromJSONfile() {

        try {
            Log.d(TAG, "inside costam");

            spnPresetsArrayList.clear();
            JSONObject jsonObject = new JSONObject(getBodyOfJSONfile());
            Iterator<String> keysIterator = jsonObject.keys();
            while (keysIterator.hasNext()) {
                String key = keysIterator.next();
                String value = jsonObject.getString(key);
                if (key.contains("_nm") && value.length() > 0) {
                    spnPresetsArrayList.add(value);
                    //Log.d(TAG, "key: " + key + ", value: " + value + "(" + value.length() + ")");
                }
                if (key.contains("_def")) {
                    //Log.d(TAG, "key: " + key + ", value: " + value);
                }
            }
        } catch (JSONException ioe) {
            ioe.printStackTrace();
        }
        //Collections.sort(spnPresetsArrayList);
        adapter.notifyDataSetChanged();
    }

    private String getBodyOfJSONfile() {
        String readString = "";
        try {

            PackageManager m = getPackageManager();
            String s = getPackageName();
            try {
                PackageInfo p = m.getPackageInfo(s, 0);
                s = p.applicationInfo.dataDir;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            //Log.d(TAG, "PATH: " + s);
            File file = new File(s + "/files/" + Constants.PRESETS_DEFINITION_JSONFILE);
            FileInputStream fin = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            readString = new String(sb);
            reader.close();
            fin.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return readString;
    }

    public String getRGBValues(String sPresetName) {
        String sPresetRGB = "";
        String sPresetDefinitions = getBodyOfJSONPresetsfile();
        String sPresetRGBValues = null;
        JSONObject jsonPresets = null;
        Log.d(TAG, "getRGBValues_() - fetching RGB of preset name '" + sPresetName + "'.");

        try {
            int slot = findGivenPresetSlot(sPresetName, sPresetDefinitions);
            jsonPresets = new JSONObject(sPresetDefinitions);
            sPresetRGBValues = jsonPresets.getString("preset" + slot + "_rgbw");
            Log.d(TAG, "btnClicked_UV_normal_() - Found RGB values of preset'" + sPresetName + "': " + sPresetRGBValues);
            sPresetRGB = convertRGBwithCommasToHexString(sPresetRGBValues);
        } catch (NullPointerException | JSONException npe) {
            npe.printStackTrace();
            makeToast("ERROR: Unable to build JSON object with presets definition. Does the correct file exist in the correct path?");
            Log.e(TAG, "JSON Object of presets definition is null or JSON exception encountered.");
        }

        return sPresetRGB;
    }

    private String getBodyOfJSONPresetsfile() {
        String readString = "";
        try {

            PackageManager m = getPackageManager();
            String s = getPackageName();
            try {
                PackageInfo p = m.getPackageInfo(s, 0);
                s = p.applicationInfo.dataDir;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "PATH: " + s);
            File file = new File(s + "/files/" + Constants.PRESETS_DEFINITION_JSONFILE);
            FileInputStream fin = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            readString = new String(sb);
            reader.close();
            fin.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return readString;
    }

    public int findGivenPresetSlot(String presetName, String jsonString) {
        int i = -1;
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            Iterator<String> keysIterator = jsonObject.keys();
            int j =0;
            while (keysIterator.hasNext()) {
                String key = keysIterator.next();
                String value = jsonObject.getString(key);

                if (key.contains("_nm")) {
                    j++;
                    if (value.equalsIgnoreCase(presetName)) {
                        return j;
                    }
                }
            }
        } catch (JSONException ioe) {
            ioe.printStackTrace();
        }
        return i;
    }

    public void generateSequenceCommand(View v) {
        String sSequencePrefix = "X201";
        Date dteNow = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String sPresetName = "";
        int iCounter = 0;
        String sNewSequenceCommand = "";

        SharedPreferences sp = getSharedPreferences(CONFIG_SETTINGS, 0);
        SharedPreferences.Editor sp_editor = sp.edit();

        if (aSwitch.isChecked()) {
            sp_editor.putBoolean("LOOP_SEQUENCE", true);
            sp_editor.apply();

        } else {
            sp_editor.putBoolean("LOOP_SEQUENCE", false);
            sp_editor.apply();
        }
        SharedPreferences spLampTimers = getSharedPreferences(SP_LAMP_TIMERS, 0);
        SharedPreferences.Editor editorLampTimer = spLampTimers.edit();
        editorLampTimer.clear();
        editorLampTimer.apply();

        int iTotal = 0;
        for (String seq_step_entry: listItems) {
            //makeToast("Processing '" + seq_step_entry + "'"); // e.g. '1. D65: 10s', '2. A: 5s'
            try {
                sPresetName = seq_step_entry.substring(seq_step_entry.indexOf(".") + 2, seq_step_entry.indexOf(":")); // e.g. 'D65, 'A'
                if (sPresetName.length() > 0) {
                    String seq_step_timer = seq_step_entry.substring(seq_step_entry.indexOf(":") + 2, seq_step_entry.length() - 1); //e.g. 10, 5
                    Log.d(TAG, "Processing preset '" + sPresetName + "' with timer '" + seq_step_timer + "'");

                    iTotal++;
                    sNewSequenceCommand = sNewSequenceCommand.concat(sPresetName).concat(":").concat(String.format("%04d", Integer.valueOf(seq_step_timer))).concat(",");
                    writeSequenceToFile(sPresetName, Integer.valueOf(seq_step_timer), String.valueOf(++iCounter));
                }
            } catch ( IndexOutOfBoundsException e ) {
                makeToast("Unable to process sequence step: '" + seq_step_entry + "'");
            }
        }
        if (listItems.size() > 0) {
            Toast.makeText(this, "Sequence data stored on mobile device.\nPress PRG on main panel to run", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Nothing to be saved", Toast.LENGTH_SHORT).show();
        }

        //Moved from Digital Panel
        String sSequence = sSequencePrefix;
//        duplicateSPFile();
//        spLampTimers = getSharedPreferences(Constants.SP_LAMP_TIMERS, 0);
//        SharedPreferences spLampSequence = getSharedPreferences(SP_SEQUENCE_COMMAND_GENERATE_NEW, 0);
//
//        TreeMap<String, ?> allEntries = new TreeMap<String, Object>(spLampSequence.getAll());
//        int i = 1;
//        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
//            String sVal = entry.getValue().toString();
//            String[] sBuffer = sVal.split(",");
//            sSequence = sSequence.concat("0" + i);
//            sSequence = sSequence.concat(sBuffer[1]);
//            encodedPresetRGBs.add("0" + i + sBuffer[1]);
//            encodedPresetNames.add(sBuffer[0]);
//            Log.d(TAG, "Currently sSequence: " + entry.getKey() + ": " + sSequence);
//            i++;
//        }
        //strip last comma
        sNewSequenceCommand = sNewSequenceCommand.substring(0, sNewSequenceCommand.length()-1);
//        sSequence = sSequence.concat("^").concat((check_sequence_for_loop()) ? "1" : "0");
//        sSequence = sSequence.concat(System.lineSeparator());

        String sResult = sNewSequenceCommand.concat("^").concat((check_sequence_for_loop()) ? "1" : "0");
        sSequence = sSequencePrefix + String.format("%02d", iTotal) + "," + sResult + System.lineSeparator();
        Log.d(TAG, "Plomien 81: " + sResult);

        //lclBTServiceInstance.sendData(sSequence);
        lclBTServiceInstance.sendData(sSequence);
        //lclUsbServiceInstance.sendBytes(sSequence.getBytes());

        Log.d(TAG, "Sending Bytes: " + sSequence);

        SystemClock.sleep(200);
    }

    public void onClickBack (View v) {
        finish();
    }

    /*private void generate_sequence_ver2 () {
        String sVal;
        String sKey;
        ArrayList<String> encodedPresetNames = new ArrayList<>();
        ArrayList<String> encodedPresetRGBs = new ArrayList<>();

        //For each pair of key-> value add item to the list

        Map<String, ?> allEntries = spsValues.getAll();
        int i = 1;
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            sKey = entry.getKey();
            sVal = entry.getValue().toString();

            Log.d(TAG, "Sending '" + sVal + "' to convert to HEX");
            encodedPresetRGBs.add(convertRGBwithCommasToHexString(sVal));
            encodedPresetNames.add(sKey);
        }

        String sSize;
        if (encodedPresetNames.size() < 10) {
            sSize =  "0" + encodedPresetNames.size();
        } else {
            sSize = String.valueOf(encodedPresetNames.size());
        }

        String sCommand = "Q" + sSize + TextUtils.join("", encodedPresetRGBs) + TextUtils.join(",", encodedPresetNames) + "$\n";
        Log.d(TAG, sCommand);
        if (lclBTServiceInstance.connected) {
            Log.d(TAG, "New way to send the presets: '" + sCommand.replace("\n", "\\n").replace("\r", "\\r") + "'");
            lclBTServiceInstance.sendData(sCommand);
        } else {
            Log.d(TAG, "Service btService not connected!");
        }
    }*/

    public void duplicateSPFile() {
        //sp1 is the shared pref to copy to

        SharedPreferences prefsFrom = getSharedPreferences(SP_SEQUENCE_COMMAND_GENERATED, 0);
        SharedPreferences prefsTo = getSharedPreferences(SP_SEQUENCE_COMMAND_EXECUTED, 0);
        SharedPreferences.Editor ed = prefsTo.edit();
        //SharedPreferences sp = prefsTo; //The shared preferences to copy from
        ed.clear(); // This clears the one we are copying to, but you don't necessarily need to do that.

        for(Map.Entry<String,?> entry : prefsFrom.getAll().entrySet()){
            Object v = entry.getValue();
            String key = entry.getKey();
            //Now we just figure out what type it is, so we can copy it.
            // Note that i am using Boolean and Integer instead of boolean and int.
            // That's because the Entry class can only hold objects and int and boolean are primatives.
            if(v instanceof Boolean)
                // Also note that i have to cast the object to a Boolean
                // and then use .booleanValue to get the boolean
                ed.putBoolean(key, ((Boolean)v).booleanValue());
            else if(v instanceof Float)
                ed.putFloat(key, ((Float)v).floatValue());
            else if(v instanceof Integer)
                ed.putInt(key, ((Integer)v).intValue());
            else if(v instanceof Long)
                ed.putLong(key, ((Long)v).longValue());
            else if(v instanceof String)
                ed.putString(key, ((String)v));
        }
        ed.commit(); //save it.
    }

    boolean check_sequence_for_loop() {
        boolean flag;
        SharedPreferences sp = getSharedPreferences(CONFIG_SETTINGS, 0);
        flag = sp.getBoolean("LOOP_SEQUENCE", false);

        return flag;
    }

    private int get_prefs_contents_size(String prefs_file_name) {
        int iSize=0;
        SharedPreferences prefs = getSharedPreferences(prefs_file_name, MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            iSize++;
        }
        return iSize;
    }

    public void writeSequenceToFile(String sLampName, Integer iTimeToDisplay, String seq_id) {
        SharedPreferences new_prefs = getSharedPreferences(SP_SEQUENCE_COMMAND_LOCAL, 0);
        SharedPreferences.Editor new_editor = new_prefs.edit();

        JSONArray new_jsonArray = new JSONArray();
        new_jsonArray.put(sLampName);
        new_jsonArray.put(iTimeToDisplay);

        Log.d(TAG, "Writing '" + new_jsonArray + "' to Sequence file under id: '" + seq_id + "'");
        new_editor.putString(seq_id, new_jsonArray.toString());
        new_editor.apply();
    }

    private void clear_local_sequence_file() {
        SharedPreferences spLampTimers = getSharedPreferences(SP_SEQUENCE_COMMAND_LOCAL, 0);
        SharedPreferences.Editor editorLampTimer = spLampTimers.edit();
        editorLampTimer.clear();
        editorLampTimer.apply();
    }

    public void resetSequenceCommand(View v) {
        clear_local_sequence_file();
        listItems.clear();
        adapter.notifyDataSetChanged();
    }

    private void add_seconds_to_step(int step_id, int seconds_accrual) {
        String seq_step_entry = listItems.get(step_id);
        String sPresetName = seq_step_entry.substring(seq_step_entry.indexOf(".") + 2, seq_step_entry.indexOf(":"));
        if (sPresetName.length() > 0) {
            String seq_step_timer = seq_step_entry.substring(seq_step_entry.indexOf(":") + 2, seq_step_entry.length() - 1);
            int new_time = Integer.valueOf(seq_step_timer) + seconds_accrual;
            String concat_value = sPresetName + ": " + new_time + "s";
            int count = Integer.valueOf(seq_step_entry.substring(0, seq_step_entry.indexOf(".")));
            String value = String.valueOf(count).concat(". ").concat(concat_value);
            listItems.set(step_id, value);
            adapter.notifyDataSetChanged();
        }
    }

    private void read_previous_sequence() {
        SharedPreferences spLampTimers = getSharedPreferences(SP_SEQUENCE_COMMAND_LOCAL, 0);
        TreeMap<String, ?> allEntries = new TreeMap<String, Object>(spLampTimers.getAll());
        int i = 1;
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String sVal = entry.getValue().toString();
            sVal = sVal.replaceAll("\\[", "");
            sVal = sVal.replaceAll("]", "");
            sVal = sVal.replaceAll("\"", "");
            Log.d(TAG, "Iterating over step: '" + sVal + "'.");
            String[] sBuffer = sVal.split(",");
            String sSequenceStep = i++ + ". ";
            sSequenceStep += sBuffer[0] + ": ";
            int timer = Integer.valueOf(sBuffer[1]);
            sSequenceStep += timer + "s";

            listItems.add(sSequenceStep);
        }
        adapter.notifyDataSetChanged();
    }

    public void makeToast (String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }




}