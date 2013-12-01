package com.utoronto.syncgallery.IF;

/**
 * This serves as the interface for Gallery and Dropbox operations
 * 
 * @author KOBE
 *
 */


public interface GalleryBasicOperations {

	public void delete();
	public void rename();
	public void copyToSyncGallery();
	public void share();
	public void createFolder();
	public void showProperties();
}
