package wisol.demo.loragpstracker;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.android.volley.Request;

import android.util.Base64;
import android.util.Log;

public class ThingPlugDevice {
	private final String thingProtocol = "http";
	private String mHost = "61.250.21.211";
	private int mHostPort = 10005;

	private String mServiceName;
	private String mSclId;
	private String mDeviceId;
	private String mAuthId;
	private String mAuthKey;

	private String mTag;
	private boolean isRegistered = false;

	final private String defServiceName = "ThingPlug";
	final private String defSclId = "SC10009801";
	final private String defDeviceId = "AD10014854";
	final private String defAuthId = "AP10005666";
	final private String defAuthKey = "AK10000176";

	public ThingPlugDevice() {
		setServiceName(defServiceName);
		setSclId(defSclId);
		setDeviceId(defDeviceId);
		setAuthId(defAuthId);
		setAuthKey(defAuthKey);
	}

	public ThingPlugDevice(String pServiceName, String pSclId, String pDeviceId, String pAuthId, String pAuthKey) {
		setServiceName(pServiceName);
		setSclId(pSclId);
		setDeviceId(pDeviceId);
		setAuthId(pAuthId);
		setAuthKey(pAuthKey);

	}

	public ThingPlugDevice setTag(String pTag) {
		this.mTag = pTag;
		return this;
	}

	public String getTag() {
		if (this.mTag == null) {
			this.mTag = "";
		}
		return this.mTag;
	}

	public ThingPlugDevice registerDevice(boolean pRegister) {
		this.isRegistered = pRegister;
		return this;
	}

	public boolean isRegistered() {
		return this.isRegistered;
	}

	public String getAuthorization() {
		String str = Base64.encodeToString((this.mAuthId + ":" + this.mAuthKey).getBytes(), Base64.URL_SAFE);
		Log.v(getClass().getName(), str);

		return str;
	}

	public String getServiceName() {
		return mServiceName;
	}

	public void setServiceName(String mServiceName) {
		this.mServiceName = mServiceName;
	}

	public String getSclId() {
		return mSclId;
	}

	public void setSclId(String mSclId) {
		this.mSclId = mSclId;
	}

	public String getDeviceId() {
		return mDeviceId;
	}

	public void setDeviceId(String mDeviceId) {
		this.mDeviceId = mDeviceId;
	}

	public String getAuthId() {
		return mAuthId;
	}

	public void setAuthId(String mAuthId) {
		this.mAuthId = mAuthId;
	}

	public String getAuthKey() {
		return mAuthKey;
	}

	public void setAuthKey(String mAuthKey) {
		this.mAuthKey = mAuthKey;
	}

	public String getHost() {
		return mHost;
	}

	public void setHost(String mHost) {
		this.mHost = mHost;
	}

	public int getHostPort() {
		return mHostPort;
	}

	public void setHostPort(int mHostPort) {
		this.mHostPort = mHostPort;
	}

	public String getProtocol() {
		return this.thingProtocol;
	}

	public URL getUrlAttachedDevices() {
		URL result = null;

		try {

			result = new URL(this.getProtocol(), this.getHost(), this.getHostPort(),
					"/ThingPlug/scls/"
							+ this.getSclId() +
							"/attachedDevices/");

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return result;
	}

	public URL getUrlContenInstances() {
		URL result = null;

		try {

			result = new URL(this.getProtocol(), this.getHost(), this.getHostPort(),
					"/ThingPlug/scls/"
							+ this.getSclId() +
							"/applications/" +
							this.getDeviceId()
							+ "/containers/contCollection/contentInstances/");

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return result;
	}

	public URL getUrlContenInstancesDetailed(int pSequentialNum, int pReadCount) {
		URL result = null;
		Map<String, String> pBodyMap = new HashMap<String, String>();
		pBodyMap.put("num", String.valueOf(pSequentialNum));
		pBodyMap.put("count", String.valueOf(pReadCount));

		try {

			result = new URL(this.getProtocol(), this.getHost(), this.getHostPort(),
					"/ThingPlug/scls/"
							+ this.getSclId() +
							"/applications/" +
							this.getDeviceId()
							+ "/containers/contCollection/contentInstances/detailed?" +
							getXmlBodyString(pBodyMap)
					);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return result;
	}

	public URL getUrlGwDeviceReset(int pReqMethod) {
		URL result = null;
		try {

			if (pReqMethod == Request.Method.PUT) {
				result = new URL(this.getProtocol(), this.getHost(), this.getHostPort(),
						"ThingPlug/scls/" +
								this.getSclId() +
								"/attachedDevices/" +
								this.getDeviceId() +
								"/mgmtObjs/mgmtReset/resetAction"
						);
			} else {
				result = new URL(this.getProtocol(), this.getHost(), this.getHostPort(),
						"ThingPlug/scls/" +
								this.getSclId() +
								"/attachedDevices/" +
								this.getDeviceId() +
								"/mgmtObjs/mgmtReset"
						);
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return result;
	}

	private String getXmlElementString(String pElement, String pValue) {
		String result;
		result = "<" + pElement + ">" + pValue + "</" + pElement + ">";
		return result;
	}

	private String getXmlBodyString(Map<String, String> pBodyMap) {
		String result = "body=";
		for (String k : pBodyMap.keySet()) {
			result += getXmlElementString(k, pBodyMap.get(k));
		}
		return result;
	}

}
