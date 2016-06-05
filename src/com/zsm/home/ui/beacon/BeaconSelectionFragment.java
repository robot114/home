package com.zsm.home.ui.beacon;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zsm.android.beacon.BeaconDevice;
import com.zsm.android.beacon.BeaconOperator;
import com.zsm.android.beacon.BeaconOperator.PROTOCOL;
import com.zsm.android.beacon.BeaconOperatorFactory;
import com.zsm.home.R;
import com.zsm.home.app.HomeApplication;
import com.zsm.home.ui.MainActivity;

public class BeaconSelectionFragment extends Fragment {

	public static final String KEY_BEACON_PROTOCOL = "BEACON_PROTOCOL";
	public static final String KEY_CURRENT_DEVICE = "CURRENT_BEACON_DEVICE";

	private PROTOCOL mProtocol;
	private BeaconDevice mCurrentDevice;

	private View mView;
	private ListView mDeviceList;
	
	private ArrayAdapter<BeaconDevice> mDeviceAdapter;

	private TextView mDeviceStateView;

	private BeaconOperator mSelectionOperator;
	private BeaconOperator mStateOperator;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu( true );
		
		Intent intent = getActivity().getIntent();
		mProtocol = (PROTOCOL) intent.getSerializableExtra( KEY_BEACON_PROTOCOL );
		if( mProtocol == null ) {
			mProtocol = PROTOCOL.BLUETOOTH;
		}
		mCurrentDevice = intent.getParcelableExtra( KEY_CURRENT_DEVICE );
		
		BeaconOperator.Callback listScanCallback = new BeaconOperator.Callback() {
			@Override
			public void stopped( BeaconOperator.STOP_TYPE type ) {
				if( type != BeaconOperator.STOP_TYPE.CANCELLED ) {
					Toast.makeText( getActivity(),
									"Finished scanning beacon",
									Toast.LENGTH_SHORT )
						 .show();
				}
			}
			
			@Override
			public void backgroundFunction(int backgroundCalledTimes) {
				Toast.makeText( getActivity(),
								"Finished " + backgroundCalledTimes*10
								+ "% scanning beacon",
								Toast.LENGTH_SHORT )
					 .show();
			}
			
			@Override
			public void newBeaconFound(BeaconDevice device) {
				mDeviceAdapter.add( device );
			}
		};
		
		BeaconOperator.Callback stateScanCallback = new BeaconOperator.Callback() {
			@Override
			public void stopped( BeaconOperator.STOP_TYPE type ) {
				if( type == BeaconOperator.STOP_TYPE.TIMEOUT ) {
					updateDeviceView( mCurrentDevice,
									  R.string.stateBtDeviceNotInRange );
				}
			}
			
			@Override
			public void backgroundFunction(int backgroundCalledTimes) {
			}
			
			@Override
			public void newBeaconFound(BeaconDevice device) {
				updateDeviceView( device, R.string.stateBtDeviceInRange );
			}
		};
		
		mSelectionOperator
			= BeaconOperatorFactory
				.createOperator( getActivity(), mProtocol, listScanCallback,
								 HomeApplication.DEVICE_SCAN_PRIOD,
				 				 new Handler(), 10);
		mStateOperator
			= BeaconOperatorFactory
				.createOperator( getActivity(), mProtocol, stateScanCallback);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		
		if( mView == null ) {
			mView
				= inflater.inflate( R.layout.beacon_selection_fragment,
									container, false );
			
			mDeviceList
				= (ListView)mView.findViewById( R.id.listViewSelectDevice );
			mDeviceAdapter
				= new ArrayAdapter<BeaconDevice>(
						getActivity(), android.R.layout.simple_list_item_1);
			mDeviceList.setAdapter(mDeviceAdapter);
			mDeviceList.setOnItemClickListener( new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
										int position, long id) {
					Intent intent = new Intent();
					intent.putExtra( MainActivity.EXTRAS_DEVICE,
									 (Parcelable)mDeviceAdapter.getItem(position) );
					getActivity().setResult( Activity.RESULT_OK, intent );
					getActivity().finish();
				}
			});
			
			mDeviceStateView
				= (TextView)mView.findViewById( R.id.textViewBeaconAtHome );
		}
		
		return mView;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateBtDeviceState( );
		scanRemoteDevices();
	}

	private void updateBtDeviceState( ) {
		if( mCurrentDevice == null ) {
			mDeviceStateView.setText( "" );
		} else {
			mStateOperator.startScan( mCurrentDevice );
	        updateDeviceView( mCurrentDevice, R.string.stateBtDeviceChecking );
		}
	}
	
	private void updateDeviceView(BeaconDevice device, int stateResId ) {
		if( getActivity() == null ) {
			return;
		}
		
		if( device == null ) {
			mDeviceStateView.setText( R.string.promptNoDeviceSelected );
			return;
		}
		
		Resources resources = getResources();
		String stateStr = resources.getString( stateResId );
		String text
			= resources.getString( R.string.beaconState,
								   device.getProtocol().name(),
								   device.toString(), stateStr );
		mDeviceStateView.setText(text);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate( R.menu.beacon_selection, menu);
		MenuItem refresh = menu.findItem( R.id.menuDeviceRefresh );
		refresh.setOnMenuItemClickListener( new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				scanRemoteDevices();
				return true;
			}
		});
	}

	private void scanRemoteDevices() {
        mDeviceAdapter.clear();
		mSelectionOperator.startScan( );
	}

	@Override
	public void onDestroy() {
		mSelectionOperator.stopScan( BeaconOperator.STOP_TYPE.CANCELLED );
		mView = null;
		super.onDestroy();
	}
	
}
