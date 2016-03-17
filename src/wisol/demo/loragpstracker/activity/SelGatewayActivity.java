package wisol.demo.loragpstracker.activity;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
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
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
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

public class SelGatewayActivity extends Activity
		implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

	final String EXTRA_LOACTION = "locationData";
	final String EXTRA_LATITUDE = "LATITUDE";
	final String EXTRA_LONGITUDE = "LONGITUDE";

	ImageView mImageGeoMarkTop;
	TextView mTextViewLocation;
	Button mBtnGatewayHere;
	Button mBtnEditLocation;
	LinearLayout mLayoutManualInput;
	EditText mTextLatitude;
	EditText mTextLongitude;
	Button mBtnSetGateway;
	Button mBtnKnownGateway;
	LatLng mLatLngKnownGateway;
	boolean isLoadKnownGateway = false;

	ThingPlugDevice mGatewayDevice;
	String THING_AUTHORIZATION;
	String THING_REQ_URI;

	private static final String TAG = MainActivity.class.getSimpleName();
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
	private Location mLastLocation;
	// Google client to interact with Google API
	private GoogleApiClient mGoogleApiClient;
	// boolean flag to toggle periodic location updates
	private boolean mRequestingLocationUpdates = false;
	private LocationRequest mLocationRequest;

	// Location updates intervals in sec
	private static int UPDATE_INTERVAL = 20000;
	private static int FATEST_INTERVAL = 10000;
	private static int DISPLACEMENT = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sel_gateway);

		initUiComponents();
		initDevice();

		if (checkPlayServices()) {
			buildGoogleApiClient();
			createLocationRequest();
		}
		getThingPlugDeviceContentOfGateWay();
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

	private void displayLocation() {

		mLastLocation = LocationServices.FusedLocationApi
				.getLastLocation(mGoogleApiClient);

		if (mLastLocation != null) {
			double latitude = mLastLocation.getLatitude();
			double longitude = mLastLocation.getLongitude();

			mTextViewLocation.setText(latitude + ", " + longitude);
			mBtnGatewayHere.setVisibility(View.VISIBLE);

		} else {

			mTextViewLocation
					.setText("Couldn't get the location. Make sure location is enabled on the device");
		}
	}

	private void initUiComponents() {
		mImageGeoMarkTop = (ImageView) findViewById(R.id.iv_selgw_mark);
		mTextViewLocation = (TextView) findViewById(R.id.tv_selgw_location);
		mBtnGatewayHere = (Button) findViewById(R.id.btn_gatewayhere);
		mBtnEditLocation = (Button) findViewById(R.id.btn_editlocation);
		mLayoutManualInput = (LinearLayout) findViewById(R.id.selgateway_manual_location);
		mTextLatitude = (EditText) findViewById(R.id.manual_latitude);
		mTextLongitude = (EditText) findViewById(R.id.manual_longitude);
		mBtnSetGateway = (Button) findViewById(R.id.btn_setgateway);
		mBtnKnownGateway = (Button) findViewById(R.id.btn_knownlocation);

		mLayoutManualInput.setVisibility(View.GONE);
		mTextViewLocation.setText("HERE");
		mBtnGatewayHere.setVisibility(View.GONE);
		mBtnKnownGateway.setTextColor(0xff4d4d4d);
	}

	public void onClickYouAreAt(View v) {
		if (mImageGeoMarkTop.getVisibility() == View.GONE) {
			mLayoutManualInput.setVisibility(View.GONE);
			mImageGeoMarkTop.setVisibility(View.VISIBLE);
			mBtnEditLocation.setVisibility(View.VISIBLE);
		}
	}

	public void onClickUpdateGeo(View v) {
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

	public void onClickGatewayHere(View v) {
		if (mLastLocation != null) {
			startMapActivity(mLastLocation.getLatitude(), mLastLocation.getLongitude());
		} else {
			Toast.makeText(this, "Invalid location", Toast.LENGTH_SHORT).show();
		}

	}

	private void startMapActivity(double pLatitude, double pLongitude) {
		Intent intent = new Intent(this, MapActivity.class);
		intent.putExtra(EXTRA_LATITUDE, String.valueOf(pLatitude));
		intent.putExtra(EXTRA_LONGITUDE, String.valueOf(pLongitude));
		startActivity(intent);
	}

	public void onClickEditLocation(View v) {
		mLayoutManualInput.setVisibility(View.VISIBLE);
		mImageGeoMarkTop.setVisibility(View.GONE);
		mBtnEditLocation.setVisibility(View.GONE);
	}

	public void onClickKnownLocation(View v) {
		if (isLoadKnownGateway) {
			startMapActivity(mLatLngKnownGateway.latitude,mLatLngKnownGateway.longitude);
		}else{
			Toast.makeText(this, "No known gateway", Toast.LENGTH_SHORT).show();
		}
	}

	public void onClickSetGateway(View v) {
		if ((mTextLatitude.length() < 3) || (mTextLongitude.length() < 3)) {
			Toast.makeText(this, "Check geo data", Toast.LENGTH_SHORT).show();
		} else {
			startMapActivity(Double.valueOf(mTextLatitude.getText().toString()),
					Double.valueOf(mTextLongitude.getText().toString()));
		}
	}

	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API).build();
	}

	private void initDevice() {
		MyThingPlugDevices myThingPlugDevices = MyThingPlugDevices.getInstance();
		MyDevices THIS_DEVICE = MyDevices.GATEWAY;
//		MyDevices THIS_DEVICE = MyDevices.MESSAGE;

		mGatewayDevice = new ThingPlugDevice(
				myThingPlugDevices.getServiceName(THIS_DEVICE),
				myThingPlugDevices.getSclId(THIS_DEVICE),
				myThingPlugDevices.getDeviceId(THIS_DEVICE),
				myThingPlugDevices.getAuthId(THIS_DEVICE),
				myThingPlugDevices.getAuthKey(THIS_DEVICE))
				.setTag("RoLa Gateway")
				.registerDevice(true);

		THING_AUTHORIZATION = mGatewayDevice.getAuthorization();
		THING_REQ_URI = mGatewayDevice.getUrlContenInstancesDetailed(0, 1).toString();

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

	private boolean checkDeviceLocation(JsonContentInstanceDetail pJsonContentInstanceDetail) {
		boolean result = false;
		double latitude = 0, longitude = 0;

		String[] geoString = pJsonContentInstanceDetail.getContent().split(",");
		Date creationDate = pJsonContentInstanceDetail.getCreationTime();

		if ((geoString.length == 3) && (creationDate != null)) {
			result = true;
			String doublePtnString = "^[\\+\\-]{0,1}[0-9]+[\\.\\,]{1}[0-9]+$";

			try {
				latitude = Double
						.valueOf(geoString[1].replace(doublePtnString, ""));
				longitude = Double
						.valueOf(geoString[2].replace(doublePtnString, ""));

				mLatLngKnownGateway = new LatLng(latitude, longitude);
				if (mGatewayDevice != null) {
					mGatewayDevice.setTag(geoString[0]);
				}

			} catch (Exception e) {
				result = false;
				e.printStackTrace();
			}
		}

		return result;
	}

	private void updateDeviceLocation(JSONObject pJsonObject) {
		JsonResponseContentInstanceDetailedLastOne response = toJsonResponse(pJsonObject);
		if(response.getCurrentNrOfInstances()==0){
			this.mBtnKnownGateway.setText("Gateway Location is not found");
			isLoadKnownGateway = false;
		}else{
			if (checkDeviceLocation(response.getContentInstanceDetail()) == true) {
				this.mBtnKnownGateway.setText("Gateway @" + mGatewayDevice.getTag());
				this.mBtnKnownGateway.setTextColor(0xffffffff);
				isLoadKnownGateway = true;
			}
		}
		
	}

	private synchronized void getThingPlugDeviceContentOfGateWay() {
		if (mGatewayDevice == null) {
			initDevice();
		}

		Volley.newRequestQueue(this).add(
				new StringRequest(Request.Method.GET, THING_REQ_URI, new Response.Listener<String>() {

					@Override
					public void onResponse(String response) {
						try {
							JSONObject jsonObject = XML.toJSONObject(response);
							updateDeviceLocation(jsonObject);
						} catch (JSONException e) {
							e.printStackTrace();
							Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
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

	/**
	 * Method to verify google play services on the device
	 * */
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
		stopService(new Intent(this, TestService.class));
		super.onResume();

		checkPlayServices();
		if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
			startLocationUpdates();
		}
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
		// TODO Auto-generated method stub
		super.onPause();
		stopLocationUpdates();
		launchTestService();
	}
	
	public void launchTestService() {
		Intent i = new Intent(this, TestService.class);

		startService(i);
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
				+ result.getErrorCode());

	}

	@Override
	public void onConnected(Bundle connectionHint) {
		displayLocation();
		if (mRequestingLocationUpdates) {
			startLocationUpdates();
		}
	}

	@Override
	public void onConnectionSuspended(int cause) {
		mGoogleApiClient.connect();
	}

	@Override
	public void onLocationChanged(Location location) {
		// Assign the new location
		mLastLocation = location;
		Toast.makeText(getApplicationContext(), "Location changed!",
				Toast.LENGTH_SHORT).show();
		displayLocation();

	}
}
