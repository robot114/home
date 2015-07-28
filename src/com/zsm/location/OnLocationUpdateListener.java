package com.zsm.location;

public interface OnLocationUpdateListener<L> {

	void onUpdate( L location );
	
	void onCancel();
}
