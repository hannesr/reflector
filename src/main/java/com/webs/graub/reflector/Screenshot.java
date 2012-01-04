package com.webs.graub.reflector;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

/**
 * A class that represents a snapshot of the screen. When an object of this
 * class is instantiated, the snapshot is created. The class uses ASL
 * (android screenshot library) native library to accomplish this and then
 * converts the data to bitmap form. Test existence of the library
 * using the static isAvailable() method. If the library is not installed,
 * you should install to your device first. <br>
 * 
 * Credits: thanks go to pl.polidea.asl.ScreenshotService, which has been
 * used as an example
 */
public class Screenshot {

	// members
	WindowManager wm;
	Buffer pixels;
	int width;
	int height;
	int bpp;

	// constants
	private static final int PORT = 42380;
//	private static final String NATIVE_PROCESS_NAME = "asl-native"; 


	/**
	 * takes a screenshot
	 * @param wm reference to window manager
	 * @param res reference to application resources, where screen default
	 *            orientation is
	 */
	public Screenshot(WindowManager wm) {
		this.wm = wm;
	}

	/**
	 * tests availability of the class
	 */
	public static boolean isAvailable() {
		try {
			Socket sock = new Socket();
			sock.connect(new InetSocketAddress("localhost", PORT), 10);	// short timeout
			sock.close();
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * tests validity of this screenshot
	 */
	public boolean isValid() {
		if (pixels == null || pixels.capacity() == 0 || pixels.limit() == 0)
			return false;
		if (width <= 0 || height <= 0)
			return false;
		return true;
	}

	/*
	 * Determines whether the phone's screen is rotated.
	 */
	private int getScreenRotation()  {
		Display disp = wm.getDefaultDisplay();
		
		// check whether we operate under Android 2.2 or later
		try {
			Class<?> displayClass = disp.getClass();
			Method getRotation = displayClass.getMethod("getRotation");
			int rot = ((Integer)getRotation.invoke(disp)).intValue();
			
				switch (rot) {
					case Surface.ROTATION_0:	return 0;
					case Surface.ROTATION_90:	return 90;
					case Surface.ROTATION_180:	return 180;
					case Surface.ROTATION_270:	return 270;
					default:					return 0;
				}
		} catch (NoSuchMethodException e) {
			// no getRotation() method -- fall back to dispation()
			int orientation = disp.getOrientation();

			// Sometimes you may get undefined orientation Value is 0
			// simple logic solves the problem compare the screen
			// X,Y Co-ordinates and determine the Orientation in such cases
			if(orientation==Configuration.ORIENTATION_UNDEFINED){

				//if height and widht of screen are equal then
				// it is square orientation
				if(disp.getWidth()==disp.getHeight()){
					orientation = Configuration.ORIENTATION_SQUARE;
				}else{ //if widht is less than height than it is portrait
					if(disp.getWidth() < disp.getHeight()){
						orientation = Configuration.ORIENTATION_PORTRAIT;
					}else{ // if it is not any of the above it will defineitly be landscape
						orientation = Configuration.ORIENTATION_LANDSCAPE;
					}
				}
			}

			return orientation == 1 ? 0 : 90; // 1 for portrait, 2 for landscape
		} catch (Exception e) {
			return 0; // bad, I know ;P
		}
	}

	/*
	 * Communicates with the native service and retrieves a screenshot from it
	 * as a 2D array of bytes.
	 */
	public Screenshot take() {
		try {
			// connect to native application
			// We use SocketChannel,because is more convenience and fast
			SocketChannel socket = SocketChannel.open(new InetSocketAddress("localhost", PORT));
			socket.configureBlocking(false);
			
			//Send command to take screenshot
			ByteBuffer cmdBuffer = ByteBuffer.wrap("SCREEN".getBytes("ASCII"));
			socket.write(cmdBuffer);
			
			//build a buffer to save the info of screenshot
			//3 parts,width height bpp
			//3 bytes width + 1 byte space + 3 bytes height + 1 byte space + 2 bytes bpp
			byte[] info = new byte[3 + 3 + 2 + 2];
			ByteBuffer infoBuffer = ByteBuffer.wrap(info);
			
			//we must make sure all the data have been read
			while(infoBuffer.position() != infoBuffer.limit())
				socket.read(infoBuffer);
			
			//we must read one more byte,because after this byte,we will read the image byte
			socket.read(ByteBuffer.wrap(new byte[1]));
			
			//set the position to zero that we can read it.
			infoBuffer.position(0);
			
			StringBuffer sb = new StringBuffer();
			for(int i = 0;i < (3 + 3 + 2 + 2); i++) {
				sb.append((char)infoBuffer.get());
			}
			
			String[] screenData = sb.toString().split(" ");
			if (screenData.length >= 3) {
				width = Integer.parseInt(screenData[0]);
				height = Integer.parseInt(screenData[1]);
				bpp = Integer.parseInt(screenData[2]);

				// retrieve the screenshot
				// (this method - via ByteBuffer - seems to be the fastest)
				ByteBuffer bytes = ByteBuffer.allocate (width * height * bpp / 8);
				while(bytes.position() != bytes.limit()) {
					// in the cycle,we must make sure all the image data have been read
					// maybe sometime the socket will delay a bit time and return some invalid bytes.
					socket.read(bytes);
				}
				bytes.position(0);					// reset position to the beginning of ByteBuffer
				pixels = bytes;
			}
		}
		catch (Exception e) {
			pixels = null;
		}
		return this;
	}


	/**
	 * retrieves the bitmap represented by this screenshot.
	 */
	public Bitmap toBitmap(boolean rotateAccordingToOrientation) {
		if (!isValid())
			return null;

		Bitmap.Config pf;
		switch (bpp) {
			case 16:	pf = Config.RGB_565; break;
			case 32:	pf = Config.ARGB_8888; break;
			default:	pf = Config.ARGB_8888; break;
		}
		Bitmap bmp = Bitmap.createBitmap(width, height, pf);
		bmp.copyPixelsFromBuffer(pixels);

		// handle the screen rotation
		if (rotateAccordingToOrientation) {
			int rot = getScreenRotation();
			if (rot != 0) {
				Matrix matrix = new Matrix();
				matrix.postRotate(-rot);
				bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
			}
		}
		return bmp;
	}
	
}

