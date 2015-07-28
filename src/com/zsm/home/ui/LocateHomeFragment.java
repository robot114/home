package com.zsm.home.ui;

import java.util.Locale;

import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.TextView;

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

public class LocateHomeFragment extends Fragment
				implements OnLocationUpdateListener<Location>, LocationSource,
							OnSuggestionListener {

	private static final int MARK_AS_LOCATION = 2;
	private static final int MARK_AS_HOME = 1;
	private static final LatLng DEFAULT_LOCATION = new LatLng( 116.391469, 39.906708 );
	private static final float DEFAULT_ZOOM_FACTOR = 18.f;
	private MapView mapView;
	private AMap aMap;
	private LocationClient<String, Location> locationClient;
	private SearchView searchView;
	private Location currentLocation;
	private View view;
	
	private FragmentWizard fragmentWizard;

	public LocateHomeFragment( FragmentWizard fw ) {
		super();
		fragmentWizard = fw;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		if( view == null ) {
			view
				= inflater.inflate( R.layout.locate_home_fragment, 
									container, false );
			initLocationClient();
			initMap(savedInstanceState);
		} else {
			if( view.getParent() != null ) {
				((ViewGroup) view.getParent()).removeView(view);
			}
		}
		
		return view;
	}

	private void locateOnMap() {
		Location homeLoca = Preferences.getInstance().getHomeLocation();
		Log.d( "Locate on the map", "current", currentLocation, "home", homeLoca );
		if( currentLocation != null ) {
			updateToPosition( currentLocation,
							  currentLocation == homeLoca 
							  	? MARK_AS_HOME : MARK_AS_LOCATION );
		} else if( homeLoca != null ) {
			updateToPosition( homeLoca, MARK_AS_HOME );
			currentLocation = homeLoca;
			String hat = Preferences.getInstance().getHomeAddress();
			TextView homeAddress
				= (TextView)view.findViewById( R.id.textViewHomeAddress );
			homeAddress.setText(hat);
		} else {
			currentLocation = null;
			toMyLocation();
		}
	}

	private void initMap(Bundle savedInstanceState) {
		mapView = (MapView) view.findViewById(R.id.mapHome);
		mapView.onCreate(savedInstanceState);
		aMap = mapView.getMap();
		aMap.setLocationSource(this);
		aMap.getUiSettings().setMyLocationButtonEnabled(true);
		aMap.setMyLocationEnabled( true );
	}

	private void initLocationClient() {
		if( locationClient == null ) {
			LocationManager locationManager
				= (LocationManager)getActivity()
					.getSystemService(Context.LOCATION_SERVICE);
		
			locationClient = new AndroidLocationClient();
			locationClient.init( locationManager,
								 HomeApplication.LOCATION_DELTA_TIME );
		}
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate( R.menu.locate_home, menu);
		MenuItem searchItem = menu.findItem(R.id.itemSearchHomeAddress);
		searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
		
		OnLocationQueryListener queryListener = new OnLocationQueryListener( );
		searchView.setOnQueryTextListener( queryListener );
		SearchManager sm
			= (SearchManager)getActivity().getSystemService(Context.SEARCH_SERVICE);
		searchView.setSearchableInfo(
				sm.getSearchableInfo( getActivity().getComponentName()));
		searchView.setOnSuggestionListener(this);
		
		MenuItem addressHomeItem = menu.findItem( R.id.itemAddressHome );
		addressHomeItem.setOnMenuItemClickListener( new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				toAddressHome();
				return true;
			}
			
		} );
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		mapView.onResume();
		locateOnMap();
	}

	@Override
	public void onPause() {
		super.onPause();
		mapView.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		locationClient.cancelUpdate( this );
		mapView.onDestroy();
		view = null;
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
		if( currentLocation != null ) {
			 updateToPosition(currentLocation, MARK_AS_HOME );
			 return;
		}
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
		currentLocation = new Location( (String)"?" );
		currentLocation.setLatitude( lat );
		currentLocation.setLongitude( lng );
		currentLocation.setTime( System.currentTimeMillis() );
		currentLocation.setAccuracy( 10.0f );
		
		return true;
	}
	
	private void toAddressHome( ) {
		if( currentLocation == null ) {
			Toast.makeText( getActivity(), R.string.noCurrentLocation,
							Toast.LENGTH_LONG )
				 .show();
			return;
		}
		
		Bundle data = new Bundle();
		data.putParcelable( "HOME_LOCATION", currentLocation );
		data.putString( "LOCATION_ADDRAESS", searchView.getQuery().toString() );
		fragmentWizard.next( this, data );
	}
}
