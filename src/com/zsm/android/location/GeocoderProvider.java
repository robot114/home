package com.zsm.android.location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.provider.BaseColumns;

import com.zsm.android.AndroidUtility;
import com.zsm.location.GeodeticCoordinate;
import com.zsm.location.GeodeticCoordinate.TYPE;
import com.zsm.location.android.LocationUtility;
import com.zsm.log.Log;

public class GeocoderProvider extends ContentProvider {

//	private static final double NEAR_TOILANCE = LocationUtility.METER_TO_DEGREE*1000;
	private static final String COLUMN_ID = BaseColumns._ID;
	public static final String COLUMN_ADDRESS = SearchManager.SUGGEST_COLUMN_TEXT_1;
	public static final String COLUMN_ADDRESS_DESC = SearchManager.SUGGEST_COLUMN_TEXT_2;
	
	public static final String COLUMN_LATITUDE = "LATITUDE";
	public static final String COLUMN_LONGITUDE = "LONGITUDE";
	public static final String[] COLUMN_NAMES
		= { COLUMN_ID, COLUMN_LATITUDE, COLUMN_LONGITUDE,
			COLUMN_ADDRESS, COLUMN_ADDRESS_DESC };
	
	public static final Uri URI_QUERY_POSITION;
	public static final Uri URI_QUERY_NAME;
	public static final Uri URI_QUERY_LOCATION_NAME_AND_RANGE;
	public static final Uri URI_QUERY_POSITION_OF_ADDRESS;
	public static final String KEY_MAX_RESULTS = "maxRes";
	public static final String KEY_LOCATION_NAME = "name";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_UPPER_RIGHT_LONGITUDE = "upperRightLongitude";
	public static final String KEY_UPPER_RIGHT_LATITUDE = "upperRightLatitude";
	public static final String KEY_LOWER_LEFT_LONGITUDE = "lowerLeftLongitude";
	public static final String KEY_LOWER_LEFT_LATITUDE = "lowerLeftLatitude";
	public static final String KEY_DISTANCE_IN_METERS = "distance_in_meters";
	public static final String KEY_GEO_COORDINATE = "geo_coordinate";
	public static final String VALUE_GEO_COOR_WGS
									= TYPE.WGS84.name();
	public static final String VALUE_GEO_COOR_GCJ
									= TYPE.GCJ02.name();
	public static final String VALUE_GEO_COOR_BD
									= TYPE.BD09.name();
	
	private static final int DEFAULT_MAX_RESULTS = 10;
	private static final TYPE DEFAULT_COORDINATE_SYSTEM = TYPE.WGS84;
	
	private static final int BY_POSITION = 0x1;
	private static final int BY_LOCATION_NAME = 0x2;
	private static final int BY_LOCATION_NAME_AND_RANGE = 0x3;
	private static final int BY_POSITION_OF_ADDRESS = 0x4;
	private static final int NO_SUGGESTION = 0x10;
	private static final int SUGGEST_BY_POSITION = 0x11;
	private static final int SUGGEST_BY_LOCATION_NAME = 0x12;
	private static final int SUGGEST_BY_LOCATION_NAME_AND_RANGE = 0x13;
	
	private static final String PATH_POSITION = "position";
	private static final String PATH_POSITION_OF_ADDRESS = "position_of_address";
	private static final String PATH_LOCATION_NAME = "location_name";
	private static final String PATH_LOCATION_NAME_AND_RANGE = "location_name_position";
	private static final String GEOCODER_PROVIDER_URI = "com.zsm.geocoderprovider";
	private static final String GEOCODER_PATH = "geocoder";
	private static final String QUERY_PATH_POSITION
									= GEOCODER_PATH + "/" + PATH_POSITION;
	private static final String QUERY_PATH_POSITION_OF_ADDRESS
									= GEOCODER_PATH + "/" + PATH_POSITION_OF_ADDRESS;

	private static UriMatcher uriMatcher;
	
	static {
		uriMatcher = new UriMatcher( UriMatcher.NO_MATCH );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
				   		   SearchManager.SUGGEST_URI_PATH_QUERY
				   		   		+ "/" + PATH_POSITION,
				   		   SUGGEST_BY_POSITION );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
				   		   SearchManager.SUGGEST_URI_PATH_QUERY,
				   		   NO_SUGGESTION );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
				   		   SearchManager.SUGGEST_URI_PATH_QUERY+"/*",
				   		   SUGGEST_BY_LOCATION_NAME );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
				   		   SearchManager.SUGGEST_URI_PATH_QUERY
				   		   		+ "/" + PATH_LOCATION_NAME_AND_RANGE,
				   		   SUGGEST_BY_LOCATION_NAME_AND_RANGE );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
						   QUERY_PATH_POSITION,
				   		   BY_POSITION );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
						   QUERY_PATH_POSITION_OF_ADDRESS,
				   		   BY_POSITION_OF_ADDRESS );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
						   GEOCODER_PATH + "/" + PATH_LOCATION_NAME,
				   		   BY_LOCATION_NAME );
		uriMatcher.addURI( GEOCODER_PROVIDER_URI,
						   GEOCODER_PATH + "/" + PATH_LOCATION_NAME_AND_RANGE,
				   		   BY_LOCATION_NAME_AND_RANGE );
		
		URI_QUERY_POSITION
			= new Uri.Builder()
				.scheme( "content" )
				.authority( GEOCODER_PROVIDER_URI )
				.appendPath( GEOCODER_PATH )
				.appendPath( PATH_POSITION )
				.build();
		
		URI_QUERY_NAME
			= new Uri.Builder()
				.scheme( "content" )
				.authority( GEOCODER_PROVIDER_URI )
				.appendPath( GEOCODER_PATH )
				.appendPath( PATH_LOCATION_NAME )
				.build();
		
		URI_QUERY_POSITION_OF_ADDRESS
			= new Uri.Builder()
				.scheme( "content" )
				.authority( GEOCODER_PROVIDER_URI )
				.appendPath( GEOCODER_PATH )
				.appendPath( PATH_POSITION_OF_ADDRESS )
				.build();
	
		URI_QUERY_LOCATION_NAME_AND_RANGE
			= new Uri.Builder()
				.scheme( "content" )
				.authority( GEOCODER_PROVIDER_URI )
				.appendPath( GEOCODER_PATH )
				.appendPath( PATH_LOCATION_NAME_AND_RANGE )
				.build();
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
		Cursor c;
		if( AndroidUtility.isAndroidEmulator() ) {
			addresses = fakeAddress( uri );
			return listToCursor(addresses, TYPE.WGS84, TYPE.WGS84);
		} else {
			try {
				c = query(uri);
			} catch (IOException e) {
				Log.e(e, "Failed to get addresses!", "uri", uri );
				return null;
			}
		}
		 
		return c;
	}

	private List<Address> fakeAddress( Uri uri ) {
		List<Address> l = new ArrayList<Address>();
		Address a = new Address(Locale.getDefault() );
		a.setLatitude( 36 );
		a.setLongitude( 119 );
		a.setAddressLine( 0, "ABCDEF" );
		a.setAddressLine( 1, "AAABBB" );
		l.add( a );
		a = new Address(Locale.getDefault() );
		a.setLatitude( 36 );
		a.setLongitude( 120 );
		a.setAddressLine( 0, "KKKKKK" );
		l.add( a );
		return l;
	}

	private MatrixCursor listToCursor(List<Address> addresses, TYPE type, TYPE target) {
		if( addresses == null ) {
			return null;
		}
		
		MatrixCursor c = new MatrixCursor( COLUMN_NAMES );
		int id = 0;
		
		GeodeticCoordinate geoCor = new GeodeticCoordinate( 0, 0, type );
		for( Address address : addresses ) {
			geoCor.setData(address.getLatitude(), address.getLongitude(), type);
			geoCor.transformTo(target);
			Log.d( "Get address at.", "lat", address.getLatitude(), "lng",
				   address.getLongitude(),
				   "address line num", address.getMaxAddressLineIndex()+1, 
				   "address0", address.getAddressLine( 0 ) );
			String addressInfo = getAddressInfo(address);
			String addressFeature = getAddressFeature(address);

			for( int i = 0; i <= address.getMaxAddressLineIndex(); i++ ) {
				String addressLine = address.getAddressLine( i );
				MatrixCursor.RowBuilder row = c.newRow();
				row.add( id );
				addAddress(addressLine,
						   geoCor.getLatitude(), geoCor.getLongitude(), addressInfo,
						   addressFeature, row);
				id++;
			}
		}
		return c;
	}

	private Address exactOneInList( List<Address> addresses, String addressLine ) {
		
		Log.d( "Result addresses for name", "count", addresses.size(), "name", addressLine );

		for( Address address : addresses ) {
			Log.d( "Address from list for name.", "lat", address.getLatitude(),
					"lng", address.getLongitude(),
					"address line num", address.getMaxAddressLineIndex()+1,
					"address feature", address.getFeatureName() );
			if( address.getFeatureName().contains(addressLine) ) {
				return address;
			}
			for( int i = 0; i <= address.getMaxAddressLineIndex(); i++ ) {
				if( address.getAddressLine(i).contains(addressLine) ) {
					return address;
				}
			}
		}
		
		return null;
	}
	
	private Cursor positionOfAddress( List<Address> addresses,
									  String addressLine,
									  TYPE type, TYPE target ) {
		
		MatrixCursor c = new MatrixCursor( COLUMN_NAMES );
		Address address = exactOneInList( addresses, addressLine );
		if(address != null ) {
			MatrixCursor.RowBuilder row = c.newRow();
			row.add( 1 );
			GeodeticCoordinate geoCor = new GeodeticCoordinate( 0, 0, type );
			geoCor.setData(address.getLatitude(), address.getLongitude(), type);
			geoCor.transformTo(target);
			addAddress( addressLine, geoCor.getLatitude(), geoCor.getLongitude(), 
						getAddressInfo(address), getAddressFeature(address), row  );
			Log.d( "Get exact position for address.",
					"address", addressLine,
					"lat", address.getLatitude(),
					"lng", address.getLongitude(),
					"address line num", address.getMaxAddressLineIndex()+1, 
					"address0", address.getAddressLine( 0 ) );
		}
		return c;
	}

	private void addAddress(String addressLine,
							double latitude, double longitude,
							String addressInfo, String addressFeature,
							MatrixCursor.RowBuilder row) {
		
		Log.d( "Adding data to the row", "lat", latitude, "lng", longitude, 
				addressLine, addressFeature, addressInfo );
		row.add( latitude );
		row.add( longitude );
		row.add( addressLine + addressFeature );
		row.add( addressInfo );
	}

	private String getAddressFeature(Address address) {
		String featureName = address.getFeatureName();
		StringBuilder sbu = new StringBuilder();
		if( featureName != null ) {
			sbu.append( "( " ).append( featureName ).append( " )" );
		}
		String addressFeature = sbu.toString();
		return addressFeature;
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

	private Cursor query(Uri uri) throws IOException {
		List<Address> addresses;
		int maxRes = getMaxResults(uri);
		String name = null;
		int matchId = uriMatcher.match(uri);
		TYPE type = DEFAULT_COORDINATE_SYSTEM;
		TYPE target = getTargetGeoCoordinateSystem(uri);
		
		switch( matchId ) {
			case SUGGEST_BY_POSITION:
			case BY_POSITION:
				double latitude = getDouble(uri, KEY_LATITUDE);
				double longitude = getDouble(uri, KEY_LONGITUDE);
				Log.d( "Get from location.", "lat", latitude, "lng", longitude );
				addresses = gc.getFromLocation( latitude, longitude, maxRes);
				type = TYPE.WGS84;
				break;
			case BY_LOCATION_NAME:
				name = uri.getQueryParameter(KEY_LOCATION_NAME);
				addresses = gc.getFromLocationName(name, maxRes);
				type = TYPE.GCJ02;
				break;
			case SUGGEST_BY_LOCATION_NAME:
				name = uri.getLastPathSegment();
				addresses = gc.getFromLocationName(name, maxRes);
				type = TYPE.GCJ02;
				break;
			case BY_POSITION_OF_ADDRESS:
				name = uri.getQueryParameter(KEY_LOCATION_NAME);
				name = name == null ? "" : name;
				double lat = getDouble( uri, KEY_LATITUDE );
				double lng = getDouble( uri, KEY_LONGITUDE );
				int distance = getInteger( uri, KEY_DISTANCE_IN_METERS );
				double toil = LocationUtility.distanceToToilence(distance);
				double llLat = lat - toil;
				double llLng = lng - toil;
				double urLat = lat + toil;
				double urLng = lng + toil;
				addresses
					= gc.getFromLocationName(name, 10, llLat, llLng, urLat, urLng);
				type = TYPE.GCJ02;
				return positionOfAddress( addresses, name, type, target );
			case SUGGEST_BY_LOCATION_NAME_AND_RANGE:
			case BY_LOCATION_NAME_AND_RANGE:
				name = uri.getQueryParameter(KEY_LOCATION_NAME);
				name = name == null ? "" : name;
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
//				// For the SearchView, there is a background thread to query when the text changed.
//				// So if the text is cleared, a query will be triggered. So the result of the query
//				// for position before, will be overloaded. 
//				addresses = lastQueriedAddressesByPosition;
//				break;
			default:
				IllegalArgumentException e
					= new IllegalArgumentException( "Unsupport URI: " + uri );
				Log.e( e );
				throw e;
		}
		
		return listToCursor( addresses, type, target );
	}

	private TYPE getTargetGeoCoordinateSystem(Uri uri) {
		TYPE target = DEFAULT_COORDINATE_SYSTEM;
		String typeString = uri.getQueryParameter(KEY_GEO_COORDINATE);
		if( typeString == null ) {
			return target;
		}
		try {
			target = TYPE.valueOf(typeString);
		} catch ( Exception e ) {
			Log.e( e, "Invalid Geodetic Coordinate System", typeString );
		}
		return target;
	}

	private int getMaxResults(Uri uri) {
		int maxRes = DEFAULT_MAX_RESULTS;
		String str = uri.getQueryParameter( KEY_MAX_RESULTS );
		try {
			maxRes = Integer.valueOf( str );
		} catch ( Exception e ) {
		}
		return maxRes;
	}

	private double getDouble(Uri uri, String name) {
		String str = uri.getQueryParameter( name );
		if( str != null ) {
			return Double.valueOf(str);
		} else {
			return 0;
		}
	}

	private int getInteger(Uri uri, String name) {
		String str = uri.getQueryParameter( name );
		if( str != null ) {
			return Integer.valueOf(str);
		} else {
			return 0;
		}
	}

	@Override
	public String getType(Uri uri) {
		return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.zsm.home.address";
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
