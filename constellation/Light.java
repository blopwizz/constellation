package constellation;

import processing.core.*;

public class Light {
	private int number;
	private PVector coor3D;
	private PVector coor2D;
	public static float RADIUS = 30;
	
	public Light(int n, float x_, float y_, float z_){ 
		number = n;
		coor3D = new PVector(x_, y_, z_);	
	}
	
	
	public void setCoor(PVector p) {
		coor3D = p;
	}
	
	public int getNumber() {return number;}
	public float getX() {return coor3D.x;}
	public float getY() {return coor3D.y;}
	public float getZ() {return coor3D.z;}
	public PVector getCoor3D() {return coor3D;}
	public PVector getCoor2D() {
		PVector temp = new PVector();
		Launcher.camera.convertRealWorldToProjective(coor3D, temp);
		return temp;
	}
}
