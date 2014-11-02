package com.mopoko.toqpiq;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetReq<V> {
    static ExecutorService sNetExec = Executors.newFixedThreadPool(4);
    static ConcurrentHashMap<String, NetReq<?>> sCache = new ConcurrentHashMap<String, NetReq<?>>();

    HashSet<UnaryVoid<V>> mListeners;
    String mURL;
    V mValue;

    public NetReq(String URL) {
        mListeners = new HashSet<UnaryVoid<V>>();
        mURL = URL;
    }

    public NetReq<?> launch(UnaryVoid<V> f) {
        assert mListeners.isEmpty();
        NetReq<?> d;
        synchronized (sCache) {
            d = (NetReq<?>) sCache.get(mURL);
            if (d == null) {
                sCache.put(mURL, this);
            }
        }
        if (d == null) {
            listen(f);
            startReq();
            return this;
        } else {
            d.listen(f);
            return d;
        }
    }

    private void startReq() {
        sNetExec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection) new URL(mURL).openConnection();

                    if (urlConnection.getResponseCode() != 200) {
                        throw new IOException("HTTP status code " + Integer.toString(urlConnection.getResponseCode()) + ": " + mURL);
                    }
                    InputStream stream = urlConnection.getInputStream();
                    V v;
                    try {
                        v = decodeStream(stream);
                    } finally {
                        stream.close();
                    }

                    mValue = v;
                    maybeSendResult();

                } catch (Exception e) {
                    E.ex(e);
                }


            }
        });
    }

    protected V decodeStream(InputStream stream) {
        throw new UnsupportedOperationException("you must subclass NetReq");
    }

    public void maybeSendResult() {
        HashSet<UnaryVoid<V>> tmp;
        synchronized (this) {
            if (mValue == null) {
                return;
            }
            tmp = mListeners;
            mListeners = null;
        }

        for (UnaryVoid<V> f : tmp) {
            try {
                f.call(mValue);
            } catch (Exception e) {
                E.ex(e);
            }
        }
    }


    private NetReq<V> listen(Object f) {
        synchronized (this) {
            mListeners.add((UnaryVoid<V>)f);
        }
        maybeSendResult();
        return this;
    }
}
