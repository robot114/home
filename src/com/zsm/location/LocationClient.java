package com.zsm.location;

import java.util.Collection;

/**
 * 
 * @author zsm
 *
 * @param <P> Type of the providers
 * @param <L> Type of the locations
 */
public interface LocationClient<P, L> {

	void init( Object manager, long locationDeltaTime );
	
	void addExclusiveProvider( final P exclusiveProvider );
	
	Collection<String> updateProviders();
	
	boolean anyProviderEnabled();
	
	L getLastUpdatedLocation( L current );
	
	void updateCurrentLocation( long timeoutInMs,
								OnLocationUpdateListener<L> listener );
	
	void cancelUpdate( OnLocationUpdateListener<L> listener );
}
