package it.polimi.snowwatch;

import it.polimi.snowwatch.utils.Vector3;

/**
 * Mountain descriptor
 * @author b3rn475
 *
 */
public class Mountain {
	/**
	 * Name of the Mountain (is not editable)
	 */
	public final String name;
	/**
	 * Distance from the observer in Km (is not editable)
	 */
	public final float distance;
	/**
	 * Given Location of the Mountain in the 3D environment (is not editable)
	 */
	public final Vector3 location;
	
	private Vector3 mCorrectedLocation;
	private String label;
	private boolean mVisible = true;
	private boolean mSelected = true;

	/**
	 * Instantiate a new mountain
	 * @param name
	 * @param height
	 * @param location
	 * @param visible
	 */
	public Mountain(String name, float distance, Vector3 location){
		this.name = name;
		this.distance = distance;
		this.location = location;
		setCorrectedLocation(location);
		label = name + " (" + Double.toString(Math.floor(distance)) + "m)";
	}
	
	/**
	 * Corrected location after fine grained algorithms 
	 * @return
	 */
	public Vector3 getCorrectedLocation(){
		synchronized(this){
			return mCorrectedLocation;
		}
	}
	
	/**
	 * This is just a placeholder that allow to introduce label formatters
	 * @return
	 */
	public String getLabel(){
		// TODO : add label formatters
		return label;
	}
	
	/**
	 * The mountain is currently visible (estimated by image processing algorithms)
	 * @return
	 */
	public boolean isVisible(){
		synchronized(this){
			return mVisible;
		}
	}
	
	/**
	 * Set the visibility of the Mountain
	 * @param visible
	 */
	public void setVisibility(boolean visible){
		synchronized(this){
			mVisible = visible;
		}
	}
	
	/**
	 * The mountain has been selected by the user (and so be rendered)
	 * @return
	 */
	public boolean isSelected(){
		return mSelected;
	}
	
	/**
	 * Set the selection of the Mountain
	 * @param selected
	 */
	public void setSelected(boolean selected){
		mSelected = selected;
	}

	/**
	 * Set the corrected location
	 * @param vector3
	 */
	public void setCorrectedLocation(Vector3 vector3) {
		synchronized(this){
			mCorrectedLocation = vector3;
		}
	}
}
