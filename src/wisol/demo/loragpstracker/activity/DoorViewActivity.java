package wisol.demo.loragpstracker.activity;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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

public class DoorViewActivity extends Activity {
	TextView mTvDoorInfo;
	TextView mTvDoorInfoDate;
	ListView mLvDoorList;
	ArrayList<ThingPlugDevice> mDoorDevices;
	ArrayList<JsonContentInstanceDetail> mDoorContentList;
	private DoorListViewAdapter mDoorListViewAdapter;

	Calendar mCalendar;

	Handler mHandler;
	static boolean isActivated = false;
	final String EXTRA_THIS_DEVICE = "THIS_DEVICE";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_door_view);

		mCalendar = Calendar.getInstance();

		initUiComponents();
		initDoorListArray();

		mHandler = new WeakHandler(this) {
			@Override
			public void handleMessage(Message msg) {
				getThingPlugDeviceContent(msg.what);
			}
		};

	}

	static public class WeakHandler extends Handler {
		private final WeakReference<Activity> mHandlerObj;

		public WeakHandler(Activity pHandlerObj) {
			mHandlerObj = new WeakReference<Activity>(pHandlerObj);
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		stopService(new Intent(this, TestService.class));
		super.onResume();
		isActivated = true;
		sendInitEmptyMsg();
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

	private void sendInitEmptyMsg() {
		int i = 1;
		for (ThingPlugDevice pDevice : mDoorDevices) {
			int what = mDoorDevices.indexOf(pDevice);
			i++;
			if (pDevice.isRegistered()) {
				mHandler.sendEmptyMessageDelayed(what, 5 * i);
			}
		}
	}

	private JsonResponseContentInstanceDetailedLastOne toJsonResponse(int pDeviceNum,
			JSONObject pJsonObject) {
		try {
			Log.d("json", String.valueOf(pDeviceNum) + "\n" + pJsonObject.toString(3));
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

	synchronized private void updateMessageList(int pDeviceNum, JSONObject pJsonObject) {
		JsonResponseContentInstanceDetailedLastOne response = toJsonResponse(pDeviceNum,
				pJsonObject);
		long delayTime = 3500;

		mCalendar.setTime(new Date());
		mTvDoorInfoDate.setText("@" + mCalendar.getTime().toString());

		if (response.getCurrentNrOfInstances() == 0) {
			delayTime = 10000;// + pDeviceNum * 50;
		} else {

			if (mDoorContentList.get(pDeviceNum).getCreationTime() != null) {
				if (mDoorContentList.get(pDeviceNum).getCreationTime()
						.compareTo((response.getContentInstanceDetail().getCreationTime())) < 0) {
					Log.d("compareTo", mDoorContentList.get(pDeviceNum).getCreationTime().toString() + "\n"
							+ response.getContentInstanceDetail().getCreationTime().toString());
					delayTime = 4000;// + pDeviceNum * 50;
				} else {
					delayTime = 8000;// + pDeviceNum * 50;
				}
			} else {
				delayTime = 8000;// + pDeviceNum * 50;
			}

			mDoorContentList.set(
					pDeviceNum,
					response.getContentInstanceDetail()
							.setName(mDoorContentList.get(pDeviceNum).getName())
							.register(mDoorContentList.get(pDeviceNum).isRegistered()));
			updateDataList();
		}
		mHandler.sendEmptyMessageDelayed(pDeviceNum, delayTime);

	}

	private synchronized void getThingPlugDeviceContent(int pDeviceNum) {
		final String authorization = mDoorDevices.get(pDeviceNum).getAuthorization();
		final String reqUrl = mDoorDevices.get(pDeviceNum).getUrlContenInstancesDetailed(0, 1).toString();
		final int deviceNum = pDeviceNum;

		if (isActivated == false) {
			return;
		}
		Volley.newRequestQueue(this).add(
				new StringRequest(Request.Method.GET, reqUrl, new Response.Listener<String>() {

					@Override
					public void onResponse(String response) {
						try {
							JSONObject jsonObject = XML.toJSONObject(response);
							updateMessageList(deviceNum, jsonObject);

						} catch (JSONException e) {
							e.printStackTrace();
							Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
						}
					}

				}, new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						Toast.makeText(getApplicationContext(), String.valueOf(deviceNum) + ":Error occured",
								Toast.LENGTH_SHORT).show();
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

	private void initDoorListArray() {
		final int UNREG_DEVICE_NUM = 10;
		final String unRegDeviceName = "Unregistered ";
		int regDeviceNum = 0;

		mDoorDevices = new ArrayList<ThingPlugDevice>();
		mDoorContentList = new ArrayList<JsonContentInstanceDetail>();

		MyThingPlugDevices myThingPlugDevices = MyThingPlugDevices.getInstance();

		mDoorDevices.add(new ThingPlugDevice(
				myThingPlugDevices.getServiceName(MyDevices.DOOR1),
				myThingPlugDevices.getSclId(MyDevices.DOOR1),
				myThingPlugDevices.getDeviceId(MyDevices.DOOR1),
				myThingPlugDevices.getAuthId(MyDevices.DOOR1),
				myThingPlugDevices.getAuthKey(MyDevices.DOOR1)).setTag("LoRa door 01").registerDevice(true));

		mDoorDevices.add(new ThingPlugDevice(
				myThingPlugDevices.getServiceName(MyDevices.DOOR2),
				myThingPlugDevices.getSclId(MyDevices.DOOR2),
				myThingPlugDevices.getDeviceId(MyDevices.DOOR2),
				myThingPlugDevices.getAuthId(MyDevices.DOOR2),
				myThingPlugDevices.getAuthKey(MyDevices.DOOR2)).setTag("LoRa door 02").registerDevice(true));

		mDoorDevices.add(new ThingPlugDevice(
				myThingPlugDevices.getServiceName(MyDevices.DOOR3),
				myThingPlugDevices.getSclId(MyDevices.DOOR3),
				myThingPlugDevices.getDeviceId(MyDevices.DOOR3),
				myThingPlugDevices.getAuthId(MyDevices.DOOR3),
				myThingPlugDevices.getAuthKey(MyDevices.DOOR3)).setTag("LoRa door 03").registerDevice(true));

		// for (int i = 0; i < UNREG_DEVICE_NUM; i++) {
		// mDoorDevices.add(new ThingPlugDevice()
		// .setTag(unRegDeviceName + String.valueOf(i))
		// .registerDevice(false));
		// }

		for (ThingPlugDevice pDevice : mDoorDevices) {
			mDoorContentList.add(new JsonContentInstanceDetail()
					.setName(pDevice.getTag())
					.register(pDevice.isRegistered()));
			if (pDevice.isRegistered()) {
				regDeviceNum += 1;
				mTvDoorInfo.setText(String.valueOf(regDeviceNum) + " places are registered@Server");
			}
		}

		updateDataList();

	}

	private void initUiComponents() {
		mTvDoorInfo = (TextView) findViewById(R.id.door_info);
		mTvDoorInfoDate = (TextView) findViewById(R.id.door_info_date);
		mLvDoorList = (ListView) findViewById(R.id.door_list);
		mLvDoorList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				startMessageActivity(position);
			}

		});
	}

	private void startMessageActivity(int position) {
		Intent intent = new Intent(this, MessageViewActivity.class);
		intent.putExtra(EXTRA_THIS_DEVICE, "DOOR" + String.valueOf(position + 1));
		startActivity(intent);
	}

	private void updateDataList() {
		if (mDoorListViewAdapter == null) {
			mDoorListViewAdapter = new DoorListViewAdapter(this, R.layout.door_listview_item,
					this.mDoorContentList);
			mLvDoorList.setAdapter(mDoorListViewAdapter);
		} else {
			mDoorListViewAdapter.notifyDataSetChanged();
		}
	}

	private class DoorListViewAdapter extends ArrayAdapter<JsonContentInstanceDetail> {

		public DoorListViewAdapter(Context context, int resource, ArrayList<JsonContentInstanceDetail> objects) {
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
				v = vi.inflate(R.layout.door_listview_item, parent, false);

				viewHolder.img = (ImageView) v.findViewById(R.id.doorItemStateImg);
				viewHolder.title = (TextView) v.findViewById(R.id.doorItemTitle);
				viewHolder.msg = (TextView) v.findViewById(R.id.doorItemStateText);
				viewHolder.date = (TextView) v.findViewById(R.id.doorItemDateText);

				v.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) v.getTag();

			}

			if (item.isRegistered()) {
				if (item != null) {
					// viewHolder.img
					switch (isDoorOpenClose(item.getContent())) {
					case OPEN:
						viewHolder.img.setImageResource(R.drawable.door_open);
						break;
					case CLOSE:
						viewHolder.img.setImageResource(R.drawable.door_close);
						break;
					default:
						viewHolder.img.setImageResource(R.drawable.door_nodata);
						break;

					}
					viewHolder.title.setText(item.getName());

					if (item.getContent().length() < 1) {
						viewHolder.msg.setText("No data");
					} else {
						viewHolder.msg.setText(item.getContent());
					}
					if (item.getCreationTime() == null) {
						mCalendar.setTime(new Date());
						viewHolder.date.setText(mCalendar.getTime().toString());
					} else {
						viewHolder.date.setText(item.getCreationTime().toString());
					}

				}
			} else {
				viewHolder.img.setImageResource(R.drawable.door_unregistered);
				viewHolder.title.setText(item.getName());
				viewHolder.msg.setText("-----");
				viewHolder.date.setText("-----");
			}

			return v;
		}
	}

	public DoorState isDoorOpenClose(String pInput) {// open:false, close:true;
		DoorState result = DoorState.UNKOWN;
		final String DOOR_OPEN = "open";
		final String DOOR_CLOSE = "close";

		final String pInputLowerCase = pInput.toLowerCase();

		final String ptnStrinOpen = "(.*)" + DOOR_OPEN + "(.*)";
		final String ptnStrinClose = "(.*)" + DOOR_CLOSE + "(.*)";

		Pattern ptnOpen = Pattern.compile(ptnStrinOpen);
		Pattern ptnClose = Pattern.compile(ptnStrinClose);

		Matcher mOpen = ptnOpen.matcher(pInputLowerCase);
		Matcher mClose = ptnClose.matcher(pInputLowerCase);

		if (mOpen.find()) {
			result = DoorState.OPEN;
		} else if (mClose.find()) {
			result = DoorState.CLOSE;
		} else {
			result = DoorState.UNKOWN;
		}

		return result;
	}

	private enum DoorState {
		OPEN, CLOSE, UNKOWN
	}

	public static class ViewHolder {
		ImageView img;
		TextView title;
		TextView msg;
		TextView date;
	}
}
