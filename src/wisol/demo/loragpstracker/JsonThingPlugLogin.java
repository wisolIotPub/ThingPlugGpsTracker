package wisol.demo.loragpstracker;

import com.google.gson.annotations.SerializedName;

public class JsonThingPlugLogin extends JsonThingPlug {
	@SerializedName("user")
	private ThingPlugUser thingPlugUuser;

	public ThingPlugUser getThingPlugUser() {
		return this.thingPlugUuser;
	}

	public boolean isAdmin() {
		return getThingPlugUser().isAdmin();
	}

	public String getPassword() {
		return getThingPlugUser().getPassword();
	}

	public String getUKey() {
		return getThingPlugUser().getUKey();
	}

	public String getUserId() {
		return getThingPlugUser().getUserId();
	}

	private class ThingPlugUser {
		@SerializedName("admin_yn")
		private String adminYn;
		@SerializedName("password")
		private String password;
		@SerializedName("uKey")
		private String uKey;
		@SerializedName("user_id")
		private String UserId;

		public boolean isAdmin() {
			boolean result = false;
			if (this.adminYn.equals("Y")) {
				result = true;
			}

			return result;
		}

		public String getPassword() {
			return this.password;
		}

		public String getUKey() {
			return this.uKey;
		}

		public String getUserId() {
			return this.UserId;
		}
	}
}
