package com.utoronto.syncgallery.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class SyncGalleryUtils {
	public static final File getThumbnailFile(final File file) {
		return new File(file.getParent()
				+ SyncGalleryConstants.Gallery_THUMBNAILS + file.getName());
	}

	/** Convert the file size. */
	public static final String formatFileSize(long bytes) {
		int unit = 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = "KMGTPE".charAt(exp - 1) + "i";
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public static final boolean sdCardMounted(final Context context) {
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Toast.makeText(context, "SD card is not available...",
					Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}
	
	public static final String meta2Name(String filename) {
		String[] tokens = filename.split("-");
		StringBuilder builder = new StringBuilder();
		for (String dir : tokens) {
			if (new File(SyncGalleryConstants.Gallery_SYNC_DIR
					+ File.separator + dir).exists()) {
				builder.append(dir + "/");
			} else {
				builder.append(dir + "-");
			}
		}
		builder.deleteCharAt(builder.length() - 1);
		return builder.toString();
	}
	
	public static final String name2Meta(String filename) {
		String[] tokens = filename.split("/");
		StringBuilder builder = new StringBuilder();
		for (String dir : tokens) {
			if (new File(SyncGalleryConstants.Gallery_SYNC_DIR
					+ File.separator + dir).exists()) {
				builder.append(dir + "-");
			} else {
				builder.append(dir + "/");
			}
		}
		builder.deleteCharAt(builder.length() - 1);
		return builder.toString();
	}
	
	public static final boolean copyfile(File srFile, File dtFile) {
		try {
			InputStream in = new FileInputStream(srFile);

			// For Overwrite the file.
			OutputStream out = new FileOutputStream(dtFile);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			Log.i("SyncGallery+", "File copied");
			return true;
		} catch (FileNotFoundException ex) {
			Log.i("SyncGallery", ex.getMessage()
					+ " in the specified directory.");
			return false;
		} catch (IOException e) {
			Log.i("SyncGallery", e.getMessage());
			return false;
		}
	}

	/** Helper function to get the thumbnail */
	public static final Bitmap getImageThumbnail(File file) {

		/** Set bitmap size. */
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 4;

		File thumbfile = new File(file.getParentFile().getAbsolutePath()
				+ SyncGalleryConstants.Gallery_THUMBNAILS + file.getName());
		if (thumbfile.exists()) {
			/** Retrieve from the thumbnail folder */
			return BitmapFactory.decodeFile(thumbfile.getAbsolutePath());
		} else {
			/** Create the thumbnail based on orignal image */
			Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath(),
					options);
			Bitmap thumbnail = ThumbnailUtils.extractThumbnail(image, 100, 100);
			if (thumbnail != null) {
				/** Save it to thumbnail folder for future use */
				saveBitMap(thumbnail, file);
			}
			return thumbnail;
		}
	}

	/** Save the bitmap to the thumbnail folder */
	public static final void saveBitMap(Bitmap thumbnail, File file) {

		/** Get thumbnail folder directory. */
		File dir = new File(file.getParentFile().getAbsolutePath()
				+ SyncGalleryConstants.Gallery_THUMBNAILS);

		/** thumbnails folder does not exist. */
		if (!dir.exists())
			dir.mkdirs();

		/** Try to compress and save it. */
		try {
			File outfile = new File(dir, file.getName());
			FileOutputStream fOut = new FileOutputStream(outfile);
			thumbnail.compress(Bitmap.CompressFormat.JPEG, 50, fOut);
			fOut.flush();
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static final String generateDCIMFilename() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat time = new SimpleDateFormat("Hmmss");
		return "IMG_" + date.format(cal.getTime()) + "_"
				+ time.format(cal.getTime()) + ".jpg";
	}

	public static final void rotateImage(File image, int rotation) {
		Bitmap bm = createOptimizedBitMap(image);
		Matrix matrix = new Matrix();
		matrix.postRotate(rotation);
		Bitmap out = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
				bm.getHeight(), matrix, true);
		try {
			File outfile = new File(image.getParentFile(), image.getName());
			FileOutputStream fOut = new FileOutputStream(outfile);
			out.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
			fOut.flush();
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static final Bitmap createOptimizedBitMap(File image) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		Log.i("KOBE", image.length() + "");
		options.inSampleSize = (int) (image.length() / (512 * 1024)) + 1;
		Log.i("KOBE-Size", options.inSampleSize + "");
		return BitmapFactory.decodeFile(image.getAbsolutePath(), options);
	}

}
