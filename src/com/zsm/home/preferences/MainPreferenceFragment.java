package com.zsm.home.preferences;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

import com.zsm.home.R;
import com.zsm.home.location.HomeLocation;

public class MainPreferenceFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.std_preference);
		
		Preference po = findPreference( Preferences.KEY_PROXIMITY_ALERT_ON );
		po.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				Preferences prefs = Preferences.getInstance();
				Activity activity = MainPreferenceFragment.this.getActivity();
				HomeLocation.getInstance()
					.setProximityAlert( activity, (boolean)newValue,
									    prefs.getHomeLocation(),
									    prefs.getHomeProximityDistance() );
				return true;
			} } );
		
		Preference pd = findPreference( Preferences.KEY_PROXIMITY_DISTANCE );
		pd.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				Preferences prefs = Preferences.getInstance();
				boolean alertOn = prefs.isProximityAlertOn();
				if( !alertOn ) {
					return true;
				}
				
				Activity activity = MainPreferenceFragment.this.getActivity();
				float distance = Float.valueOf((String) newValue);
				HomeLocation.getInstance()
					.setProximityAlert( activity, alertOn,
									    prefs.getHomeLocation(),
									    distance );
				return true;
			}
		} );
	}
}
