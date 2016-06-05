package com.zsm.android.beacon;

public abstract class BeaconScanFilter {

	private String mDeviceName;
	private String mAddress;

	public BeaconScanFilter( String name, String address ) {
		mDeviceName = name;
		mAddress = address;
		if( !checkAddress( address ) ) {
			throw new IllegalArgumentException( "Invalid address: " + address );
		}
	}
	
	public BeaconScanFilter( BeaconDevice device ) {
		this( device.getName(), device.getAddress() );
	}
	
	abstract boolean checkAddress( String address );
	
	public boolean matches( BeaconDevice device ) {
		if( mDeviceName != null && !mDeviceName.equals(device.getName()) ) {
			return false;
		}
		
		if( mAddress != null && !mAddress.equals(device.getAddress()) ) {
			return false;
		}
		
		return true;
	}

	public String getDeviceName() {
		return mDeviceName;
	}

	public String getAddress() {
		return mAddress;
	}
}
