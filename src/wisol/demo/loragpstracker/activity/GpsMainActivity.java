package wisol.demo.loragpstracker.activity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import wisol.demo.loragpstracker.JsonContentInstanceDetail;
import wisol.demo.loragpstracker.JsonResponseContentInstanceDetailedLastOne;
import wisol.demo.loragpstracker.MyThingPlugDevices;
import wisol.demo.loragpstracker.MyThingPlugDevices.MyDevices;
import wisol.demo.loragpstracker.R;
import wisol.demo.loragpstracker.TestService;
import wisol.demo.loragpstracker.ThingPlugDevice;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.gson.reflect.TypeToken;

public class GpsMainActivity extends Activity
		implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

	final String EXTRA_GPS_UPDATE = "GPS_UPDATE";
	final String EXTRA_LATITUDE = "LATITUDE";
	final String EXTRA_LONGITUDE = "LONGITUDE";

	final String SHARE_SAVE_TAG_NAME = " wisol.demo.loragpstracker.tagname";

	final String DELIMITER = ",";

	TextView mTvDistance;
	TextView mTvYouAreAtPlace;
	ImageView mImageGeoMarkTop;
	TextView mTvGpsTagName;
	TextView mTvGpsIsAtPlace;
	TextView mTvGpsDeviceGetTime;
	ImageView mIvPhoneActive, mIvGpsActive;

	private Location mLastLocation;
	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;

	private static int UPDATE_INTERVAL = 5000;
	private static int FATEST_INTERVAL = 1000;
	private static int DISPLACEMENT = 1;

	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

	Geocoder mGeocoder;
	private boolean mRequestingLocationUpdates = false;
	static boolean isActivated = false;

	Handler mHandler;
	ThingPlugDevice mapDevice;
	String THING_AUTHORIZATION;
	String THING_REQ_URI;

	LoRaGpsDevice mLoRaGpsNow;

	private final long REQ_FAST = 1000;
	private final long REQ_NORMAL = 3000;
	final MyDevices mCheckDevice = MyDevices.GPS01C;// MyDevices.GPS01C;//MyDevices.GPS02;
	RequestQueue mRequestQueue;

	private void initUiComponents() {
		mImageGeoMarkTop = (ImageView) findViewById(R.id.gpsmain_mark);
		mTvDistance = (TextView) findViewById(R.id.gpsmain_distance);
		mTvYouAreAtPlace = (TextView) findViewById(R.id.gpsmain_youareatplace);
		mTvGpsTagName = (TextView) findViewById(R.id.gpsmain_gpsisat);
		mTvGpsIsAtPlace = (TextView) findViewById(R.id.gpsmain_gpsisatplace);
		mTvGpsDeviceGetTime = (TextView) findViewById(R.id.gpsmain_gpsattime);
		mIvPhoneActive = (ImageView) findViewById(R.id.phoneActive);
		mIvGpsActive = (ImageView) findViewById(R.id.gpsActive);
	}

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
		stopService(new Intent(this, TestService.class));
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
	protected void onPause() {
		super.onPause();
		stopLocationUpdates();
		mHandler.removeMessages(0);
		// launchTestService();
		isActivated = false;
	}

	public void launchTestService() {
		Intent i = new Intent(this, TestService.class);

		startService(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_gps_main);

		initUiComponents();
		getSavedGpsTagName();
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

	private JsonResponseContentInstanceDetailedLastOne toJsonResponse(JSONObject pJsonObject) {
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

	private void updateDeviceLocation(JSONObject pJsonObject) {
		JsonResponseContentInstanceDetailedLastOne response = toJsonResponse(pJsonObject);

		long delayTime = 10000;

		if (response.getCurrentNrOfInstances() != 0) {
			if (checkDeviceLocation(response.getContentInstanceDetail())) {
				checkGpsActivation();
			}

		} else {
			updateGpsDeviceLocation("GPS device is not found");
		}
		// mHandler.sendEmptyMessageDelayed(0, delayTime);
	}

	private boolean checkDeviceLocation(JsonContentInstanceDetail pJsonContentInstanceDetail) {
		boolean result = false;
		double latitude = 0, longitude = 0;

		String[] geoString = pJsonContentInstanceDetail.getContent().split(DELIMITER);
		Date creationDate = pJsonContentInstanceDetail.getCreationTime();

		if ((geoString.length == 2) && (creationDate != null)) {
			result = true;
			String doublePtnString = "^[\\+\\-]{0,1}[0-9]+[\\.\\,]{1}[0-9]+$";

			try {
				latitude = Double
						.valueOf(geoString[0].replace(doublePtnString, ""));
				longitude = Double
						.valueOf(geoString[1].replace(doublePtnString, ""));

				if (mLoRaGpsNow == null) {
					mLoRaGpsNow = new LoRaGpsDevice("LoRa GPS device")
							.setLatLng(new LatLng(latitude, longitude))
							.setCreationDate(creationDate);

					updateGpsDeviceLocation();
				} else {
					if (creationDate.after(mLoRaGpsNow.getCreationDate())) {
						mLoRaGpsNow.setLatLng(new LatLng(latitude, longitude));
						mLoRaGpsNow.setCreationDate(creationDate);
						updateGpsDeviceLocation();
					}
				}
			} catch (Exception e) {
				result = false;
				e.printStackTrace();
			}
		}

		return result;
	}

	// private void initDevice() {
	// MyThingPlugDevices myThingPlugDevices = MyThingPlugDevices.getInstance();
	//
	// mapDevice = new ThingPlugDevice(
	// myThingPlugDevices.getServiceName(MyDevices.GPS02),
	// myThingPlugDevices.getSclId(MyDevices.GPS02),
	// myThingPlugDevices.getDeviceId(MyDevices.GPS02),
	// myThingPlugDevices.getAuthId(MyDevices.GPS02),
	// myThingPlugDevices.getAuthKey(MyDevices.GPS02))
	// .setTag("RoLa GPS")
	// .registerDevice(true);
	//
	// THING_AUTHORIZATION = mapDevice.getAuthorization();
	// THING_REQ_URI = mapDevice.getUrlContenInstancesDetailed(0, 1).toString();
	//
	// }

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
		THING_REQ_URI = mapDevice.getUrlContenInstancesDetailed(0, 1).toString();

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

	private void updateDistanceBetween() {
		try {
			float pDistanceMeter = mLastLocation.distanceTo(mLoRaGpsNow.getLocation());
			String pDistanceStr = "";
			if (pDistanceMeter < 1000) {// display by unit m
				pDistanceStr = String.valueOf(pDistanceMeter) + "m";
			} else {// display by unit km
				pDistanceStr = String.format("%.2f", pDistanceMeter / 1000.0f) + "km";
			}
			mTvDistance.setText(String.valueOf(pDistanceStr));
		} catch (Exception e) {
			e.printStackTrace();
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
			mTvYouAreAtPlace.setText(addresses.get(0).getAddressLine(0));
			updateDistanceBetween();
		}
	}

	private void setPhoneGpsActive() {
		this.mIvPhoneActive.setImageResource(R.drawable.checked_circle);
	}

	private void updateGpsDeviceLocation() {
		String errorMessage = "";
		List<Address> addresses = null;
		try {
			addresses = mGeocoder.getFromLocation(
					mLoRaGpsNow.getLatLng().latitude,
					mLoRaGpsNow.getLatLng().longitude,
					1);
		} catch (IOException ioException) {
			Log.e(getClass().getSimpleName(), errorMessage, ioException);
		} catch (IllegalArgumentException illegalArgumentException) {
			Log.e(getClass().getSimpleName(), errorMessage + ". " +
					"Latitude = " + mLoRaGpsNow.getLatLng().latitude +
					", Longitude = " +
					mLoRaGpsNow.getLatLng().longitude, illegalArgumentException);
		}

		// Handle case where no address was found.
		if (addresses == null || addresses.size() == 0) {
			if (errorMessage.isEmpty()) {
				Log.e(getClass().getSimpleName(), errorMessage);
			}
		} else {
			mTvGpsIsAtPlace.setText(addresses.get(0).getAddressLine(0));

			updateDistanceBetween();
		}
	}

	private void checkGpsActivation() {
		mTvGpsDeviceGetTime.setText("@ " + mLoRaGpsNow.getCreationDate().toLocaleString());
		Calendar.getInstance().setTime(new Date());
		// new Date(Calendar.getInstance().getTime());
		Log.v("mil",
				String.valueOf(Calendar.getInstance().getTime().getTime()
						- mLoRaGpsNow.getCreationDate().getTime()));
		if ((Calendar.getInstance().getTime().getTime() - mLoRaGpsNow.getCreationDate().getTime()) < 600000) {
			this.mIvGpsActive.setImageResource(R.drawable.checked_circle);
		} else {
			this.mIvGpsActive.setImageResource(R.drawable.unchecked_circle);
		}

	}

	private void updateGpsDeviceLocation(String pDeviceLocationStr) {
		mTvGpsIsAtPlace.setText(pDeviceLocationStr);
	}

	@Override
	public void onBackPressed() {
		this.finishAffinity();
	}

	@Override
	public void onLocationChanged(Location location) {
		// mLastLocation = LocationServices.FusedLocationApi
		// .getLastLocation(mGoogleApiClient);

		// Toast.makeText(getApplicationContext(), "Location changed!",
		// Toast.LENGTH_SHORT).show();
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
		setPhoneGpsActive();
		if (mRequestingLocationUpdates) {
			startLocationUpdates();
		}
		mHandler.sendEmptyMessageDelayed(0, 80);
	}

	@Override
	public void onConnectionSuspended(int cause) {
		mGoogleApiClient.connect();
	}

	public void onClickMark(View v) {
		if (mRequestingLocationUpdates) {
			mRequestingLocationUpdates = false;
			stopLocationUpdates();
			mImageGeoMarkTop.setImageResource(R.drawable.mark_uarat);
		} else {
			mRequestingLocationUpdates = true;
			startLocationUpdates();
			mImageGeoMarkTop.setImageResource(R.drawable.mark_uaratupdate);
		}
	}

	public void onClickMenuMap(View v) {
		Intent intent = new Intent(this, MapActivity.class);
		if (mLastLocation != null) {
			intent.putExtra(EXTRA_GPS_UPDATE, mRequestingLocationUpdates);
			intent.putExtra(EXTRA_LATITUDE, String.valueOf(mLastLocation.getLatitude()));
			intent.putExtra(EXTRA_LONGITUDE, String.valueOf(mLastLocation.getLongitude()));
			startActivity(intent);
		} else {
			Toast.makeText(this, "Your location is not detected yet!", Toast.LENGTH_SHORT).show();
		}
	}

	public void onClickMenuConfig(View v) {
		showNameInputDialog("GPS Tag Name");
	}

	protected void showNameInputDialog(final String title) {
		// get prompts.xml view
		LayoutInflater layoutInflater = LayoutInflater.from(this);
		View promptView = layoutInflater.inflate(R.layout.input_dialog, null);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setView(promptView);

		final EditText editText = (EditText) promptView.findViewById(R.id.inputDialogInputEditText);
		// setup a dialog window
		alertDialogBuilder.setCancelable(false).setTitle(title)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// resultText.setText("Hello, " + editText.getText());
						// changeDoorName(title, editText.getText().toString());
						saveGpsTagName(editText.getText().toString());
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

		// create an alert dialog
		AlertDialog alert = alertDialogBuilder.create();

		alert.show();
	}

	private void saveGpsTagName(String newName) {
		this.mTvGpsTagName.setText(newName);
		saveSharedPreference(this.SHARE_SAVE_TAG_NAME, newName);
	}

	private void saveSharedPreference(String pKey, String pValue) {
		SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(pKey, pValue);

		editor.commit();
	}

	private void getSavedGpsTagName() {
		SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
		mTvGpsTagName.setText(sharedPref.getString(this.SHARE_SAVE_TAG_NAME, "Gps tag is"));
	}

	static public class WeakHandler extends Handler {
		private final WeakReference<GpsMainActivity> mHandlerObj;

		public WeakHandler(GpsMainActivity pHandlerObj) {
			mHandlerObj = new WeakReference<GpsMainActivity>(pHandlerObj);
		}
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
}
