package it.polimi.snowwatch;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

/**
 * Class that manages the Location Sensors (GPS)
 * @author B3rn475
 *
 */
public class LocationManager implements LocationListener {
	private final Context mContext;
	private final android.location.LocationManager mLocationManager;
	
	private static final int mInterval = 60000; // 1 min
	private static final int mMinDistance = 20; //meters
	
	private OnLocationEventListener mListener = null;
	
	private Double mLastLatitude = null;
	private Double mLastLongitude = null;
	private Double mLastAltitude = null;
	
	/**
	 * New Location manager
	 * @param context
	 */
	public LocationManager(final Context context){
    	mContext = context;

    	mLocationManager = (android.location.LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
    	resume();
    }
	
	/**
	 * Current Latitude
	 * @return null if not available
	 */
	public Double getLatitude(){
		return mLastLatitude;
	}
	
	/**
	 * Current Longitude
	 * @return null if not available
	 */
	public Double getLongitude(){
		return mLastLongitude;
	}
	
	/**
	 * Current Altitude
	 * @return null if not available
	 */
	public Double getAltitude(){
		return mLastAltitude;
	}
			
	/**
	 * GPS status changed
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d("SnowWatch", provider + " GPS " + status);
	}
	
	/**
	 * Provider enabled (check GPS)
	 */
	@Override
	public void onProviderEnabled(String provider) {
		if (mListener != null){
			mListener.onGPSStatusUpdate(OnLocationEventListener.GPS_ON);
		}
	}
	
	/**
	 * Provider Disabled (check GPS)
	 */
	@Override
	public void onProviderDisabled(String provider) {
		if (mListener != null){
			mListener.onGPSStatusUpdate(OnLocationEventListener.GPS_OFF);
		}
	}
	
	/**
	 * New Location
	 */
	@Override
	public void onLocationChanged(Location location) {
		mLastLatitude = location.getLatitude();
		mLastLongitude = location.getLongitude();
		if (location.hasAltitude()){
			mLastAltitude = location.getAltitude();
		} else {
			mLastAltitude = null;
		}
		if (mListener != null){
			mListener.onLocationUpdate(mLastLatitude, mLastLongitude, mLastAltitude);
		}
	}
	
	/**
	 * Class for the management of Location Callbacks
	 * @author B3rn475
	 *
	 */
	public interface OnLocationEventListener{
		/**
		 *  New location available
		 * @param latitude Latitude angle of the current location
		 * @param longitude Longitude angle of the current location
		 * @param altitude Altitude of the current location (May be null if this is not available)
		 */
		public void onLocationUpdate(double latitude, double longitude, Double altitude);
		public void onGPSStatusUpdate(int status);
		
		public static final int GPS_ON = 1;
		public static final int GPS_OFF = 0;
	}

	/**
	 * Set the current event listener
	 * @param onLocationUpdateEventListener
	 */
	public void setLocationUpdateEventListener(OnLocationEventListener listener) {
		mListener=listener;
	}
	
	/**
	 * Pause the location process
	 */
	public void pause(){
		mLocationManager.removeUpdates(this);
	}
	
	/**
	 * Resume the location process
	 */
	public void resume(){
		mLocationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER,
                mInterval,
                mMinDistance, 
                this);
	}

	/**
	 * Are coodinates available
	 * @return
	 */
	public boolean hasCoordinates() {
		return mLastAltitude != null && mLastLatitude != null;
	}
}
