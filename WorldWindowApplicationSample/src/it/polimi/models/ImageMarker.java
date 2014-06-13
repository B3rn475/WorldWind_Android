package it.polimi.models;

public class ImageMarker extends SquareMarker {
	
	public final String name;
	
	public ImageMarker(final Coordinate center, final String name) {
		super(center);
		this.name = name;
	}
}
