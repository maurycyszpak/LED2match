package com.kiand.LED2match;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;


/**
 * Created by Morris Starling on 08.01.2017.
 * This file serves managing the activity on Lamp Sequence page
 */

public class SequenceProgramming extends Activity implements Runnable {

    public static String TAG = "MORRIS-SEQ-PROGRAM";
    public static final String APP_SEQ_PRESET = "EEPROM_SEQ_PRESET"; //Mauricio
    private static final int MSG_EXIT_LOOP = 1;
    public boolean blRunningThread;

    public Spinner spinSequences;
    public ArrayAdapter<String> arrAdapterSequences;
    public ArrayList<String> arrListSequences = new ArrayList<String>();

    int iCurrRow = 0;
    TextView textCurrentActiveRow;
    public ArrayList<String> spinLampsArrList = new ArrayList<>(8);
    public ArrayAdapter<String> spinLampsAdapter;
    TextView textSeqCommand;
    private Timer myTimer;
    private Long lNextStopTime;
    EditText edTextDebug;
    public static Thread T1;

    private static Handler messageHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == MSG_EXIT_LOOP) {
                String message = (String)msg.obj;
                Toast.makeText(App.context, message , Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void run() {
        int i = 0;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sequence_page_layout);

        //final ImageView imageAddRow = (ImageView)findViewById(R.id.imageViewAddRow);

        //change the image while clicked
        /*imageAddRow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                imageAddRow.setImageResource(R.drawable.ic_add_pressed);
            }
        });*/

        edTextDebug = (EditText)findViewById(R.id.editTextTime1);
        spinSequences = (Spinner) findViewById(R.id.spinnerStoredSequences); // Mauricio
        arrAdapterSequences = new ArrayAdapter<String>(SequenceProgramming.this, R.layout.spinner_row, arrListSequences); // Mauricio
        spinSequences.setAdapter (arrAdapterSequences);

        Spinner spinnerLampsRow1 = (Spinner) findViewById(R.id.spinnerLampsRow1);
        Spinner spinnerLampsRow2 = (Spinner) findViewById(R.id.spinnerLampsRow2);
        Spinner spinnerLampsRow3 = (Spinner) findViewById(R.id.spinnerLampsRow3);
        Spinner spinnerLampsRow4 = (Spinner) findViewById(R.id.spinnerLampsRow4);
        Spinner spinnerLampsRow5 = (Spinner) findViewById(R.id.spinnerLampsRow5);
        Spinner spinnerLampsRow6 = (Spinner) findViewById(R.id.spinnerLampsRow6);

        final EditText editTextRed1 = (EditText) findViewById(R.id.editTextRed1);
        final EditText editTextRed2 = (EditText) findViewById(R.id.editTextRed2);
        final EditText editTextRed3 = (EditText) findViewById(R.id.editTextRed3);
        final EditText editTextRed4 = (EditText) findViewById(R.id.editTextRed4);
        final EditText editTextRed5 = (EditText) findViewById(R.id.editTextRed5);
        final EditText editTextRed6 = (EditText) findViewById(R.id.editTextRed6);

        final EditText editTextGreen1 = (EditText) findViewById(R.id.editTextGreen1);
        final EditText editTextGreen2 = (EditText) findViewById(R.id.editTextGreen2);
        final EditText editTextGreen3 = (EditText) findViewById(R.id.editTextGreen3);
        final EditText editTextGreen4 = (EditText) findViewById(R.id.editTextGreen4);
        final EditText editTextGreen5 = (EditText) findViewById(R.id.editTextGreen5);
        final EditText editTextGreen6 = (EditText) findViewById(R.id.editTextGreen6);

        final EditText editTextBlue1 = (EditText) findViewById(R.id.editTextBlue1);
        final EditText editTextBlue2 = (EditText) findViewById(R.id.editTextBlue2);
        final EditText editTextBlue3 = (EditText) findViewById(R.id.editTextBlue3);
        final EditText editTextBlue4 = (EditText) findViewById(R.id.editTextBlue4);
        final EditText editTextBlue5 = (EditText) findViewById(R.id.editTextBlue5);
        final EditText editTextBlue6 = (EditText) findViewById(R.id.editTextBlue6);

        final EditText editTextWhite1 = (EditText) findViewById(R.id.editTextWhite1);
        final EditText editTextWhite2 = (EditText) findViewById(R.id.editTextWhite2);
        final EditText editTextWhite3 = (EditText) findViewById(R.id.editTextWhite3);
        final EditText editTextWhite4 = (EditText) findViewById(R.id.editTextWhite4);
        final EditText editTextWhite5 = (EditText) findViewById(R.id.editTextWhite5);
        final EditText editTextWhite6 = (EditText) findViewById(R.id.editTextWhite6);

        final EditText editTextTime1 = (EditText) findViewById(R.id.editTextTime1);
        final EditText editTextTime2 = (EditText) findViewById(R.id.editTextTime2);
        final EditText editTextTime3 = (EditText) findViewById(R.id.editTextTime3);
        final EditText editTextTime4 = (EditText) findViewById(R.id.editTextTime4);
        final EditText editTextTime5 = (EditText) findViewById(R.id.editTextTime5);
        final EditText editTextTime6 = (EditText) findViewById(R.id.editTextTime6);

        textSeqCommand = (TextView) findViewById(R.id.textSequenceCommand);

        textCurrentActiveRow = (TextView) findViewById(R.id.textCurrentRow);
        spinLampsAdapter = new ArrayAdapter<>(SequenceProgramming.this, R.layout.spinner_row, spinLampsArrList); // Mauricio

        iCurrRow = 1;
        textCurrentActiveRow.setText(String.valueOf(iCurrRow));
        makeLayoutInvisible(2);
        makeLayoutInvisible(3);
        makeLayoutInvisible(4);
        makeLayoutInvisible(5);
        makeLayoutInvisible(6);

        spinLampsArrList.clear();
        populateLampNames();
        spinnerLampsRow1.setAdapter(spinLampsAdapter);
        spinnerLampsRow2.setAdapter(spinLampsAdapter);
        spinnerLampsRow3.setAdapter(spinLampsAdapter);
        spinnerLampsRow4.setAdapter(spinLampsAdapter);
        spinnerLampsRow5.setAdapter(spinLampsAdapter);
        spinnerLampsRow6.setAdapter(spinLampsAdapter);

        refreshItemsOnSpinner();
        spinLampsAdapter.notifyDataSetChanged();

        spinnerLampsRow1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // Mauricio
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int pos, long id) { // Mauricio
                if (pos == 0) {
                    editTextRed1.setVisibility(View.VISIBLE);
                    editTextGreen1.setVisibility(View.VISIBLE);
                    editTextBlue1.setVisibility(View.VISIBLE);
                    editTextWhite1.setVisibility(View.VISIBLE);
                    
                } else {
                    //resetLayoutContent(R.id.layoutSeqStep1);
                    editTextRed1.setVisibility(View.INVISIBLE);
                    editTextGreen1.setVisibility(View.INVISIBLE);
                    editTextBlue1.setVisibility(View.INVISIBLE);
                    editTextWhite1.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) { // Mauricio
                // DO NOTHING
            }
        });

        spinnerLampsRow2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // Mauricio
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int pos, long id) { // Mauricio
                if (pos == 0) {
                    editTextRed2.setVisibility(View.VISIBLE);
                    editTextGreen2.setVisibility(View.VISIBLE);
                    editTextBlue2.setVisibility(View.VISIBLE);
                    editTextWhite2.setVisibility(View.VISIBLE);

                } else {
                    //resetLayoutContent(R.id.layoutSeqStep2);
                    editTextRed2.setVisibility(View.INVISIBLE);
                    editTextGreen2.setVisibility(View.INVISIBLE);
                    editTextBlue2.setVisibility(View.INVISIBLE);
                    editTextWhite2.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) { // Mauricio
                // DO NOTHING
            }
        });

        spinnerLampsRow3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // Mauricio
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int pos, long id) { // Mauricio
                if (pos == 0) {
                    editTextRed3.setVisibility(View.VISIBLE);
                    editTextGreen3.setVisibility(View.VISIBLE);
                    editTextBlue3.setVisibility(View.VISIBLE);
                    editTextWhite3.setVisibility(View.VISIBLE);

                } else {
                    //resetLayoutContent(R.id.layoutSeqStep3);
                    editTextRed3.setVisibility(View.INVISIBLE);
                    editTextGreen3.setVisibility(View.INVISIBLE);
                    editTextBlue3.setVisibility(View.INVISIBLE);
                    editTextWhite3.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) { // Mauricio
                // DO NOTHING
            }
        });

        spinnerLampsRow4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // Mauricio
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int pos, long id) { // Mauricio
                if (pos == 0) {
                    editTextRed4.setVisibility(View.VISIBLE);
                    editTextGreen4.setVisibility(View.VISIBLE);
                    editTextBlue4.setVisibility(View.VISIBLE);
                    editTextWhite4.setVisibility(View.VISIBLE);

                } else {
                    //resetLayoutContent(R.id.layoutSeqStep4);
                    editTextRed4.setVisibility(View.INVISIBLE);
                    editTextGreen4.setVisibility(View.INVISIBLE);
                    editTextBlue4.setVisibility(View.INVISIBLE);
                    editTextWhite4.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) { // Mauricio
                // DO NOTHING
            }
        });

        spinnerLampsRow5.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // Mauricio
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int pos, long id) { // Mauricio
                if (pos == 0) {
                    editTextRed5.setVisibility(View.VISIBLE);
                    editTextGreen5.setVisibility(View.VISIBLE);
                    editTextBlue5.setVisibility(View.VISIBLE);
                    editTextWhite5.setVisibility(View.VISIBLE);

                } else {
                    //resetLayoutContent(R.id.layoutSeqStep5);
                    editTextRed5.setVisibility(View.INVISIBLE);
                    editTextGreen5.setVisibility(View.INVISIBLE);
                    editTextBlue5.setVisibility(View.INVISIBLE);
                    editTextWhite5.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) { // Mauricio
                // DO NOTHING
            }
        });

        spinnerLampsRow6.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // Mauricio
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int pos, long id) { // Mauricio
                if (pos == 0) {
                    editTextRed6.setVisibility(View.VISIBLE);
                    editTextGreen6.setVisibility(View.VISIBLE);
                    editTextBlue6.setVisibility(View.VISIBLE);
                    editTextWhite6.setVisibility(View.VISIBLE);

                } else {
                    //resetLayoutContent(R.id.layoutSeqStep6);
                    editTextRed6.setVisibility(View.INVISIBLE);
                    editTextGreen6.setVisibility(View.INVISIBLE);
                    editTextBlue6.setVisibility(View.INVISIBLE);
                    editTextWhite6.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) { // Mauricio
                // DO NOTHING
            }
        });

        editTextRed1.addTextChangedListener(new RGBTextWatcher(editTextRed1));
        editTextRed2.addTextChangedListener(new RGBTextWatcher(editTextRed2));
        editTextRed3.addTextChangedListener(new RGBTextWatcher(editTextRed3));
        editTextRed4.addTextChangedListener(new RGBTextWatcher(editTextRed4));
        editTextRed5.addTextChangedListener(new RGBTextWatcher(editTextRed5));
        editTextRed6.addTextChangedListener(new RGBTextWatcher(editTextRed6));

        editTextGreen1.addTextChangedListener(new RGBTextWatcher(editTextGreen1));
        editTextGreen2.addTextChangedListener(new RGBTextWatcher(editTextGreen2));
        editTextGreen3.addTextChangedListener(new RGBTextWatcher(editTextGreen3));
        editTextGreen4.addTextChangedListener(new RGBTextWatcher(editTextGreen4));
        editTextGreen5.addTextChangedListener(new RGBTextWatcher(editTextGreen5));
        editTextGreen6.addTextChangedListener(new RGBTextWatcher(editTextGreen6));

        editTextBlue1.addTextChangedListener(new RGBTextWatcher(editTextBlue1));
        editTextBlue2.addTextChangedListener(new RGBTextWatcher(editTextBlue2));
        editTextBlue3.addTextChangedListener(new RGBTextWatcher(editTextBlue3));
        editTextBlue4.addTextChangedListener(new RGBTextWatcher(editTextBlue4));
        editTextBlue5.addTextChangedListener(new RGBTextWatcher(editTextBlue5));
        editTextBlue6.addTextChangedListener(new RGBTextWatcher(editTextBlue6));

        editTextWhite1.addTextChangedListener(new RGBTextWatcher(editTextWhite1));
        editTextWhite2.addTextChangedListener(new RGBTextWatcher(editTextWhite2));
        editTextWhite3.addTextChangedListener(new RGBTextWatcher(editTextWhite3));
        editTextWhite4.addTextChangedListener(new RGBTextWatcher(editTextWhite4));
        editTextWhite5.addTextChangedListener(new RGBTextWatcher(editTextWhite5));
        editTextWhite6.addTextChangedListener(new RGBTextWatcher(editTextWhite6));

        /*editTextTime1.addTextChangedListener(new TIMETextWatcher(editTextTime1));
        editTextTime2.addTextChangedListener(new TIMETextWatcher(editTextTime2));
        editTextTime3.addTextChangedListener(new TIMETextWatcher(editTextTime3));
        editTextTime4.addTextChangedListener(new TIMETextWatcher(editTextTime4));
        editTextTime5.addTextChangedListener(new TIMETextWatcher(editTextTime5));
        editTextTime6.addTextChangedListener(new TIMETextWatcher(editTextTime6));*/
    }



    protected void onResume()
    {
        super.onResume();
        //Toast.makeText(this.getBaseContext(),"Activity resumed", Toast.LENGTH_SHORT).show();
        populateLampNames();
    }
    protected void onDestroy()
    {
        super.onDestroy();
        //Toast.makeText(this.getBaseContext(),"Activity destroyed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_, menu);
        return true;
    }

    public void populateLampNames() {
        if (BtCore.Connected()) {
            String sReply = LightAdjustments.bluetoothAskReply("P").trim();
            spinLampsArrList.clear();
            spinLampsArrList.add(0, "LED-BAR");

            String[] sLamps = sReply.split(",");
            if (sLamps.length == 7) {
                for (int i = 1; i <= 7; i++) {
                    spinLampsArrList.add(i, sLamps[i - 1]);
                }
                spinLampsAdapter.notifyDataSetChanged();
            }
        }
    }

    public void addRow(View v) {
        if (iCurrRow <= 6) {

            if (validateRow(iCurrRow)) {
                makeLayoutVisible(iCurrRow + 1);
                iCurrRow++;
                textCurrentActiveRow.setText(String.valueOf(iCurrRow));
            }
        }
    }

    public void setTextCurrentActiveRow(int i) {
        TextView tv = (TextView) findViewById(R.id.textCurrentRow);
        tv.setText(String.valueOf(iCurrRow));
    }

    public void generateSequenceCommand(View v) {
        EditText et;
        TextView tsc = (TextView) findViewById(R.id.textSequenceCommand);
        String sCommand;

        String sSteps = "Initial Title2";
        AlertDialog dlg = new AlertDialog.Builder(this).create();
        dlg.setTitle("Sequence to be executed:");
        
        //splashSequenceSummary();

        et = (EditText) findViewById(R.id.editSeqStep1);
        sCommand = et.getText().toString();
        sSteps.concat(sCommand+"\n");

        et = (EditText) findViewById(R.id.editSeqStep2);
        sCommand = sCommand.concat(et.getText().toString());
        sSteps.concat(sCommand+"\n");

        et = (EditText) findViewById(R.id.editSeqStep3);
        sCommand = sCommand.concat(et.getText().toString());
        sSteps.concat(sCommand+"\n");

        et = (EditText) findViewById(R.id.editSeqStep4);
        sCommand = sCommand.concat(et.getText().toString());
        sSteps.concat(sCommand+"\n");

        et = (EditText) findViewById(R.id.editSeqStep5);
        sCommand = sCommand.concat(et.getText().toString());
        sSteps.concat(sCommand+"\n");

        et = (EditText) findViewById(R.id.editSeqStep6);
        sCommand = sCommand.concat(et.getText().toString());
        sSteps.concat(sCommand+"\n");

        //dlg.show();

        tsc.setText(sCommand);
        Log.d(TAG, "Sending SEQ data to controller: D"+sCommand );
        BtCore.sendMessageBluetooth("D"+sCommand+"\n");
        //String sSteps = LightAdjustments.bluetoothAskReply("D"+sCommand);
        //Log.d(TAG, "Sent SEQ data to controller, reply: " + sSteps);

        TextView tsr = (TextView) findViewById(R.id.textSequenceResponse);
        String sReply = LightAdjustments.bluetoothAskReply("E");
        tsr.setText(sReply);
        Toast.makeText(this, "SEQ data sent to controller.\nPress Execute to run", Toast.LENGTH_SHORT).show();
    }

    public void executeStoredSequence(View v) {
        TextView tsr = (TextView) findViewById(R.id.textSequenceResponse);
        String sReply = LightAdjustments.bluetoothAskReply("E");
        tsr.setText(sReply);

        String sResponse = sReply;
        List<String> listSeqLines = new ArrayList<>(Arrays.asList(sResponse.split("\\|")));
        Log.d(TAG, "No of Sequence lines:" + listSeqLines.size());
        //Log.d(TAG, sResponse + ": Line1: " + listSeqLines.get(1));
        //Log.d(TAG, sResponse + ": Line2: " + listSeqLines.get(2));
        //Log.d(TAG, sResponse + ": Line3: " + listSeqLines.get(3));
        //LightAdjustments.switchOffAllLamps();
        for (int j = 0; j < listSeqLines.size(); j++) {
            List<String> listSeqLineValues = new ArrayList<>(Arrays.asList(listSeqLines.get(j).split(",")));

            Log.d(TAG, "Sequence line: " + j + "/" + listSeqLines.size() + ", # of value elements:" + listSeqLineValues.size());
            if (listSeqLineValues.size() == 6) {

                int iLampNo = Integer.parseInt(listSeqLineValues.get(0));
                int iRed = Integer.parseInt(listSeqLineValues.get(1));
                int iGreen = Integer.parseInt(listSeqLineValues.get(2));
                int iBlue = Integer.parseInt(listSeqLineValues.get(3));
                int iWhite = Integer.parseInt(listSeqLineValues.get(4));
                long lTime = Long.parseLong(listSeqLineValues.get(5));
                Log.d(TAG, "Line " + j + ":" + String.valueOf(iLampNo) + ",R"+iRed+",G"+iGreen+",B"+iBlue+",W"+iWhite+",TIME"+lTime);
                //String sTime = listSeqLineValues.get(5);

                lNextStopTime = System.currentTimeMillis() + lTime*1000;
                Log.d(TAG, "Executing timer until: " + formatTime(lNextStopTime));
                String sBuf = "";

                Log.d(TAG, "Executing lamp: " + iLampNo);
                //sBuf = LightAdjustments.bluetoothAskReply("L0" + iLampNo+ ",1");
                BtCore.sendMessageBluetooth("L0" + iLampNo+ ",1\n");

                if (iLampNo == 8) {
                    BtCore.sendMessageBluetooth("R" + iRed + "\n");
                    BtCore.sendMessageBluetooth("G" + iGreen + "\n");
                    BtCore.sendMessageBluetooth("B" + iBlue + "\n");
                    BtCore.sendMessageBluetooth("W" + iWhite + "\n");
                }
                while (System.currentTimeMillis() < lNextStopTime) {
                    SystemClock.sleep(250);
                }
                Log.d(TAG, "Switching off all lamps");
                //LightAdjustments.switchOffAllLamps();

                if (j == listSeqLines.size()-1) {
                    CheckBox cb = (CheckBox)findViewById(R.id.checkBoxLoop);

                    if (cb.isChecked()) {
                        j = -1;
                    }
                }
                //Timer myTimer = new Timer();
                //myTimer.schedule(new TimerTask() { @Override public void run() { TimerMethod(); }}, 0, 2000);
            }
        }
    }

    public String formatTime(long millis) {
        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        String sTime = String.format(Locale.US, "%02d:%02d:%02d:%d", hour, minute, second, millis);
        return sTime;
    }
    public void removeRow(View v) {
        int iRowNum = Integer.parseInt(v.getTag().toString());

        makeLayoutInvisible(iRowNum);
        if (iCurrRow > 1) {
            iCurrRow--;
        }
        textCurrentActiveRow.setText(String.valueOf(iCurrRow));

        if (iRowNum == 6) {
            EditText et = (EditText) findViewById(R.id.editSeqStep6);
            et.setText("");
        } else if (iRowNum == 5) {
            EditText et = (EditText) findViewById(R.id.editSeqStep5);
            et.setText("");
        } else if (iRowNum == 4) {
            EditText et = (EditText) findViewById(R.id.editSeqStep4);
            et.setText("");
        } else if (iRowNum == 3) {
            EditText et = (EditText) findViewById(R.id.editSeqStep3);
            et.setText("");
        } else if (iRowNum == 2) {
            EditText et = (EditText) findViewById(R.id.editSeqStep2);
            et.setText("");
        }

        //changeIcon(iRowNum-1, false);
    }

    public boolean validateRow (int iRow) {
        boolean bResult = false;
        EditText et;
        String sCommand;
        Integer iPos;
        String sValue;
        if (iRow == 1) {
            sCommand="";
            Spinner s = (Spinner) findViewById(R.id.spinnerLampsRow1);
            iPos = (s.getSelectedItemPosition() == 0) ? 8 : s.getSelectedItemPosition();
            //iPos = (s.getSelectedItemPosition() == -1) ? 9 : s.getSelectedItemPosition();
            sValue = Integer.toString(iPos);
            sCommand = sCommand.concat(sValue);

            et = (EditText) findViewById(R.id.editTextRed1);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);

            et = (EditText) findViewById(R.id.editTextGreen1);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);

            et = (EditText) findViewById(R.id.editTextBlue1);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);

            et = (EditText) findViewById(R.id.editTextWhite1);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);

            et = (EditText) findViewById(R.id.editTextTime1);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = sValue.equals("9999") ? "999999" : sValue;
            sValue = String.format(Locale.US, "%06d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            Log.d(TAG, "validaterow(1): editTextTime1: " + sValue);

            if (sCommand.length() == 19) {
            //if (sCommand.length() > 0 ) {
                bResult = true;
                EditText editRowSum = (EditText) findViewById(R.id.editSeqStep1);
                editRowSum.setText(sCommand);
            } else {
                Toast.makeText(this, sCommand + "\nRow doesn't have valid values). Is the phone connected to the controller?", Toast.LENGTH_SHORT).show();
            }
        } else if (iRow == 2) {
            sCommand="";
            Spinner s = (Spinner) findViewById(R.id.spinnerLampsRow2);
            iPos = (s.getSelectedItemPosition() == 0) ? 8 : s.getSelectedItemPosition();
            sValue = Integer.toString(iPos);
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextRed2);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextGreen2);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextBlue2);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextWhite2);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextTime2);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = sValue.equals("9999") ? "999999" : sValue;
            sValue = String.format(Locale.US, "%06d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);

            if (sCommand.length() == 19) {
            //if (sCommand.length() > 0 ) {
                bResult = true;
                EditText editRowSum = (EditText) findViewById(R.id.editSeqStep2);
                editRowSum.setText(sCommand);
            } else {
                Toast.makeText(this, "Row doesn't have valid values. Is the phone connected to the controller?", Toast.LENGTH_SHORT).show();
            }
        } else if (iRow == 3) {
            sCommand="";
            Spinner s = (Spinner) findViewById(R.id.spinnerLampsRow3);
            iPos = (s.getSelectedItemPosition() == 0) ? 8 : s.getSelectedItemPosition();
            sValue = Integer.toString(iPos);
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextRed3);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextGreen3);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextBlue3);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextWhite3);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextTime3);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = sValue.equals("9999") ? "999999" : sValue;
            sValue = String.format(Locale.US, "%06d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            if (sCommand.length() == 19) {
                bResult = true;
                EditText editRowSum = (EditText) findViewById(R.id.editSeqStep3);
                editRowSum.setText(sCommand);
            } else {
                Toast.makeText(this, "Row doesn't have valid values. Is the phone connected to the controller?", Toast.LENGTH_SHORT).show();
            }
        } else if (iRow == 4) {
            sCommand="";
            Spinner s = (Spinner) findViewById(R.id.spinnerLampsRow4);
            iPos = (s.getSelectedItemPosition() == 0) ? 8 : s.getSelectedItemPosition();
            sValue = Integer.toString(iPos);
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextRed4);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextGreen4);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextBlue4);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextWhite4);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextTime4);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = sValue.equals("9999") ? "999999" : sValue;
            sValue = String.format(Locale.US, "%06d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            if (sCommand.length() == 19) {
                bResult = true;
                EditText editRowSum = (EditText) findViewById(R.id.editSeqStep4);
                editRowSum.setText(sCommand);
            } else {
                Toast.makeText(this, "Row doesn't have valid values. Is the phone connected to the controller?", Toast.LENGTH_SHORT).show();
            }
        } else if (iRow == 5) {
            sCommand="";
            Spinner s = (Spinner) findViewById(R.id.spinnerLampsRow5);
            iPos = (s.getSelectedItemPosition() == 0) ? 8 : s.getSelectedItemPosition();
            sValue = Integer.toString(iPos);
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextRed5);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextGreen5);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextBlue5);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextWhite5);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextTime5);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = sValue.equals("9999") ? "999999" : sValue;
            sValue = String.format(Locale.US, "%06d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            if (sCommand.length() == 19) {
                bResult = true;
                EditText editRowSum = (EditText) findViewById(R.id.editSeqStep5);
                editRowSum.setText(sCommand);
            } else {
                Toast.makeText(this, "Row doesn't have valid values. Is the phone connected to the controller?", Toast.LENGTH_SHORT).show();
            }
        } else if (iRow == 6) {
            sCommand="";
            Spinner s = (Spinner) findViewById(R.id.spinnerLampsRow6);
            iPos = (s.getSelectedItemPosition() == 0) ? 8 : s.getSelectedItemPosition();
            sValue = Integer.toString(iPos);
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextRed6);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextGreen6);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextBlue6);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextWhite6);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = String.format(Locale.US, "%03d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            et = (EditText) findViewById(R.id.editTextTime6);
            sValue = (et.getText().toString().equals("")) ? "0" : et.getText().toString();
            sValue = sValue.equals("9999") ? "999999" : sValue;
            sValue = String.format(Locale.US, "%06d", Integer.parseInt(sValue));
            sCommand = sCommand.concat(sValue);
            

            if (sCommand.length() == 19) {
                bResult = true;
                EditText editRowSum = (EditText) findViewById(R.id.editSeqStep6);
                editRowSum.setText(sCommand);
            } else {
                Toast.makeText(this, "Row doesn't have valid values. Is the phone connected to the controller?", Toast.LENGTH_SHORT).show();
            }
        }
        return bResult;
    }

    /* public void changeIcon (int iRowNum, boolean blRemovable) {
        if (iRowNum == 1) {
            ImageView iv = (ImageView)findViewById(R.id.imageViewAddRow1);
            iv.setImageResource(R.drawable.icon_remove_selector);
            //iv.setColorFilter(ContextCompat.getColor(context,R.color.hts_blue));
        } else if (iRowNum == 2) {
            ImageView iv = (ImageView)findViewById(R.id.imageViewAddRow2);
            iv.setImageResource(R.drawable.icon_remove_selector);
            //iv.setColorFilter(ContextCompat.getColor(context,R.color.hts_blue));
        } else if (iRowNum == 3) {
            ImageView iv = (ImageView)findViewById(R.id.imageViewAddRow3);
            iv.setImageResource(R.drawable.icon_remove_selector);
            //iv.setColorFilter(ContextCompat.getColor(context,R.color.hts_blue));
        } else if (iRowNum == 4) {
            ImageView iv = (ImageView)findViewById(R.id.imageViewAddRow4);
            iv.setImageResource(R.drawable.icon_remove_selector);
            //iv.setColorFilter(ContextCompat.getColor(context,R.color.hts_blue));
        } else if (iRowNum == 5) {
            ImageView iv = (ImageView)findViewById(R.id.imageViewAddRow5);
            if (blRemovable) {
                iv.setImageResource(R.drawable.icon_remove_selector);
            } else {
                iv.setImageResource(R.drawable.icon_add_selector);
            }
            //iv.setColorFilter(ContextCompat.getColor(context,R.color.hts_blue));
        } else if (iRowNum == 6) { //totally not needed, row 6 only allows removing, no further row to be added
            ImageView iv = (ImageView) findViewById(R.id.imageViewAddRow6);
            if (blRemovable) {
                iv.setImageResource(R.drawable.icon_remove_selector);
            } else {
                iv.setImageResource(R.drawable.icon_add_selector);
            }
            //iv.setColorFilter(ContextCompat.getColor(context, R.color.hts_blue));
        }
    } // this is not needed any more as the layout doesn't require changing the icon image any  more
    */

    public void resetLayoutContent(int iLayoutID) {
        LinearLayout ll = (LinearLayout) findViewById(iLayoutID);
        for ( int i = 0; i < ll.getChildCount();  i++ ){
            View view = ll.getChildAt(i);
            if (view instanceof EditText) {
                EditText et = (EditText) findViewById(view.getId());
                et.setText("");
            } /*else if (view instanceof Spinner) {
                Spinner s = (Spinner) findViewById(view.getId());
                s.setSelection(0, true);
            } */
        }
    }
    public void disableLayoutContent(int iLayoutID) {
        LinearLayout ll = (LinearLayout) findViewById(iLayoutID);
        for ( int i = 0; i < ll.getChildCount();  i++ ){
            View view = ll.getChildAt(i);
            if (view instanceof EditText) {
                EditText et = (EditText) findViewById(view.getId());
                et.setHintTextColor(getResources().getColor(R.color.light_gray));
                et.setTextColor(getResources().getColor(R.color.light_gray));
            }

            view.setEnabled(false); // Or whatever you want to do with the view.
        }
    }

    public void enableLayoutContent(int iLayoutID) {
        LinearLayout ll = (LinearLayout) findViewById(iLayoutID);
        for ( int i = 0; i < ll.getChildCount();  i++ ){
            View view = ll.getChildAt(i);
            if (view instanceof EditText) {
                EditText et = (EditText) findViewById(view.getId());
                et.setHintTextColor(getResources().getColor(R.color.hts_blue));
                et.setTextColor(getResources().getColor(R.color.hts_blue));
            }
            view.setEnabled(true); // Or whatever you want to do with the view.
        }
    }

    public void makeLayoutVisible(int iRowNum) {
        if (iRowNum == 1) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.layoutSeqStep1);
            ll.setVisibility(View.VISIBLE);

        } else if(iRowNum == 2) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.layoutSeqStep2);
            ll.setVisibility(View.VISIBLE);

            int iLayoutID = R.id.layoutSeqStep1;
            disableLayoutContent(iLayoutID);
        } else if (iRowNum == 3) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.layoutSeqStep3);
            ll.setVisibility(View.VISIBLE);

            int iLayoutID = R.id.layoutSeqStep2;
            disableLayoutContent(iLayoutID);
        } else if (iRowNum == 4) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.layoutSeqStep4);
            ll.setVisibility(View.VISIBLE);

            int iLayoutID = R.id.layoutSeqStep3;
            disableLayoutContent(iLayoutID);
        } else if (iRowNum == 5) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.layoutSeqStep5);
            ll.setVisibility(View.VISIBLE);

            int iLayoutID = R.id.layoutSeqStep4;
            disableLayoutContent(iLayoutID);
        } else if (iRowNum == 6) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.layoutSeqStep6);
            ll.setVisibility(View.VISIBLE);

            int iLayoutID = R.id.layoutSeqStep5;
            disableLayoutContent(iLayoutID);
        }
    }

    public void makeLayoutInvisible(int iRowNum) {
        if (iRowNum == 1) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.layoutSeqStep1);
            ll.setVisibility(View.GONE);

        } else if(iRowNum == 2) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.layoutSeqStep2);
            ll.setVisibility(View.GONE);

            int iLayoutID = R.id.layoutSeqStep1;
            enableLayoutContent(iLayoutID);
        } else if (iRowNum == 3) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.layoutSeqStep3);
            ll.setVisibility(View.GONE);

            int iLayoutID = R.id.layoutSeqStep2;
            enableLayoutContent(iLayoutID);
        } else if(iRowNum == 4) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.layoutSeqStep4);
            ll.setVisibility(View.GONE);

            int iLayoutID = R.id.layoutSeqStep3;
            enableLayoutContent(iLayoutID);
        } else if(iRowNum == 5) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.layoutSeqStep5);
            ll.setVisibility(View.GONE);

            int iLayoutID = R.id.layoutSeqStep4;
            enableLayoutContent(iLayoutID);
        } else if(iRowNum == 6) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.layoutSeqStep6);
            ll.setVisibility(View.GONE);

            int iLayoutID = R.id.layoutSeqStep5;
            enableLayoutContent(iLayoutID);
        }
    }

    private class RGBTextWatcher implements TextWatcher {
        private EditText editTextControl;

        public RGBTextWatcher(EditText e) {
            editTextControl = e;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence arg0, int start, int before, int count) {
            String strEnteredVal = arg0.toString();
            if (!strEnteredVal.equals("")) {
                int num = Integer.parseInt(strEnteredVal);
                if (num > 255) {
                    editTextControl.setText("255");
                    editTextControl.setSelection(editTextControl.getText().length());
                }
            }
        }

        public void afterTextChanged(Editable s) {
        }
    }

    private class TIMETextWatcher implements TextWatcher {
        private EditText editTextControl;

        public TIMETextWatcher(EditText e) {
            editTextControl = e;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence arg0, int start, int before, int count) {
            String strEnteredVal = arg0.toString();
            if (!strEnteredVal.equals("")) {
                int num = Integer.parseInt(strEnteredVal);
                if (num > 999999) {
                    editTextControl.setText("999999");
                    editTextControl.setSelection(editTextControl.getText().length());
                }
            }
        }

        public void afterTextChanged(Editable s) {
        }
    }

    public void saveSequencePreset (final View view) {
        // get changelampname.xml view

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        final View seqNameView = layoutInflater.inflate(R.layout.add_sequence_preset, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set changelampname.xml to be the layout file of the alertdialog builder
        alertDialogBuilder.setView(seqNameView);
        final EditText edtInput = (EditText) seqNameView.findViewById(R.id.newSequenceName);

        // setup a dialog window
        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //get user input and set it to result

                        if (edtInput.getText().toString().length() > 0) {
                            String newSeqName = edtInput.getText().toString();
                            //newLampName = newLampName.substring(0, 8);
                            updateSeqName(view, newSeqName);
                            //populateLampNames();
                            //dialog.cancel();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
    }

    public String buildSequenceFromRows() {
        String sReturn = "";
        EditText et;

        et = (EditText) findViewById(R.id.editSeqStep1);
        sReturn = sReturn.concat(et.getText().toString());
        et = (EditText) findViewById(R.id.editSeqStep2);
        sReturn = sReturn.concat(et.getText().toString());
        et = (EditText) findViewById(R.id.editSeqStep3);
        sReturn = sReturn.concat(et.getText().toString());
        et = (EditText) findViewById(R.id.editSeqStep4);
        sReturn = sReturn.concat(et.getText().toString());
        et = (EditText) findViewById(R.id.editSeqStep5);
        sReturn = sReturn.concat(et.getText().toString());
        et = (EditText) findViewById(R.id.editSeqStep6);
        sReturn = sReturn.concat(et.getText().toString());

        return sReturn;
    }

    public void updateSeqName(View view, String newSeqName) {
        //writeLampName(view, newLampName);
        //checkForNewLampName();
        SharedPreferences spsSequence = getSharedPreferences(APP_SEQ_PRESET, 0);
        SharedPreferences.Editor spsEditor = spsSequence.edit();
        TextView tv = (TextView) findViewById(R.id.textSequenceCommand);

        if (newSeqName.length() > 0) {
            //spsEditor.clear(); //Delete previous presets
            //spsEditor.commit();
            String sValue = buildSequenceFromRows();

            //spsEditor.putString(newSeqName, tv.getText().toString());
            spsEditor.putString(newSeqName, sValue);
            spsEditor.commit();
            refreshItemsOnSpinner();
        }
        Toast.makeText(this, "Sequence " + tv.getText().toString() + "stored under name: " + newSeqName + "'.", Toast.LENGTH_SHORT).show();
    }
    public void populateSequenceNames() {
        refreshItemsOnSpinner();
    }
    
    public void populateRow(int i, String sLamp, String sRed, String sGreen, String sBlue, String sWhite, String sTime) {
        EditText etRed, etGreen, etBlue, etWhite, etTime;
        Spinner sp;
        LinearLayout ll;

        Log.d(TAG, "function populateRow:" + sLamp+","+sRed+","+sGreen+","+sBlue+","+sWhite+","+sTime);
        if (i==1) {

            ll = (LinearLayout)findViewById(R.id.layoutSeqStep1);
            if (ll.getVisibility() == View.GONE) { ll.setVisibility(View.VISIBLE);}

            etRed = (EditText)findViewById(R.id.editTextRed1);
            etGreen = (EditText)findViewById(R.id.editTextGreen1);
            etBlue = (EditText)findViewById(R.id.editTextBlue1);
            etWhite = (EditText)findViewById(R.id.editTextWhite1);
            etTime = (EditText)findViewById(R.id.editTextTime1);
            sp = (Spinner)findViewById(R.id.spinnerLampsRow1);
            if (sLamp.equals("8")) {
                
                sRed = (sRed.equals("000") ? "" : sRed);
                sGreen = (sGreen.equals("000") ? "" : sGreen);
                sBlue = (sBlue.equals("000") ? "" : sBlue);
                sWhite = (sWhite.equals("000") ? "" : sWhite);
                sTime = (sTime.equals("000000") ? "" : sTime);

                etTime.setText(sTime);
                sp.setSelection(0);
                etRed.setText(sRed);
                etGreen.setText(sGreen);
                etBlue.setText(sBlue);
                etWhite.setText(sWhite);

            } else {
                etTime.setText(sTime);
                sp.setSelection(Integer.parseInt(sLamp));
            }
            Log.d(TAG, "("+i+")Populating EditTexts: RED:"+sRed + ",GREEN:"+sGreen+",BLUE:"+sBlue+",WHITE:"+sWhite+",TIME:"+sTime);
        } else if (i==2) {

            ll = (LinearLayout)findViewById(R.id.layoutSeqStep2);
            if (ll.getVisibility() == View.GONE) { ll.setVisibility(View.VISIBLE);}

            etRed = (EditText)findViewById(R.id.editTextRed2);
            etGreen = (EditText)findViewById(R.id.editTextGreen2);
            etBlue = (EditText)findViewById(R.id.editTextBlue2);
            etWhite = (EditText)findViewById(R.id.editTextWhite2);
            etTime = (EditText)findViewById(R.id.editTextTime2);
            sp = (Spinner)findViewById(R.id.spinnerLampsRow2);
            if (sLamp.equals("8")) {

                sRed = (sRed.equals("000") ? "" : sRed);
                sGreen = (sGreen.equals("000") ? "" : sGreen);
                sBlue = (sBlue.equals("000") ? "" : sBlue);
                sWhite = (sWhite.equals("000") ? "" : sWhite);
                sTime = (sTime.equals("000000") ? "" : sTime);

                sp.setSelection(0);
                etRed.setText(sRed);
                etGreen.setText(sGreen);
                etBlue.setText(sBlue);
                etWhite.setText(sWhite);
            } else {
                sp.setSelection(Integer.parseInt(sLamp));
            }
            etTime.setText(sTime);
            Log.d(TAG, "("+i+")Populating EditTexts: RED:"+sRed + ",GREEN:"+sGreen+",BLUE:"+sBlue+",WHITE:"+sWhite+",TIME:"+sTime);
        } else if (i==3) {
            ll = (LinearLayout)findViewById(R.id.layoutSeqStep3);
            if (ll.getVisibility() == View.GONE) { ll.setVisibility(View.VISIBLE);}

            etRed = (EditText)findViewById(R.id.editTextRed3);
            etGreen = (EditText)findViewById(R.id.editTextGreen3);
            etBlue = (EditText)findViewById(R.id.editTextBlue3);
            etWhite = (EditText)findViewById(R.id.editTextWhite3);
            etTime = (EditText)findViewById(R.id.editTextTime3);
            sp = (Spinner)findViewById(R.id.spinnerLampsRow3);
            if (sLamp.equals("8")) {
                sRed = (sRed.equals("000") ? "" : sRed);
                sGreen = (sGreen.equals("000") ? "" : sGreen);
                sBlue = (sBlue.equals("000") ? "" : sBlue);
                sWhite = (sWhite.equals("000") ? "" : sWhite);
                sTime = (sTime.equals("000000") ? "" : sTime);

                sp.setSelection(0);
                etRed.setText(sRed);
                etGreen.setText(sGreen);
                etBlue.setText(sBlue);
                etWhite.setText(sWhite);
            } else {
                sp.setSelection(Integer.parseInt(sLamp));
            }
            etTime.setText(sTime);
            Log.d(TAG, "("+i+")Populating EditTexts: RED:"+sRed + ",GREEN:"+sGreen+",BLUE:"+sBlue+",WHITE:"+sWhite+",TIME:"+sTime);
        } else if (i==4) {
            ll = (LinearLayout)findViewById(R.id.layoutSeqStep4);
            if (ll.getVisibility() == View.GONE) { ll.setVisibility(View.VISIBLE);}

            etRed = (EditText)findViewById(R.id.editTextRed4);
            etGreen = (EditText)findViewById(R.id.editTextGreen4);
            etBlue = (EditText)findViewById(R.id.editTextBlue4);
            etWhite = (EditText)findViewById(R.id.editTextWhite4);
            etTime = (EditText)findViewById(R.id.editTextTime4);
            sp = (Spinner)findViewById(R.id.spinnerLampsRow4);
            if (sLamp.equals("8")) {
                sRed = (sRed.equals("000") ? "" : sRed);
                sGreen = (sGreen.equals("000") ? "" : sGreen);
                sBlue = (sBlue.equals("000") ? "" : sBlue);
                sWhite = (sWhite.equals("000") ? "" : sWhite);
                sTime = (sTime.equals("000000") ? "" : sTime);

                sp.setSelection(0);
                etRed.setText(sRed);
                etGreen.setText(sGreen);
                etBlue.setText(sBlue);
                etWhite.setText(sWhite);
            } else {
                sp.setSelection(Integer.parseInt(sLamp));
            }
            etTime.setText(sTime);
            Log.d(TAG, "("+i+")Populating EditTexts: RED:"+sRed + ",GREEN:"+sGreen+",BLUE:"+sBlue+",WHITE:"+sWhite+",TIME:"+sTime);
        } else if (i==5) {
            ll = (LinearLayout)findViewById(R.id.layoutSeqStep5);
            if (ll.getVisibility() == View.GONE) { ll.setVisibility(View.VISIBLE);}

            etRed = (EditText)findViewById(R.id.editTextRed5);
            etGreen = (EditText)findViewById(R.id.editTextGreen5);
            etBlue = (EditText)findViewById(R.id.editTextBlue5);
            etWhite = (EditText)findViewById(R.id.editTextWhite5);
            etTime = (EditText)findViewById(R.id.editTextTime5);
            sp = (Spinner)findViewById(R.id.spinnerLampsRow5);
            if (sLamp.equals("8")) {
                sRed = (sRed.equals("000") ? "" : sRed);
                sGreen = (sGreen.equals("000") ? "" : sGreen);
                sBlue = (sBlue.equals("000") ? "" : sBlue);
                sWhite = (sWhite.equals("000") ? "" : sWhite);
                sTime = (sTime.equals("000000") ? "" : sTime);

                sp.setSelection(0);
                etRed.setText(sRed);
                etGreen.setText(sGreen);
                etBlue.setText(sBlue);
                etWhite.setText(sWhite);
            } else {
                sp.setSelection(Integer.parseInt(sLamp));
            }
            etTime.setText(sTime);
            Log.d(TAG, "("+i+")Populating EditTexts: RED:"+sRed + ",GREEN:"+sGreen+",BLUE:"+sBlue+",WHITE:"+sWhite+",TIME:"+sTime);
        } else if (i==6) {
            ll = (LinearLayout)findViewById(R.id.layoutSeqStep6);
            if (ll.getVisibility() == View.GONE) { ll.setVisibility(View.VISIBLE);}

            etRed = (EditText)findViewById(R.id.editTextRed6);
            etGreen = (EditText)findViewById(R.id.editTextGreen6);
            etBlue = (EditText)findViewById(R.id.editTextBlue6);
            etWhite = (EditText)findViewById(R.id.editTextWhite6);
            etTime = (EditText)findViewById(R.id.editTextTime6);
            sp = (Spinner)findViewById(R.id.spinnerLampsRow6);
            if (sLamp.equals("8")) {

                sRed = (sRed.equals("000") ? "" : sRed);
                sGreen = (sGreen.equals("000") ? "" : sGreen);
                sBlue = (sBlue.equals("000") ? "" : sBlue);
                sWhite = (sWhite.equals("000") ? "" : sWhite);
                sTime = (sTime.equals("000000") ? "" : sTime);

                sp.setSelection(0);
                etRed.setText(sRed);
                etGreen.setText(sGreen);
                etBlue.setText(sBlue);
                etWhite.setText(sWhite);
            } else {
                sp.setSelection(Integer.parseInt(sLamp));
            }
            etTime.setText(sTime);
            Log.d(TAG, "("+i+")Populating EditTexts: RED:"+sRed + ",GREEN:"+sGreen+",BLUE:"+sBlue+",WHITE:"+sWhite+",TIME:"+sTime);
        }
    }

    public void loadSequencePreset(View v) {

        clearPageState(v);
        SharedPreferences spsSequence = getSharedPreferences(APP_SEQ_PRESET, 0);
        //SharedPreferences.Editor spsEditor = spsSequence.edit();
        Spinner ss = (Spinner) findViewById(R.id.spinnerStoredSequences);
        TextView tv = (TextView) findViewById(R.id.textSequenceCommand);
        String sSeqName = ss.getSelectedItem().toString();
        String sValue = "";

        Log.d(TAG, "Executing loadSequencePreset");

        if (sSeqName.length() > 0) {
            //spsEditor.clear(); //Delete previous presets
            //spsEditor.commit();
            Map<String, ?> allEntries = spsSequence.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String sKey = entry.getKey();
                if (sKey.equals(sSeqName)) {
                    sValue = entry.getValue().toString();

                    String line;
                    int iRows = sValue.length() / 19;
                    Log.d(TAG, "Found " + iRows + " of row sequences in the preset");
                    for (int i = 1; i<= iRows; i++) {
                        Log.d(TAG, "Sending RGBW+T:" + sValue.substring((i-1)*19, (i-1)*19+1) + "," + sValue.substring((i-1)*19+1, (i-1)*19+4) +"," + sValue.substring((i-1)*19+4, (i-1)*19+7)+"," + sValue.substring((i-1)*19+7, (i-1)*19+10) +"," + sValue.substring((i-1)*19+10, (i-1)*19+13) + "," + sValue.substring((i-1)*19+13, (i-1)*19+19));
                        populateRow(i, sValue.substring((i-1)*19, (i-1)*19+1), sValue.substring((i-1)*19+1, (i-1)*19+4), sValue.substring((i-1)*19+4, (i-1)*19+7), sValue.substring((i-1)*19+7, (i-1)*19+10), sValue.substring((i-1)*19+10, (i-1)*19+13), sValue.substring((i-1)*19+13, (i-1)*19+19));
                        validateRow(i);
                        //int iLayoutID = R.id.layoutSeqStep3;
                        //disableLayoutContent(iLayoutID);
                        setTextCurrentActiveRow(i);
                    }

                }
            }


            TextView tsc = (TextView) findViewById(R.id.textSequenceCommand);
            tsc.setText(sValue);
            Log.d(TAG, "Sending SEQ data to controller: D"+sValue);
            //String sReply = LightAdjustments.bluetoothAskReply("D"+sValue);
            BtCore.sendMessageBluetooth("D"+sValue+"\n");
            //Log.d(TAG, "Sent SEQ data to controller, reply: " + sReply);
        }
    }

    public void clearPageState(View v) {
        resetLayoutContent(((LinearLayout)findViewById((R.id.layoutSeqStep1))).getId());
        resetLayoutContent(((LinearLayout)findViewById((R.id.layoutSeqStep2))).getId());
        resetLayoutContent(((LinearLayout)findViewById((R.id.layoutSeqStep3))).getId());
        resetLayoutContent(((LinearLayout)findViewById((R.id.layoutSeqStep4))).getId());
        resetLayoutContent(((LinearLayout)findViewById((R.id.layoutSeqStep5))).getId());
        resetLayoutContent(((LinearLayout)findViewById((R.id.layoutSeqStep6))).getId());
        Spinner s = (Spinner) findViewById(R.id.spinnerLampsRow1);
        s.setSelection(0, true);
        s = (Spinner) findViewById(R.id.spinnerLampsRow2);
        s.setSelection(0, true);
        s = (Spinner) findViewById(R.id.spinnerLampsRow3);
        s.setSelection(0, true);
        s = (Spinner) findViewById(R.id.spinnerLampsRow4);
        s.setSelection(0, true);
        s = (Spinner) findViewById(R.id.spinnerLampsRow5);
        s.setSelection(0, true);
        s = (Spinner) findViewById(R.id.spinnerLampsRow6);
        s.setSelection(0, true);

        makeLayoutInvisible(6);
        makeLayoutInvisible(5);
        makeLayoutInvisible(4);
        makeLayoutInvisible(3);
        makeLayoutInvisible(2);

        iCurrRow = 1;
    }

    public void refreshItemsOnSpinner() {

        SharedPreferences spsValues = getSharedPreferences(APP_SEQ_PRESET, 0);
        String sVal;
        arrListSequences.clear();
        //read in APP_SHAREDPREFS_READ;
        //For each pair of key-> value add item to the list
        //refresh the list and the spinner
        Map<String, ?> allEntries = spsValues.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            sVal = entry.getKey();
            arrListSequences.add(sVal);
            Log.d(TAG, "APP_SEQ_PRESET: " + entry.getKey() + ": " + entry.getValue().toString());
            //Toast.makeText(this, "APP_SEQ_PRESET: " + entry.getKey() + ": " + sVal, Toast.LENGTH_SHORT).show();
        }
        Collections.sort(arrListSequences);
        arrAdapterSequences.notifyDataSetChanged();
    }

    private void TimerMethod() {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        this.runOnUiThread(Timer_Tick);
    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {

            //This method runs in the same thread as the UI.
            //Do something to the UI thread here
            Log.d (TAG, System.currentTimeMillis() + "Running Sequence step until: " +lNextStopTime);
            if (System.currentTimeMillis() > lNextStopTime) {
                try {
                    myTimer.cancel();
                    myTimer = null;
                } catch (NullPointerException e) {
                    // not needed if the Timer is gone
                } finally {
                    Log.d (TAG, "Stopping Sequence Step");
                }

            }
        }
    };

    public void openSeqLayout (final View view) {
        EditText et;
        String sReply = "";
        String sValue = "";

        et = (EditText) findViewById(R.id.editSeqStep1);
        sValue = et.getText().toString();
        if (sValue.length() == 19 ) {
            sReply = sReply.concat(sValue);
            sReply = sReply.concat("\n");
        }

        //Toast.makeText(this, "Editbox value: "+ sValue +", sReply: " + sReply, Toast.LENGTH_SHORT).show();

        et = (EditText) findViewById(R.id.editSeqStep2);
        sValue = et.getText().toString();
        if (sValue.length() == 19 ) {
            sReply = sReply.concat(sValue);
            sReply = sReply.concat("\n");
        }

        et = (EditText) findViewById(R.id.editSeqStep3);
        sValue = et.getText().toString();
        if (sValue.length() == 19 ) {
            sReply = sReply.concat(sValue);
            sReply = sReply.concat("\n");
        }

        et = (EditText) findViewById(R.id.editSeqStep4);
        sValue = et.getText().toString();
        if (sValue.length() == 19 ) {
            sReply = sReply.concat(sValue);
            sReply = sReply.concat("\n");
        }

        et = (EditText) findViewById(R.id.editSeqStep5);
        sValue = et.getText().toString();
        if (sValue.length() == 19 ) {
            sReply = sReply.concat(sValue);
            sReply = sReply.concat("\n");
        }

        et = (EditText) findViewById(R.id.editSeqStep6);
        sValue = et.getText().toString();
        if (sValue.length() == 19 ) {
            sReply = sReply.concat(sValue);
            sReply = sReply.concat("\n");
        }

        sReply = sReply.length() < 19 ? "" : sReply;
        Intent intent = new Intent(SequenceProgramming.this, sequenceSummaryPage.class);
        Log.d(TAG, "Sending the following key1 to new activity: " + sReply);
        intent.putExtra("key1", sReply);
        startActivity(intent);
    }

    private void splashSequenceSummary() {

        String sReply = "Initial Title";
        AlertDialog dlg = new AlertDialog.Builder(this).create();
        dlg.setTitle("Sequence to be executed:");

        EditText et;
        et = (EditText) findViewById(R.id.editSeqStep1);
        sReply.concat(et.getText().toString());
        sReply.concat("\n");
        et = (EditText) findViewById(R.id.editSeqStep2);
        sReply.concat(et.getText().toString());
        sReply.concat("\n");
        et = (EditText) findViewById(R.id.editSeqStep3);
        sReply.concat(et.getText().toString());
        sReply.concat("\n");
        et = (EditText) findViewById(R.id.editSeqStep4);
        sReply.concat(et.getText().toString());
        sReply.concat("\n");
        et = (EditText) findViewById(R.id.editSeqStep5);
        sReply.concat(et.getText().toString());
        sReply.concat("\n");
        et = (EditText) findViewById(R.id.editSeqStep6);
        sReply.concat(et.getText().toString());
        sReply.concat("\n");

        dlg.setMessage(sReply);
        dlg.setIcon(R.drawable.icon_main);
        dlg.show();
    }
    public void runLoopOnThread (View v) {
        Log.d(TAG, "Creating Thread");
        //Thread threadSequence = new Thread(new SequenceLoop());
        //threadSequence.start();

        //T1 = new Thread(new SequenceLoop());
        //T1.start();
        blRunningThread = true;
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                executeStoredSequenceThreaded();
            }
        });
        t1.start();
    }

    public void executeStoredSequenceThreaded() {
        Log.d(TAG, "starting executeStoredSequenceThreaded()");
        TextView tsr = (TextView) findViewById(R.id.textSequenceResponse);
        String sReply = LightAdjustments.bluetoothAskReply("E");
        tsr.setText(sReply);

        String sResponse = sReply;
        List<String> listSeqLines = new ArrayList<>(Arrays.asList(sResponse.split("\\|")));
        Log.d(TAG, "No of Sequence lines:" + listSeqLines.size());

        //LightAdjustments.switchOffAllLamps();
        int iLimit = listSeqLines.size();
        for (int j = 0; j < iLimit; j++) {

            List<String> listSeqLineValues = new ArrayList<>(Arrays.asList(listSeqLines.get(j).split(",")));

            Log.d(TAG, "Sequence line: " + j + "/" + listSeqLines.size() + ", # of value elements:" + listSeqLineValues.size());
            if (listSeqLineValues.size() == 6) {

                int iLampNo = Integer.parseInt(listSeqLineValues.get(0));
                int iRed = Integer.parseInt(listSeqLineValues.get(1));
                int iGreen = Integer.parseInt(listSeqLineValues.get(2));
                int iBlue = Integer.parseInt(listSeqLineValues.get(3));
                int iWhite = Integer.parseInt(listSeqLineValues.get(4));
                long lTime = Long.parseLong(listSeqLineValues.get(5));
                Log.d(TAG, "Line " + j + ":" + String.valueOf(iLampNo) + ",R" + iRed + ",G" + iGreen + ",B" + iBlue + ",W" + iWhite + ",TIME" + lTime);

                lNextStopTime = System.currentTimeMillis() + lTime * 1000;
                Log.d(TAG, "Executing timer until: " + formatTime(lNextStopTime));
                Log.d(TAG, "Executing lamp: " + iLampNo);
                BtCore.sendMessageBluetooth("L0" + iLampNo + ",1\n");
                if (iLampNo == 8) {
                    BtCore.sendMessageBluetooth("R" + iRed + "\n");
                    BtCore.sendMessageBluetooth("G" + iGreen + "\n");
                    BtCore.sendMessageBluetooth("B" + iBlue + "\n");
                    BtCore.sendMessageBluetooth("W" + iWhite + "\n");
                }
                while (System.currentTimeMillis() < lNextStopTime) {
                    SystemClock.sleep(250);
                }
                Log.d(TAG, "Switching off all lamps");
                //LightAdjustments.switchOffAllLamps();
                if (!blRunningThread) iLimit = -1;

                if (j == listSeqLines.size() - 1) {
                    CheckBox cb = (CheckBox) findViewById(R.id.checkBoxLoop);

                    if (cb.isChecked()) {
                        j = -1;
                    }
                }
            }
        }
    }

    public void stopThreadExecution (View v) {
        Log.d(TAG, "Stopping Thread");

        /*Message msg = new Message();
        msg.what = MSG_EXIT_LOOP;
        msg.obj = "Password incorrect";
        messageHandler.sendMessage(msg);
        T1.interrupt();*/

        blRunningThread = false;
    }

    public class StopThread {
        public void main(String[] args) {

        }
    }

    public class SequenceLoop implements Runnable {
        public volatile boolean running = true;
        public Handler mHandler;

        public void run() {

            Looper.prepare();
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    // process incoming messages here
                    if(msg.what == -1){
                        running = false;
                        Looper.myLooper().quit();
                    }
            }};
            Looper.loop();

            try {
                int i = 0 ;
                //android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                Log.d(TAG, "Inside SequenceLoop's.run() function");
                executeStoredSequenceThreaded();
            } catch (IllegalStateException e) {
                Log.d (TAG, "Thread ended with and exception");
                this.running = false;
                Thread.currentThread().interrupt();
            }
        }
    }
}

