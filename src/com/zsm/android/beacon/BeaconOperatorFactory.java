package com.zsm.android.beacon;

import com.zsm.android.beacon.BeaconOperator.PROTOCOL;
import com.zsm.home.app.HomeApplication;

import android.content.Context;
import android.os.Handler;

public class BeaconOperatorFactory {

	public static BeaconOperator createOperator( Context context, BeaconOperator.PROTOCOL protocol, 
												 BeaconOperator.Callback cb, int scanPeriod,
												 Handler handler, int backgroundTimes ) {
		
		switch( protocol ) {
			case BLUETOOTH:
				return new BleBeaconOperator(context, cb, scanPeriod, handler, backgroundTimes);
			case WIFI:
				return new WifiBeaconOperator(context, cb, scanPeriod, handler, backgroundTimes);
			default:
				throw new IllegalArgumentException( "Unsupported protocol: " + protocol );
		}
	}

	public static BeaconOperator createOperator( Context context, BeaconOperator.PROTOCOL protocol, 
			 									 BeaconOperator.Callback cb ) {
		return createOperator( context, protocol, cb, HomeApplication.DEVICE_SCAN_PRIOD,
							   new Handler(), 0 );
	}
	
	static BeaconScanFilter createScanFilter( BeaconDevice device ) {
		switch( device.getProtocol() ) {
			case WIFI:
				return new WifiBeaconScanFilter( device );
			case BLUETOOTH:
			default:
				throw new IllegalArgumentException( "Unsupported protocol: " + device.getProtocol() );
		}
	}
	
	static BeaconScanFilter createScanFilter( String name, String address, PROTOCOL protocol ) {
		switch( protocol ) {
			case WIFI:
				return new WifiBeaconScanFilter( name, address );
			case BLUETOOTH:
			default:
				throw new IllegalArgumentException( "Unsupported protocol: " + protocol );
		}
	}
}
