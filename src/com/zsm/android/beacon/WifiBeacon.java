package com.zsm.android.beacon;

import com.zsm.android.beacon.BeaconOperator.PROTOCOL;

import android.net.wifi.ScanResult;
import android.os.Parcel;
import android.os.Parcelable;

public class WifiBeacon extends BeaconDevice implements Parcelable {

	private String mSsid;		// Name of network
	private String mBssid;

	public WifiBeacon( String alias, String SSID, String BSSID ) {
		super( alias );
		mSsid = SSID;
		mBssid = BSSID;
	}
	
	public WifiBeacon( ScanResult sr ) {
		super( null );
		mSsid = sr.SSID;
		mBssid = sr.BSSID;
	}
	
	@Override
	public
	String getName() {
		return mSsid;
	}

	@Override
	public String getAddress() {
		return mBssid;
	}

	@Override
	public
	PROTOCOL getProtocol() {
		return PROTOCOL.WIFI;
	}
    
	@Override
	public boolean equals(Object o) {
		if( o instanceof WifiBeacon ) {
			WifiBeacon wb = (WifiBeacon)o;
			if( wb.mBssid != null ) {
				// Same address same device
				return wb.mBssid.equalsIgnoreCase( mBssid );
			} else {
				return mBssid == null;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return mBssid.hashCode();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString( mAlias );
		dest.writeString( mSsid );
		dest.writeString( mBssid );
	}

    public static final Parcelable.Creator<WifiBeacon> CREATOR =
            new Parcelable.Creator<WifiBeacon>() {
    	
        public WifiBeacon createFromParcel(Parcel in) {
        	String alias = in.readString();
        	String ssid = in.readString();
        	String bssid = in.readString();
        	
            return new WifiBeacon( alias, ssid, bssid );
        }
        
        public WifiBeacon[] newArray(int size) {
            return new WifiBeacon[size];
        }
        
    };
}
