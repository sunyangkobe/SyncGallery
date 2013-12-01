package com.utoronto.syncgallery.adapter;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.utoronto.syncgallery.AlbumActivity;
import com.utoronto.syncgallery.R;
import com.utoronto.syncgallery.utils.SyncGalleryConstants;
import com.utoronto.syncgallery.widget.IconView;

/**
 * IconAdapter, for GalleryFileExplorer
 * 
 * @author KOBE
 * 
 */

public class IconAdapter extends BaseAdapter {
	private AlbumActivity gallery;

	public IconAdapter(Context c) {
		gallery = (AlbumActivity) c;
	}

	public int getCount() {
		return gallery.getmFiles().size();
	}

	public Object getItem(int arg0) {
		return gallery.getmFiles().get(arg0);
	}

	public long getItemId(int arg0) {
		return arg0;
	}

	public View getView(int index, View convertView, ViewGroup parent) {
		IconView icon;
		File currentFile = gallery.getmFiles().get(index);

		Bitmap iconBm;
		String filename;

		if (index == 0
				&& (currentFile.getParentFile() == null || currentFile
						.getParentFile().getAbsolutePath()
						.compareTo(gallery.getmCurrentDir().getAbsolutePath()) != 0)) {
			iconBm = BitmapFactory.decodeResource(gallery.getResources(),
					R.drawable.updirectory);
			filename = new String("..");
		} else {
			iconBm = getIconBitmap(currentFile);
			filename = currentFile.getName();
		}

		if (convertView == null) {
			icon = new IconView((Context) gallery, iconBm, filename);
		} else {
			icon = (IconView) convertView;
			icon.setIconBitmap(iconBm);
			icon.setFileName(filename);
		}
		return icon;
	}

	private Bitmap getIconBitmap(File file) {

		if (file.isDirectory())
			return BitmapFactory.decodeResource(gallery.getResources(),
					R.drawable.directory);

		for (String ext : gallery.getmImageExt()) {
			if (file.getName().toLowerCase().endsWith(ext)) {
				return BitmapFactory.decodeFile(file.getParentFile()
						.getAbsolutePath()
						+ SyncGalleryConstants.Gallery_THUMBNAILS
						+ file.getName());
			}
		}

		return BitmapFactory.decodeResource(gallery.getResources(),
				R.drawable.unknown);
	}

}
