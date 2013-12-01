package com.utoronto.syncgallery.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Specifically extends LinearLayout, define the IconView widget
 * 
 * @author KOBE
 * 
 */

public class IconView extends LinearLayout {
	private ImageView mIcon;
	private TextView mFileName;

	public IconView(Context context, Bitmap bitmap, String filename) {
		super(context);

		setOrientation(VERTICAL);
		setPadding(3, 3, 3, 3);
		setGravity(Gravity.CENTER_HORIZONTAL);

		mIcon = new ImageView(context);
		mIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		mIcon.setImageBitmap(bitmap);
		addView(mIcon, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));

		mFileName = new TextView(context);
		mFileName.setSingleLine();
		mFileName.setEllipsize(TextUtils.TruncateAt.END);
		mFileName.setText(filename);
		mFileName.setTextColor(Color.BLACK);
		addView(mFileName, new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

	}

	public void select() {
		mFileName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
	}

	public void deselect() {
		mFileName.setEllipsize(TextUtils.TruncateAt.END);
	}

	public void setIconBitmap(Bitmap bm) {
		mIcon.setImageBitmap(bm);
	}

	public void setFileName(String fileName) {
		mFileName.setText(fileName);
	}

}
