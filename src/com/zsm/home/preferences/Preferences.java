package com.zsm.home.preferences;

import com.zsm.log.Log;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

public class Preferences {

	static final String KEY_PROXIMITY_ALERT_ON = "PROXIMITY_ALERT_ON";
	static final String KEY_PROXIMITY_DISTANCE = "PROXIMITY_DISTANCE";

	private static final String KEY_LOCATION_TIME = "LOCATION_TIME";

	private static final String KEY_LOCATION_PROVIDER = "LOCATION_PROVIDER";

	private static final String KEY_LOCATION_HAS_ALTITUDE = "LOCATION_HAS_ALTITUDE";

	private static final String KEY_LOCATION_ALTITUDE = "LOCATION_ALTITUDE";

	private static final String KEY_LOCATION_LONGITUDE = "LOCATION_LONGITUDE";

	private static final String KEY_LOCATION_LATITUDE = "LOCATION_LATITUDE";

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
		editor.putFloat(KEY_LOCATION_LATITUDE, (float) l.getLatitude());
		editor.putFloat(KEY_LOCATION_LONGITUDE, (float) l.getLongitude());
		editor.putBoolean(KEY_LOCATION_HAS_ALTITUDE, l.hasAltitude());
		if( l.hasAltitude() ) {
			editor.putFloat(KEY_LOCATION_ALTITUDE, (float) l.getAltitude());
		} else {
			editor.remove(KEY_LOCATION_ALTITUDE);
		}
		editor.putLong(KEY_LOCATION_TIME, l.getTime());
		editor.putString(KEY_LOCATION_PROVIDER, l.getProvider());
		editor.commit();
	}
	
	/**
	 * Get the location of home from the preferences.
	 * 
	 * @return location of home, null if it has not been set.
	 */
	public Location getHomeLocation() {
		float latitude = preferences.getFloat( KEY_LOCATION_LATITUDE, Float.NaN );
		float longitude = preferences.getFloat( KEY_LOCATION_LONGITUDE, Float.NaN );
		if( Float.isNaN(latitude) || Float.isNaN(longitude) ) {
			return null;
		}
		
		Location l = new Location( (String)"?" );
		l.setLatitude( latitude );
		l.setLongitude( longitude );
		
		float altitude = preferences.getFloat( KEY_LOCATION_ALTITUDE, Float.NaN );
		if( altitude != Float.NaN
			&& preferences.getBoolean( KEY_LOCATION_HAS_ALTITUDE, false) ) {
			
			l.setAltitude(altitude);
		}
		
		String provider = preferences.getString( KEY_LOCATION_PROVIDER, "?" );
		l.setProvider(provider);
		long time = preferences.getLong( KEY_LOCATION_TIME, System.currentTimeMillis() );
		l.setTime(time);
		
		return l;
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
	
}
