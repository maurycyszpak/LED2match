package com.kiand.LED2match;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class MyListAdapter extends Activity {
    ArrayList<HashMap<String,String>> listItems = new ArrayList<HashMap<String, String>>();

    private final Activity context;



    public MyListAdapter(Activity context, ArrayList<HashMap<String, String>> listItems) {

        //super(context, R.layout.prg_sequence_listitem_view);
        // TODO Auto-generated constructor stub

        this.context=context;

    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.prg_sequence_listitem_view, null,true);

        TextView titleText = (TextView) rowView.findViewById(R.id.title1);
        //ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        TextView subtitleText = (TextView) rowView.findViewById(R.id.title2);

        //titleText.setText(maintitle[position]);
        //imageView.setImageResource(imgid[position]);
        //subtitleText.setText(subtitle[position]);

        return rowView;

    };

    public void addItem(String sTitle, String sTimer) {
        HashMap<String, String> temp = new HashMap<>();
        temp.put("title", sTitle);
        temp.put("timer", sTimer);
        listItems.add(temp);
    }

    public void addRow(String sTitle, String sTimer) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.prg_sequence_listitem_view, null,true);

        TextView titleText = (TextView) rowView.findViewById(R.id.title1);
        TextView subtitleText = (TextView) rowView.findViewById(R.id.title2);

        titleText.setText(sTitle);
        subtitleText.setText(sTimer);
    };
}