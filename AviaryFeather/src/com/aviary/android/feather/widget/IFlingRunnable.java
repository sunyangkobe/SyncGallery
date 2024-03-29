package com.aviary.android.feather.widget;

abstract class IFlingRunnable implements Runnable {
	
	public static interface FlingRunnableView
	{
		boolean removeCallbacks(Runnable action);
		boolean post(Runnable action);
		void scrollIntoSlots();
		void trackMotionScroll( int newX );
		int getMinX();
		int getMaxX();
	}

	int mLastFlingX;
	boolean mShouldStopFling;
	FlingRunnableView mParent;
	int mAnimationDuration;
	
	
	public IFlingRunnable( FlingRunnableView parent, int animationDuration ) {
		mParent = parent;
		mAnimationDuration = animationDuration;
	}

	protected void startCommon() {
		mParent.removeCallbacks( this );
	}

	public void stop( boolean scrollIntoSlots ) {
		mParent.removeCallbacks( this );
		endFling( scrollIntoSlots );
	}

	public void startUsingDistance( int initialX, int distance ) {
		if ( distance == 0 ) return;
		startCommon();
		mLastFlingX = initialX;
		_startUsingDistance( mLastFlingX, distance );
		mParent.post( this );
	}

	public void startUsingVelocity( int initialX, int initialVelocity ) {
		if ( initialVelocity == 0 ) return;
		startCommon();
		mLastFlingX = initialX;
		_startUsingVelocity( mLastFlingX, initialVelocity );
		mParent.post( this );
	}

	protected void endFling( boolean scrollIntoSlots ) {
		forceFinished( true );

		if ( scrollIntoSlots ) {
			mParent.scrollIntoSlots();
		}
	}

	@Override
	public void run() {
		mShouldStopFling = false;

		final boolean more = computeScrollOffset();
		int x = getCurrX();

		mParent.trackMotionScroll( x );

		if ( more && !mShouldStopFling ) {
			mLastFlingX = x;
			mParent.post( this );
		} else {
			endFling( true );
		}
	}

	protected abstract boolean computeScrollOffset();

	protected abstract int getCurrX();

	protected abstract void forceFinished( boolean finished );

	protected abstract void _startUsingVelocity( int initialX, int velocity );

	protected abstract void _startUsingDistance( int initialX, int distance );

	public abstract boolean isFinished();
}
