package com.mopoko.toqpiq;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;

public class BitmapNetReq extends NetReq<Bitmap> {

    @Override
    protected Bitmap decodeStream(InputStream stream) {
        return BitmapFactory.decodeStream(stream);
    }

    public BitmapNetReq(String URL) {
        super(URL);
    }
}
