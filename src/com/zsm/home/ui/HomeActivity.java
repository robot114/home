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
        
        // Add the fragment to the 'fragment_container' FrameLayout
        getFragmentManager().beginTransaction()
                .add(R.id.homeFragmentContainer, locateHomeFragment).commit();
        
    }

	@Override
	public void next(Fragment from, Bundle data) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		if( from == locateHomeFragment ) {
            ft.replace(R.id.homeFragmentContainer, addressHomeFragment);
            getActionBar().setDisplayOptions( ActionBar.DISPLAY_SHOW_CUSTOM  );
		} else {
			ft.replace(R.id.homeFragmentContainer, locateHomeFragment);
	        getActionBar().setDisplayOptions( 0, ActionBar.DISPLAY_SHOW_CUSTOM  );
		}
		
		ft.commit();
	}

}
