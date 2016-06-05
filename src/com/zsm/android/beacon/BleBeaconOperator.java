package com.zsm.android.beacon;

import java.util.ArrayList;
import java.util.List;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;

public class BleBeaconOperator extends BeaconOperator {
	
	private List<ScanFilter> mFilterList;
	private ScanSettings mAndroidScanSettings;
	private ScanCallback mBleScanCallback;
	
	BleBeaconOperator( Context context, BeaconOperator.Callback cb,
					   int scanPeriod, Handler handler, int backgroundTimes ) {
		
		super( context, PROTOCOL.BLUETOOTH, cb, scanPeriod, handler, backgroundTimes );
		
		mFilterList = new ArrayList<ScanFilter>();
		mAndroidScanSettings = new ScanSettings.Builder().build();
		
		mBleScanCallback = getBleScanCallback();
	}

	ScanCallback getBleScanCallback() {
		if( mBleScanCallback != null ) {
			return mBleScanCallback;
		}
		
		mBleScanCallback = new ScanCallback() {
			
			@Override
			public void onScanResult(int callbackType, ScanResult result) {
				deviceFound(new BluetoothBeacon( result.getDevice() ));
			}
		};
		
		return mBleScanCallback;
	}

	@Override
	public void setFilter( BeaconDevice device ) {
		ScanFilter filter 
			= new ScanFilter.Builder().setDeviceAddress( device.getAddress() ).build();
		mFilterList.clear();
		mFilterList.add( filter );
		mScanSettings = BeaconOperator.CALLBACK_TYPE_FIRST_MATCH;
	}
	
	@Override
	public void setFilter( BeaconScanFilter filter, int settings ) {
		mFilterList.clear();
		mFilterList.add( toBleScanFilter( filter ) );
		mScanSettings = settings;
	}

	@Override
	public void setFilter( List<BeaconScanFilter> filters, int settings ) {
		mFilterList.clear();
		for( BeaconScanFilter bsf : filters ) {
			mFilterList.add( toBleScanFilter( bsf ) );
		}
		mScanSettings = settings;
	}

	@Override
	protected List<BeaconScanFilter> getScanFilters() {
		throw new UnsupportedOperationException(
					"This method is not supported by BleBeaconOperator!" );
	}

	@Override
	public boolean matchScanFilter(BeaconDevice device) {
		if( !( device instanceof BluetoothBeacon ) ) {
			return false;
		}
		
		if( mFilterList == null || mFilterList.size() == 0 ) {
			return true;
		}
		
		ScanResult sr
			= new ScanResult( ((BluetoothBeacon)device).getDevice(), null, 0, 0 );
		for( ScanFilter filter : mFilterList ) {
			if( filter.matches( sr ) ) {
				return true;
			}
		}
		
		return false;
	}
	
	private ScanFilter toBleScanFilter( BeaconScanFilter filter ) {
		ScanFilter bleScanFilter
			= new ScanFilter.Builder()
					.setDeviceAddress( filter.getAddress() )
					.setDeviceName( filter.getDeviceName() )
					.build();
		
		return bleScanFilter;
	}
	
	List<ScanFilter> getAndroidFilter() {
		return mFilterList;
	}
	
	ScanSettings getAndroidSettings() {
		return mAndroidScanSettings;
	}
}
