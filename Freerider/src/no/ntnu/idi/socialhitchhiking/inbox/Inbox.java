/*******************************************************************************
 * @contributor(s): Freerider Team (Group 4, IT2901 Fall 2012, NTNU)
 * @contributor(s): Freerider Team 2 (Group 3, IT2901 Spring 2013, NTNU)
 * @version: 2.0
 * 
 * Copyright 2013 Freerider Team 2
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package no.ntnu.idi.socialhitchhiking.inbox;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.ClientProtocolException;

import no.ntnu.idi.freerider.model.Journey;
import no.ntnu.idi.freerider.model.Notification;
import no.ntnu.idi.freerider.model.NotificationType;
import no.ntnu.idi.freerider.model.Route;
import no.ntnu.idi.freerider.protocol.JourneyRequest;
import no.ntnu.idi.freerider.protocol.JourneyResponse;
import no.ntnu.idi.freerider.protocol.NotificationResponse;
import no.ntnu.idi.freerider.protocol.Request;
import no.ntnu.idi.freerider.protocol.RequestType;
import no.ntnu.idi.freerider.protocol.Response;
import no.ntnu.idi.freerider.protocol.ResponseStatus;
import no.ntnu.idi.freerider.protocol.UserRequest;
import no.ntnu.idi.socialhitchhiking.R;
import no.ntnu.idi.socialhitchhiking.SocialHitchhikingApplication;
import no.ntnu.idi.socialhitchhiking.client.RequestTask;
import no.ntnu.idi.socialhitchhiking.map.MapRoute;
import no.ntnu.idi.socialhitchhiking.utility.SectionedListViewAdapter;
import no.ntnu.idi.socialhitchhiking.utility.SocialHitchhikingActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
/**
 * Activity that shows a users received {@link Notification}s.
 * Notifications are shown in a listview and handled by 
 * {@link NotificationHandler}
 * 
 * @author Christian Thurmann-Nielsen
 * @author Thomas Gjerde
 */
public class Inbox extends SocialHitchhikingActivity implements PropertyChangeListener{
	private ListView notifications;
	private CheckedTextView header;
	private Notification sel;
	private List<Notification> sorted,notifHistory,requestList;
	private boolean history;
	private boolean request;
	private Calendar today,yesterday, last4Days, last10Days;
	private SectionedListViewAdapter active = new SectionedListViewAdapter() {
		protected View getHeaderView(String caption, int index,View convertView, ViewGroup parent) {
			TextView result = (TextView) convertView;

			if (convertView == null) {
				result = (TextView) getLayoutInflater().inflate(
						R.layout.listview_sectioned, null);
			}

			result.setText(caption);
			result.setTextColor(Color.BLACK);
			result.setBackgroundColor(Color.rgb(170, 170, 170));

			return (result);
		}
	};
	private SectionedListViewAdapter requestAdapter = new SectionedListViewAdapter() {
		protected View getHeaderView(String caption, int index,View convertView, ViewGroup parent) {
			TextView result = (TextView) convertView;

			if (convertView == null) {
				result = (TextView) getLayoutInflater().inflate(
						R.layout.listview_sectioned, null);
			}

			result.setText(caption);
			result.setTextColor(Color.BLACK);
			result.setBackgroundColor(Color.rgb(170, 170, 170));

			return (result);
		}
	};
	private SectionedListViewAdapter historyAdap = new SectionedListViewAdapter() {
		protected View getHeaderView(String caption, int index,View convertView, ViewGroup parent) {
			TextView result = (TextView) convertView;

			if (convertView == null) {
				result = (TextView) getLayoutInflater().inflate(
						R.layout.listview_sectioned, null);
			}
			result.setText(caption);
			result.setTextColor(Color.BLACK);
			result.setBackgroundColor(Color.rgb(170, 170, 170));

			return (result);
		}
	};
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main_loading);
		new InboxLoader(this).execute();
	}
	/**
	 * Create custom {@link Menu} in the Inbox activity. Gives a user the possibility
	 * of viewing old notifications.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		menu.add("Show Notification history").
		setIcon(android.R.drawable.ic_menu_recent_history).
		setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				return true;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}
	private void initAdapter(SectionedListViewAdapter adp,List<Notification> list){
		List<Notification> temp = new ArrayList<Notification>(list);
		Collections.copy(temp, list);
		List<Notification> result = new ArrayList<Notification>();


		result = getToday(temp);
		if(result.size() != 0)adp.addSection("Today", new NotificationAdapter(this, 0, result));

		result = getYesterday(temp);
		if(result.size() != 0)adp.addSection("Yesterday", new NotificationAdapter(this, 0, result));

		result = getLast4Days(temp);
		if(result.size() != 0)adp.addSection("Last 4 days", new NotificationAdapter(this, 0, result));

		result = getLast10Days(temp);
		if(result.size() != 0)adp.addSection("last 10 days", new NotificationAdapter(this, 0, result));

		
		if(temp.size() != 0 && list.size() != 0){
			adp.addSection("Later", new NotificationAdapter(this, 0, temp));
			adp.addSection("", new NotificationAdapter(this, 0,new ArrayList<Notification>()));
		}
		else if(list.size() != 0) adp.addSection("", new NotificationAdapter(this, 0,new ArrayList<Notification>()));
	}
	private void initCalendars(){
		today = Calendar.getInstance();
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		
		yesterday = Calendar.getInstance();
		yesterday.set(Calendar.HOUR_OF_DAY, 0);
		yesterday.set(Calendar.MINUTE, 0);
		yesterday.set(Calendar.SECOND, 0);
		yesterday.add(Calendar.DATE, -1);

		last4Days = Calendar.getInstance();
		last4Days.add(Calendar.DATE, -4);

		last10Days = Calendar.getInstance();
		last10Days.add(Calendar.DATE,-10);
	}
	/**
	 * This changes the adapter of the notifications listview based on the status of 2 booleans
	 * !history && request == Shows unread requests
	 * !history && !request == Shows unread messages
	 * history == Shows read messages/requests 
	 */
	private void changeNotificationList(){
		if(getApp().getUser() != null){
			if(!history){
				if(request){
					if(requestAdapter.isEmpty()){
						Toast.makeText(this, "You have no new requests", Toast.LENGTH_SHORT).show();
					}
					notifications.setAdapter(requestAdapter);
				}
				else
				{
					if(active.isEmpty()) {
						Toast.makeText(this, "You have no unread notifications", Toast.LENGTH_SHORT).show();
					}
					notifications.setAdapter(active);
				}

				header.setText("Active Notifications");
			}
			else {
				if(historyAdap.isEmpty()){
					Toast.makeText(this, "You have no read notifications", Toast.LENGTH_SHORT).show();
				}
				notifications.setAdapter(historyAdap);
				header.setText("Notification History");
			}
		}
	}

	/**
	 * Method for getting only unread notifications
	 */
	private List<Notification> sortNotifications(List<Notification> ns){
		List<Notification> list = new ArrayList<Notification>();
		for (Notification n : ns){
			if(!n.isRead())list.add(n);
		}
		ns.removeAll(list);
		return list;
	}
	/**
	 * Method for marking a notification as read
	 * @param n
	 */
	public void setNotificationRead(Notification n){
		n.setRead(true);
		if(sorted.contains(n)){
			sorted.remove(n);
			notifHistory.add(n);
			active.removeObject(n);
			historyAdap.reset();
			initAdapter(historyAdap, notifHistory);
		}
		else if(requestList.contains(n)){
			requestList.remove(n);
			notifHistory.add(n);
			requestAdapter.removeObject(n);
			historyAdap.reset();
			initAdapter(historyAdap, notifHistory);
		}
	}
	/**
	 * Method for getting a list of notifications for today
	 * @param list
	 * @return
	 */
	private List<Notification> getToday(List<Notification> list){
		List<Notification> no = new ArrayList<Notification>();
		Calendar tomorrow = Calendar.getInstance();
		tomorrow.set(Calendar.HOUR_OF_DAY, 0);
		tomorrow.set(Calendar.MINUTE, 0);
		tomorrow.set(Calendar.SECOND, 0);
		tomorrow.add(Calendar.DATE, 1);

		for(Notification n : list){
			if(n.getTimeSent().after(today) && n.getTimeSent().before(tomorrow)){
				no.add(n);
			}
		}
		list.removeAll(no);

		return no;
	}
	/**
	 * Method for getting a list of notifications for the last 4 days
	 * @param list
	 * @return
	 */
	private List<Notification> getLast4Days(List<Notification> list){
		List<Notification> no = new ArrayList<Notification>();

		for(Notification n : list){
			if(n.getTimeSent().after(last4Days) && n.getTimeSent().before(today)){
				no.add(n);
			}
		}
		list.removeAll(no);

		return no;
	}
	/**
	 * Method for getting a list of notifications for the last 10 days
	 * @param list
	 * @return
	 */
	private List<Notification> getLast10Days(List<Notification> list){
		List<Notification> no = new ArrayList<Notification>();

		for(Notification n : list){
			if(n.getTimeSent().after(last10Days) && n.getTimeSent().before(today)){
				no.add(n);
			}
		}
		list.removeAll(no);

		return no;
	}
	/**
	 * Method for getting a list of notifications for yesterday
	 * @param list
	 * @return
	 */
	private List<Notification> getYesterday(List<Notification> list){
		List<Notification> no = new ArrayList<Notification>();

		for(Notification n : list){
			if(n.getTimeSent().after(yesterday) && n.getTimeSent().before(today)){
				no.add(n);
			}
		}
		list.removeAll(no);

		return no;
	}
	private class NotificationAdapter extends ArrayAdapter<Notification>{


		public NotificationAdapter(Context context, int textViewResourceId,
				List<Notification> objects) {
			super(context, textViewResourceId, objects);
			// TODO Auto-generated constructor stub
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Notification current = this.getItem(position);

			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.notification_list_item, parent, false);

			TextView status = (TextView)row.findViewById(R.id.notification_item_status);
			TextView startTime = (TextView)row.findViewById(R.id.notification_item_starttime);
			TextView start = (TextView)row.findViewById(R.id.notification_item_start);
			TextView stop = (TextView)row.findViewById(R.id.notification_item_stop);

			status.setText(setStatus(current));
			status.setTextColor(Color.BLACK);
			start.setText(Html.fromHtml("<b>Sender: </b>\t\t"+current.getSenderName()));
			stop.setText(Html.fromHtml("<b>Message: </b>\t"+current.getComment()));
			startTime.setText(Html.fromHtml("<b>Time sent: </b>\t"+current.getTimeSent().getTime().toLocaleString()));

			return row;
		}
		private String setStatus(Notification n){
			switch (n.getType()) {
			case DRIVER_CANCEL:
				return "Driver cancelled journey";
			case HITCHHIKER_ACCEPTS_DRIVER_CANCEL:
				return "Hitchhiker accepts cancel";
			case HITCHHIKER_CANCEL:
				return "Hitchhiker cancelled request";
			case HITCHHIKER_REQUEST:
				return "A Hitchhiker sent a request";
			case REQUEST_ACCEPT:
				return "Request accepted";
			case REQUEST_REJECT:
				return "Request rejected";
			case MESSAGE:
				return "Message";
			case RATING:
				return "Rating request";
			default:
				return "Status unknown";
			}
		}

	}
	/**
	 * Takes a notification as a parameter, and displays the journey associated with the notification in a map
	 * @param n
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void showInMap(Notification n) throws InterruptedException, ExecutionException {
		getApp().setJourneyPickupPoint(n.getStartPoint());
		getApp().setJourneyDropoffPoint(n.getStopPoint());

		Journey journey = null;
		int serial = n.getJourneySerial();
		if(getApp().getJourneys() == null)getApp().sendJourneysRequest();
		for (Journey j : getApp().getJourneys()) {
			if(j.getSerial() == serial)journey = j;
		}
		if(journey != null){
			Request r = new JourneyRequest(RequestType.GET_JOURNEY, getApp().getUser(), journey);

			try{
				JourneyResponse response = (JourneyResponse)RequestTask.sendRequest(r,getApp());
				if(response.getStatus() == ResponseStatus.OK){
					if(response.getJourneys().size() > 0){
						journey = response.getJourneys().get(0);
					}
				}
				else if(response.getStatus() == ResponseStatus.FAILED){
					createAlertDialog(this, false, "Journey", "fetched", "Can't fetch corresponding Journey from server");
					return;
				}
			}catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} 
			if(journey == null) {
				return;
			}
			Route sr = journey.getRoute();
			Intent intent = new Intent(this, no.ntnu.idi.socialhitchhiking.map.MapActivityJourneyAccept.class);
			intent.putExtra("journey", true);
			MapRoute mr = new MapRoute(sr.getOwner(), sr.getName(), sr.getSerial(), sr.getMapPoints());
			getApp().setSelectedMapRoute(mr);
			getApp().setSelectedJourney(journey);
			getApp().setSelectedNotification(n);
			startActivity(intent);

		}
		else{
			createAlertDialog(this, false, "Journey", "fetched", "The corresponding journey was not found");
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent ev) {
		if(ev.getPropertyName() == SocialHitchhikingApplication.NOTIFICATION){
			Notification n = (Notification) ev.getNewValue();
			setNotificationRead(n);
		}
	}

	public void showMain(Response response) {
		setContentView(R.layout.inbox);
		Intent intent = getIntent();
		
		history = intent.getBooleanExtra("history", false);
		request = intent.getBooleanExtra("request", false);
		
		header = (CheckedTextView) findViewById(R.id.inbox_header);
		header.setText("Active Notifications");
		header.setVisibility(View.GONE);
		sorted = new ArrayList<Notification>();
		notifHistory = new ArrayList<Notification>();
		notifications = (ListView) findViewById(R.id.notification_list);
		notifications.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View child, int pos,
					long arg3) {
				sel = (Notification) notifications.getAdapter().getItem(pos);
				NotificationHandler.handleNotification(sel, getApp(),Inbox.this);

			}
		});
		
		NotificationResponse notif = null;
		if(response instanceof NotificationResponse && response.getStatus() == ResponseStatus.OK){
			notif = (NotificationResponse) response;
			getApp().setNotifications(notif.getNotifications());
			notifHistory = getApp().getNotifications();
			sorted = sortNotifications(notifHistory);
			requestList = new ArrayList<Notification>();
			for(int i = 0; i < sorted.size(); i++){
				if(sorted.get(i).getType() == NotificationType.HITCHHIKER_REQUEST){
					requestList.add(sorted.get(i));
				}
			}
			sorted.removeAll(requestList);
			System.out.println("JoYo Inbox" + notifHistory.size());
			initAdapter(requestAdapter, requestList);
			initAdapter(active,sorted);
			initAdapter(historyAdap,notifHistory);
			notifications.setAdapter(active);
		}
		
		initCalendars();
		if(!getApp().isKey("inbox") && getApp().getSettings().isCheckSettings()){
			getApp().setKeyState("inbox", true);
		}
		changeNotificationList();
	}
}
/**
 * Gets notifications from server before showing actual GUI
 */
class InboxLoader extends AsyncTask<Void, Integer, Response>{
	
	Inbox activity;
	public InboxLoader(Inbox activity){
		this.activity = (Inbox) activity;
	}
	
	protected Response doInBackground(Void... params) {
		UserRequest req = new UserRequest(RequestType.PULL_NOTIFICATIONS,activity.getApp().getUser());
		Response response = null;
		try {
			response = RequestTask.sendRequest(req,activity.getApp());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return response;
	}

	protected void onPostExecute(Response result) {
		activity.showMain(result);	
	}
}
