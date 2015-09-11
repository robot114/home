package com.zsm.android.location;

import android.location.Location;
import android.widget.CursorAdapter;

public interface GeocoderClient {

	Location getLocationToQuery();
	
	CursorAdapter getDisplayAdapter();
}
