package world.frisbee.morris.led_controller;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


/**
 * Created by Maurycy on 28.02.2017.
 */

public class sequenceSummaryPage extends Activity {
    public static String TAG = "MORRIS-SEQ-PROGRAM";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//
        setContentView(R.layout.sequence_summary_page);// main
//        Intent i = getIntent();
        Bundle bundle = getIntent().getExtras();
        String message = bundle.getString("key1");
        String sToastMsg = "";
        String sLamp;
        String sRed;
        String sGreen;
        String sBlue;
        String sWhite;
        String sTime;
        boolean blContinue = true;
        String sRow;

        String arrRows[] = message.split("\n");
        for (int i = 0; blContinue; i++) {
            //arrListLampNames.add(i+1, arrRows[i]);

            //sLamp = arrRows[i].substring(0, 1);
            try {
                Log.d(TAG, "i=" + i + "; arrRows[].length=" + arrRows.length + "; arrRows[" + i + "].length()=" + arrRows[i].length() + ";value: " + arrRows[i]);
                try {
                    sLamp = arrRows[i].substring(0, 1);
                    sRed = arrRows[i].substring(1, 4);
                    sGreen = arrRows[i].substring(4, 7);
                    sBlue = arrRows[i].substring(7, 10);
                    sWhite = arrRows[i].substring(10, 13);
                    sTime = arrRows[i].substring(13);

                    sLamp = (sLamp.equals("8")) ? "LED" : sLamp;

                    sRow = "Lamp: " + sLamp + "\nRED: " + sRed + ", GREEN: " + sGreen + ", BLUE: " + sBlue + ", WHITE: " + sWhite + "\nTIME: " + sTime;
                } catch (StringIndexOutOfBoundsException e) {
                    sRow = "EMPTY SEQUENCE ROW";
                }
                if (i == 0) {
                    TextView tv = (TextView) findViewById(R.id.textViewRow1);
                    tv.setText(sRow);
                } else if (i == 1) {
                    TextView tv = (TextView) findViewById(R.id.textViewRow2);
                    tv.setText(sRow);
                } else if (i == 2) {
                    TextView tv = (TextView) findViewById(R.id.textViewRow3);
                    tv.setText(sRow);
                } else if (i == 3) {
                    TextView tv = (TextView) findViewById(R.id.textViewRow4);
                    tv.setText(sRow);
                } else if (i == 4) {
                    TextView tv = (TextView) findViewById(R.id.textViewRow5);
                    tv.setText(sRow);
                } else if (i == 5) {
                    TextView tv = (TextView) findViewById(R.id.textViewRow6);
                    tv.setText(sRow);
                }

                //sToastMsg += sLamp + " " + sRed + " " + sGreen + " " + sBlue + " " + sWhite + " " + sTime + "\n";
            } catch (ArrayIndexOutOfBoundsException e) {
                blContinue = false;
            }
        }

        //Toast.makeText(this, sToastMsg, Toast.LENGTH_LONG).show();
    }
}
