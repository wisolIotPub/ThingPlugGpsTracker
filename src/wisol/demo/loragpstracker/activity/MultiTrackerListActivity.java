package wisol.demo.loragpstracker.activity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import wisol.demo.loragpstracker.JsonContentInstanceDetail;
import wisol.demo.loragpstracker.JsonResponseContentInstanceDetailedLastOne;
import wisol.demo.loragpstracker.JsonResponseContentInstancesDetailed;
import wisol.demo.loragpstracker.MyThingPlugDevices;
import wisol.demo.loragpstracker.MyThingPlugDevices.MyDevices;
import wisol.demo.loragpstracker.R;
import wisol.demo.loragpstracker.ThingPlugDevice;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class MultiTrackerListActivity extends Activity
		implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

	final String EXTRA_GPS_UPDATE = "GPS_UPDATE";
	final String EXTRA_LATITUDE = "LATITUDE";
	final String EXTRA_LONGITUDE = "LONGITUDE";

	TextView mTextKeyInfo;
	ListView mListDeviceList;

	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
	private Location mLastLocation;
	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;

	Geocoder mGeocoder;
	private boolean mRequestingLocationUpdates = false;
	static boolean isActivated = false;

	private static int UPDATE_INTERVAL = 5000;
	private static int FATEST_INTERVAL = 1000;
	private static int DISPLACEMENT = 1;

	Handler mHandler;
	ThingPlugDevice mapDevice;
	String THING_AUTHORIZATION;
	String THING_REQ_URI;

	private final long REQ_FAST = 1000;
	private final long REQ_NORMAL = 5000;
	final MyDevices mCheckDevice = MyDevices.GPS02;// MyDevices.GPS01C;//MyDevices.GPS02;
	RequestQueue mRequestQueue;

	final String DELIMITER = ",";
	// LoRaGpsDevice mLoRaGpsNow;
	ArrayList<LoRaGpsDevice> mLoRaGpsDeviceList;
	private MsgListViewAdapter mMsgListViewAdapter;

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		if (mGoogleApiClient != null) {
			mGoogleApiClient.connect();
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		checkPlayServices();
		if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
			startLocationUpdates();
		}
		isActivated = true;
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}

	@Override
	public void onBackPressed() {
		this.finishAffinity();
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopLocationUpdates();
		mHandler.removeMessages(0);
		// launchTestService();
		isActivated = false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_multi_tracker_list);

		initUiComponents();

		initDevice(mCheckDevice);

		mGeocoder = new Geocoder(this);

		if (checkPlayServices()) {
			buildGoogleApiClient();
			createLocationRequest();
		}

		mHandler = new WeakHandler(this) {
			@Override
			public void handleMessage(Message msg) {
				Log.v("handlerCheck", "in");
				mHandler.sendEmptyMessageDelayed(0, REQ_NORMAL);
				getThingPlugDeviceContent();
			}
		};
	}

	static public class WeakHandler extends Handler {
		private final WeakReference<MultiTrackerListActivity> mHandlerObj;

		public WeakHandler(MultiTrackerListActivity pHandlerObj) {
			mHandlerObj = new WeakReference<MultiTrackerListActivity>(pHandlerObj);
		}
	}

	/**
	 * Creating location request object
	 * */
	protected void createLocationRequest() {
		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		mLocationRequest.setFastestInterval(FATEST_INTERVAL);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setSmallestDisplacement(DISPLACEMENT); // 10 meters
	}

	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API).build();
	}

	/**
	 * Starting the location updates
	 * */
	protected void startLocationUpdates() {

		LocationServices.FusedLocationApi.requestLocationUpdates(
				mGoogleApiClient, mLocationRequest, this);

	}

	/**
	 * Stopping location updates
	 */
	protected void stopLocationUpdates() {
		LocationServices.FusedLocationApi.removeLocationUpdates(
				mGoogleApiClient, this);
	}

	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Toast.makeText(getApplicationContext(),
						"This device is not supported.", Toast.LENGTH_LONG)
						.show();
				finish();
			}
			return false;
		}
		return true;
	}

	private void initUiComponents() {
		mTextKeyInfo = (TextView) findViewById(R.id.multitracker_keyinfo);
		mListDeviceList = (ListView) findViewById(R.id.device_list);

		setKeyInfoText("No GPS devices detected");

		mListDeviceList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				String errorMessage = "";
				List<Address> addresses = null;
				try {
					addresses = mGeocoder.getFromLocation(
							((MsgListViewAdapter) parent.getAdapter()).getItem(position).getLatLng().latitude,
							((MsgListViewAdapter) parent.getAdapter()).getItem(position).getLatLng().longitude,
							1);
				} catch (IOException ioException) {
					Log.e(getClass().getSimpleName(), errorMessage, ioException);
				} catch (IllegalArgumentException illegalArgumentException) {
					Log.e(getClass().getSimpleName(), errorMessage
							+ ((MsgListViewAdapter) parent.getAdapter()).getItem(position).getLatLng()
									.toString(), illegalArgumentException);
				}

				// Handle case where no address was found.
				if (addresses == null /* || addresses.size() == 0 */) {
					if (errorMessage.isEmpty()) {
						Log.e(getClass().getSimpleName(), errorMessage);
					}
				} else if (addresses.size() > 0) {
					makeToastMessage(addresses.get(0).getAddressLine(0));
				}

			}

		});
	}

	private void makeToastMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	private void setKeyInfoText(CharSequence pText) {
		this.mTextKeyInfo.setText(pText);
	}

	public void onClickBtnMap(View v) {
		Toast.makeText(this, "onButtonClick", Toast.LENGTH_SHORT).show();
		// startActivity(new Intent(this, MultiMapActivity.class));

		Intent intent = new Intent(this, MultiMapActivity.class);
		if (mLastLocation != null) {
			intent.putExtra(EXTRA_GPS_UPDATE, mRequestingLocationUpdates);
			intent.putExtra(EXTRA_LATITUDE, String.valueOf(mLastLocation.getLatitude()));
			intent.putExtra(EXTRA_LONGITUDE, String.valueOf(mLastLocation.getLongitude()));
			startActivity(intent);
		} else {
			Toast.makeText(this, "Your location is not detected yet!", Toast.LENGTH_SHORT).show();
		}

	}

	private void upDateGpsLocation() {
		mLastLocation = LocationServices.FusedLocationApi
				.getLastLocation(mGoogleApiClient);

		String errorMessage = "";
		List<Address> addresses = null;
		try {
			addresses = mGeocoder.getFromLocation(
					mLastLocation.getLatitude(),
					mLastLocation.getLongitude(),
					// In this sample, get just a single address.
					1);
		} catch (IOException ioException) {
			Log.e(getClass().getSimpleName(), errorMessage, ioException);
		} catch (IllegalArgumentException illegalArgumentException) {
			Log.e(getClass().getSimpleName(), errorMessage + ". " +
					"Latitude = " + mLastLocation.getLatitude() +
					", Longitude = " +
					mLastLocation.getLongitude(), illegalArgumentException);
		}

		// Handle case where no address was found.
		if (addresses == null /* || addresses.size() == 0 */) {
			if (errorMessage.isEmpty()) {
				Log.e(getClass().getSimpleName(), errorMessage);
			}
		} else if (addresses.size() > 0) {
			// mTvYouAreAtPlace.setText(addresses.get(0).getAddresSsLine(0));
			// updateDistanceBetween();
			setKeyInfoText(addresses.get(0).getAddressLine(0));
		}
	}

	@Override
	public void onLocationChanged(Location arg0) {
		upDateGpsLocation();
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult result) {
		Log.i(this.getClass().getSimpleName(), "Connection failed: ConnectionResult.getErrorCode() = "
				+ result.getErrorCode());

	}

	@Override
	public void onConnected(@Nullable Bundle connectionHint) {
		upDateGpsLocation();
		Toast.makeText(this, "GPS is connected", Toast.LENGTH_SHORT).show();
		if (mRequestingLocationUpdates) {
			startLocationUpdates();
		}
		mHandler.sendEmptyMessageDelayed(0, 80);
	}

	@Override
	public void onConnectionSuspended(int cause) {
		mGoogleApiClient.connect();
	}

	public class LoRaGpsDevice implements Cloneable {
		private LatLng mLatLng = null;
		private Date creationDate;
		private Location myLocation = null;

		public LoRaGpsDevice(String pLocationName) {
			myLocation = new Location(pLocationName);
		}

		public LoRaGpsDevice setLatLng(LatLng pLatLng) {
			mLatLng = pLatLng;
			if (myLocation != null) {
				myLocation.setLatitude(mLatLng.latitude);
				myLocation.setLongitude(mLatLng.longitude);
			}

			return this;
		}

		public LatLng getLatLng() {
			return this.mLatLng;
		}

		public LoRaGpsDevice setCreationDate(Date pDate) {
			creationDate = pDate;
			return this;
		}

		public Date getCreationDate() {
			return this.creationDate;
		}

		public Location getLocation() {
			return myLocation;
		}

		public String getProvider() {
			return myLocation.getProvider();
		}

		@Override
		protected LoRaGpsDevice clone() throws CloneNotSupportedException {
			LoRaGpsDevice pLoraDevice = (LoRaGpsDevice) super.clone();
			pLoraDevice.mLatLng = this.mLatLng;
			pLoraDevice.creationDate = this.creationDate;

			return pLoraDevice;
		}
	}

	private void initDevice(MyDevices pDevice) {
		MyThingPlugDevices myThingPlugDevices = MyThingPlugDevices.getInstance();

		mapDevice = new ThingPlugDevice(
				myThingPlugDevices.getServiceName(pDevice),
				myThingPlugDevices.getSclId(pDevice),
				myThingPlugDevices.getDeviceId(pDevice),
				myThingPlugDevices.getAuthId(pDevice),
				myThingPlugDevices.getAuthKey(pDevice))
				.setTag("RoLa GPS")
				.registerDevice(true);

		THING_AUTHORIZATION = mapDevice.getAuthorization();
		THING_REQ_URI = mapDevice.getUrlContenInstancesDetailed(0, 10).toString();

	}

	private synchronized void getThingPlugDeviceContent() {
		if (mapDevice == null) {
			initDevice(mCheckDevice);
		}

		if (isActivated == false) {
			Log.d("ThingPlugReq", "Activity is paused~~ ");
			return;
		}

		if (mRequestQueue == null) {
			mRequestQueue = Volley.newRequestQueue(this);
		}

		mRequestQueue.add(
				new StringRequest(Request.Method.GET, THING_REQ_URI, new Response.Listener<String>() {

					@Override
					public void onResponse(String response) {
						try {
							Log.v("gpsBug", response);
							JSONObject jsonObject = XML.toJSONObject(response);

							updateDeviceLocation(jsonObject);
						} catch (JSONException e) {
							e.printStackTrace();
							// Toast.makeText(getApplicationContext(),
							// e.toString(), Toast.LENGTH_SHORT).show();
						}
					}

				}, new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						Toast.makeText(getApplicationContext(), ":Error occured",
								Toast.LENGTH_SHORT).show();
					}
				}) {

					@Override
					public Map<String, String> getHeaders() throws AuthFailureError {
						Map<String, String> headers = new HashMap<String, String>();
						headers.put("Content-Type", "application/xml");
						headers.put("Authorization", THING_AUTHORIZATION);
						headers.put("charset", "UTF-8");

						return headers;
					}
				});

	}

	private JsonResponseContentInstanceDetailedLastOne toJsonResponseOne(JSONObject pJsonObject) {
		try {
			Log.d("json", pJsonObject.toString(3));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Type type = new TypeToken<JsonResponseContentInstanceDetailedLastOne>() {
		}.getType();

		JsonResponseContentInstanceDetailedLastOne response = new GsonBuilder().create().fromJson(
				pJsonObject.toString(), type);

		return response;
	}

	private JsonResponseContentInstancesDetailed toJsonResponseContentInstancesDetailed(JSONObject pJsonObject) {
		try {
			Log.d("json", pJsonObject.toString(3));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Type type = new TypeToken<JsonResponseContentInstancesDetailed>() {
		}.getType();
		JsonResponseContentInstancesDetailed response = new GsonBuilder().create().fromJson(
				pJsonObject.toString(), type);

		return response;
	}

	private ArrayList<JsonContentInstanceDetail> getContentInstanceDetails(JSONObject pJsonObject) {
		ArrayList<JsonContentInstanceDetail> result = null;// = new
															// ArrayList<JsonContentInstanceDetail>();
		int currentNrOfInstances = 0;
		if (pJsonObject.has("contentInstances")) {
			try {
				currentNrOfInstances = pJsonObject.getJSONObject("contentInstances").getInt(
						"currentNrOfInstances");
			} catch (Exception e) {
				e.printStackTrace();
			}
			// Log.v("jsonObj", String.valueOf(currentNrOfInstances));
			if (currentNrOfInstances == 1) {
				Type type = new TypeToken<JsonContentInstanceDetail>() {
				}.getType();

				try {
					JsonContentInstanceDetail detailObj = new GsonBuilder().create().fromJson(
							pJsonObject
									.getJSONObject("contentInstances")
									.getJSONObject("contentInstancesCollection")
									.getString("contentInstance"), type);
					result = new ArrayList<JsonContentInstanceDetail>();
					result.add(detailObj);

				} catch (JsonSyntaxException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}

			} else if (currentNrOfInstances > 1) {
				result = new ArrayList<JsonContentInstanceDetail>(toJsonResponseContentInstancesDetailed(
						pJsonObject).getContentInstanceDetails());
			}
		}
		return result;
	}

	private void updateDeviceLocation(JSONObject pJsonObject) {
		ArrayList<JsonContentInstanceDetail> detailArray = getContentInstanceDetails(pJsonObject);

		if (detailArray == null) {
			this.mTextKeyInfo.setText("No GPS devices detected");
		} else {
			checkGpsDevices(detailArray);
		}
	}

	private void checkGpsDevices(ArrayList<JsonContentInstanceDetail> detailArray) {
		for (JsonContentInstanceDetail pJsonContentInstanceDetail : detailArray) {
			LoRaGpsDevice tempDevice = getValidGpsDevice(pJsonContentInstanceDetail);

			if (mLoRaGpsDeviceList == null) {
				mLoRaGpsDeviceList = new ArrayList<MultiTrackerListActivity.LoRaGpsDevice>();
				mLoRaGpsDeviceList.add(tempDevice);
				upDateDataList();
			} else {
				boolean hasAlready = false;
				for (LoRaGpsDevice device : mLoRaGpsDeviceList) {
					if (device.getProvider().equalsIgnoreCase(tempDevice.getProvider())) {
						hasAlready = true;

						if (tempDevice.getCreationDate().after(device.getCreationDate())) {
							device.setLatLng(tempDevice.getLatLng()).setCreationDate(
									tempDevice.getCreationDate());
							upDateDataList();
						}
						break;
					}
				}
				if (hasAlready == false) {
					mLoRaGpsDeviceList.add(tempDevice);
					upDateDataList();
				}
			}
		}
	}

	private LoRaGpsDevice getValidGpsDevice(JsonContentInstanceDetail pJsonContentInstanceDetail) {
		LoRaGpsDevice result = null;

		double latitude = 0, longitude = 0;
		String gpsDeviceName, latitudeString, longitudeString;

		String[] geoString = pJsonContentInstanceDetail.getContent().split(DELIMITER);
		Date creationDate = pJsonContentInstanceDetail.getCreationTime();

		gpsDeviceName = geoString.length == 2 ? "noName" : geoString[0];
		latitudeString = geoString.length == 2 ? geoString[0] : geoString[1];
		longitudeString = geoString.length == 2 ? geoString[1] : geoString[2];

		if (creationDate != null) {
			String doublePtnString = "^[\\+\\-]{0,1}[0-9]+[\\.\\,]{1}[0-9]+$";

			try {
				latitude = Double
						.valueOf(latitudeString.replace(doublePtnString, ""));
				longitude = Double
						.valueOf(longitudeString.replace(doublePtnString, ""));

				result = new LoRaGpsDevice(gpsDeviceName)
						.setLatLng(new LatLng(latitude, longitude))
						.setCreationDate(creationDate);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	private void upDateDataList() {
		if (mMsgListViewAdapter == null) {
			mMsgListViewAdapter = new MsgListViewAdapter(this, R.layout.msg_listview_item,
					this.mLoRaGpsDeviceList);
			mListDeviceList.setAdapter(mMsgListViewAdapter);
		} else {
			mMsgListViewAdapter.notifyDataSetChanged();
		}

	}

	private class MsgListViewAdapter extends ArrayAdapter<LoRaGpsDevice> {

		public MsgListViewAdapter(Context context, int resource, ArrayList<LoRaGpsDevice> objects) {
			super(context, resource, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			LoRaGpsDevice item = getItem(position);
			ViewHolder viewHolder;

			if (v == null) {
				viewHolder = new ViewHolder();
				LayoutInflater vi = LayoutInflater.from(getContext());
				v = vi.inflate(R.layout.msg_listview_item, parent, false);

				viewHolder.msg = (TextView) v.findViewById(R.id.msgListMsgText);
				viewHolder.date = (TextView) v.findViewById(R.id.msgListDateText);

				v.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) v.getTag();

			}

			if (item != null) {
				viewHolder.msg.setText("GPS " + item.getProvider() + "\n   " + item.getLatLng().toString());
				viewHolder.date.setText(item.getCreationDate().toLocaleString());
			}

			return v;
		}

	}

	public static class ViewHolder {
		TextView msg;
		TextView date;
	}

}
