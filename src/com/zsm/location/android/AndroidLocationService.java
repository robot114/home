package com.zsm.location.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import com.zsm.home.R;
import com.zsm.location.LocationSerivce;
import com.zsm.location.LocationUpdateListener;
import com.zsm.location.LocationUpdateListener.CANCEL_REASON;
import com.zsm.log.Log;

public class AndroidLocationService
				implements LocationSerivce<String, Location>, LocationListener {

	private static final int TIME_OIUT_NUM = 10;

	private static final int PROVIDER_MAX_NUM = 5;
	
	private LocationManager locationManager;
	private ArrayList<String> exclusiveProviders
				= new ArrayList<String>( PROVIDER_MAX_NUM );
	private List<String> providers;

	private long locationDeltaTime;

	private Set<LocationUpdateListener<Location>> locationListeners
				= Collections.synchronizedSet( 
						new HashSet<LocationUpdateListener<Location>>() );

	private int requestUpdateNumber;

	private Location currentLocation;
	
	private Handler handler;
    
	private long timeoutInMs;
	private int timeoutCount;

	private Context context;

	private String mostAccurateProvider;

	private Runnable timeoutRunnable = new Runnable() {
		synchronized public void run() {
			timeoutCount++;
			if( timeoutCount < TIME_OIUT_NUM ) {
				handler.postDelayed(timeoutRunnable, timeoutInMs);
				String string
					= context.getResources()
						.getString( R.string.locatingProcess,
									timeoutCount*100/TIME_OIUT_NUM );
				Toast.makeText( context, string, Toast.LENGTH_SHORT ).show();
								
			} else {
				locationManager.removeUpdates(AndroidLocationService.this);
				if( currentLocation != null ) {
				    // Some providers did not return, set the best location by now.
					Log.i( "Best location when locate time out. ", currentLocation );
					updateCurrentLocationFinished();
				} else {
					Log.i( "Home location not available" );
					cancelUpdateCurrentLocation( CANCEL_REASON.TIME_OUT );
				}
			}
		}
    };

	@Override
	public void init(Object context, Object manager, long locationDeltaTime,
					 long timeoutInMs ) {
		
		this.context = (Context)context;
		this.locationManager = (LocationManager)manager;
		this.locationDeltaTime = locationDeltaTime;
	    handler = new Handler(Looper.myLooper());
	    this.timeoutInMs = timeoutInMs/TIME_OIUT_NUM;
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
		updateMostAccurateProvider();
		return providers;
	}

	private void updateMostAccurateProvider() {
		int mostAcc = 1000000;
		for( String name : providers ) {
			LocationProvider p = locationManager.getProvider( name );
			int curAcc = p.getAccuracy();
			if( mostAcc > curAcc ) {
				mostAcc = curAcc;
				mostAccurateProvider = name;
			}
		}
	}

	@Override
	public boolean anyActiveProviderEnabled() {
		updateProviders();
		for( String p : providers ) {
			if( !p.equals(LocationManager.PASSIVE_PROVIDER)
				&& locationManager.isProviderEnabled(p) ) {
				
				return true;
			}
		}
		return false;
	}

	@Override
	public String getMostAccurateProvider() {
		if( mostAccurateProvider == null ) {
			updateProviders();
		}
		
		Log.d( "The current most accurate provider ", mostAccurateProvider );
		return mostAccurateProvider;
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
				&& ( System.currentTimeMillis() - location.getTime() ) 
						< locationDeltaTime;
	}

	@Override
	synchronized public void startUpdate( 
					LocationUpdateListener<Location> listener ) {
		
		Log.d( "Starting update location with listener", listener );
		if( listener == null ) {
			throw new NullPointerException( "listener is null!" );
		}
		if( !anyActiveProviderEnabled() ) {
		    promptEnableProvider(); 
		}
		if( locationListeners.size() == 0 ) {
			updateProviders();
			currentLocation = null;
			requestUpdateNumber = providers.size();
			for( String p : providers ) {
				locationManager.requestSingleUpdate(p, this, null );
			}
			timeoutCount = 0;
			Toast.makeText(context, R.string.locatingStart, Toast.LENGTH_SHORT)
				 .show();
			handler.postDelayed(timeoutRunnable, timeoutInMs);
		}
		locationListeners.add(listener);
	}

	private void promptEnableProvider() {
		// notify user
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setMessage( R.string.noLocationEnabled );
		dialog.setPositiveButton(android.R.string.yes,
								 new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface paramDialogInterface,
		    					int paramInt) {
		    	
		        Intent intent
		        	= new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		        context.startActivity(intent);
		    }
		});
		dialog.setNegativeButton(android.R.string.no,
								 new DialogInterface.OnClickListener() {

		    @Override
		    public void onClick(DialogInterface paramDialogInterface,
		    					int paramInt) {
		    }
		});
		dialog.show();
	}

	@Override
	synchronized public void cancelUpdate(
					LocationUpdateListener<Location> listener ) {
		
		Log.d( "To STOP the update listener", "listener", listener, 
				"listeners num", locationListeners.size() );
		cancelUpdateCurrentLocation(listener, CANCEL_REASON.STOP);
	}
	
	@Override
	synchronized public void cancelUpdate() {
		Log.d( "To STOP all the update listeners", "listeners num",
				locationListeners.size() );
		cancelUpdateCurrentLocation(CANCEL_REASON.STOP);
	}

	@Override
	synchronized public void onLocationChanged(Location location) {
		currentLocation
			= LocationUtility.getBetterLocation(location, currentLocation,
												locationDeltaTime);
		requestUpdateNumber--;
		if( requestUpdateNumber == 0 
			|| getMostAccurateProvider().equals( currentLocation.getProvider() )) {
			
			Log.d( "Choice the best location from all providers", currentLocation );
			updateCurrentLocationFinished( );
		}
	}

	synchronized private void updateCurrentLocationFinished() {
		for( LocationUpdateListener<Location> listener : locationListeners ) {
			listener.onUpdate(currentLocation);
		}
		locationListeners.clear();
		String string
			= context.getResources().getString(
					R.string.locatingSuccess,
					currentLocation.getLatitude(),
					currentLocation.getLongitude(),
					currentLocation.getProvider() );
		
		Toast.makeText(context, string, Toast.LENGTH_LONG ).show();
		cancelTimeoutTask();
	}

	private void cancelUpdateCurrentLocation(CANCEL_REASON r ) {
		for( LocationUpdateListener<Location> listener : locationListeners ) {
			cancelUpdateCurrentLocation( listener, r );
		}
	}
	
	private void cancelUpdateCurrentLocation(
						LocationUpdateListener<Location> listener,
						CANCEL_REASON r ) {
		
		if( locationListeners.remove(listener) ) {
			listener.onCancel( r );
		}
		if( locationListeners.isEmpty() ) {
			cancelTimeoutTask();
		}
	}
	
	private void cancelTimeoutTask() {
		handler.removeCallbacks(timeoutRunnable);
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
