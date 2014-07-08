package it.trilogis.android.ww;

import java.util.ArrayList;

import it.trilogis.android.ww.util.SystemUiHider;


import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.ToggleButton;
import it.polimi.models.Coordinate;
import it.polimi.models.ImageMarker;
import it.polimi.snowwatch.Mountain;
import it.polimi.snowwatch.MountainAdapter;
import it.polimi.snowwatch.MountainOverlay;
import it.polimi.snowwatch.OrientationManager;
import it.polimi.snowwatch.CameraPreview;
import it.polimi.snowwatch.utils.Vector3;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class ARActivity extends Activity implements
		OrientationManager.OnUpdateEventListener,
		CameraPreview.OnPreviewEventListener {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 10000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = 0;// SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	/**
	 * Managers
	 */

	private OrientationManager mOrientationManager = null;

	/**
	 * UI Components
	 */

	private CameraPreview mPreview = null;
	private MountainOverlay mMountainOverlay = null;
	private View mControlsView = null;
	private View mWaitView = null;
	private View mSensorsView = null;
	private View mPeaksView = null;
	private SeekBar mAzimuthSeekBar = null;
	private SeekBar mPitchSeekBar = null;
	private SeekBar mRollSeekBar = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Window w = getWindow();
		w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_ar);

		mControlsView = findViewById(R.id.fullscreen_content_controls);
		final View controlsView = mControlsView;
		mWaitView = findViewById(R.id.wait_layout);
		mSensorsView = findViewById(R.id.sensors_correction_layout);
		mPeaksView = findViewById(R.id.mountain_selection_layout);
		mAzimuthSeekBar = (SeekBar) findViewById(R.id.seek_bar_azimuth);
		mPitchSeekBar = (SeekBar) findViewById(R.id.seek_bar_pitch);
		mRollSeekBar = (SeekBar) findViewById(R.id.seek_bar_roll);

		mOrientationManager = new OrientationManager(this);

		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this);
		final FrameLayout cameraView = (FrameLayout) findViewById(R.id.camera_preview);
		cameraView.addView(mPreview);

		mMountainOverlay = new MountainOverlay(this);
		cameraView.addView(mMountainOverlay);

		mPreview.setPreviewEventListener(this);
		mOrientationManager.setUpdateEventListener(this);

		Bundle b = getIntent().getExtras();
		Coordinate location = new Coordinate(b.getDouble("latitude"), b.getDouble("longitude"));
		double altitude = b.getDouble("altitude");
		@SuppressWarnings("unchecked")
		ArrayList<ImageMarker> markers = (ArrayList<ImageMarker>)b.getSerializable("markers");
		
		for (ImageMarker marker : markers) {
			mMountainOverlay.add(new Mountain(
					marker.name,
					(float)location.distance(marker.center),
					new Vector3((float) Math.cos(Math.toRadians(location.bearing(marker.center))),
							(float) ((marker.altitude - altitude) / location.distance(marker.center) * 1000),
							(float) Math.sin(Math.toRadians(location.bearing(marker.center))))
					));
		}
		
		// mDownloader.setID("44");

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, cameraView,
				HIDER_FLAGS);
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
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide()
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		cameraView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		mAzimuthSeekBar
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						mMountainOverlay.saveSettings();
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onProgressChanged(SeekBar arg0, int value,
							boolean arg2) {
						mMountainOverlay.setAzimuthCorrection((value - 180) / 8f);
					}
				});

		mPitchSeekBar
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						mMountainOverlay.saveSettings();
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onProgressChanged(SeekBar arg0, int value,
							boolean arg2) {
						mMountainOverlay.setPitchCorrection((value - 180) / 8f);
					}
				});

		mRollSeekBar
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						mMountainOverlay.saveSettings();
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onProgressChanged(SeekBar arg0, int value,
							boolean arg2) {
						mMountainOverlay.setRollCorrection((value - 180) / 8f);
					}
				});
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) 
    {
        switch (keyCode)
        {
        /**
         * To manage settings in a standard way
         */
        case KeyEvent.KEYCODE_BACK:
            if (isSensorsCorrectionOpen()){
            	hideSensors();
            	return true;
            }
            if (isPeaksListOpen()){
            	hidePeaksList();
            	return true;
            }
            break;
        /**
         * Take picture physical button
         */
        case KeyEvent.KEYCODE_CAMERA:
        /**
         * Android 4.0+ Google Edition camera app like (take picture with volume button)
         */
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_UP:
        	return true;
        /**
         * Iterate through settings using menu button
         */
        case KeyEvent.KEYCODE_MENU:
        	if (isWaiting()) break;
        	if (isSensorsCorrectionOpen()){
        		hideSensors();
        		showPeaksList();
        	} else if (isPeaksListOpen()){
        		hidePeaksList();
        	} else {
        		showSensors();
        	}
        	return true;
        }
        return super.onKeyDown(keyCode, event);
    }

	@Override
	public void onOrientationUpdate(float[] angles) {
		mMountainOverlay.setAngles(angles);
	}

	@Override
	public void onFOVChange(float hFov, float vFov) {
		// Log.d("FOV:", "" + hFov + " " + vFov);
		mMountainOverlay.setFov(hFov, vFov);
	}
	
	private boolean isWaiting() {
		return mWaitView.getVisibility() == View.VISIBLE;
	}
	
	/*private void hideWait(){
		if (isWaiting())
		{
			mWaitView.setVisibility(View.INVISIBLE);
			ProgressBar bar = (ProgressBar)findViewById(R.id.wait_progress_bar);
			bar.setIndeterminate(false); // try to reduce computing power
			ToggleButton tb = (ToggleButton)findViewById(R.id.sensors_correction_toogle_button);
			tb.setEnabled(true);
			tb = (ToggleButton)findViewById(R.id.peaks_selection_toogle_button);
			tb.setEnabled(true);
			tb = (ToggleButton)findViewById(R.id.location_update_toogle_button);
			tb.setEnabled(true);
			Button bt = (Button)findViewById(R.id.take_picture);
			bt.setEnabled(true);
		}
	}*/
	
	/*private void openWait(int resId){
		if (!isWaiting()){
			mWaitView.setVisibility(View.VISIBLE);
			ProgressBar bar = (ProgressBar)findViewById(R.id.wait_progress_bar);
			bar.setIndeterminate(true);
			ToggleButton tb = (ToggleButton)findViewById(R.id.sensors_correction_toogle_button);
			tb.setEnabled(false);
			tb = (ToggleButton)findViewById(R.id.peaks_selection_toogle_button);
			tb.setEnabled(false);
			tb = (ToggleButton)findViewById(R.id.location_update_toogle_button);
			tb.setEnabled(false);
			Button bt = (Button)findViewById(R.id.take_picture);
			bt.setEnabled(false);
		}
		TextView text = (TextView)findViewById(R.id.wait_text);
		text.setText(resId);
	}*/

	private boolean isPeaksListOpen() {
		final ToggleButton button = (ToggleButton) findViewById(R.id.peaks_selection_toogle_button);
		return button.isChecked();
	}

	private boolean isSensorsCorrectionOpen() {
		final ToggleButton button = (ToggleButton) findViewById(R.id.sensors_correction_toogle_button);
		return button.isChecked();
	}
	
	public void onSensorsCorrectionClicked(View v) {
		if (v.getId() != R.id.sensors_correction_toogle_button)
			return;
		final ToggleButton button = (ToggleButton) v;

		// mMountainOverlay.setSensorsVisibility(button.isChecked());
		if (button.isChecked()) {
			if (isWaiting()) {
				button.setChecked(false);
			} else {
				hidePeaksList();
				showSensors();
			}
		} else {
			hideSensors();
		}
	}

	public void onDefaultCorrectionsClicked(View v) {
		if (v.getId() != R.id.default_correction)
			return;
		mMountainOverlay.resetCorrections();
		updateSeekBars();
	}

	private void hideSensors() {
		mSensorsView.setVisibility(View.INVISIBLE);
		final ToggleButton tb = (ToggleButton)findViewById(R.id.sensors_correction_toogle_button);
		tb.setChecked(false);
	}

	private void showSensors() {
		mSensorsView.setVisibility(View.VISIBLE);
		updateSeekBars();
		final ToggleButton tb = (ToggleButton)findViewById(R.id.sensors_correction_toogle_button);
		tb.setChecked(true);
	}

	private void updateSeekBars() {
		// 4 because +-27.5° are the maximum 0.125° step
		mAzimuthSeekBar.setProgress(180 + (int) (mMountainOverlay
				.getAzimuthCorrection() * 8f));
		mPitchSeekBar.setProgress(180 + (int) (mMountainOverlay
				.getPitchCorrection() * 8f));
		mRollSeekBar.setProgress(180 + (int) (mMountainOverlay
				.getRollCorrection() * 8f));
	}

	public void onPeakSelectionClicked(View v) {
		if (v.getId() != R.id.peaks_selection_toogle_button)
			return;
		final ToggleButton button = (ToggleButton) v;

		// mMountainOverlay.setSensorsVisibility(button.isChecked());
		if (button.isChecked()) {
			if (isWaiting()) {
				button.setChecked(false);
			} else {
				hideSensors();
				showPeaksList();
			}
		} else {
			hidePeaksList();
		}
	}

	public void onAllPeaksSelection(View v) {
		if (v.getId() != R.id.all_peaks)
			return;
		for (Mountain m : mMountainOverlay) {
			m.setSelected(true);
		}
		final ListView view = (ListView) findViewById(R.id.peaks_list_view);
		((MountainAdapter) view.getAdapter()).notifyDataSetChanged();
	}

	public void onDefaultPeaksSelection(View v) {
		if (v.getId() != R.id.default_peaks)
			return;
		for (Mountain m : mMountainOverlay) {
			m.setSelected(true);
		}
		mMountainOverlay.setDefaultSelecton();
		final ListView view = (ListView) findViewById(R.id.peaks_list_view);
		((MountainAdapter) view.getAdapter()).notifyDataSetChanged();
	}

	public void onNonePeaksSelection(View v) {
		if (v.getId() != R.id.no_peaks)
			return;
		for (Mountain m : mMountainOverlay) {
			m.setSelected(false);
		}
		final ListView view = (ListView) findViewById(R.id.peaks_list_view);
		((MountainAdapter) view.getAdapter()).notifyDataSetChanged();
	}

	private void hidePeaksList() {
		mPeaksView.setVisibility(View.INVISIBLE);
		final ListView view = (ListView) findViewById(R.id.peaks_list_view);
		view.setAdapter(null);
		final ToggleButton tb = (ToggleButton)findViewById(R.id.peaks_selection_toogle_button);
		tb.setChecked(false);
	}

	private void showPeaksList() {
		mPeaksView.setVisibility(View.VISIBLE);
		MountainAdapter adapter = new MountainAdapter(this,
				R.layout.mountain_list_view, mMountainOverlay);
		final ListView view = (ListView) findViewById(R.id.peaks_list_view);
		view.setAdapter(adapter);
		final ToggleButton tb = (ToggleButton)findViewById(R.id.peaks_selection_toogle_button);
		tb.setChecked(true);
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

	@Override
	protected void onResume() {
		super.onResume();
		Log.d("SnowWatch", "On Resume");
		resume();
	}

	private void resume() {
		Log.d("SnowWatch", "Resume");
		mOrientationManager.resume();
		//isPaused = false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d("SnowWatch", "On Pause");
		pause();
		//isPaused = true;
	}

	private void pause() {
		Log.d("SnowWatch", "Pause");
		mOrientationManager.pause();
	}

	@Override
	public void onCameraError() {
		finish();
	}

	@Override
	public void onCameraCalibration(byte[] frame) {
	}
}
