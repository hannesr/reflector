package com.webs.graub.reflector;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import static com.webs.graub.reflector.ReflectorComms.comms;

public class ControlPanel extends Activity implements ReflectorClient {
	// members
	ToggleButton mOnOff;
	TextView mStatus;
	Button mDeviceButton;
	ProgressBar mDeviceProgress;
	ListView mDeviceList;
	ArrayAdapter<String> mDeviceListAdapter;
	ReflectorService mService;

	@Override
	public void onCreate( Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mOnOff = (ToggleButton)findViewById(R.id.onOffButton);
		mStatus = (TextView)findViewById(R.id.statusView);
		mDeviceList = (ListView)findViewById(R.id.deviceList);
		mDeviceListAdapter = new ArrayAdapter<String>(this, R.layout.list_item);
		mDeviceList.setAdapter(mDeviceListAdapter);

		ReflectorComms.registerClient(this);

		// update UI state
		mOnOff.setChecked(comms().isServiceRunning());
		mStatus.setText(comms().getServiceStatus());
		// onoff button handler
		mOnOff.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ControlPanel.this, ReflectorService.class);
				if (mOnOff.isChecked()) {
					startService(i);
				} else {
					stopService(i);
				}
			}
		});

		// the device list
		mDeviceList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> av, View view, int position, long id) {
				String devName = mDeviceListAdapter.getItem(position);
				Log.d("Reflector", "onItemClick: "+position+" -> "+devName);
				comms().selectDevice(devName);
			}
		});

	}

	@Override
	public void onDestroy() {
		ReflectorComms.registerClient(null);
		super.onDestroy();
	}

	@Override
	public void notifyServiceRunning(boolean running) {
		Log.d("Reflector", "notifyServiceRunning: "+running);
		mOnOff.setChecked(running);
	}

	@Override
	public void notifyStatus(String status) {
		Log.d("Reflector", "notifyStatus: "+status);
		mStatus.setText(status);
	}

	@Override
	public void notifyDeviceList(List<String> devices) {
		Log.d("Reflector", "notifyDeviceList: list("+devices.size()+")");
		mDeviceListAdapter.clear();
		for(String d: devices) {
			mDeviceListAdapter.add(d);
		}
	}

}

