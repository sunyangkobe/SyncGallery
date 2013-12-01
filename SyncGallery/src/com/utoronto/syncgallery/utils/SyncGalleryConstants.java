package com.utoronto.syncgallery.utils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Environment;

import com.dropbox.client2.session.Session.AccessType;

public final class SyncGalleryConstants {

	/**
	 * DropBox Constants
	 */
	public static final String DropBox_APP_KEY = "3lzsalfwubb7bpw";
	public static final String DropBox_APP_SECRET = "enu3us3qncwjysh";
	public static final AccessType DropBox_ACCESS_TYPE = AccessType.APP_FOLDER;

	/**
	 * Aviary Constants
	 */
	public static final String Aviary_APP_KEY = "c8deade2a";
	public static final String Aviary_APP_SECRET = "90ee51062";
	public static final boolean Aviary_FAST_RENDERING = true;
	public static final int Aviary_ACTION_REQUEST_GALLERY = 99;
	public static final int Aviary_ACTION_REQUEST_FEATHER = 100;
	public static final int Aviary_EXTERNAL_STORAGE_UNAVAILABLE = 1;
	public static final String Aviary_FOLDER_NAME = "aviary";
	public static final String Aviary_LOG_TAG = "feather-launcher";

	/**
	 * Gallery Constants
	 */
	public static final int CAMERA_PIC_REQUEST = 100;
	public static final String Gallery_ENTRY_DIR = Environment
			.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
			.getAbsolutePath();
	public static final String Gallery_SYNC_DIR = Environment
			.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_PICTURES + File.separator
							+ "SyncGallery").getAbsolutePath();
	public static final String Gallery_DCIM = Environment
			.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
			.getAbsolutePath();
	public static final String Gallery_THUMBNAILS = File.separator
			+ ".thumbnails" + File.separator;
	public static final String Gallery_RESERVEDCHARS = "|\\?*<\":>+[]/'";

	/**
	 * Bump Constants
	 */
	public static final int BUMP_INIT_REQUEST = 0;
	public static final String BUMP_APP_KEY = "e76f0718f3ec4751a906ab6da3c17a9e";

	public static final String getBumpUsername(AccountManager manager) {
		Account[] accounts = manager.getAccountsByType("com.google");
		List<String> possibleEmails = new LinkedList<String>();

		for (Account account : accounts) {
			possibleEmails.add(account.name);
		}

		if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
			String email = possibleEmails.get(0);
			String[] parts = email.split("@");
			if (parts.length > 0 && parts[0] != null)
				return parts[0];
			else
				return "SyncGallery+ User";
		} else
			return "SyncGallery+ User";
	}

}
