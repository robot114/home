package com.zsm.home.ui;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.zsm.home.R;
import com.zsm.home.app.HomeApplication;
import com.zsm.home.app.HomeProximityReceiver;
import com.zsm.home.location.HomeLocation;
import com.zsm.home.preferences.MainPreferencesActivity;
import com.zsm.home.preferences.Preferences;
import com.zsm.log.Log;

public class MainActivity extends Activity implements Observer {

	final private LocationListener locationListener
		= new LocationListener() {
	
			@Override
			public void onLocationChanged(Location location) {
				updateView(location, currentAt, R.string.currentAt );
			}
	
			@Override
			public void onStatusChanged(String provider, int status,
										Bundle extras) {
			}
	
			@Override
			public void onProviderEnabled(String provider) {
			}
	
			@Override
			public void onProviderDisabled(String provider) {
			}
		
	};
	private LocationManager locationManager;
	private TextView currentAt;
	private TextView homeAt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView( R.layout.main );
		
		Preferences prefs = Preferences.getInstance();
		Location homeLocation = prefs.getHomeLocation();
		HomeLocation.getInstance()
			.setProximityAlert(this, prefs.isProximityAlertOn(),
							   homeLocation,
							   prefs.getHomeProximityDistance() );
		
		IntentFilter filter = new IntentFilter( HomeApplication.HOME_PROXIMITY_ALERT );
		registerReceiver(new HomeProximityReceiver(), filter);
		Log.d( "Proximity alert registered.", filter );
		
		homeAt = (TextView)findViewById( R.id.textViewHomeLocation );
		updateView(homeLocation, homeAt, R.string.homeAt);
		
		currentAt = (TextView)findViewById( R.id.textViewCurrentLocation );
		currentAt.setText( "" );
		
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater mi = getMenuInflater();
		mi.inflate( R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		HomeLocation.getInstance().addObserver( this );
		List<String> providers = locationManager.getAllProviders();
		for( String p : providers ) {
			locationManager.requestLocationUpdates( p, 200, 1, locationListener);
			Log.d( "Location listener registered.", p );
		}
	}

	@Override
	protected void onPause() {
		locationManager.removeUpdates(locationListener);
		HomeLocation.getInstance().deleteObserver( this );
		super.onPause();
	}

	public void onSetHome( MenuItem item ) {
		if( !HomeLocation.getInstance().anyProviderEnabled() ) {
			
			Toast.makeText(this, R.string.promptTurnOnLocationAccess,
						   Toast.LENGTH_LONG).show();
            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(myIntent);
		}
		
		HomeLocation.getInstance().updateAndSaveLocation();
	}

	public void onPreferences( MenuItem item ) {
		Intent intent = new Intent( this, MainPreferencesActivity.class );
		startActivity( intent );
	}

	@Override
	public void update(Observable observable, Object data) {
		Location location = (Location) data;
		toastHome( location );
		updateView(location, homeAt, R.string.homeAt);
	}
	
	private void toastHome( Location homeLocation ) {
		String homeStr
			= getResources().getString( R.string.homeAt,
										homeLocation.getLongitude(),
										homeLocation.getLatitude(),
										homeLocation.getProvider() );
		Toast.makeText(this, homeStr, Toast.LENGTH_LONG ).show();
	}

	private void updateView(Location location, TextView textView, int resId) {
		if( location != null ) {
			String homeStr
				= getResources().getString( resId,
											location.getLongitude(),
											location.getLatitude(),
											location.getProvider() );
			textView.setText(homeStr);
		} else {
			textView.setText( "" );
		}
	}

}

