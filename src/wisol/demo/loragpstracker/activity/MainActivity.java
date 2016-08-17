package wisol.demo.loragpstracker.activity;

import java.util.Timer;
import java.util.TimerTask;

import wisol.demo.loragpstracker.R;
import wisol.demo.loragpstracker.TestService;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);// It must be located
		// prior to the
		// setContentView
		setContentView(R.layout.layout_activity_main);

		if (!isLocationServiceEnabled()) {
			popUpLocationSetting();
		}
	}

	private void startActivityDelayed(Intent pIntent, long pTimeDelay) {
		final Intent intent = pIntent;

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				startActivity(intent);
			}
		}, pTimeDelay);
	}
	
	
	@Override
	protected void onPause() {
//		launchTestService();

		super.onPause();
	}

	public void launchTestService() {
		Intent i = new Intent(this, TestService.class);

		startService(i);
	}

	@Override
	protected void onResume() {
		stopService(new Intent(this, TestService.class));

		if (isLocationServiceEnabled()) {
//			startActivityDelayed(new Intent(this, GpsMainActivity.class), 2500);
			startActivityDelayed(new Intent(this, MultiTrackerListActivity.class), 2500);
		}
		super.onResume();
	}

	private void popUpLocationSetting() {
		AlertDialog.Builder dlgLocation = new AlertDialog.Builder(this);

		dlgLocation.setMessage("Go to Location setting")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					}
				})
				.setNegativeButton("Exit", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						finishAffinity();
					}
				})
				.setTitle("Location Service is disabled")
				.setIcon(R.drawable.ic_launcher)
				.show();
	}

	private boolean isLocationServiceEnabled() {
		boolean result = false;
		LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		boolean gps_enabled = false;
		boolean network_enabled = false;

		try {
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		result = gps_enabled | network_enabled;

		return result;
	}
}
