package wisol.demo.loragpstracker;

import com.google.gson.annotations.SerializedName;

public class JsonThingPlug {
	@SerializedName("result_code")
	private String resultCode;
	@SerializedName("result_msg")
	private String resultMsg;

	public int getResultCode() {
		return Integer.valueOf(this.resultCode);
	}

	public String getResultMsg() {
		return this.resultMsg;
	}
}