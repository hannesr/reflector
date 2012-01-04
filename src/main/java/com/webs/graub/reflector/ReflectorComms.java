package com.webs.graub.reflector;

import java.util.List;


public class ReflectorComms implements ReflectorClient {

	static ReflectorComms mCommsInstance;
	ReflectorService mService;
	ReflectorClient mClient;
	// cached data
	boolean mServiceRunning = false;
	String mServiceStatus = "";
	List<String> mDeviceList = null;


	private ReflectorComms() {
	}

	static ReflectorComms comms() {
		assert(mCommsInstance!=null);
		return mCommsInstance;
	}

	private static void checkCreateSingleton() {
		if (mCommsInstance==null) {
			mCommsInstance = new ReflectorComms();
		}
	}

	private static void checkDisposeSingleton() {
		if (mCommsInstance.mService==null && mCommsInstance.mClient==null) {
			mCommsInstance = null;
		}
	}

	// API for the Service to attach
	static void registerService(ReflectorService service) {
		if (service != null) {
			// create instance
			checkCreateSingleton();
			mCommsInstance.mService = service;
		} else {
			mCommsInstance.mService = null;
			checkDisposeSingleton();
		}
	}

	// API for the Client to attach
	static void registerClient(ReflectorClient client) {
		if (client != null) {
			// create instance
			checkCreateSingleton();
			mCommsInstance.mClient = client;
		} else {
			mCommsInstance.mClient = null;
			checkDisposeSingleton();
		}
	}

	// communication methods for the Service
	@Override
	public void notifyServiceRunning(boolean running) {
		mServiceRunning = running;
		if (mClient!=null) {
			mClient.notifyServiceRunning(running);
		}
	}

	@Override
	public void notifyStatus(String status) {
		mServiceStatus = status;
		if (mClient!=null) {
			mClient.notifyStatus(status);
		}
	}

	@Override
	public void notifyDeviceList(List<String> devices) {
		mDeviceList = devices;
		if (mClient!=null) {
			mClient.notifyDeviceList(devices);
		}
	}

	// Methods for the client
	public void selectDevice(String device) {
		if (mService!=null) {
			mService.setSelectedDevice(device);
		}
	}

	public boolean isServiceRunning() {
		return mServiceRunning;
	}

	public String getServiceStatus() {
		return mServiceStatus;
	}

	public List<String> getDeviceList() {
		return mDeviceList;
	}

}
