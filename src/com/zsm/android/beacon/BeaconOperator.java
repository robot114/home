package com.zsm.android.beacon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.os.Handler;

public abstract class BeaconOperator {

	public enum STOP_TYPE {
		TIMEOUT, FOUND, CANCELLED
	}

	public final static int CALLBACK_TYPE_FIRST_MATCH = 0x2;
	public final static int CALLBACK_TYPE_ALL_MATCHES = 0x1;
	
	public interface Callback {
		public void newBeaconFound(BeaconDevice device);
		public void backgroundFunction(int backgroundCalledTimes);
		public void stopped(STOP_TYPE type );
	}

	public enum PROTOCOL {
		BLUETOOTH, WIFI
	}

	private Context mContext;
	private PROTOCOL mProtocol;
	private Scanner mScanner;
	
	private Handler mHandler;
	private Callback mCallback;
	private int mScanPeriod;
	private int mBackgroundTimes;
	private int mBackgroundInterval;
	
	private boolean mScanning;
	private Set<BeaconDevice> mBeacons;
	private Runnable mBackgroundRunner;
	protected int mBackgroundCalledTimes;
	
	private List<BeaconScanFilter> mScanFilters;
	protected int mScanSettings;
	
	public BeaconOperator( Context context, PROTOCOL protocol,
						   Callback cb, int scanPeriod,
						   Handler handler, int backgroundTimes ) {
		
		mContext = context;
		mProtocol = protocol;
		mHandler = handler;
		mCallback = cb;
		mScanPeriod = scanPeriod;
		mBackgroundTimes = backgroundTimes;
		
		int times = Math.max( backgroundTimes, 1 );
		mBackgroundInterval = mScanPeriod/times;
		
		mBeacons = Collections.synchronizedSet( new HashSet<BeaconDevice>() );
		mBackgroundRunner = getBackgroundRunner();
		
		initScanner(protocol);
	}

	protected void initScanner( PROTOCOL protocol ) {
		mScanner
			= (Scanner)ScannerFactory.getInstance().getScanner(getContext(), protocol );
	}

	protected Context getContext( ) {
		return mContext;
	}

	private Runnable getBackgroundRunner() {
		if( mBackgroundRunner != null ) {
			return mBackgroundRunner;
		}
		
		mBackgroundRunner = new Runnable() {
			@Override
			public void run() {
				mBackgroundCalledTimes++;
				if( mBackgroundCalledTimes >= mBackgroundTimes || !mScanning ) {
					STOP_TYPE type
						= mBackgroundCalledTimes < mBackgroundTimes 
							? STOP_TYPE.CANCELLED : STOP_TYPE.TIMEOUT;
					
					stopScan( type );
				} else {
					mCallback.backgroundFunction( mBackgroundCalledTimes );
					mHandler.postDelayed( mBackgroundRunner, mBackgroundInterval);
				}
			}
		};
		
		return mBackgroundRunner;
	}

	Handler getHandler() {
		return mHandler;
	}
	
	protected Scanner getScanner() {
		return mScanner;
	}

	public void startScan() {
		stopCurrentScan();
		
		mScanSettings = CALLBACK_TYPE_ALL_MATCHES;
		startScanInner();
	}

	private void startScanInner() {
		mBeacons.clear();
		
		mScanning = true;
		mScanner.startScan( this );
		
		mBackgroundCalledTimes = 0;
		mHandler.postDelayed( mBackgroundRunner, mBackgroundInterval);
	}

	public void startScan( BeaconDevice device ) {
		stopCurrentScan();
		setFilter( device );
		startScanInner();
	}
	
	public void startScan( BeaconScanFilter filter, int settings ) {
		stopCurrentScan();
		setFilter( filter, settings );
		startScanInner();
	}

	public void startScan( List<BeaconScanFilter> filters, int settings ) {
		stopCurrentScan();
		setFilter( filters, settings );
		startScanInner();
	}

	private void stopCurrentScan() {
		if( mScanning ) {
			stopScan( STOP_TYPE.CANCELLED );
		}
	}

	public void stopScan(STOP_TYPE type) {
		if( mScanning ) {
			mScanning = false;
			mHandler.removeCallbacksAndMessages(null);
			mScanner.stopScan( this );
			mCallback.stopped( type );
		}
	}
	
	public boolean isScanning() {
		return mScanning;
	}

	/**
	 * Invoked when a device found by the scanner. The device may be found before
	 * in this scan cycle.
	 * 
	 * @param device which device found
	 * @return true, the scan task should continue for this operator; false, the
	 * 			 scan task need to be stopped.
	 */
	protected boolean deviceFound(BeaconDevice device) {
		if( isScanning() && !mBeacons.contains( device ) ) {
			mBeacons.add(device);
			mCallback.newBeaconFound( device );
			if( shouldStopBySettings() ) {
				stopScan( STOP_TYPE.FOUND );
				return false;
			}
		}
		
		return true;
	}

	boolean shouldStopBySettings(){
		return mScanSettings == CALLBACK_TYPE_FIRST_MATCH;
	}

	protected void setFilter(BeaconDevice device) {
		prepareFilterList();
		mScanFilters.add( BeaconOperatorFactory.createScanFilter(device) );
		mScanSettings = CALLBACK_TYPE_FIRST_MATCH;
	}

	protected void setFilter(BeaconScanFilter filter, int settings) {
		prepareFilterList();
		mScanFilters.add( filter );
		mScanSettings = settings;
	}

	private void prepareFilterList() {
		if( mScanFilters == null ) {
			mScanFilters = new ArrayList<BeaconScanFilter>();
		} else {
			mScanFilters.clear();
		}
	}

	protected void setFilter(List<BeaconScanFilter> filters, int settings) {
		mScanFilters = filters;
		mScanSettings = settings;
	}

	protected List<BeaconScanFilter> getScanFilters() {
		return mScanFilters;
	}

	public boolean matchScanFilter(BeaconDevice device) {
		List<BeaconScanFilter> filters = getScanFilters();
		if( getScanFilters() == null || filters.size() == 0 ) {
			return true;
		}
		
		for( BeaconScanFilter filter : filters ) {
			if( filter.matches( device ) ) {
				return true;
			}
		}
		
		return false;
	}
}
