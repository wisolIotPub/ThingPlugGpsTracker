package wisol.demo.loragpstracker.activity;

import java.io.File;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import wisol.demo.loragpstracker.AppConfig;
import wisol.demo.loragpstracker.JsonDataThingPlugLogin;
import wisol.demo.loragpstracker.R;
import wisol.demo.loragpstracker.TestService;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);//It must be located prior to the setContentView
		setContentView(R.layout.layout_activity_main);
	}

	@Override
	protected void onPause() {
		launchTestService();

		super.onPause();
	}

	public void launchTestService() {
		Intent i = new Intent(this, TestService.class);

		startService(i);
	}


	@Override
	protected void onResume() {
		stopService(new Intent(this, TestService.class));
		super.onResume();
	}











}
