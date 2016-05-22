package com.zsm.android.beacon;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.os.Handler;

import com.zsm.android.bluetooth.BluetoothUtility;
import com.zsm.home.app.HomeApplication;

public class BeaconScanner {
	
	public enum STOP_TYPE {
		TIMEOUT, FOUND, CANCELLED
	};
	
	public interface Callback {
		public void newBeaconFound(BluetoothDevice device);
		public void backgroundFunction(int backgroundCalledTimes);
		public void stopped(STOP_TYPE type );
	}
	
	public final static int CALLBACK_TYPE_ALL_MATCHES = 0x1;
	public final static int CALLBACK_TYPE_FIRST_MATCH = 0x2;
	
	private Context mContext;
	
	private BluetoothAdapter mBluetoothAdapter;
	private Hashtable< String, BeaconOperator >  mOperators;
	private BluetoothLeScanner mBleScanner;
	
	public BeaconScanner( Context context ) {
		
		mContext = context;
		mOperators = new Hashtable<String, BeaconOperator>();
		init();
	}

	private void init() {
		mBluetoothAdapter = BluetoothUtility.getAdapter(mContext);
		mBleScanner = mBluetoothAdapter.getBluetoothLeScanner();
	}
	
	public BeaconOperator newOperator( String scanName, Callback cb ) {
		BeaconOperator o
			= new BeaconOperator(cb, HomeApplication.BLUETOOTH_SCAN_PRIOD,
								 new Handler(), 0 );
		o.setScanner( mBleScanner );
		mOperators.put(scanName, o);
		return o;
	}

	public BeaconOperator newOperator( String scanName, Callback cb, int scanPeriod,
		  	  						   Handler handler, int backgroundTimes ) {
		BeaconOperator o
			= new BeaconOperator(cb, scanPeriod, handler, backgroundTimes);
		o.setScanner( mBleScanner );
		mOperators.put(scanName, o);
		return o;
	}

	public void startScan( String scanName ) {
		BeaconOperator o = getOperator(scanName);
		if( o != null ) {
			o.startScan();
		}
	}

	public void startScan( String scanName, BluetoothDevice device ) {
		ScanFilter filter 
			= new ScanFilter.Builder().setDeviceAddress( device.getAddress() ).build();
		startScan( scanName, filter, CALLBACK_TYPE_FIRST_MATCH );
	}
	
	public void startScan( String scanName, ScanFilter filter, int settings ) {
		
		BeaconOperator o = getOperator(scanName);
		if( o != null ) {
			o.startScan( filter, settings );
		}
	}

	public void startScan( String scanName, List<ScanFilter> filters,
						   int settings ) {
		
		BeaconOperator o = getOperator(scanName);
		if( o != null ) {
			o.startScan( filters, settings );
		}
	}

	public void stopScan( String scanName ) {
		stopScan(scanName, STOP_TYPE.CANCELLED);
	}

	public void stopScan(String scanName, STOP_TYPE type) {
		BeaconOperator o = getOperator( scanName );
		if( o != null ) {
			o.stopScan( type );
		}
	}
	
	public void stopAll() {
		Enumeration<BeaconOperator> ops = mOperators.elements();
		
		while( ops.hasMoreElements() ) {
			BeaconOperator op = ops.nextElement();
			op.stopScan( STOP_TYPE.CANCELLED );
		}
	}

	private BeaconOperator getOperator(String scanName) {
		return mOperators.get(scanName);
	}
}
