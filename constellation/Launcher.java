
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

import java.util.HashMap;
import java.util.ArrayList;
import constellation.SpeechUnit.Command;
import processing.core.*;

public class Launcher extends PApplet {
	/**
	 * serialization (required by java on this class)
	 */
	private static final long serialVersionUID = 1505207865812651811L;

	/*
	 * Here begins Processing sketch
	 */

	public static void main(String[] args) {
		PApplet.main("constellation.Launcher");
	}

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
	private String jsonStateBefore = ""; //for undo
	private ArrayList<Integer> prevSelectedLights;
	private ArrayList<Integer> selectedLights;

	private enum State {
		IDLE, WAITING_FOR_COMMAND, INSTRUCTED, COPY_WAITING;
	}

	public void setup() {
		size(640, 480);
		frameRate(15);
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
			calibWindow = new CalibFrame(0, 0, 640, 480, "Calibration");
		}

		if (buttonLaser.mouseIsOver()) {
			// map = calibWindow.getMap();
			laserWindow = new LaserFrame(0, 0, 1024, 768, "Laser Beam");
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
		// this.shouldStop = true;
		// TO DO: exit system
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
		(new Thread(this.voice)).start();
		this.prevSelectedLights = new ArrayList<Integer>();
		this.selectedLights = new ArrayList<Integer>();
		light = new LightUnit();
	}

	public void onAllSelectionTrigger() {
		for (int i = 1; i < 10; i++) {
			selectedLights.add(i);
			light.alertLight(i);
		}
		switchState(State.WAITING_FOR_COMMAND);
	}

	public void undoLast() {
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

	public void onCopy2Trigger() {
		if (laserWindow != null) {
			int selectedLight2 = laserWindow.getLightSelected();
			if (selectedLight2 > 0) {
				System.out.println("Last light selected: " + selectedLight2);
				this.selectedLights.add(selectedLight2);
				light.alertLight(selectedLight2);
				switchState(State.IDLE);
				String state1 = light.getJsonState(this.selectedLights.get(0));
				light.setJsonState(selectedLight2, state1);
			} else {
				System.out.println("no light selected");
			}
		}
		
	}

}
