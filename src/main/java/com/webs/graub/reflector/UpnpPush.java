package com.webs.graub.reflector;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.android.AndroidUpnpServiceConfiguration;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.message.header.STAllHeader;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.RemoteService;
import org.teleal.cling.model.types.UDAServiceId;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.SetAVTransportURI;

import com.webs.graub.tinywebserver.Content;
import com.webs.graub.tinywebserver.HttpStatus;
import com.webs.graub.tinywebserver.Library;
import com.webs.graub.tinywebserver.Server;

import static com.webs.graub.reflector.ReflectorComms.comms;

/**
 * Class that handles UPNP Push control sequence to remote renderer
 */
public class UpnpPush {

	static final int PORT = 27801;
	enum State {
		OFF,
		INITIALIZING,
		STOPPED,
		WAITING_URI,
		WAITING_PLAY,
		PLAYING,
		WAITING_STOP
	};

	// members
	private UpnpService mUpnpService;
	private State mState;
	private HashMap<UDN,RemoteDevice> mRenderers = new HashMap<UDN,RemoteDevice>();
	private ReflectorService mService;
	private RemoteService mTransportService;
	private Server mWebServer;
	private BitmapContentAdapter mWebContent;
	private WifiManager mWifiManager;

	public UpnpPush(ReflectorService service) {
		this.mService = service;
		mState = State.OFF;
	}

	class LocalListener implements RegistryListener {
		@Override
		public void afterShutdown() {
			Log.i("Reflector", "RegistryListener.afterShutdown()");
		}
		@Override
		public void beforeShutdown(Registry arg0) {
			Log.i("Reflector", "RegistryListener.beforeShutdown()");
		}
		@Override
		public void localDeviceAdded(Registry arg0, LocalDevice arg1) {
			Log.i("Reflector", "RegistryListener.localDeviceAdded()");
		}
		@Override
		public void localDeviceRemoved(Registry arg0, LocalDevice arg1) {
			Log.i("Reflector", "RegistryListener.localDeviceRemoved()");
		}
		@Override
		public void remoteDeviceDiscoveryFailed(Registry arg0, RemoteDevice arg1, Exception arg2) {
			Log.i("Reflector", "RegistryListener.remoteDeviceDiscoveryFailed()");
		}
		@Override
		public void remoteDeviceDiscoveryStarted(Registry arg0, RemoteDevice arg1) {
			Log.i("Reflector", "RegistryListener.remoteDeviceDiscoveryStarted()");
		}
		@Override
		public void remoteDeviceAdded(Registry arg0, RemoteDevice arg1) {
			Log.i("Reflector", "RegistryListener.remoteDeviceAdded()"
					+ " type=" + arg1.getType().getType()
					+ " name=" + arg1.getDetails().getFriendlyName() );
			if (arg1.getType().getType().equals("MediaRenderer")) {
				mRenderers.put(arg1.getIdentity().getUdn(), arg1);
				updateDeviceList(mRenderers);
			}
		}
		@Override
		public void remoteDeviceRemoved(Registry arg0, RemoteDevice arg1) {
			Log.i("Reflector", "RegistryListener.remoteDeviceRemoved():"
					+arg1.getDetails().getFriendlyName());
			if (mRenderers.containsKey(arg1.getIdentity().getUdn())) {
				mRenderers.remove(arg1.getIdentity().getUdn());
				updateDeviceList(mRenderers);
			}
		}
		@Override
		public void remoteDeviceUpdated(Registry arg0, RemoteDevice arg1) {
			Log.i("Reflector", "RegistryListener.remoteDeviceUpdated()");
		}
	};

	private void updateDeviceList(Map<UDN,RemoteDevice> renderers) {
		LinkedList<String> dlist = new LinkedList<String>();
		for(RemoteDevice d: renderers.values()) {
			dlist.add(d.getDetails().getFriendlyName());
		}
		comms().notifyDeviceList(dlist);
	}


	public void init() throws IOException {

		mState = State.INITIALIZING;

		mWifiManager = (WifiManager)mService.getSystemService(Context.WIFI_SERVICE);
        mUpnpService = new UpnpServiceImpl(new AndroidUpnpServiceConfiguration(mWifiManager));
        mUpnpService.getRegistry().addListener(new LocalListener());
        mUpnpService.getControlPoint().search(new STAllHeader());
		mState = State.STOPPED;

		Library lib = new Library() {
			@Override
			public Content getContent(URI uri) throws HttpStatus {
				if (uri.getPath().contains(mWebContent.uniqueId())) {
					return mWebContent;
				} else {
					return null;
				}
			}
		};
		mWebServer = new Server(lib, PORT);
		mWebServer.start();
	}

	public boolean checkRenderer() {
		if (mService.getSelectedDevice() == null) {
			return false; // no error, renderer just not selected yet
		}

		// see the renderer user has chosen
		mTransportService = null;
		for(RemoteDevice d: mRenderers.values()) {
			if (d.getDetails().getFriendlyName().equals(mService.getSelectedDevice())) {
				mTransportService = d.findService(new UDAServiceId("AVTransport"));
				return true;
			}
		}

		comms().notifyStatus("Device not present");
		return false;
	}

	public void push(Bitmap bitmap) {
		if (mTransportService==null) return;

		String addr = "http://" + getDeviceIp() + ":" + PORT + "/" + mWebContent.uniqueId();
        new SetAVTransportURI(mTransportService, addr, "") {
            @SuppressWarnings("rawtypes")
			@Override
            public void success(ActionInvocation invocation) {
                new Play(mTransportService) {
                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    	comms().notifyStatus("Play error");
                        mState = State.STOPPED;
                    }
                };
            }
            @SuppressWarnings("rawtypes")
			@Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
            	comms().notifyStatus("setAVTransport error");
                mState = State.STOPPED;
            }
        };


	}

	public void shutdown() {
		mUpnpService.shutdown();
		mUpnpService = null;

		mWebServer.stopServer();
	}

	String getDeviceIp() {
		return Formatter.formatIpAddress(mWifiManager.getConnectionInfo().getIpAddress());
	}

	State state() {
		return mState;
	}


}
