package com.zsm.home.ui;

import android.app.ActionBar;
import android.app.Fragment;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.Spinner;

import com.zsm.android.location.GeocoderClient;
import com.zsm.android.location.GeocoderLoaderCallbacks;
import com.zsm.android.location.GeocoderProvider;
import com.zsm.android.ui.ClearableEditor;
import com.zsm.android.ui.ClickableSpinner;
import com.zsm.android.ui.SpinnerClickAdapter;
import com.zsm.driver.android.log.ShowLogTrigger;
import com.zsm.home.R;
import com.zsm.home.app.HomeApplication;
import com.zsm.home.preferences.Preferences;

public class AddressHomeFragment extends Fragment implements GeocoderClient {

	private static final int ADDRESS_QUERY_ID = 3;
	private static final int[] SEARCH_LIST_ITEM_VIEW_IDS
		= new int[]{android.R.id.text1, android.R.id.text2};
	private static final String[] SEARCH_LIST_COLUMNS
		= new String[]{ GeocoderProvider.COLUMN_ADDRESS,
						GeocoderProvider.COLUMN_ADDRESS_DESC };
	
	private View view;
	private FragmentWizard fragmentWizard;
	private Bundle contextData;
	private ClearableEditor textViewAddress;
	private Spinner spinner;
	private CursorAdapter adapter;
	private GeocoderLoaderCallbacks geocoderLoaderCallbacks;

	public AddressHomeFragment(FragmentWizard fw) {
		super();
		fragmentWizard = fw;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		geocoderLoaderCallbacks
			= new GeocoderLoaderCallbacks( getActivity(), this );
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		if( view == null ) {
			view
				= inflater.inflate( R.layout.address_home_fragment, 
									container, false );
			textViewAddress
				= (ClearableEditor)view.findViewById( R.id.textViewHomeAddress );
			if( textViewAddress.getText(  ).length() == 0 ) {
				String address
					= contextData.getString( 
							HomeApplication.KEY_HOME_LOCATION_ADDRESS );
				
				textViewAddress.setText( address );
			}
			textViewAddress
				.addTextChangedListener( new ShowLogTrigger( getActivity() ) );
			
			adapter
				= new SpinnerClickAdapter( 
							getActivity(), 
							android.R.layout.two_line_list_item,
							null,
							SEARCH_LIST_COLUMNS,
							SEARCH_LIST_ITEM_VIEW_IDS,
							0 );
			initSpinner(adapter);
			getLoaderManager()
				.initLoader( GeocoderLoaderCallbacks.LOCATION_QUERY_ID,
							 null,
							 geocoderLoaderCallbacks );
		}
		return view;
	}

	private void initSpinner(final CursorAdapter cursorAdapter) {
		spinner = (ClickableSpinner)view.findViewById( R.id.spinnerSearchResult );
		spinner.setAdapter( cursorAdapter );
		spinner.setOnItemClickListener( new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
									int position, long id) {
				
				Cursor c = (Cursor)cursorAdapter.getItem(position);
				String text
					= c.getString( 
						c.getColumnIndex( GeocoderProvider.COLUMN_ADDRESS ) );
				String address = textViewAddress.getText() + text;
				textViewAddress.setText(address);
			}
			
		} );
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setCustomView(R.layout.action_bar_address_home);
		View v = actionBar.getCustomView();
		View toLocate = v.findViewById( R.id.imageViewToLocateHome );
		toLocate.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				Bundle data = new Bundle();
				data.putString( HomeApplication.KEY_HOME_LOCATION_ADDRESS,
								textViewAddress.getText().toString() );
				fragmentWizard.next( AddressHomeFragment.this, data );
			}
		} );
		
		View ok = v.findViewById( R.id.imageViewSaveHome );
		ok.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				Location l
					= contextData.getParcelable( HomeApplication.KEY_HOME_LOCATION );
				Preferences.getInstance().setHomeLocation( l );
				Preferences.getInstance()
					.setHomeAddress( textViewAddress.getText().toString() );
				getActivity().finish();
			}
		} );
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		view = null;
		contextData = null;
		textViewAddress = null;
		spinner = null;
		adapter = null;
		if( getLoaderManager().getLoader(ADDRESS_QUERY_ID) != null ) {
			getLoaderManager().destroyLoader(ADDRESS_QUERY_ID);
		}
	}

	public void setHomeLocationData(Bundle data) {
		contextData = data;
	}

	@Override
	public Location getLocationToQuery() {
		return contextData.getParcelable( HomeApplication.KEY_HOME_LOCATION );
	}

	@Override
	public CursorAdapter getDisplayAdapter() {
		return adapter;
	}

}
