package com.utoronto.syncgallery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
//import android.widget.GridView;
import android.widget.ListView;

import com.bumptech.bumpapi.BumpAPI;
import com.bumptech.bumpapi.BumpAPIListener;
import com.bumptech.bumpapi.BumpConnectFailedReason;
import com.bumptech.bumpapi.BumpConnection;
import com.bumptech.bumpapi.BumpDisconnectReason;
import com.utoronto.syncgallery.adapter.IconAdapter;
import com.utoronto.syncgallery.async_task.DropboxSyncTask;
import com.utoronto.syncgallery.receivers.FolderListener;
import com.utoronto.syncgallery.receivers.LocalFileListener;
import com.utoronto.syncgallery.receivers.SyncFileListener;
import com.utoronto.syncgallery.utils.DropboxAuthSession;
import com.utoronto.syncgallery.utils.GalleryOperations;
import com.utoronto.syncgallery.utils.SyncGalleryConstants;
import com.utoronto.syncgallery.utils.SyncGalleryUtils;
import com.utoronto.syncgallery.widget.okDialogBuilder;

/**
 * AlbumActivity (original GalleryActivity)
 * 
 * @author KOBE, Daniel, Sarah
 * 
 */

public class AlbumActivity extends Activity implements OnItemClickListener,
		OnItemLongClickListener, BumpAPIListener {

	private ListView gallery;
	private File mCurrentDir;
	private ArrayList<File> mFiles;
	private ArrayList<File> mImages;
	private BumpConnection conn;
	private ProgressDialog bumpDialog;
	private File cameraOutput;
	private static boolean dirty = false;

	/** Called when activity is invoked */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFiles = new ArrayList<File>();
		mImages = new ArrayList<File>();
		mCurrentDir = new File(SyncGalleryConstants.Gallery_ENTRY_DIR);
		bumpDialog = new ProgressDialog(this);
		bumpDialog.setTitle("SyncGallery+");
		bumpDialog.setMessage("Pic is on the way! \nPlease wait...");
		bumpDialog.setIndeterminate(true);
		bumpDialog.setCancelable(false);

		//getAlbumList();

		// determine whether to go for authentication
		if (verifyRequest()) {
			DropboxAuthSession.getInstance().startAuthentication(
					this.getApplicationContext());
		} else {
			deployLayout();
		}
		browseTo(mCurrentDir);
	}

	/** Fired after the application is resumed or finish Dropbox authentication */
	@Override
	protected void onResume() {
		super.onResume();
		if (DropboxAuthSession.getInstance().getDBSession()
				.authenticationSuccessful()) {
			try {
				if (verifyRequest()) {
					DropboxAuthSession.getInstance().finishAuthentication(this);
					deployLayout();
				}
			} catch (IllegalStateException e) {
				Log.i("DbAuthLog", "Error authenticating", e);
			}
		} else {
			DropboxAuthSession.getInstance().unlink();
		}

		// check dirty bit and refresh the folder
		if (isDirty()) {
			browseTo(mCurrentDir);
			setDirty(false);
		}
	}

	/** Fired after the application is resumed from Camera or Bump */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (!SyncGalleryUtils.sdCardMounted(this)) {
			return;
		}

		if (requestCode == SyncGalleryConstants.CAMERA_PIC_REQUEST
				&& resultCode == RESULT_OK) {
			try {
				ExifInterface exif = new ExifInterface(getCameraOutput()
						.getAbsolutePath());
				int orientation = exif.getAttributeInt(
						ExifInterface.TAG_ORIENTATION,
						ExifInterface.ORIENTATION_NORMAL);
				int rotate = 0;
				switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_270:
					rotate += 90;
				case ExifInterface.ORIENTATION_ROTATE_180:
					rotate += 90;
				case ExifInterface.ORIENTATION_ROTATE_90:
					rotate += 90;
				}
				SyncGalleryUtils.rotateImage(getCameraOutput(), rotate);
				setDirty(true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (requestCode == SyncGalleryConstants.BUMP_INIT_REQUEST) {
			if (resultCode == RESULT_OK) {
				bumpDialog.show();
				conn = (BumpConnection) data
						.getParcelableExtra(BumpAPI.EXTRA_CONNECTION);
				conn.setListener(this);
			} else {
				BumpConnectFailedReason reason = (BumpConnectFailedReason) data
						.getSerializableExtra(BumpAPI.EXTRA_REASON);
				Log.i("KOBE", reason.toString());
			}
		}
	}

	/** Return true if user chooses to connect to cloud */
	private boolean verifyRequest() {
		return !DropboxAuthSession.getInstance().isLinked()
				&& getIntent().getExtras().getBoolean("con2Cloud");
	}

	/** Deploy the UI and attach the listeners */
	private final void deployLayout() {
		if (gallery == null) {
			setContentView(R.layout.new_gallery);
			gallery = (ListView) findViewById(R.id.gallery_list);
			gallery.setOnItemClickListener(this);
			gallery.setOnItemLongClickListener(this);
			gallery.setAdapter(new IconAdapter(this));
		}
	}

	/** Implements onItemClickListener */
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if (!SyncGalleryUtils.sdCardMounted(this)) {
			return;
		}

		final File file = mFiles.get((int) id);
		if (file.isDirectory()) {
			browseTo(file);
		} else {
			// Open the picture in full screen
			switchToView(file);
		}
	}

	/** Implements onItemLongClickListener */
	public boolean onItemLongClick(AdapterView<?> parentView, View view,
			int arg2, long id) {
		if (!SyncGalleryUtils.sdCardMounted(this)) {
			return true;
		}

		final File file = mFiles.get((int) id);
		if (!file.getParentFile().equals(getmCurrentDir())) {
			return true;
		}

		final GalleryOperations operations = new GalleryOperations(this, file);

		final Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Operations");
		builder.setIcon(R.drawable.operations);

		// Folder operations.
		if (file.isDirectory()) {
			// No operation for the parent folder.
			Log.i("SyncGallery+", "regular folder clicked");
			builder.setItems(R.array.fileoperation, new FolderListener(
					operations));
		} else if (file.getParent().equals(
				SyncGalleryConstants.Gallery_SYNC_DIR)) {
			// Images operations.
			Log.i("SyncGallery+", "images inside sync folder clicked");
			builder.setItems(R.array.imageoperation1, new SyncFileListener(
					operations));
		} else {
			Log.i("SyncGallery+", "regular images clicked");
			builder.setItems(R.array.imageoperation2, new LocalFileListener(
					operations));
		}

		builder.create();
		builder.show();
		return true;
	}

	/** browseTo is the central engine to change the UI */
	public synchronized void browseTo(final File location) {
		browseTo(location, null);
	}

	/**
	 * browseTo is the central engine to change the UI, String filter helps to
	 * filter out unwanted files
	 */
	public synchronized void browseTo(final File location, final String filter) {
		mCurrentDir = location;
		mFiles.clear();
		mImages.clear();
		int count = 0;
		boolean thumbExists = true;

		this.setTitle(mCurrentDir.getName().equals("") ? mCurrentDir.getPath()
				: mCurrentDir.getName());

		if (mCurrentDir.getParentFile() != null && filter == null) {
			mFiles.add(mCurrentDir.getParentFile());
			count++;
		}

		for (File file : mCurrentDir.listFiles()) {
			// apply filter on the results
			if (filter != null
					&& !file.getName().toLowerCase()
							.startsWith(filter.toLowerCase())) {
				continue;
			}
			// hide .thumbnail folder
			if (file.isDirectory() && !file.getName().equals(".thumbnails")) {
				mFiles.add(count, file);
				count++;
			} else {
				for (String ext : getmImageExt()) {
					// add images to an array for future use
					if (file.getName().toLowerCase().endsWith(ext)) {
						mFiles.add(file);
						mImages.add(file);
						thumbExists &= new File(mCurrentDir.getAbsolutePath()
								+ SyncGalleryConstants.Gallery_THUMBNAILS
								+ file.getName()).exists();
						continue;
					}
				}
			}
		}

		// create thumbnail if it doesn't exist
		if (!thumbExists && mImages.size() > 0) {
			new CreateThumbTask().execute();
		} else if (gallery != null) {
			gallery.setAdapter(new IconAdapter(this));
		}
	}

	/**
	 * The AsyncTask creates the thumbnail and show the progress dialog
	 * Producing the thumbnail is a heavy work, need multithread here.
	 * */
	private class CreateThumbTask extends AsyncTask<Void, Void, Void> {
		final ProgressDialog dialog = new ProgressDialog(AlbumActivity.this) {
			@Override
			public void onBackPressed() {
				super.onBackPressed();
				CreateThumbTask.this.cancel(true);
				mCurrentDir = mCurrentDir.getParentFile();
				browseTo(mCurrentDir);
			};
		};

		@Override
		protected Void doInBackground(Void... params) {
			for (File image : mImages) {
				SyncGalleryUtils.getImageThumbnail(image);
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			dialog.setTitle("SyncGallery+");
			dialog.setMessage("Loading... Please wait...\n\nThis may take a few secs to finish for the first time...");
			dialog.setIndeterminate(true);
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();
		}

		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();
			if (gallery != null) {
				gallery.setAdapter(new IconAdapter(AlbumActivity.this));
			}
		}
	}

	/** Redefine the back button */
	@Override
	public void onBackPressed() {
		if (mCurrentDir.getParentFile() == null
				|| mCurrentDir.getAbsolutePath().equals(
						SyncGalleryConstants.Gallery_ENTRY_DIR)) {
			super.onBackPressed();
		} else {
			browseTo(mCurrentDir.getParentFile());
		}
	}

	/** Option menu items */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.gallery_menu, menu);
		return true;
	}

	/** Only show sync and logout button when authenticated. */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.setGroupVisible(R.id.authenticatedgroup, DropboxAuthSession
				.getInstance().isLinked());
		return true;
	}

	/** Menu button. */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!SyncGalleryUtils.sdCardMounted(this)) {
			return true;
		}

		final GalleryOperations operations = new GalleryOperations(this,
				mCurrentDir);
		switch (item.getItemId()) {
		case R.id.add:
			operations.createFolder();
			return true;
			// Call built-in camera.
		case R.id.camera:
			operations.startCameraIntent();
			return true;
			// Perform search.
		case R.id.search:
			onSearchRequested();
			return true;
		case R.id.receive:
			operations.startBumpIntent();
			return true;
		case R.id.home:
			/** Build the rename UI interface. */
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/** Group Menu button. */
	public void onAuthenticatedItemClick(MenuItem item) {
		if (!SyncGalleryUtils.sdCardMounted(this)) {
			return;
		}

		switch (item.getItemId()) {
		// Synchronize using Dropbox.
		case R.id.sync:
			NotificationManager notificationmanager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			new DropboxSyncTask(notificationmanager, this).execute();
			break;
		// Log out Dropbox.
		case R.id.logout:
			new okDialogBuilder(this, "Logout", "Are you sure?",
					new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							DropboxAuthSession.getInstance().unlink();
							finish();
						}
					}).setNegativeButton("Cancel", null).show();
			break;
		}
	}

	/** Call built-in search UI and perform search. */
	@Override
	public boolean onSearchRequested() {
		SearchActivity.TARGETDIR = getmCurrentDir();
		startSearch(null, false, null, false);
		return true;
	}

	/** Invoked when the data is received from peer */
	@Override
	public void bumpDataReceived(byte[] chunk) {
		try {
			Log.i("KOBE", conn.getOtherUserName());
			String filePath = mCurrentDir.getAbsolutePath() + File.separator
					+ SyncGalleryUtils.generateDCIMFilename();
			FileOutputStream output = new FileOutputStream(filePath);
			output.write(chunk);
			bumpDialog.dismiss();
			setDirty(true);
			// Show the result on the fullscreen
			switchToView(new File(filePath));
		} catch (Exception e) {
			Log.e("KOBE", "Failed to parse incoming data");
			e.printStackTrace();
		}
	}

	/** Invoked when bump connection is lost for whatever reason */
	@Override
	public void bumpDisconnect(BumpDisconnectReason reason) {
		switch (reason) {
		case END_OTHER_USER_QUIT:
			Log.i("KOBE", "--- " + conn.getOtherUserName() + " QUIT ---");
			break;
		case END_OTHER_USER_LOST:
			Log.i("KOBE", "--- " + conn.getOtherUserName() + " LOST ---");
			break;
		}
	}

	/** Start the fullscreen view activity */
	private final void switchToView(File file) {
		Intent fullScreen = new Intent(this, ViewpicActivity.class);
		fullScreen.putExtra("file", file);
		startActivity(fullScreen);
	}

	/** Accesser */
	public File getmCurrentDir() {
		return mCurrentDir;
	}

	/** List of files and directories. */
	public ArrayList<File> getmFiles() {
		return mFiles;
	}

	/** Get image extension. */
	public String[] getmImageExt() {
		return getResources().getStringArray(R.array.fileEndingImage);
	}

	public static void setDirty(boolean isDirty) {
		dirty = isDirty;
	}

	public static boolean isDirty() {
		return dirty;
	}

	public File getCameraOutput() {
		return cameraOutput;
	}

	public void setCameraOutput(File cameraOutput) {
		this.cameraOutput = cameraOutput;
	}

	/*private void getAlbumList() {
		// which image properties are we querying
		// KOBE: a temporary hack to deal with group by
		String[] projection = new String[] {
				"MIN(" + MediaStore.Images.Media.BUCKET_ID + ") AS "
						+ MediaStore.Images.Media.BUCKET_ID,
				MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
				"MIN(" + MediaStore.Images.Media.DATE_TAKEN + ") AS "
						+ MediaStore.Images.Media.DATE_TAKEN,
				"MIN(" + MediaStore.Images.Media.DATA + ") AS "
						+ MediaStore.Images.Media.DATA };

		// Get the base URI for the People table in the Contacts content
		// provider.
		Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

		// Make the query.
		// KOBE: a temporary hack to add group by clause
		final Cursor cur = new CursorLoader(this, images, projection,
				"1 = 1) GROUP BY ("
						+ MediaStore.Images.Media.BUCKET_DISPLAY_NAME, null, "").loadInBackground();

		Log.i("SyncGallery+", " query count=" + cur.getCount());

		if (cur.moveToFirst()) {
			int bucketColumn = cur
					.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

			int dateColumn = cur
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);

			int dataColumn = cur
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

			do {
				// Get the field values
				final String bucket = cur.getString(bucketColumn);
				final String date = cur.getString(dateColumn);
				final String filePath = cur.getString(dataColumn);
				final int secondLastSeparator = filePath.lastIndexOf(
						File.separator,
						filePath.lastIndexOf(File.separator) - 1);
				final String albumPath = filePath.substring(0,
						secondLastSeparator);

				// Do something with the values.
				Log.i("SyncGallery+", " bucket=" + bucket + "  date_taken="
						+ date + " path=" + albumPath);
			} while (cur.moveToNext());
		}
	}*/
}