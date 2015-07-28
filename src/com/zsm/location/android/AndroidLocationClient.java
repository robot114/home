package com.zsm.location.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.zsm.location.LocationClient;
import com.zsm.location.OnLocationUpdateListener;
import com.zsm.log.Log;

public class AndroidLocationClient implements LocationClient<String, Location>, LocationListener {

	private static final int PROVIDER_MAX_NUM = 5;
	
	private LocationManager locationManager;
	private ArrayList<String> exclusiveProviders
				= new ArrayList<String>( PROVIDER_MAX_NUM );
	private List<String> providers;

	private long locationDeltaTime;

	private Set<OnLocationUpdateListener<Location>> locationListeners
				= Collections.synchronizedSet( 
						new HashSet<OnLocationUpdateListener<Location>>() );

	private int requestUpdateNumber;

	private Location currentLocation;
	
	private Handler handler;
    
	private Runnable timeoutRunnable = new Runnable() {
		synchronized public void run() {
			locationManager.removeUpdates(AndroidLocationClient.this);
			if( currentLocation != null ) {
			    // Some providers did not return, set the best location by now.
				Log.i( "Best location when locate time out. ", currentLocation );
				updateCurrentLocationFinished();
			} else {
				Log.i( "Home location not available" );
				cancelUpdateCurrentLocation();
			}
		}
    };

	@Override
	public void init(Object manager, long locationDeltaTime ) {
		this.locationManager = (LocationManager)manager;
		this.locationDeltaTime = locationDeltaTime;
	    handler = new Handler(Looper.myLooper());
	}

	@Override
	public void addExclusiveProvider(String exclusiveProvider) {
		exclusiveProviders.add(exclusiveProvider);
	}

	@Override
	public  Collection<String> updateProviders() {
		if( locationManager == null ) {
			throw new IllegalStateException( "Location client has not been "
											 + "initialized, call init first!" );
		}
		providers = locationManager.getAllProviders();
		providers.remove( exclusiveProviders );
		return providers;
	}

	@Override
	public boolean anyProviderEnabled() {
		updateProviders();
		for( String p : providers ) {
			if( locationManager.isProviderEnabled(p) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Location getLastUpdatedLocation(Location current) {
		updateProviders();
		
		Location location = current;
		for( String p : providers ) {
			location = getLastUpdatedLocation( p, location );
		}
		
		return location;
	}

	private Location getLastUpdatedLocation( String provider, Location current ) {
		Location newLocation = locationManager.getLastKnownLocation(provider);
		Location l;
		if( LocationUtility.isBetterNewLocation( newLocation,
												 current,
												 locationDeltaTime ) ) {
			l = newLocation;
		} else {
			l = current;
		}
		
		if( locationNewEnough( l ) ) {
			return l;
		}
		
		return null;
	}

	private boolean locationNewEnough(Location location) {
		return location != null 
				&& ( System.currentTimeMillis() - location.getTime() )  < locationDeltaTime;
	}

	@Override
	synchronized public void updateCurrentLocation( 
					long timeoutInMs,
					final OnLocationUpdateListener<Location> listener ) {
		
		if( listener == null ) {
			throw new NullPointerException( "listener is null!" );
		}
		if( locationListeners.size() == 0 ) {
			updateProviders();
			currentLocation = null;
			requestUpdateNumber = providers.size();
			for( String p : providers ) {
				locationManager.requestSingleUpdate(p, this, null );
			}
			handler.postDelayed(timeoutRunnable, timeoutInMs);
		}
		locationListeners.add(listener);
	}

	@Override
	synchronized public void cancelUpdate(
					OnLocationUpdateListener<Location> listener) {
		
		locationListeners.remove(listener);
	}
	
	@Override
	synchronized public void onLocationChanged(Location location) {
		currentLocation
			= LocationUtility.getBetterLocation(location, currentLocation,
												locationDeltaTime);
		requestUpdateNumber--;
		if( requestUpdateNumber == 0 ) {
			Log.d( "Choice the best location from all providers", currentLocation );
			updateCurrentLocationFinished( );
			cancelTimeoutTask();
		}
	}

	private void cancelTimeoutTask() {
		handler.removeCallbacks(timeoutRunnable);
	}

	synchronized private void updateCurrentLocationFinished() {
		for( OnLocationUpdateListener<Location> listener : locationListeners ) {
			listener.onUpdate(currentLocation);
		}
		cleanUp();
	}

	private void cancelUpdateCurrentLocation() {
		for( OnLocationUpdateListener<Location> listener : locationListeners ) {
			listener.onCancel();
		}
		cleanUp();
	}
	
	private void cleanUp() {
		locationManager.removeUpdates(this);
		locationListeners.clear();
		currentLocation = null;
	}


	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

}
