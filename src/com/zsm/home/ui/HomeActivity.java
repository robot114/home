package com.zsm.home.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.zsm.home.R;

public class HomeActivity extends Activity implements FragmentWizard {
	
	private LocateHomeFragment locateHomeFragment;
	private AddressHomeFragment addressHomeFragment;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }


        locateHomeFragment = new LocateHomeFragment( this );
        addressHomeFragment = new AddressHomeFragment( this );
        // In case this activity was started with special instructions from an
        // Intent, pass the Intent's extras to the fragment as arguments
        locateHomeFragment.setArguments(getIntent().getExtras());
        addressHomeFragment.setArguments(getIntent().getExtras());
        
        getActionBar().setDisplayOptions( ActionBar.DISPLAY_SHOW_CUSTOM  );
        // Add the fragment to the 'fragment_container' FrameLayout
        getFragmentManager().beginTransaction()
                .add(R.id.homeFragmentContainer, locateHomeFragment).commit();
        
    }

	@Override
	public void next(Fragment from, Bundle data) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.setCustomAnimations( R.animator.left_in, R.animator.right_out );
		if( from == locateHomeFragment ) {
            ft.replace(R.id.homeFragmentContainer, addressHomeFragment);
            addressHomeFragment.setHomeLocationData( data );
		} else {
			ft.replace(R.id.homeFragmentContainer, locateHomeFragment);
			locateHomeFragment.setHomeAddressData( data );
		}
		
		ft.commit();
	}

}
