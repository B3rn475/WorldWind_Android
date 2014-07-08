package it.polimi.models;

import java.io.Serializable;

public class ImageMarker extends SquareMarker{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final String name;
	public final Double altitude;
	public final String url;
	
	public ImageMarker(final Coordinate center, final double altitude, final String name, final String url) {
		super(center);
		this.name = name;
		this.url = url;
		this.altitude = altitude;
	}
	
	public ImageMarker(final Coordinate center, final String name, final String url) {
		super(center);
		this.name = name;
		this.url = url;
		this.altitude = null;
	}
}
