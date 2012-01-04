package com.webs.graub.reflector;

import java.util.List;

public interface ReflectorClient {

	void notifyServiceRunning(boolean running);

	void notifyStatus(String status);
	
	void notifyDeviceList(List<String> devices);

}
