package com.utoronto.syncgallery.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;

public class okDialogBuilder extends AlertDialog.Builder {

	public okDialogBuilder(Context context, String title, String msg) {
		this(context, title, msg, null);
	}

	public okDialogBuilder(Context context, String title, String msg,
			OnClickListener listener) {
		super(context);
		this.setTitle(title);
		this.setMessage(msg);
		this.setPositiveButton("OK", listener);
	}

}
