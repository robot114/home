package com.zsm.android.beacon;

import android.content.Context;
import android.os.Handler;

public class WifiBeaconOperator extends BeaconOperator {

	public WifiBeaconOperator(Context context, Callback cb, int scanPeriod,
							  Handler handler, int backgroundTimes) {
		
		super(context, PROTOCOL.WIFI, cb, scanPeriod, handler, backgroundTimes);
	}

}
