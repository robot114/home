package com.zsm.home.ui.bluetooth;

import com.zsm.home.R;

import android.app.Activity;
import android.os.Bundle;

public class BluetoothSelectionActivity extends Activity {

	private BluetoothSelectionFragment mBluetoothSelectionFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        setContentView(R.layout.bluetooth_selection_activity);

        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }
		
        mBluetoothSelectionFragment = new BluetoothSelectionFragment();
        // Add the fragment to the 'fragment_container' FrameLayout
        getFragmentManager().beginTransaction()
                .add(R.id.bluetoothFragmentContainer, mBluetoothSelectionFragment)
                .commit();
	}

}
