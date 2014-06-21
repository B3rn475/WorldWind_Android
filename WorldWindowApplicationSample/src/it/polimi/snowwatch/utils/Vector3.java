package it.polimi.snowwatch.utils;

/**
 * 3D non omogeneus vector
 * @author B3rn475
 *
 */
public class Vector3 extends Vector {
	public final float x;
	public final float y;
	public final float z;
	
	public Vector3(final float x, final float y, final float z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3(final Vector4 v){
		final float w = v.w;
		this.x = v.x / w;
		this.y = v.y / w;
		this.z = v.z / w;
	}

	@Override
	public float norm() {
		return (float) Math.sqrt(x*x + y*y + z*z);
	}
	
	@Override
	public Vector3 multiply(final float a) {
		return new Vector3(x*a, y*a, z*a);
	}
	
	public static final Vector3 ZERO = new Vector3(0, 0, 0);
}
