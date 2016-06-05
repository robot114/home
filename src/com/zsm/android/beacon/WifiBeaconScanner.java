package com.zsm.android.beacon;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zsm.android.beacon.BeaconOperator.STOP_TYPE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

class WifiBeaconScanner implements Scanner {

	private static final IntentFilter SCAN_FILTER
		= new IntentFilter( WifiManager.SCAN_RESULTS_AVAILABLE_ACTION );

	private Context mContext;
	
	private WifiManager mWifiManager;
	private BroadcastReceiver mScanReceiver;
	private boolean mScanning;
	
	private Set<WifiBeaconOperator> mOperators;
	
	WifiBeaconScanner( Context context ) {
		mContext = context;
		mWifiManager = (WifiManager)context.getSystemService( Context.WIFI_SERVICE );
		mOperators = new HashSet<WifiBeaconOperator>();
		mScanReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				handleScanResults();
			}
		};
	}

	synchronized public void startScan(BeaconOperator o) {
		mOperators.add( (WifiBeaconOperator)o );
		if( !mScanning ) {
			mContext.registerReceiver(mScanReceiver, SCAN_FILTER, null,
									  o.getHandler() );
			mScanning = mWifiManager.startScan();
			mScanning = true;
		}
	}

	synchronized public void stopScan(BeaconOperator o) {
		if( mOperators.size() == 1 && mOperators.contains(o) ) {
			mScanning = false;
			mContext.unregisterReceiver(mScanReceiver);
		}
		mOperators.remove(o);
	}

	synchronized private void handleScanResults() {
		List<ScanResult> results = mWifiManager.getScanResults();
		int size = mOperators.size();
		BeaconOperator os[] = new BeaconOperator[ size ];
		mOperators.toArray( os );
		for( ScanResult result : results ) {
			WifiBeacon b = new WifiBeacon( result );
			for( int i = 0; i < size; i++ ) {
				if( mOperators.contains(os[i]) && os[i].matchScanFilter( b ) ) {
					// For the operator just need the first device,
					// the scan will be stopped after the operator
					// is notified. And the operator will be removed
					// from the operator set. So it is unnecessary
					// to deal with this.
					os[i].deviceFound(b);
				}
			}
		}
		
		size = mOperators.size();
		os = new BeaconOperator[ size ];
		mOperators.toArray( os );
		for( int i = 0; i < size; i++ ) {
			os[i].stopScan( STOP_TYPE.TIMEOUT );
		}
	}
	
}
