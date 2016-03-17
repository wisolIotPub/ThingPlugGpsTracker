package wisol.demo.loragpstracker;

import com.google.gson.annotations.SerializedName;

public class JsonDataThingPlug {
	@SerializedName("ThingPlug")
	private JsonThingPlug thingPlug;

	public JsonThingPlug getThingPlug() {
		return this.thingPlug;
	}

	public int getResultCode() {
		return getThingPlug().getResultCode();
	}

	public String getResultMsg() {
		return getThingPlug().getResultMsg();
	}
}
