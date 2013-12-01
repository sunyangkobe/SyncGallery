package com.utoronto.syncgallery.receivers;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;

import com.utoronto.syncgallery.utils.GalleryOperations;

public class FolderListener implements OnClickListener {

	private GalleryOperations operations;
	
	public FolderListener(GalleryOperations operations) {
		this.operations = operations;
		Log.i("SyncGallery+", "FolderListener invoked");
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		// Delete selected file.
		case 0:
			operations.delete();
			break;
		// Rename a selected file.
		case 1:
			operations.rename();
			break;
		// Show properties of the selected files.
		case 2:
			operations.showProperties();
			break;
		}
		dialog.dismiss();
	}
}
