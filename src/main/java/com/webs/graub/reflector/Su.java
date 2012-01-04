package com.webs.graub.reflector;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import android.util.Log;

public class Su {

	public static boolean testRootPrivileges() {
		boolean retval = false;
		Process suProcess;

		try {
			suProcess = Runtime.getRuntime().exec("su");

			DataOutputStream os = new DataOutputStream(
					suProcess.getOutputStream());
			BufferedReader osRes = new BufferedReader(
					new InputStreamReader(suProcess.getInputStream()));

			if (null != os && null != osRes) {
				// Getting the id of the current user to check if this is root
				os.writeBytes("id\n");
				os.flush();

				String currUid = osRes.readLine();
				boolean exitSu = false;
				if (null == currUid) {
					retval = false;
					exitSu = false;
					Log.d("Reflector", "Can't get root access or denied by user");
				} else if (true == currUid.contains("uid=0")) {
					retval = true;
					exitSu = true;
					Log.d("Reflector", "Root access granted");
				} else {
					retval = false;
					exitSu = true;
					Log.d("Reflector", "Root access rejected: " + currUid);
				}

				if (exitSu) {
					os.writeBytes("exit\n");
					os.flush();
				}
			}
		} catch (Exception e) {
			// Can't get root !
			// Probably broken pipe exception on trying to write to output
			// stream after su failed, meaning that the device is not rooted

			retval = false;
			Log.d("Reflector", "Root access rejected [" + e.getClass().getName()
					+ "] : " + e.getMessage());
		}

		return retval;
	}

	public static boolean execute(List<String> commands) {
		boolean retval = false;

		try {
			if (null != commands && commands.size() > 0) {
				Process suProcess = Runtime.getRuntime().exec("su");

				DataOutputStream os = new DataOutputStream(
						suProcess.getOutputStream());

				// Execute commands that require root access
				for (String currCommand : commands) {
					os.writeBytes(currCommand + "\n");
					os.flush();
				}

				os.writeBytes("exit\n");
				os.flush();

				try {
					int suProcessRetval = suProcess.waitFor();
					if (255 != suProcessRetval) {
						// Root access granted
						retval = true;
					} else {
						// Root access denied
						retval = false;
					}
				} catch (Exception ex) {
					Log.e("Reflector", "Error executing root action", ex);
				}
			}
		} catch (IOException ex) {
			Log.w("Reflector", "Can't get root access", ex);
		} catch (SecurityException ex) {
			Log.w("Reflector", "Can't get root access", ex);
		} catch (Exception ex) {
			Log.w("Reflector", "Error executing internal operation", ex);
		}

		return retval;
	}

}
