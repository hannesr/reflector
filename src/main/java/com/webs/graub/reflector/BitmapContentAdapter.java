package com.webs.graub.reflector;

import java.io.IOException;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

import com.webs.graub.tinywebserver.OutContent;

/**
 * Web Content adapter that represents an Android Bitmap file.
 * Upon request the class will compress the bitmap into PNG or JPG
 * and send it into the given stream, to be sent to web client.
 */
public class BitmapContentAdapter implements OutContent {

	Bitmap mBitmap;
	CompressFormat mFormat;

	public BitmapContentAdapter(Bitmap bitmap, CompressFormat format) {
		this.mBitmap = bitmap;
	}

	public String uniqueId() {
		return Integer.toHexString(mBitmap.hashCode());
	}

	@Override
	public String getMimetype() {
		if (mFormat==CompressFormat.JPEG) return "image/jpeg";
		if (mFormat==CompressFormat.PNG) return "image/png";
		throw new RuntimeException("Unrecognized bitmap compress format: "+mFormat);
	}

	@Override
	public int getDataSize() {
		return 0; // unknown
	}

	@Override
	public void out(OutputStream out) throws IOException {
		mBitmap.compress(mFormat, 100, out);
		out.flush();
	}

}
