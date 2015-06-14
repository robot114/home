package com.zsm.home.location;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.zsm.home.app.HomeApplication;
import com.zsm.home.preferences.Preferences;
import com.zsm.log.Log;

public class HomeLocation extends Observable
				implements LocationListener, Observer {

	private static HomeLocation instance;
	private LocationManager locationManager;
	private List<String> providers;
	private Handler handler;
	private Runnable timeoutRunnable;
	private Location homeLocation;
	private boolean settingHomeLocation;
	private long locationDeltaTime;
	private int homeLocationSet;

	public static void init( Context c, long locationDeltaTime ) {
		if( instance != null ) {
			throw new IllegalStateException( "HomeLocation has been initialized!" );
		}
		instance = new HomeLocation( c, locationDeltaTime );
	}
	
	public static HomeLocation getInstance() {
		return instance;
	}
	
	private HomeLocation( Context c, long locationDeltaTime ) {
		this.locationDeltaTime = locationDeltaTime;
		
		homeLocation = Preferences.getInstance().getHomeLocation();
		
		locationManager
			= (LocationManager)c.getSystemService(Context.LOCATION_SERVICE);
	
	    handler = new Handler(Looper.myLooper());
	    timeoutRunnable = new Runnable() {
			public void run() {
				locationManager.removeUpdates(HomeLocation.this);
				if( homeLocation != null ) {
				    // Some providers did not return, set the best location by now.
					Log.i( "Best location when locate time out. ", homeLocation );
					notifyObservers( homeLocation );
				} else {
					Log.i( "Home location not available" );
				}
				settingHomeLocation = false;
			}
	    };
	}

	public void updateAndSaveLocation() {
		if( settingHomeLocation ) {
			return;
		}
		settingHomeLocation = true;
		homeLocationSet = 0;
		
		providers = updateProviders();
		
		// initialize the home location with last known location, if the last
		// known location is gotten recently. Otherwise, the home location
		// is used as last set location. All this is to shorten the time
		// to display the home location.
		for( String p : providers ) {
			getLastUsableLocation( p );
		}
		
		for( String p : providers ) {
			locationManager.requestSingleUpdate(p, this, null);
		}
		
		handler.postDelayed(timeoutRunnable, locationDeltaTime*2);

	}
	
	public boolean anyProviderEnabled() {
		return updateProviders().size() > 0;
	}

	private List<String> updateProviders() {
		List<String> ps = locationManager.getAllProviders();
		ps.remove( LocationManager.PASSIVE_PROVIDER );
		return ps;
	}
	
	private void getLastUsableLocation(final String provider) {
		Location ll = locationManager.getLastKnownLocation(provider);
		updateHomeLocation(ll);
	}

	private void updateHomeLocation(Location newLocation) {
		if( LocationUtility.isBetterNewLocation( newLocation,
												 homeLocation,
												 locationDeltaTime ) ) {
			setChanged();
			homeLocation = newLocation;
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		updateHomeLocation(location);
		Log.d( "Better location gotten when location changed. ", homeLocation );
		homeLocationSet++;
		if( homeLocationSet == providers.size() ) {
			locationManager.removeUpdates(this);
			handler.removeCallbacks(timeoutRunnable);
			// All the providers update the location, choose the best one.
			Log.i( "Best location gotten when location changed. ", homeLocation );
			notifyObservers(homeLocation);
			settingHomeLocation = false;
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d( "Location provider status changed. ", provider, status );
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d( "Location provider enabled. ", provider );
	}

	@Override
	public void onProviderDisabled(String provider) {
	}
	
	public void setProximityAlert(Context activity, boolean alertOn,
								  Location home, float distance) {
		
		LocationManager lm
			= (LocationManager)activity
					.getSystemService( Context.LOCATION_SERVICE );
		
		if( HomeApplication.proximityAlertIntent != null ) {
			Log.d( "Remove the previous alert first!",
				   HomeApplication.proximityAlertIntent );
			lm.removeProximityAlert( HomeApplication.proximityAlertIntent );
			HomeApplication.proximityAlertIntent = null;
		}
		if( alertOn && home != null ) {
			Log.d( "Alert is ON, add new alert.", "homeLocation", home, "distance", distance );
			Intent intent = new Intent( HomeApplication.HOME_PROXIMITY_ALERT );
			HomeApplication.proximityAlertIntent
				= PendingIntent.getBroadcast( activity, -1, intent,
											  PendingIntent.FLAG_CANCEL_CURRENT );
			lm.addProximityAlert( home.getLatitude(), home.getLongitude(),
								  distance, -1,
								  HomeApplication.proximityAlertIntent );
		}
	}

	@Override
	public void update(Observable observable, Object data) {
		Preferences.getInstance().setHomeLocation((Location) data);
	}
}
