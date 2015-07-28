package com.zsm.home.ui;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;
import com.zsm.android.location.GeocoderProvider;
import com.zsm.home.R;
import com.zsm.home.app.HomeApplication;
import com.zsm.home.preferences.Preferences;
import com.zsm.location.LocationClient;
import com.zsm.location.OnLocationUpdateListener;
import com.zsm.location.android.AndroidLocationClient;
import com.zsm.log.Log;

public class HomeActivitySingle extends Activity
				implements OnLocationUpdateListener<Location>, LocationSource,
							OnSuggestionListener, OnLongClickListener {

	private static final int MARK_AS_LOCATION = 2;
	private static final int MARK_AS_HOME = 1;
	private static final LatLng DEFAULT_LOCATION = new LatLng( 116.391469, 39.906708 );
	private static final float DEFAULT_ZOOM_FACTOR = 18.f;
	private MapView mapView;
	private AMap aMap;
	private LocationManager locationManager;
	private LocationClient<String, Location> locationClient;
	private SearchView searchView;
	private Location currentLocation;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		
		setContentView(R.layout.home_single_activity);
		
		searchView = (SearchView)findViewById( R.id.searchViewAddress );
		OnLocationQueryListener queryListener = new OnLocationQueryListener( );
		searchView.setOnQueryTextListener( queryListener );
		SearchManager sm = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
		searchView.setSearchableInfo(sm.getSearchableInfo(getComponentName()));
		searchView.setOnSuggestionListener(this);
		
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationClient = new AndroidLocationClient();
		locationClient.init( locationManager, HomeApplication.LOCATION_DELTA_TIME );
		
		mapView = (MapView) findViewById(R.id.mapHome);
		mapView.onCreate(savedInstanceState);
		aMap = mapView.getMap();
		aMap.setLocationSource(this);
		aMap.getUiSettings().setMyLocationButtonEnabled(true);
		aMap.setMyLocationEnabled( true );
		
		Location homeLoca = Preferences.getInstance().getHomeLocation();
		if( homeLoca != null ) {
			updateToPosition( homeLoca, MARK_AS_HOME );
			currentLocation = homeLoca;
		} else {
			currentLocation = null;
			toMyLocation();
		}
		
		ImageView setHome = (ImageView)findViewById( R.id.imageViewSetHome );
		ImageView setCurrentAsHome = (ImageView)findViewById( R.id.imageViewCurrentLocation );
		
		setHome.setOnLongClickListener( this );
		setCurrentAsHome.setOnLongClickListener( this );
	}

	private void toMyLocation() {
		Location lastLocation
			= locationClient.getLastUpdatedLocation( currentLocation );
		
		if ( lastLocation != null ) {
			updateToPosition( lastLocation, MARK_AS_LOCATION );
			currentLocation = lastLocation;
		} else {
			locationClient.updateCurrentLocation( 
								HomeApplication.LOCATION_DELTA_TIME*2, this);
		}
	}

	private void updateToPosition(Location loca, int markFlag ) {
		updateToPosition(loca.getLatitude(), loca.getLongitude(), markFlag);
	}

	private void updateToPosition(double latitude, double longitude, int markFlag) {
		
		Log.d( "Move to the position", "latitude", latitude,
				"longitude", longitude, "markFlag", markFlag );
		
		aMap.clear();
		
		LatLng latLng = new LatLng( latitude, longitude );
		MarkerOptions marker
			= new MarkerOptions().position( latLng );
		if( markFlag == MARK_AS_HOME ) {
			marker
				.setGps( false )
				.icon( BitmapDescriptorFactory.fromResource( R.drawable.home ) );
		}
		CameraUpdate cu
			= CameraUpdateFactory.newCameraPosition(
					new CameraPosition( latLng, DEFAULT_ZOOM_FACTOR, 0, 0));
		aMap.moveCamera(cu);
		aMap.addMarker(marker);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mapView.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mapView.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		locationClient.cancelUpdate( this );
		mapView.onDestroy();
	}

	@Override
	public void onUpdate(Location location) {
		updateToPosition(location, MARK_AS_LOCATION);
		currentLocation = location;
	}

	@Override
	public void onCancel() {
	}

	@Override
	public void activate(OnLocationChangedListener l) {
		Log.d("To my current location" );
		toMyLocation();
	}

	@Override
	public void deactivate() {
		Log.d("Remove current location listener");
		locationClient.cancelUpdate( this );
	}

	@Override
	public boolean onSuggestionSelect(int position) {
		return false;
	}

	@Override
	public boolean onSuggestionClick(int position) {
		Cursor cursor = (Cursor)(searchView.getSuggestionsAdapter().getItem(position));
		int latColIndex = cursor.getColumnIndex( GeocoderProvider.COLUMN_LATITUDE );
		double lat = cursor.getDouble(latColIndex);
		int lngColIndex = cursor.getColumnIndex( GeocoderProvider.COLUMN_LONGITUDE );
		double lng = cursor.getDouble(lngColIndex);
		
		int textColIndex = cursor.getColumnIndex( SearchManager.SUGGEST_COLUMN_TEXT_1 );
		String address = cursor.getString( textColIndex );
		searchView.setQuery( address, false );
		
		Log.d( "Clicked address: ", address, "lat", lat, "lng", lng );
		updateToPosition( lat, lng, MARK_AS_LOCATION );
		
		return true;
	}

	@Override
	public boolean onLongClick(View v) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.hint, (ViewGroup)null );

		TextView text = (TextView) layout.findViewById(R.id.hint_text);
		text.setText(v.getContentDescription());

		int[] parentPos = new int[2];
		((View)v.getParent()).getLocationOnScreen(parentPos);
		Toast toast = new Toast(v.getContext());
		toast.setView(layout);
		int[] pos = new int[2];
		v.getLocationOnScreen(pos);
		int yOffset = pos[1] - parentPos[1] + getActionBar().getHeight() + v.getHeight();
		toast.setGravity( Gravity.START|Gravity.TOP, pos[0], yOffset );
		toast.setDuration(Toast.LENGTH_LONG);
		toast.show();
		return false;
	}
}
