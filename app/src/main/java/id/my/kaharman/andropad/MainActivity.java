package id.my.kaharman.andropad;

import id.my.kaharman.andropad.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.IOException;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

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

    private static final String BASE_URL = "http://192.168.43.208:7777/1/";
//    private static final String BASE_URL = "http://192.168.43.187:7777/1/";
    private Vibrator hapticVibration;
    private SparseArray<PointF> mActivePointers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

//        final View controlsView = findViewById(R.id.fullscreen_content_controls);
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
/*
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
*/

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
/*
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
*/

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        hapticVibration = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        mActivePointers = new SparseArray<PointF>();
        ImageView controllerMask = (ImageView) findViewById(R.id.buttonMaskImage);
        controllerMask.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // get pointer index from the event object
                int pointerIndex = event.getActionIndex();

                // get pointer ID
                int pointerId = event.getPointerId(pointerIndex);

                // get masked (not specific to a pointer) action
                int maskedAction = event.getActionMasked();

                Boolean flagUpdate = false;

                switch (maskedAction) {

                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN: {
                        // We have a new pointer. Lets add it to the list of pointers

                        PointF f = new PointF();
                        f.x = event.getX(pointerIndex);
                        f.y = event.getY(pointerIndex);
                        mActivePointers.put(pointerId, f);
                        flagUpdate = true;
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: { // a pointer was moved
                        for (int size = event.getPointerCount(), i = 0; i < size; i++) {
                            PointF point = mActivePointers.get(event.getPointerId(i));
                            if (point != null) {
                                point.x = event.getX(i);
                                point.y = event.getY(i);
                            }
                        }
                        break;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        mActivePointers.remove(pointerId);
                        break;
                    }
                }

                if (flagUpdate == true) {
                    for (int size = mActivePointers.size(), i = 0; i < size; i++) {
                        PointF point = mActivePointers.valueAt(i);
                        if (point != null) {
                            int touchColor = getHotspotColor(R.id.buttonMaskImage, (int) point.x, (int) point.y);

                            ColorTool ct = new ColorTool();
                            if (ct.closeMatch(0xFF800000, touchColor, 25)) {
                                new SendCommand().execute(BASE_URL + "up/1");
                                hapticVibration.vibrate(50);
                            }
                        }
                    }
                }

                return true;
            }
        });

/*
        ImageButton buttonUp = (ImageButton) findViewById(R.id.buttonUp);
        buttonUp.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new SendCommand().execute(BASE_URL + "up/1");
                    hapticVibration.vibrate(50);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    new SendCommand().execute(BASE_URL + "up/0");
                }
                return false;
            }
        });

        ImageButton buttonDown = (ImageButton) findViewById(R.id.buttonDown);
        buttonDown.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new SendCommand().execute(BASE_URL + "down/1");
                    hapticVibration.vibrate(50);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    new SendCommand().execute(BASE_URL + "down/0");
                }
                return false;
            }
        });
        ImageButton buttonLeft = (ImageButton) findViewById(R.id.buttonLeft);
        buttonLeft.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new SendCommand().execute(BASE_URL + "left/1");
                    hapticVibration.vibrate(50);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    new SendCommand().execute(BASE_URL + "left/0");
                }
                return false;
            }
        });

        ImageButton buttonRight = (ImageButton) findViewById(R.id.buttonRight);
        buttonRight.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new SendCommand().execute(BASE_URL + "right/1");
                    hapticVibration.vibrate(50);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    new SendCommand().execute(BASE_URL + "right/0");
                }
                return false;
            }
        });

        ImageButton buttonA = (ImageButton) findViewById(R.id.buttonA);
        buttonA.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new SendCommand().execute(BASE_URL + "a/1");
                    hapticVibration.vibrate(50);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    new SendCommand().execute(BASE_URL + "a/0");
                }
                return false;
            }
        });

        ImageButton buttonB = (ImageButton) findViewById(R.id.buttonB);
        buttonB.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new SendCommand().execute(BASE_URL + "b/1");
                    hapticVibration.vibrate(50);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    new SendCommand().execute(BASE_URL + "b/0");
                }
                return false;
            }
        });

        ImageButton buttonX = (ImageButton) findViewById(R.id.buttonX);
        buttonX.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new SendCommand().execute(BASE_URL + "x/1");
                    hapticVibration.vibrate(50);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    new SendCommand().execute(BASE_URL + "x/0");
                }
                return false;
            }
        });

        ImageButton buttonY = (ImageButton) findViewById(R.id.buttonY);
        buttonY.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new SendCommand().execute(BASE_URL + "y/1");
                    hapticVibration.vibrate(50);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    new SendCommand().execute(BASE_URL + "y/0");
                }
                return false;
            }
        });

        ImageButton buttonSelect = (ImageButton) findViewById(R.id.buttonSelect);
        buttonSelect.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new SendCommand().execute(BASE_URL + "select/1");
                    hapticVibration.vibrate(50);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    new SendCommand().execute(BASE_URL + "select/0");
                }
                return false;
            }
        });

        ImageButton buttonStart = (ImageButton) findViewById(R.id.buttonStart);
        buttonStart.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new SendCommand().execute(BASE_URL + "start/1");
                    hapticVibration.vibrate(50);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    new SendCommand().execute(BASE_URL + "start/0");
                }
                return false;
            }
        });

        ImageButton buttonSettings = (ImageButton) findViewById(R.id.buttonSettings);
        buttonSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });
*/
    }

    public int getHotspotColor (int hotspotId, int x, int y) {
        ImageView img = (ImageView) findViewById (hotspotId);
        img.setDrawingCacheEnabled(true);
        Bitmap hotspots = Bitmap.createBitmap(img.getDrawingCache());
        img.setDrawingCacheEnabled(false);
        return hotspots.getPixel(x, y);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private class SendCommand extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String response = null;

            try {
                HttpConnection httpConnection = new HttpConnection();
                response = httpConnection.downloadUrl(urls[0]);
            }
            catch (IOException e) {
                Log.e("SendCommand", "Error HttpConnection");
                return null;
            }

            Log.d("SendCommand", "[" + response.length() + "] "+ response);

            return response;
        }

        @Override
        protected void onPostExecute(String response) {

        }
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
