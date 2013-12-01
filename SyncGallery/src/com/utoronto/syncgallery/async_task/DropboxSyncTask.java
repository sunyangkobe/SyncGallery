package com.utoronto.syncgallery.async_task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;
import com.utoronto.syncgallery.AlbumActivity;
import com.utoronto.syncgallery.R;
import com.utoronto.syncgallery.utils.DropboxAuthSession;
import com.utoronto.syncgallery.utils.SyncGalleryConstants;
import com.utoronto.syncgallery.utils.SyncGalleryUtils;

public class DropboxSyncTask extends AsyncTask<Void, Void, Void> {

	ArrayList<String> localPictureArray = new ArrayList<String>();
	ArrayList<String> localDirArray = new ArrayList<String>();

	NotificationManager mNotificationManager;
	Notification.Builder nBuilder;
	AlbumActivity mGalleryActivity;

	public DropboxSyncTask(NotificationManager nm, AlbumActivity ga) {
		mGalleryActivity = ga;
		mNotificationManager = nm;
		nBuilder = new Notification.Builder(mGalleryActivity)
				.setAutoCancel(true)
				.setSmallIcon(R.drawable.small_logo)
				.setContentText("SyncGallery+")
				.setContentTitle("SyncGallery Notification")
				.setContentIntent(
						PendingIntent.getActivity(mGalleryActivity, 0,
								new Intent(), 0));
	}

	@Override
	protected void onPreExecute() {
		/** Sets up the message to be displayed in notification bar */
		mNotificationManager.notify(1,
				nBuilder.setContentText("Syncing... Touch to Cancel")
						.getNotification());
	}

	@Override
	protected Void doInBackground(Void... params) {
		/** Start by looking in root directory */
		directory("/");
		return null;
	}

	protected void directory(String dir) {
		/** Recursive function that will be update files at every directory */
		// If we have an empty directory, we create it on dropbox
		File[] localPictureList = new File(
				SyncGalleryConstants.Gallery_SYNC_DIR + dir).listFiles();

		check(mGalleryActivity.fileList());

		final Entry rootEntries = getRootEntries(dir);
		if (rootEntries == null) {
			return;
		}
		final List<DropboxAPI.Entry> dropboxPictureList = rootEntries.contents;

		if (localPictureList == null) {
			localPictureList = createDropboxFolder(dir);
		}

		// Loop through all the files on current directory, and updates each if
		// necessary
		for (File f : localPictureList) {
			if (!f.isHidden()) {
				if (f.isDirectory()) {
					dealWithDirectory(f);
				} else {
					dealWithImage(f);
				}
			}
		}

		// Loops through files in Dropbox and updates if necessary
		for (Entry e : dropboxPictureList) {
			String filename2 = e.path;
			if (!e.isDir) {
				if ((!localPictureArray.contains(filename2)) && (!e.isDeleted)) {
					download(e);
				}
			} else {
				if (!localDirArray.contains(filename2)) {
					File newDir = new File(
							SyncGalleryConstants.Gallery_SYNC_DIR + filename2);
					newDir.mkdirs();
					directory(filename2);
				}
			}
		}
		return;
	}

	private File[] createDropboxFolder(String dir) {
		try {
			DropboxAuthSession.getInstance().getApi().createFolder(dir);
		} catch (DropboxException e1) {
			e1.printStackTrace();
		}
		return new File[0];
	}

	private Entry getRootEntries(String dir) {
		try {
			return DropboxAuthSession.getInstance().getApi()
					.metadata(dir, 0, null, true, null);
		} catch (DropboxServerException e) {
			if (e.error == DropboxServerException._404_NOT_FOUND) {
				try {
					DropboxAuthSession.getInstance().getApi().createFolder(dir);
				} catch (DropboxException e1) {
					e1.printStackTrace();
				}
			}
			return null;
		} catch (DropboxException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void dealWithDirectory(File f) {
		String dirname = "/"
				+ f.getAbsolutePath().split(
						Environment.DIRECTORY_PICTURES + "/SyncGallery")[1];
		if (dirname.charAt(1) == '/')
			dirname = dirname.substring(1);
		directory(dirname);
		localDirArray.add(dirname);
	}

	private void dealWithImage(File f) {
		Entry DropboxEntry = null;
		String filename = f.getAbsolutePath().split(
				Environment.DIRECTORY_PICTURES + "/SyncGallery")[1];
		localPictureArray.add(filename);
		try {
			DropboxEntry = DropboxAuthSession.getInstance().getApi()
					.metadata(filename, 0, null, false, null);
			// Meta data for local file
			FileInputStream meta = mGalleryActivity
					.openFileInput(SyncGalleryUtils.name2Meta(filename
							.substring(1)) + ".meta");

			// Meta data for the most recent file
			FileInputStream newest = mGalleryActivity
					.openFileInput(SyncGalleryUtils.name2Meta(filename
							.substring(1)) + ".newest");
			// Meta data of file on Dropbox
			String revValue = DropboxEntry.rev;

			BufferedReader br = new BufferedReader(new InputStreamReader(meta));
			String metaValue = br.readLine();
			br = new BufferedReader(new InputStreamReader(newest));
			String newestValue = br.readLine();
			meta.close();
			newest.close();

			// if file on dropbox is newer
			if ((metaValue.equals(newestValue))
					&& !(revValue.equals(newestValue))) {
				if (DropboxEntry.isDeleted) {
					f.delete();
					mGalleryActivity.deleteFile(SyncGalleryUtils
							.name2Meta(filename.substring(1)) + ".meta");
					mGalleryActivity.deleteFile(SyncGalleryUtils
							.name2Meta(filename.substring(1)) + ".newest");
				} else {
					download(DropboxEntry);
					Log.i("DOWLOAD", "Downloaded " + DropboxEntry.path);
				}
				// if file on phone is newer
			} else if (!(metaValue.equals(newestValue))
					&& (revValue.equals(newestValue))) {
				upload(f);
				Log.i("UPLOAD", "Uploaded " + f.getPath());
			}

		} catch (DropboxServerException e1) {
			// Tried to get meta data for a file that does not exist
			// on dropbox
			// that means it is a new file, so we upload
			if (e1.error == DropboxServerException._404_NOT_FOUND) {
				upload(f);
				Log.i("UPLOAD", "Uploaded " + f.getPath());
			}
		} catch (DropboxException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e) {
			// Cannot open meta data for file
			// If the file is not deleted on dropbox we download
			if (!(DropboxEntry.isDeleted)) {
				download(DropboxEntry);
				Log.i("DOWLOAD", "Downloaded " + DropboxEntry.path);
			} else {
				upload(f);
				Log.i("UPLOAD", "Uploaded " + f.getPath());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		// Set the new notification
		mNotificationManager.notify(1, nBuilder.setContentText("Synced")
				.getNotification());
		// Tells the device to search for new images
		mGalleryActivity
				.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
						.parse("file://"
								+ Environment.getExternalStorageDirectory())));
		if (mGalleryActivity.getmCurrentDir().getAbsolutePath()
				.startsWith(SyncGalleryConstants.Gallery_SYNC_DIR)) {
			mGalleryActivity.browseTo(mGalleryActivity.getmCurrentDir());
		}
		Toast.makeText(mGalleryActivity, "Synchronization is done!",
				Toast.LENGTH_SHORT).show();
	}

	protected void check(String[] files) {
		/**
		 * Checks the .meta and .newest files. If there is a .newest but not a
		 * .meta file, that means that file has been deleted on local device so
		 * we need to delete from dropbox.
		 */

		for (String filename : files) {
			String newFilename = filename.split(".meta|.newest")[0];
			Log.i("SyncGallery+", "meta file path is " + filename);
			try {
				mGalleryActivity.openFileInput(newFilename + ".meta");
			} catch (FileNotFoundException e) {
				try {
					DropboxAuthSession
							.getInstance()
							.getApi()
							.delete("/"
									+ SyncGalleryUtils.meta2Name(newFilename));
					mGalleryActivity.deleteFile(filename);
				} catch (DropboxException e1) {
					e1.printStackTrace();
				}
			}

		}
	}

	protected void download(Entry e) {
		/** Downloads entry from dropbox */

		File file = new File(SyncGalleryConstants.Gallery_SYNC_DIR, e.path);
		OutputStream os;
		try {
			os = new FileOutputStream(file);
			DropboxAuthSession.getInstance().getApi()
					.getFile(e.path, null, os, null);
			os.close();

			// Update new .meta and .newest files
			FileOutputStream fos = mGalleryActivity.openFileOutput(
					SyncGalleryUtils.name2Meta(e.path.substring(1)) + ".meta",
					Context.MODE_PRIVATE);
			fos.write(e.rev.getBytes());
			fos.close();
			FileOutputStream fos2 = mGalleryActivity
					.openFileOutput(
							SyncGalleryUtils.name2Meta(e.path.substring(1))
									+ ".newest", Context.MODE_PRIVATE);
			fos2.write(e.rev.getBytes());
			fos2.close();

			// Refresh thumbnail
			File thumbfile = new File(file.getParentFile().getAbsolutePath()
					+ SyncGalleryConstants.Gallery_THUMBNAILS + file.getName());
			thumbfile.delete();

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (DropboxException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}

	protected void upload(File f) {
		/** Upload file to dropbox */

		String pathname = f.getAbsolutePath().split(
				Environment.DIRECTORY_PICTURES + "/SyncGallery")[1];
		try {
			FileInputStream file = new FileInputStream(f);
			Entry fileinfo = DropboxAuthSession.getInstance().getApi()
					.putFile(pathname, file, file.available(), null, null);

			// Update/Create .meta .newest files
			FileOutputStream fos = mGalleryActivity
					.openFileOutput(
							SyncGalleryUtils.name2Meta(pathname.substring(1))
									+ ".meta", Context.MODE_PRIVATE);
			fos.write(fileinfo.rev.getBytes());
			fos.close();
			FileOutputStream fos2 = mGalleryActivity.openFileOutput(
					SyncGalleryUtils.name2Meta(pathname.substring(1))
							+ ".newest", Context.MODE_PRIVATE);
			fos2.write(fileinfo.rev.getBytes());
			fos2.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (DropboxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
