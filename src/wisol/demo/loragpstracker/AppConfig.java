package wisol.demo.loragpstracker;

import java.util.HashMap;
import java.util.Map;

public class AppConfig {
	private static AppConfig mInstance;

	private final String mHost = "thingplug.sktiot.com";
	private final int mHostPort = 9000;
	private final String PUT_LOGIN_PATH = "?division=user&function=login";
	private String mPath = "ThingPlug";

	private final String DEF_USERID = "wisolHsLee";
	private final String DEF_PASSWORD = "wisol2016!";
//	private final String DEF_USERID = "AP10005666";
//	private final String DEF_PASSWORD = "AK10000176";

	private String userId = DEF_USERID;
	private String userPassword = DEF_PASSWORD;

	private JsonDataThingPlugLogin mJsonDataThingPlugLogin;
	
	public final Map<String, String> LOGIN_SEGMENT = new HashMap<String, String>(){
		{
			put("division", "user");
			put("function","login");
		}
	};
	
	public final Map<String, String> SEARCH_MY_DEVICE_SEGMENT = new HashMap<String, String>(){
		{
			put("division", "searchDevice");
			put("function","myDevice");
			put("startIndex","1");
			put("countPerPage","5");
		}
	};

	public static synchronized AppConfig getInstance() {
		if (mInstance == null) {
			mInstance = new AppConfig();
		}
		return mInstance;
	}

	public JsonDataThingPlugLogin getLoginResponse() {
		return mJsonDataThingPlugLogin;
	}

	public void setLoginResponse(JsonDataThingPlugLogin pDataThingPlugLogin) {
		this.mJsonDataThingPlugLogin = pDataThingPlugLogin;
	}

	public String getHost() {
		return this.mHost;
	}

	public int getHostPort() {
		return this.mHostPort;
	}


	public String getPath() {
		return mPath;
	}

	public String getPutLoginPath() {
		return this.PUT_LOGIN_PATH;
	}

	public String getDefaultUserId() {
		return this.DEF_USERID;
	}

	public String getDefaultPassword() {
		return this.DEF_PASSWORD;
	}

	public void setUserId(String pUserId) {
		this.userId = pUserId;
	}

	public void setUserPassword(String pUserPassword) {
		this.userPassword = pUserPassword;
	}
	

}
