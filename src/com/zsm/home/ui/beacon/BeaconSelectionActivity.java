package com.zsm.home.ui.beacon;

import com.zsm.home.R;

import android.app.Activity;
import android.os.Bundle;

public class BeaconSelectionActivity extends Activity {

	private BeaconSelectionFragment mBeaconSelectionFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        setContentView(R.layout.beacon_selection_activity);

        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }
		
        mBeaconSelectionFragment = new BeaconSelectionFragment();
        // Add the fragment to the 'fragment_container' FrameLayout
        getFragmentManager().beginTransaction()
                .add(R.id.beaconFragmentContainer, mBeaconSelectionFragment)
                .commit();
	}

}
