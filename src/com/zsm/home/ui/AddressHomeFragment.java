package com.zsm.home.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.zsm.home.R;

public class AddressHomeFragment extends Fragment {

	private View view;
	private FragmentWizard fragmentWizard;

	public AddressHomeFragment(FragmentWizard fw) {
		super();
		fragmentWizard = fw;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		if( view == null ) {
			view
				= inflater.inflate( R.layout.address_home_fragment, 
									container, false );
		}
		return view;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		getActivity().getActionBar().setCustomView(R.layout.action_bar_address_home);
		View v = getActivity().getActionBar().getCustomView();
		View toLocate = v.findViewById( R.id.imageViewToLocateHome );
		toLocate.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				fragmentWizard.next( AddressHomeFragment.this, (Bundle)null );
			}
		} );
	}

}
