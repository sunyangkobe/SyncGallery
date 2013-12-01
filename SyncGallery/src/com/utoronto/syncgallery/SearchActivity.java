package com.utoronto.syncgallery;

import java.io.File;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * SearchActivity
 * 
 * @author KOBE, Daniel
 * 
 */

public class SearchActivity extends AlbumActivity {

	public static File TARGETDIR;
	private String query;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/** Get the intent, verify the action and get the query. */
		if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
			/** Perform search. */
			query = getIntent().getStringExtra(SearchManager.QUERY);
			browseTo(TARGETDIR, query);
		}
	}

	@Override
	public void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		/** Get the intent, verify the action and get the query. */
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			/** Perform search. */
			query = intent.getStringExtra(SearchManager.QUERY);
			browseTo(TARGETDIR, query);
		}
	}

	@Override
	public void onBackPressed() {
		Log.i("KOBE", "current directory:" + getmCurrentDir().getAbsolutePath());
		if (getmCurrentDir().getParentFile() == null
				|| getmCurrentDir().equals(TARGETDIR)) {
			this.finish();
		} else if (getmCurrentDir().getParentFile().equals(TARGETDIR)) {
			browseTo(TARGETDIR, query);
		} else {
			browseTo(getmCurrentDir().getParentFile());
		}
	}

}
