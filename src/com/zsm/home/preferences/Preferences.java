package com.zsm.home.preferences;

import com.zsm.android.beacon.BluetoothBeacon;
import com.zsm.android.beacon.WifiBeacon;
import com.zsm.log.Log;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

public class Preferences {

	private static final String KEY_HOME_BLUETOOTH_ADDRESS = "HOME_BLUETOOTH_ADDRESS";
	private static final String KEY_HOME_BLUETOOTH_ALIAS = "HOME_BLUETOOTH_ALIAS";
	private static final String KEY_HOME_WIFI_ADDRESS = "HOME_WIFI_BSSID";
	private static final String KEY_HOME_WIFI_ALIAS = "HOME_WIFI_ALIAS";
	private static final String KEY_HOME_WIFI_NAME = "HOME_WIFI_SSID";
	static final String KEY_PROXIMITY_ALERT_ON = "PROXIMITY_ALERT_ON";
	static final String KEY_PROXIMITY_DISTANCE = "PROXIMITY_DISTANCE";

	private static final String KEY_HOME_LOCATION_TIME = "HOME_LOCATION_TIME";

	private static final String KEY_HOME_LOCATION_PROVIDER = "HOME_LOCATION_PROVIDER";

	private static final String KEY_HOME_LOCATION_HAS_ALTITUDE = "HOME_LOCATION_HAS_ALTITUDE";

	private static final String KEY_HOME_LOCATION_ALTITUDE = "HOME_LOCATION_ALTITUDE";

	private static final String KEY_HOME_LOCATION_LONGITUDE = "HOME_LOCATION_LONGITUDE";

	private static final String KEY_HOME_LOCATION_LATITUDE = "HOME_LOCATION_LATITUDE";
	
	private static final String KEY_HOME_ADDRESS = "HOME_ADDRESS";

	static private Preferences instance;
	
	final private SharedPreferences preferences;
	
	private StackTraceElement[] stackTrace;
	
	private Preferences( Context context ) {
		preferences
			= PreferenceManager
				.getDefaultSharedPreferences( context );
		
	}
	
	static public void init( Context c ) {
		if( instance != null ) {
			throw new IllegalStateException( "Preference has been initialized! "
											 + "Call getInitStackTrace() to get "
											 + "the initlization place." );
		}
		instance = new Preferences( c );
		instance.stackTrace = Thread.currentThread().getStackTrace();
	}
	
	static public Preferences getInstance() {
		return instance;
	}
	
	public StackTraceElement[] getInitStackTrace() {
		return stackTrace;
	}
	
	public void setHomeLocation( Location l ) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putFloat(KEY_HOME_LOCATION_LATITUDE, (float) l.getLatitude());
		editor.putFloat(KEY_HOME_LOCATION_LONGITUDE, (float) l.getLongitude());
		editor.putBoolean(KEY_HOME_LOCATION_HAS_ALTITUDE, l.hasAltitude());
		if( l.hasAltitude() ) {
			editor.putFloat(KEY_HOME_LOCATION_ALTITUDE, (float) l.getAltitude());
		} else {
			editor.remove(KEY_HOME_LOCATION_ALTITUDE);
		}
		editor.putLong(KEY_HOME_LOCATION_TIME, l.getTime());
		editor.putString(KEY_HOME_LOCATION_PROVIDER, l.getProvider());
		editor.commit();
		Log.d( "Put home location to preferences.", l );
	}
	
	/**
	 * Get the location of home from the preferences.
	 * 
	 * @return location of home, null if it has not been set.
	 */
	public Location getHomeLocation() {
		float latitude = preferences.getFloat( KEY_HOME_LOCATION_LATITUDE, Float.NaN );
		float longitude = preferences.getFloat( KEY_HOME_LOCATION_LONGITUDE, Float.NaN );
		Log.d( "Home location from pereferences.", "Lat", latitude, "lng", longitude );
		
		if( Float.isNaN(latitude) || Float.isNaN(longitude) 
			|| checkLocation(latitude, longitude) ) {
			
			return null;
		}
		
		Location l = new Location( (String)"?" );
		l.setLatitude( latitude );
		l.setLongitude( longitude );
		
		float altitude = preferences.getFloat( KEY_HOME_LOCATION_ALTITUDE, Float.NaN );
		if( !Float.isNaN( altitude )
			&& preferences.getBoolean( KEY_HOME_LOCATION_HAS_ALTITUDE, false) ) {
			
			l.setAltitude(altitude);
		}
		
		String provider = preferences.getString( KEY_HOME_LOCATION_PROVIDER, "?" );
		l.setProvider(provider);
		long time = preferences.getLong( KEY_HOME_LOCATION_TIME,
										 System.currentTimeMillis() );
		l.setTime(time);
		
		return l;
	}

	private boolean checkLocation(float latitude, float longitude) {
		return latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180;
	}

	public String getHomeAddress() {
		return preferences.getString( KEY_HOME_ADDRESS, "" );
	}
	
	public void setHomeAddress(String address) {
		preferences.edit()
				   .putString( KEY_HOME_ADDRESS, address )
				   .commit();
	}

	public boolean isProximityAlertOn() {
		return preferences.getBoolean( KEY_PROXIMITY_ALERT_ON, true );
	}
	
	public void setProximityAlertOn( boolean on ) {
		preferences.edit().putBoolean( KEY_PROXIMITY_ALERT_ON, on ).commit();
	}

	public float getHomeProximityDistance() {
		String value = preferences.getString( KEY_PROXIMITY_DISTANCE, "10" );
		try {
			return Float.valueOf( value );
		} catch( Exception e ) {
			Log.i( "Value of " + KEY_PROXIMITY_DISTANCE
					+ " is illegal. Default distance returned." );
			return 10.f;
		}
	}

	public void setHomeBluetooth(BluetoothBeacon device) {
		preferences
			.edit()
			.putString( KEY_HOME_BLUETOOTH_ALIAS, device.getAlias() )
			.putString( KEY_HOME_BLUETOOTH_ADDRESS, device.getAddress() )
			.commit();
	}
	
	public BluetoothBeacon getHomeBluetoothDevice() {
		String address = preferences.getString( KEY_HOME_BLUETOOTH_ADDRESS, null );
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		BluetoothDevice phyDevice = null;
		if( address == null 
			|| (phyDevice = adapter.getRemoteDevice(address) ) == null ) {
			
			return null;
		}
		String alias = preferences.getString( KEY_HOME_BLUETOOTH_ALIAS, null );
		return new BluetoothBeacon( phyDevice, alias );
	}

	public void setHomeWifi(WifiBeacon device) {
		preferences
			.edit()
			.putString( KEY_HOME_WIFI_ALIAS, device.getAlias() )
			.putString( KEY_HOME_WIFI_ADDRESS, device.getAddress() )
			.putString( KEY_HOME_WIFI_NAME, device.getName() )
			.commit();
	}
	
	public WifiBeacon getHomeWifiDevice() {
		String bssid = preferences.getString( KEY_HOME_WIFI_ADDRESS, null );
		if( bssid == null ) {
			return null;
		}
		String alias = preferences.getString( KEY_HOME_WIFI_ALIAS, null );
		String ssid = preferences.getString( KEY_HOME_WIFI_NAME, null );
		return new WifiBeacon( alias, ssid, bssid );
	}
}
