package com.zsm.android.beacon;

import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;

import com.zsm.android.bluetooth.BluetoothUtility;

class BleBeaconScanner implements Scanner{

	private BluetoothLeScanner mScanner;
	private BluetoothAdapter mBluetoothAdapter;

	public BleBeaconScanner( Context context ) {
		mBluetoothAdapter = BluetoothUtility.getAdapter(context);
		mScanner = mBluetoothAdapter.getBluetoothLeScanner();
	}
	
	@Override
	public void startScan( BeaconOperator o ) {
		BleBeaconOperator bo = (BleBeaconOperator)o;
		List<ScanFilter> filters = bo.getAndroidFilter();
		ScanSettings settings = bo.getAndroidSettings();
		if( filters != null && settings != null && filters.size() > 0 ) {
			mScanner.startScan(filters, settings, bo.getBleScanCallback() );
		} else {
			mScanner.startScan( bo.getBleScanCallback() );
		}
	}

	@Override
	public void stopScan( BeaconOperator o ) {
		mScanner.stopScan( ((BleBeaconOperator)o).getBleScanCallback() );
	}

}
