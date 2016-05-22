package com.zsm.android.beacon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;

import com.zsm.android.beacon.BeaconScanner.Callback;
import com.zsm.android.beacon.BeaconScanner.STOP_TYPE;

public class BeaconOperator {
	
	private Handler mHandler;
	private Callback mCallback;
	private int mScanPeriod;
	private int mBackgroundTimes;
	private int mBackgroundInterval;
	
	private BluetoothLeScanner mScanner;
	
	private ScanCallback mBleScanCallback;
	protected boolean mScanning;
	private Set<BluetoothDevice> mBeacons;
	protected int mScanSettings;
	private Runnable mBackgroundRunner;
	protected int mBackgroundCalledTimes;
	private List<ScanFilter> mFilterList;
	private ScanSettings mAndroidScanSettings;

	public BeaconOperator( Callback cb, int scanPeriod, Handler handler,
						   int backgroundTimes ) {
		
		mHandler = handler;
		mCallback = cb;
		mScanPeriod = scanPeriod;
		mBackgroundTimes = backgroundTimes;
		
		int times = Math.max( backgroundTimes, 1 );
		mBackgroundInterval = mScanPeriod/times;
		
		mBeacons = Collections.synchronizedSet( new HashSet<BluetoothDevice>() );
		mFilterList = new ArrayList<ScanFilter>();
		mAndroidScanSettings = new ScanSettings.Builder().build();
		
		mBleScanCallback = getBleScanCallback();
		mBackgroundRunner = getBackgroundRunner();
	}

	void setScanner( BluetoothLeScanner scanner ) {
		mScanner = scanner;
	}
	
	ScanCallback getBleScanCallback() {
		if( mBleScanCallback != null ) {
			return mBleScanCallback;
		}
		
		mBleScanCallback = new ScanCallback() {
			
			@Override
			public void onScanResult(int callbackType, ScanResult result) {
				if( mScanning ) {
					BluetoothDevice device = result.getDevice();
					if( !mBeacons.contains( device ) ) {
						mBeacons.add(device);
						mCallback.newBeaconFound( device );
						if( shouldStopBySettings() ) {
							stopScan( STOP_TYPE.FOUND );
						}
					}
				}
			}

			private boolean shouldStopBySettings(){
				return mScanSettings == BeaconScanner.CALLBACK_TYPE_FIRST_MATCH;
			}
		};
		
		return mBleScanCallback;
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
	
	public void startScan() {
		stopCurrentScan();
		
		mScanSettings = BeaconScanner.CALLBACK_TYPE_ALL_MATCHES;
		mBeacons.clear();
		
		mScanning = true;
		mScanner.startScan(mBleScanCallback);
		
		mBackgroundCalledTimes = 0;
		mHandler.postDelayed( mBackgroundRunner, mBackgroundInterval);
	}

	public void startScan( BluetoothDevice device ) {
		ScanFilter filter 
			= new ScanFilter.Builder().setDeviceAddress( device.getAddress() ).build();
		startScan( filter, BeaconScanner.CALLBACK_TYPE_FIRST_MATCH );
	}
	
	public void startScan( ScanFilter filter, int settings ) {
		stopCurrentScan();
		mFilterList.clear();
		mFilterList.add( filter );
		mScanSettings = settings;
		
		startScanWithFilter();
	}

	public void startScan( List<ScanFilter> filters, int settings ) {
		stopCurrentScan();
		mFilterList.clear();
		mFilterList.addAll( filters );
		mScanSettings = settings;
		
		startScanWithFilter();
	}

	private void stopCurrentScan() {
		if( mScanning ) {
			stopScan( STOP_TYPE.CANCELLED );
		}
	}

	private void startScanWithFilter() {
		mBeacons.clear();
		
		mScanning = true;
		mScanner.startScan(mFilterList, mAndroidScanSettings, mBleScanCallback);
		
		mBackgroundCalledTimes = 0;
		mHandler.postDelayed( mBackgroundRunner, mBackgroundInterval);
	}

	void stopScan(STOP_TYPE type) {
		if( mScanning ) {
			mScanning = false;
			mHandler.removeCallbacksAndMessages(null);
			mScanner.stopScan(mBleScanCallback);
			mCallback.stopped( type );
		}
	}
	
}
