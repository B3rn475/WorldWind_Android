package it.polimi.models;

import java.io.Serializable;

public class SquareMarker implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final Coordinate center;
	
	private static double EARTH_RADIUS = 6371e3;
	
	public SquareMarker(final Coordinate center){
		if (center == null)
			throw new NullPointerException("center cannot be null");
		this.center = center;
	}
	
	public Coordinate getLowerBoundaryCoordinate(final double size){
		return new Coordinate(latDistance(-size,center.latitude), longDistance(-size, center.latitude, center.longitude));
	}
	
	public Coordinate getUpperBoundaryCoordinate(final double size){
		return new Coordinate(latDistance(size,center.latitude), longDistance(size, center.latitude, center.longitude));
	}
	
	private static double latDistance(double distance, double latitude){
        return latitude + radToDeg(distance / EARTH_RADIUS);
    }
	
    private static double longDistance(double distance, double latitude, double longitude) {
        return longitude + radToDeg(distance / EARTH_RADIUS / Math.cos(degToRad(latitude)));
    }
    
    private static double degToRad(double degrees) {
        return (degrees * Math.PI) / 180;
    }
    
    private static double radToDeg(double radians) {
        return (180.0 * radians) / Math.PI;
    }

	public boolean isInside(final Coordinate coordinates, final double size) {
		final Coordinate lb = this.getLowerBoundaryCoordinate(size);
		final Coordinate ub = this.getUpperBoundaryCoordinate(size);
		return coordinates.latitude >= lb.latitude && coordinates.latitude <= ub.latitude && coordinates.longitude >= lb.longitude && coordinates.longitude <= ub.longitude;
	}
}
