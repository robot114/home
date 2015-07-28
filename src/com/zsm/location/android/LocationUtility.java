package com.zsm.location.android;

import com.zsm.log.Log;

import android.location.Location;

public class LocationUtility {

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
	
}
