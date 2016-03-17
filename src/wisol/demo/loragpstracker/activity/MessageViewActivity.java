package wisol.demo.loragpstracker.activity;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import wisol.demo.loragpstracker.JsonContentInstanceDetail;
import wisol.demo.loragpstracker.JsonResponseContentInstancesDetailed;
import wisol.demo.loragpstracker.MyThingPlugDevices;
import wisol.demo.loragpstracker.MyThingPlugDevices.MyDevices;
import wisol.demo.loragpstracker.R;
import wisol.demo.loragpstracker.TestService;
import wisol.demo.loragpstracker.ThingPlugDevice;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

public class MessageViewActivity extends Activity {

	final String EXTRA_THIS_DEVICE = "THIS_DEVICE";
	TextView mTvLastMsg;
	TextView mTvLastDate;
	ListView mLvMsgList;

	private ThingPlugDevice mThingPlugDevice;
	MyThingPlugDevices myThingPlugDevices;

	Handler mMainHandler;

	boolean testBool = false;
	boolean isFirstReadFromServer = true;
	private MsgListViewAdapter mMsgListViewAdapter;
	private ArrayList<JsonContentInstanceDetail> mArrayListDetailes;
	private int thingPlugDataPageNum = 0;
	MyDevices THIS_DEVICE;
	static boolean isActivated = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_message_view);
		initUiComponents();

		myThingPlugDevices = MyThingPlugDevices.getInstance();
		// THIS_DEVICE = MyDevices.MESSAGE;
		// THIS_DEVICE = MyDevices.MAP;
		getExtra(savedInstanceState);
		initThingPlugRequest();
	}

	private void getExtra(Bundle pBundle) {
		Log.d("", getIntent().getStringExtra(EXTRA_THIS_DEVICE));
		THIS_DEVICE = MyDevices.valueOf(getIntent().getStringExtra(EXTRA_THIS_DEVICE));

	}

	@Override
	protected void onResume() {
		stopService(new Intent(this, TestService.class));
		super.onResume();
		Log.d("ThingPlugReq", "Activity is started or resumed~~ ");
		isActivated = true;
		mMainHandler.sendEmptyMessageDelayed(getThingPlugPageNum(), 20);

		mThingPlugDevice = new ThingPlugDevice(myThingPlugDevices.getServiceName(THIS_DEVICE),
				myThingPlugDevices.getSclId(THIS_DEVICE),
				myThingPlugDevices.getDeviceId(THIS_DEVICE),
				myThingPlugDevices.getAuthId(THIS_DEVICE),
				myThingPlugDevices.getAuthKey(THIS_DEVICE));

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		launchTestService();
		isActivated = false;
		super.onPause();
	}

	public void launchTestService() {
		Intent i = new Intent(this, TestService.class);

		startService(i);
	}

	private void initThingPlugRequest() {
		if (mMainHandler == null) {
			mMainHandler = new WeakHandler(this) {
				@Override
				public void handleMessage(Message msg) {
					super.handleMessage(msg);
					thingPlugRequest(mThingPlugDevice,
							mThingPlugDevice.getUrlContenInstancesDetailed(msg.what, 10)
									.toString(),
							Request.Method.GET);
				}
			};
		}
	}

	static public class WeakHandler extends Handler {
		private final WeakReference<Activity> mHandlerObj;

		public WeakHandler(Activity pHandlerObj) {
			mHandlerObj = new WeakReference<Activity>(pHandlerObj);
		}
	}

	private void initUiComponents() {
		mTvLastMsg = (TextView) findViewById(R.id.msg_last_msg);
		mTvLastDate = (TextView) findViewById(R.id.msg_last_date);
		mLvMsgList = (ListView) findViewById(R.id.msg_list);
	}

	private int getThingPlugPageNum() {
		return this.thingPlugDataPageNum;
	}

	private int setThingPlugPageNum(int pPageNum) {
		this.thingPlugDataPageNum = pPageNum;
		return this.thingPlugDataPageNum;
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

	private void updateMessageList(JSONObject pJsonObject) {
		JsonResponseContentInstancesDetailed response = toJsonResponseContentInstancesDetailed(pJsonObject);
		boolean todoCheckNext = true;

		if (response.getCurrentNrOfInstances() == 0) {
			if (mArrayListDetailes == null) {
				mTvLastMsg.setText("No data today");
				mTvLastDate.setText("----.--.-- --:--");
			}
			mMainHandler.sendEmptyMessageDelayed(setThingPlugPageNum(0), 10000);

		} else {
			if (this.mArrayListDetailes == null) {
				mArrayListDetailes = new ArrayList<JsonContentInstanceDetail>(
						response.getContentInstanceDetails());
			} else {
				// mArrayListDetailes.addAll(response.getContentInstanceDetails());
				todoCheckNext = compareUpdateDataList(response.getContentInstanceDetails());
			}
			upDateLastMessage(mArrayListDetailes.get(0));
			upDateDataList();

			Log.d("todoCheck", String.valueOf(todoCheckNext));

			mMainHandler.sendEmptyMessageDelayed(setThingPlugPageNum(0), 5000);
		}
	}

	private void upDateLastMessage(JsonContentInstanceDetail pLastDetail) {
		mTvLastMsg.setText(pLastDetail.getContent());
		mTvLastDate.setText(pLastDetail.getCreationTime().toString());
	}

	private boolean compareUpdateDataList(ArrayList<JsonContentInstanceDetail> pNewData) {
		boolean result = true;
		final int pDataSize = pNewData.size();

		for (int i = 0; i < pDataSize; i++) {
			if (pNewData.get(pDataSize - (1 + i)).getCreationTime()
					.after(this.mArrayListDetailes.get(0).getCreationTime())) {
				mArrayListDetailes.add(0, pNewData.get(pDataSize - (i + 1)));

				if (mArrayListDetailes.size() > 30) {
					mArrayListDetailes.remove(mArrayListDetailes.size() - 1);
				}
				Toast.makeText(this, "newData", Toast.LENGTH_SHORT).show();
			} else {
				result = false;
			}
		}

		return result;
	}

	private void upDateDataList() {
		if (mMsgListViewAdapter == null) {
			mMsgListViewAdapter = new MsgListViewAdapter(this, R.layout.msg_listview_item,
					this.mArrayListDetailes);
			mLvMsgList.setAdapter(mMsgListViewAdapter);
		} else {
			mMsgListViewAdapter.notifyDataSetChanged();
		}

	}

	private void testUpdateMsgList(JsonResponseContentInstancesDetailed pResponse) {
		mMsgListViewAdapter = new MsgListViewAdapter(this, R.layout.msg_listview_item,
				pResponse.getContentInstanceDetails());
		mLvMsgList.setAdapter(mMsgListViewAdapter);
	}

	private void thingPlugRequest(ThingPlugDevice pThingPlugDevice, String reqUrl,
			int pRequestMethod) {
		Log.v(getClass().getName(), reqUrl);
		final String authorization = pThingPlugDevice.getAuthorization();
		final int reqMethod = pRequestMethod;

		if (isActivated == false) {
			Log.d("ThingPlugReq", "Activity is paused~~ ");
			return;
		}

		Volley.newRequestQueue(this).add(new StringRequest(reqMethod, reqUrl, new Response.Listener<String>() {

			@Override
			public void onResponse(String response) {
				try {
					JSONObject jsonObject = XML.toJSONObject(response);
					updateMessageList(jsonObject);

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
		});

	}

	private class MsgListViewAdapter extends ArrayAdapter<JsonContentInstanceDetail> {

		public MsgListViewAdapter(Context context, int resource, ArrayList<JsonContentInstanceDetail> objects) {
			super(context, resource, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			JsonContentInstanceDetail item = getItem(position);
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
				viewHolder.msg.setText(item.getContent());
				viewHolder.date.setText(item.getCreationTime().toString());
			}

			return v;
		}

	}

	public static class ViewHolder {
		TextView msg;
		TextView date;
	}
}
