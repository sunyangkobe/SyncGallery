package com.utoronto.syncgallery.test;

import android.test.ActivityInstrumentationTestCase2;
import com.utoronto.syncgallery.GalleryActivity;

public class GalleryActivityTest extends
		ActivityInstrumentationTestCase2<GalleryActivity> {

	private GalleryActivity mActivity;
	
	public GalleryActivityTest(String name) {
		super("com.utoronto.syncgallery", GalleryActivity.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		mActivity = this.getActivity();
		
	}
	
	public void testPreconditions() {
		
	}
	
	public void testBrowseTo() {
		
	}

}
