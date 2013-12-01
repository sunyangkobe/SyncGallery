package com.utoronto.syncgallery.utils;

import java.io.File;

public class config {
	public static String CR_SDCARD = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
	
	public static final String CR_URL_DBFILE = "/listview";
	public static final String CR_PATH = CR_SDCARD+CR_URL_DBFILE;
	public static final String CR_DBNAME = "database.db";
	public static final String CR_URL_DBTABLE = CR_PATH+"/"+CR_DBNAME;//database file
	public static File path = new File(CR_PATH); 
	public static File f = new File(CR_URL_DBTABLE);
}
