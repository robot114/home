package com.zsm.location;

import java.util.Collection;

/**
 * Interface to remove the difference of different platform.
 * 
 * @author zsm
 *
 * @param <P> Type of the providers
 * @param <L> Type of the locations
 */
public interface LocationSerivce<P, L> {
	
	/**
	 * Must be called before the Service can be used.
	 * 
	 * @param context context to do location
	 * @param manager Platform relative manager to provide actual location
	 * 				service object.
	 * @param locationDeltaTime The location will be new enough when it is
	 * 				gotten at the time shorter than this one from now. 
	 * @param timeoutInMs When the location cannot be gotten before timeout 
	 * {@link LocationUpdateListener#onCancel} of the listeners will be called, 
	 * and the reason will be 
	 * {@linkplain LocationUpdateListener.CANCEL_REASON#TIME_OUT}
	 */
	void init( Object context, Object manager, long locationDeltaTime, long timeoutInMs );
	
	/**
	 * The exclusive providers are the ones will not be used to get the location.
	 * 
	 * @param exclusiveProvider the exclusive provider
	 */
	void addExclusiveProvider( final P exclusiveProvider );
	
	/**
	 * Update the providers set from system. The exclusive providers will be
	 * removed from the result.
	 * 
	 * @return the updated providers set
	 */
	Collection<String> updateProviders();
	
	/**
	 * Check if any provider excluding the exclusive ones and the passive one enabled.
	 * If no active provider enabled, something need to be done. For example
	 * to prompt the user enable them.
	 * 
	 * @return true, if any provider excluding the exclusive providers enabled;
	 * 			false, otherwise
	 */
	boolean anyActiveProviderEnabled();
	
	/**
	 * Get the provider with the highest accuracy from the current provider set.
	 * So the exclusive providers will be ignored. If the provider set has not
	 * been updated, the method {@link #updateProviders()} will be invoked first.
	 * 
	 * @return the provider with the highest accuracy.
	 */
	P getMostAccurateProvider();
	
	/**
	 * Get last updated location. The location may not updated by the request
	 * of the client of the service. If the last updated location is not good
	 * enough than the current one, null will be returned.
	 * 
	 * @param current the current location compared to the last updated one
	 * @return the last updated location, if it is better than the current one;
	 * 			null, otherwise
	 */
	L getLastUpdatedLocation( L current );
	
	/**
	 * Start the location service to get a location from hardware. This method
	 * need to be called when the {@link getLastUpdatedLocation} returns null.
	 * <p>This method of a single service can invoked more than once. When
	 * the updating is in progress, the new listener will listen the on
	 * going progress. If there is no on going progress, a new update progress
	 * will be created. When the location is obtained correctly in time, the 
	 * updating progress will finish and all the listeners be notified by 
	 * {@link LocationUpdateListener#onUpdate(Object)} with the newly obtained
	 * location.
	 * <p>When the location cannot be gotten before the time being out or 
	 * {@link #cancelUpdate(LocationUpdateListener)} called, the 
	 * {@link LocationUpdateListener#onCancel} of the callbacks will be called, 
	 * and the reason will be 
	 * {@linkplain LocationUpdateListener.CANCEL_REASON#TIME_OUT} or
	 * {@linkplain LocationUpdateListener.CANCEL_REASON#STOP}.
	 * 
	 * @param listener the callback to handle the service events.
	 */
	void startUpdate( LocationUpdateListener<L> listener );
	
	/**
	 * Cancel a special listener of the service. The method 
	 * {@linkplain LocationUpdateListener#onCancel} of the listener
	 * will be called with the reason 
	 * {@linkplain LocationUpdateListener.CANCEL_REASON#STOP}.
	 * If all the listeners are cancelled, the update progress will be cancelled
	 * 
	 * @param listener to handle the cancel event
	 */
	void cancelUpdate( LocationUpdateListener<L> listener );

	/**
	 * Cancel all listeners of the service. The method 
	 * {@linkplain LocationUpdateListener#onCancel} of the listener
	 * will be called with the reason 
	 * {@linkplain LocationUpdateListener.CANCEL_REASON#STOP}.
	 * The update progress will be cancelled at the same time.
	 */
	void cancelUpdate( );
}
