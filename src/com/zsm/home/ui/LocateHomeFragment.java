package com.zsm.home.ui;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptor;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;
import com.zsm.home.R;
import com.zsm.home.app.HomeApplication;
import com.zsm.home.preferences.Preferences;
import com.zsm.location.GeodeticCoordinate;
import com.zsm.location.GeodeticCoordinate.TYPE;
import com.zsm.location.LocationSerivce;
import com.zsm.location.LocationUpdateListener;
import com.zsm.location.android.AndroidLocationService;
import com.zsm.location.android.LocationUtility;
import com.zsm.log.Log;

public class LocateHomeFragment extends Fragment
				implements LocationUpdateListener<Location>, LocationSource {

	/**
	 * The Geodetic Coordinate System for this class is WGS84
	 * 
	 * @author zsm
	 *
	 */
	public static class CurrentLocation {
		// The Geodetic Coordinate System for this class is WGS84
		final static private TYPE gcsType = TYPE.WGS84;
		private Location location;
		private int mark;
		private String address;

		public CurrentLocation() {
			address = "";
		}

		public Location getLocation() {
			return location;
		}

		public int getMark() {
			return mark;
		}

		public void set(Location l, int mark, @NonNull String address) {
			location = l;
			this.mark = mark;
			this.address = address;
		}

		public String getAddress() {
			return address;
		}

		@Override
		public String toString() {
			return location + ", mark: " + mark
					+ ", address: " + address;
		}
	}

	private GeodeticCoordinate gcForTrans
				= new GeodeticCoordinate( 0, 0, CurrentLocation.gcsType );
	
	static final int MARK_AS_LOCATION = 2;
	static final int MARK_AS_HOME = 1;
	static final int NO_MARK = 0;
	private static final LatLng DEFAULT_LOCATION = new LatLng( 39.906708, 116.391469 );
	private static final float DEFAULT_ZOOM_FACTOR = 18.f;
	private MapView mapView;
	private AMap aMap;
	private LocationSerivce<String, Location> locationService;
	private CurrentLocation currentLocation = new CurrentLocation();
	private View view;
	
	private FragmentWizard fragmentWizard;
	private Bundle contextData;

	private static int logoAndToAddressWidth = 0;
	private static BitmapDescriptor markHome;
	
	private Location homeLocation;
	private String homeAddress;
	private LocationSearcher searcher;

	public LocateHomeFragment( FragmentWizard fw ) {
		super();
		fragmentWizard = fw;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		homeLocation = Preferences.getInstance().getHomeLocation();
		homeAddress = Preferences.getInstance().getHomeAddress();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		if( view == null ) {
			view
				= inflater.inflate( R.layout.locate_home_fragment, 
									container, false );
			initLocationService();
			initMap(savedInstanceState);
			view.findViewById( R.id.imageViewToHome )
				.setOnClickListener( new OnClickListener() {
					@Override
					public void onClick(View v) {
						if( homeLocation != null ) {
							updateCurrentLocation( homeLocation,
												   MARK_AS_HOME,
												   homeAddress );
						}
					}
				} );
		} else {
			if( view.getParent() != null ) {
				((ViewGroup) view.getParent()).removeView(view);
			}
		}
		
		return view;
	}

	private void locateOnMap() {
		Location cl = currentLocation.getLocation();
		Log.d( "Locate on the map", "current", currentLocation,
				"home", homeLocation, "home addr", homeAddress );
		if( cl != null ) {
			String ca = currentLocation.getAddress();
			int mark;
			String ad;
			if( LocationUtility.samePosition(homeLocation, cl ) 
				&& ( ca.length() == 0 || ca.equals( homeAddress ) ) ) {
				
				mark = MARK_AS_HOME;
				ad = homeAddress;
			} else {
				mark = MARK_AS_LOCATION;
				ad = ca;
			}
			updateToPosition(cl, mark);
			currentLocation.set(cl, mark, ad);
		} else if( homeLocation != null ) {
			updateToPosition( homeLocation, MARK_AS_HOME );
			currentLocation.set(homeLocation, MARK_AS_HOME, homeAddress );
		} else {
			updateToPosition( DEFAULT_LOCATION.latitude,
							  DEFAULT_LOCATION.longitude,
							  NO_MARK );
			currentLocation.set(null, NO_MARK, "" );
			toMyLocation();
		}
	}

	private void setAddressText() {
		String address = null;
		if( contextData != null ) {
			address = contextData.getString( 
						HomeApplication.KEY_HOME_LOCATION_ADDRESS );
		}
		
		if( !TextUtils.isEmpty( address ) ) {
			// Back from the address fragment, and do some input for the address
			searcher.setViewIconified(false);
			searcher.setSearchQuery(address, false);
		} 
		TextView homeAddressView
			= (TextView)view.findViewById( R.id.textViewHomeAddress );
		homeAddressView.setText(homeAddress);
	}

	private void initMap(Bundle savedInstanceState) {
		mapView = (MapView) view.findViewById(R.id.mapHome);
		mapView.onCreate(savedInstanceState);
		aMap = mapView.getMap();
		aMap.getUiSettings().setMyLocationButtonEnabled(true);
		aMap.setMyLocationEnabled( true );
		aMap.setLocationSource(this);
	}

	private void initLocationService() {
		if( locationService == null ) {
			LocationManager locationManager
				= (LocationManager)getActivity()
					.getSystemService(Context.LOCATION_SERVICE);
		
			locationService = new AndroidLocationService();
			locationService.init( getActivity(),
								  locationManager,
								  HomeApplication.LOCATION_DELTA_TIME,
								  HomeApplication.LOCATION_DELTA_TIME*3 );
		}
	}

	private void toMyLocation() {
		Location lastLocation
			= locationService
				.getLastUpdatedLocation( currentLocation.getLocation() );
		
		if ( lastLocation != null ) {
			updateToPosition( lastLocation, MARK_AS_LOCATION );
			currentLocation.set(lastLocation, MARK_AS_LOCATION, "");
		} else {
			locationService.startUpdate(this);
		}
	}

	private void updateToPosition(Location loca, int markFlag ) {
		updateToPosition(loca.getLatitude(), loca.getLongitude(), markFlag);
	}

	/**
	 * 
	 * @param latitude in WGS84
	 * @param longitude in WGS84
	 * @param markFlag
	 */
	private void updateToPosition(double latitude, double longitude, int markFlag) {
		
		gcForTrans.setData(latitude, longitude, CurrentLocation.gcsType );
		
		Log.d( "Move to the position", "position", gcForTrans,
			   "markFlag", markFlag );
		
		gcForTrans.transformTo( TYPE.GCJ02 );
		Log.d( "Position in GCJ", gcForTrans );
		
		aMap.clear();
		float zoom;
		if( markFlag == NO_MARK ) {
			zoom = aMap.getCameraPosition().zoom;
		} else {
			zoom = DEFAULT_ZOOM_FACTOR;
		}
		
		LatLng latLng
			= new LatLng( gcForTrans.getLatitude(), gcForTrans.getLongitude() );
		CameraUpdate cu
			= CameraUpdateFactory
				.newCameraPosition( new CameraPosition( latLng, zoom, 0, 0));
		
		aMap.moveCamera(cu);
		if( markFlag == NO_MARK ) {
			return;
		}
		
		MarkerOptions marker
			= new MarkerOptions().position( latLng );
		if( markFlag == MARK_AS_HOME ) {
			marker
				.setGps( false )
				.icon( getMarkHome() );
		}
		aMap.addMarker(marker);
	}

	private BitmapDescriptor getMarkHome() {
		if( markHome == null ) {
			markHome = BitmapDescriptorFactory.fromResource( R.drawable.home );
		}
		
		return markHome;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setCustomView( R.layout.action_bar_locate_home );
		final View v = actionBar.getCustomView();
		
		initSearchView(v);
		View toAddressHome = v.findViewById( R.id.imageViewToAddressHome );
		toAddressHome.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {
				toAddressHome();
			}
			
		} );
		
		setAddressText();
	}

	private void initSearchView(View v) {
		SearchView searchView
			= (SearchView) v.findViewById( R.id.searchViewSearchHome );
		searcher = new LocationSearcher(this, searchView);
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
		// In case the searchview not be clicked and logoAndToAddressWidth
		// not initialized
		getLogoAndToAddressWidth();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		locationService.cancelUpdate( );
		locationService = null;
		mapView.onDestroy();
		view = null;
		markHome = null;
		searcher.destroy();
	}

	@Override
	public void onUpdate(Location location) {
		updateCurrentLocation(location, MARK_AS_LOCATION, "");
	}

	private void updateCurrentLocation(Location location, int mark, String address) {
		updateToPosition(location, mark);
		currentLocation.set(location, mark, address);
		searcher.setViewIconified(false);
		searcher.setSearchQuery(address, true);
	}

	@Override
	public void onCancel( CANCEL_REASON reason ) {
		if( reason == CANCEL_REASON.TIME_OUT ) {
			Toast
				.makeText(getActivity(), R.string.locateCurrentTimeout,
						  Toast.LENGTH_LONG )
				.show();
		}
	}

	@Override
	public void activate(OnLocationChangedListener l) {
		Log.d("To my current location" );
		toMyLocation();
	}

	@Override
	public void deactivate() {
		Log.d("Remove current location listener");
		locationService.cancelUpdate( );
	}

	private void toAddressHome( ) {
		if( currentLocation.getLocation() == null ) {
			Toast.makeText( getActivity(), R.string.noCurrentLocation,
							Toast.LENGTH_LONG )
				 .show();
			return;
		}
		
		Bundle data = new Bundle();
		data.putParcelable( HomeApplication.KEY_HOME_LOCATION,
							currentLocation.getLocation() );
		String add;
		switch( currentLocation.getMark() ) {
			case MARK_AS_LOCATION:
				add = searcher.getQueryText().toString();
				if( add.isEmpty() ) {
					add = homeAddress;
				}
				break;
			case MARK_AS_HOME:
				add = homeAddress;
				break;
			default:
				add = "";
				break;
		}
		data.putString( HomeApplication.KEY_HOME_LOCATION_ADDRESS, add );
		fragmentWizard.next( this, data );
	}

	public void setHomeAddressData(Bundle data) {
		contextData = data;
	}

	int getLogoAndToAddressWidth() {
		if( logoAndToAddressWidth == 0 ) {
			View abv = getActivity().getActionBar().getCustomView();
			View logo = abv.findViewById( R.id.imageViewLocateHomeLogo );
			View ta = abv.findViewById( R.id.imageViewToAddressHome );
			logoAndToAddressWidth = logo.getWidth() + ta.getWidth();
		}
		
		return logoAndToAddressWidth;
	}

	CurrentLocation getCurrentLocation() {
		return currentLocation;
	}

	void setCurrentLocationAndUpdateMap( double lat, double lng, String address ) {
		updateToPosition( lat, lng, MARK_AS_LOCATION );
		Location l = new Location( (String)"?" );
		l.setLatitude( lat );
		l.setLongitude( lng );
		// Time of the location MUST be old enough to make the location update
		// happens when the 'To My Location' button pressed on the map.
		l.setTime( 0 );
		l.setAccuracy( 10.0f );
		currentLocation.set(l, MARK_AS_LOCATION, address);
	}
}
