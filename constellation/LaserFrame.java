package constellation;
import java.awt.Frame;
import java.awt.event.*;
import SimpleOpenNI.SimpleOpenNI;
import java.awt.BorderLayout;
import java.util.HashMap;

import processing.core.*;


public class LaserFrame extends Frame {
	private int[] lightsInUse = {4,5,6};
	private static final long serialVersionUID = -8312777389085939599L;
	int xLoc;
	int yLoc;
	int h;
	int w;
	String name;
	HashMap<String, PVector> map;
	LaserApplet laser;
	private int lightSelected;

	LaserFrame(int x_, int y_, int w_, int h_, String name_, HashMap<String, PVector> map_){
		super("Embedded PApplet");
		xLoc = x_;
		yLoc = y_;
		w = w_;
		h = h_;
		name = name_;
		map = map_;
		laser = new LaserApplet();
		add(laser, BorderLayout.CENTER);
		laser.init();
		setBounds(xLoc, yLoc, w, h);
		setTitle(name);
		setVisible(true);
		addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
            	laser.noLoop();
            	dispose();
            }
        });
	}

	public int getLightSelected() {
		return lightSelected;
	}

	public class LaserApplet extends PApplet {
		//seriqlizqtion of the class
		private static final long serialVersionUID = -743200068153160983L;
		//view parameters
		float zoomF = 0.5f;
		float rotX = radians(180);  // by default rotate the hole scene 180deg around the x-axis,
		float rotY = radians(0);
		boolean autoCalib = true;
		int white = color(255, 255, 255);
		int[] userClr = new int[] { color(255, 0, 0), color(0, 255, 0), color(0, 0, 255), color(255, 255, 0),
				color(255, 0, 255), color(0, 255, 255) };
		float sphereRadius = 400;
		
		SimpleOpenNI context;

		public void setup() {
			size(1024, 768, P3D);

			// camera setup
			Launcher.parent = this;
			context = Launcher.context;
			context.setMirror(true);
			if (context.isInit() == false) {
				println("Can't init SimpleOpenNI, maybe the camera is not connected!");
				exit();
				return;
			}
			context.enableDepth();
			context.enableUser();
			// transform display to show perspective
			perspective(radians(45), (float) width / height, 10, 150000);
		}

		public void draw() {
			background(0, 0, 0);

			// update the cam
			context.update();

			// set the scene pos
			translate(width / 2, height / 2, 0);
			rotateX(rotX);
			rotateY(rotY);
			scale(zoomF);

			int[] depthMap = context.depthMap();
			int steps = 4; // to speed up the drawing, draw every fourth point
			int index;
			PVector realWorldPoint;

			translate(0, 0, -1000); // set the rotation center of the scene 1000
									// infront of the camera

			// points projected representation
			strokeWeight(3);
			stroke(150);
			for (int y = 0; y < context.depthHeight(); y += steps) {
				for (int x = 0; x < context.depthWidth(); x += steps) {
					index = x + y * context.depthWidth();
					if (depthMap[index] > 0) {
						realWorldPoint = context.depthMapRealWorld()[index];
						point(realWorldPoint.x, realWorldPoint.y, realWorldPoint.z);
					}
				}
			}

			// draw the skeleton if it's available, for each the users
			int[] userList = context.getUsers();

			for (int i = 0; i < userList.length; i++) {
				if (context.isTrackingSkeleton(userList[i])) {
					drawIntersection(userList[i]);
				}
			}
			
			pushStyle(); // style layer: modifications of style until popStyle
			stroke(255, 255, 255, 150);
			strokeWeight(5);
			for (int k = 0; k < User.NUM_LIGHTS; k++) {
				ellipse3D(User.lightsVectors[k], 30, white);
			}
			popStyle();
		}

		public void drawIntersection(int userId) {

			// get the position of the elbow and the arm
			PVector jointPos1 = new PVector();
			PVector jointPos2 = new PVector();
			// call to get skeleton data from the cam (returns confidence)
			float c1 = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, jointPos1);
			float c2 = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_HAND, jointPos2);
			// draw ray from the hand + 4m
			PVector dir = PVector.sub(jointPos2, jointPos1);
			dir.normalize();
			PVector rayEnd = PVector.mult(dir, 4000);
			rayEnd.add(jointPos2);

			pushStyle(); // style layer: modifications of style until popStyle
			stroke(255);
			strokeWeight(10);
			line(jointPos2.x, jointPos2.y, jointPos2.z, rayEnd.x, rayEnd.y, rayEnd.z);
			
			for (int k = 0; k < User.NUM_LIGHTS; k++) {
				if(intersectionLight(jointPos2, dir, User.lightsVectors[k], userClr[k])) {lightSelected = lightsInUse[k];}
			}
			popStyle();
		}

		// test for intersections
		public boolean intersectionLight(PVector joint, PVector direction, PVector light, int c) {

			PVector hit1 = new PVector();
			PVector hit2 = new PVector();

			// Open NI call: need an origin, a direction, a sphere target with a
			// radius
			// Output: two vector of intersection with the sphere
			int intersectionSphere = SimpleOpenNI.raySphereIntersection(joint, direction, light, sphereRadius, hit1,
					hit2);
			
			if (intersectionSphere > 0) {
				ellipse3D(light, 50, c);
				return true;
			}
			else {return false;}
		}

		// -----------------------------------------------------------------
		// SimpleOpenNI events

		public void onNewUser(SimpleOpenNI curContext, int userId) {
			println("onNewUser - userId: " + userId);
			println("\tstart tracking skeleton");
			curContext.startTrackingSkeleton(userId);
		}

		public void onLostUser(SimpleOpenNI curContext, int userId) {
			println("onLostUser - userId: " + userId);
		}

		// --------------------------------
		// useful

		public void line3D(PVector p, PVector q) {
			line(p.x, p.y, p.z, q.x, q.y, q.z);
		}

		public void ellipse3D(PVector p, int r, int c) {
			pushMatrix();
			translate(p.x, p.y, p.z);
			fill(c);
			sphere(r);
			popMatrix();
		}

	}

}
