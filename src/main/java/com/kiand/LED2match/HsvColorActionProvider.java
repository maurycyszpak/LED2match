package com.kiand.LED2match;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Toast;
import android.widget.ToggleButton;

public class HsvColorActionProvider extends ActionProvider {

	public interface OnColorChangedListener {

		public void onColorChanged(int color);
		public void onColorSelected(int color);
		public void onCanceled();
	}
	public static String TAG = "MORRIS-HSV_CAP";

	private OnColorChangedListener mListener;
	private Context mContext;
	private LayoutInflater mLayoutInflater;
	private PopupWindow mPopupWindow;
	private HsvColorPickerView mColorPicker;

	private int mInitialColor;
	String _strMacAddress = "";

	public HsvColorActionProvider(Context context) {
		super(context);

		mContext = context;

		mInitialColor = Color.HSVToColor(new float[] { 180, 0.7f, 0.8f });
		mLayoutInflater = LayoutInflater.from(context);

		View v = mLayoutInflater.inflate(R.layout.color_popup, null, false);
		mColorPicker = (HsvColorPickerView) v.findViewById(R.id.color_picker);
		mColorPicker.showPreview(true);
		mColorPicker.setIniticalColor(mInitialColor);

		mColorPicker
		.setOnColorChangedListener(new HsvColorPickerView.OnColorChangedListener() {

			@Override
			public void onColorChanged(int color, float[] hsv) {
				if (mListener != null) 
				{  
					mListener.onColorChanged(color);

				}

			}
		});

		v.findViewById(R.id.cancel_btn).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						mPopupWindow.dismiss();

						if (mListener != null) {
							mListener.onCanceled();
						}
					}
				});

		v.findViewById(R.id.ok_btn).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						mInitialColor = mColorPicker.getCurrentColor();

						if (mListener != null) {
							mListener.onColorSelected(mInitialColor);
						}
						mPopupWindow.dismiss();
					}
				});

		mPopupWindow = new PopupWindow(v);
		mPopupWindow.setBackgroundDrawable(mContext.getResources().getDrawable(
				R.drawable.panel_bg));
		mPopupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		mPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
	}

	public void setOnColorChangedListener(OnColorChangedListener l) {
		mListener = l;
	}

	@Override
	public View onCreateActionView() {

		LayoutInflater layoutInflater = LayoutInflater.from(mContext);
		final View actionItem = layoutInflater.inflate(R.layout.main, null);

		SharedPreferences prefs = mContext.getSharedPreferences("MyPrefsFile",
				Context.MODE_PRIVATE);

		_strMacAddress = prefs.getString("KEY_BTMACADDRESS", "");
		/*final ToggleButton _toggleOnOff;
		//final String TAG = "HSB-CONTR";

		_toggleOnOff = (ToggleButton) actionItem
				.findViewById(R.id.toggleButtonOnOff);

		_toggleOnOff.setText("Inactive");
		_toggleOnOff.setOnClickListener(new View.OnClickListener()

		{
			public void onClick(View v) {
				int retCon = 0;
				// Perform action on clicks
				if (_toggleOnOff.isChecked()) {
					int initRet = BtCore.initBluetooth();

					Log.d(TAG, "initBluetooth: " + initRet);

					if (initRet == -1) {
						Toast.makeText(mContext, "Bluetooth is not available.",
								Toast.LENGTH_LONG).show();

//						/Log.e(TAG, "error -1");
						// finish();
					}
					if (initRet == -2) {
						Toast.makeText(
								mContext,
								"Please enable your BT and re-run this program.",
								Toast.LENGTH_LONG).show();
						//Log.e(TAG, "error -2");
						// finish();
					}

					BtCore.setServerAddress(_strMacAddress);
					//System.out.println("mac adr" + _strMacAddress);

					int crtRet = BtCore.createSocketBluetooth();
					//Log.v(TAG, "createSocketBluetooth: " + crtRet);

					if (crtRet == -1) // il dispositivo potrebbe essere spento
					{

					}

					String remoteName = BtCore.getRemoteDeviceName();
					//Log.v(TAG, "RemoteName: " + remoteName);

					retCon = BtCore.connectBluetooth();
					//Log.v(TAG, "connectBluetooth:" + retCon);

					if (retCon != 0) {
						Toast toastStart = Toast
								.makeText(mContext,
										"Unable to connect to BT socket!",
										Toast.LENGTH_LONG);
						toastStart.show();
						_toggleOnOff.setChecked(false);
						_toggleOnOff.setText("Inactive");
						//Log.v(TAG, "exit");

						return;
					} else {
						try {
							if (BtCore.Connected()) {
								*//*Intent intent = new Intent(v.getContext(), LightAdjustments.class);
								LightAdjustments objectB = new LightAdjustments();
								Log.d(TAG, "autopopulateUnitName()");
								String title = objectB.checkForNewUnitName();
								Log.d (TAG, "Title to be set: " + title);
								//objectB.readEE_presets_click(null);
								Log.d(TAG, "autopopulateUnitName() and readEE_presets completed");*//*
							}
							//Toast.makeText(App.context, "Connected to Controller over BT.", Toast.LENGTH_LONG).show();
						} catch (NullPointerException e3) {
							Log.d(TAG, "RESUME: BTCore socket is a null object", e3);
						}

						_toggleOnOff.setChecked(true);
						_toggleOnOff.setText("Connected");
					}
					//Log.v(TAG, "On");


				} else {
					if (retCon != 0) {
						_toggleOnOff.setText("Inactive");
						return;
					}
					//Log.v(TAG, "Off");

					BtCore.closeBluetooth();
				}
			}
		});*/

		return actionItem;
	}
}
