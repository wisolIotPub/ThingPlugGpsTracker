package wisol.demo.loragpstracker;

import com.google.gson.annotations.SerializedName;

public class JsonDataThingPlugLogin {
	@SerializedName("ThingPlug")
	private JsonThingPlugLogin thingPlug;

	public JsonThingPlugLogin getThingPlug() {
		return this.thingPlug;
	}

	public int getResultCode() {
		return getThingPlug().getResultCode();
	}

	public String getResultMsg() {
		return getThingPlug().getResultMsg();
	}

	public boolean isAdmin() {
		return getThingPlug().isAdmin();
	}

	public String getPassword() {
		return getThingPlug().getPassword();
	}

	public String getUKey() {
		return getThingPlug().getUKey();
	}

	public String getUserId() {
		return getThingPlug().getUserId();
	}
}
