package com.utoronto.syncgallery.receivers;

import com.utoronto.syncgallery.utils.GalleryOperations;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class SyncFileListener implements OnClickListener {

	private GalleryOperations operations;
	
	public SyncFileListener(GalleryOperations operations) {
		this.operations = operations;
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		// Delete selected file.
		case 0:
			operations.delete();
			break;
		// Share file through other activity.
		case 1:
			operations.share();
			break;
		// Rename a selected file.
		case 2:
			operations.rename();
			break;
		// Show properties of the selected files.
		case 3:
			operations.showProperties();
			break;
		}
		dialog.dismiss();
	}
}
