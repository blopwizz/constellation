
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
*------------------------------------------------------
*[TO DO]
* - fix the calibration mode to do it several times if needed
* ------------------------------------------------------
* 
* Coder: Jorg, Frederic, Stephane
* Date: February 2017
* Organisation: TU Berlin
*/

package constellation;

import java.util.HashMap;

import SimpleOpenNI.SimpleOpenNI;

import java.util.ArrayList;
import constellation.SpeechUnit.Command;
import processing.core.*;

public class Launcher extends PApplet {
	
	/*************************************************************
	 * serialization (required by java on this class)
	 */
	private static final long serialVersionUID = 1505207865812651811L;

	/*
	 * Here begins Processing sketch
	 */

	public static void main(String[] args) {
		PApplet.main("constellation.Launcher");
	}

	public static PApplet parent;
	public static SimpleOpenNI context;
	private CalibFrame calibWindow;
	private LaserFrame laserWindow;
	private Button buttonCalib, buttonLaser;

	private SpeechUnit voice;
	private LightUnit light;

	private Command command;

	private State state = State.IDLE;
	private boolean shouldStop = false;

	HashMap<String, PVector> map;

	/*
	 * private SpeechUnit.Command command;
	 */
	private String jsonStateBefore = ""; // for undo
	private ArrayList<Integer> prevSelectedLights;
	private ArrayList<Integer> selectedLights;
	


	private enum State {
		IDLE, WAITING_FOR_COMMAND, INSTRUCTED, COPY_WAITING;
	}

	public void setup() {
		size(640, 480);
		frameRate(15);
		parent = new PApplet();
		context = new SimpleOpenNI(parent);
		buttonCalib = new Button(this, "Start Calibration", width / 2, height / 3, 200, 100);
		buttonLaser = new Button(this, "Start Light Control", width / 2, 2 * height / 3, 200, 100);
		voice = new SpeechUnit(this);
		initialize();
	}

	public void draw() {
		background(255);
		buttonCalib.display();
		buttonLaser.display();

		switch (this.state) {
		case IDLE:
			break;
		case WAITING_FOR_COMMAND:
			break;
		case INSTRUCTED:
			this.jsonStateBefore = light.getJsonState();
			light.performAction(this.selectedLights, this.command);
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

	// mouse button clicked
	public void mousePressed() {
		if (buttonCalib.mouseIsOver()) {
			// calibration info
			User.lightsVectors[0] = new PVector  (-2197.7861f,  640.2997f, 9870.0f);
			User.lightsVectors[1] = new PVector (-1751.3065f,  1342.899f, 3948.0f);
			User.lightsVectors[2] = new PVector ( 2718.128f, 1205.9951f, 5291.0f);
			User.lightsVectors[3] = new PVector   (2630.421f, 224.97023f, 9870.0f);
			User.lightsVectors[4] = new PVector    (-1260.5065f, 441.17722f, 3994.0f);
			User.lightsVectors[5] = new PVector     (1033.5884f, 498.20435f, 4241.0f);
			User.lightsVectors[6] = new PVector     (-136.5742f, 1012.2554f, 4582.0f);
			calibWindow = new CalibFrame(0, 0, 640, 480, "Calibration");
		}

		if (buttonLaser.mouseIsOver()) {
			println("got calib data");
			map = calibWindow.getMap();
			laserWindow = new LaserFrame(0, 0, 1024, 768, "Laser Beam", map);
		}
	}

	boolean onSelectionTrigger() {
		if (laserWindow != null) {
			int selectedLight = laserWindow.getLightSelected();
			if (selectedLight > 0) {
				System.out.println("Last light selected: " + selectedLight);
				this.selectedLights.add(selectedLight);
				light.alertLight(selectedLight);
				switchState(State.WAITING_FOR_COMMAND);
				return true;
			} else {
				System.out.println("no light selected");
			}
		}
		return false;
	}

	void onClose() {
		switchState(State.IDLE);
	}

	void onCommand(SpeechUnit.Command c) {
		command = c;
		switchState(State.INSTRUCTED);
	}

	private void switchState(State state) {
		System.out.println("Switching state to:" + state);
		this.state = state;
	}

	private void initialize() {
		if (User.SPEECH_ACTIVE) {
			(new Thread(this.voice)).start();
		}
		this.prevSelectedLights = new ArrayList<Integer>();
		this.selectedLights = new ArrayList<Integer>();
		light = new LightUnit();
		light.setHueActivation(User.HUE_ACTIVE);
	}

	public void onAllSelectionTrigger() {
		for (int i = 1; i < 10; i++) {
			selectedLights.add(i);
			light.alertLight(i);
		}
		switchState(State.WAITING_FOR_COMMAND);
	}

	public void undoLast() {
		System.out.println("Reverting last change.");
		light.setJsonState(jsonStateBefore);
	}

	public void onCopyTrigger() {
		if (laserWindow != null) {
			int selectedLight = laserWindow.getLightSelected();
			if (selectedLight > 0) {
				System.out.println("Last light selected: " + selectedLight);
				this.selectedLights.add(selectedLight);
				light.alertLight(selectedLight);
				switchState(State.COPY_WAITING);
			} else {
				System.out.println("no light selected");
			}
		}
	}

	public void onPasteTrigger() {
		if (laserWindow != null) {
			int selectedLight2 = laserWindow.getLightSelected();
			if (selectedLight2 > 0) {
				System.out.println("Copying light settings");
				System.out.println("First light selected:" + selectedLights.get(0));
				System.out.println("Second light selected: " + selectedLight2);
				light.alertLight(selectedLight2);
				switchState(State.IDLE);
				this.jsonStateBefore = light.getJsonState();
				String state1 = light.getJsonState(this.selectedLights.get(0));
				light.setJsonState(selectedLight2, state1);
				prevSelectedLights = this.selectedLights;
				selectedLights=new ArrayList<Integer>();
			} else {
				System.out.println("no light selected");
			}
		}
	}

	public void onCopyAgain() {
		this.selectedLights = prevSelectedLights;
		if (laserWindow != null) {
			int selectedLight2 = laserWindow.getLightSelected();
			if (selectedLight2 > 0) {
				System.out.println("Copying light settings");
				System.out.println("First light selected:" + selectedLights.get(0));
				System.out.println("Second light selected: " + selectedLight2);
				light.alertLight(selectedLight2);
				switchState(State.IDLE);
				this.jsonStateBefore = light.getJsonState();
				String state1 = light.getJsonState(this.selectedLights.get(0));
				light.setJsonState(selectedLight2, state1);
				prevSelectedLights = this.selectedLights;
				selectedLights=new ArrayList<Integer>();
			} else {
				System.out.println("no light selected");
			}
		}
	}

	public void onCorrectionTrigger() {
		if(State.WAITING_FOR_COMMAND==state) {

		}
	}
	
	
	public void setParentPApplet(PApplet p) {
		parent = p;
	}
	
	public PApplet getParentPapplet() {
		return parent;
	}
}
