package it.polimi.models;

import java.io.Serializable;

import android.location.Location;

public class Coordinate implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final double latitude;
	public final double longitude;
	
	private static double EARTH_RADIUS = 6371e3;
	
	public Coordinate(final double latitude, final double longitude){
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public double distance(Coordinate point){
		/*double f1 = Math.toRadians(latitude);
		double f2 = Math.toRadians(point.latitude);
		double df = Math.toRadians(point.latitude-latitude);
		double dl = Math.toRadians(point.longitude-longitude);

		double a = Math.sin(df/2) * Math.sin(df/2) +
		        Math.cos(f1) * Math.cos(f2) *
		        Math.sin(dl/2) * Math.sin(dl/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

		return EARTH_RADIUS * c;*/
		float[] results = new float[3];
		Location.distanceBetween(this.latitude, this.longitude, point.latitude, point.longitude, results);
		return results[0];
	}
	
	public double bearing(Coordinate point){
		/*double f1 = Math.toRadians(latitude);
		double f2 = Math.toRadians(point.latitude);
		double df = Math.toRadians(point.latitude-latitude);
		double dl = Math.toRadians(point.longitude-longitude);

		double a = Math.sin(df/2) * Math.sin(df/2) +
		        Math.cos(f1) * Math.cos(f2) *
		        Math.sin(dl/2) * Math.sin(dl/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

		return EARTH_RADIUS * c;*/
		float[] results = new float[3];
		Location.distanceBetween(this.latitude, this.longitude, point.latitude, point.longitude, results);
		return results[1];
	}
}
