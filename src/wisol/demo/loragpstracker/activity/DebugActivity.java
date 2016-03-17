package wisol.demo.loragpstracker.activity;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import wisol.demo.loragpstracker.JsonContentInstanceDetail;
import wisol.demo.loragpstracker.JsonResponseContentInstancesDetailed;
import wisol.demo.loragpstracker.R;
import wisol.demo.loragpstracker.ThingPlugDevice;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class DebugActivity extends Activity {

	TextView mTextView;
	private ThingPlugDevice mThingPlugDevice;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debug);
		initUIcomponents();
		mThingPlugDevice = new ThingPlugDevice();

		thingPlugRequest(mThingPlugDevice, mThingPlugDevice.getUrlContenInstancesDetailed(0, 10).toString(),
				Request.Method.GET);
	}

	public void onClickDebugText(View v) {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "thingPlug Debug msg");
		sendIntent.putExtra(Intent.EXTRA_TEXT, this.mTextView.getText());
		sendIntent.setType("text/plain");
		this.startActivity(Intent.createChooser(sendIntent, "Sharing"));
	}

	private void initUIcomponents() {
		mTextView = (TextView) findViewById(R.id.debug_text);
	}
	
	private void testGsonObject(JSONObject pJsonObject){
		Type type = new TypeToken<JsonResponseContentInstancesDetailed>() {
		}.getType();

		JsonResponseContentInstancesDetailed response = new GsonBuilder().create().fromJson(pJsonObject.toString(), type);

		mTextView.clearComposingText();
		for(JsonContentInstanceDetail detail:response.getContentInstanceDetails()){
			mTextView.append(
					"\n"+
					detail.getId()+"," +
					detail.getCreationTime().toString()+"," +
					detail.getLastModifiedTime().toString()+"," +
					detail.getContent()+"," +
					detail.getCountIndex()+"," +
					detail.getTatalCount()+"," +
					detail.getCurrentCount()+"\n");
		}
	}

	private void thingPlugRequest(ThingPlugDevice pThingPlugDevice, String reqUrl, int pRequestMethod) {
		Log.v(getClass().getName(), reqUrl);
		final String authorization = pThingPlugDevice.getAuthorization();
		final int reqMethod = pRequestMethod;

		Volley.newRequestQueue(this).add(new StringRequest(reqMethod, reqUrl, new Response.Listener<String>() {

			@Override
			public void onResponse(String response) {
				try {
					JSONObject jsonObject = XML.toJSONObject(response);
					mTextView.setText(jsonObject.toString(3));
					testGsonObject(jsonObject);

				} catch (JSONException e) {
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
				}
			}

		}, new Response.ErrorListener() {

			@Override
			public void onErrorResponse(VolleyError error) {
				Toast.makeText(getApplicationContext(), "Error occured", Toast.LENGTH_SHORT).show();
			}
		}) {

			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Content-Type", "application/xml");
				headers.put("Authorization", authorization);
				headers.put("charset", "UTF-8");

				return headers;
			}

			@Override
			public byte[] getBody() throws AuthFailureError {
				// TODO Auto-generated method stub
				return super.getBody();
			}

		});

	}
}
