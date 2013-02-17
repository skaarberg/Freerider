/**
 * @contributor(s): Freerider Team (Group 4, IT2901 Fall 2012, NTNU)
 * @version: 		1.0
 *
 * Copyright (C) 2012 Freerider Team.
 *
 * Licensed under the Apache License, Version 2.0.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 */
package no.ntnu.idi.socialhitchhiking.findDriver;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import no.ntnu.idi.freerider.model.Journey;
import no.ntnu.idi.freerider.model.Location;
import no.ntnu.idi.freerider.model.Route;
import no.ntnu.idi.freerider.protocol.JourneyResponse;
import no.ntnu.idi.freerider.protocol.Request;
import no.ntnu.idi.freerider.protocol.ResponseStatus;
import no.ntnu.idi.freerider.protocol.SearchRequest;
import no.ntnu.idi.socialhitchhiking.R;
import no.ntnu.idi.socialhitchhiking.SocialHitchhikingApplication;
import no.ntnu.idi.socialhitchhiking.client.RequestTask;
import no.ntnu.idi.socialhitchhiking.map.AutoCompleteTextWatcher;
import no.ntnu.idi.socialhitchhiking.map.GeoHelper;
import no.ntnu.idi.socialhitchhiking.map.MapActivitySearch;
import no.ntnu.idi.socialhitchhiking.map.MapRoute;
import no.ntnu.idi.socialhitchhiking.utility.DateChooser;
import no.ntnu.idi.socialhitchhiking.utility.JourneyAdapter;
import no.ntnu.idi.socialhitchhiking.utility.SocialHitchhikingActivity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * This is the activity where the hitchhiker search for available drivers. The hitchhiker enters where he or she
 * is going from and where he or she is going to. By pressing the search button a search is started and
 * the result is shown in a list. By clicking on a driver in the list the hitchhiker will get extra information
 * about the driver and the drivers journey. The hitchhiker can then define pickup and dropoff points, and send a request 
 * to join. If desirable a comment can also be sent with the request.
 * 
 * @author P�l
 * @author Christian Thurmann-Nielsen
 *
 */
public class FindDriver extends SocialHitchhikingActivity implements PropertyChangeListener{

	protected AutoCompleteTextView searchTo;
	protected AutoCompleteTextView searchFrom;
	protected Cursor cursor;
	protected ListAdapter adapter;
	private Button search;
	private ImageButton clear,mapmode;
	ArrayAdapter<String> adapter2;
	protected ListView driverList;
	private Journey selectedJourney;
	List<Journey> journeys;
	private DateChooser dc;
	private Calendar searchDate;
	private Location goingFrom;
	private Location goingTo;
	private Spinner pickDate;
	private DatePickerDialog datePicker;
	private OnDateSetListener odsl;
	private ArrayList<SpinnerEntry> spinnerList;
	private ArrayAdapter<SpinnerEntry> spinnerAdapter;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		journeys = new ArrayList<Journey>();
		
		
		getApp().addPropertyListener(this);
		setContentView(R.layout.find_driver);
		searchTo = (AutoCompleteTextView) findViewById (R.id.search2);
		searchFrom = (AutoCompleteTextView) findViewById (R.id.searchText);
		pickDate = (Spinner) findViewById(R.id.pickDate);

		spinnerList = new ArrayList<SpinnerEntry>();
		spinnerList.add(new SpinnerEntry("Upcoming"));
		spinnerList.add(new SpinnerEntry("Specific date"));
		spinnerAdapter = new ArrayAdapter<FindDriver.SpinnerEntry>(this, android.R.layout.simple_spinner_dropdown_item,spinnerList);
		pickDate.setAdapter(spinnerAdapter);
		spinnerAdapter.notifyDataSetChanged();
		//clear = (ImageButton)findViewById(R.id.finddriver_clear);
		//mapmode = (ImageButton)findViewById(R.id.finddriver_mapmode);
		search = (Button) findViewById(R.id.searchButton);
		driverList = (ListView) findViewById (R.id.list);
		dc = new DateChooser(this, this);
		dc.setTitle("Set Search Date", "Set Search Time");
		searchDate = Calendar.getInstance();
		driverList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				selectedJourney = (Journey) driverList.getAdapter().getItem(position);
				Route sr = selectedJourney.getRoute();
				Intent intent = new Intent(FindDriver.this, no.ntnu.idi.socialhitchhiking.map.MapActivityAddPickupAndDropoff.class);
				intent.putExtra("journey", true);
				intent.putExtra("searchFrom", (Serializable)goingFrom);
				intent.putExtra("searchTo", (Serializable)goingTo);
				MapRoute mr = new MapRoute(sr.getOwner(), sr.getName(), sr.getSerial(), sr.getMapPoints());
				getApp().setSelectedMapRoute(mr);
				getApp().setSelectedJourney(selectedJourney);
				startActivity(intent);
			}

		});
		initAutocomplete();
		search.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.e("Clicked","Search");
				checkAccess();
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(searchFrom.getWindowToken(), 0);
				imm.hideSoftInputFromWindow(searchTo.getWindowToken(), 0);
			}
		});
		pickDate.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				
				if(position == 1) {
					Calendar cal=Calendar.getInstance();
				    DatePickerDialog datePickDiag=new DatePickerDialog(FindDriver.this,odsl,cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH));
				    datePickDiag.show();
				    //Check if set
				}
				else if(position == 0) {
					//searchDate.set(2000, 1, 1);
				    if(spinnerList.size() == 3) {
				    	spinnerList.remove(2);
				    	spinnerAdapter.notifyDataSetChanged();
				    }
				}
				
				Log.e("Pos",Integer.toString(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				Log.e("Nothing","Selected");
				
			}
			
		});
		odsl = new OnDateSetListener() {

			@Override
			public void onDateSet(DatePicker arg0, int arg1, int arg2, int arg3) {
				searchDate.set(arg0.getYear(), arg0.getMonth(), arg0.getDayOfMonth());
				//Spinner spinner = (Spinner) findViewById(R.id.pickDate);

			    if(spinnerList.size() == 3) {
				    SpinnerEntry se = spinnerList.get(2);
				    se.setDate(searchDate);
			    }
			    else if(spinnerList.size() == 2) {
			    	spinnerList.add(new SpinnerEntry("Selected date",searchDate));
			    }
			    if(spinnerList.size() == 3) {
			    	Spinner spinner = (Spinner) findViewById(R.id.pickDate);
			    	spinner.setSelection(2);
			    }
			    //spinnerList.add(new SpinnerEntry("Date",searchDate));
			    spinnerAdapter.notifyDataSetChanged();
			    Log.e("ODSL", "Run");
				
			}
			
		};
		/*
		clear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				clear();
			}
		});
		mapmode.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mapMode();
			}
		});
		*/

	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		//Returns from mapmode:
		if(resultCode == 7331){
			Serializable ser1 = data.getSerializableExtra("goingFrom");
			Serializable ser2 = data.getSerializableExtra("goingTo");
			if(ser1 != null && ser1 instanceof Location){
				goingFrom = (Location) ser1;
				if(goingFrom.getAddress() != null && goingFrom.getAddress().length() > 0){
					searchFrom.setText(goingFrom.getAddress());
				}
			}
			if(ser2 != null && ser2 instanceof Location){
				goingTo = (Location) ser2;
				if(goingTo.getAddress() != null && goingTo.getAddress().length() > 0){
					searchTo.setText(goingTo.getAddress());
				}
			}
			search.performClick();
		}
	}
	/**
	 * Method called by the PropertyChangeListener when the application gets a
	 * new access token from facebook.
	 */
	private void relogin(){
		if(sendLoginRequest()){
			//chooseDate();
		}
	}
	private void clear(){
		searchFrom.setText("");
		searchTo.setText("");
		searchDate = null;
		journeys = new ArrayList<Journey>();
		driverList.setAdapter(new JourneyAdapter(this, 0, journeys));
	}
	private void mapMode(){
		Intent mapIntent = new Intent(getApplicationContext(), MapActivitySearch.class);
		startActivityForResult(mapIntent, 7331);
	}
	private void chooseDate(){
		dc.show();
	}
	/**
	 * Search for journeys on the server and updates the list of journeys accordingly.
	 */
	private void doSearch(){
		try {
			if(pickDate.getSelectedItemPosition() == 0) {
				Calendar c = Calendar.getInstance();
				journeys = new ArrayList<Journey>();
				for(int i = 0; i < 7; i++) {
					searchDate.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
					List<Journey> tempList = search();
					for(int j = 0; j < tempList.size(); j++) {
						journeys.add(tempList.get(i));
					}
				}
			} else {
				journeys = search();
			}
			
			driverList.setAdapter(new JourneyAdapter(this, 0, journeys));
		} catch (NullPointerException e) {
			Toast toast = Toast.makeText(FindDriver.this, "ERROR in receiving journeys", 2);
			toast.show();
			e.printStackTrace();
		}
	}

	/**
	 * Checks whether the current access token from Facebook is still valid.
	 * Tries to acquire a new access token if it's not valid, else, a search is made.
	 */
	private void checkAccess(){

		if(!getApp().getMain().isSession()){
			getApp().getMain().getAccess();
		}
		else {
			doSearch();
		}
		getApp().getMain();
	}

	/**
	 * Sends a search request to the server, with start and stop locations.
	 * Creates a dialog if the request is not sent.
	 * 
	 * @return
	 */
	private List<Journey> search() {
		JourneyResponse res = null;

		try{
			if(goingFrom == null){
				goingFrom = GeoHelper.getLocation(searchFrom.getText().toString());
			}
			
			if(goingTo == null){
				goingTo = GeoHelper.getLocation(searchTo.getText().toString());
			}
		} 
		catch(IndexOutOfBoundsException e){

		}
		Calendar cal = Calendar.getInstance();
		if(searchDate != null)cal = searchDate;
		Request req = new SearchRequest(getApp().getUser() , goingFrom, goingTo,cal);

		try {
			res = (JourneyResponse) RequestTask.sendRequest(req,getApp());

			if(res.getStatus() != ResponseStatus.OK){
				
			}

		} catch (MalformedURLException e) {
			createAlertDialog(this, false, "Search request","sent", "MalformedURLException");
			e.printStackTrace();
		} catch (IOException e) {
			createAlertDialog(this, false, "Search request","sent", "IOException");
			e.printStackTrace();
		}   

		if(res.getJourneys().size() == 0){
			String msg = "No results...";
			Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
			toast.setGravity(Gravity.BOTTOM, toast.getXOffset() / 2, toast.getYOffset() / 2);
			toast.show();
		}
		return res.getJourneys();
	}

	/**
	 * Initialize the {@link AutoCompleteTextView}'s with an {@link ArrayAdapter} 
	 * and a listener ({@link AutoCompleteTextWatcher}). The listener gets autocomplete 
	 * data from the Google Places API and updates the ArrayAdapter with these.
	 */
	public void initAutocomplete() {
		adapter2 = new ArrayAdapter<String>(this,R.layout.item_list);
		adapter2.setNotifyOnChange(true); 

		searchFrom = (AutoCompleteTextView) findViewById(R.id.searchText);
		searchFrom.setAdapter(adapter2);
		searchFrom.addTextChangedListener(new AutoCompleteTextWatcher(this, adapter2, searchFrom));
		searchFrom.setThreshold(1);

		searchTo = (AutoCompleteTextView) findViewById(R.id.search2);  
		searchTo.setAdapter(adapter2);
		searchTo.addTextChangedListener(new AutoCompleteTextWatcher(this, adapter2, searchTo));

	}

	@Override
	public void propertyChange(PropertyChangeEvent ev) {
		if(ev.getPropertyName() == SocialHitchhikingApplication.ACCESS_TOKEN){
			relogin();
		}
		if(ev.getPropertyName() == DateChooser.DATE_CHANGED){
			if(ev.getNewValue() != null)searchDate = (Calendar) ev.getNewValue();
			doSearch();
		}
	}
	private class SpinnerEntry{
		String name;
		Calendar date;
		public SpinnerEntry(String name) {
			this.name = name;
		}
		public SpinnerEntry(String name, Calendar date) {
			this.name = name;
			this.date = date;
		}
		public void setDate(Calendar date) {
			this.date = date;
		}
		public Calendar getDate() {
			return this.date;
		}
		public void clearDate() {
			this.date = null;
		}
		public String toString() {
			if(date != null) {
				return name + " (" + date.get(Calendar.DAY_OF_MONTH) + "/" + (date.get(Calendar.MONTH)+1) + "/" + date.get(Calendar.YEAR) + ")";
			}
			else
			{
				return name;
			}
		}
	}
}


