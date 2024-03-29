package com.aviary.android.feather;

import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import java.util.HashMap;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;
import com.aviary.android.feather.effects.AbstractEffectPanel;
import com.aviary.android.feather.effects.AbstractEffectPanel.ContentPanel;
import com.aviary.android.feather.effects.AbstractEffectPanel.OnApplyResultListener;
import com.aviary.android.feather.effects.AbstractEffectPanel.OnContentReadyListener;
import com.aviary.android.feather.effects.AbstractEffectPanel.OnErrorListener;
import com.aviary.android.feather.effects.AbstractEffectPanel.OnPreviewListener;
import com.aviary.android.feather.effects.AbstractEffectPanel.OnProgressListener;
import com.aviary.android.feather.effects.AbstractEffectPanel.OptionPanel;
import com.aviary.android.feather.effects.EffectLoaderService;
import com.aviary.android.feather.library.content.EffectEntry;
import com.aviary.android.feather.library.content.FeatherIntent;
import com.aviary.android.feather.library.filters.FilterService;
import com.aviary.android.feather.library.graphics.drawable.IBitmapDrawable;
import com.aviary.android.feather.library.log.LoggerFactory;
import com.aviary.android.feather.library.log.LoggerFactory.Logger;
import com.aviary.android.feather.library.log.LoggerFactory.LoggerType;
import com.aviary.android.feather.library.moa.MoaActionList;
import com.aviary.android.feather.library.plugins.ExternalPacksTask;
import com.aviary.android.feather.library.plugins.PluginManager;
import com.aviary.android.feather.library.services.BackgroundService;
import com.aviary.android.feather.library.services.ConfigService;
import com.aviary.android.feather.library.services.EffectContext;
import com.aviary.android.feather.library.services.EffectContextService;
import com.aviary.android.feather.library.services.HiResService;
import com.aviary.android.feather.library.services.PluginService;
import com.aviary.android.feather.library.services.PreferenceService;
import com.aviary.android.feather.library.services.ServiceLoader;
import com.aviary.android.feather.library.tracking.Tracker;
import com.aviary.android.feather.receivers.FeatherSystemReceiver;
import com.aviary.android.feather.widget.BottombarViewFlipper;
import com.aviary.android.feather.widget.BottombarViewFlipper.OnPanelCloseListener;
import com.aviary.android.feather.widget.BottombarViewFlipper.OnPanelOpenListener;
import com.aviary.android.feather.widget.ToolbarView;

// TODO: Auto-generated Javadoc
/**
 * FilterManager is the core of feather. It manages all the tool panels, notifies about new plugins installed, etc
 * 
 * @author alessandro
 * 
 */
public final class FilterManager implements OnPreviewListener, OnApplyResultListener, EffectContext, OnErrorListener,
		OnContentReadyListener, OnProgressListener {

	/**
	 * The Interface FeatherContext.<br />
	 * The activity caller must implement this interface
	 */
	public interface FeatherContext {

		/**
		 * Gets the Activity main image view.
		 * 
		 * @return the main image
		 */
		ImageViewTouchBase getMainImage();

		/**
		 * Gets the Activity bottom bar view.
		 * 
		 * @return the bottom bar
		 */
		BottombarViewFlipper getBottomBar();

		/**
		 * Gets the Activity options panel container view.
		 * 
		 * @return the options panel container
		 */
		ViewGroup getOptionsPanelContainer();

		/**
		 * Gets the Activity drawing image container view.
		 * 
		 * @return the drawing image container
		 */
		ViewGroup getDrawingImageContainer();

		/**
		 * Show tool progress.
		 */
		void showToolProgress();

		/**
		 * Hide tool progress.
		 */
		void hideToolProgress();

		/**
		 * Show a modal progress
		 */
		void showModalProgress();

		/**
		 * Hide the modal progress
		 */
		void hideModalProgress();

		/**
		 * Gets the toolbar.
		 * 
		 * @return the toolbar
		 */
		ToolbarView getToolbar();

		Uri getOriginalUri();

		String getOriginalFilePath();
	}
	
	public interface OnHiResListener {
		void OnLoad( Uri uri );
		void OnApplyActions( MoaActionList actionlist );
	}

	/**
	 * The listener interface for receiving onTool events. The class that is interested in processing a onTool event implements this
	 * interface, and the object created with that class is registered with a component using the component's
	 * <code>addOnToolListener<code> method. When
	 * the onTool event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see OnToolEvent
	 */
	public static interface OnToolListener {

		/**
		 * On tool completed.
		 */
		void onToolCompleted();
	}

	/**
	 * All the possible states the filtermanager can use during the feather lifecycle.
	 * 
	 * @author alessandro
	 */
	static enum STATE {
		CLOSED_CANCEL, CLOSED_CONFIRMED, CLOSING, DISABLED, OPENED, OPENING,
	}

	/** The Constant STATE_OPENING. */
	public static final int STATE_OPENING = 0;

	/** The Constant STATE_OPENED. */
	public static final int STATE_OPENED = 1;

	/** The Constant STATE_CLOSING. */
	public static final int STATE_CLOSING = 2;

	/** The Constant STATE_CLOSED. */
	public static final int STATE_CLOSED = 3;

	/** The Constant STATE_DISABLED. */
	public static final int STATE_DISABLED = 4;
	
	/** The current bitmap. */
	private Bitmap mBitmap;

	/** The base context. This is the main activity */
	private FeatherContext mContext;

	/** The current active effect. */
	private AbstractEffectPanel mCurrentEffect;

	/** The current active effect entry. */
	private EffectEntry mCurrentEntry;

	/** The current panel state. */
	private STATE mCurrentState;

	/** The main layout inflater. */
	private LayoutInflater mLayoutInflater;

	/** The main tool listener. */
	private OnToolListener mToolListener;

	private final Handler mHandler;
	private final ServiceLoader<EffectContextService> mServiceLoader;
	private EffectLoaderService mEffectLoader;
	private Logger logger;

	/** The changed state. If the original image has been modified. */
	private boolean mChanged;
	
	private Configuration mConfiguration;

	private String mApiKey;
	private String mSessionId;

	private boolean mHiResEnabled = false;
	private OnHiResListener mHiResListener;
	

	/**
	 * Instantiates a new filter manager.
	 * 
	 * @param context
	 *           the context
	 * @param handler
	 *           the handler
	 */
	public FilterManager( final FeatherContext context, final Handler handler, final String apiKey ) {
		final Activity activity = (Activity) context;
		mContext = context;
		mHandler = handler;
		mApiKey = apiKey;
		mLayoutInflater = (LayoutInflater) activity.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		logger = LoggerFactory.getLogger( "FilterManager", LoggerType.ConsoleLoggerType );

		// mSessionId = StringUtils.getSha256( System.currentTimeMillis() + mApiKey );
		mServiceLoader = new ServiceLoader<EffectContextService>();

		// background tasks queue manager for very simple tasks
		mServiceLoader.register( BackgroundService.class );
		mServiceLoader.register( ConfigService.class );
		mServiceLoader.register( FilterService.class );
		mServiceLoader.register( new PluginService( this ) );
		mServiceLoader.register( EffectLoaderService.class );
		mServiceLoader.register( PreferenceService.class );
		mServiceLoader.register( HiResService.class );

		// Register the plugin manager task in the background task queue
		BackgroundService background = getService( BackgroundService.class );
		if ( background != null ) {
			
			final boolean externalItemsEnabled = Constants.getValueFromIntent( Constants.EXTRA_EFFECTS_ENABLE_EXTERNAL_PACKS, true );
			
			PluginManager pluginManager = new PluginManager( activity, "com.aviary.android.feather" );
			pluginManager.setExternalFiltersEnabled( externalItemsEnabled );
			background.setHandler( mBackgroundHandler );
			background.start();
			background.register( pluginManager, true );
			
			if( externalItemsEnabled ){
				ExternalPacksTask a = new ExternalPacksTask();
				background.register(a, true);
			}
			
		} else {
			logger.error( "failed to retrieve BackgroundService" );
		}
		
		mConfiguration = new Configuration( getBaseActivity().getResources().getConfiguration() );

		setCurrentState( STATE.DISABLED );
		mChanged = false;
	}

	/**
	 * Register a default handler to receive
	 * hi-res messages
	 * @param handler
	 */
	public void setOnHiResListener( OnHiResListener listener ){
		mHiResListener = listener;
	}

	private void initHiResService() {
		
		logger.info( "initHiResService" );

		if ( Constants.containsValue( Constants.EXTRA_OUTPUT_HIRES_SESSION_ID ) ) {

			mSessionId = Constants.getValueFromIntent( Constants.EXTRA_OUTPUT_HIRES_SESSION_ID, "" );
			logger.info( "session-id: " + mSessionId + ", length: " + mSessionId.length() );

			if ( mSessionId != null && mSessionId.length() == 64 ) {
				mHiResEnabled = true;

				HiResService service = getService( HiResService.class );
				if ( !service.isRunning() ) {
					service.start();
				}
				service.load( mSessionId, mApiKey, mContext.getOriginalUri() );
			} else {
				logger.error( "session id is invalid" );
			}
		} else {
			logger.warning( "missing session id" );
			
			if( null != mHiResListener ){
				mHiResListener.OnLoad( mContext.getOriginalUri() );
			}
		}
		
	}

	/**
	 * This is the entry point of every feather tools. The main activity catches the tool onClick listener and notify the
	 * filtermanager.
	 * 
	 * @param tag
	 *           the tag
	 */
	public void activateEffect( final EffectEntry tag ) {
		if ( !getEnabled() || !isClosed() || mBitmap == null ) return;

		if ( mCurrentEffect != null ) throw new IllegalStateException( "There is already an active effect. Cannot activate new" );
		if ( mEffectLoader == null ) mEffectLoader = (EffectLoaderService) getService( EffectLoaderService.class );

		final AbstractEffectPanel effect = mEffectLoader.load( tag );

		if ( effect != null ) {
			mCurrentEffect = effect;
			mCurrentEntry = tag;

			setCurrentState( STATE.OPENING );
			prepareEffectPanel( effect, tag );

			Tracker.recordTag( mCurrentEntry.name.name().toLowerCase() + ": opened" );
			mContext.getBottomBar().setOnPanelOpenListener( new OnPanelOpenListener() {

				@Override
				public void onOpened() {
					setCurrentState( STATE.OPENED );
					mContext.getBottomBar().setOnPanelOpenListener( null );
				}

				@Override
				public void onOpening() {
					mCurrentEffect.onOpening();
				}
			} );

			mContext.getBottomBar().open();
		}
	}

	/**
	 * Dispose.
	 */
	public void dispose() {
		// TODO: if a panel is opened the deactivate an destroy it

		if ( mCurrentEffect != null ) {
			logger.log( "Deactivate and destroy current panel" );
			mCurrentEffect.onDeactivate();
			mCurrentEffect.onDestroy();
			mCurrentEffect = null;
		}
		mServiceLoader.dispose();
		mContext = null;
		mLayoutInflater = null;
		mToolListener = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.library.services.EffectContext#getApplicationMaxMemory()
	 */
	@Override
	public int getApplicationMaxMemory() {
		return Constants.getApplicationMaxMemory();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.library.services.EffectContext#getBaseContext()
	 */
	@Override
	public Context getBaseContext() {
		return (Activity) mContext;
	}
	
	@Override
	public Activity getBaseActivity(){
		return (Activity) mContext;
	}

	/**
	 * Return the current bitmap.
	 * 
	 * @return the bitmap
	 */
	public Bitmap getBitmap() {
		return mBitmap;
	}

	/**
	 * Compare the size of 2 bitmaps to see if they're different
	 * 
	 * @param bmp1
	 *           the bmp1
	 * @param bmp2
	 *           the bmp2
	 * @return the bitmap changed
	 */
	private boolean getBitmapChanged( final Bitmap bmp1, final Bitmap bmp2 ) {
		if ( ( bmp1.getWidth() != bmp2.getWidth() ) || ( bmp2.getHeight() != bmp2.getHeight() ) ) return true;
		return false;
	}

	/**
	 * Return true if the main image has been modified by any of the feather tools.
	 * 
	 * @return the bitmap is changed
	 */
	public boolean getBitmapIsChanged() {
		return mChanged;
	}

	/**
	 * Return the active tool, null if there is not active tool.
	 * 
	 * @return the current effect
	 */
	@Override
	public EffectEntry getCurrentEffect() {
		return mCurrentEntry;
	}

	/**
	 * Return the current panel associated with the active tool. Null if there's no active tool
	 * 
	 * @return the current panel
	 */
	public AbstractEffectPanel getCurrentPanel() {
		return mCurrentEffect;
	}

	/**
	 * Return the current image transformation matrix. this is useful for those tools which implement ContentPanel and want to
	 * display the preview bitmap with the same zoom level of the main image
	 * 
	 * @return the current image view matrix
	 * @see ContentPanel
	 */
	@Override
	public Matrix getCurrentImageViewMatrix() {
		return mContext.getMainImage().getDisplayMatrix();
	}

	/**
	 * Return true if enabled.
	 * 
	 * @return the enabled
	 */
	public boolean getEnabled() {
		return mCurrentState != STATE.DISABLED;
	}

	/**
	 * Return the service, if previously registered using ServiceLoader.
	 * 
	 * @param <T>
	 *           the generic type
	 * @param cls
	 *           the cls
	 * @return the service
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getService( Class<T> cls ) {
		try {
			return (T) mServiceLoader.getService( (Class<EffectContextService>) cls, this );
		} catch ( IllegalAccessException e ) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Return true if there's no active tool.
	 * 
	 * @return true, if is closed
	 */
	public boolean isClosed() {
		return ( mCurrentState == STATE.CLOSED_CANCEL ) || ( mCurrentState == STATE.CLOSED_CONFIRMED );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.library.services.EffectContext#isConnectedOrConnecting()
	 */
	@Override
	public boolean isConnectedOrConnecting() {
		try {
			final ConnectivityManager conn = (ConnectivityManager) getBaseContext().getSystemService( Context.CONNECTIVITY_SERVICE );
			if ( ( conn.getActiveNetworkInfo() == null ) || !conn.getActiveNetworkInfo().isConnectedOrConnecting() ) return false;
			return true;
		} catch ( final SecurityException e ) {
			// the android.permission.ACCESS_NETWORK_STATE is not set, so we're assuming
			// an internet connection is available
			logger.error( "android.permission.ACCESS_NETWORK_STATE is not defined. Assuming network is fine" );
			return true;
		}
	}

	/**
	 * return true if there's one active tool.
	 * 
	 * @return true, if is opened
	 */
	public boolean isOpened() {
		return mCurrentState == STATE.OPENED;
	}

	/**
	 * On activate.
	 * 
	 * @param bitmap
	 *           the bitmap
	 */
	public void onActivate( final Bitmap bitmap ) {
		if ( mCurrentState != STATE.DISABLED ) throw new IllegalStateException( "Cannot activate. Already active!" );

		if ( ( mBitmap != null ) && !mBitmap.isRecycled() ) {
			mBitmap = null;
		}

		mBitmap = bitmap;
		mChanged = false;
		setCurrentState( STATE.CLOSED_CONFIRMED );
		initHiResService();
	}

	/**
	 * Current activity is asking to apply the current tool.
	 */
	public void onApply() {
		logger.info( "FilterManager::onapply" );
		if ( !getEnabled() || !isOpened() ) return;

		if ( mCurrentEffect == null ) throw new IllegalStateException( "there is no current effect active in the context" );

		if ( !mCurrentEffect.isEnabled() ) return;

		if ( mCurrentEffect.getIsChanged() ) {
			mCurrentEffect.onSave();
			mChanged = true;
		} else {
			onCancel();
		}
	}

	/**
	 * Parent activity just received a onBackPressed event. If there's one active tool, it will be asked to manage the onBackPressed
	 * event. If the active tool onBackPressed method return a false then try to close it.
	 * 
	 * @return true, if successful
	 */
	public boolean onBackPressed() {
		if ( isClosed() ) return false;
		if ( mCurrentState != STATE.DISABLED ) {
			if ( isOpened() ) {
				if ( !mCurrentEffect.onBackPressed() ) onCancel();
			}
			return true;
		}
		return false;
	}

	/**
	 * Main activity asked to cancel the current operation.
	 */
	public void onCancel() {
		if ( !getEnabled() || !isOpened() ) return;
		if ( mCurrentEffect == null ) throw new IllegalStateException( "there is no current effect active in the context" );
		if ( !mCurrentEffect.onCancel() ) {
			cancel();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.library.services.EffectContext#cancel()
	 */
	@Override
	public void cancel() {

		logger.info( "FilterManager::cancel" );

		if ( !getEnabled() || !isOpened() ) return;
		if ( mCurrentEffect == null ) throw new IllegalStateException( "there is no current effect active in the context" );

		Tracker.recordTag( mCurrentEntry.name.name().toLowerCase() + ": cancelled" );

		// send the cancel event to the effect
		mCurrentEffect.onCancelled();

		// check changed image
		if ( mCurrentEffect.getIsChanged() ) {
			// panel is changed, restore the original bitmap

			if ( mCurrentEffect instanceof ContentPanel ) {
				ContentPanel panel = (ContentPanel) mCurrentEffect;
				setNextBitmap( mBitmap, true, panel.getContentDisplayMatrix() );
			} else {
				setNextBitmap( mBitmap, false );
			}

		} else {
			// panel is not changed
			if ( mCurrentEffect instanceof ContentPanel ) {
				ContentPanel panel = (ContentPanel) mCurrentEffect;
				setNextBitmap( mBitmap, true, panel.getContentDisplayMatrix() );
			} else {
				setNextBitmap( mBitmap, false );
			}
		}
		onClose( false );
	}

	/**
	 * On close.
	 * 
	 * @param isConfirmed
	 *           the is confirmed
	 */
	private void onClose( final boolean isConfirmed ) {

		logger.info( "onClose" );

		setCurrentState( STATE.CLOSING );

		mContext.getBottomBar().setOnPanelCloseListener( new OnPanelCloseListener() {

			@Override
			public void onClosed() {
				setCurrentState( isConfirmed ? STATE.CLOSED_CONFIRMED : STATE.CLOSED_CANCEL );
				mContext.getBottomBar().setOnPanelCloseListener( null );
			}

			@Override
			public void onClosing() {
				mCurrentEffect.onClosing();
			}
		} );
		mContext.getBottomBar().close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.effects.AbstractEffectPanel.OnApplyResultListener#onComplete(android.graphics.Bitmap,
	 * java.util.HashMap)
	 */
	@Override
	public void onComplete( final Bitmap result, MoaActionList actions, HashMap<String, String> trackingAttributes ) {
		logger.info( "onComplete: " + android.os.Debug.getNativeHeapAllocatedSize() );
		Tracker.recordTag( mCurrentEntry.name.name().toLowerCase() + ": applied", trackingAttributes );

		if ( result != null ) {
			if ( mCurrentEffect instanceof ContentPanel ) {
				ContentPanel panel = (ContentPanel) mCurrentEffect;
				final boolean changed = getBitmapChanged( mBitmap, result );
				setNextBitmap( result, true, changed ? null : panel.getContentDisplayMatrix() );
			} else {
				setNextBitmap( result, false );
			}

		} else {
			logger.error( "Error: returned bitmap is null!" );
			setNextBitmap( mBitmap, true );
		}

		onClose( true );

		if ( mHiResEnabled ) {
			// send the actions...
			if ( null == actions ) logger.error( "WTF actionlist is null!!!!" );

			HiResService service = getService( HiResService.class );
			if ( service.isRunning() ) {
				service.execute( mSessionId, mApiKey, actions );
			}
		} else {
			if( null != mHiResListener ){
				mHiResListener.OnApplyActions( actions );
			}
		}

	}

	/**
	 * Sets the next bitmap.
	 * 
	 * @param bitmap
	 *           the new next bitmap
	 */
	void setNextBitmap( Bitmap bitmap ) {
		setNextBitmap( bitmap, true );
	}

	/**
	 * Sets the next bitmap.
	 * 
	 * @param bitmap
	 *           the bitmap
	 * @param update
	 *           the update
	 */
	void setNextBitmap( Bitmap bitmap, boolean update ) {
		setNextBitmap( bitmap, update, null );
	}

	/**
	 * Sets the next bitmap.
	 * 
	 * @param bitmap
	 *           the bitmap
	 * @param update
	 *           the update
	 * @param matrix
	 *           the matrix
	 */
	void setNextBitmap( Bitmap bitmap, boolean update, Matrix matrix ) {
		logger.log( "setNextBitmap", bitmap, update, matrix );

		mContext.getMainImage().setImageBitmap( bitmap, update, matrix, Constants.IMAGE_VIEW_MAX_ZOOM );

		if ( !mBitmap.equals( bitmap ) ) {
			logger.warning( "[recycle] original Bitmap: " + mBitmap );
			mBitmap.recycle();
			mBitmap = null;
		}
		mBitmap = bitmap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.effects.AbstractEffectPanel.OnErrorListener#onError(java.lang.String)
	 */
	@Override
	public void onError( final String error ) {
		new AlertDialog.Builder( (Activity) mContext ).setTitle( R.string.generic_error_title ).setMessage( error )
				.setIcon( android.R.drawable.ic_dialog_alert ).show();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.effects.AbstractEffectPanel.OnPreviewListener#onPreviewChange(android.graphics.Bitmap)
	 */
	@Override
	public void onPreviewChange( final Bitmap result ) {
		if ( !getEnabled() || !isOpened() ) return;

		final boolean changed = getBitmapChanged( ( (IBitmapDrawable) mContext.getMainImage().getDrawable() ).getBitmap(), result );
		mContext.getMainImage().setImageBitmap( result, changed, null, Constants.IMAGE_VIEW_MAX_ZOOM );

		if ( LoggerFactory.LOG_ENABLED ) {
			final ActivityManager activityManager = (ActivityManager) ( (Activity) mContext )
					.getSystemService( Context.ACTIVITY_SERVICE );
			final MemoryInfo mi = new MemoryInfo();
			activityManager.getMemoryInfo( mi );
			logger.log( "memory free", ( mi.availMem / 1048576L ) );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.effects.AbstractEffectPanel.OnPreviewListener#onPreviewChange(android.graphics.ColorFilter)
	 */
	@Override
	public void onPreviewChange( ColorFilter colorFilter ) {
		if ( !getEnabled() || !isOpened() ) return;
		mContext.getMainImage().setColorFilter( colorFilter );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.effects.AbstractEffectPanel.OnContentReadyListener#onReady(com.aviary.android.feather.effects.
	 * AbstractEffectPanel)
	 */
	@Override
	public void onReady( final AbstractEffectPanel panel ) {
		runOnUiThread( new Runnable() {

			@Override
			public void run() {
				mContext.getMainImage().setVisibility( View.GONE );
			}
		} );
	}

	/**
	 * Replace the current bitmap.
	 * 
	 * @param bitmap
	 *           the bitmap
	 */
	public void onReplaceImage( final Bitmap bitmap ) {
		if ( !getEnabled() || !isClosed() ) throw new IllegalStateException( "Cannot replace bitmap. Not active nor closed!" );

		if ( ( mBitmap != null ) && !mBitmap.isRecycled() ) {
			logger.warning( "[recycle] original Bitmap: " + mBitmap );
			mBitmap.recycle();
			mBitmap = null;
		}
		mChanged = false;
		mBitmap = bitmap;

		HiResService service = getService( HiResService.class );
		if ( mHiResEnabled && service.isRunning() ) {
			service.replace( mSessionId, mApiKey, mContext.getOriginalUri() );
		} else {
			if( null != mHiResListener ) {
				mHiResListener.OnLoad( mContext.getOriginalUri() );
			}
		}
	}

	/**
	 * On save.
	 */
	public void onSave() {
		if ( !getEnabled() || !isClosed() ) return;
	}

	/**
	 * Prepare effect panel.
	 * 
	 * @param effect
	 *           the effect
	 * @param entry
	 *           the entry
	 */
	private void prepareEffectPanel( final AbstractEffectPanel effect, final EffectEntry entry ) {
		View option_child = null;
		View drawing_child = null;

		if ( effect instanceof OptionPanel ) {
			option_child = ( (OptionPanel) effect ).getOptionView( mLayoutInflater, mContext.getOptionsPanelContainer() );
			mContext.getOptionsPanelContainer().addView( option_child );
		}

		if ( effect instanceof ContentPanel ) {
			drawing_child = ( (ContentPanel) effect ).getContentView( mLayoutInflater );
			drawing_child.setLayoutParams( new LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT ) );
			mContext.getDrawingImageContainer().addView( drawing_child );
		}

		effect.onCreate( mBitmap );
	}

	/**
	 * Run a Runnable on the main UI thread.
	 * 
	 * @param action
	 *           the action
	 */
	@Override
	public void runOnUiThread( final Runnable action ) {
		if ( mContext != null ) ( (Activity) mContext ).runOnUiThread( action );
	}

	/**
	 * Sets the current state.
	 * 
	 * @param newState
	 *           the new current state
	 */
	private void setCurrentState( final STATE newState ) {
		if ( newState != mCurrentState ) {
			final STATE previousState = mCurrentState;
			mCurrentState = newState;

			switch ( newState ) {
				case OPENING:
					mCurrentEffect.setOnPreviewListener( this );
					mCurrentEffect.setOnApplyResultListener( this );
					mCurrentEffect.setOnErrorListener( this );
					mCurrentEffect.setOnProgressListener( this );

					if ( mCurrentEffect instanceof ContentPanel ) ( (ContentPanel) mCurrentEffect ).setOnReadyListener( this );

					mHandler.sendEmptyMessage( FilterManager.STATE_OPENING );
					break;

				case OPENED:
					mCurrentEffect.onActivate();
					mHandler.sendEmptyMessage( FilterManager.STATE_OPENED );
					break;

				case CLOSING:
					mHandler.sendEmptyMessage( FilterManager.STATE_CLOSING );
					mCurrentEffect.onDeactivate();

					mContext.getMainImage().clearColorFilter();
					mContext.getMainImage().setVisibility( View.VISIBLE );

					if ( mCurrentEffect instanceof ContentPanel ) {
						( (ContentPanel) mCurrentEffect ).setOnReadyListener( null );
					}
					mContext.getDrawingImageContainer().removeAllViews();
					break;

				case CLOSED_CANCEL:
				case CLOSED_CONFIRMED:

					mContext.getOptionsPanelContainer().removeAllViews();

					if ( previousState != STATE.DISABLED ) {
						mCurrentEffect.onDestroy();
						mCurrentEffect.setOnPreviewListener( null );
						mCurrentEffect.setOnApplyResultListener( null );
						mCurrentEffect.setOnErrorListener( null );
						mCurrentEffect.setOnProgressListener( null );
						mCurrentEffect = null;
						mCurrentEntry = null;
					}

					mHandler.sendEmptyMessage( FilterManager.STATE_CLOSED );

					if ( ( newState == STATE.CLOSED_CONFIRMED ) && ( previousState != STATE.DISABLED ) )
						if ( mToolListener != null ) mToolListener.onToolCompleted();
					break;

				case DISABLED:
					mHandler.sendEmptyMessage( FilterManager.STATE_DISABLED );
					break;
			}
		}
	}

	/**
	 * Sets the enabled.
	 * 
	 * @param value
	 *           the new enabled
	 */
	public void setEnabled( final boolean value ) {
		if ( !value ) {
			if ( isClosed() ) {
				setCurrentState( STATE.DISABLED );
			} else {
				logger.warning( "FilterManager must be closed to change state" );
			}
		}
	}

	/**
	 * Sets the on tool listener.
	 * 
	 * @param listener
	 *           the new on tool listener
	 */
	public void setOnToolListener( final OnToolListener listener ) {
		mToolListener = listener;
	}

	/**
	 * Main Activity configuration changed We want to dispatch the configuration event also to the opened panel.
	 * 
	 * @param newConfig
	 *           the new config
	 * @return true if the event has been handled
	 */
	public boolean onConfigurationChanged( Configuration newConfig ) {

		boolean result = false;
		logger.info( "onConfigurationChanged: " + newConfig.orientation + ", " + mConfiguration.orientation );
		
		if ( mCurrentEffect != null ) {
			if ( mCurrentEffect.isCreated() ) {
				logger.info( "onConfigurationChanged, sending event to ", mCurrentEffect );
				mCurrentEffect.onConfigurationChanged( newConfig, mConfiguration );
				result = true;
			}
		}
		
		mConfiguration = new Configuration( newConfig );
		return result;
	}

	/**
	 * A plugin or theme has been installed/removed or replaced Notify the internal pluginservice about the new plugin. All the
	 * classes which have a listener attached to the PluginService will be notified too.
	 * 
	 * @param intent
	 *           the intent
	 * @see FeatherSystemReceiver
	 */
	public void onPluginChanged( Intent intent ) {
		logger.info( "onReceive", intent );
		logger.info( "data", intent.getData() );

		// TODO move the update process inside the PluginManager

		BackgroundService backgroundService = getService( BackgroundService.class );
		PluginManager pluginManager = (PluginManager) backgroundService.getInternalTask( PluginManager.class );
		pluginManager.update( intent.getExtras() );

		// PluginService pluginService = getService( PluginService.class );
		// if ( pluginService != null ) pluginService.update( intent );
	}

	/** The m background handler. */
	private Handler mBackgroundHandler = new Handler() {

		@Override
		public void handleMessage( Message msg ) {

			switch ( msg.what ) {
				case BackgroundService.TASK_COMPLETED:

					Bundle result = msg.getData();
					logger.debug( "task completed: " + msg.obj );

					if ( msg.obj.equals( PluginManager.class.getName() ) ) {
						BackgroundService backgroundService = getService( BackgroundService.class );
						if ( backgroundService != null ) {
							PluginManager pluginManager = (PluginManager) backgroundService.getInternalTask( PluginManager.class );
							PluginService pluginService = getService( PluginService.class );
							if ( pluginService != null ) {
								pluginService.update( pluginManager.getInstalledPlugins(), result );
							}
						}
					}
					
					else if(msg.obj.equals(ExternalPacksTask.class.getName())){
						BackgroundService backgroundService = getService( BackgroundService.class );
						if ( backgroundService != null ) {
							//ExternalPacksTask task = (ExternalPacksTask) backgroundService.getInternalTask( ExternalPacksTask.class );
							PluginService pluginService = getService( PluginService.class );
							if ( pluginService != null ) {
								pluginService.updateExternalPackages(result);
							}
						}
					}
					
					break;
			}
			super.handleMessage( msg );
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.effects.AbstractEffectPanel.OnProgressListener#onProgressStart()
	 */
	@Override
	public void onProgressStart() {
		mContext.showToolProgress();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.effects.AbstractEffectPanel.OnProgressListener#onProgressEnd()
	 */
	@Override
	public void onProgressEnd() {
		mContext.hideToolProgress();
	}

	@Override
	public void onProgressModalStart() {
		mContext.showModalProgress();
	}

	@Override
	public void onProgressModalEnd() {
		mContext.hideModalProgress();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.library.services.EffectContext#setToolbarTitle(int)
	 */
	@Override
	public void setToolbarTitle( int resId ) {
		setToolbarTitle( getBaseContext().getString( resId ) );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.library.services.EffectContext#setToolbarTitle(java.lang.CharSequence)
	 */
	@Override
	public void setToolbarTitle( CharSequence value ) {
		mContext.getToolbar().setTitle( value );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aviary.android.feather.library.services.EffectContext#restoreToolbarTitle()
	 */
	@Override
	public void restoreToolbarTitle() {
		if ( mCurrentEntry != null ) mContext.getToolbar().setTitle( mCurrentEntry.labelResourceId );
	}
	
	@Override
	public void downloadPlugin( final String packageName, final int type ){
		logger.info( "downloadPlugins: " + packageName + ", type: " + type );
		
		final String referrer = getBaseContext().getPackageName();
		
		Intent intent = new Intent( Intent.ACTION_VIEW );
		intent.setData( Uri.parse( "market://details?id=" + packageName + "&referrer=" + referrer  ) );

		try {

			HashMap<String, String> attrs = new HashMap<String, String>();
			attrs.put( "assetType", FeatherIntent.PluginType.getName( type ) );
			Tracker.recordTag( "content: addMoreClicked", attrs );

			getBaseContext().startActivity( intent );
		} catch ( ActivityNotFoundException e ) {
			Toast.makeText( getBaseContext(), R.string.feather_activity_not_found, Toast.LENGTH_SHORT ).show();
			e.printStackTrace();
		}
	}
}
