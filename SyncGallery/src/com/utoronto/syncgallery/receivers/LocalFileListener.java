package com.utoronto.syncgallery.receivers;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.utoronto.syncgallery.utils.GalleryOperations;

public class LocalFileListener implements OnClickListener {

	private GalleryOperations operations;
	
	public LocalFileListener(GalleryOperations operations) {
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
		// Move selected file to SyncGallery folder.
		case 2:
			operations.copyToSyncGallery();
			break;
		// Rename a selected file.
		case 3:
			operations.rename();
			break;
		// Show properties of the selected files.
		case 4:
			operations.showProperties();
			break;
		}
		dialog.dismiss();
	}
}
