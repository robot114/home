package com.zsm.android.location;

import java.util.Arrays;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import com.zsm.log.Log;

public class GeocoderLoaderCallbacks implements LoaderCallbacks<Cursor> {

	public static final String KEY_QUERY_NAME = "KEY_NAME";
	public static final int LOCATION_QUERY_ID = 2;
	public static final int NAME_QUERY_ID = 5;
	
	private GeocoderClient client;
	private Context context;

	public GeocoderLoaderCallbacks( Context c,
									GeocoderClient client ) {
		
		this.context = c;
		this.client = client;
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri;
		switch( id ) {
			case LOCATION_QUERY_ID:
				Location l = client.getLocationToQuery();
				uri = getPositionQueryUri(l);
				
				Log.d( "Start to query position", uri );
				break;
			case NAME_QUERY_ID:
				String name = args.getString( KEY_QUERY_NAME );
				name = name == null ? "" : name;
				uri = getNameQueryUri(name);
				break;
			default:
				return null;
		}
		
		return new CursorLoader(context, uri, null, null, null, null);
	}

	private Uri getPositionQueryUri(Location l) {
		Uri uri
			= GeocoderProvider.URI_QUERY_POSITION
				.buildUpon()
				.appendQueryParameter( GeocoderProvider.KEY_LATITUDE,
									   Double.toString(l.getLatitude()) )
				.appendQueryParameter( GeocoderProvider.KEY_LONGITUDE,
									   Double.toString( l.getLongitude() ) )
				.appendQueryParameter( GeocoderProvider.KEY_MAX_RESULTS,
									   "20" )
				.build();
		return uri;
	}

	private Uri getNameQueryUri(String name) {
		Uri uri
			= GeocoderProvider.URI_QUERY_NAME
				.buildUpon()
				.appendQueryParameter( GeocoderProvider.KEY_LOCATION_NAME, name )
				.appendQueryParameter( GeocoderProvider.KEY_MAX_RESULTS,
									   "20" )
				.build();
		return uri;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.d( "Query finished.", "number of data", data.getCount(),
				"columns", Arrays.toString( data.getColumnNames() ) );
		client.getDisplayAdapter().changeCursor(data);
		client.getDisplayAdapter().notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		client.getDisplayAdapter().changeCursor(null);
	}

}
