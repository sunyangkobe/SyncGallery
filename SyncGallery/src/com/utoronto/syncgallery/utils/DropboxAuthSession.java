package com.utoronto.syncgallery.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

public class DropboxAuthSession {

	private static DropboxAPI<AndroidAuthSession> mDBApi;

	private DropboxAuthSession() {
		AppKeyPair appKeys = new AppKeyPair(
				SyncGalleryConstants.DropBox_APP_KEY,
				SyncGalleryConstants.DropBox_APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys,
				SyncGalleryConstants.DropBox_ACCESS_TYPE);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
	}

	private static class DropboxAuthHolder {
		public static final DropboxAuthSession instance = new DropboxAuthSession();
	}

	public static DropboxAuthSession getInstance() {
		return DropboxAuthHolder.instance;
	}

	public final void finishAuthentication(Activity activity) {
		getDBSession().finishAuthentication();
		// storeTokens(activity, getDBSession().getAccessTokenPair());
	}

	public final void storeTokens(Activity activity, AccessTokenPair tokens) {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(activity);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("TOKEN_KEY", tokens.key);
		editor.putString("TOKEN_SECRET", tokens.secret);
		editor.apply();
	}

	public final void startAuthentication(Context context) {
		if (!mDBApi.getSession().isLinked()) {
			getDBSession().startAuthentication(context);
		}
	}

	public final DropboxAPI<AndroidAuthSession> getApi() {
		return mDBApi;
	}

	public final AndroidAuthSession getDBSession() {
		return mDBApi.getSession();
	}

	public final boolean isLinked() {
		return mDBApi.getSession().isLinked();
	}

	public final void unlink() {
		mDBApi.getSession().unlink();
	}
}
