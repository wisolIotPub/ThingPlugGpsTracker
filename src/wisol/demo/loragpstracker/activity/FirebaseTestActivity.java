package wisol.demo.loragpstracker.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import wisol.demo.loragpstracker.R;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class FirebaseTestActivity extends Activity {

	final String mFirebaseUri = "https://shining-fire-4201.firebaseio.com";
	TextView debugTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_firebase_test);
		debugTextView = (TextView) findViewById(R.id.tv_firebase);

		// testJson();
		// if (isExternalStorageAvailable()) {
		// makeDummyFiles(10);
		// }
		getFirebaseTest();
	}

	private boolean isExternalStorageAvailable() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	private synchronized void getFirebaseTest() {

		Volley.newRequestQueue(this).add(
				new StringRequest(Request.Method.POST, mFirebaseUri + "/user.json",
						new Response.Listener<String>() {

							@Override
							public void onResponse(String response) {
								try {
									JSONObject jsonObject = XML.toJSONObject(response);
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
					public byte[] getBody() throws AuthFailureError {
						// TODO Auto-generated method stub
						// String serverValue = ",\".sv\":\"timestamp\"";
//						debugTextView.setText((testJson()));
						return testJson().getBytes();
						// return "{\".sv\":\"timestamp\"}".getBytes();
					}

					// @Override
					// public Map<String, String> getHeaders() throws
					// AuthFailureError {
					// Map<String, String> headers = new HashMap<String,
					// String>();
					// // headers.put("Content-Type", "application/xml");
					// headers.put("Authorization", THING_AUTHORIZATION);
					// headers.put("charset", "UTF-8");
					//
					// return headers;
					// }
				});

	}

	private void saveTempFile() {
		String dirName = "volley/";
		String filename = "testFile.json";
		String toStrings = testJson();
		FileOutputStream outputStream;

		File file = new File(Environment.getExternalStorageDirectory()
				, dirName);
		Log.v("gsonTest", file.getPath());
		if (file.mkdirs()) {
			Log.v("gsonTest", file.getPath());
			try {
				FileWriter fw = new FileWriter(new File(file.getPath(), filename));
				fw.write(toStrings);
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private void makeDummyFiles(int pFileCount) {
		File pDir = makeDir("jsonTestFolder");
		try {

			for (int i = 0; i < pFileCount; i++) {
				FileWriter fw = new FileWriter(new File(pDir, "t" + String.valueOf(i) + ".txt"));

				for (int j = 0; j < (1500); j++) {
					fw.write(String.valueOf(j) + " : file write test\r\n");
				}
				fw.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private File makeDir(String path) {
		File pFile = null;

		pFile = new File(Environment.getExternalStorageDirectory(), path);
		if (pFile.exists()) {
			// System.out.println("folder is exist");
			Log.v("gsonTest", "folder is exist");
		} else {
			pFile.mkdirs();
		}

		return pFile;
	}
	
	int test1=0;

	private String testJson() {
		String result;
		User newUser = new User("Heesang"+String.valueOf(test1++), 1977);
		Gson gson = new Gson();

		result = gson.toJson(newUser).toString();
		Log.v("gsonTest", gson.toJson(newUser).toString());

		return result;
	}

	public class User {
		private int birthYear;
		private String fullName;
		// @SerializedName(".sv")
//		private TimeStamp timeStamp;

		public User() {

		}

		public User(String pFullName, int pBirthYear) {
			this.fullName = pFullName;
			this.birthYear = pBirthYear;
//			this.timeStamp = new TimeStamp();
		}

		public long getBirthYear() {
			return birthYear;
		}

		public String getFullName() {
			return this.fullName;
		}

		public class TimeStamp {
			@SerializedName(".sv")
			private String timeStamp;

			public TimeStamp() {
				this.timeStamp = "timestamp";
			}
		}

	}

}
