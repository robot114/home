package com.zsm.location.android;

import com.zsm.log.Log;

import android.location.Location;

public class LocationUtility {

	public static final double METER_TO_DEGREE = 8.982999673118623e-6;
	public static final double DEFAULT_LOCATION_TOILANCE = METER_TO_DEGREE;
	
	static public Location getBetterLocation( Location a, Location b,
											  long deltaTime ) {
		
		if( a == null ) {
			return b;
		}
		Log.d( a, b );
		return isBetterNewLocation( a, b, deltaTime ) ? a : b;
	}
	
	/** Determines whether one Location newly reading is better than the current
	 *  Location fix
	 * @param newLocation  The new Location that you want to evaluate
	 * @param currentBestLocation  The current Location fix, to which you want to
	 * 			compare the new one
	 */
	static public boolean isBetterNewLocation(Location newLocation,
									 		  Location currentBestLocation,
									 		  long deltaTime ) {
		
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }
	    
	    if( newLocation == null ) {
	    	return false;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > deltaTime;
	    boolean isSignificantlyOlder = timeDelta < -deltaTime;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location,
	    // use the new location because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta
	    	= (int) (newLocation.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;
	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}

	/** Checks whether two providers are the same */
	static public boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	
	static public boolean samePosition( Location a, Location b ) {
		return nearEnough( a, b, DEFAULT_LOCATION_TOILANCE );
	}
	
	static public boolean nearEnough(Location a, Location b, int distanceInMeters) {
		return nearEnough( a, b, distanceToToilence( distanceInMeters ) );
	}

	static public boolean nearEnough(Location a, Location b, double toilance) {
		if( a == b ) {
			return true;
		}
		
		if( a == null || b == null ) {
			return false;
		}
		
		if( a.hasAltitude() && b.hasAltitude()
				&& Math.abs( a.getAltitude() - b.getAltitude() ) > toilance ) {
			
			return false;
		}
		
		if( Math.abs( a.getLatitude() - b.getLatitude() ) > toilance
			|| Math.abs( a.getLongitude() - b.getLongitude() ) > toilance ) {
			
			return false;
		}
		
		return true;
	}
	
	static public boolean nearEnough( double lat1, double lng1,
									  double lat2, double lng2,
									  double toilance ) {
		
		return ( Math.abs( lat1 - lat2 ) <= toilance 
				 && Math.abs( lng1 - lng2 ) <= toilance );
	}

	static public double distanceToToilence( int distanceInMeters ) {
		
		return (double)distanceInMeters * METER_TO_DEGREE;
	}
}
