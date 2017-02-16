package constellation;

import java.awt.Frame;
import java.awt.event.*;
import SimpleOpenNI.SimpleOpenNI;
import java.awt.BorderLayout;
import java.util.HashMap;

import processing.core.*;

public class CalibFrame extends Frame {
	private static final long serialVersionUID = -8312777389085939599L;
	int xLoc;
	int yLoc;
	int h;
	int w;
	String name;
	CalibApplet calib; 
	
	int numLights = 3;

	CalibFrame(int x_, int y_, int w_, int h_, String name_) {
		super("Embedded PApplet");
		xLoc = x_;
		yLoc = y_;
		w = w_;
		h = h_;
		name = name_;
		calib = new CalibApplet();
		add(calib, BorderLayout.CENTER);
		calib.init();
		setBounds(xLoc, yLoc, w, h);
		setTitle(name);
		setVisible(true);
		addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
            	calib.noLoop();
            	dispose();
            }
        });
	}

	public HashMap<String, PVector> getMap(){
		return calib.getMap();
	}
	
	public static class CalibApplet extends PApplet {
		// serialization of the class
		private static final long serialVersionUID = -2073021198946817730L;

		private int calibStep = 0;
		boolean screenFlag = true;
		public static PVector[] lightsVectors;
		public static PVector[] tempVectors;
		
		private HashMap<String, PVector> map;
		public HashMap<String, PVector> getMap() {
			return this.map;
		}
		
		SimpleOpenNI context;

		public void setup() {
			size(640, 480);
			
			//SimpleOpenNI camera set up
			Launcher.parent = this;
			context = Launcher.context;
			context.setMirror(false);
			context.enableDepth();  
			if (context.isInit() == false) {
				println("Can't open the depthMap, maybe the camera is not connected!");
				exit();
			}
			context.alternativeViewPointDepthToImage(); // align depth data to image data
			
			// calibration info
			lightsVectors = new PVector[User.NUM_LIGHTS];
			lightsVectors[0] = new PVector  (-2197.7861f,  640.2997f, 9870.0f);
			lightsVectors[1] = new PVector (-1751.3065f,  1342.899f, 3948.0f);
			lightsVectors[2] = new PVector ( 2718.128f, 1205.9951f, 5291.0f);
			lightsVectors[3] = new PVector   (2630.421f, 224.97023f, 9870.0f);
			lightsVectors[4] = new PVector    (-1260.5065f, 441.17722f, 3994.0f);
			lightsVectors[5] = new PVector     (1033.5884f, 498.20435f, 4241.0f);
			lightsVectors[6] = new PVector     (-136.5742f, 1012.2554f, 4582.0f);
			tempVectors[0] = new PVector   (640.2997f, 9870.0f);
			tempVectors[1] = new PVector   (1342.899f, 3948.0f);
			tempVectors[2] = new PVector   (1205.9951f, 5291.0f);
			tempVectors[3] = new PVector   (224.97023f, 9870.0f);
			tempVectors[4] = new PVector   (441.17722f, 3994.0f);
			tempVectors[5] = new PVector   (498.20435f, 4241.0f);
			tempVectors[6] = new PVector   (1012.2554f, 4582.0f);
		}

		public void draw() {
			context.update();   //update the cam
			image(context.depthImage(), 0, 0);   //display the camera image

			// instructions
			pushStyle();
			noStroke();
			fill(0, 200, 0, 100);
			rect(0, 0, width, 40);
			popStyle();
			if (calibStep == 0) {text("To start the calibration press SPACE!", 5, 30);}
			else if(calibStep == User.NUM_LIGHTS -1) {text("New ceiling is defined! You can close this window.", 5, 30);}
			else{
				text("Set the light number " + calibStep + " and press SPACE to confirm.", 5, 30);
			}

			drawCalibPoint();

			for (int k = 0; k< User.NUM_LIGHTS; k++){
				PVector tempVector = new PVector();
				context.convertRealWorldToProjective(lightsVectors[k], tempVector);
				tempVectors[k] = tempVector;
			}
			
			// drawing lights display
			pushStyle();
			strokeWeight(3);
			noFill();
			stroke(255, 0, 0);
			for (int k = 0; k< User.NUM_LIGHTS; k++){
				ellipse(tempVectors[k].x, tempVectors[k].y, 10, 10);
			}
			popStyle();
		}
		
		
		

		public void drawCalibPoint() {
			pushStyle();
			strokeWeight(3);
			noFill();
			
			if (calibStep == 0) {
			}
			if (calibStep == User.NUM_LIGHTS) {
			}
			else {
				for (int k = 0; k < calibStep; k++){
					context.convertRealWorldToProjective(lightsVectors[k], tempVectors[k]);
					stroke(255, 0, 0);
					ellipse(tempVectors[k].x, tempVectors[k].y, 10, 10);
				}
			}
			popStyle();
		}
		
		
		

		public void keyPressed() {
			switch (key) {
			case '1':
				screenFlag = !screenFlag;
				break;
			case ' ':
				calibStep++;
				if (calibStep == User.NUM_LIGHTS) {
					println("Lights position are calibrated.");
					for (int k = 0; k< User.NUM_LIGHTS; k++){
						println("Light " + k + lightsVectors[k]);
					}

					this.map = new HashMap<>();
					for (int k = 0; k< User.NUM_LIGHTS; k++){
						map.put("k", lightsVectors[k]);
					}
				}
				break;
			}
		}

		public void mousePressed() {
			if (mouseButton == LEFT) {
				PVector[] realWorldMap = context.depthMapRealWorld();
				int index = mouseX + mouseY * context.depthWidth();
				
				for (int k = 0; k< User.NUM_LIGHTS; k++){
					lightsVectors[k].set(realWorldMap[index]);
				}
			} else {
				PVector[] realWorldMap = context.depthMapRealWorld();
				int index = mouseX + mouseY * context.depthWidth();
				println("Point3d: " + realWorldMap[index].x + "," + realWorldMap[index].y + ","
						+ realWorldMap[index].z);
			}
		}

		public void mouseDragged() {
			if (mouseButton == LEFT) {
				PVector[] realWorldMap = context.depthMapRealWorld();
				int index = mouseX + mouseY * context.depthWidth();
				
				if (calibStep == 0) {
				}
				if (calibStep == User.NUM_LIGHTS) {
				}
				else {
					for (int k = 0; k < calibStep; k++){
						lightsVectors[k].set(realWorldMap[index]);
					}
				}
			}
		}
		
	}
}