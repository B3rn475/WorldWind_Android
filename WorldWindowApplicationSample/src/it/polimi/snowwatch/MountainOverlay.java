package it.polimi.snowwatch;

import it.polimi.snowwatch.utils.Matrix;
import it.polimi.snowwatch.utils.Vector3;
import it.polimi.snowwatch.utils.Vector4;
import it.trilogis.android.ww.R;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

/**
 * View that draws on the screen the mountains inserted into it, taking in account the sensors readings
 * @author b3rn475
 *
 */
public class MountainOverlay extends View implements List<Mountain> {
	private final Paint mTextPaint;
	private final Paint mCompassPaint;
	private final Paint mBackgroundPaint;
	private final float mTextScaling = 0.02f; //3% of the width
	
	
	private static final int mSensorsArraySize = 5;
	private final float[] mAzimuth = new float[mSensorsArraySize];
	private final float[] mPitch = new float[mSensorsArraySize];
	private final float[] mRoll = new float[mSensorsArraySize];
	private int mIndex = 0;
	
	private float mAzimuthCorrection = 0.0f;
	private float mPitchCorrection = 0.0f;
	private float mRollCorrection = 0.0f;
	
	private Float mComputedAzimuth = null;
	private Float mComputedPitch = null;
	private Float mComputedRoll = null;
	
	private float mHFov = 60;
	private float mVFov = 60;
	private boolean mShowSensors = false;
	
	private final List<Mountain> mMountains;
	private final Bitmap mMountainVisibleBMP;
	private final Bitmap mMountainHiddenBMP;
	
	private static final String[] mDirections = new String[]{ "N", "NNW", "NW", "WNW", "W", "WSE", "SW", "SSW", "S", "SSE", "SE", "ESE", "E", "ENE", "NE", "NNE"};
	private static final int mCompassMainDivisions = mDirections.length;
	private static final int mCompassSubDivision = 3;
	private static final int mCompassTotalDivisions = (int) (mCompassMainDivisions * Math.pow(2, mCompassSubDivision));
	private static final int mCompassLineHeight = 24;
	private static final float mLabelRotation = -25.0f;
	private static final float mVisibleAreaX = 0.75f;
	private static final float mVisibleAreaY = 0.75f;
	private static final int mMaxPeaksDefaultSelected = 36; // max 1 every 10°
	private static final int mJpegQuality = 90; // min 0 max 100
	
	private boolean mLocationUpdateActive = false;
	
	private static final String mAzimuthCorretionString = "AzimuthCorrection";
	private static final String mPitchCorretionString = "PitchCorrection";
	private static final String mRollCorretionString = "RollCorrection";
	
	private final SharedPreferences mPreferences;
	
	/**
	 * The location used is the one computer or the one corrected
	 */
	public boolean isLocationUpdateActive(){
		return mLocationUpdateActive;
	}
	
	/**
	 * Set if the location used is the one computer or the one corrected
	 * @param active
	 */
	public void setIsLocationUpdateActive(boolean active){
		mLocationUpdateActive = active;
	}
	
	/**
	 * Set the horizontal FOV
	 * This is needed for the projection matrix used during computations
	 * @param angle
	 */
	public void setFov(final float hFov, final float vFov){
		mHFov = hFov;
		mVFov = hFov;
		this.invalidate();
	}
	
	/**
	 * Get the user defined Azimuth correction
	 * @return
	 */
	public float getAzimuthCorrection(){
		return mAzimuthCorrection;
	}
	
	/**
	 * Get the user defined Pitch correction
	 * @return
	 */
	public float getPitchCorrection(){
		return mPitchCorrection;
	}
	
	/**
	 * Get the user defined Roll correction
	 * @return
	 */
	public float getRollCorrection(){
		return mRollCorrection;
	}
	
	/**
	 * Set the user defined Azimuth correction
	 * @param correction
	 */
	public void setAzimuthCorrection(final float correction){
		mAzimuthCorrection = correction;
		this.invalidate();
	}
	
	/**
	 * Set the user defined Pitch correction
	 * @param correction
	 */
	public void setPitchCorrection(final float correction){
		mPitchCorrection = correction;
		this.invalidate();
	}
	
	/**
	 * Set the user defined Roll correction
	 * @param correction
	 */
	public void setRollCorrection(final float correction){
		mRollCorrection = correction;
		this.invalidate();
	}
	
	/**
	 * Save the user defined corrections
	 */
	public void saveSettings() {
		Editor ed = mPreferences.edit();
		ed.putFloat(mRollCorretionString, mRollCorrection);
		ed.putFloat(mPitchCorretionString, mPitchCorrection);
		ed.putFloat(mAzimuthCorretionString, mAzimuthCorrection);
		ed.commit();
	}	
	
	/**
	 * Reset the correction to default values
	 */
	public void resetCorrections(){
		mAzimuthCorrection = 0.0f;
		mPitchCorrection = 0.0f;
		mRollCorrection = 0.0f;
	}
	
	/**
	 * Activate the visibility of the sensors data in the middle of the screen.
	 * This is main for debug purposes
	 * @param visible
	 */
	public void setSensorsVisibility(final boolean visible){
		mShowSensors = visible;
	}

	/**
	 * Insantiate a new MountainOverlay
	 * @param context
	 */
	public MountainOverlay(final Context context) {
		super(context);
		
		mPreferences = context.getSharedPreferences("OrientationManager", Context.MODE_PRIVATE);
		
		mAzimuthCorrection = mPreferences.getFloat(mAzimuthCorretionString, 0.0f);
		mPitchCorrection = mPreferences.getFloat(mPitchCorretionString, 0.0f);
		mRollCorrection = mPreferences.getFloat(mRollCorretionString, 0.0f);
		
		/**
		 * Init resources
		 */
		
		Paint tPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		tPaint.setColor(Color.BLUE);
		tPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		tPaint.setTextSize(30);
		mCompassPaint = tPaint;
		
		tPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		tPaint.setColor(Color.BLACK);
		tPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		tPaint.setTextSize(30);
		
		mTextPaint = tPaint;
		
		tPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		tPaint.setColor(Color.WHITE);
		tPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		tPaint.setTextSize(30);
		
		mBackgroundPaint = tPaint;
		
		final Resources res = getResources();
		
		mMountainVisibleBMP = BitmapFactory.decodeResource(res, R.drawable.mountain);
		mMountainHiddenBMP = BitmapFactory.decodeResource(res, R.drawable.mountain_hidden);
		
		/**
		 * Init internal objects
		 */
		
		mMountains = new ArrayList<Mountain>();
	}
	
	/**
	 * Add a new sensors reading to update the view.
	 * @param angles
	 */
	public void setAngles(final float[] angles){
		mAzimuth[mIndex] = (float) Math.toDegrees(angles[0]);
		mPitch[mIndex] = (float) Math.toDegrees(angles[2]);
		mRoll[mIndex] = (float) Math.toDegrees(angles[1]);
		mIndex = (mIndex + 1) % mAzimuth.length;

		//Computation of the actual values (smoothing) is demanded to the getters
		mComputedAzimuth = null;
		mComputedPitch = null;
		mComputedRoll = null;
		
		this.invalidate();
	}
	
	/**
	 * Computes the median of the azimuth of the last N readings
	 * @return the current Azimuth
	 */
	private float getUncorrectedAzimuth() {
		//check if already computed
		if (mComputedAzimuth == null)
		{
			float[] sorted = mAzimuth.clone();
			Arrays.sort(sorted);
			mComputedAzimuth = sorted[mAzimuth.length / 2];
		}
		return mComputedAzimuth;
	}
	
	/**
	 * Computes the corrected azimuth
	 * @return the current Azimuth
	 */
	private float getAzimuth() {
		
		return getUncorrectedAzimuth() + mAzimuthCorrection;
	}
	
	/**
	 * Computes the median of the pitch of the last N readings
	 * @return the current Pitch
	 */
	private float getUncorrectedPitch() {
		//check if already computed
		if (mComputedPitch == null)
		{
			float[] sorted = mPitch.clone();
			Arrays.sort(sorted);
			mComputedPitch = sorted[mPitch.length / 2];
		}
		return mComputedPitch;
	}
	
	/**
	 * Computes the corrected Pitch
	 * @return the current Pitch
	 */
	private float getPitch() {
		return getUncorrectedPitch() + mPitchCorrection;
	}
	
	/**
	 * Computes the median of the roll of the last N readings
	 * @return the current Roll
	 */
	private float getUncorrectedRoll() {
		//check if already computed
		if (mComputedRoll == null)
		{
			float[] sorted = mRoll.clone();
			Arrays.sort(sorted);
			mComputedRoll = sorted[mRoll.length / 2];
		}
		return mComputedRoll;
	}
	
	/**
	 * Computes the corrected Roll
	 * @return the current Roll
	 */
	private float getRoll() {
		return getUncorrectedRoll() + mRollCorrection;
	}
	
	public String getSensorsString() {
		return getSensorsString(getMemento());
	}
	
	public static String getSensorsString(final Memento memento) {
		return "SnowWatch:(" + angleTo6String(memento.azimuth) + "," + angleTo6String(memento.pitch) + "," + angleTo6String(memento.roll) + ")";
	}
	
	private static String angleTo6String(float f){
		f = to360(f);
		int multiplier = 100;
		String ret = Float.toString(f);
		if (ret.length() == 6) return ret;
		do{
			ret = Integer.toString((int)(f*multiplier));
			multiplier *= 10;
		} while (ret.length() < 5);
		return ret.substring(0,3) + "." + ret.substring(3);
	}
	
	private static float to360(float f){
		while (f < 0){
			f += 360f;
		}
		while (f > 360){
			f -= 360f;
		}
		return f;
	}
	
	private static float to180(float f){
		while (f < -180){
			f += 360f;
		}
		while (f > 180){
			f -= 360f;
		}
		return f;
	}
	
	
	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
	}
	
	/**
	 * Draw the current sensors reading
	 */
	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		onDrawInternal(canvas, getMemento());
	}
	
	/**
	 * OnDraw not related to current sensors readings
	 * @param canvas
	 * @param memento Memento of the readings to use
	 */
	private void onDrawInternal(final Canvas canvas, final Memento memento){		
		drawCompass(canvas, memento);
		drawMountains(canvas, memento);
		if (mShowSensors){
			drawAngles(canvas, memento);
		}
	}

	/**
	 * Draws the current sensors data (Mainly for debug purposes)
	 * @param canvas
	 */
	private void drawAngles(final Canvas canvas, final Memento memento) {
		final float width = canvas.getWidth(); 
		final float height = canvas.getHeight();
		final float hWidth = width / 2f; 
		final float hHeight = height / 2f;
		canvas.translate(hWidth, hHeight);
		
		final float azimuth = memento.azimuth;
		final float pitch = memento.pitch;
		final float roll = memento.roll;
		
		if (azimuth > 0)
			canvas.drawRect(-hWidth/2-10, 0,-hWidth/2+10, azimuth / 180f * hHeight, mCompassPaint);
		else
			canvas.drawRect(-hWidth/2-10, azimuth / 180.0f * hHeight,-hWidth/2+10, 0, mCompassPaint);
		canvas.drawText("azimuth", -hWidth/2+12, 0, mCompassPaint);
		if (pitch > 0)
			canvas.drawRect(-10, 0,10, pitch / 180f * hHeight, mCompassPaint);
		else
			canvas.drawRect(-10, pitch / 180f * hHeight,+10, 0, mCompassPaint);
		canvas.drawText("pitch", 12, 0, mCompassPaint);
		float cRoll = to180(roll - 180);
		if (cRoll > 0)
			canvas.drawRect(hWidth/2-10, 0,hWidth/2+10, cRoll / 180.0f * hHeight, mCompassPaint);
		else
			canvas.drawRect(hWidth/2-10, cRoll / 180f * hHeight,hWidth/2+10, 0, mCompassPaint);
		canvas.drawText("roll", hWidth/2+12, 0, mCompassPaint);
		
		canvas.translate(-hWidth, -hHeight);
	}

	/**
	 * Draws the mountains on the canvas based on the current sensors readings
	 * @param canvas
	 */
	private void drawMountains(final Canvas canvas, final Memento memento) {
		final float width = canvas.getWidth(); 
		final float height = canvas.getHeight();
		final float hWidth = width / 2f; 
		final float hHeight = height / 2f;
		canvas.translate(hWidth, hHeight);		
		final float BVWidth = mMountainVisibleBMP.getWidth();
		final float BVHeight = mMountainVisibleBMP.getHeight();
		final float hBVWidth = BVWidth / 2f;
		final float hBVHeight = BVHeight / 2f;
		final float BHWidth = mMountainHiddenBMP.getWidth();
		final float BHHeight = mMountainHiddenBMP.getHeight();
		final float hBHWidth = BHWidth / 2f;
		
		
		final float roll = memento.roll;
		
		final float TextSize = mTextPaint.getTextSize();
		final float hTextSize = TextSize / 2f;
		
		final float scaleFactor = Math.max(width, height) * mTextScaling / mTextPaint.getTextSize();
		final float iScaleFactor = 1 / scaleFactor;
		
		final float BVScaleFactor = 2 * mTextPaint.getTextSize() / BVHeight ;
		final float iBVScaleFactor = 1 / BVScaleFactor;
		
		final float BHScaleFactor = 2 * mTextPaint.getTextSize() / BHHeight ;
		final float iBHScaleFactor = 1 / BHScaleFactor;
		
		//Matrix view = Matrix.makeViewMatrix(Vector3.ZERO, getYaw(), getPitch(), getRoll());
		
		final Matrix PM = getTransformationMatrix(width, height, memento);
		
		final float totalRotationFix = - mLabelRotation - roll;
		
		for (Mountain mountain : mMountains){
			if (!mountain.isSelected()) continue;
			final Vector4 pPoint4;
			if(mLocationUpdateActive){
				pPoint4 = PM.multiply(mountain.getCorrectedLocation());
			} else {
				pPoint4 = PM.multiply(mountain.location);
			}
			/*
			 * Step by step operations (for debug)
			Vector3 ppPoint = view.multiply(point).toVector3();
			Vector4 pPoint4 = proj.multiply(ppPoint);
			if (!pPoint4.needClipping()){
			*/
			if (!pPoint4.needClipping()){
				final Vector3 pPoint = pPoint4.toVector3();
				final float x = pPoint.x*hWidth;
				final float y = pPoint.y*hHeight;
				canvas.translate(x, y);
				canvas.rotate(roll);
				canvas.scale(scaleFactor, scaleFactor);
				if (!mLocationUpdateActive || mountain.isVisible()){
					
					canvas.scale(BVScaleFactor, BVScaleFactor);
					canvas.drawBitmap(mMountainVisibleBMP, -hBVWidth, -hBVHeight, mBackgroundPaint);
					canvas.scale(iBVScaleFactor, iBVScaleFactor);
					//show label
					if (Math.abs(pPoint.x) < mVisibleAreaX && Math.abs(pPoint.y) < mVisibleAreaY){
						canvas.rotate(mLabelRotation);
						final String label = mountain.getLabel();
						final Rect bounds = new Rect();
						mTextPaint.getTextBounds(label, 0, label.length(), bounds);
						
						bounds.left += TextSize + 12;
						bounds.right += TextSize + 12;
						bounds.top += bounds.height() / 2.0f;
						bounds.bottom += bounds.height() / 2.0f;
						
						final Rect intBounds = new Rect(bounds);
						
						intBounds.left -= 5;
						intBounds.right += 5;
						intBounds.top -= 6;
						intBounds.bottom += 10;
						
						final Rect extBounds = new Rect(bounds);
						
						extBounds.left -= 7;
						extBounds.right += 7;
						extBounds.top -= 8;
						extBounds.bottom += 12;
						
						canvas.drawRect(extBounds, mTextPaint);
						canvas.drawRect(intBounds, mBackgroundPaint);
						canvas.drawText(label, bounds.left, bounds.bottom, mTextPaint);
						canvas.rotate(totalRotationFix);
					} else {
						canvas.rotate(-roll);
					}
				} else {
					canvas.scale(BHScaleFactor, BHScaleFactor);
					canvas.drawBitmap(mMountainHiddenBMP, -hBHWidth, -BHHeight, mBackgroundPaint);
					canvas.scale(iBHScaleFactor, iBHScaleFactor);
					canvas.rotate(-roll);
				}
				canvas.scale(iScaleFactor, iScaleFactor);
				canvas.translate(-x, -y);
			}
		}
		canvas.translate(-hWidth, -hHeight);
	}

	/**
	 * Get the current Transformation Matrix
	 * @param width
	 * @param height
	 * @return
	 */
	public Matrix getTransformationMatrix(final float width, final float height) {
		return getTransformationMatrix(width, height, getMemento());
	}
	
	/**
	 * Get the Transformation Matrix releated to a particular Memento
	 * @param width
	 * @param height
	 * @param memento Memento to use
	 * @return
	 */
	public static Matrix getTransformationMatrix(final float width, final float height, final Memento memento) {
		final Matrix view = getViewMatrix(memento);
		final Matrix projection = getProjectionMatrix(width, height, memento);
		return projection.multiply(view);
	}

	/**
	 * Get the current View Matrix
	 * @return
	 */
	public Matrix getViewMatrix() {
		return getViewMatrix(getMemento());
	}
	
	/**
	 * Get the View Matrix releated to a particular Memento
	 * @param memento Memento to use
	 * @return
	 */
	public static Matrix getViewMatrix(final Memento memento) {	
		return Matrix.makeViewMatrix(Vector3.ZERO, memento.azimuth, memento.pitch, memento.roll);
	}
	
	/**
	 * Get the current Projection Matrix
	 * @param width
	 * @param height
	 * @return
	 */
	public Matrix getProjectionMatrix(final float width, final float height) {
		return getProjectionMatrix(width, height, getMemento());
	}
	
	/**
	 * Get the Projection Matrix releated to a particular Memento
	 * @param width
	 * @param height
	 * @param memento Memento to use
	 * @return
	 */
	public static Matrix getProjectionMatrix(final float width, final float height, final Memento memento) {
		return Matrix.makePerspectiveProj(width, height, 0.01f, 10f, memento.vFov / 2f);
	}

	/**
	 * Draw the compass on the canvas based on the current azimuth angle
	 * @param canvas
	 */
	private void drawCompass(final Canvas canvas, final Memento memento) {
		final float width = canvas.getWidth(); 
		final float height = canvas.getHeight();
		final float hWidth = width / 2.0f; 
		
		canvas.drawRect(0, height-1, width, height, mCompassPaint);
		
		canvas.translate(hWidth, 0);
		
		final float azimuth = memento.azimuth - 90;
		
		//final float northPosition = -azimuth / mHFov * hWidth;
		final float multiplier = width / mHFov;
		final float stepD = 360.0f / mDirections.length;
		
		float angle = azimuth;
		for (String direction : mDirections){
		
			final float position = -angle * multiplier;
			
			canvas.drawText(direction, position - 11 * direction.length(), height - mCompassLineHeight - 8, mCompassPaint);
			angle += stepD;
			if (angle > 180.0f){
				angle -= 360.0f;
			}
		}
		
		final float stepSD = 360.0f / mCompassTotalDivisions;
		
		angle = azimuth;
		for (int i=0; i< mCompassTotalDivisions; i++){
			final float position = -angle * multiplier;
			int divider = 1;
			for (int k = mCompassSubDivision; k > 0; k--){
				if (i % (int)Math.pow(2, k) == 0){
					break;
				} else {
					divider++;
				}
			}
			final float lHeight = mCompassLineHeight / divider;
			canvas.drawRect(position - 2, height - lHeight - 2, position + 2, height, mCompassPaint);
			angle += stepSD;
			if (angle > 180.0f){
				angle -= 360.0f;
			}
		}
		
		canvas.translate(-hWidth, 0);
	}

	/**
	 * Reset the selected pitch to the dafault ones
	 */
	public void setDefaultSelecton(){
		
		final float minAngle = (float) Math.PI / mMaxPeaksDefaultSelected * 2;
		
		for (Mountain m : this){
			m.setSelected(true);
		}
		
		Comparator<Mountain> heightComparer = new Comparator<Mountain>() {
			
			@Override
			public int compare(Mountain lhs, Mountain rhs) {
				// TODO Auto-generated method stub
				return Double.compare(lhs.distance, rhs.distance); // from the nearest
			}
		};
		
		Collections.sort(mMountains, heightComparer);
		
		List<Mountain> needToBeAnalized = new LinkedList<Mountain>(mMountains);
		ArrayList<Mountain> toBeRemoved = new ArrayList<Mountain>();
		
		while(needToBeAnalized.size() > 0) //still mountains that needs to be analized
		{
			Mountain highest = needToBeAnalized.get(0); // it is sorted
			needToBeAnalized.remove(0);
			final float hAngle = (float) Math.atan2(highest.location.x, highest.location.z);
			
			toBeRemoved.clear();
			
			for (Mountain m : needToBeAnalized){
				final float mAngle = (float) Math.atan2(m.location.x, m.location.z);
				if (Math.abs(hAngle - mAngle) <= minAngle){
					m.setSelected(false);
					toBeRemoved.add(m);
				}
			}
			
			needToBeAnalized.removeAll(toBeRemoved);
		}
	}
	
	/**
	 * Get the Memento of the sensors
	 * @return
	 */
	public Memento getMemento() {
		return new Memento(getAzimuth(), getPitch(), getRoll(), mVFov);
	}
	
	/**
	 * Draw on a Jpeg istead of a Canvas the current situation
	 * @param stream
	 * @param data
	 */
	public void drawOnJpeg(OutputStream stream, byte[] data){
		drawOnJpeg(stream, data, getMemento());
	}

	/**
	 * Draw on a Jpeg based on a particular Memento
	 * @param stream
	 * @param data
	 * @param memento
	 */
	public void drawOnJpeg(OutputStream stream, byte[] data, Memento memento) {
		Bitmap img = BitmapFactory.decodeByteArray(data, 0, data.length);
		Bitmap mutableBitmap = img.copy(Bitmap.Config.ARGB_8888, true);
		img.recycle();
		
		Canvas canvas = new Canvas(mutableBitmap);
		
		onDrawInternal(canvas, memento);
		
		mutableBitmap.compress(CompressFormat.JPEG, mJpegQuality, stream);
		mutableBitmap.recycle();
	}
	
	/**
	 * Memento of the Sensors status
	 * @author B3rn475
	 *
	 */
	public class Memento {
		public final float azimuth;
		public final float pitch;
		public final float roll;
		public final float vFov;
		
		protected Memento(float azimuth, float pitch, float roll, float vFov){
			this.azimuth = azimuth;
			this.pitch = pitch;
			this.roll = roll;
			this.vFov = vFov;
		}
	}
	
	/* List<Mountains> Wrappers area */
	
	@Override
	public boolean add(Mountain object) {
		return mMountains.add(object);
	}

	@Override
	public void add(int location, Mountain object) {
		mMountains.add(location, object);
	}

	@Override
	public boolean addAll(Collection<? extends Mountain> arg0) {
		return mMountains.addAll(arg0);
	}

	@Override
	public boolean addAll(int arg0, Collection<? extends Mountain> arg1) {
		return mMountains.addAll(arg0, arg1);
	}

	@Override
	public void clear() {
		mMountains.clear();
	}

	@Override
	public boolean contains(Object object) {
		return mMountains.contains(object);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		return mMountains.containsAll(arg0);
	}

	@Override
	public Mountain get(int location) {
		return mMountains.get(location);
	}

	@Override
	public int indexOf(Object object) {
		return mMountains.indexOf(object);
	}

	@Override
	public boolean isEmpty() {
		return mMountains.isEmpty();
	}

	@Override
	public Iterator<Mountain> iterator() {
		return mMountains.iterator();
	}

	@Override
	public int lastIndexOf(Object object) {
		return mMountains.lastIndexOf(object);
	}

	@Override
	public ListIterator<Mountain> listIterator() {
		return mMountains.listIterator();
	}

	@Override
	public ListIterator<Mountain> listIterator(int location) {
		return mMountains.listIterator(location);
	}

	@Override
	public Mountain remove(int location) {
		return mMountains.remove(location);
	}

	@Override
	public boolean remove(Object object) {
		return mMountains.remove(object);
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		return mMountains.removeAll(arg0);
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		return mMountains.retainAll(arg0);
	}

	@Override
	public Mountain set(int location, Mountain object) {
		return mMountains.set(location, object);
	}

	@Override
	public int size() {
		return mMountains.size();
	}

	@Override
	public List<Mountain> subList(int start, int end) {
		return mMountains.subList(start, end);
	}

	@Override
	public Object[] toArray() {
		return mMountains.toArray();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return mMountains.toArray(array);
	}
}
