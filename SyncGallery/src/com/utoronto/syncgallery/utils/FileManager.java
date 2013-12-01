package com.utoronto.syncgallery.utils;

import java.io.File;
import java.util.Date;

import android.util.Log;
import android.widget.Toast;

import com.utoronto.syncgallery.AlbumActivity;
import com.utoronto.syncgallery.widget.okDialogBuilder;

/**
 * FileManager is the class to handle files.
 * 
 * @author Kobe Sun
 * 
 */

public class FileManager {

	private AlbumActivity gallery;

	public FileManager(AlbumActivity gallery) {
		this.gallery = gallery;
	}

	/** Rename the file with new file name newname. */
	public boolean rename(File file, String newname) {
		/** Rename the file. */
		String extension = file.isDirectory() ? "" : getExtension(file
				.getName());
		File tarFile = new File(file.getParent() + File.separator + newname
				+ extension);

		if (!nameFormatChecking("Rename", tarFile, newname)) {
			return false;
		}

		if (!file.renameTo(tarFile)) {
			new okDialogBuilder(gallery, "Rename", "Rename Fail!").show();
			return false;
		}

		Toast.makeText(gallery,
				file.getName() + " has been renamed to " + tarFile.getName(),
				Toast.LENGTH_SHORT).show();
		return true;
	}

	/** Return the file extension. */
	private String getExtension(String filename) {
		/** Check last index start before ".". */
		if ((filename != null) && (filename.length() > 0)) {
			int i = filename.lastIndexOf('.');

			/** Save the extension string. */
			if ((i > -1) && (i < (filename.length() - 1))) {
				return '.' + filename.substring(i + 1);
			} else {
				return "";
			}
		} else {
			return "";
		}
	}

	/** Delete selected folder. */
	public boolean delete(final File file) {
		/** Set the UI interface. */
		String filename = file.getName();
		if (!deleteHelper(file)) {
			new okDialogBuilder(gallery, "Delete", "Deletion failed!").show();
			return false;
		}

		Toast.makeText(gallery, filename + " has been successfully deleted!",
				Toast.LENGTH_SHORT).show();
		return true;
	}

	/** delete helper function. */
	private boolean deleteHelper(File parent) {
		// Check if there is any files in the folder, if yes, delete it.
		if (!parent.isDirectory()) {
			return parent.delete();
		}

		for (File child : parent.listFiles()) {
			if (!deleteHelper(child)) {
				return false;
			}
		}
		return parent.delete();
	}

	public void deleteMetaFile(final File file) {
		if (file.getAbsolutePath().startsWith(
				SyncGalleryConstants.Gallery_SYNC_DIR)) {
			final String filename = file.getAbsolutePath().split(
					SyncGalleryConstants.Gallery_SYNC_DIR)[1];
			gallery.deleteFile(SyncGalleryUtils.name2Meta(filename
					.substring(1)) + ".meta");
		}
	}

	/** Move the selected file to SyncGallery folder. */
	public boolean copyToSyncGallery(File srfile) {
		/** Path for writing. */
		File desFile = new File(SyncGalleryConstants.Gallery_SYNC_DIR
				+ File.separator + srfile.getName());
		Log.i("SyncGallery+", "Destination File Path is " + desFile);

		if (!nameFormatChecking("Copy to SyncGallery", desFile,
				desFile.getName())) {
			return false;
		}

		/** Check the SyncGallery folder. If it does not exist, create it. */
		File destDir = new File(SyncGalleryConstants.Gallery_ENTRY_DIR);
		if (!destDir.exists()) {
			destDir.mkdirs();
		}

		if (!SyncGalleryUtils.copyfile(srfile, desFile)) {
			new okDialogBuilder(gallery, "Copy to SyncGallery", "Copy Failed!")
					.show();
			return false;
		}

		Toast.makeText(
				gallery,
				srfile.getName()
						+ " has been copied to SyncGallery folder and is ready to be synced!",
				Toast.LENGTH_SHORT).show();
		return true;
	}

	/** Create a folder in the current directory. */
	public boolean createFolder(File file, String name) {
		File tarFile = new File(file.getAbsolutePath() + File.separator + name);

		if (!nameFormatChecking("Create a new folder", tarFile, name)) {
			return false;
		}

		if (!tarFile.mkdir()) {
			new okDialogBuilder(gallery, "Create a new folder", "Create Fail!")
					.show();
			return false;
		}

		Toast.makeText(gallery,
				tarFile.getName() + " folder has been successfully created!",
				Toast.LENGTH_SHORT).show();
		return true;
	}

	public void showProperties(File file) {
		StringBuilder msg = new StringBuilder();
		msg.append("File Name: " + file.getName());
		msg.append("\n");
		msg.append("Path: " + file.getAbsolutePath());
		msg.append("\n");
		if (!file.isDirectory()) {
			msg.append("Size: "
					+ SyncGalleryUtils.formatFileSize(file.length()));
			msg.append("\n");
		}
		msg.append("Modified: " + new Date(file.lastModified()).toString());
		msg.append("\n");
		new okDialogBuilder(gallery, "Properties", msg.toString()).show();
	}

	private boolean nameFormatChecking(String title, File tarFile, String name) {
		if (name.equals("")) {
			new okDialogBuilder(gallery, title, "Name cannot be empty!").show();
			return false;
		}

		/** Return Error if there is invalid input string. */
		for (char ch : SyncGalleryConstants.Gallery_RESERVEDCHARS.toCharArray()) {
			if (name.contains(Character.toString(ch))) {
				new okDialogBuilder(gallery, title,
						"Name contains invalid characters!").show();
				return false;
			}
		}

		if (!tarFile.getAbsolutePath().contains(
				SyncGalleryConstants.Gallery_THUMBNAILS)
				&& tarFile.exists()) {
			/** A same name file already exists in SyncGallery folder. */
			new okDialogBuilder(gallery, title, "Same name already exists!")
					.show();
			return false;
		}
		return true;
	}

}
