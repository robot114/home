package com.zsm.home.app;

import android.app.Application;
import android.app.PendingIntent;

import com.zsm.driver.android.log.LogInstaller;
import com.zsm.driver.android.log.LogPreferences;
import com.zsm.home.location.HomeLocation;
import com.zsm.home.preferences.Preferences;

public class HomeApplication extends Application {

	public static final int LOCATION_DELTA_TIME = 1000 * 60 /12;
	
	public static final String HOME_PROXIMITY_ALERT = "com.zsm.home.ACTION_HOME_PROXIMITY_ALERT";
	
	public static PendingIntent proximityAlertIntent;

	public static final String KEY_HOME_LOCATION_ADDRESS = "HOME_LOCATION_ADDRAESS";

	public static final String KEY_HOME_LOCATION = "HOME_LOCATION";
	
	public HomeApplication() {
		LogInstaller.installAndroidLog( "Home" );
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Preferences.init(this);
		HomeLocation.init( this, LOCATION_DELTA_TIME );
		
		LogPreferences.init( this );
		LogInstaller.installFileLog( this );
	}

}
