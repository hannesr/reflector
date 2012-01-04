package com.webs.graub.reflector;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

import static com.webs.graub.reflector.ReflectorComms.comms;

public class ReflectorService extends Service {
	// constants
	private static final String PREFERENCES = "Reflector";
	private static final String KEY_SELECTED_DEVICE = "SelectedDevice";
	private static final String KEY_REFLECT_DELAY = "ReflectDelay";

	// members
	private SharedPreferences mSettings;
	private UpnpPush mPush;
	private WindowManager mWindowManager;
	private boolean mTestMode;
	Timer mTimer = new Timer("Reflector timer");

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("Reflector", "onCreate");
		ReflectorComms.registerService(this);

		// read settings
		mSettings = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
		mWindowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
		TestBitmap.reset();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("Reflector", "onStartCommand");

		if (!Installer.install()) {
			Log.d("Reflector", "running in test mode");
			mTestMode = true;
		}

		mPush = new UpnpPush(this);
		try {
			mPush.init();
			comms().notifyStatus(mTestMode ? "running (test mode)" : "running");
		} catch (Exception ex) {
			comms().notifyStatus("error: "+ex);
		}

		// start timer
		int delay = getReflectDelay();
		mTimer.schedule(new ReflectTask(), delay, delay);
		// This service will run until explicitly stopped
		comms().notifyServiceRunning(true);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.d("Reflector", "onDestroy");
		// cancel timer
		mTimer.cancel();
		// stop the control point
		mPush.shutdown();
		// mark server inactive in settings
		comms().notifyStatus("not running");
		comms().notifyServiceRunning(false);
		ReflectorComms.registerService(null);
		super.onDestroy();
	}

	String getSelectedDevice() {
		return mSettings.getString(KEY_SELECTED_DEVICE, "");
	}

	void setSelectedDevice(String device) {
		mSettings.edit()
		.putString(KEY_SELECTED_DEVICE, device)
		.commit();
	}

	/**
	 * TimerTask to execute the single "Reflect" operation: taking a
	 * screenshot and showing it on the UPNP renderer
	 */
	class ReflectTask extends TimerTask {
		@Override
		public void run() {
			if (mPush.checkRenderer()) {
				
				Bitmap bmp = null;
				if (!mTestMode) {
					bmp = new Screenshot(mWindowManager).take().toBitmap(true);
				} else {
					bmp = new TestBitmap().getBitmap();
				}

				if (bmp != null) {
					mPush.push(bmp);
				} else {
					Log.i("Reflector", "Unable to reflect - bitmap creation failed");
				}
			} else {
				Log.i("Reflector", "Unable to reflect - renderer not present");
			}
		}
		
	}

	int getReflectDelay() {
		return mSettings.getInt(KEY_REFLECT_DELAY, 2000);
	}

	void setReflectDelay(int delay) {
		mSettings.edit().putInt(KEY_REFLECT_DELAY, delay).commit();
	}
}

