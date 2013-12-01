package com.aviary.android.feather.async_tasks;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.aviary.android.feather.library.log.LoggerFactory;
import com.aviary.android.feather.library.log.LoggerFactory.Logger;
import com.aviary.android.feather.library.log.LoggerFactory.LoggerType;
import com.aviary.android.feather.library.utils.BitmapUtils;
import com.aviary.android.feather.library.utils.BitmapUtils.FLIP_MODE;
import com.aviary.android.feather.library.utils.BitmapUtils.ROTATION;
import com.aviary.android.feather.library.utils.ImageLoader;

// TODO: Auto-generated Javadoc
/**
 * Load an internal asset asynchronous.
 *
 * @author alessandro
 */
public class AssetsAsyncDownloadManager {

	// handler messages
	/** The Constant THUMBNAIL_LOADED. */
	public static final int THUMBNAIL_LOADED = 1;

	/** Cache variables. */
	private static final int HARD_CACHE_CAPACITY = 10;
	
	/** The Constant DELAY_BEFORE_PURGE. */
	private static final int DELAY_BEFORE_PURGE = 60 * 1000;
	
	/** The purge handler. */
	private final Handler purgeHandler = new Handler();

	/** The current context. */
	@SuppressWarnings("unused")
	private Context mContext;
	
	/** The thumbnail size. */
	private int mThumbSize = -1;
	
	private Handler mHandler;
	
	/** is stopped. */
	private Boolean mStopped = false;
	
	/** The number of threads. */
	private final int nThreads;
	
	/** The threads pool. */
	private final PoolWorker[] threads;
	
	/** The current runnable queue. */
	private final LinkedList<MyRunnable> mQueue;

	/**
	 * Instantiates a new assets async download manager.
	 *
	 * @param context the context
	 * @param handler the handler
	 */
	public AssetsAsyncDownloadManager( Context context, Handler handler ) {
		mContext = context;
		mHandler = handler;
		nThreads = 1;
		mQueue = new LinkedList<MyRunnable>();
		threads = new PoolWorker[nThreads];

		for ( int i = 0; i < nThreads; i++ ) {
			threads[i] = new PoolWorker();
			threads[i].start();
		}
	}

	/**
	 * Gets the thumb size.
	 *
	 * @return the thumb size
	 */
	public int getThumbSize() {
		return mThumbSize;
	}

	/**
	 * set the default thumbnail size when resizing a bitmap.
	 *
	 * @param size the new thumb size
	 */
	public void setThumbSize( int size ) {
		mThumbSize = size;
	}

	/**
	 * Shut down now.
	 */
	public void shutDownNow() {
		mStopped = true;

		synchronized ( mQueue ) {
			mQueue.clear();
		}

		mContext = null;
	}

	/**
	 * The Class MyRunnable.
	 */
	private abstract class MyRunnable implements Runnable {

		/** The view. */
		public WeakReference<ImageView> view;

		/**
		 * Instantiates a new my runnable.
		 *
		 * @param image the image
		 */
		public MyRunnable( ImageView image ) {
			this.view = new WeakReference<ImageView>( image );
		}
	};

	/**
	 * Load asset.
	 *
	 * @param resource the resource
	 * @param srcFile the src file
	 * @param background the background
	 * @param view the view
	 */
	public void loadAsset( final Resources resource, final String srcFile, final Drawable background, final ImageView view ) {

		if ( mStopped || mThumbSize < 1 ) return;

		resetPurgeTimer();

		runTask( new MyRunnable( view ) {

			@Override
			public void run() {
				if ( mStopped ) return;

				Message message = mHandler.obtainMessage();

				Bitmap bitmap = getBitmapFromCache( srcFile );
				if ( bitmap != null ) {
					message.what = THUMBNAIL_LOADED;
					message.obj = new Thumb( bitmap, view.get() );
				} else {
					bitmap = downloadBitmap( resource, srcFile, background, view.get() );
					if ( bitmap != null ) addBitmapToCache( srcFile, bitmap );

					ImageView imageView = view.get();

					if ( imageView != null ) {
						MyRunnable bitmapTask = getBitmapTask( imageView );
						if ( this == bitmapTask ) {
							imageView.setTag( null );
							message.what = THUMBNAIL_LOADED;
							message.obj = new Thumb( bitmap, imageView );
						} else {
							Logger logger = LoggerFactory.getLogger( "AssetsDownloadManager", LoggerType.ConsoleLoggerType );
							logger.error( "image tag is different than current task!" );
						}
					}
				}

				if ( message.what == THUMBNAIL_LOADED ) mHandler.sendMessage( message );
			}
		} );
	}

	/**
	 * Load asset icon.
	 *
	 * @param info the info
	 * @param pm the pm
	 * @param view the view
	 */
	public void loadAssetIcon( final ApplicationInfo info, final PackageManager pm, final ImageView view ) {

		if ( mStopped || mThumbSize < 1 ) return;

		resetPurgeTimer();

		runTask( new MyRunnable( view ) {

			@Override
			public void run() {
				if ( mStopped ) return;

				Message message = mHandler.obtainMessage();

				Bitmap bitmap = getBitmapFromCache( info.packageName );
				if ( bitmap != null ) {
					message.what = THUMBNAIL_LOADED;
					message.obj = new Thumb( bitmap, view.get() );
				} else {
					bitmap = downloadIcon( info, pm, view.get() );
					if ( bitmap != null ) addBitmapToCache( info.packageName, bitmap );

					ImageView imageView = view.get();

					if ( imageView != null ) {
						MyRunnable bitmapTask = getBitmapTask( imageView );
						if ( this == bitmapTask ) {
							imageView.setTag( null );
							message.what = THUMBNAIL_LOADED;
							message.obj = new Thumb( bitmap, imageView );
						} else {
							Logger logger = LoggerFactory.getLogger( "AssetsDownloadManager", LoggerType.ConsoleLoggerType );
							logger.error( "image tag is different than current task!" );
						}
					}
				}

				if ( message.what == THUMBNAIL_LOADED ) mHandler.sendMessage( message );
			}
		} );
	}

	/**
	 * Run task.
	 *
	 * @param task the task
	 */
	private void runTask( MyRunnable task ) {
		synchronized ( mQueue ) {

			Iterator<MyRunnable> iterator = mQueue.iterator();
			while ( iterator.hasNext() ) {
				MyRunnable current = iterator.next();
				ImageView image = current.view.get();

				if ( image == null ) {
					Log.d( LoggerFactory.LOG_TAG, "remove current: " + mQueue.size() );
					iterator.remove();
					Log.d( LoggerFactory.LOG_TAG, "removed: " + mQueue.size() );
					//mQueue.remove( current );
				} else {
					if ( image.equals( task.view.get() ) ) {
						current.view.get().setTag( null );
						Log.d( LoggerFactory.LOG_TAG, "remove current: " + mQueue.size() );
						iterator.remove();
						Log.d( LoggerFactory.LOG_TAG, "removed: " + mQueue.size() );
						//mQueue.remove( current );
						break;
					}
				}
			}

			task.view.get().setTag( new CustomTag( task ) );

			mQueue.add( task );
			mQueue.notify();
		}
	}

	/**
	 * Download bitmap.
	 *
	 * @param resource the resource
	 * @param url the url
	 * @param background the background
	 * @param view the view
	 * @return the bitmap
	 */
	Bitmap downloadBitmap( Resources resource, String url, Drawable background, View view ) {

		if ( view == null ) return null;

		try {
			Bitmap bitmap = ImageLoader.loadFromAsset( resource, url, mThumbSize, mThumbSize );
			Bitmap result;

			if ( background != null ) {
				result = BitmapUtils.createThumbnail( bitmap, mThumbSize, mThumbSize, ROTATION.ROTATE_NULL, FLIP_MODE.None, null,
						background, 20, 10 );
			} else {
				result = BitmapUtils.createThumbnail( bitmap, mThumbSize, mThumbSize, FLIP_MODE.None, 0 );
			}

			bitmap.recycle();
			return result;
		} catch ( Exception e ) {
			return null;
		}
	}

	/**
	 * Download icon.
	 *
	 * @param info the info
	 * @param pm the pm
	 * @param view the view
	 * @return the bitmap
	 */
	Bitmap downloadIcon( ApplicationInfo info, PackageManager pm, View view ) {
		if ( view == null ) return null;

		Drawable d = info.loadIcon( pm );
		if ( d instanceof BitmapDrawable ) {
			Bitmap bitmap = ( (BitmapDrawable) d ).getBitmap();
			// Bitmap result = BitmapUtils.createThumbnail( bitmap, mThumbSize, mThumbSize, ROTATION.ROTATE_NULL, FLIP_MODE.None, null,
			// Color.WHITE, Color.WHITE, 5, 5 );
			// bitmap.recycle();
			return bitmap;
		}

		return null;

	}

	/**
	 * The Class PoolWorker.
	 */
	private class PoolWorker extends Thread {

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			Runnable r;

			while ( mStopped != true ) {
				synchronized ( mQueue ) {
					while ( mQueue.isEmpty() ) {
						if ( mStopped ) break;
						try {
							mQueue.wait();
						} catch ( InterruptedException ignored ) {}
					}
					r = (Runnable) mQueue.removeFirst();
				}

				try {
					r.run();
				} catch ( RuntimeException e ) {
					Logger logger = LoggerFactory.getLogger( "AssetsDownloadManager", LoggerType.ConsoleLoggerType );
					logger.error( e.getMessage() );
				}
			}
		}
	}

	/**
	 * The Class CustomTag.
	 */
	static class CustomTag {

		/** The task reference. */
		private final WeakReference<MyRunnable> taskReference;

		/**
		 * Instantiates a new custom tag.
		 *
		 * @param task the task
		 */
		public CustomTag( MyRunnable task ) {
			super();
			taskReference = new WeakReference<MyRunnable>( task );
		}

		/**
		 * Gets the downloader task.
		 *
		 * @return the downloader task
		 */
		public MyRunnable getDownloaderTask() {
			return taskReference.get();
		}
	}

	/**
	 * Gets the bitmap task.
	 *
	 * @param imageView the image view
	 * @return the bitmap task
	 */
	private static MyRunnable getBitmapTask( ImageView imageView ) {
		if ( imageView != null ) {
			Object tag = imageView.getTag();
			if ( tag instanceof CustomTag ) {
				CustomTag runnableTag = (CustomTag) tag;
				return runnableTag.getDownloaderTask();
			}
		}
		return null;
	}

	/** Hard cache, with a fixed maximum capacity and a life duration. */
	private final HashMap<String, Bitmap> sHardBitmapCache = new LinkedHashMap<String, Bitmap>( HARD_CACHE_CAPACITY / 2, 0.75f, true ) {

		private static final long serialVersionUID = 7320831300767054723L;

		@Override
		protected boolean removeEldestEntry( LinkedHashMap.Entry<String, Bitmap> eldest ) {
			if ( size() > HARD_CACHE_CAPACITY ) {
				// Entries push-out of hard reference cache are transferred to soft
				// reference cache
				sSoftBitmapCache.put( eldest.getKey(), new SoftReference<Bitmap>( eldest.getValue() ) );
				return true;
			} else
				return false;
		}
	};

	/** Soft cache for bitmaps kicked out of hard cache. */
	private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>(
			HARD_CACHE_CAPACITY / 2 );

	/** The m purger. */
	private final Runnable mPurger = new Runnable() {

		@Override
		public void run() {
			clearCache();
		}
	};

	/**
	 * Clears the image cache used internally to improve performance. Note that for memory efficiency reasons, the cache will
	 * automatically be cleared after a certain inactivity delay.
	 */
	public void clearCache() {
		sHardBitmapCache.clear();
		sSoftBitmapCache.clear();
	}

	/**
	 * Allow a new delay before the automatic cache clear is done.
	 */
	private void resetPurgeTimer() {
		purgeHandler.removeCallbacks( mPurger );
		purgeHandler.postDelayed( mPurger, DELAY_BEFORE_PURGE );
	}

	/**
	 * Try to get the image from cache first.
	 *
	 * @param url the url
	 * @return the bitmap from cache
	 */
	private Bitmap getBitmapFromCache( String url ) {
		// First try the hard reference cache
		synchronized ( sHardBitmapCache ) {
			final Bitmap bitmap = sHardBitmapCache.get( url );
			if ( bitmap != null ) {
				// Bitmap found in hard cache
				// Move element to first position, so that it is removed last
				sHardBitmapCache.remove( url );
				sHardBitmapCache.put( url, bitmap );
				return bitmap;
			}
		}

		// Then try the soft reference cache
		SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get( url );
		if ( bitmapReference != null ) {
			final Bitmap bitmap = bitmapReference.get();
			if ( bitmap != null ) {
				// Bitmap found in soft cache
				return bitmap;
			} else {
				// Soft reference has been Garbage Collected
				sSoftBitmapCache.remove( url );
			}
		}

		return null;
	}

	/**
	 * Adds this bitmap to the cache.
	 *
	 * @param url the url
	 * @param bitmap . The newly downloaded bitmap.
	 */
	private void addBitmapToCache( String url, Bitmap bitmap ) {
		if ( bitmap != null ) {
			synchronized ( sHardBitmapCache ) {
				sHardBitmapCache.put( url, bitmap );
			}
		}
	}

	/**
	 * The Class Thumb.
	 */
	public static class Thumb {

		/** The bitmap. */
		public Bitmap bitmap;
		
		/** The image. */
		public ImageView image;

		/**
		 * Instantiates a new thumb.
		 *
		 * @param bmp the bmp
		 * @param img the img
		 */
		public Thumb( Bitmap bmp, ImageView img ) {
			image = img;
			bitmap = bmp;
		}
	}
}
