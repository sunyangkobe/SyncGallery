package com.aviary.android.feather.widget;

import it.sephiroth.android.library.imagezoom.easing.Easing;
import it.sephiroth.android.library.imagezoom.easing.Expo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews.RemoteView;

// TODO: Auto-generated Javadoc
/**
 * Displays an arbitrary image, such as an icon. The ImageView class can load images from various sources (such as resources or
 * content providers), takes care of computing its measurement from the image so that it can be used in any layout manager, and
 * provides various display options such as scaling and tinting.
 * 
 * @attr ref android.R.styleable#ImageView_adjustViewBounds
 * @attr ref android.R.styleable#ImageView_src
 * @attr ref android.R.styleable#ImageView_maxWidth
 * @attr ref android.R.styleable#ImageView_maxHeight
 * @attr ref android.R.styleable#ImageView_tint
 * @attr ref android.R.styleable#ImageView_scaleType
 * @attr ref android.R.styleable#ImageView_cropToPadding
 */
@RemoteView
public class AdjustImageView extends View {

	/** The Constant LOG_TAG. */
	static final String LOG_TAG = "rotate";

	// settable by the client
	/** The m uri. */
	private Uri mUri;
	
	/** The m resource. */
	private int mResource = 0;
	
	/** The m matrix. */
	private Matrix mMatrix;
	
	/** The m scale type. */
	private ScaleType mScaleType;
	
	/** The m adjust view bounds. */
	private boolean mAdjustViewBounds = false;
	
	/** The m max width. */
	private int mMaxWidth = Integer.MAX_VALUE;
	
	/** The m max height. */
	private int mMaxHeight = Integer.MAX_VALUE;

	// these are applied to the drawable
	/** The m color filter. */
	private ColorFilter mColorFilter;
	
	/** The m alpha. */
	private int mAlpha = 255;
	
	/** The m view alpha scale. */
	private int mViewAlphaScale = 256;
	
	/** The m color mod. */
	private boolean mColorMod = false;

	/** The m drawable. */
	private Drawable mDrawable = null;
	
	/** The m state. */
	private int[] mState = null;
	
	/** The m merge state. */
	private boolean mMergeState = false;
	
	/** The m level. */
	private int mLevel = 0;
	
	/** The m drawable width. */
	private int mDrawableWidth;
	
	/** The m drawable height. */
	private int mDrawableHeight;
	
	/** The m draw matrix. */
	private Matrix mDrawMatrix = null;
	
	/** The m rotate matrix. */
	private Matrix mRotateMatrix = new Matrix();
	
	/** The m flip matrix. */
	private Matrix mFlipMatrix = new Matrix();

	// Avoid allocations...
	/** The m temp src. */
	private RectF mTempSrc = new RectF();
	
	/** The m temp dst. */
	private RectF mTempDst = new RectF();

	/** The m crop to padding. */
	private boolean mCropToPadding;

	/** The m baseline. */
	private int mBaseline = -1;
	
	/** The m baseline align bottom. */
	private boolean mBaselineAlignBottom = false;

	/** The m have frame. */
	private boolean mHaveFrame;
	
	/** The m easing. */
	private Easing mEasing = new Expo();

	/** View is in the reset state. */
	boolean isReset = false;

	/** reset animation time. */
	int resetAnimTime = 200;

	/**
	 * Sets the reset anim duration.
	 *
	 * @param value the new reset anim duration
	 */
	public void setResetAnimDuration( int value ) {
		resetAnimTime = value;
	}

	/**
	 * The listener interface for receiving onReset events.
	 * The class that is interested in processing a onReset
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addOnResetListener<code> method. When
	 * the onReset event occurs, that object's appropriate
	 * method is invoked.
	 *
	 * @see OnResetEvent
	 */
	public interface OnResetListener {

		/**
		 * On reset complete.
		 */
		void onResetComplete();
	}

	/** The m reset listener. */
	private OnResetListener mResetListener;

	/**
	 * Sets the on reset listener.
	 *
	 * @param listener the new on reset listener
	 */
	public void setOnResetListener( OnResetListener listener ) {
		mResetListener = listener;
	}

	/** The Constant sScaleTypeArray. */
	@SuppressWarnings("unused")
	private static final ScaleType[] sScaleTypeArray = {
		ScaleType.MATRIX, ScaleType.FIT_XY, ScaleType.FIT_START, ScaleType.FIT_CENTER, ScaleType.FIT_END, ScaleType.CENTER,
		ScaleType.CENTER_CROP, ScaleType.CENTER_INSIDE };

	/**
	 * Instantiates a new adjust image view.
	 *
	 * @param context the context
	 */
	public AdjustImageView( Context context ) {
		super( context );
		initImageView();
	}

	/**
	 * Instantiates a new adjust image view.
	 *
	 * @param context the context
	 * @param attrs the attrs
	 */
	public AdjustImageView( Context context, AttributeSet attrs ) {
		this( context, attrs, 0 );
	}

	/**
	 * Instantiates a new adjust image view.
	 *
	 * @param context the context
	 * @param attrs the attrs
	 * @param defStyle the def style
	 */
	public AdjustImageView( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
		initImageView();

		/*
		 * TypedArray a = context.obtainStyledAttributes( attrs, com.android.internal.R.styleable.ImageView, defStyle, 0 );
		 * 
		 * Drawable d = a.getDrawable( com.android.internal.R.styleable.ImageView_src ); if ( d != null ) { setImageDrawable( d ); }
		 * 
		 * mBaselineAlignBottom = a.getBoolean( com.android.internal.R.styleable.ImageView_baselineAlignBottom, false );
		 * 
		 * mBaseline = a.getDimensionPixelSize( com.android.internal.R.styleable.ImageView_baseline, -1 );
		 * 
		 * setAdjustViewBounds( a.getBoolean( com.android.internal.R.styleable.ImageView_adjustViewBounds, false ) );
		 * 
		 * setMaxWidth( a.getDimensionPixelSize( com.android.internal.R.styleable.ImageView_maxWidth, Integer.MAX_VALUE ) );
		 * 
		 * setMaxHeight( a.getDimensionPixelSize( com.android.internal.R.styleable.ImageView_maxHeight, Integer.MAX_VALUE ) );
		 * 
		 * int index = a.getInt( com.android.internal.R.styleable.ImageView_scaleType, -1 ); if ( index >= 0 ) { setScaleType(
		 * sScaleTypeArray[index] ); }
		 * 
		 * int tint = a.getInt( com.android.internal.R.styleable.ImageView_tint, 0 ); if ( tint != 0 ) { setColorFilter( tint ); }
		 * 
		 * int alpha = a.getInt( com.android.internal.R.styleable.ImageView_drawableAlpha, 255 ); if ( alpha != 255 ) { setAlpha(
		 * alpha ); }
		 * 
		 * mCropToPadding = a.getBoolean( com.android.internal.R.styleable.ImageView_cropToPadding, false );
		 * 
		 * a.recycle();
		 */

		// need inflate syntax/reader for matrix
	}

	/**
	 * Sets the easing.
	 *
	 * @param value the new easing
	 */
	public void setEasing( Easing value ) {
		mEasing = value;
	}

	/**
	 * Inits the image view.
	 */
	private void initImageView() {
		mMatrix = new Matrix();
		mScaleType = ScaleType.FIT_CENTER;
	}

	/* (non-Javadoc)
	 * @see android.view.View#verifyDrawable(android.graphics.drawable.Drawable)
	 */
	@Override
	protected boolean verifyDrawable( Drawable dr ) {
		return mDrawable == dr || super.verifyDrawable( dr );
	}

	/* (non-Javadoc)
	 * @see android.view.View#invalidateDrawable(android.graphics.drawable.Drawable)
	 */
	@Override
	public void invalidateDrawable( Drawable dr ) {
		if ( dr == mDrawable ) {
			/*
			 * we invalidate the whole view in this case because it's very hard to know where the drawable actually is. This is made
			 * complicated because of the offsets and transformations that can be applied. In theory we could get the drawable's bounds
			 * and run them through the transformation and offsets, but this is probably not worth the effort.
			 */
			invalidate();
		} else {
			super.invalidateDrawable( dr );
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View#onSetAlpha(int)
	 */
	@Override
	protected boolean onSetAlpha( int alpha ) {
		if ( getBackground() == null ) {
			int scale = alpha + ( alpha >> 7 );
			if ( mViewAlphaScale != scale ) {
				mViewAlphaScale = scale;
				mColorMod = true;
				applyColorMod();
			}
			return true;
		}
		return false;
	}

	/**
	 * Set this to true if you want the ImageView to adjust its bounds to preserve the aspect ratio of its drawable.
	 * 
	 * @param adjustViewBounds
	 *           Whether to adjust the bounds of this view to presrve the original aspect ratio of the drawable
	 * 
	 * @attr ref android.R.styleable#ImageView_adjustViewBounds
	 */
	public void setAdjustViewBounds( boolean adjustViewBounds ) {
		mAdjustViewBounds = adjustViewBounds;
		if ( adjustViewBounds ) {
			setScaleType( ScaleType.FIT_CENTER );
		}
	}

	/**
	 * An optional argument to supply a maximum width for this view. Only valid if {@link #setAdjustViewBounds(boolean)} has been set
	 * to true. To set an image to be a maximum of 100 x 100 while preserving the original aspect ratio, do the following: 1) set
	 * adjustViewBounds to true 2) set maxWidth and maxHeight to 100 3) set the height and width layout params to WRAP_CONTENT.
	 * 
	 * <p>
	 * Note that this view could be still smaller than 100 x 100 using this approach if the original image is small. To set an image
	 * to a fixed size, specify that size in the layout params and then use {@link #setScaleType(android.widget.ImageView.ScaleType)}
	 * to determine how to fit the image within the bounds.
	 * </p>
	 * 
	 * @param maxWidth
	 *           maximum width for this view
	 * 
	 * @attr ref android.R.styleable#ImageView_maxWidth
	 */
	public void setMaxWidth( int maxWidth ) {
		mMaxWidth = maxWidth;
	}

	/**
	 * An optional argument to supply a maximum height for this view. Only valid if {@link #setAdjustViewBounds(boolean)} has been
	 * set to true. To set an image to be a maximum of 100 x 100 while preserving the original aspect ratio, do the following: 1) set
	 * adjustViewBounds to true 2) set maxWidth and maxHeight to 100 3) set the height and width layout params to WRAP_CONTENT.
	 * 
	 * <p>
	 * Note that this view could be still smaller than 100 x 100 using this approach if the original image is small. To set an image
	 * to a fixed size, specify that size in the layout params and then use {@link #setScaleType(android.widget.ImageView.ScaleType)}
	 * to determine how to fit the image within the bounds.
	 * </p>
	 * 
	 * @param maxHeight
	 *           maximum height for this view
	 * 
	 * @attr ref android.R.styleable#ImageView_maxHeight
	 */
	public void setMaxHeight( int maxHeight ) {
		mMaxHeight = maxHeight;
	}

	/**
	 * Return the view's drawable, or null if no drawable has been assigned.
	 *
	 * @return the drawable
	 */
	public Drawable getDrawable() {
		return mDrawable;
	}

	/**
	 * Sets a drawable as the content of this ImageView.
	 * 
	 * <p class="note">
	 * This does Bitmap reading and decoding on the UI thread, which can cause a latency hiccup. If that's a concern, consider using
	 *
	 * @param resId the resource identifier of the the drawable
	 * {@link #setImageDrawable(android.graphics.drawable.Drawable)} or {@link #setImageBitmap(android.graphics.Bitmap)} and
	 * {@link android.graphics.BitmapFactory} instead.
	 * </p>
	 * @attr ref android.R.styleable#ImageView_src
	 */
	public void setImageResource( int resId ) {
		if ( mUri != null || mResource != resId ) {
			updateDrawable( null );
			mResource = resId;
			mUri = null;
			resolveUri();
			requestLayout();
			invalidate();
		}
	}

	/**
	 * Sets the content of this ImageView to the specified Uri.
	 * 
	 * <p class="note">
	 * This does Bitmap reading and decoding on the UI thread, which can cause a latency hiccup. If that's a concern, consider using
	 *
	 * @param uri The Uri of an image
	 * {@link #setImageDrawable(android.graphics.drawable.Drawable)} or {@link #setImageBitmap(android.graphics.Bitmap)} and
	 * {@link android.graphics.BitmapFactory} instead.
	 * </p>
	 */
	public void setImageURI( Uri uri ) {
		if ( mResource != 0 || ( mUri != uri && ( uri == null || mUri == null || !uri.equals( mUri ) ) ) ) {
			updateDrawable( null );
			mResource = 0;
			mUri = uri;
			resolveUri();
			requestLayout();
			invalidate();
		}
	}

	/**
	 * Sets a drawable as the content of this ImageView.
	 * 
	 * @param drawable
	 *           The drawable to set
	 */
	public void setImageDrawable( Drawable drawable ) {
		if ( mDrawable != drawable ) {
			mResource = 0;
			mUri = null;

			int oldWidth = mDrawableWidth;
			int oldHeight = mDrawableHeight;

			updateDrawable( drawable );

			if ( oldWidth != mDrawableWidth || oldHeight != mDrawableHeight ) {
				requestLayout();
			}
			invalidate();
		}
	}

	/**
	 * Sets a Bitmap as the content of this ImageView.
	 * 
	 * @param bm
	 *           The bitmap to set
	 */
	public void setImageBitmap( Bitmap bm ) {
		// if this is used frequently, may handle bitmaps explicitly
		// to reduce the intermediate drawable object
		setImageDrawable( new BitmapDrawable( getContext().getResources(), bm ) );
	}

	/**
	 * Sets the image state.
	 *
	 * @param state the state
	 * @param merge the merge
	 */
	public void setImageState( int[] state, boolean merge ) {
		mState = state;
		mMergeState = merge;
		if ( mDrawable != null ) {
			refreshDrawableState();
			resizeFromDrawable();
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View#setSelected(boolean)
	 */
	@Override
	public void setSelected( boolean selected ) {
		super.setSelected( selected );
		resizeFromDrawable();
	}

	/**
	 * Sets the image level, when it is constructed from a {@link android.graphics.drawable.LevelListDrawable}.
	 * 
	 * @param level
	 *           The new level for the image.
	 */
	public void setImageLevel( int level ) {
		mLevel = level;
		if ( mDrawable != null ) {
			mDrawable.setLevel( level );
			resizeFromDrawable();
		}
	}

	/**
	 * Options for scaling the bounds of an image to the bounds of this view.
	 */
	public enum ScaleType {
		/**
		 * Scale using the image matrix when drawing. The image matrix can be set using {@link ImageView#setImageMatrix(Matrix)}. From
		 * XML, use this syntax: <code>android:scaleType="matrix"</code>.
		 */
		MATRIX( 0 ),
		/**
		 * Scale the image using {@link Matrix.ScaleToFit#FILL}. From XML, use this syntax: <code>android:scaleType="fitXY"</code>.
		 */
		FIT_XY( 1 ),
		/**
		 * Scale the image using {@link Matrix.ScaleToFit#START}. From XML, use this syntax: <code>android:scaleType="fitStart"</code>
		 * .
		 */
		FIT_START( 2 ),
		/**
		 * Scale the image using {@link Matrix.ScaleToFit#CENTER}. From XML, use this syntax:
		 * <code>android:scaleType="fitCenter"</code>.
		 */
		FIT_CENTER( 3 ),
		/**
		 * Scale the image using {@link Matrix.ScaleToFit#END}. From XML, use this syntax: <code>android:scaleType="fitEnd"</code>.
		 */
		FIT_END( 4 ),
		/**
		 * Center the image in the view, but perform no scaling. From XML, use this syntax: <code>android:scaleType="center"</code>.
		 */
		CENTER( 5 ),
		/**
		 * Scale the image uniformly (maintain the image's aspect ratio) so that both dimensions (width and height) of the image will
		 * be equal to or larger than the corresponding dimension of the view (minus padding). The image is then centered in the view.
		 * From XML, use this syntax: <code>android:scaleType="centerCrop"</code>.
		 */
		CENTER_CROP( 6 ),
		/**
		 * Scale the image uniformly (maintain the image's aspect ratio) so that both dimensions (width and height) of the image will
		 * be equal to or less than the corresponding dimension of the view (minus padding). The image is then centered in the view.
		 * From XML, use this syntax: <code>android:scaleType="centerInside"</code>.
		 */
		CENTER_INSIDE( 7 );

		/**
		 * Instantiates a new scale type.
		 *
		 * @param ni the ni
		 */
		ScaleType( int ni ) {
			nativeInt = ni;
		}

		/** The native int. */
		final int nativeInt;
	}

	/**
	 * Controls how the image should be resized or moved to match the size of this ImageView.
	 * 
	 * @param scaleType
	 *           The desired scaling mode.
	 * 
	 * @attr ref android.R.styleable#ImageView_scaleType
	 */
	public void setScaleType( ScaleType scaleType ) {
		if ( scaleType == null ) {
			throw new NullPointerException();
		}

		if ( mScaleType != scaleType ) {
			mScaleType = scaleType;

			setWillNotCacheDrawing( mScaleType == ScaleType.CENTER );

			requestLayout();
			invalidate();
		}
	}

	/**
	 * Return the current scale type in use by this ImageView.
	 *
	 * @return the scale type
	 * @see ImageView.ScaleType
	 * @attr ref android.R.styleable#ImageView_scaleType
	 */
	public ScaleType getScaleType() {
		return mScaleType;
	}

	/**
	 * Return the view's optional matrix. This is applied to the view's drawable when it is drawn. If there is not matrix, this
	 * method will return null. Do not change this matrix in place. If you want a different matrix applied to the drawable, be sure
	 * to call setImageMatrix().
	 *
	 * @return the image matrix
	 */
	public Matrix getImageMatrix() {
		return mMatrix;
	}

	/**
	 * Sets the image matrix.
	 *
	 * @param matrix the new image matrix
	 */
	public void setImageMatrix( Matrix matrix ) {
		// collaps null and identity to just null
		if ( matrix != null && matrix.isIdentity() ) {
			matrix = null;
		}

		// don't invalidate unless we're actually changing our matrix
		if ( matrix == null && !mMatrix.isIdentity() || matrix != null && !mMatrix.equals( matrix ) ) {
			mMatrix.set( matrix );
			configureBounds();
			invalidate();
		}
	}

	/**
	 * Resolve uri.
	 */
	private void resolveUri() {
		if ( mDrawable != null ) {
			return;
		}

		Resources rsrc = getResources();
		if ( rsrc == null ) {
			return;
		}

		Drawable d = null;

		if ( mResource != 0 ) {
			try {
				d = rsrc.getDrawable( mResource );
			} catch ( Exception e ) {
				Log.w( LOG_TAG, "Unable to find resource: " + mResource, e );
				// Don't try again.
				mUri = null;
			}
		} else if ( mUri != null ) {
			String scheme = mUri.getScheme();
			if ( ContentResolver.SCHEME_ANDROID_RESOURCE.equals( scheme ) ) {

			} else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) || ContentResolver.SCHEME_FILE.equals( scheme ) ) {
				try {
					d = Drawable.createFromStream( getContext().getContentResolver().openInputStream( mUri ), null );
				} catch ( Exception e ) {
					Log.w( LOG_TAG, "Unable to open content: " + mUri, e );
				}
			} else {
				d = Drawable.createFromPath( mUri.toString() );
			}

			if ( d == null ) {
				System.out.println( "resolveUri failed on bad bitmap uri: " + mUri );
				// Don't try again.
				mUri = null;
			}
		} else {
			return;
		}

		updateDrawable( d );
	}

	/* (non-Javadoc)
	 * @see android.view.View#onCreateDrawableState(int)
	 */
	@Override
	public int[] onCreateDrawableState( int extraSpace ) {
		if ( mState == null ) {
			return super.onCreateDrawableState( extraSpace );
		} else if ( !mMergeState ) {
			return mState;
		} else {
			return mergeDrawableStates( super.onCreateDrawableState( extraSpace + mState.length ), mState );
		}
	}

	/**
	 * Update drawable.
	 *
	 * @param d the d
	 */
	private void updateDrawable( Drawable d ) {
		if ( mDrawable != null ) {
			mDrawable.setCallback( null );
			unscheduleDrawable( mDrawable );
		}
		mDrawable = d;
		if ( d != null ) {
			d.setCallback( this );
			if ( d.isStateful() ) {
				d.setState( getDrawableState() );
			}
			d.setLevel( mLevel );
			mDrawableWidth = d.getIntrinsicWidth();
			mDrawableHeight = d.getIntrinsicHeight();
			applyColorMod();
			configureBounds();
		} else {
			mDrawableWidth = mDrawableHeight = -1;
		}
	}

	/**
	 * Resize from drawable.
	 */
	private void resizeFromDrawable() {
		Drawable d = mDrawable;
		if ( d != null ) {
			int w = d.getIntrinsicWidth();
			if ( w < 0 ) w = mDrawableWidth;
			int h = d.getIntrinsicHeight();
			if ( h < 0 ) h = mDrawableHeight;
			if ( w != mDrawableWidth || h != mDrawableHeight ) {
				mDrawableWidth = w;
				mDrawableHeight = h;
				requestLayout();
			}
		}
	}

	/** The Constant sS2FArray. */
	private static final Matrix.ScaleToFit[] sS2FArray = {
		Matrix.ScaleToFit.FILL, Matrix.ScaleToFit.START, Matrix.ScaleToFit.CENTER, Matrix.ScaleToFit.END };

	/**
	 * Scale type to scale to fit.
	 *
	 * @param st the st
	 * @return the matrix. scale to fit
	 */
	private static Matrix.ScaleToFit scaleTypeToScaleToFit( ScaleType st ) {
		// ScaleToFit enum to their corresponding Matrix.ScaleToFit values
		return sS2FArray[st.nativeInt - 1];
	}

	/* (non-Javadoc)
	 * @see android.view.View#onLayout(boolean, int, int, int, int)
	 */
	@Override
	protected void onLayout( boolean changed, int left, int top, int right, int bottom ) {
		super.onLayout( changed, left, top, right, bottom );

		if ( changed ) {
			mHaveFrame = true;
			configureBounds();
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View#onMeasure(int, int)
	 */
	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
		resolveUri();
		int w;
		int h;

		// Desired aspect ratio of the view's contents (not including padding)
		float desiredAspect = 0.0f;

		// We are allowed to change the view's width
		boolean resizeWidth = false;

		// We are allowed to change the view's height
		boolean resizeHeight = false;

		final int widthSpecMode = MeasureSpec.getMode( widthMeasureSpec );
		final int heightSpecMode = MeasureSpec.getMode( heightMeasureSpec );

		if ( mDrawable == null ) {
			// If no drawable, its intrinsic size is 0.
			mDrawableWidth = -1;
			mDrawableHeight = -1;
			w = h = 0;
		} else {
			w = mDrawableWidth;
			h = mDrawableHeight;
			if ( w <= 0 ) w = 1;
			if ( h <= 0 ) h = 1;

			// We are supposed to adjust view bounds to match the aspect
			// ratio of our drawable. See if that is possible.
			if ( mAdjustViewBounds ) {
				resizeWidth = widthSpecMode != MeasureSpec.EXACTLY;
				resizeHeight = heightSpecMode != MeasureSpec.EXACTLY;

				desiredAspect = (float) w / (float) h;
			}
		}

		int pleft = getPaddingLeft();
		int pright = getPaddingRight();
		int ptop = getPaddingTop();
		int pbottom = getPaddingBottom();

		int widthSize;
		int heightSize;

		if ( resizeWidth || resizeHeight ) {
			/*
			 * If we get here, it means we want to resize to match the drawables aspect ratio, and we have the freedom to change at
			 * least one dimension.
			 */

			// Get the max possible width given our constraints
			widthSize = resolveAdjustedSize( w + pleft + pright, mMaxWidth, widthMeasureSpec );

			// Get the max possible height given our constraints
			heightSize = resolveAdjustedSize( h + ptop + pbottom, mMaxHeight, heightMeasureSpec );

			if ( desiredAspect != 0.0f ) {
				// See what our actual aspect ratio is
				float actualAspect = (float) ( widthSize - pleft - pright ) / ( heightSize - ptop - pbottom );

				if ( Math.abs( actualAspect - desiredAspect ) > 0.0000001 ) {

					boolean done = false;

					// Try adjusting width to be proportional to height
					if ( resizeWidth ) {
						int newWidth = (int) ( desiredAspect * ( heightSize - ptop - pbottom ) ) + pleft + pright;
						if ( newWidth <= widthSize ) {
							widthSize = newWidth;
							done = true;
						}
					}

					// Try adjusting height to be proportional to width
					if ( !done && resizeHeight ) {
						int newHeight = (int) ( ( widthSize - pleft - pright ) / desiredAspect ) + ptop + pbottom;
						if ( newHeight <= heightSize ) {
							heightSize = newHeight;
						}
					}
				}
			}
		} else {
			/*
			 * We are either don't want to preserve the drawables aspect ratio, or we are not allowed to change view dimensions. Just
			 * measure in the normal way.
			 */
			w += pleft + pright;
			h += ptop + pbottom;

			w = Math.max( w, getSuggestedMinimumWidth() );
			h = Math.max( h, getSuggestedMinimumHeight() );

			widthSize = resolveSize( w, widthMeasureSpec );
			heightSize = resolveSize( h, heightMeasureSpec );
		}

		setMeasuredDimension( widthSize, heightSize );
	}

	/**
	 * Resolve adjusted size.
	 *
	 * @param desiredSize the desired size
	 * @param maxSize the max size
	 * @param measureSpec the measure spec
	 * @return the int
	 */
	private int resolveAdjustedSize( int desiredSize, int maxSize, int measureSpec ) {
		int result = desiredSize;
		int specMode = MeasureSpec.getMode( measureSpec );
		int specSize = MeasureSpec.getSize( measureSpec );
		switch ( specMode ) {
			case MeasureSpec.UNSPECIFIED:
				/*
				 * Parent says we can be as big as we want. Just don't be larger than max size imposed on ourselves.
				 */
				result = Math.min( desiredSize, maxSize );
				break;
			case MeasureSpec.AT_MOST:
				// Parent says we can be as big as we want, up to specSize.
				// Don't be larger than specSize, and don't be larger than
				// the max size imposed on ourselves.
				result = Math.min( Math.min( desiredSize, specSize ), maxSize );
				break;
			case MeasureSpec.EXACTLY:
				// No choice. Do what we are told.
				result = specSize;
				break;
		}
		return result;
	}

	/**
	 * Configure bounds.
	 */
	private void configureBounds() {
		if ( mDrawable == null || !mHaveFrame ) {
			return;
		}

		int dwidth = mDrawableWidth;
		int dheight = mDrawableHeight;

		int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
		int vheight = getHeight() - getPaddingTop() - getPaddingBottom();

		boolean fits = ( dwidth < 0 || vwidth == dwidth ) && ( dheight < 0 || vheight == dheight );

		if ( dwidth <= 0 || dheight <= 0 || ScaleType.FIT_XY == mScaleType ) {
			/*
			 * If the drawable has no intrinsic size, or we're told to scaletofit, then we just fill our entire view.
			 */
			mDrawable.setBounds( 0, 0, vwidth, vheight );
			mDrawMatrix = null;
		} else {
			// We need to do the scaling ourself, so have the drawable
			// use its native size.
			mDrawable.setBounds( 0, 0, dwidth, dheight );

			if ( ScaleType.MATRIX == mScaleType ) {
				// Use the specified matrix as-is.
				if ( mMatrix.isIdentity() ) {
					mDrawMatrix = null;
				} else {
					mDrawMatrix = mMatrix;
				}
			} else if ( fits ) {
				// The bitmap fits exactly, no transform needed.
				mDrawMatrix = null;
			} else if ( ScaleType.CENTER == mScaleType ) {
				// Center bitmap in view, no scaling.
				mDrawMatrix = mMatrix;
				mDrawMatrix.setTranslate( (int) ( ( vwidth - dwidth ) * 0.5f + 0.5f ), (int) ( ( vheight - dheight ) * 0.5f + 0.5f ) );
			} else if ( ScaleType.CENTER_CROP == mScaleType ) {
				mDrawMatrix = mMatrix;

				float scale;
				float dx = 0, dy = 0;

				if ( dwidth * vheight > vwidth * dheight ) {
					scale = (float) vheight / (float) dheight;
					dx = ( vwidth - dwidth * scale ) * 0.5f;
				} else {
					scale = (float) vwidth / (float) dwidth;
					dy = ( vheight - dheight * scale ) * 0.5f;
				}

				mDrawMatrix.setScale( scale, scale );
				mDrawMatrix.postTranslate( (int) ( dx + 0.5f ), (int) ( dy + 0.5f ) );
			} else if ( ScaleType.CENTER_INSIDE == mScaleType ) {
				mDrawMatrix = mMatrix;
				float scale;
				float dx;
				float dy;

				if ( dwidth <= vwidth && dheight <= vheight ) {
					scale = 1.0f;
				} else {
					scale = Math.min( (float) vwidth / (float) dwidth, (float) vheight / (float) dheight );
				}

				dx = (int) ( ( vwidth - dwidth * scale ) * 0.5f + 0.5f );
				dy = (int) ( ( vheight - dheight * scale ) * 0.5f + 0.5f );

				mDrawMatrix.setScale( scale, scale );
				mDrawMatrix.postTranslate( dx, dy );
			} else {
				// Generate the required transform.
				mTempSrc.set( 0, 0, dwidth, dheight );
				mTempDst.set( 0, 0, vwidth, vheight );

				mDrawMatrix = mMatrix;
				mDrawMatrix.setRectToRect( mTempSrc, mTempDst, scaleTypeToScaleToFit( mScaleType ) );
				mCurrentScale = getMatrixScale( mDrawMatrix )[0];

				Matrix tempMatrix = new Matrix( mMatrix );
				RectF src = new RectF();
				RectF dst = new RectF();
				src.set( 0, 0, dheight, dwidth );
				dst.set( 0, 0, vwidth, vheight );
				tempMatrix.setRectToRect( src, dst, scaleTypeToScaleToFit( mScaleType ) );

				mVerticalScale = getValue( tempMatrix, Matrix.MSCALE_X );

				Log.d( LOG_TAG, "current scale: " + mCurrentScale );
				Log.d( LOG_TAG, "vertical scale: " + mVerticalScale );

				tempMatrix = new Matrix( mDrawMatrix );
				tempMatrix.invert( tempMatrix );

				float invertScale = getMatrixScale( tempMatrix )[0];

				Log.d( LOG_TAG, "inverted scale: " + invertScale );
				Log.d( LOG_TAG, "diff: " + ( invertScale - mCurrentScale ) );

				mDrawMatrix.postScale( invertScale, invertScale, vwidth / 2, vheight / 2 );
				mRotateMatrix.postScale( mCurrentScale, mCurrentScale, vwidth / 2, vheight / 2 );

				Log.d( LOG_TAG, "now matrix scale: " + getMatrixScale( mDrawMatrix )[0] );
			}
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View#drawableStateChanged()
	 */
	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		Drawable d = mDrawable;
		if ( d != null && d.isStateful() ) {
			d.setState( getDrawableState() );
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw( Canvas canvas ) {
		super.onDraw( canvas );

		if ( mDrawable == null ) {
			return; // couldn't resolve the URI
		}

		if ( mDrawableWidth == 0 || mDrawableHeight == 0 ) {
			return; // nothing to draw (empty bounds)
		}

		final int mPaddingTop = getPaddingTop();
		final int mPaddingLeft = getPaddingLeft();
		final int mPaddingBottom = getPaddingBottom();
		final int mPaddingRight = getPaddingRight();

		if ( mDrawMatrix == null && mPaddingTop == 0 && mPaddingLeft == 0 ) {
			mDrawable.draw( canvas );
		} else {
			int saveCount = canvas.getSaveCount();
			canvas.save();

			if ( mCropToPadding ) {
				final int scrollX = getScrollX();
				final int scrollY = getScrollY();
				canvas.clipRect( scrollX + mPaddingLeft, scrollY + mPaddingTop, scrollX + getRight() - getLeft() - mPaddingRight,
						scrollY + getBottom() - getTop() - mPaddingBottom );
			}

			canvas.translate( mPaddingLeft, mPaddingTop );

			if ( mFlipMatrix != null ) {
				canvas.concat( mFlipMatrix );
			}

			if ( mRotateMatrix != null ) {
				canvas.concat( mRotateMatrix );
			}

			if ( mDrawMatrix != null ) {
				canvas.concat( mDrawMatrix );
			}

			mDrawable.draw( canvas );
			canvas.restoreToCount( saveCount );
		}
	}

	/**
	 * <p>
	 * Return the offset of the widget's text baseline from the widget's top boundary.
	 * </p>
	 * 
	 * @return the offset of the baseline within the widget's bounds or -1 if baseline alignment is not supported.
	 */
	@Override
	public int getBaseline() {
		if ( mBaselineAlignBottom ) {
			return getMeasuredHeight();
		} else {
			return mBaseline;
		}
	}

	/**
	 * <p>
	 * Set the offset of the widget's text baseline from the widget's top boundary. This value is overridden by the
	 *
	 * @param baseline The baseline to use, or -1 if none is to be provided.
	 * {@link #setBaselineAlignBottom(boolean)} property.
	 * </p>
	 * @see #setBaseline(int)
	 * @attr ref android.R.styleable#ImageView_baseline
	 */
	public void setBaseline( int baseline ) {
		if ( mBaseline != baseline ) {
			mBaseline = baseline;
			requestLayout();
		}
	}

	/**
	 * Set whether to set the baseline of this view to the bottom of the view. Setting this value overrides any calls to setBaseline.
	 * 
	 * @param aligned
	 *           If true, the image view will be baseline aligned with based on its bottom edge.
	 * 
	 * @attr ref android.R.styleable#ImageView_baselineAlignBottom
	 */
	public void setBaselineAlignBottom( boolean aligned ) {
		if ( mBaselineAlignBottom != aligned ) {
			mBaselineAlignBottom = aligned;
			requestLayout();
		}
	}

	/**
	 * Return whether this view's baseline will be considered the bottom of the view.
	 *
	 * @return the baseline align bottom
	 * @see #setBaselineAlignBottom(boolean)
	 */
	public boolean getBaselineAlignBottom() {
		return mBaselineAlignBottom;
	}

	/**
	 * Set a tinting option for the image.
	 * 
	 * @param color
	 *           Color tint to apply.
	 * @param mode
	 *           How to apply the color. The standard mode is {@link PorterDuff.Mode#SRC_ATOP}
	 * 
	 * @attr ref android.R.styleable#ImageView_tint
	 */
	public final void setColorFilter( int color, PorterDuff.Mode mode ) {
		setColorFilter( new PorterDuffColorFilter( color, mode ) );
	}

	/**
	 * Set a tinting option for the image. Assumes {@link PorterDuff.Mode#SRC_ATOP} blending mode.
	 * 
	 * @param color
	 *           Color tint to apply.
	 * @attr ref android.R.styleable#ImageView_tint
	 */
	public final void setColorFilter( int color ) {
		setColorFilter( color, PorterDuff.Mode.SRC_ATOP );
	}

	/**
	 * Clear color filter.
	 */
	public final void clearColorFilter() {
		setColorFilter( null );
	}

	/**
	 * Apply an arbitrary colorfilter to the image.
	 * 
	 * @param cf
	 *           the colorfilter to apply (may be null)
	 */
	public void setColorFilter( ColorFilter cf ) {
		if ( mColorFilter != cf ) {
			mColorFilter = cf;
			mColorMod = true;
			applyColorMod();
			invalidate();
		}
	}

	/**
	 * Sets the alpha.
	 *
	 * @param alpha the new alpha
	 */
	public void setAlpha( int alpha ) {
		alpha &= 0xFF; // keep it legal
		if ( mAlpha != alpha ) {
			mAlpha = alpha;
			mColorMod = true;
			applyColorMod();
			invalidate();
		}
	}

	/**
	 * Apply color mod.
	 */
	private void applyColorMod() {
		// Only mutate and apply when modifications have occurred. This should
		// not reset the mColorMod flag, since these filters need to be
		// re-applied if the Drawable is changed.
		if ( mDrawable != null && mColorMod ) {
			mDrawable = mDrawable.mutate();
			mDrawable.setColorFilter( mColorFilter );
			mDrawable.setAlpha( mAlpha * mViewAlphaScale >> 8 );
		}
	}

	/** The m handler. */
	protected Handler mHandler = new Handler();
	
	/** The m rotation. */
	protected int mRotation = 0;
	
	/** The m current scale. */
	protected float mCurrentScale = 0;
	
	/** The m vertical scale. */
	protected float mVerticalScale = 0;
	
	/** The m running. */
	protected boolean mRunning = false;

	/**
	 * Rotate90.
	 *
	 * @param cw the cw
	 * @param durationMs the duration ms
	 */
	public void rotate90( boolean cw, int durationMs ) {
		rotateTo( cw, durationMs );
	}

	/**
	 * Rotate to.
	 *
	 * @param cw the cw
	 * @param durationMs the duration ms
	 */
	protected void rotateTo( final boolean cw, final int durationMs ) {

		if ( mRunning ) {
			return;
		}

		mRunning = true;
		final float destRotation = mRotation + 90;

		Log.i( LOG_TAG, "rotateCW > " + destRotation );

		final long startTime = System.currentTimeMillis();

		final int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
		final int vheight = getHeight() - getPaddingTop() - getPaddingBottom();
		final int centerX = vwidth / 2;
		final int centerY = vheight / 2;

		Log.d( LOG_TAG, "currentScale: " + mCurrentScale );
		Log.d( LOG_TAG, "destScale: " + mVerticalScale );

		mRotateMatrix.setRotate( mRotation, centerX, centerY );
		mRotateMatrix.setScale( mCurrentScale, mCurrentScale, centerX, centerY );

		mHandler.post( new Runnable() {

			@SuppressWarnings("unused")
			float old_scale = 0;
			@SuppressWarnings("unused")
			float old_rotation = 0;

			@Override
			public void run() {
				long now = System.currentTimeMillis();

				float currentMs = Math.min( durationMs, now - startTime );

				float new_rotation = (float) mEasing.easeInOut( currentMs, 0, cw ? 90 : -90, durationMs );
				float new_scale = (float) mEasing.easeInOut( currentMs, 0, ( mVerticalScale - mCurrentScale ), durationMs );

				mRotateMatrix.setScale( mCurrentScale + new_scale, mCurrentScale + new_scale, centerX, centerY );
				mRotateMatrix.postRotate( mRotation + new_rotation, vwidth / 2, vheight / 2 );

				old_scale = new_scale;
				old_rotation = new_rotation;

				invalidate();

				if ( currentMs < durationMs ) {
					mHandler.post( this );
				} else {
					mRotation += cw ? 90 : -90;
					mRotation = mRotation % 360;

					if ( mRotation < 0 ) mRotation = 360 + mRotation;

					float t = mVerticalScale;
					mVerticalScale = mCurrentScale;
					mCurrentScale = t;

					mRotateMatrix.setRotate( mRotation, centerX, centerY );
					mRotateMatrix.postScale( mCurrentScale, mCurrentScale, centerX, centerY );

					invalidate();
					printDetails();

					mRunning = false;

					if ( isReset ) {
						onReset();
					}
				}
			}
		} );
	}

	/**
	 * Prints the details.
	 */
	public void printDetails() {
		Log.i( LOG_TAG, "details:" );
		Log.d( LOG_TAG, "	flip horizontal: "
				+ ( ( mFlipType & FlipType.FLIP_HORIZONTAL.nativeInt ) == FlipType.FLIP_HORIZONTAL.nativeInt ) );
		Log.d( LOG_TAG, "	flip vertical: " + ( ( mFlipType & FlipType.FLIP_VERTICAL.nativeInt ) == FlipType.FLIP_VERTICAL.nativeInt ) );
		Log.d( LOG_TAG, "	rotation: " + mRotation );
		Log.d( LOG_TAG, "--------" );
	}

	/**
	 * Flip.
	 *
	 * @param horizontal the horizontal
	 * @param durationMs the duration ms
	 */
	public void flip( boolean horizontal, int durationMs ) {
		flipTo( horizontal, durationMs );
	}

	/** The m camera enabled. */
	private boolean mCameraEnabled;

	/**
	 * Sets the camera enabled.
	 *
	 * @param value the new camera enabled
	 */
	public void setCameraEnabled( final boolean value ) {
		if ( android.os.Build.VERSION.SDK_INT >= 14 && value )
			mCameraEnabled = value;
		else
			mCameraEnabled = false;
	}

	/**
	 * Flip to.
	 *
	 * @param horizontal the horizontal
	 * @param durationMs the duration ms
	 */
	protected void flipTo( final boolean horizontal, final int durationMs ) {

		if ( mRunning ) {
			return;
		}

		mRunning = true;

		final long startTime = System.currentTimeMillis();
		final int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
		final int vheight = getHeight() - getPaddingTop() - getPaddingBottom();
		final float centerx = vwidth / 2;
		final float centery = vheight / 2;

		final Camera camera = new Camera();

		mHandler.post( new Runnable() {

			@Override
			public void run() {
				long now = System.currentTimeMillis();

				double currentMs = Math.min( durationMs, now - startTime );

				if ( mCameraEnabled ) {
					float degrees = (float) ( 0 + ( ( -180 - 0 ) * ( currentMs / durationMs ) ) );

					camera.save();
					if ( horizontal ) {
						camera.rotateY( degrees );
					} else {
						camera.rotateX( degrees );
					}
					camera.getMatrix( mFlipMatrix );
					camera.restore();
					mFlipMatrix.preTranslate( -centerx, -centery );
					mFlipMatrix.postTranslate( centerx, centery );
				} else {

					double new_scale = mEasing.easeInOut( currentMs, 1, -2, durationMs );
					if ( horizontal )
						mFlipMatrix.setScale( (float) new_scale, 1, centerx, centery );
					else
						mFlipMatrix.setScale( 1, (float) new_scale, centerx, centery );
				}

				invalidate();

				if ( currentMs < durationMs ) {
					mHandler.post( this );
				} else {

					boolean isRotate = mRotation % 180 == 90;

					if ( horizontal ) {
						mFlipType ^= isRotate ? FlipType.FLIP_VERTICAL.nativeInt : FlipType.FLIP_HORIZONTAL.nativeInt;
						if ( mRotation % 180 == 0 )
							mDrawMatrix.postScale( -1, 1, centerx, centery );
						else
							mDrawMatrix.postScale( 1, -1, centerx, centery );
					} else {
						mFlipType ^= isRotate ? FlipType.FLIP_HORIZONTAL.nativeInt : FlipType.FLIP_VERTICAL.nativeInt;
						if ( mRotation % 180 == 0 )
							mDrawMatrix.postScale( 1, -1, centerx, centery );
						else
							mDrawMatrix.postScale( -1, 1, centerx, centery );
					}

					mFlipMatrix.reset();

					invalidate();
					printDetails();

					mRunning = false;

					if ( isReset ) {
						onReset();
					}
				}
			}
		} );
	}

	/** The m matrix values. */
	protected final float[] mMatrixValues = new float[9];

	/**
	 * Gets the value.
	 *
	 * @param matrix the matrix
	 * @param whichValue the which value
	 * @return the value
	 */
	protected float getValue( Matrix matrix, int whichValue ) {
		matrix.getValues( mMatrixValues );
		return mMatrixValues[whichValue];
	}

	/**
	 * Gets the matrix scale.
	 *
	 * @param matrix the matrix
	 * @return the matrix scale
	 */
	protected float[] getMatrixScale( Matrix matrix ) {
		float[] result = new float[2];
		result[0] = getValue( matrix, Matrix.MSCALE_X );
		result[1] = getValue( matrix, Matrix.MSCALE_Y );
		return result;
	}

	/** The m flip type. */
	protected int mFlipType = FlipType.FLIP_NONE.nativeInt;

	/**
	 * The Enum FlipType.
	 */
	public enum FlipType {
		
		/** The FLI p_ none. */
		FLIP_NONE( 1 << 0 ), 
 /** The FLI p_ horizontal. */
 FLIP_HORIZONTAL( 1 << 1 ), 
 /** The FLI p_ vertical. */
 FLIP_VERTICAL( 1 << 2 );

		/**
		 * Instantiates a new flip type.
		 *
		 * @param ni the ni
		 */
		FlipType( int ni ) {
			nativeInt = ni;
		}

		/** The native int. */
		public final int nativeInt;
	}

	/* (non-Javadoc)
	 * @see android.view.View#getRotation()
	 */
	public float getRotation() {
		return mRotation;
	}

	/**
	 * Gets the horizontal flip.
	 *
	 * @return the horizontal flip
	 */
	public boolean getHorizontalFlip() {
		if ( mFlipType != FlipType.FLIP_NONE.nativeInt ) {
			if ( mRotation % 180 == 90 ) {
				return ( mFlipType & FlipType.FLIP_VERTICAL.nativeInt ) == FlipType.FLIP_VERTICAL.nativeInt;
			} else {
				return ( mFlipType & FlipType.FLIP_HORIZONTAL.nativeInt ) == FlipType.FLIP_HORIZONTAL.nativeInt;
			}
		}
		return false;
	}

	/**
	 * Gets the vertical flip.
	 *
	 * @return the vertical flip
	 */
	public boolean getVerticalFlip() {
		if ( mFlipType != FlipType.FLIP_NONE.nativeInt ) {
			if ( mRotation % 180 == 90 ) {
				return ( mFlipType & FlipType.FLIP_HORIZONTAL.nativeInt ) == FlipType.FLIP_HORIZONTAL.nativeInt;
			} else {
				return ( mFlipType & FlipType.FLIP_VERTICAL.nativeInt ) == FlipType.FLIP_VERTICAL.nativeInt;
			}
		}
		return false;
	}

	/**
	 * Gets the flip type.
	 *
	 * @return the flip type
	 */
	public int getFlipType() {
		return mFlipType;
	}

	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning() {
		return mRunning;
	}

	/**
	 * Reset the image to the original state.
	 */
	public void reset() {
		Log.i( LOG_TAG, "reset" );
		isReset = true;
		onReset();
	}

	/**
	 * On reset.
	 */
	private void onReset() {
		if ( isReset ) {
			Log.d( LOG_TAG, "onReset. rotation: " + mRotation + ", hflip: " + getHorizontalFlip() + ", vflip: " + getVerticalFlip() );
			final boolean hflip = getHorizontalFlip();
			final boolean vflip = getVerticalFlip();
			boolean handled = false;

			switch ( mRotation ) {
				case 0:
					if ( hflip ) {
						flip( true, resetAnimTime );
						handled = true;
					} else if ( vflip ) {
						flip( false, resetAnimTime );
						handled = true;
					}
					break;

				case 90:
					rotate90( false, resetAnimTime );
					handled = true;
					break;

				case 180:
					if ( hflip && vflip ) {
						handled = false;
					} else {
						if ( !hflip && !vflip ) {
							flip( false, resetAnimTime );
							handled = true;
						} else if ( vflip ) {
							flip( true, resetAnimTime );
							handled = true;
						} else if ( hflip ) {
							flip( false, resetAnimTime );
							handled = true;
						}
					}
					break;

				case 270:
					rotate90( true, resetAnimTime );
					handled = true;
					break;
			}

			if ( !handled ) {
				fireOnResetComplete();
			}
		}
	}

	/**
	 * Fire on reset complete.
	 */
	private void fireOnResetComplete() {
		Log.w( LOG_TAG, "onResetComplete" );
		if ( mResetListener != null ) {
			mResetListener.onResetComplete();
		}
	}
}
