package com.zsm.android.beacon;

import android.content.Context;

enum ScannerFactory {

	instance;
	
	private Scanner mBleScanner;
	private WifiBeaconScanner mWifiScanner;

	private ScannerFactory( ) {
	}
	
	static ScannerFactory getInstance() {
		return instance;
	}
	
	Scanner getScanner( Context context, BeaconOperator.PROTOCOL protocol ) {
		Scanner s;
		switch( protocol ) {
			case BLUETOOTH:
				s = createBleScanner( context ); 
				break;
			case WIFI:
				s = createWifiScanner( context );
				break;
			default:
				throw new IllegalArgumentException( "Ivalid protocol: " + protocol );
		}
		
		return s;
	}
	
	private Scanner createBleScanner(Context context) {
		if( mBleScanner == null ) {
			mBleScanner = new BleBeaconScanner( context );
		}
		
		return mBleScanner;
	}

	private WifiBeaconScanner createWifiScanner(Context context) {
		if( mWifiScanner == null ) {
			mWifiScanner = new WifiBeaconScanner( context );
		}
		
		return mWifiScanner;
	}
}
