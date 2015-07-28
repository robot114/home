package com.zsm.android.location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.provider.BaseColumns;

import com.zsm.android.AndroidUtility;
import com.zsm.log.Log;

public class GeocoderProvider extends ContentProvider {

	private static final String KEY_UPPER_RIGHT_LONGITUDE = "upperRightLongitude";
	private static final String KEY_UPPER_RIGHT_LATITUDE = "upperRightLatitude";
	private static final String KEY_LOWER_LEFT_LONGITUDE = "lowerLeftLongitude";
	private static final String KEY_LOWER_LEFT_LATITUDE = "lowerLeftLatitude";
	
	public static final String COLUMN_LATITUDE = "LATITUDE";
	public static final String COLUMN_LONGITUDE = "LONGITUDE";
	private static final String[] SIMPLE_RESULT_COLUMN_NAMES
		= { BaseColumns._ID,
			COLUMN_LATITUDE,
			COLUMN_LONGITUDE,
			SearchManager.SUGGEST_COLUMN_TEXT_1,
			SearchManager.SUGGEST_COLUMN_TEXT_2 };
	
	private static final String KEY_MAX_RESULTS = "maxRes";
	private static final String KEY_LOCATION_NAME = "name";
	private static final String KEY_LONGITUDE = "longitude";
	private static final String KEY_LATITUDE = "latitude";
	private static final int DEFAULT_MAX_RESULTS = 10;
	
	private static final int BY_POSITION = 0x1;
	private static final int BY_LOCATION_NAME = 0x2;
	private static final int BY_LOCATION_NAME_AND_RANGE = 0x3;
	private static final int NO_SUGGESTION = 0x10;
	private static final int SUGGEST_BY_POSITION = 0x11;
	private static final int SUGGEST_BY_LOCATION_NAME = 0x12;
	private static final int SUGGEST_BY_LOCATION_NAME_AND_RANGE = 0x13;
	
	private static final String GEOCODER_PROVIDER_URI = "com.zsm.geocoderprovider";
	private static final String GEOCODER_PATH = "geocoder";

	private static UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher( UriMatcher.NO_MATCH );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
				   		   SearchManager.SUGGEST_URI_PATH_QUERY
				   		   		+ "/position",
				   		   SUGGEST_BY_POSITION );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
				   		   SearchManager.SUGGEST_URI_PATH_QUERY,
				   		   NO_SUGGESTION );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
				   		   SearchManager.SUGGEST_URI_PATH_QUERY+"/*",
				   		   SUGGEST_BY_LOCATION_NAME );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
				   		   SearchManager.SUGGEST_URI_PATH_QUERY
				   		   		+ "/location_name_position",
				   		   SUGGEST_BY_LOCATION_NAME_AND_RANGE );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
						   GEOCODER_PATH + "/position",
				   		   BY_POSITION );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
						   GEOCODER_PATH + "/location_name",
				   		   BY_LOCATION_NAME );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
						   GEOCODER_PATH + "/location_name_position",
				   		   BY_LOCATION_NAME_AND_RANGE );
	}

	private Geocoder gc;
	
	@Override
	public boolean onCreate() {
		gc = new Geocoder( getContext(), Locale.getDefault() );
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
						String[] selectionArgs, String sortOrder) {
		
		Log.d( "Query for geo coder. ", "uri", uri, "projection", projection,
				"selection", selection, "selectionArgs", selectionArgs,
				"sortOrder", sortOrder );
		List<Address> addresses = null;
		if( AndroidUtility.isAndroidEmulator() ) {
			addresses = fakeAddress();
		} else {
			try {
				addresses = getAddressList(uri);
			} catch (IOException e) {
				Log.e(e, "Failed to get addresses!", "uri", uri );
				return null;
			}
		}
		MatrixCursor c = listToCursor(addresses);
		return c;
	}

	private List<Address> fakeAddress() {
		List<Address> l = new ArrayList<Address>();
		Address a = new Address(Locale.getDefault() );
		a.setLatitude( 119 );
		a.setLongitude( 36 );
		a.setAddressLine( 0, "ABCDEF" );
		a.setAddressLine( 1, "AAABBB" );
		l.add( a );
		a = new Address(Locale.getDefault() );
		a.setLatitude( 119 );
		a.setLongitude( 36 );
		a.setAddressLine( 0, "KKKKKK" );
		l.add( a );
		return l;
	}

	private MatrixCursor listToCursor(List<Address> addresses) {
		MatrixCursor c = new MatrixCursor( SIMPLE_RESULT_COLUMN_NAMES );
		int id = 0;
		for( Address address : addresses ) {
			MatrixCursor.RowBuilder row = c.newRow();
			row.add( id++ );
			row.add( address.getLatitude() );
			row.add( address.getLongitude() );
			row.add( getAddressString(address) );
			row.add( getAddressInfo(address) );
		}
		return c;
	}

	private String getAddressInfo(Address address) {
		ArrayList<String> v = new ArrayList<String>();
		v.add( address.getAdminArea() );
		v.add( address.getSubAdminArea() );
		v.add( address.getLocality() );
		v.add( address.getSubLocality() );
		v.add( address.getThoroughfare() );
		boolean firstTime = true;
		StringBuilder sbu = new StringBuilder();
		for( String text : v ) {
			if( text != null ) {
				if( !firstTime ) {
					sbu.append( ", " );
				}
				firstTime = false;
				sbu.append( text );
			}
		}
		String addressStr = sbu.toString();
		return addressStr;
	}

	private String getAddressString(Address address) {
		StringBuilder sbu = new StringBuilder();
		for( int i = 0; i <= address.getMaxAddressLineIndex(); i++ ) {
			if( i > 0 ) {
				sbu.append( ", " );
			}
			sbu.append( address.getAddressLine( i ) );
		}
		String featureName = address.getFeatureName();
		if( featureName != null ) {
			sbu.append( "( " ).append( featureName ).append( " )" );
		}
		String addressStr = sbu.toString();
		return addressStr;
	}

	private List<Address> getAddressList(Uri uri) throws IOException {
		List<Address> addresses;
		int maxRes = getMaxResults(uri);
		String name = null;
		switch( uriMatcher.match(uri) ) {
			case SUGGEST_BY_POSITION:
			case BY_POSITION:
				double latitude = getDouble(uri, KEY_LATITUDE);
				double longitude = getDouble(uri, KEY_LONGITUDE);
				addresses = gc.getFromLocation( latitude, longitude, maxRes);
				break;
			case SUGGEST_BY_LOCATION_NAME:
			case BY_LOCATION_NAME:
				name = uri.getLastPathSegment();
				addresses = gc.getFromLocationName(name, maxRes);
				break;
			case SUGGEST_BY_LOCATION_NAME_AND_RANGE:
			case BY_LOCATION_NAME_AND_RANGE:
				name = uri.getLastPathSegment();
				double lowerLeftLatitude
					= getDouble(uri, KEY_LOWER_LEFT_LATITUDE);
				double lowerLeftLongitude
					= getDouble(uri, KEY_LOWER_LEFT_LONGITUDE);
				double upperRightLatitude
					= getDouble(uri, KEY_UPPER_RIGHT_LATITUDE);
				double upperRightLongitude
					= getDouble(uri, KEY_UPPER_RIGHT_LONGITUDE);
				addresses
					= gc.getFromLocationName(name, maxRes, lowerLeftLatitude,
											 lowerLeftLongitude, upperRightLatitude,
											 upperRightLongitude);
				break;
			case NO_SUGGESTION:
				addresses = null;
				break;
			default:
				throw new IllegalArgumentException( "Unsupport URI: " + uri );
		}
		
		return addresses;
	}

	private int getMaxResults(Uri uri) {
		int maxRes = DEFAULT_MAX_RESULTS;
		String str = uri.getQueryParameter( KEY_MAX_RESULTS );
		try {
			maxRes = Integer.valueOf( str );
		} catch ( Exception e ) {
			Log.w( "No max res number or wrong max res number", "uri", uri );
		}
		return maxRes;
	}

	private double getDouble(Uri uri, String name) {
		String str = uri.getQueryParameter( name );
		return Double.valueOf(str);
	}

	@Override
	public String getType(Uri uri) {
		return SearchManager.SUGGEST_MIME_TYPE;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException( "This is a readonly content provider: " + uri );
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException( "This is a readonly content provider: " + uri );
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		throw new UnsupportedOperationException( "This is a readonly content provider: " + uri );
	}

}
