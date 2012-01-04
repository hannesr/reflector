package com.webs.graub.reflector;

import java.util.ArrayList;

import android.util.Log;

public class Installer {

	static boolean install() {

		// 1. check if native is already installed
		if (Screenshot.isAvailable()) {
			return true; // yes is installed and running
		}

		Log.d("Reflector", "Screenshot service is not available, trying to install");

		// 2. check if user has root privileges
		if (!Su.testRootPrivileges()) {
			Log.w("Reflector", "Unable to install, lacking root privileges");
			return false;
		}

		// 3. install the native
		// TODO: maybe we should have several precompiled binaries ? check android version
		// here and choose and install the correct binary here???
		ArrayList<String> cmds = new ArrayList<String>();
		cmds.add("unzip -o /data/app/com.acs.screencap* *asl-native -d /data/local/tmp");
		cmds.add("mv /data/local/tmp/assets/asl-native /data/local/asl-native");
		cmds.add("chmod 0777 /data/local/asl-native");
		cmds.add("/data/local/asl-native");
		Su.execute(cmds);

		return false;
	}
}
