package it.polimi.snowwatch;

import it.trilogis.android.ww.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * This class manages the rendering of the live camera view
 * @author B3rn475
 *
 */
public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback,
		Camera.PreviewCallback {
	private static final String TAG = "SnowWatch - CameraPreview";
	private final SurfaceHolder mHolder;
	private Camera mCamera;
	private OnPreviewEventListener mListener = null;
	private boolean mCalibrationOnNextFrame = false;

	private float mVFov = 60;
	
	/**
	 * Get the current Vertical Field Of View
	 * @return
	 */
	public float getVFov(){
		return mVFov;
	}
	
	/** A safe way to get an instance of the Camera object. */
	private static Camera getCameraInstance() {
		Camera c = null;
		try {
			Log.d("Bella", Integer.toString(Camera.getNumberOfCameras()));
			if (Camera.getNumberOfCameras() == 0) return null;
			for (int i=0; i < Camera.getNumberOfCameras(); i++){
				Camera.CameraInfo info = new Camera.CameraInfo();
				Camera.getCameraInfo(i, info);
				if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) continue;
				c = Camera.open(i); // attempt to get a Camera instance
				break;
			}
			return c;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		finally{
			if (c == null){
				Log.d(TAG, "Camera Not Available");
			}
		}
	}
	
	/**
	 * Instantate a new CameraPreview
	 * @param context
	 */
	public CameraPreview(final Context context) {
		super(context);

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	/**
	 * The surface is created so i instantiate the Camera and connect it
	 */
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("SnowWatch", "Surface Created");
		// The Surface has been created, now tell the camera where to draw the
		// preview.
		try {
			mCamera = getCameraInstance();
			if (mCamera == null)
			{
				Toast.makeText(getContext(), getContext().getString(R.string.no_camera_available),
					Toast.LENGTH_SHORT).show();
				if (mListener != null){
					mListener.onCameraError();
				}
			} else {
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			}
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	/**
	 * Surface destroyed Release the camera
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	/**
	 * Compute the best size from sizes that respect the w and h
	 * @param sizes List of Sizes
	 * @param w Width
	 * @param h Height
	 * @return best Size
	 */
	private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
	
	/**
	 * Surface is changed (rotation)
	 */
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {	
		Log.d(TAG, "Surface Changed");
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here
		Camera.CameraInfo info = new Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(0, info);
		int rotation = ((WindowManager) (this.getContext()
				.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		Log.d(TAG, "Rotation " + rotation);
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		mCamera.setDisplayOrientation(result); // preview rotation

		Camera.Parameters parameters = mCamera.getParameters();
        List<Size> sizes = parameters.getSupportedPreviewSizes();
        Size optimalSize;
        if (result % 180 == 0)
		{
        	optimalSize = getOptimalPreviewSize(sizes, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
		} else {
			optimalSize = getOptimalPreviewSize(sizes, getResources().getDisplayMetrics().heightPixels, getResources().getDisplayMetrics().widthPixels);
		}
        parameters.setRotation(result); //image rotation
        
        Log.d("Aspect:", "" + optimalSize.width + " " + optimalSize.height);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        mCamera.setParameters(parameters);
		
        final float hFov = mCamera.getParameters().getHorizontalViewAngle();
        final float vFov = (float) (2f * Math.toDegrees(Math.atan((((float)optimalSize.height) / ((float)optimalSize.width))*Math.tan(Math.toRadians(hFov) / 2f))));
        
		if (mListener != null) {
			if (result % 180 == 0)
			{
				mVFov = vFov;
				mListener.onFOVChange(hFov, vFov);
			} else {
				mVFov = hFov;
				mListener.onFOVChange(vFov, hFov);
			}
		}

		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();

		} catch (Exception e) {
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}
	
	/**
	 * Called from external code that require the next Camera Frame
	 * Asynchronous
	 */
	public void requestCalibration(){
		if (mCalibrationOnNextFrame) return;
		if (mCamera == null) return;
		mCalibrationOnNextFrame = true;
		mCamera.setOneShotPreviewCallback(this);
	}

	/***
	 * When requested here comes the Camera Frame
	 */
	@Override
	public void onPreviewFrame(final byte[] data, final Camera camera) {
		mCalibrationOnNextFrame = false;
		/**
		 * If there is no camera the calibration is useless
		 */
		if (mListener != null && mCamera != null){
			Camera.Parameters params = camera.getParameters();
			YuvImage image = new YuvImage(data, params.getPreviewFormat(), params.getPreviewSize().width, params.getPreviewSize().height,null);
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			image.compressToJpeg(new Rect(0, 0, params.getPreviewSize().width, params.getPreviewSize().height), 90, stream);
			mListener.onCameraCalibration(stream.toByteArray());
		}
	}
	
	/**
	 * Interface for listeners on CameraPreview events
	 * @author B3rn475
	 *
	 */
	public interface OnPreviewEventListener {
		public void onFOVChange(float hFov, float vFov);
		public void onCameraCalibration(byte[] frame);
		public void onCameraError();
	}
	
	/**
	 * Set the current listener
	 * @param listener
	 */
	public void setPreviewEventListener(OnPreviewEventListener listener){
		mListener = listener;
	}

	/**
	 * get the current active camera
	 * @return
	 */
	public Camera getCamera() {
		return mCamera;
	}
}
