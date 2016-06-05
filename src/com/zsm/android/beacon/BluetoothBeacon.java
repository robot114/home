package com.zsm.android.beacon;

import com.zsm.android.beacon.BeaconOperator.PROTOCOL;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

public class BluetoothBeacon extends BeaconDevice implements Parcelable {

	private BluetoothDevice mDevice;
	
	public BluetoothBeacon( BluetoothDevice device ) {
		this( device, null );
	}

	public BluetoothBeacon( BluetoothDevice device, String alias ) {
		super( alias );
		mDevice = device;
	}
	
	public BluetoothDevice getDevice() {
		return mDevice;
	}

	@Override
	public
	String getName() {
		return mDevice == null ? null : mDevice.getName();
	}

	@Override
	public
	String getAddress() {
		return mDevice == null ? null : mDevice.getAddress();
	}

	@Override
	public
	PROTOCOL getProtocol() {
		return PROTOCOL.BLUETOOTH;
	}
    
	@Override
	public boolean equals(Object o) {
		if( o instanceof BluetoothBeacon ) {
			return mDevice.equals( ((BluetoothBeacon)o).getDevice() );
		}
		return false;
	}

	@Override
	public int hashCode() {
		return mDevice.hashCode();
	}

	@Override
	public int describeContents() {
		return mDevice.describeContents();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		mDevice.writeToParcel(dest, flags);
	}

    public static final Parcelable.Creator<BluetoothBeacon> CREATOR =
            new Parcelable.Creator<BluetoothBeacon>() {
    	
        public BluetoothBeacon createFromParcel(Parcel in) {
        	BluetoothDevice device = BluetoothDevice.CREATOR.createFromParcel(in);
            return new BluetoothBeacon( device );
        }
        
        public BluetoothBeacon[] newArray(int size) {
            return new BluetoothBeacon[size];
        }
        
    };
}
