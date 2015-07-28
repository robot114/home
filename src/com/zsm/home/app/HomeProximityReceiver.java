package com.zsm.home.app;

import com.zsm.home.ui.MainActivity;
import com.zsm.log.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

public class HomeProximityReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String key = LocationManager.KEY_PROXIMITY_ENTERING;
		if( !intent.hasExtra(key) ) {
			Log.i( "The key does not exist, it is a fake alert!", key );
			return;
		}
		
		Log.d( "A proximity alert received.", intent );
		Intent ai = new Intent( context, MainActivity.class );
		ai.putExtras(intent);
		context.startActivity( ai );
	}

}
