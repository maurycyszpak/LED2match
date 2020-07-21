package com.kiand.LED2match;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import static com.kiand.LED2match.Constants.CONFIG_SETTINGS;
import static com.kiand.LED2match.Constants.PRESETS_DEFINITION;
import static com.kiand.LED2match.Constants.SP_LAMP_TIMERS;
import static com.kiand.LED2match.Constants.SP_SEQUENCE_COMMAND_GENERATE;
import static com.kiand.LED2match.TRSDigitalPanel.TL84_TAG;
import static com.kiand.LED2match.TRSSettings.TL84_DELAY_KEY;


public class TRSSequence extends ListActivity {
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayList<HashMap<String,String>> list;
    private BaseAdapter adapter;
    int clickCounter = 0;
    final Context context = this;
    ListView listView;
    Switch aSwitch;
    public static final String TAG = "MORRIS-LSTVIEW";

    @Override
    protected void onResume() {
        super.onResume();
        listItems.clear();
        read_previous_sequence();
        //clear_lamp_timers_sp_file();
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
        for (String seq_step_entry: listItems) {
            int index = listItems.indexOf(seq_step_entry);
            String sLampName = seq_step_entry.substring(seq_step_entry.indexOf(".") + 2, seq_step_entry.indexOf(":"));
            String sLampDelay = seq_step_entry.substring(seq_step_entry.indexOf(":") + 2, seq_step_entry.length() - 1);

            String concat_value = (index +1) + ". " + sLampName + ": " + sLampDelay + "s";
            listItems.set(index, concat_value);
        }


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

                })
                .setNegativeButton("Cancel",
                        (dialog, id) -> dialog.cancel());

        ArrayList<String> spnPresetsArrayList = extractPresetsFromJson();
        if (spnPresetsArrayList.size() > 0) {

            String[] delays = {"5", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "60"};

            ArrayAdapter spnPresetsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, spnPresetsArrayList); // Mauricio
            spinner_control_preset_list.setAdapter(spnPresetsAdapter); // Mauricio

            ArrayAdapter spnDelaysAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, delays); // Mauricio
            spinner_delay.setAdapter(spnDelaysAdapter); // Mauricio

            String sLampName = listItems.get(step_id).substring(listItems.get(step_id).indexOf(".") + 2, listItems.get(step_id).indexOf(":"));
            String sLampDelay = listItems.get(step_id).substring(listItems.get(step_id).indexOf(":") + 2, listItems.get(step_id).length() - 1);
            makeToast("Will look for delay: '" + sLampDelay + "'");
            makeToast("Will look for lamp name: '" + sLampName + "'");

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
                //listItems.add(concat_value);
                adapter.notifyDataSetChanged();

            })
            .setNegativeButton("Cancel",
                    (dialog, id) -> dialog.cancel());

        ArrayList<String> spnPresetsArrayList = extractPresetsFromJson();
        if (spnPresetsArrayList.size() > 0) {

            String[] delays = {"5", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "60"};

            ArrayAdapter spnPresetsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, spnPresetsArrayList); // Mauricio
            spinner_control_preset_list.setAdapter(spnPresetsAdapter); // Mauricio

            ArrayAdapter spnDelaysAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, delays); // Mauricio
            spinner_delay.setAdapter(spnDelaysAdapter); // Mauricio

            spnPresetsAdapter.notifyDataSetChanged();
            spnDelaysAdapter.notifyDataSetChanged();
        }




        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
    }

    public ArrayList<String> extractPresetsFromJson() {
        SharedPreferences prefs_presets = getSharedPreferences(PRESETS_DEFINITION, 0);
        ArrayList<String> spnPresetsArrayList = new ArrayList<>();
        spnPresetsArrayList.clear();
        Map<String,?> keys = prefs_presets.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            spnPresetsArrayList.add(entry.getKey());
        }
        Collections.sort(spnPresetsArrayList);

        return spnPresetsArrayList;
    }

    public String getRGBValues(String sPresetName) {
        String sPresetRGB = "";

        SharedPreferences spsValues = getSharedPreferences(PRESETS_DEFINITION, 0);
        Map<String, ?> allEntries = spsValues.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String sVal = entry.getKey();
            if (sVal.equalsIgnoreCase(sPresetName)) {
                String sValue = entry.getValue().toString();
                sPresetRGB = convertRGBwithCommasToHexString(sValue);
            }
        }
        return sPresetRGB;
    }

    public void generateSequenceCommand(View v) {

        Date dteNow = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String sTimeStamp = sdf.format(dteNow);
        String sSequence = "D";
        String sPresetName = "";
        int iCounter = 0;
        String sDelay = "0000";

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


        for (String seq_step_entry: listItems) {
            //makeToast("Processing '" + seq_step_entry + "'");
            try {
                sPresetName = seq_step_entry.substring(seq_step_entry.indexOf(".") + 2, seq_step_entry.indexOf(":"));
                if (sPresetName.length() > 0) {
                    String seq_step_timer = seq_step_entry.substring(seq_step_entry.indexOf(":") + 2, seq_step_entry.length() - 1);
                    //makeToast("Processing preset '" + sPresetName + "' with timer '" + seq_step_timer + "'");
                    writeSequenceToFile(sPresetName, Integer.valueOf(seq_step_timer), String.valueOf(++iCounter));

                    String sHEX = getRGBValues(sPresetName);
                    sHEX = sHEX.concat(String.format("%04d", Integer.valueOf(seq_step_timer)));
                    int i_flag_TL84 = 0;
                    if (sPresetName.equalsIgnoreCase(TL84_TAG)) {
                        i_flag_TL84 = 1;
                        sDelay = get_tl84_delay();
                    }
                    //Log.d(TAG, "Calling updateLampHEX with preset:" + sPresetName + ", HEX: " + sHEX);
                    updateLampHEXsequence("SEQ"+iCounter, sPresetName+ "," + sHEX + ((i_flag_TL84 == 1) ? "01" : "00") + sDelay);
                    //generateSequenceCommand
                }


            } catch ( IndexOutOfBoundsException e ) {
                makeToast("Unable to process sequence step: '" + seq_step_entry + "'");
            }


        }
        if (get_prefs_contents_size(SP_LAMP_TIMERS) > 0) {
            Toast.makeText(this, "Sequence data stored on mobile device.\nPress PRG on main panel to run", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Nothing to be saved", Toast.LENGTH_SHORT).show();
        }
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

    public String get_tl84_delay() {
        SharedPreferences config_prefs = getSharedPreferences(Constants.CONFIG_SETTINGS, 0);
        Log.d(TAG, " ** TL84_delay from file: " + String.format(Locale.US, "%04d", config_prefs.getInt(TL84_DELAY_KEY, 0)));
        return String.format(Locale.US, "%04d", config_prefs.getInt(TL84_DELAY_KEY, 0));

    }

    public void writeSequenceToFile(String sLampName, Integer iTimeToDisplay, String seq_id) {

        SharedPreferences prefs = getSharedPreferences(SP_SEQUENCE_COMMAND_GENERATE, 0);
        SharedPreferences.Editor editor = prefs.edit();

        JSONArray jsonArray = new JSONArray();
        jsonArray.put(sLampName);
        jsonArray.put(iTimeToDisplay);
        //Log.d(TAG, "Writing '" + jsonArray.toString() + "' to Sequence file under id: '" + seq_id + "'");
        editor.putString(seq_id, jsonArray.toString());
        editor.apply();
    }

    private void clear_lamp_timers_sp_file() {
        SharedPreferences spLampTimers = getSharedPreferences(SP_LAMP_TIMERS, 0);
        SharedPreferences.Editor editorLampTimer = spLampTimers.edit();
        editorLampTimer.clear();
        editorLampTimer.apply();
    }

    public void resetSequenceCommand(View v) {
        clear_lamp_timers_sp_file();
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
        SharedPreferences spLampTimers = getSharedPreferences(SP_LAMP_TIMERS, 0);
        TreeMap<String, ?> allEntries = new TreeMap<String, Object>(spLampTimers.getAll());
        int i = 1;
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String sVal = entry.getValue().toString();
            String[] sBuffer = sVal.split(",");
            String sSequenceStep = i++ + ". ";
            sSequenceStep += sBuffer[0] + ": ";
            int timer = Integer.valueOf(sBuffer[1].substring(20, 24));
            sSequenceStep += timer + "s";
            //Log.d(TAG, "Currently sSequence: " + entry.getKey() + ": " + sSequence);
            listItems.add(sSequenceStep);
        }
        adapter.notifyDataSetChanged();
    }

    public void makeToast (String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }




}