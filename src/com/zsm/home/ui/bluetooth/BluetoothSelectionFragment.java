package com.zsm.home.ui.bluetooth;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
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

import com.zsm.android.beacon.BeaconScanner;
import com.zsm.android.beacon.BeaconScanner.Callback;
import com.zsm.android.beacon.BeaconScanner.STOP_TYPE;
import com.zsm.android.bluetooth.BluetoothUtility;
import com.zsm.home.R;
import com.zsm.home.app.HomeApplication;
import com.zsm.home.preferences.Preferences;
import com.zsm.home.ui.MainActivity;

public class BluetoothSelectionFragment extends Fragment {

	private static final String SELECTION_LIST_OPERATOR = "SelectionList";

	private static final String SELECTION_STATE_OPERATOR = "SelectionState";

	private static final int BLUETOOTH_ENABLE_REQUEST = 101;
	
	private View mView;
	private ListView mDeviceList;
	
	private BluetoothAdapter mBluetoothAdapter;
	
	private BroadcastReceiver mStateReceiver;
	private ArrayAdapter<NamedBluetoothDevice> mDeviceAdapter;

	private BeaconScanner mBeaconScanner;

	private TextView mDeviceStateView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu( true );
		
		mBluetoothAdapter = BluetoothUtility.getAdapter(getActivity());
		mBeaconScanner = new BeaconScanner( getActivity() );
		
		Callback listScanCallback = new BeaconScanner.Callback() {
			@Override
			public void stopped( STOP_TYPE type ) {
				if( type != STOP_TYPE.CANCELLED ) {
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
			public void newBeaconFound(BluetoothDevice device) {
				mDeviceAdapter.add( new NamedBluetoothDevice(device) );
			}
		};
		
		Callback stateScanCallback = new BeaconScanner.Callback() {
			@Override
			public void stopped( STOP_TYPE type ) {
				if( type == STOP_TYPE.TIMEOUT ) {
					NamedBluetoothDevice device
						= Preferences.getInstance().getHomeBluetoothDevice();
					updateBluetoothView( device, R.string.stateBtDeviceNotInRange );
				}
			}
			
			@Override
			public void backgroundFunction(int backgroundCalledTimes) {
			}
			
			@Override
			public void newBeaconFound(BluetoothDevice device) {
				updateBluetoothView( new NamedBluetoothDevice( device ),
									 R.string.stateBtDeviceInRange );
			}
		};
		
		mBeaconScanner.newOperator( SELECTION_LIST_OPERATOR, listScanCallback, 
				 					HomeApplication.BLUETOOTH_SCAN_PRIOD,
				 					new Handler(),
				 					10);
		mBeaconScanner.newOperator( SELECTION_STATE_OPERATOR, stateScanCallback);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		
		if( mView == null ) {
			mView
				= inflater.inflate( R.layout.bluetooth_selection_fragment,
									container, false );
			
			mDeviceList
				= (ListView)mView.findViewById( R.id.listViewSelectBluetooth );
			mDeviceAdapter
				= new ArrayAdapter<NamedBluetoothDevice>(
						getActivity(), android.R.layout.simple_list_item_1);
			mDeviceList.setAdapter(mDeviceAdapter);
			mDeviceList.setOnItemClickListener( new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
										int position, long id) {
					Intent intent = new Intent();
					intent.putExtra( MainActivity.EXTRAS_BLUETOOTH_DEVICE,
									 mDeviceAdapter.getItem(position) );
					getActivity().setResult( Activity.RESULT_OK, intent );
					getActivity().finish();
				}
			});
			
			mDeviceStateView
				= (TextView)mView.findViewById( R.id.textViewBluetoothAtHome );
		}
		
		return mView;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateBtDeviceState( );
		startToScanBluetooth();
	}

	private void updateBtDeviceState( ) {
		final NamedBluetoothDevice device
			= Preferences.getInstance().getHomeBluetoothDevice();
		if( device == null ) {
			mDeviceStateView.setText( "" );
		} else {
			mBeaconScanner
				.startScan( SELECTION_STATE_OPERATOR, device.getDevice() );
	        updateBluetoothView( device, R.string.stateBtDeviceChecking );
		}
	}
	
	private void updateBluetoothView(NamedBluetoothDevice device, int stateResId ) {
		if( device == null ) {
			mDeviceStateView.setText( R.string.promptNoBluetoothSelected );
			return;
		}
		
		Resources resources = getResources();
		String stateStr = resources.getString( stateResId );
		String text
			= resources.getString( R.string.mainBluetooth,
								   device.toString(), stateStr );
		mDeviceStateView.setText(text);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate( R.menu.bluetooth_selection, menu);
		MenuItem refresh = menu.findItem( R.id.menuBluetoothRefresh );
		refresh.setOnMenuItemClickListener( new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				scanRemoteDevices();
				return true;
			}
		});
	}

	private void startToScanBluetooth() {
		if( !mBluetoothAdapter.isEnabled() ) {
			startActivityForResult( 
					new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE ),
								BLUETOOTH_ENABLE_REQUEST );
		} else {
			scanRemoteDevices();
		}
	}

	private void scanRemoteDevices() {
        mDeviceAdapter.clear();
		mBeaconScanner.startScan( SELECTION_LIST_OPERATOR );
	}

	@Override
	public void onDestroy() {
		mBeaconScanner.stopAll( );
		mView = null;
		super.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if( requestCode == BLUETOOTH_ENABLE_REQUEST ) {
			promptBluetooth( resultCode );
		}
	}

	private void promptBluetooth( int resultCode ) {
		int id;
		if( resultCode == Activity.RESULT_CANCELED ) {
			id = R.string.promptBluetoothEnableOk;
			registerBluetoothStateReceiver();
		} else {
			id = R.string.promptBluetoothEnableCanceled;
		}
		
		Toast.makeText( getActivity(), id, Toast.LENGTH_LONG ).show();
	}
	
	private void registerBluetoothStateReceiver() {
		IntentFilter filter = new IntentFilter( BluetoothAdapter.ACTION_STATE_CHANGED );
		getActivity().registerReceiver(mStateReceiver, filter);
	}

}
