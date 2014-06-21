package it.polimi.snowwatch;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Class that manages the Orientation Sensors
 * @author B3rn475
 *
 */
public class OrientationManager implements SensorEventListener {

	private final Context mContext;
	private OnUpdateEventListener mListener = null;
	
    private final SensorManager mSensMan;
    private final float[] mRotationVector = new float[3];
    private final float[] mOrientation = new float[3];
    private final float[] mRotationM = new float[9];
    
    public OrientationManager(Context context){
    	mContext = context;
    	mSensMan = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
    	resume();
    }
    
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub (this is needed?)
	}

	/**
	 * New sensors reading
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		/**
		 * Old data copy 
		 for (int i=0; i<3; i++){
			mRotationVector[i] = event.values[i];
		}*/
		System.arraycopy(event.values,0,mRotationVector,0,3);
		//Maybe there is a better solution, but I'll search it later
		final int rotation = ((WindowManager) (mContext
				.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay()
				.getRotation();
		
		final float tmp = mRotationVector[0]; //x
		switch (rotation) {
			case Surface.ROTATION_0:
				mRotationVector[0] = mRotationVector[1]; //y
				mRotationVector[1] = -tmp;  //-x
				break;
			case Surface.ROTATION_90:
				break;
			case Surface.ROTATION_180:
				mRotationVector[0] = -mRotationVector[1]; //-y
				mRotationVector[1] = tmp; //x
				break;
			case Surface.ROTATION_270:
				mRotationVector[0] = -mRotationVector[0]; //-x
				mRotationVector[1] = -mRotationVector[1]; //-y
				break;
		}
		
		SensorManager.getRotationMatrixFromVector(mRotationM, mRotationVector);
		
		SensorManager.getOrientation(mRotationM, mOrientation);
		
		switch (rotation) {
			case Surface.ROTATION_0:
				mOrientation[0] += (float) Math.PI / 2f;
				mOrientation[2] -= (float) Math.PI / 2f;
				break;
			case Surface.ROTATION_90:
				mOrientation[0] += (float) Math.PI;
				mOrientation[2] -= (float) Math.PI / 2f;
				break;
			case Surface.ROTATION_180:
				mOrientation[0] -= (float) Math.PI / 2f;
				mOrientation[2] -= (float) Math.PI / 2f;
				break;
			case Surface.ROTATION_270:
				mOrientation[2] -= (float) Math.PI / 2f;
				break;
		}
		for (int i=0; i<3; i++){
			while (mOrientation[i] > Math.PI){
				mOrientation[i] -= 2f * Math.PI;
			}
			while (mOrientation[i] < -Math.PI){
				mOrientation[i] += 2f * Math.PI;
			}
		}
		if(mListener!=null) mListener.onOrientationUpdate(mOrientation);
	}

	/**
	 * Listener for orientation changes
	 * @author b3rn475
	 *
	 */
	public interface OnUpdateEventListener{
		public void onOrientationUpdate(float[] orientation);
	}

	/**
	 * Set the listener
	 * @param onUpdateEventListener
	 */
	public void setUpdateEventListener(OnUpdateEventListener onUpdateEventListener) {
		mListener=onUpdateEventListener;
	}

	/**
	 * Resume updates
	 */
	public void resume() {
		mSensMan.registerListener(this,
				mSensMan.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), 
				SensorManager.SENSOR_DELAY_GAME);
	}
	
	/**
	 * Pause updated
	 */
	public void pause() {
		mSensMan.unregisterListener(this);
	}
}