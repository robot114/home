package com.zsm.home.ui;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.zsm.android.beacon.BeaconScanner;
import com.zsm.android.beacon.BeaconScanner.Callback;
import com.zsm.android.beacon.BeaconScanner.STOP_TYPE;
import com.zsm.home.R;
import com.zsm.home.app.HomeApplication;
import com.zsm.home.app.HomeProximityReceiver;
import com.zsm.home.location.HomeLocation;
import com.zsm.home.preferences.MainPreferencesActivity;
import com.zsm.home.preferences.Preferences;
import com.zsm.home.ui.bluetooth.BluetoothSelectionActivity;
import com.zsm.home.ui.bluetooth.NamedBluetoothDevice;
import com.zsm.log.Log;

public class MainActivity extends Activity implements Observer {

	private static final String MAIN_SCANNER_NAME = "MainState";

	public static final String EXTRAS_BLUETOOTH_DEVICE = "BLUETOOTH_DEVICE";

	private static final int REQUEST_HOME_LOCATION = 1;
	private static final int REQUEST_BLUETOOTH_SELECTION = 2;
	
	private static final int REQUEST_BLUETOOTH_ENABLE = 100;
	
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
	
	private boolean mBluetoothPrompted = false;
	private TextView mBluetoothView;
	private NamedBluetoothDevice mBluetoothDevice;

	private BeaconScanner mBeaconScanner;
	
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
		
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		prepareForBluetooth();
	}

	private void prepareForBluetooth() {
		mBluetoothDevice = Preferences.getInstance().getHomeBluetoothDevice();
		
		Callback callback = new BeaconScanner.Callback() {
			@Override
			public void stopped(STOP_TYPE type) {
				int resId = R.string.stateBtDeviceNotInRange;
				
				if( type == STOP_TYPE.FOUND ) {
					resId = R.string.stateBtDeviceInRange;
				}
                updateBluetoothView(mBluetoothDevice, resId );
			}
			
			@Override
			public void newBeaconFound(BluetoothDevice device) {
				if( device.equals( mBluetoothDevice.getDevice() ) ) {
	                updateBluetoothView(mBluetoothDevice,
	                					R.string.stateBtDeviceInRange );
				}
			}
			
			@Override
			public void backgroundFunction(int backgroundCalledTimes) {
				// TODO Auto-generated method stub
				
			}
		};
		mBeaconScanner = new BeaconScanner( this );
		mBeaconScanner.newOperator(MAIN_SCANNER_NAME, callback);
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
		
		enableBluetooth();
		
		updateBtDeviceState(mBluetoothDevice);
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
	}

	public void onSetHome( MenuItem item ) {
		Intent homeIntent = new Intent( this, HomeActivity.class );
		startActivityForResult(homeIntent, REQUEST_HOME_LOCATION);
	}

	public void onSelectBluetooth( MenuItem item ) {
		Intent btIntent = new Intent( this, BluetoothSelectionActivity.class );
		startActivityForResult(btIntent, REQUEST_BLUETOOTH_SELECTION);
	}

	public void onPreferences( MenuItem item ) {
		Intent intent = new Intent( this, MainPreferencesActivity.class );
		startActivity( intent );
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch( requestCode ) {
			case REQUEST_BLUETOOTH_ENABLE:
				promptBluetooth( resultCode );
				break;
			case REQUEST_BLUETOOTH_SELECTION:
				if( resultCode == RESULT_OK ) {
					NamedBluetoothDevice device
						= data.getParcelableExtra( EXTRAS_BLUETOOTH_DEVICE );
					if( resultCode == RESULT_OK && device != null ) {
						Preferences.getInstance().setHomeBluetooth( device );
						mBluetoothDevice = device;
						updateBtDeviceState(device);
					}
				}
				break;
			default:
				break;
		}
	}

	private void updateBtDeviceState( final NamedBluetoothDevice device ) {
		mBeaconScanner
			.startScan( MAIN_SCANNER_NAME, device.getDevice() );
        updateBluetoothView( mBluetoothDevice, R.string.stateBtDeviceChecking );
	}
	
	private void updateBluetoothView(NamedBluetoothDevice device, int stateResId ) {
		if( device == null ) {
			mBluetoothView.setText( R.string.promptNoBluetoothSelected );
			return;
		}
		
		Resources resources = getResources();
		String stateStr = resources.getString( stateResId );
		String text
			= resources.getString( R.string.mainBluetooth,
								   device.toString(), stateStr );
		mBluetoothView.setText(text);
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
	
	private void enableBluetooth() {
		BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
		if( !mBluetoothPrompted && !bluetooth.isEnabled() ) {
			startActivityForResult( 
					new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE ),
								REQUEST_BLUETOOTH_ENABLE );
			
			mBluetoothPrompted = true;
		}
	}

	private void promptBluetooth( int resultCode ) {
		int id;
		if( resultCode == RESULT_OK ) {
			id = R.string.promptBluetoothEnableOk;
		} else {
			id = R.string.promptBluetoothEnableCanceled;
		}
		
		Toast.makeText( this, id, Toast.LENGTH_LONG ).show();
	}

	private void stopScanBluetooth() {
		mBeaconScanner.stopAll( );
	}
}

