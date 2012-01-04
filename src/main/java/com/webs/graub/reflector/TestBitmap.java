package com.webs.graub.reflector;

import android.graphics.Bitmap;
import android.graphics.Color;

public class TestBitmap {

	static int counter = 0;
	Bitmap bmp;

	TestBitmap() {
		++counter;
		bmp = Bitmap.createBitmap(300,600,Bitmap.Config.ARGB_8888);
		
		for(int i=0; i<counter; ++i) {
			if (4*i > 600) break;
			for(int x=10;x<290;++x) {
				bmp.setPixel(x,4*i,Color.RED);
			}
		}
	}

	static void reset() {
		counter = 0;
	}

	Bitmap getBitmap() {
		return bmp;
	}
}
