
/*  ************** READ ME *************
 * CONSTELLATION
 *  Speech and Gesture Light System Control
 *  
*	Tested with ASUS Xtion Pro
*	
*   Please make sure that:
*   - Processing 2 is installed
*   - Simple Open NI is installed
*	- the camera is connected
*	to the computer via a USB 2.0 port
*	- run this program as a Java Applet
*   
*   Refer to Constellation Setup Guide
*   https://docs.google.com/document/d/1HpO8hGLaa7HpF74TBYGDHB-de1lfbYJK24u8smRUuSE
*
*------------------------------------------------------
*[TO DO]
* - fix the calibration mode to do it several times if needed
* - fix the ray
* ------------------------------------------------------
* 
* Coder: Jorg, Frederic, Stephane
* Date: February 2017
* Organisation: TU Berlin
*/

package constellation;

import SimpleOpenNI.SimpleOpenNI;
import java.util.ArrayList;
//import constellation.SpeechUnit.Command;
import processing.core.*;
import processing.data.Table;
import processing.data.TableRow;
import controlP5.*;

public class Launcher extends PApplet {
	public static void main(String[] args) {
		PApplet.main("constellation.Launcher");
	}
	private ControlP5 cp5;
	private SimpleOpenNI camera;
	//private SpeechUnit voice;
	//private LightUnit light;
	//private Command command;
	private State state = State.IDLE;
	private boolean shouldStop = false;
	//private SpeechUnit.Command command;
	// HELP TO CONTINUE
	private boolean helpDisplay = false;
	private int numberLights = 7;
	private int editLight = 1;
	private ArrayList<Light> lights;
	private Table lightsCoor;
	private int lightSelected;
	private int[] lightsInUse = {4,5,6};
	private String jsonStateBefore = ""; // for undo
	private ArrayList<Integer> prevSelectedLights;
	private ArrayList<Integer> selectedLights;
	float sphereRadius = 400;

	private enum State {
		IDLE, WAITING_FOR_COMMAND, INSTRUCTED, COPY_WAITING;
	}

	
	public void setup() {
		size(640, 480);
		setupUI();
		//voice = new SpeechUnit(this);
		camera = new SimpleOpenNI(this);
		camera.setMirror(true);
		camera.enableDepth();
		if (camera.isInit() == false) {
			println("Can't init SimpleOpenNI, maybe the camera is not connected!");
			exit();
			return;
		}
		camera.enableUser();
		camera.alternativeViewPointDepthToImage();
		initialize();
	}
	
	
	public void setupUI() {
		//buttons
		cp5 = new ControlP5(this);
		cp5.addButton("quit")
	     .setPosition(20,20)
	     .setSize(40,19)
	     ;
		cp5.addButton("help")
	     .setPosition(70,20)
	     .setSize(40,19)
	     ;
		
		//saved lights coordinates
		lightsCoor = loadTable("data/lights_coordinates.csv", "header");
		numberLights = lightsCoor.getRowCount();
		println(numberLights);
		lights = new ArrayList<Light>();
		for (TableRow row : lightsCoor.rows()) {
			lights.add(new Light(row.getFloat("x"), row.getFloat("y"), row.getFloat("z")));
		}
	}
	
	public void saveLightsCoor() {
		for (int k = 0; k < numberLights; k++){
			TableRow newRow = lightsCoor.addRow();
			newRow.setInt("number", k);
			Light light = lights.get(k);
			newRow.setFloat("x", light.getX());
			newRow.setFloat("y", light.getY());
			newRow.setFloat("z", light.getZ());
		}
		saveTable(lightsCoor, "data/lights_coordinates.csv");
	}

	public void draw() {
		camera.update(); 
		image(camera.depthImage(),0, 0);

		// draw the skeleton of users
		int[] userList = camera.getUsers();
		for (int i = 0; i < userList.length; i++) {
			if (camera.isTrackingSkeleton(userList[i])) {
				drawIntersection(userList[i]);
			}
		}
		
		drawLightsPoints();
	
		
		//interaction states
		switch (this.state) {
		case IDLE:
			break;
		case WAITING_FOR_COMMAND:
			break;
		case INSTRUCTED:
			//this.jsonStateBefore = light.getJsonState();
			//light.performAction(this.selectedLights, this.command);
			switchState(State.IDLE);
			this.prevSelectedLights = this.selectedLights;
			this.selectedLights = new ArrayList<Integer>();
			break;
		case COPY_WAITING:
			break;
		default:
			break;
		}
	}
	
	public void quit(int v) {
		println("ok");
		saveLightsCoor();
	}
	
	public void drawLightsPoints() {
		pushStyle();
		stroke(255, 255, 255, 150);
		strokeWeight(5);
		for (int k = 0; k < numberLights; k++){
			Light light = lights.get(k);
			PVector tempVec = new PVector();
			camera.convertRealWorldToProjective(light.getCoor(), tempVec);
			ellipse(tempVec.x, tempVec.y, 10, 10);
		}
		popStyle();
	}


//	boolean onSelectionTrigger() {
//		if (laserWindow != null) {
//			int selectedLight = laserWindow.getLightSelected();
//			if (selectedLight > 0) {
//				System.out.println("Last light selected: " + selectedLight);
//				this.selectedLights.add(selectedLight);
//				light.alertLight(selectedLight);
//				switchState(State.WAITING_FOR_COMMAND);
//				return true;
//			} else {
//				System.out.println("no light selected");
//			}
//		}
//		return false;
//	}

	void onClose() {
		switchState(State.IDLE);
	}

//	void onCommand(SpeechUnit.Command c) {
//		command = c;
//		switchState(State.INSTRUCTED);
//	}

	private void switchState(State state) {
		System.out.println("Switching state to:" + state);
		this.state = state;
	}

	private void initialize() {
		//(new Thread(this.voice)).start();
		this.prevSelectedLights = new ArrayList<Integer>();
		this.selectedLights = new ArrayList<Integer>();
		//light = new LightUnit();
	}

	public void onAllSelectionTrigger() {
		for (int i = 1; i < 10; i++) {
			selectedLights.add(i);
			//light.alertLight(i);
		}
		switchState(State.WAITING_FOR_COMMAND);
	}

	public void undoLast() {
		System.out.println("undoing last command");
		//light.setJsonState(jsonStateBefore);
	}

//	public void onCopyTrigger() {
//		if (laserWindow != null) {
//			int selectedLight = laserWindow.getLightSelected();
//			if (selectedLight > 0) {
//				System.out.println("Last light selected: " + selectedLight);
//				this.selectedLights.add(selectedLight);
//				light.alertLight(selectedLight);
//				switchState(State.COPY_WAITING);
//			} else {
//				System.out.println("no light selected");
//			}
//		}
//	}

//	public void onCopy2Trigger() {
//		if (laserWindow != null) {
//			int selectedLight2 = laserWindow.getLightSelected();
//			if (selectedLight2 > 0) {
//				System.out.println("Copying light settings");
//				System.out.println("First light selected:" + selectedLights.get(0));
//				System.out.println("Second light selected: " + selectedLight2);
//				light.alertLight(selectedLight2);
//				switchState(State.IDLE);
//				String state1 = light.getJsonState(this.selectedLights.get(0));
//				light.setJsonState(selectedLight2, state1);
//			} else {
//				System.out.println("no light selected");
//			}
//		}
//	}
	

	public void mousePressed() {
		PVector[]  realWorldMap = camera.depthMapRealWorld();
		int index = mouseX + mouseY * camera.depthWidth();
		if (mouseButton == LEFT) {
			//lightsVectors[editLight].set(realWorldMap[index]);
		} else {
			System.out.println("Point3d: " + realWorldMap[index].x + "," + realWorldMap[index].y + ","
					+ realWorldMap[index].z);
		}
	}
	
	public void mouseDragged(){
		if (mouseButton == LEFT) {
			PVector[] realWorldMap = camera.depthMapRealWorld();
			int index = mouseX + mouseY * camera.depthWidth();
			//lightsVectors[editLight].set(realWorldMap[index]);
		}
	}
	
	public void keyPressed() {
		switch(key) {
		case 'h' : helpDisplay = true;
		case '1' : editLight = 1;
		case '2' : editLight = 2;
		case '3' : editLight = 3;
		case '4' : editLight = 4;
		case '5' : editLight = 5;
		case '6' : editLight = 6;
		}
	}
	
	

	public void drawIntersection(int userId) {
		PVector jointPos1 = new PVector();
		PVector jointPos2 = new PVector();
		// call to get skeleton data from the cam (returns confidence)
		float c1 = camera.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, jointPos1);
		float c2 = camera.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_HAND, jointPos2);
		PVector dir = PVector.sub(jointPos2, jointPos1);
		dir.normalize();
		PVector rayEnd = PVector.mult(dir, 4000);  //direction 4m
		rayEnd.add(jointPos2);

		pushStyle(); // style layer: modifications of style until popStyle
		stroke(255);
		strokeWeight(10);
		PVector temp1 = new PVector();
		PVector temp2 = new PVector();
		camera.convertRealWorldToProjective(jointPos2, temp1);
		camera.convertRealWorldToProjective(rayEnd, temp2);
		line(temp1.x, temp1.y, temp2.x, temp2.y);

		//if(intersectionLight(jointPos2, dir, light1, 0)) {lightSelected = lightsInUse[0];}

		popStyle();
	}

	// test for intersections
	public boolean intersectionLight(PVector joint, PVector direction, PVector light, int c) {
		PVector hit1 = new PVector();
		PVector hit2 = new PVector();
		// raySphereIntersection: origin, direction, a sphere target with radius
		// Output: two vector of intersection with the sphere  ->( )->
		int intersectionSphere = SimpleOpenNI.raySphereIntersection(joint, direction, light, sphereRadius, hit1,
				hit2);
		if (intersectionSphere > 0) {
			//ellipse(temp1.x, temp1.y, 50, 50);
			return true;
		}
		else {return false;}
	}

	// -----------------------------------------------------------------
	// SimpleOpenNI events

	public void onNewUser(SimpleOpenNI curcamera, int userId) {
		System.out.println("onNewUser - userId: " + userId);
		System.out.println("\tstart tracking skeleton");
		camera.startTrackingSkeleton(userId);
	}

	public void onLostUser(SimpleOpenNI curContext, int userId) {
		System.out.println("onLostUser - userId: " + userId);
	}
}
