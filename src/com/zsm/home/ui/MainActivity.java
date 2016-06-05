package com.zsm.home.ui;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.zsm.android.beacon.BeaconDevice;
import com.zsm.android.beacon.BeaconOperator;
import com.zsm.android.beacon.BeaconOperator.PROTOCOL;
import com.zsm.android.beacon.BeaconOperatorFactory;
import com.zsm.android.beacon.BluetoothBeacon;
import com.zsm.android.beacon.WifiBeacon;
import com.zsm.home.R;
import com.zsm.home.app.HomeApplication;
import com.zsm.home.app.HomeProximityReceiver;
import com.zsm.home.location.HomeLocation;
import com.zsm.home.preferences.MainPreferencesActivity;
import com.zsm.home.preferences.Preferences;
import com.zsm.home.ui.beacon.BeaconSelectionActivity;
import com.zsm.home.ui.beacon.BeaconSelectionFragment;
import com.zsm.log.Log;

public class MainActivity extends Activity implements Observer {

	public static final String EXTRAS_DEVICE = "DEVICE";

	private static final int REQUEST_HOME_LOCATION = 1;
	private static final int REQUEST_BLUETOOTH_SELECTION = 2;
	
	private static final int REQUEST_WIFI_SELECTION = 101;
	
	final private LocationListener locationListener
		= new LocationListener() {
	
			@Override
			public void onLocationChanged(Location location) {
				updateView(location, mCurrentAt, R.string.currentAt );
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
	private LocationManager mLocationManager;
	private TextView mCurrentAt;
	private TextView mHomeAt;
	private HomeProximityReceiver mHomeProxReceiver;
	
	private TextView mBluetoothView;
	private BluetoothBeacon mBluetoothDevice;

	private BeaconOperator mMainBleOperator;

	private WifiBeacon mWifiDevice;
	private BeaconOperator mMainWifiOperator;
	protected BroadcastReceiver mWifiStateReceiver;
	private TextView mWifiView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView( R.layout.main );
		
		IntentFilter filter = new IntentFilter( HomeApplication.HOME_PROXIMITY_ALERT );
		mHomeProxReceiver = new HomeProximityReceiver();
		registerReceiver(mHomeProxReceiver, filter);
		Log.d( "Proximity alert registered.", filter );
		
		Preferences prefs = Preferences.getInstance();
		Location homeLocation = prefs.getHomeLocation();
		HomeLocation.getInstance()
			.setProximityAlert(prefs.isProximityAlertOn(),
							   homeLocation,
							   prefs.getHomeProximityDistance() );
		
		mHomeAt = (TextView)findViewById( R.id.textViewHomeLocation );
		updateView(homeLocation, mHomeAt, R.string.homeAt);
		
		mCurrentAt = (TextView)findViewById( R.id.textViewCurrentLocation );
		mCurrentAt.setText( "" );
		
		mBluetoothView = (TextView)findViewById( R.id.textViewHomeBluetooth );
		mWifiView = (TextView)findViewById( R.id.textViewHomeWifi );
		
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		prepareForBluetooth();
		prepareForWifi();
	}

	private void prepareForBluetooth() {
		mBluetoothDevice = Preferences.getInstance().getHomeBluetoothDevice();
		
		BeaconOperator.Callback callback = new BeaconOperator.Callback() {
			@Override
			public void stopped(BeaconOperator.STOP_TYPE type) {
				int resId = R.string.stateBtDeviceNotInRange;
				
				if( type == BeaconOperator.STOP_TYPE.FOUND ) {
					resId = R.string.stateBtDeviceInRange;
				}
                updateDeviceView(mBluetoothDevice, mBluetoothView, resId );
			}
			
			@Override
			public void backgroundFunction(int backgroundCalledTimes) {
			}

			@Override
			public void newBeaconFound(BeaconDevice device) {
				if( device.equals( mBluetoothDevice ) ) {
	                updateDeviceView(mBluetoothDevice,
	                					mBluetoothView, R.string.stateBtDeviceInRange );
				}
			}
		};
		
		mMainBleOperator
			= BeaconOperatorFactory
				.createOperator( this, BeaconOperator.PROTOCOL.BLUETOOTH, callback );
	}

	private void prepareForWifi() {
		mWifiDevice = Preferences.getInstance().getHomeWifiDevice();
		
		BeaconOperator.Callback callback = new BeaconOperator.Callback() {
			@Override
			public void stopped(BeaconOperator.STOP_TYPE type) {
				int resId = R.string.stateBtDeviceNotInRange;
				
				if( type == BeaconOperator.STOP_TYPE.FOUND ) {
					resId = R.string.stateBtDeviceInRange;
				}
                updateDeviceView( mWifiDevice, mWifiView, resId );
			}
			
			@Override
			public void backgroundFunction(int backgroundCalledTimes) {
			}

			@Override
			public void newBeaconFound(BeaconDevice device) {
				if( device.equals( mWifiDevice ) ) {
	                updateDeviceView( mWifiDevice, mWifiView,
	                				  R.string.stateBtDeviceInRange );
				}
			}
		};
		
		mMainWifiOperator
			= BeaconOperatorFactory
				.createOperator( this, BeaconOperator.PROTOCOL.WIFI, callback );
		
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
		List<String> providers = mLocationManager.getAllProviders();
		for( String p : providers ) {
			mLocationManager.requestLocationUpdates( p, 200, 1, locationListener);
			Log.d( "Location listener registered.", p );
		}
		
		updateBtDeviceState(mBluetoothDevice);
		updateWifiDeviceState(mWifiDevice);
	}

	@Override
	protected void onPause() {
		mLocationManager.removeUpdates(locationListener);
		HomeLocation.getInstance().deleteObserver( this );
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mHomeProxReceiver);
        stopScanBluetooth();
        
        if( mWifiStateReceiver != null ) {
        	unregisterReceiver(mWifiStateReceiver);
        	mWifiStateReceiver = null;
        }
        mMainWifiOperator.stopScan( BeaconOperator.STOP_TYPE.CANCELLED );
	}

	public void onSetHome( MenuItem item ) {
		Intent homeIntent = new Intent( this, HomeActivity.class );
		startActivityForResult(homeIntent, REQUEST_HOME_LOCATION);
	}

	public void onSelectBluetooth( MenuItem item ) {
		Intent intent = new Intent( this, BeaconSelectionActivity.class );
		intent.putExtra( BeaconSelectionFragment.KEY_BEACON_PROTOCOL,
						 PROTOCOL.BLUETOOTH );
		intent.putExtra( BeaconSelectionFragment.KEY_CURRENT_DEVICE,
						 Preferences.getInstance().getHomeBluetoothDevice() );
		startActivityForResult(intent, REQUEST_BLUETOOTH_SELECTION);
	}

	public void onSelectWifi( MenuItem item ) {
		Intent intent = new Intent( this, BeaconSelectionActivity.class );
		intent.putExtra( BeaconSelectionFragment.KEY_BEACON_PROTOCOL,
						 PROTOCOL.WIFI );
		intent.putExtra( BeaconSelectionFragment.KEY_CURRENT_DEVICE,
						 Preferences.getInstance().getHomeWifiDevice() );
		startActivityForResult(intent, REQUEST_WIFI_SELECTION);
	}

	public void onPreferences( MenuItem item ) {
		Intent intent = new Intent( this, MainPreferencesActivity.class );
		startActivity( intent );
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch( requestCode ) {
			case REQUEST_BLUETOOTH_SELECTION:
				if( resultCode == RESULT_OK ) {
					BluetoothBeacon device
						= data.getParcelableExtra( EXTRAS_DEVICE );
					if( resultCode == RESULT_OK && device != null ) {
						Preferences.getInstance().setHomeBluetooth( device );
						mBluetoothDevice = device;
						updateBtDeviceState(device);
					}
				}
				break;
			case REQUEST_WIFI_SELECTION:
				if( resultCode == RESULT_OK ) {
					WifiBeacon device
						= data.getParcelableExtra( EXTRAS_DEVICE );
					if( resultCode == RESULT_OK && device != null ) {
						Preferences.getInstance().setHomeWifi( device );
						mWifiDevice = device;
						updateWifiDeviceState(device);
					}
				}
				break;
			default:
				break;
		}
	}

	private void updateBtDeviceState( final BluetoothBeacon device ) {
		if( device == null ) {
			mBluetoothView.setText( R.string.promptNoDeviceSelected );
			return;
		}
		
		// The scan can be performed, even the bluetooth is not enabled.
		mMainBleOperator.startScan( device );
        updateDeviceView( mBluetoothDevice, mBluetoothView, R.string.stateBtDeviceChecking );
	}
	
	private void updateDeviceView( BeaconDevice device, TextView textView,
								   int stateResId ) {
		if( device == null ) {
			textView.setText( R.string.promptNoDeviceSelected );
			return;
		}
		
		Resources resources = getResources();
		String stateStr = resources.getString( stateResId );
		String text
			= resources.getString( R.string.beaconState,
								   device.getProtocol().name(),
								   device.toString(), stateStr );
		textView.setText(text);
	}

	private void updateWifiDeviceState( final WifiBeacon device ) {
		if( device == null ) {
			mWifiView.setText( R.string.promptNoDeviceSelected );
			return;
		}
		
		// It is able to scan, even the wifi is not enabled.
		mMainWifiOperator.startScan( device );
        updateDeviceView( device, mWifiView, R.string.stateBtDeviceChecking );
	}
	
	@Override
	public void update(Observable observable, Object data) {
		Location location = (Location) data;
		toastHome( location );
		updateView(location, mHomeAt, R.string.homeAt);
	}
	
	private void toastHome( Location homeLocation ) {
		String homeStr = toLocationString(homeLocation, R.string.homeAt);
		Toast.makeText(this, homeStr, Toast.LENGTH_LONG ).show();
	}

	private void updateView(Location location, TextView textView, int resId) {
		if( location != null ) {
			textView.setText(toLocationString(location, resId));
		} else {
			textView.setText( "" );
		}
	}

	private String toLocationString(Location location, int resId) {
		return getResources().getString( resId,
									location.getLatitude(),
									location.getLongitude(),
									location.getProvider() );
	}
	
	private void stopScanBluetooth() {
		mMainBleOperator.stopScan( BeaconOperator.STOP_TYPE.CANCELLED );
	}

}

