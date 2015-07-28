package com.zsm.home.ui;

import android.widget.SearchView.OnQueryTextListener;

import com.zsm.log.Log;

class OnLocationQueryListener implements OnQueryTextListener {
	OnLocationQueryListener( ) {
	}
	
	@Override
	public boolean onQueryTextSubmit(String query) {
		Log.d( query );
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		Log.d( newText );
		return true;
	}
}
