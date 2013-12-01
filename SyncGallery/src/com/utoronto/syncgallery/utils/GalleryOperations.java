package com.utoronto.syncgallery.utils;

import java.io.File;

import android.accounts.AccountManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.EditText;

import com.bumptech.bumpapi.BumpAPI;
import com.utoronto.syncgallery.AlbumActivity;
import com.utoronto.syncgallery.IF.GalleryBasicOperations;
import com.utoronto.syncgallery.widget.okDialogBuilder;

/**
 * Define all the basic operations for the gallery
 * 
 * @author Kobe, Daniel, Jacky
 * 
 */

public class GalleryOperations implements GalleryBasicOperations {

	private AlbumActivity gallery;
	private File target;
	private FileManager fileManager;

	public GalleryOperations(AlbumActivity gallery, File file) {
		this.gallery = gallery;
		this.target = file;
		this.fileManager = new FileManager(gallery);
	}

	/** Delete selected picture */
	public void delete() {
		new okDialogBuilder(gallery, "Delete", "Are you sure?",
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						File parent = target.getParentFile();
						if (!target.isDirectory()) {
							fileManager.delete(SyncGalleryUtils
									.getThumbnailFile(target));
						}
						fileManager.delete(target);
						fileManager.deleteMetaFile(target);
						gallery.browseTo(parent);
					}
				}).setNegativeButton("Cancel", null).show();
	}

	/** Rename selected picture */
	public void rename() {
		final EditText input = new EditText(gallery);
		final int nameLength = target.isDirectory() ? target.getName().length()
				: target.getName().lastIndexOf(".");
		final String fileName = target.getName().substring(0, nameLength);
		input.setText(fileName);
		input.setSelection(0, fileName.length());

		new okDialogBuilder(gallery, "Rename", "Please enter a new name: ",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

						/** Get new file name. */
						String newname = input.getText().toString();
						Log.i("SyncGallery+", "target path is " + target);

						/** Rename both the selected file and its thumbnail. */
						if (!target.isDirectory()) {
							fileManager.rename(
									SyncGalleryUtils.getThumbnailFile(target),
									newname);
							fileManager.deleteMetaFile(target);
						}
						fileManager.rename(target, newname);

						/** Refresh the current directory. */
						gallery.browseTo(target.getParentFile());
					}
				}).setView(input).setNegativeButton("Cancel", null).show();
	}

	/** Copy the selected picture to SyncGallery folder. */
	public void copyToSyncGallery() {
		fileManager.copyToSyncGallery(target);
		gallery.browseTo(target.getParentFile());
	}

	/** Share the selected picture. */
	public void share() {
		/** Parse the intent to the Android default share menu. */
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setType("image/*");
		intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(target));
		gallery.startActivity(Intent.createChooser(intent, null));
	}

	public void createFolder() {
		final EditText input = new EditText(gallery);
		input.setWidth(30);
		new okDialogBuilder(gallery, "Create a new folder",
				"Please enter a name: ", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						/** Rename both the selected file and its thumbnail. */
						fileManager.createFolder(target, input.getText()
								.toString());
						gallery.browseTo(target);
					}
				}).setView(input).setNegativeButton("Cancel", null).show();
	}

	public void showProperties() {
		fileManager.showProperties(target);
	}

	public void startCameraIntent() {
		final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		final File output = new File(gallery.getmCurrentDir() + File.separator
				+ SyncGalleryUtils.generateDCIMFilename());
		gallery.setCameraOutput(output);
		Uri uriSavedImage = Uri.fromFile(gallery.getCameraOutput());
		intent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);
		gallery.startActivityForResult(intent,
				SyncGalleryConstants.CAMERA_PIC_REQUEST);
	}

	public void startBumpIntent() {
		final Intent bump = new Intent(gallery, BumpAPI.class);
		bump.putExtra(BumpAPI.EXTRA_USER_NAME, SyncGalleryConstants
				.getBumpUsername(AccountManager.get(gallery)));
		bump.putExtra(BumpAPI.EXTRA_API_KEY, SyncGalleryConstants.BUMP_APP_KEY);
		gallery.startActivityForResult(bump,
				SyncGalleryConstants.BUMP_INIT_REQUEST);
	}

}
