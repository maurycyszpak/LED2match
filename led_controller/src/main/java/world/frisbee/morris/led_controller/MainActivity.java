//CLEAN FILE
package world.frisbee.morris.led_controller;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class MainActivity extends Activity {
    public static final String BTCOMMSLOG_SHAREDPREFS = "Controller-communication-log"; //Mauricio

    private EditText edRed, edGreen, edBlue, edWhite, edLED65, edLEDUVA, edLED50, edLED27, edLED395, edLED660;
    private SeekBar barWhite, barRed, barGreen, barBlue, barLED65, barLEDUVA, barLED50, barLED27, barLED395, barLED660;
    RelativeLayout rgbLay;
    public static boolean blLamp1On, blLamp2On, blLamp3On, blLamp4On, blLamp5On, blLamp6On, blLamp7On;

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private TextView display;
    private EditText editText;
    private CheckBox box9600, box38400;
    private MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(Menu.NONE, 0, 0, "Scan").setIcon(
                getResources().getDrawable(R.drawable.icon_scan));

        menu.add(Menu.NONE, 1, 1, "Service page").setIcon(
                getResources().getDrawable(R.drawable.icon_scan));

        menu.add(Menu.NONE, 2, 2, "About").setIcon(
                getResources().getDrawable(R.drawable.icon_information));

        menu.add(Menu.NONE, 3, 3, "READ EEPROM").setIcon(getResources().getDrawable(R.drawable.icon_information));

        getMenuInflater().inflate(R.menu.menu, menu);

        MenuItem item = menu.findItem(R.id.menu_color_picker);


        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MyHandler(this);

        edRed = (EditText) findViewById(R.id.edRed);
        edGreen = (EditText) findViewById(R.id.edGreen);
        edBlue = (EditText) findViewById(R.id.edBlue);
        edWhite = (EditText) findViewById(R.id.edWhite);
        edLED65 = (EditText) findViewById(R.id.edLED65);
        edLEDUVA = (EditText) findViewById(R.id.edLEDUVA);
        edLED50 = (EditText) findViewById(R.id.edLED50);
        edLED27 = (EditText) findViewById(R.id.edLED27);
        edLED395 = (EditText) findViewById(R.id.edLED395);
        edLED660 = (EditText) findViewById(R.id.edLED660);

        barWhite = (SeekBar)findViewById(R.id.barWhite);
        barRed = (SeekBar)findViewById(R.id.barRed);
        barGreen = (SeekBar)findViewById(R.id.barGreen);
        barBlue = (SeekBar)findViewById(R.id.barBlue);
        barLED65 = (SeekBar)findViewById(R.id.barLED65);
        barLEDUVA = (SeekBar)findViewById(R.id.barLEDUVA);
        barLED50 = (SeekBar)findViewById(R.id.barLED50);
        barLED27 = (SeekBar)findViewById(R.id.barLED27);
        barLED395 = (SeekBar)findViewById(R.id.barLED395);
        barLED660 = (SeekBar)findViewById(R.id.barLED660);
        rgbLay = (RelativeLayout) findViewById(R.id.RGBlayout);


        display = (TextView) findViewById(R.id.display);
        editText = (EditText) findViewById(R.id.editText1);
        
        
        LEDoff(null);
        Button sendButton = (Button) findViewById(R.id.buttonSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usbService != null) { // if UsbService was correctly binded, Send data
                    //usbService.write(data.getBytes());
                    sendLEDValuesToItsy();
                }

            }
        });

        barRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                edRed.setText("" + arg1);
                fncRefreshRGB();
            }
        });

        barGreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                edGreen.setText("" + arg1);
                fncRefreshRGB();
            }
        });

        barBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                edBlue.setText("" + arg1);
                fncRefreshRGB();
            }
        });

        barWhite.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                edWhite.setText("" + arg1);
                fncRefreshRGB();
            }
        });

        barLED65.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                edLED65.setText("" + arg1);
                fncRefreshRGB();
            }
        });

        barLEDUVA.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                edLEDUVA.setText("" + arg1);
            }
        });

        barLED50.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                edLED50.setText("" + arg1);
            }
        });

        barLED27.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                edLED27.setText("" + arg1);
            }
        });

        barLED395.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                edLED395.setText("" + arg1);
            }
        });

        barLED660.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                edLED660.setText("" + arg1);
            }
        });

        /*Button clearButton = (Button) findViewById(R.id.button2);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                display.setText("");
            }
        });*/
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    //mActivity.get().display.append(data);
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.SYNC_READ:
                    String buffer = (String) msg.obj;
                    //mActivity.get().display.append(buffer);
                    break;
            }
        }
    }

    public void LEDoff(View v) {
        edRed.setText("0");
        edGreen.setText("0");
        edBlue.setText("0");
        edWhite.setText("0");
        edLED65.setText("0");
        edLEDUVA.setText("0");
        edLED50.setText("0");
        edLED27.setText("0");
        edLED395.setText("0");
        edLED660.setText("0");

        barBlue.setProgress(0);
        barGreen.setProgress(0);
        barRed.setProgress(0);
        barWhite.setProgress(0);
        barLED65.setProgress(0);
        barLEDUVA.setProgress(0);
        barLED50.setProgress(0);
        barLED27.setProgress(0);
        barLED395.setProgress(0);
        barLED660.setProgress(0);

        barBlue.setMax(255);
        barGreen.setMax(255);
        barRed.setMax(255);
        barWhite.setMax(255);
        barLED65.setMax(255);
        barLEDUVA.setMax(255);
        barLED50.setMax(255);
        barLED27.setMax(255);
        barLED395.setMax(255);
        barLED660.setMax(255);
    }
    public void sendLEDValuesToItsy() {
        String sColour = edRed.getText().toString();
        if (sColour.length() == 1) {
            sColour = "00" + sColour;
        } else if (sColour.length() == 2) {
            sColour = "0" + sColour;
        }
        String string = "S06" + sColour + "\n";
        usbService.write(string.getBytes());

        //tvAppend(textView, "\nData Sent: " + string);
        //Toast.makeText(getBaseContext(), string, Toast.LENGTH_SHORT).show();

        sColour = edGreen.getText().toString();
        if (sColour.length() == 1) {
            sColour = "00" + sColour;
        } else if (sColour.length() == 2) {
            sColour = "0" + sColour;
        }
        string = "S07" + sColour + "\n";
        usbService.write(string.getBytes());

        //tvAppend(textView, "\nData Sent: " + string);

        sColour = edBlue.getText().toString();
        if (sColour.length() == 1) {
            sColour = "00" + sColour;
        } else if (sColour.length() == 2) {
            sColour = "0" + sColour;
        }
        string = "S08" + sColour + "\n";
        usbService.write(string.getBytes());

        //tvAppend(textView, "\nData Sent: " + string);

        sColour = edWhite.getText().toString();
        if (sColour.length() == 1) {
            sColour = "00" + sColour;
        } else if (sColour.length() == 2) {
            sColour = "0" + sColour;
        }
        string = "S05" + sColour + "\n";
        usbService.write(string.getBytes());

        //tvAppend(textView, "\nData Sent: " + string);

        sColour = edLED65.getText().toString();
        if (sColour.length() == 1) {
            sColour = "00" + sColour;
        } else if (sColour.length() == 2) {
            sColour = "0" + sColour;
        }
        string = "S01" + sColour + "\n";
        usbService.write(string.getBytes());

        //tvAppend(textView, "\nData Sent: " + string);

        sColour = edLEDUVA.getText().toString();
        if (sColour.length() == 1) {
            sColour = "00" + sColour;
        } else if (sColour.length() == 2) {
            sColour = "0" + sColour;
        }
        string = "S02" + sColour + "\n";
        usbService.write(string.getBytes());

        //tvAppend(textView, "\nData Sent: " + string);

        sColour = edLED50.getText().toString();
        if (sColour.length() == 1) {
            sColour = "00" + sColour;
        } else if (sColour.length() == 2) {
            sColour = "0" + sColour;
        }
        string = "S03" + sColour + "\n";
        usbService.write(string.getBytes());
        //tvAppend(textView, "\nData Sent: " + string);

        sColour = edLED27.getText().toString();
        if (sColour.length() == 1) {
            sColour = "00" + sColour;
        } else if (sColour.length() == 2) {
            sColour = "0" + sColour;
        }
        string = "S04" + sColour + "\n";
        usbService.write(string.getBytes());

        //tvAppend(textView, "\nData Sent: " + string);

        sColour = edLED395.getText().toString();
        if (sColour.length() == 1) {
            sColour = "00" + sColour;
        } else if (sColour.length() == 2) {
            sColour = "0" + sColour;
        }
        string = "S09" + sColour + "\n";
        usbService.write(string.getBytes());

        //tvAppend(textView, "\nData Sent: " + string);

        sColour = edLED660.getText().toString();
        if (sColour.length() == 1) {
            sColour = "00" + sColour;
        } else if (sColour.length() == 2) {
            sColour = "0" + sColour;
        }
        string = "S10" + sColour + "\n";
        usbService.write(string.getBytes());

        //tvAppend(textView, "\nData Sent: " + string);
    }
    public void fncRefreshRGB() {
        int rr, gg, bb, ww;

        rr = Integer.parseInt(edRed.getText().toString());
        gg = Integer.parseInt(edGreen.getText().toString());
        bb = Integer.parseInt(edBlue.getText().toString());
        ww = Integer.parseInt(edWhite.getText().toString());

        //Toast.makeText(this, (ww/2+rr/2)+","+(ww/2+gg/2)+","+(ww/2+bb/2), Toast.LENGTH_LONG).show();
        //botLay.setBackgroundColor(Color.rgb(rr, gg, bb));
        rgbLay.setBackgroundColor(Color.rgb((ww/2+rr/2), (ww/2+gg/2), (ww/2+bb/2)));
    }

    public void openSeqLayout (final View view) {
		/*MyRunnable mrn = new MyRunnable("A");
		this.runOnUiThread(mrn);
		SystemClock.sleep(4000);
		hideSplash();*/
        Intent intent = new Intent(MainActivity.this, SequenceProgramming.class);
        startActivity(intent);
    }

    public static void switchOffAllLamps() {
        //BtCore.sendMessageBluetooth("N\n");
		/*sReply = bluetoothAskReply("L02,0");
		sReply = bluetoothAskReply("L03,0");
		sReply = bluetoothAskReply("L04,0");
		sReply = bluetoothAskReply("L05,0");
		sReply = bluetoothAskReply("L06,0");
		sReply = bluetoothAskReply("L07,0");*/
        blLamp1On = blLamp2On = blLamp3On = blLamp4On = blLamp5On = blLamp6On = blLamp7On = false;
    }

    public static String bluetoothAskReply(String sCommand) {
        String[] arrResponse;

        String newline = System.getProperty("line.separator");
        boolean hasNewLine = sCommand.contains(newline);

        BtCore.sendMessageBluetooth(sCommand + '\n');
        BTCommsLog btLog = new BTCommsLog("IN", sCommand);
        btLog.appendMessage("IN<<", sCommand);
        String response = BtCore.receiveMessageBluetooth();
        String sLog = response.replaceAll("(\\r|\\n)", "");
        btLog.appendMessage("OUT>", sLog);
        String sReply = "";
        //Log.d(TAG, "bluetoothAskReply() - Received response:" + response);
        String[] lines = response.split("\\r?\\n");
        for (String value : lines) {
            if (value.lastIndexOf(",") > -1 ) {
                arrResponse = response.split(",", 2);
                if (arrResponse[0].equals("RGBW")) {
                    sReply = arrResponse[1];
                    sReply = sReply.replaceAll("(\\r|\\n)", "");
                }
            }
        }
        return sReply.trim();
    }

    public static class BTCommsLog {
        private String sMessage;
        private String sKey;

        public BTCommsLog(String sKey, String sMessage) {this.sKey = sKey; this.sMessage = sMessage; }


        public void appendMessage(String sKey, String sMessage) {
            SharedPreferences spLogFile = MyApplication.getAppContext().getSharedPreferences(BTCOMMSLOG_SHAREDPREFS, MODE_APPEND);
            SharedPreferences.Editor spsEditor = spLogFile.edit();
            Date dteNow = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String sTimeStamp = sdf.format(dteNow);

            spsEditor.putString(sKey +"|"+sTimeStamp, sTimeStamp + " # " + sMessage);
            spsEditor.commit();
        }


    }
}
