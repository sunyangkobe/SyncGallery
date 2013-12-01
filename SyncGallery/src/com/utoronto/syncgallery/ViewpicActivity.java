package com.utoronto.syncgallery;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

import com.aviary.android.feather.FeatherActivity;
import com.bumptech.bumpapi.BumpAPI;
import com.bumptech.bumpapi.BumpAPIListener;
import com.bumptech.bumpapi.BumpConnectFailedReason;
import com.bumptech.bumpapi.BumpConnection;
import com.bumptech.bumpapi.BumpDisconnectReason;
import com.utoronto.syncgallery.receivers.ShakeListener;
import com.utoronto.syncgallery.receivers.ShakeListener.OnShakeListener;
import com.utoronto.syncgallery.utils.SyncGalleryConstants;
import com.utoronto.syncgallery.utils.SyncGalleryUtils;
import com.utoronto.syncgallery.widget.TouchImageView;

/**
 * ViewpicActivity is the full screen mode for showing selected picture.
 * 
 * @author Kobe, Daniel
 * 
 *         TODO: add gesture to this class
 * 
 */

public class ViewpicActivity extends Activity implements BumpAPIListener,
		OnShakeListener {
	private TouchImageView imageView;
	private ShakeListener mShaker;
	private BumpConnection conn;
	private File image;
	private Bitmap bm;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Create bitmap for full screen.
		Intent intent = getIntent();
		image = (File) intent.getExtras().get("file");

		// Get the bitmap of selected image.
		if (bm != null)
			bm.recycle();
		bm = SyncGalleryUtils.createOptimizedBitMap(image);

		// Show full screen picture.
		imageView = new TouchImageView(this);
		imageView.setImageBitmap(bm);
		imageView.setMaxZoom(4f);
		setContentView(imageView);

		// hide the battery bar on the window.
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// add listener to shake event
		mShaker = new ShakeListener(this);
		mShaker.setOnShakeListener(this);
	}

	@Override
	protected void onPause() {
		Log.i("KOBE-DEBUG", "onPause invoked");
		// remove the listener if activity runs in the background
		mShaker.setOnShakeListener(null);
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.i("KOBE-DEBUG", "onResume invoked");
		if (bm.isRecycled()) {
			bm = SyncGalleryUtils.createOptimizedBitMap(image);
			imageView.setImageBitmap(bm);
		}
		// add the listener back if it is brought to the front
		mShaker.setOnShakeListener(this);
		super.onResume();
	}

	@Override
	public void onStop() {
		Log.i("KOBE-DEBUG", "onStop invoked");
		if (conn != null)
			conn.disconnect();
		if (bm != null)
			bm.recycle();
		super.onStop();
	}

	/** Add the edit button. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.viewpic_menu, menu);
		return true;
	}

	/** If the user choose to edit the image. */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!SyncGalleryUtils.sdCardMounted(this)) {
			return true;
		}
		
		switch (item.getItemId()) {
		// Call aviary for editing.
		case R.id.edit:

			// Get the file for editing.
			Intent intent = getIntent();
			File image = (File) intent.getExtras().get("file");

			// Create the intent needed to start feather.
			Intent intentMain = new Intent(this, FeatherActivity.class);

			// Set the source image Uri.
			intentMain.setData(Uri.fromFile(image));

			// Pass the required aviary API key.
			intentMain.putExtra("API_KEY", SyncGalleryConstants.Aviary_APP_KEY);

			// Pass the uri of the destination image file to aviary.
			String newPath = "file://"
					+ image.getParentFile().getAbsolutePath() + "/new_"
					+ SyncGalleryUtils.generateDCIMFilename();
			intentMain.putExtra("output", Uri.parse(newPath));

			// Enable fast rendering preview.
			intentMain.putExtra("effect-enable-fast-preview",
					SyncGalleryConstants.Aviary_FAST_RENDERING);

			// Start Aviary.
			startActivityForResult(intentMain,
					SyncGalleryConstants.Aviary_ACTION_REQUEST_FEATHER);
			Log.i("From fullscreen picture", "Entering Aviary");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/** Show full screen picture when the aviary finishes editing. */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (!SyncGalleryUtils.sdCardMounted(this)) {
			return;
		}
		
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case SyncGalleryConstants.Aviary_ACTION_REQUEST_FEATHER:

				// Get the file path.
				File returnedImage = new File(data.getData().getPath());

				// Get the bitmap for showing.
				if (bm != null)
					bm.recycle();
				bm = SyncGalleryUtils.createOptimizedBitMap(returnedImage);

				// Show edited image.
				imageView.setImageBitmap(bm);
				imageView.setMaxZoom(4f);
				setContentView(imageView);
				AlbumActivity.setDirty(true);
				break;
			case SyncGalleryConstants.BUMP_INIT_REQUEST:
				conn = (BumpConnection) data
						.getParcelableExtra(BumpAPI.EXTRA_CONNECTION);
				conn.setListener(this);
				try {
					// Start to send the image to peer
					conn.send(readBytes(Uri.fromFile(image)));
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
		} else {
			switch (requestCode) {
			case SyncGalleryConstants.BUMP_INIT_REQUEST:
				// Retrieve the failure reason and display, for debug purpose
				if (data != null) {
					BumpConnectFailedReason reason = (BumpConnectFailedReason) data
							.getSerializableExtra(BumpAPI.EXTRA_REASON);
					Log.i("KOBE", reason.toString());
				}
				break;
			}
		}
	}

	@Override
	public void bumpDataReceived(byte[] arg0) {
		// This line should never be reached
		Log.i("KOBE",
				"Bump shouldn't receive data in ViewpicActivity; Exception...");
	}

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

	/** This is the callback function when shake event is invoked */
	@Override
	public void onShake() {
		if (!SyncGalleryUtils.sdCardMounted(this)) {
			return;
		}
		
		final Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibe.vibrate(100);

		final Intent bump = new Intent(this, BumpAPI.class);
		bump.putExtra(BumpAPI.EXTRA_USER_NAME,
				SyncGalleryConstants.getBumpUsername(AccountManager.get(this)));
		bump.putExtra(BumpAPI.EXTRA_API_KEY, SyncGalleryConstants.BUMP_APP_KEY);
		startActivityForResult(bump, SyncGalleryConstants.BUMP_INIT_REQUEST);
	}

	/** Read the file and convert it to byte array */
	public byte[] readBytes(Uri uri) throws IOException {
		InputStream inputStream = getContentResolver().openInputStream(uri);
		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

		// this is storage overwritten on each iteration with bytes
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];

		int len = 0;
		while ((len = inputStream.read(buffer)) != -1) {
			byteBuffer.write(buffer, 0, len);
		}

		return byteBuffer.toByteArray();
	}

}
