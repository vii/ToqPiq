package com.mopoko.toqpiq;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class JSONNetReq extends NetReq<JSONObject> {

    @Override
    protected JSONObject decodeStream(InputStream stream) {
        try {
            return new JSONObject(E.inputUTF8StreamToString(stream));
        } catch (Exception e) {
            E.ex(e);
            return null;
        }
    }

    public JSONNetReq(String URL) {
        super(URL);
    }
}
