package it.polimi.models;

import java.io.Serializable;

public class ImageMarker extends SquareMarker{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final String name;
	public final double altitude;
	
	public ImageMarker(final Coordinate center, final double altitude, final String name) {
		super(center);
		this.name = name;
		this.altitude = altitude;
	}
}
