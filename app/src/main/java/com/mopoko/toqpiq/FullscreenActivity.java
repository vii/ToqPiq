package com.mopoko.toqpiq;

import com.mopoko.toqpiq.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.Constants;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.ResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.ResourceStoreException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.Card;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.MenuOption;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.NotificationTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteToqNotification;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.CardImage;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.DeckOfCardsLauncherIcon;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.Resource;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.util.Logger;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.util.ParcelableUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {
    private DeckOfCardsManager toqManager;
    private RemoteResourceStore toqResources;
    private RemoteDeckOfCards toqCards;
    private int cardCount = 0;
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;
    private static final String TAG = "ToqPiq";
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
         * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;


    private void tryToqInstall() {
        if (toqCards == null) {
            return;
        }
        try {
            if (!toqManager.isInstalled()) {
                toqManager.installDeckOfCards(toqCards, toqResources);
            } else {
                toqManager.updateDeckOfCards(toqCards, toqResources);
            }
        } catch (RemoteDeckOfCardsException e) {
            E.ex(e);
        }
    }

    private void addComic(Bitmap bm, JSONObject comic) {
        if (toqCards == null) {
            toqCards = new RemoteDeckOfCards(getApplicationContext(), new ListCard());
        }
        ListCard toqList = toqCards.getListCard();
        if (toqList.isFull()) {
            toqList.remove(toqList.size()-1);
        }
        int num = cardCount;
        try {
            num = comic.getInt("num");
        } catch (JSONException e) {
            E.ex(e);
        }
        SimpleTextCard card = new SimpleTextCard("card" + Integer.toString(num));
        try {
            card.setHeaderText(comic.getString("title"));
        } catch (JSONException e) {
            E.ex(e);
        }

        try {
            card.setTitleText(comic.getString("alt"));
        } catch (JSONException e) {
            E.ex(e);
        }
        card.setReceivingEvents(true);
        card.setMenuOptionObjs(new MenuOption[]{
            new MenuOption("skip", false),
        });
        try {
            card.setMessageText(comic.getString("transcript").split("\n"));
        } catch (JSONException e) {
            E.ex(e);
        }
        CardImage cardImage = new CardImage("image" + Integer.toString(num), scaleToqBitmap(bm));
        toqResources.addResource(cardImage);
        card.setCardImage(toqResources, cardImage);
        toqList.add(card);
        ++cardCount;

        tryToqInstall();
    }

    private Bitmap scaleToqBitmap(Bitmap bm) {
        int w = 250;
        int h = 288;
        if (bm.getHeight() == h && bm.getWidth() == w) {
            return bm;
        }
        Bitmap dest = Bitmap.createBitmap(w, h, bm.getConfig());
        float sW = w / (float)bm.getWidth();
        float sH = h / (float)bm.getHeight();
        float s = Math.min(1f, Math.min(sW, sH));

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(s, s, 0, 0);

        Canvas canvas = new Canvas(dest);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bm, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG));
        canvas.save();
        return dest;
    }

    private void setupToq() {
        toqManager = DeckOfCardsManager.getInstance(getApplicationContext());
        toqResources = new RemoteResourceStore();

        if (!toqManager.isConnected()) {
            toqManager.addDeckOfCardsManagerListener(new DeckOfCardsManagerListener() {
                @Override
                public void onConnected() {
                    tryToqInstall();
                }

                @Override
                public void onDisconnected() {

                }

                @Override
                public void onInstallationSuccessful() {

                }

                @Override
                public void onInstallationDenied() {

                }

                @Override
                public void onUninstalled() {

                }
            });

            try {
                toqManager.connect();
            } catch (RemoteDeckOfCardsException e) {
                E.ex(e);
            }
        } else {
            tryToqInstall();
        }
    }

    private void fetchComicImage(String img, final JSONObject comic) {
        new BitmapNetReq(img).launch(new UnaryVoid<Bitmap>() {
            @Override
            public void call(Bitmap o) {
                addComic(o, comic);
            }
        });
    }

    private void fetchComic(String url) {
        new JSONNetReq(url).launch(new UnaryVoid<JSONObject>() {
            @Override
            public void call(JSONObject comic) {
                try {
                    fetchComicImage(comic.getString("img"), comic);
                } catch (JSONException e) {
                    E.ex(e);
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToq();
        Random r = new Random();
        for (int i=0; 20>i; ++i) {
            fetchComic("http://xkcd.com/" + Integer.toString(r.nextInt(1400)) + "/info.0.json");
        }
        setContentView(R.layout.activity_fullscreen);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
     }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
