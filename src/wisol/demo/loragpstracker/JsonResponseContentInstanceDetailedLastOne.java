package wisol.demo.loragpstracker;

import com.google.gson.annotations.SerializedName;

public class JsonResponseContentInstanceDetailedLastOne {
	@SerializedName("contentInstances")
	private JsonContentInstances contentInstances;

	public JsonContentInstances getContentInstances() {
		return this.contentInstances;
	}

	public JsonContentInstancesCollection getcontentInstancesCollection() {
		return this.getContentInstances().getcontentInstancesCollection();
	}

	public boolean isNextData() {
		return this.getContentInstances().isNextData();
	}

	public int getTotalCount() {
		return this.getContentInstances().getTotalCount();
	}

	public int getCurrentNrOfInstances() {
		return this.getContentInstances().getCurrentNrOfInstances();
	}

	public JsonContentInstanceDetail getContentInstanceDetail() {
		return this.getcontentInstancesCollection().getContentInstanceDetail();
	}

	public class JsonContentInstances {
		@SerializedName("contentInstancesCollection")
		private JsonContentInstancesCollection contentInstancesCollection;

		@SerializedName("nextData")
		private String nextData;

		@SerializedName("totalCount")
		private String totalCount;

		@SerializedName("currentNrOfInstances")
		private String currentNrOfInstances;

		public JsonContentInstancesCollection getcontentInstancesCollection() {
			return this.contentInstancesCollection;
		}

		public boolean isNextData() {
			return Boolean.valueOf(this.nextData);
		}

		public int getTotalCount() {
			return Integer.valueOf(this.totalCount);
		}

		public int getCurrentNrOfInstances() {
			int result = 0;
			try {
				result = Integer.valueOf(this.currentNrOfInstances);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return result;
		}

	}

	public class JsonContentInstancesCollection {
		@SerializedName("contentInstance")
		private JsonContentInstanceDetail contentInstanceDetail;

		public JsonContentInstanceDetail getContentInstanceDetail() {
			return contentInstanceDetail;
		}

	}
}
