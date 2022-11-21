package com.morris.LEDbar_controller;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface.OnDismissListener;
import android.os.Handler;

abstract class ProgressScan
{
	private static ProgressDialog createProgressDialog(Context context, String message)
	{
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setIndeterminate(false);
		dialog.setMessage(message);

		return dialog;
	}


	public static void indeterminateInternal(Context context, final Handler handler, String message, final Runnable runnable,
			OnDismissListener dismissListener, boolean cancelable)
	{
		final ProgressDialog dialog = createProgressDialog(context, message);
		dialog.setCancelable(cancelable);

		if (dismissListener != null)
		{
			dialog.setOnDismissListener(dismissListener);
		}
		dialog.show();

		new Thread() {

			@Override
			public void run()
			{
				runnable.run();

				handler.post(new Runnable() {

					public void run()
					{
						try
						{
							dialog.dismiss();
						}
						catch (Exception e)
						{
                            // nop.
						}
					}
				});
			}
        }.start();
	}
}
