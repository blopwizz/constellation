package constellation;

import constellation.Launcher.Light;

public class GestureUnit implements Runnable{

	@Override
	public void run() {
		System.out.println("Starting gesture unit.");
		Launcher l = new Launcher();
		l.setup();
		l.draw();
	}

	Light determineLight(){
		//TODO: Actual code to determine the light
		System.out.println("Determining light selection.");
		if (LaserFrame.lightSelected == 1) {return Light.L1;}
		else if (LaserFrame.lightSelected == 2) {return Light.L2;}
     	else if (LaserFrame.lightSelected == 3) {return Light.L3;}
		return null;
	}
}
