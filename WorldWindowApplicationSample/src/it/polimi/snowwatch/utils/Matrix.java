package it.polimi.snowwatch.utils;

import java.security.InvalidParameterException;

/**
 * Class tha describe a Matrix
 * @author B3rn475
 *
 */
public class Matrix {
		public final float m00;
		public final float m01;
		public final float m02;
		public final float m03;
		public final float m10;
		public final float m11;
		public final float m12;
		public final float m13;
		public final float m20;
		public final float m21;
		public final float m22;
		public final float m23;
		public final float m30;
		public final float m31;
		public final float m32;
		public final float m33;
		
		public Matrix(final float[] m) {
			  if (m.length == 16){
				  m00 = m[0];  m01 = m[1];  m02 = m[2];  m03 = m[3];
				  m10 = m[4];  m11 = m[5];  m12 = m[6];  m13 = m[7];
				  m20 = m[8];  m21 = m[9];  m22 = m[10]; m23 = m[11];
				  m30 = m[12]; m31 = m[13]; m32 = m[14]; m33 = m[15];
			  } else if (m.length == 9){
				  m00 = m[0]; m01 = m[1];  m02 = m[2]; m03 = 0;
				  m10 = m[3]; m11 = m[4];  m12 = m[5]; m13 = 0;
				  m20 = m[6]; m21 = m[7];  m22 = m[8]; m23 = 0;
				  m30 = 0;    m31 = 0;     m32 = 0;    m33 = 1;
			  } else {
				  throw new InvalidParameterException("Unable to generate a matrix from array of length " + m.length);
			  }
		}
		
		/**
		 * Return only the Rotation Matrix
		 * @return
		 */
		public float[] toArray9(){
			return new float[]{m00, m01, m02, m10, m11, m12, m20, m21, m22};
		}
		
		/**
		 * Complete extraction of the values
		 * @return
		 */
		public float[] toArray16(){
			return new float[]{m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33};
		}
		
		public Vector4 multiply(final Vector4 v){
			return new Vector4(
					m00 * v.x + m01 * v.y + m02 * v.z + m03 * v.w,
					m10 * v.x + m11 * v.y + m12 * v.z + m13 * v.w,
					m20 * v.x + m21 * v.y + m22 * v.z + m23 * v.w,
					m30 * v.x + m31 * v.y + m32 * v.z + m33 * v.w);
		}
		
		public Vector4 multiply(final Vector3 v){
			return new Vector4(
					m00 * v.x + m01 * v.y + m02 * v.z + m03,
					m10 * v.x + m11 * v.y + m12 * v.z + m13,
					m20 * v.x + m21 * v.y + m22 * v.z + m23,
					m30 * v.x + m31 * v.y + m32 * v.z + m33);
		}
		
		/**
		 * Multiply 2 matrix (can be optimized)
		 * @param m
		 * @return
		 */
		public Matrix multiply(final Matrix m){
			/*return new Matrix(new float[]{
					m00 * m.m00 + m01 * m.m10 + m02 * m.m20 + m03 * m.m30, m00 * m.m01 + m01 * m.m11 + m02 * m.m21 + m03 * m.m31, m00 * m.m02 + m01 * m.m12 + m02 * m.m22 + m03 * m.m32, m00 * m.m03 + m01 * m.m13 + m02 * m.m23 + m03 * m.m33,
					m10 * m.m00 + m11 * m.m10 + m12 * m.m20 + m13 * m.m30, m10 * m.m01 + m11 * m.m11 + m12 * m.m21 + m13 * m.m31, m10 * m.m02 + m01 * m.m12 + m12 * m.m22 + m13 * m.m32, m10 * m.m03 + m11 * m.m13 + m12 * m.m23 + m13 * m.m33,
					m20 * m.m00 + m21 * m.m10 + m22 * m.m20 + m23 * m.m30, m20 * m.m01 + m21 * m.m11 + m22 * m.m21 + m23 * m.m31, m20 * m.m02 + m01 * m.m12 + m22 * m.m22 + m23 * m.m32, m20 * m.m03 + m21 * m.m13 + m22 * m.m23 + m23 * m.m33,
					m30 * m.m00 + m31 * m.m10 + m32 * m.m20 + m33 * m.m30, m30 * m.m01 + m31 * m.m11 + m32 * m.m21 + m33 * m.m31, m30 * m.m02 + m01 * m.m12 + m32 * m.m22 + m33 * m.m32, m30 * m.m03 + m31 * m.m13 + m32 * m.m23 + m33 * m.m33});*/
			final float[] tM = this.toArray16();
			final float[] mM = m.toArray16();
			final float[] rM = new float[16];
			for(int r=0; r<4; r++){
				for (int c=0; c<4; c++){
					rM[r*4+c] = 0;
					for (int i=0; i<4; i++){
						rM[r*4+c] += tM[r*4+i]*mM[i*4+c];
					}
				}
			}
			return new Matrix(rM);
		}
		
		/**
		 * transposed matrix
		 * @return
		 */
		public Matrix transposed(){
			return new Matrix(new float[] {m00, m10, m20, m30, m01, m11, m21, m31, m02, m12, m22, m32, m03, m13, m23, m33});
		}
		
		public static final Matrix IDENTITY = new Matrix(new float[]{1, 0, 0, 0,
																	 0, 1, 0, 0,
																	 0, 0, 1, 0,
																	 0, 0, 0, 1});
		/**
		 * New projection matrix from settings
		 * @param width
		 * @param height
		 * @param near
		 * @param far
		 * @param fov
		 * @return
		 */
		public static Matrix makePerspectiveProj(final float width, final float height, final float near, final float far, final float fov){
			final float ar = width / height;
			final float zNear = near;
			final float zFar = far;
			final float zRange = far-near;
			final float tanHalfFOV = (float)Math.tan(Math.toRadians(fov / 2.0f));
			
			return new Matrix(new float[]{
						1.0f / (tanHalfFOV * ar),                 0,                        0, 						   0,
						                       0, 1.0f / tanHalfFOV,                        0, 	                       0,
						                       0,                 0, (-zNear - zFar) / zRange, 2 * zFar * zNear / zRange,
						                       0,                 0,                        1,                         0});
		}
		
		/**
		 * New rotation matrix along X axis
		 * @param pitch
		 * @return
		 */
		public static Matrix makeRotationX(final float pitch){
			final double rX = Math.toRadians(pitch);
			final float xC = (float) Math.cos(rX);
			final float xS = (float) Math.sin(rX);
			return new Matrix(new float[]{
                    1,  0,   0, 0, 
                    0, xC, -xS, 0,
                    0, xS,  xC, 0,
                    0,  0,   0, 1});
		}
		
		/**
		 * New rotation matrix along Y axis
		 * @param pitch
		 * @return
		 */
		public static Matrix makeRotationY(final float roll){
			final double rY = Math.toRadians(roll);
			final float yC = (float) Math.cos(rY);
			final float yS = (float) Math.sin(rY);
			return new Matrix(new float[]{
                    yC, 0, yS, 0, 
                     0, 1,  0, 0,
                   -yS, 0, yC, 0,
                     0, 0,  0, 1});
		}
		
		/**
		 * New rotation matrix along Z axis
		 * @param pitch
		 * @return
		 */
		public static Matrix makeRotationZ(final float yaw){
			final double rZ = Math.toRadians(yaw);
			final float zC = (float) Math.cos(rZ);
			final float zS = (float) Math.sin(rZ);
			return new Matrix(new float[]{
                    zC, -zS, 0, 0, 
                    zS,  zC, 0, 0,
                    0,    0, 1, 0,
                    0,    0, 0, 1});
		}
		
		/**
		 * Make a new translation matrix
		 * @param x
		 * @param y
		 * @param z
		 * @return
		 */
		public static Matrix makeTranslationMatrix(final float x, final float y, final float z){
			return new Matrix(new float[]{
                    1, 0, 0, x, 
                    0, 1, 0, y,
                    0, 0, 1, z,
                    0, 0, 0, 1});
		}
		
		/**
		 * Make a new view matrix
		 * @param eye
		 * @param azimuth
		 * @param pitch
		 * @param roll
		 * @return
		 */
		public static Matrix makeViewMatrix(final Vector3 eye, final float azimuth, final float pitch, final float roll){
			
			Matrix eyeTranslation = Matrix.makeTranslationMatrix(-eye.x, -eye.y, -eye.z);
			Matrix aRotation = Matrix.makeRotationY(azimuth);
			Matrix pRotation = Matrix.makeRotationX(pitch);
			Matrix rRotation = Matrix.makeRotationZ(roll);
			
			return rRotation.multiply(pRotation.multiply(aRotation.multiply(eyeTranslation)));
		}
}
