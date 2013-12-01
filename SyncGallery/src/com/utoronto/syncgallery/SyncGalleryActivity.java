package com.utoronto.syncgallery;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.utoronto.syncgallery.utils.SyncGalleryConstants;
import com.utoronto.syncgallery.utils.SyncGalleryUtils;

/**
 * SyncGalleryActivity is the entry class of the entire project
 * 
 * @author Kobe Sun
 * 
 */

public class SyncGalleryActivity extends Activity implements OnClickListener {
	Button conButton;
	Button galleryButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		/** Button to connect to dropbox. */
		conButton = (Button) findViewById(R.id.connect2cloud);
		conButton.setClickable(true);
		conButton.setOnClickListener(this);

		/** Button to enter gallery. */
		galleryButton = (Button) findViewById(R.id.offline_gallery);
		galleryButton.setClickable(true);
		galleryButton.setOnClickListener(this);
	}

	/** Dispatcher function */
	public void onClick(View v) {
		if (!SyncGalleryUtils.sdCardMounted(this)) {
			return;
		}

		/** Go to gallery, pass con2Cloud as a flag. */
		Intent intentMain = new Intent(this, AlbumActivity.class);
		intentMain.putExtra("con2Cloud", v == conButton);
		File sdcardFolder = new File(SyncGalleryConstants.Gallery_SYNC_DIR);
		if (!sdcardFolder.exists()) {
			sdcardFolder.mkdirs();
		}
		startActivity(intentMain);
	}
}
