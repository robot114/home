package com.zsm.location;

public interface LocationUpdateListener<L> {

	enum CANCEL_REASON { TIME_OUT, STOP };

	void onUpdate( L location );
	
	void onCancel( CANCEL_REASON reason );
}
