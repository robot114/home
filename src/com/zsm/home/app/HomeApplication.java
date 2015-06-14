package com.zsm.home.app;

import android.app.Application;
import android.app.PendingIntent;

import com.zsm.driver.android.log.AndroidLog;
import com.zsm.home.location.HomeLocation;
import com.zsm.home.preferences.Preferences;
import com.zsm.log.Log;

public class HomeApplication extends Application {

	private static final int LOCATION_DELTA_TIME = 1000 * 60 /4;
	
	private static final String ANDROID_LOG = "AndroidLog";
	private static final String FILE_LOG = "FileLog";
	private static final String DEFAULT_LOG = ANDROID_LOG;
	public static final String HOME_PROXIMITY_ALERT = "com.zsm.home.HOME_PROXIMITY_ALERT";
	
	public static PendingIntent proximityAlertIntent;
	
	public HomeApplication() {
		Log.setGlobalLevel( Log.LEVEL.DEBUG );
		
		Log.install( ANDROID_LOG, new AndroidLog( "home" ) );
		Log.setLevel( ANDROID_LOG, Log.LEVEL.DEBUG );
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Preferences.init(this);
		HomeLocation.init( this, LOCATION_DELTA_TIME );
	}

}
