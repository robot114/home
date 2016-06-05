package com.zsm.android.beacon;

interface Scanner {

	abstract void startScan(BeaconOperator o);

	abstract void stopScan(BeaconOperator o);

}
