package wisol.demo.loragpstracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MyThingPlugDevices {
	private static MyThingPlugDevices instance;
	private final String serviceNameKey = "serviceName";
	private final String sclIdKey = "sclId";
	private final String deviceIdKey = "deviceId";
	private final String authIdKey = "authId";
	private final String authKeyKey = "authKey";

	Map<String, String> deviceInfoGateway = new HashMap<String, String>() {
		{
			put(serviceNameKey, "ThingPlug");
			put(sclIdKey, "SC10010998");
			put(deviceIdKey, "AD10015209");
			put(authIdKey, "AP10005666");
			put(authKeyKey, "AK10000176");
		}
	};

	Map<String, String> deviceInfoMsg = new HashMap<String, String>() {
		{
			put(serviceNameKey, "ThingPlug");
			put(sclIdKey, "SC10009801");
			put(deviceIdKey, "AD10014958");
			put(authIdKey, "AP10005666");
			put(authKeyKey, "AK10000176");
		}
	};

	Map<String, String> deviceInfoDoor1 = new HashMap<String, String>() {// 종속디바이스
																			// 확인해야함.
		{
			put(serviceNameKey, "ThingPlug");
			put(sclIdKey, "SC10010251");
			put(deviceIdKey, "AD10014955");
			put(authIdKey, "AP10005666");
			put(authKeyKey, "AK10000176");
		}
	};

	Map<String, String> deviceInfoDoor2 = new HashMap<String, String>() {// 종속디바이스
		// 확인해야함.
		{
			put(serviceNameKey, "ThingPlug");
			put(sclIdKey, "SC10010146");
			put(deviceIdKey, "AD10014957");
			put(authIdKey, "AP10005666");
			put(authKeyKey, "AK10000176");
		}
	};

	Map<String, String> deviceInfoDoor3 = new HashMap<String, String>() {// 종속디바이스
		// 확인해야함.
		{
			put(serviceNameKey, "ThingPlug");
			put(sclIdKey, "SC10010414");
			put(deviceIdKey, "AD10014996");
			put(authIdKey, "AP10005666");
			put(authKeyKey, "AK10000176");
		}
	};

	Map<String, String> deviceInfoMap = new HashMap<String, String>() {
		{
			put(serviceNameKey, "ThingPlug");
			put(sclIdKey, "SC10010147");
			put(deviceIdKey, "AD10014956");
			put(authIdKey, "AP10005666");
			put(authKeyKey, "AK10000176");
		}
	};

	public static synchronized MyThingPlugDevices getInstance() {
		if (instance == null) {
			instance = new MyThingPlugDevices();
		}
		return instance;
	}

	public String getServiceName(MyDevices pDevice) {
		return getDeviceInfo(pDevice).get(this.serviceNameKey);
	}

	public String getSclId(MyDevices pDevice) {
		return getDeviceInfo(pDevice).get(this.sclIdKey);
	}

	public String getDeviceId(MyDevices pDevice) {
		return getDeviceInfo(pDevice).get(this.deviceIdKey);
	}

	public String getAuthId(MyDevices pDevice) {
		return getDeviceInfo(pDevice).get(this.authIdKey);
	}

	public String getAuthKey(MyDevices pDevice) {
		return getDeviceInfo(pDevice).get(this.authKeyKey);
	}

	public Map<String, String> getDeviceInfo(MyDevices pDevice) {
		Map<String, String> result = new HashMap<String, String>();
		switch (pDevice) {
		case DEFAULT:
			result.putAll(this.deviceInfoMsg);
			break;
		case GATEWAY:
			result.putAll(this.deviceInfoGateway);
			break;
		case MESSAGE:
			result.putAll(this.deviceInfoMsg);
			break;	
		case DOOR1:
			result.putAll(this.deviceInfoDoor1);
			break;
		case DOOR2:
			result.putAll(this.deviceInfoDoor2);
			break;
		case DOOR3:
			result.putAll(this.deviceInfoDoor3);
			break;
		case MAP:
			result.putAll(this.deviceInfoMap);
			break;
		default:
			result.putAll(this.deviceInfoMsg);
			break;
		}
		return result;
	}

	public ArrayList<MyDevices> getRegisteredMyDeviceList() {
		ArrayList<MyDevices> result = new ArrayList<MyThingPlugDevices.MyDevices>(Arrays.asList(MyDevices
				.values()));
		if (result.contains(MyDevices.DEFAULT)) {
			result.remove(MyDevices.DEFAULT);
		}

		return result;
	}

	public enum MyDevices {
		DEFAULT, MESSAGE, DOOR1, DOOR2, DOOR3, MAP, GATEWAY;

	}

}
