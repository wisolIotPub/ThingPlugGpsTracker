package wisol.demo.loragpstracker.activity;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import wisol.demo.loragpstracker.TestService;
import wisol.demo.loragpstracker.ThingPlugDevice;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
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
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class MultiMapActivity extends FragmentActivity
		implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener,
		LocationListener,
		GoogleMap.OnMarkerClickListener,
		GoogleMap.OnInfoWindowClickListener{

	final String EXTRA_GPS_UPDATE = "GPS_UPDATE";
	final String EXTRA_LATITUDE = "LATITUDE";
	final String EXTRA_LONGITUDE = "LONGITUDE";

	final String DELIMITER = ",";

	ArrayList<LoRaGpsDevice> mLoRaGpsDeviceList;

	// TextView mTvMapDebug;
	// TextView mTvDistance;
	Handler mHandler;
	GoogleMap mGoogleMap;
	GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;

	LatLng mGatewayLatLng;
	Location mGatewayLocation;
	Marker mGatewayMarker;
	// Marker mMarkerDeviceMax;
	Marker mMarkerDeviceNow;

	ThingPlugDevice mapDevice;

	String THING_AUTHORIZATION;
	String THING_REQ_URI;
	// ArrayList<LoRaGpsDevice> mLoRaGpsDevices;
	// LoRaGpsDevice mLoRaGpsMaxDistance;
	LoRaGpsDevice mLoRaGpsNow;

	// Circle mCircleMaxDistanceRange;
	Circle mCircleNowDistanceRange;

	static boolean isActivated = false;

	private static int UPDATE_INTERVAL = 10000;
	private static int FATEST_INTERVAL = 5000;
	private static int DISPLACEMENT = 5;
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 2000;

	private boolean mRequestingLocationUpdates = false;

	private final long REQ_FAST = 1000;
	private final long REQ_NORMAL = 3000;
	final MyDevices mCheckDevice = MyDevices.GPS02;// MyDevices.MAP;//MyDevices.GPS02;
	RequestQueue mRequestQueue;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_multi_map);

		// mTvMapDebug = (TextView) findViewById(R.id.tv_mapdebug);
		// mTvDistance = (TextView) findViewById(R.id.map_distancetext);

		getExtra(savedInstanceState);
		initDevice(mCheckDevice);

		if (checkPlayServices()) {
			buildGoogleApiClient();
			createLocationRequest();
		}

		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.multi_map);
		mapFragment.getMapAsync(this);

		mHandler = new WeakHandler(this) {

			@Override
			public void handleMessage(Message msg) {
				Log.v("handlerCheck", "mapin");
				if (isNetworkConnected()) {
					getThingPlugDeviceContent();
					this.sendEmptyMessageDelayed(0, REQ_NORMAL);
				} else {
					this.sendEmptyMessageDelayed(0, REQ_NORMAL * 2);
				}
			}
		};
	}

	private boolean isNetworkConnected() {
		boolean result = false;
		ConnectivityManager manager = (ConnectivityManager) getApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final boolean isMobileConnected = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();
		final boolean isWifiConnected = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();

		result = isMobileConnected | isWifiConnected;

		return result;
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
		// TODO Auto-generated method stub
		// stopService(new Intent(this, TestService.class));
		super.onResume();

		checkPlayServices();
		if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
			startLocationUpdates();
		}

		isActivated = true;
	}

	public void launchTestService() {
		Intent i = new Intent(this, TestService.class);

		startService(i);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		stopLocationUpdates();
		mHandler.removeMessages(0);
		// launchTestService();
		isActivated = false;
		super.onPause();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		isActivated = false;
		super.onStop();

		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}

	static public class WeakHandler extends Handler {
		private final WeakReference<FragmentActivity> mHandlerObj;

		public WeakHandler(FragmentActivity pHandlerObj) {
			mHandlerObj = new WeakReference<FragmentActivity>(pHandlerObj);
		}
	}

	public void onClickSendButton(View v) {
		Toast.makeText(this, "This function is not supported yet", Toast.LENGTH_SHORT).show();
		Log.d("putTetst", mapDevice.getUrlGwDeviceReset(Request.Method.PUT).toString());
		if (mRequestQueue == null) {
			mRequestQueue = Volley.newRequestQueue(this);
		}

		mRequestQueue.add(
				new StringRequest(Request.Method.PUT, mapDevice.getUrlGwDeviceReset(Request.Method.PUT)
						.toString(),
						new Response.Listener<String>() {

							@Override
							public void onResponse(String response) {
								try {
									JSONObject jsonObject = XML.toJSONObject(response);
									Log.d("putTetst", jsonObject.toString());

								} catch (JSONException e) {
									e.printStackTrace();
									Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT)
											.show();
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

	public void onClickGpsFocusButton(View v) {
		try {
			updateCameraBounds(CameraBoundOption.GPS_ONLY);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void onClickPhoneFocusButton(View v) {
		try {
			updateCameraBounds(CameraBoundOption.PHONE_ONLY);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onClickBetweenFocusButton(View v) {
		try {
			updateCameraBounds(CameraBoundOption.GPS_PHONE);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	// // mLoRaGpsDevices = new ArrayList<MapActivity.LoRaGpsDevice>();
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
		THING_REQ_URI = mapDevice.getUrlContenInstancesDetailed(0, 10).toString();

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

		ArrayList<JsonContentInstanceDetail> detailArray = getContentInstanceDetails(pJsonObject);

		if (detailArray == null) {
			// this.mTextKeyInfo.setText("No GPS devices detected");
		} else {
			checkGpsDevices(detailArray);
		}

		// mHandler.sendEmptyMessageDelayed(0, delayTime);
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

	private void checkGpsDevices(ArrayList<JsonContentInstanceDetail> detailArray) {
		for (JsonContentInstanceDetail pJsonContentInstanceDetail : detailArray) {
			LoRaGpsDevice tempDevice = getValidGpsDevice(pJsonContentInstanceDetail);

			if (mLoRaGpsDeviceList == null) {
				mLoRaGpsDeviceList = new ArrayList<LoRaGpsDevice>();
				mLoRaGpsDeviceList.add(tempDevice);
				tempDevice.updateMarker();
			} else {
				boolean hasAlready = false;
				for (LoRaGpsDevice device : mLoRaGpsDeviceList) {
					Log.v("multigeo",
							device.getProvider()
									+ ","
									+ tempDevice.getProvider()
									+ ","
									+ String.valueOf(device.getProvider().equalsIgnoreCase(
											tempDevice.getProvider())));
					if (device.getProvider().equalsIgnoreCase(tempDevice.getProvider())) {
						hasAlready = true;

						if (tempDevice.getCreationDate().after(device.getCreationDate())) {
							// device = tempDevice;
							device.setLatLng(tempDevice.getLatLng()).setCreationDate(
									tempDevice.getCreationDate());
							device.updateMarker();
						}
						break;
					}
				}
				if (hasAlready == false) {
					mLoRaGpsDeviceList.add(tempDevice);
					tempDevice.updateMarker();
				}
			}
		}
	}

	private void updateDeviceLocationMarker() {

		if (mMarkerDeviceNow == null) {

			mMarkerDeviceNow = mGoogleMap.addMarker(new MarkerOptions()
					.position(mLoRaGpsNow.getLatLng())
					.draggable(false)
					.title(mLoRaGpsNow.getCreationDate().toString())
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.mk_circle))
					.flat(true));

		} else {
			mMarkerDeviceNow.setPosition(mLoRaGpsNow.getLatLng());
			mMarkerDeviceNow.setTitle(mLoRaGpsNow.getCreationDate().toString());
		}

		mMarkerDeviceNow.setSnippet(updateDistanceDisplay(getDistanceFromGateway(mLoRaGpsNow)));
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
					mLoRaGpsNow = new LoRaGpsDevice("LoRa GPS divice")
							.setLatLng(new LatLng(latitude, longitude))
							.setCreationDate(creationDate);
					updateBetweenCircleRange(CameraBoundOption.GPS_PHONE);
				} else {
					if (creationDate.after(mLoRaGpsNow.getCreationDate())) {
						mLoRaGpsNow
								.setLatLng(new LatLng(latitude, longitude))
								.setCreationDate(creationDate);
						updateBetweenCircleRange(CameraBoundOption.NONE);
					}
				}

			} catch (Exception e) {
				result = false;
				e.printStackTrace();
			}
		}

		return result;
	}

	private void updateBetweenCircleRange(CameraBoundOption pOption) {
		if (mCircleNowDistanceRange == null) {
			mCircleNowDistanceRange = mGoogleMap
					.addCircle(new CircleOptions()
							.center(mGatewayLatLng)
							.radius((double) getDistanceFromGateway(mLoRaGpsNow))
							.fillColor(0x2c5a7fb1)
							.strokeWidth(0));
		} else {
			mCircleNowDistanceRange.setRadius((double) getDistanceFromGateway(mLoRaGpsNow));
			mCircleNowDistanceRange.setCenter(mGatewayLatLng);
		}

		updateCameraBounds(pOption);
	}

	private void updateCameraBounds(CameraBoundOption pOption) {
		switch (pOption) {
		case GPS_PHONE:
			double diffLat = 0;
			double diffLng = 0;
			for (LoRaGpsDevice loraD : mLoRaGpsDeviceList) {
				if (Math.abs(mGatewayLatLng.latitude - loraD.getLatLng().latitude) > diffLat) {
					diffLat = Math.abs(mGatewayLatLng.latitude - loraD.getLatLng().latitude);
				}
				if (Math.abs(mGatewayLatLng.longitude - loraD.getLatLng().longitude) > diffLng) {
					diffLng = Math.abs(mGatewayLatLng.longitude - loraD.getLatLng().longitude);
				}

			}
			// double diffLat = mGatewayLatLng.latitude -
			// mLoRaGpsNow.getLatLng().latitude;
			// double diffLng = mGatewayLatLng.longitude -
			// mLoRaGpsNow.getLatLng().longitude;
			double d = Math.sqrt(Math.pow(diffLat, 2) + Math.pow(diffLng, 2));

			double northEastLat = mGatewayLatLng.latitude + d;
			double northEastLng = mGatewayLatLng.longitude + d;
			double southWestLat = mGatewayLatLng.latitude - d;
			double southWestLng = mGatewayLatLng.longitude - d;

			LatLngBounds pBounds = new LatLngBounds(new LatLng(southWestLat, southWestLng), new LatLng(
					northEastLat, northEastLng));
			mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(pBounds, 100), 1000, null);
			break;

		case GPS_ONLY:
			mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLoRaGpsNow.getLatLng(), 17));
			break;
		case PHONE_ONLY:
			mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mGatewayLatLng, 17));
			break;
		default:

			break;
		}

	}

	private float getDistanceFromGateway(LoRaGpsDevice pDevice) {
		float resultMeter = 0;

		Location pTargetLocation = new Location("Lora Device");
		pTargetLocation.setLatitude(pDevice.getLatLng().latitude);
		pTargetLocation.setLongitude(pDevice.getLatLng().longitude);

		resultMeter = mGatewayLocation.distanceTo(pTargetLocation);

		return resultMeter;
	}

	private String updateDistanceDisplay(float pResultMeter) {
		String pDistanceStr = "";
		if (pResultMeter < 5) {
			pDistanceStr = "Nearby ";
		} else if (pResultMeter < 1000) {// display by unit m
			pDistanceStr = String.valueOf(pResultMeter) + "m";
			pDistanceStr = String.format("%.2f", pResultMeter) + "m";
		} else {// display by unit km
			pDistanceStr = String.format("%.2f", pResultMeter / 1000.0f) + "km";
		}
		// mTvDistance.setText(String.valueOf(pDistanceStr));

		return pDistanceStr;
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
							JSONObject jsonObject = XML.toJSONObject(response);

							updateDeviceLocation(jsonObject);
						} catch (JSONException e) {
							e.printStackTrace();
							// Toast.makeText(getApplicationContext(),
							// e.toString(), Toast.LENGTH_SHORT)
							// .show();
						}
					}

				}, new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						// Toast.makeText(getApplicationContext(),
						// ":Error occured",
						// Toast.LENGTH_SHORT).show();
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

	private void getExtra(Bundle pBundle) {
		mRequestingLocationUpdates = getIntent().getBooleanExtra(EXTRA_GPS_UPDATE, false);
		String latitude = getIntent().getStringExtra(EXTRA_LATITUDE);
		String longitude = getIntent().getStringExtra(EXTRA_LONGITUDE);
		double doubleLatitude = Double.valueOf(latitude);
		double doubleLongitude = Double.valueOf(longitude);

		if (mGatewayLatLng == null) {
			mGatewayLatLng = new LatLng(doubleLatitude, doubleLongitude);
			mGatewayLocation = new Location("You");
			mGatewayLocation.setLatitude(doubleLatitude);
			mGatewayLocation.setLongitude(doubleLongitude);
		}
	}

	private void upDateGpsLocation(CameraBoundOption pOption) {
		mGatewayLocation = LocationServices.FusedLocationApi
				.getLastLocation(mGoogleApiClient);

		mGatewayLatLng = new LatLng(mGatewayLocation.getLatitude(), mGatewayLocation.getLongitude());
		if (mGatewayMarker != null) {
			mGatewayMarker.setPosition(mGatewayLatLng);
			mGatewayMarker.setSnippet(mGatewayLatLng.toString());
		}

		if (mLoRaGpsNow != null) {
			updateDistanceDisplay(getDistanceFromGateway(mLoRaGpsNow));
			if (mMarkerDeviceNow != null) {
				mMarkerDeviceNow.setSnippet(updateDistanceDisplay(getDistanceFromGateway(mLoRaGpsNow)));
			}

			updateBetweenCircleRange(pOption);
		}

	}

	@Override
	public void onMapReady(GoogleMap pGoogleMap) {
		mGoogleMap = pGoogleMap;
		mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

		mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mGatewayLatLng, 17));
		mGatewayMarker = mGoogleMap.addMarker(new MarkerOptions()
				.position(mGatewayLatLng)
				.draggable(false)
				.title("You")
				.snippet(mGatewayLatLng.toString())
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.mk_blue))
				.flat(true));

		mGoogleMap.setOnMarkerClickListener(this);
		mGoogleMap.setOnInfoWindowClickListener(this);
	}

	@Override
	public void onConnected(Bundle pBundle) {
		Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show();

		upDateGpsLocation(CameraBoundOption.GPS_PHONE);

		if (mRequestingLocationUpdates) {
			startLocationUpdates();
		}
		mHandler.sendEmptyMessageDelayed(0, 15);
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		Toast.makeText(this, "onConnectionSuspended", Toast.LENGTH_SHORT).show();
		mGoogleApiClient.connect();

	}

	@Override
	public void onConnectionFailed(ConnectionResult pConnectionResult) {
		Toast.makeText(this, "onConnectionFailed", Toast.LENGTH_SHORT).show();

	}

	public class LoRaGpsDevice implements Cloneable {
		private LatLng mLatLng = null;
		private Date creationDate;
		private Location myLocation = null;
		private Marker mMarker;

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

		public void updateMarker() {
			int[] markers = { R.drawable.mk1, R.drawable.mk2, R.drawable.mk3, R.drawable.mk4, R.drawable.mk5,
					R.drawable.mk6 };
			// int mk = markers[0];// [mLoRaGpsDeviceList.indexOf(this) %
			// // markers.length];
			int mk = markers[mLoRaGpsDeviceList.indexOf(this) % markers.length];
			if (this.mMarker == null) {
				this.mMarker = mGoogleMap.addMarker(new MarkerOptions()
						.position(this.getLatLng())
						.draggable(false)
						.title(this.getProvider())
						.icon(BitmapDescriptorFactory.fromResource(mk))
						.flat(true));
			} else {
				this.mMarker.setPosition(this.getLatLng());
				this.mMarker.setTitle(this.getProvider());
			}

			this.mMarker.setSnippet("@" + this.getCreationDate().toString());

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

	@Override
	public void onLocationChanged(Location arg0) {
		// TODO Auto-generated method stub
		upDateGpsLocation(CameraBoundOption.NONE);

	}

	private enum CameraBoundOption {
		NONE, GPS_ONLY, PHONE_ONLY, GPS_PHONE
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
//		mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 17));
		return false;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 17));
		marker.hideInfoWindow();
		
	}

}
