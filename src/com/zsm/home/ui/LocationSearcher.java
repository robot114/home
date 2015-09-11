package com.zsm.home.ui;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.SearchManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.CursorAdapter;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.SimpleCursorAdapter;

import com.zsm.android.location.GeocoderClient;
import com.zsm.android.location.GeocoderLoaderCallbacks;
import com.zsm.android.location.GeocoderProvider;
import com.zsm.home.R;
import com.zsm.home.ui.LocateHomeFragment.CurrentLocation;
import com.zsm.log.Log;

class LocationSearcher
		implements OnQueryTextListener, OnSuggestionListener,
				   OnFocusChangeListener, GeocoderClient {

	private static final int LOCATION_BY_NAME_QUERY_ID = 4;
	private static final String NEAR_ENOUGH_DISTANCE = "10000";
	
	private LocateHomeFragment fragment;
	private SearchView searchView;
	private boolean queryForSearchQueryChange;
	private GeocoderLoaderCallbacks geoLoaderCallbacks;
	private QueryScheduler queryTask;
	private Timer queryTimer;

	LocationSearcher( LocateHomeFragment fragment, SearchView sv ) {
		this.fragment = fragment;
		this.searchView = sv;
		initSearchView();
	}

	private void initSearchView() {
		queryForSearchQueryChange = true;
		
		searchView.setSubmitButtonEnabled( false );
		
		searchView.setOnQueryTextListener( this );
		Activity activity = fragment.getActivity();
		SearchManager sm
			= (SearchManager)activity
				.getSystemService(Context.SEARCH_SERVICE);
		
		searchView.setSearchableInfo(
				sm.getSearchableInfo( activity.getComponentName()));
		searchView.setSuggestionsAdapter( new SimpleCursorAdapter( activity,
											R.layout.location_suggestion_item, null,
											new String[]{ GeocoderProvider.COLUMN_ADDRESS,
														  GeocoderProvider.COLUMN_ADDRESS_DESC },
											new int[]{ R.id.text1, R.id.text2 },
											0 ) );
		searchView.setOnSuggestionListener( this );
		searchView.setOnQueryTextFocusChangeListener( this );
		
		geoLoaderCallbacks 
			= new GeocoderLoaderCallbacks( activity, this );
		searchView.setOnSearchClickListener( new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int sw = fragment.getActivity().getWindow().getDecorView().getWidth();
				searchView.setMaxWidth( sw - fragment.getLogoAndToAddressWidth() );
			}
			
		} );
	}

	public void destroy() {
		geoLoaderCallbacks = null;
		LoaderManager loaderManager = fragment.getLoaderManager();
		if(loaderManager.getLoader(GeocoderLoaderCallbacks.LOCATION_QUERY_ID) != null ) {
			loaderManager.destroyLoader(GeocoderLoaderCallbacks.LOCATION_QUERY_ID);
		}
		if(loaderManager.getLoader(GeocoderLoaderCallbacks.NAME_QUERY_ID) != null ) {
			loaderManager.destroyLoader(GeocoderLoaderCallbacks.NAME_QUERY_ID);
		}
		if( queryTimer != null ) {
			queryTask.clearupTimer();
		}
	}
	
	void setSearchQuery(String address, boolean query) {
		if( queryTask != null ) {
			queryTask.clearupTimer();
		}
		queryForSearchQueryChange  = query;
		searchView.setQuery( address, false );
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return startQuery( query );
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		return startQuery( newText );
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if( hasFocus ) {
			startQuery( searchView.getQuery().toString() );
		}
	}
	
	private boolean startQuery( final String name ) {
		if( !queryForSearchQueryChange ) {
			searchView.getSuggestionsAdapter().changeCursor(null);
			queryForSearchQueryChange = true;
			return true;
		}
		
		if( queryTimer == null ) {
			synchronized(this){
				queryTask = new QueryScheduler();
				queryTask.setQueryName( name );
				queryTimer = new Timer( "Query" );
				queryTimer.schedule( queryTask, 100, 100 );
			}
		}
		queryTask.setQueryName( name );
		
		return true;
	}

	@Override
	public boolean onSuggestionSelect(int position) {
		return false;
	}

	@Override
	public boolean onSuggestionClick(int position) {
		Cursor cursor = (Cursor)(searchView.getSuggestionsAdapter().getItem(position));
		int textColIndex = cursor.getColumnIndex( SearchManager.SUGGEST_COLUMN_TEXT_1 );
		final String address = cursor.getString( textColIndex );
		int latColIndex
			= cursor.getColumnIndex( GeocoderProvider.COLUMN_LATITUDE );
		double lat = cursor.getDouble(latColIndex);
		int lngColIndex
			= cursor.getColumnIndex( GeocoderProvider.COLUMN_LONGITUDE );
		double lng = cursor.getDouble(lngColIndex);
		
//		fragment.setCurrentLocationAndUpdateMap( lat, lng, address );
		
		fragment.getLoaderManager()
			.restartLoader( 
					LOCATION_BY_NAME_QUERY_ID, null, 
					new PositionOfAddressLoaderCallbacks( address, lat, lng ) );
		return true;
	}

	@Override
	public Location getLocationToQuery() {
		return fragment.getCurrentLocation().getLocation();
	}

	@Override
	public CursorAdapter getDisplayAdapter() {
		return searchView.getSuggestionsAdapter();
	}

	public void setViewIconified(boolean b) {
		searchView.setIconified(b);
	}

	public CharSequence getQueryText() {
		return searchView.getQuery();
	}

	public CursorAdapter getSuggestionsAdapter() {
		return searchView.getSuggestionsAdapter();
	}
	
	private final class QueryScheduler extends TimerTask {

		private String queryName;
		private boolean queryChanged;
		boolean firstTime = true;
		
		@Override
		public void run() {
			if( firstTime ) {
				Looper.prepare();
				firstTime = false;
			}
			
			if( queryChanged ) {
				queryChanged = false;
			} else {
				query();
				clearupTimer();
			}
			
		}

		private void clearupTimer() {
			if( queryTimer == null ) {
				return;
			}
			synchronized( LocationSearcher.this ) {
				queryTask.cancel();
				queryTimer.cancel();
				queryTimer = null;
				queryTask = null;
			}
		}
		
		private void setQueryName(String name) {
			this.queryName = name;
			queryChanged = true;
		}
		
		private void query() {
			if( shouldNotQuery() ) {
				return;
			}
			
			if( TextUtils.isEmpty(queryName) ) {
				CurrentLocation currentLocation = fragment.getCurrentLocation();
				if( currentLocation .getLocation() == null ) {

				} else {
					Log.d( "Start to query by the position. ",
							"lat", currentLocation.getLocation().getLatitude(),
							"lng", currentLocation.getLocation().getLongitude() );
					fragment.getLoaderManager()
						.restartLoader( GeocoderLoaderCallbacks.LOCATION_QUERY_ID,
										null, geoLoaderCallbacks );
				}
			} else {
				Log.d( "Set location name to query, and waiting for querying. ",
						"name", queryName );
				Bundle bundle = new Bundle();
				bundle.putString( GeocoderLoaderCallbacks.KEY_QUERY_NAME, queryName );
				fragment.getLoaderManager()
					.restartLoader( GeocoderLoaderCallbacks.NAME_QUERY_ID,
									bundle, geoLoaderCallbacks );
			}
		}

		private boolean shouldNotQuery() {
			return !queryForSearchQueryChange;
		}
	}
	
	private class PositionOfAddressLoaderCallbacks 
		implements LoaderCallbacks<Cursor> {

		private String address;
		private double latitude;
		private double longitude;

		PositionOfAddressLoaderCallbacks( String address, double lat, double lng ) {
			this.address = address;
			this.latitude = lat;
			this.longitude = lng;
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			Uri uri = getPositionQueryUri( address, latitude, longitude );
			Log.d( "Start to query position by address", uri );
			return new CursorLoader(fragment.getActivity(),
									uri, null, null, null, null);
		}

		private Uri getPositionQueryUri(String name, double lat, double lng) {
			Uri uri
				= GeocoderProvider.URI_QUERY_POSITION_OF_ADDRESS
					.buildUpon()
					.appendQueryParameter( GeocoderProvider.KEY_LOCATION_NAME, name )
					.appendQueryParameter( GeocoderProvider.KEY_LATITUDE,
											Double.toString( latitude ) )
					.appendQueryParameter( GeocoderProvider.KEY_LONGITUDE,
											Double.toString( longitude ) )
					.appendQueryParameter( GeocoderProvider.KEY_DISTANCE_IN_METERS,
											NEAR_ENOUGH_DISTANCE )
					.appendQueryParameter( GeocoderProvider.KEY_MAX_RESULTS,
											"10" )
					.build();
			return uri;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			double lat = latitude;
			double lng = longitude;
			if( data != null && data.getCount() > 0 ) {
				data.moveToFirst();
				int latColIndex
					= data.getColumnIndex( GeocoderProvider.COLUMN_LATITUDE );
				lat = data.getDouble(latColIndex);
				int lngColIndex
					= data.getColumnIndex( GeocoderProvider.COLUMN_LONGITUDE );
				lng = data.getDouble(lngColIndex);
			} else {
				Log.d( "Cannot get exact position of address.", address );
			}
			data.close();

			setSearchQuery(address, false);

			Log.d( "Clicked address: ", address, "lat", lat, "lng", lng );
			fragment.setCurrentLocationAndUpdateMap( lat, lng, address );
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
		}
	}
}
