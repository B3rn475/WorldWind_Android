package it.polimi.snowwatch.utils;

/**
 * 3D Omogeneous vector
 * @author B3rn475
 *
 */
public class Vector4 extends Vector{
	public final float x;
	public final float y;
	public final float z;
	public final float w;
	
	public Vector4(final float x, final float y, final float z, final float w){
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public Vector4(final Vector3 v){
		this(v, 1);
	}
	
	public Vector4(final Vector3 v, final float w){
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
		this.w = w;
	}
	
	public boolean needClipping(){
		final float aw = Math.abs(w);
		if (Math.abs(x) > aw) return true;
		if (Math.abs(y) > aw) return true;
		if (Math.abs(z) > aw) return true;
		return false;
	}

	@Override
	public float norm() {
		return (float) Math.sqrt(x*x + y*y + z*z + w*w);
	}

	@Override
	public Vector4 multiply(final float a) {
		return new Vector4(x*a, y*a, z*a, w*a);
	}

	public Vector3 toVector3() {
		return new Vector3(this);
	}
}