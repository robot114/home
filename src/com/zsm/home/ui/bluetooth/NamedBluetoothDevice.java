package com.zsm.home.ui.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

public class NamedBluetoothDevice implements Parcelable {

	private BluetoothDevice mDevice;
	private String mAlias;

	public NamedBluetoothDevice( BluetoothDevice device ) {
		this( device, null );
	}

	public String getAlias() {
		return mAlias;
	}

	public void setAlias(String alias) {
		this.mAlias = alias;
	}

	public NamedBluetoothDevice( BluetoothDevice device, String alias ) {
		mAlias = alias;
		mDevice = device;
	}
	
	public BluetoothDevice getDevice() {
		return mDevice;
	}

	@Override
	public String toString() {
		if( mDevice == null ) {
			return "None";
		}
		
		StringBuilder b = new StringBuilder();
		String name = mDevice.getName();
		
		String last = " )";
		if( name != null ) {
			b.append( name ).append( "( " );
		} else if( mAlias != null ) {
			b.append( mAlias ).append( "( " );
		} else {
			last = "";
		}
		
		b.append( mDevice.getAddress() ).append( last );
		
		return b.toString();
	}

	@Override
	public boolean equals(Object o) {
		if( o instanceof NamedBluetoothDevice ) {
			return mDevice.equals( ((NamedBluetoothDevice)o).getDevice() );
		}
		return false;
	}

	@Override
	public int describeContents() {
		return mDevice.describeContents();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		mDevice.writeToParcel(dest, flags);
	}

    public static final Parcelable.Creator<NamedBluetoothDevice> CREATOR =
            new Parcelable.Creator<NamedBluetoothDevice>() {
    	
        public NamedBluetoothDevice createFromParcel(Parcel in) {
        	BluetoothDevice device = BluetoothDevice.CREATOR.createFromParcel(in);
            return new NamedBluetoothDevice( device );
        }
        
        public NamedBluetoothDevice[] newArray(int size) {
            return new NamedBluetoothDevice[size];
        }
        
    };

}
