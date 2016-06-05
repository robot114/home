package com.zsm.android.beacon;

public class WifiBeaconScanFilter extends BeaconScanFilter {

	public WifiBeaconScanFilter(BeaconDevice device) {
		super(device);
	}

	public WifiBeaconScanFilter(String name, String address) {
		super(name, address);
	}

	@Override
	boolean checkAddress(String address) {
		return true;
	}

}
