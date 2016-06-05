package com.zsm.android.beacon;

abstract public class BeaconDevice {

	protected String mAlias;

	protected BeaconDevice( String alias ) {
		mAlias = alias;
	}
	
	public String getAlias() {
		return mAlias;
	}

	public void setAlias(String alias) {
		this.mAlias = alias;
	}

	public abstract String getName();
	
	public abstract String getAddress();
	
	public abstract BeaconOperator.PROTOCOL getProtocol();

	@Override
	public String toString() {
		// Address is the unique id of the device
		if( getAddress() == null ) {
			return "None";
		}
		
		StringBuilder b = new StringBuilder();
		String name = getName();
		
		String last = " )";
		if( name != null ) {
			b.append( name ).append( "( " );
		} else if( mAlias != null ) {
			b.append( mAlias ).append( "( " );
		} else {
			last = "";
		}
		
		b.append( getAddress() ).append( last );
		
		return b.toString();
	}
}
