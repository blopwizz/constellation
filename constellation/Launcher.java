
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
*	- run this program as a Java Application
*   
*   Refer to Constellation Setup Guide
*   https://docs.google.com/document/d/1HpO8hGLaa7HpF74TBYGDHB-de1lfbYJK24u8smRUuSE
*
* ------------------------------------------------------
* 
* Coder: Joerg, Frederic, Stephane
* Date: February 2017
* Organisation: TU Berlin
*/

package constellation;

import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import constellation.SpeechUnit.Command;
import constellation.SpeechUnit.Preset;
import SimpleOpenNI.SimpleOpenNI;
import processing.core.*;
import processing.data.Table;
import processing.data.TableRow;
import controlP5.*;

public class Launcher extends PApplet {

	public static void main(String[] args) {
		PApplet.main("constellation.Launcher");
	}

	private ControlP5 cp5;
	public static SimpleOpenNI camera;
	private boolean activateSpeech = false;

	private SpeechUnit voice;
	private LightUnit lightUnit;
	private Command command;
	private boolean shouldStop = false;
	// private SpeechUnit.Command command;
	// HELP TO CONTINUE
	private boolean helpDisplay = false;
	private int numberLights;
	private ArrayList<Light> lights;
	private Table lightsCoor;
	private int lightSelected;
	private int[] lightsInUse = { 4, 5, 6 };
	private String jsonStateBefore = ""; // for undo
	private ArrayList<Integer> prevSelectedLights;
	private ArrayList<Integer> selectedLights;
	private int lastLightAdded;

	float sphereRadius = 400;

	public void setup() { // excuted one time before the draw loop (Processing)
		size(640, 480); // size of the sketch
		setupUI(); // UI: controlP5 buttons, text, user parameters
		setupCamera(); // Camera: Xtion pro, SimpleOpenNI
		initialize(); // start Speech, Light control threads
	}

	public void draw() { // loop drawing each frame (Processing)
		updateCamera(); // Simple Open NI
		drawSkeleton();
		drawLightsPoints(); // draw light calibration points
	}

	// ---------------------- SETUP FUNCTIONS ---------------------------------
	public void setupUI() {

		cp5 = new ControlP5(this);
		lightsCoor = loadTable("src/lights_coordinates.csv", "header");
		numberLights = lightsCoor.getRowCount();
		int val = numberLights; // need "fresh" int to avoid side effects
		cp5.addSlider("quantity").setPosition(20, height - 120).setSize(20, 100).setRange(0, 7).setNumberOfTickMarks(8)
				.setValue(val);
		lights = new ArrayList<Light>();
		Iterator<TableRow> it = lightsCoor.rows().iterator();
		while (it.hasNext() && lights.size() < numberLights) {
			TableRow row = it.next();
			lights.add(new Light(row.getInt("number"), row.getFloat("x"), row.getFloat("y"), row.getFloat("z")));
		}
		// buttons

		cp5.addButton("quit").setPosition(20, 20).setSize(40, 19);
		cp5.addButton("help").setPosition(70, 20).setSize(40, 19);

	}

	public void setupLightsPosition(int number) {
		// saved lights coordinates
		lights = new ArrayList<Light>();
		for (int k = 0; k < number; k++) {
			lights.add(new Light(k, -200 * cos(TWO_PI * k / 7), 500 - 200 * sin(TWO_PI * k / 7), 2000));
		}
		numberLights = number;
	}

	public void setupCamera() {
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
	}

	private void initialize() {
		voice = new SpeechUnit(this);
		if (activateSpeech) {
			(new Thread(this.voice)).start();
		}
		this.prevSelectedLights = new ArrayList<Integer>();
		this.selectedLights = new ArrayList<Integer>();
		lightUnit = new LightUnit();
	}

	// ----------------------- UPDATE AND DRAW FUNCTIONS
	// -----------------------------

	public void updateCamera() {
		camera.update();
		image(camera.depthImage(), 0, 0);
	}

	public void drawSkeleton() {
		int[] userList = camera.getUsers();
		for (int i = 0; i < userList.length; i++) {
			if (camera.isTrackingSkeleton(userList[i])) {
				drawIntersection(userList[i]);
			}
		}
	}

	public void drawLightsPoints() {

		for (int k = 0; k < lights.size(); k++) {
			PVector coor2d = lights.get(k).getCoor2D();
			pushStyle();
			stroke(255, 255, 255, 150);
			strokeWeight(5);
			ellipse(coor2d.x, coor2d.y, 20, 20);
			fill(0);
			text(k, coor2d.x - 5, coor2d.y + 4);
			popStyle();
		}

	}

	// ---------------------------- DATA -----------------------------------

	public void saveLightsCoor() {
		lightsCoor.clearRows();
		for (int k = 0; k < numberLights; k++) {
			TableRow newRow = lightsCoor.addRow();
			newRow.setInt("number", k);
			Light light = lights.get(k);
			newRow.setFloat("x", light.getX());
			newRow.setFloat("y", light.getY());
			newRow.setFloat("z", light.getZ());
		}
		String fileName = dataPath("src/lights_coordinates.csv");
		File f = new File(fileName);
		if (f.exists()) {
			f.delete();
		}
		saveTable(lightsCoor, "src/lights_coordinates.csv");
	}

	// ---------------------------------- UI EVENTS ----------------------------

	public void quit() { // controlP5 button
		saveLightsCoor();
		System.exit(0);
	}

	public void quantity(float number) { // controlP5 slider
		setupLightsPosition(floor(number));
	}

	public void mouseDragged() {
		PVector[] realWorldMap = camera.depthMapRealWorld();
		int index = mouseX + mouseY * camera.depthWidth();
		Iterator<Light> it = lights.iterator();
		boolean oneSelected = false;
		while (it.hasNext() && !oneSelected) {
			Light light = it.next();
			if (mouseOver(light)) {
				PVector currCoor = light.getCoor3D();
				PVector newCoor = realWorldMap[index];
				light.setCoor(realWorldMap[index]);
				oneSelected = true;
			}
		}
	}

	public void mousePressed() {
		PVector[] realWorldMap = camera.depthMapRealWorld();
		int index = mouseX + mouseY * camera.depthWidth();
		if (mouseButton == RIGHT) {
			printArray(realWorldMap[index]);
		}
	}

	public void keyPressed() {
		switch (key) {
		case 'h':
			helpDisplay = true;
		}
	}

	public boolean mouseOver(Light light) {
		PVector coor2d = light.getCoor2D();
		float disX = coor2d.x - mouseX;
		float disY = coor2d.y - mouseY;
		if (disX * disX + disY * disY < Light.RADIUS * Light.RADIUS / 4) {
			return true;
		} else {
			return false;
		}
	}

	// ------------------------------- STATE EVENTS
	// -------------------------------

	boolean onSelectionTrigger() {
		int selectedLight = getLightSelected();
		if (selectedLight > 0) {
			beforeAction();
			System.out.println("Last light selected: " + selectedLight);
			this.selectedLights.add(selectedLight);
			lightUnit.alertLight(selectedLight);
			return true;
		} else {
			System.out.println("no light selected");
		}
		return false;
	}

	void onClose() {
		afterAction();
	}

	void onCommand(SpeechUnit.Command c) {
		command = c;
		this.jsonStateBefore = lightUnit.getJsonState();
		lightUnit.performAction(this.selectedLights, this.command);
		this.prevSelectedLights = this.selectedLights;
		this.selectedLights = new ArrayList<Integer>();
	}

	public void onAllSelectionTrigger() {
		for (int i : lightsInUse) {
			selectedLights.add(i);
			lightUnit.alertLight(i);
		}
	}

	public void undoLast() {
		System.out.println("Reverting last change.");
		restorePreviousState();
	}

	public void onCopyTrigger() {
		int selectedLight = getLightSelected();
		if (selectedLight > 0) {
			beforeAction();
			System.out.println("Last light selected: " + selectedLight);
			this.selectedLights.add(selectedLight);
			lightUnit.alertLight(selectedLight);
		} else {
			System.out.println("no light selected");
		}

	}

	public void onPasteTrigger() {
		int selectedLight2 = getLightSelected();
		if (selectedLight2 > 0) {
			System.out.println("Copying light settings");
			System.out.println("First light selected:" + selectedLights.get(0));
			System.out.println("Second light selected: " + selectedLight2);
			lightUnit.alertLight(selectedLight2);
			String state1 = lightUnit.getJsonState(this.selectedLights.get(0));
			lightUnit.setJsonState(selectedLight2, state1);
			afterAction();
		} else {
			System.out.println("no light selected");
		}
	}

	public void onCopyAgain() {
		this.selectedLights = prevSelectedLights;

		int selectedLight2 = getLightSelected();
		if (selectedLight2 > 0) {
			System.out.println("Copying light settings");
			System.out.println("First light selected:" + selectedLights.get(0));
			System.out.println("Second light selected: " + selectedLight2);
			lightUnit.alertLight(selectedLight2);
			this.jsonStateBefore = lightUnit.getJsonState();
			String state1 = lightUnit.getJsonState(this.selectedLights.get(0));
			lightUnit.setJsonState(selectedLight2, state1);
			prevSelectedLights = this.selectedLights;
			selectedLights = new ArrayList<Integer>();
		} else {
			System.out.println("no light selected");
		}
	}

	// ------------------------- SIMPLE OPEN NI FUNCTIONS
	// ----------------------------

	public void drawIntersection(int userId) {
		PVector jointPos1 = new PVector();
		PVector jointPos2 = new PVector();
		// call to get skeleton data from the cam (returns confidence)
		float c1 = camera.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, jointPos1);
		float c2 = camera.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_HAND, jointPos2);
		PVector dir = PVector.sub(jointPos2, jointPos1);
		dir.normalize();
		PVector rayEnd = PVector.mult(dir, 4000); // direction 4m
		rayEnd.add(jointPos2);

		pushStyle(); // style layer: modifications of style until popStyle
		stroke(255);
		strokeWeight(10);
		PVector temp1 = new PVector();
		PVector temp2 = new PVector();
		camera.convertRealWorldToProjective(jointPos2, temp1);
		camera.convertRealWorldToProjective(rayEnd, temp2);
		line(temp1.x, temp1.y, temp2.x, temp2.y);

		for (Light light : lights) {
			if (intersectionLight(jointPos2, dir, light.getCoor3D(), 0)) {
				lightSelected = light.getNumber();
			}
		}

		popStyle();
	}

	public boolean intersectionLight(PVector joint, PVector direction, PVector lightCoor, int c) {
		PVector hit1 = new PVector();
		PVector hit2 = new PVector();
		// raySphereIntersection: origin, direction, a sphere target with radius
		// Output: two vector of intersection with the sphere ->( )->
		int intersectionSphere = SimpleOpenNI.raySphereIntersection(joint, direction, lightCoor, sphereRadius, hit1,
				hit2);
		if (intersectionSphere > 0) {
			PVector temp = new PVector();
			camera.convertRealWorldToProjective(lightCoor, temp);
			ellipse(temp.x, temp.y, 50, 50);
			return true;
		} else {
			return false;
		}
	}

	// SimpleOpenNI events
	public void onNewUser(SimpleOpenNI curcamera, int userId) {
		System.out.println("onNewUser - userId: " + userId);
		System.out.println("\tstart tracking skeleton");
		camera.startTrackingSkeleton(userId);
	}

	public void onLostUser(SimpleOpenNI curContext, int userId) {
		System.out.println("onLostUser - userId: " + userId);
	}

	// --------------------------- OTHER --------------------------------------

	public int getLightSelected() {
		return lightSelected;
	}

	public void onCorrectionTrigger() {
		int currentSelected = getLightSelected();
		if (currentSelected != 0) {
			restorePreviousState();
			selectedLights.remove(lastLightAdded);
			lightUnit.alertLight(currentSelected);
			selectedLights.add(currentSelected);
			lastLightAdded = currentSelected;
		} else {
			System.out.println("no light selected");
		}
	}

	private void restorePreviousState() {
		lightUnit.setJsonState(this.jsonStateBefore);
	}

	private void updatePreviousState() {
		this.jsonStateBefore = lightUnit.getJsonState();
	}

	public void onLoopStop() {
		for (int id : selectedLights) {
			lightUnit.stopColorloop(id);
		}
	}

	public void onLoopStart() {
		for (int id : selectedLights) {
			lightUnit.startColorloop(id);
		}
	}

	public void onPreset(Preset preset) {
		lightUnit.loadPreset(selectedLights, preset);
		afterAction();
	}

	public void onLoadTrigger() {
		beforeAction();
		for (int id : lightsInUse) {
			selectedLights.add(id);
		}
	}

	private void beforeAction() {
		updatePreviousState();
	}

	private void afterAction() {
		prevSelectedLights = selectedLights;
		selectedLights = new ArrayList<>();
	}
}
