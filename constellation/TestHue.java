package constellation;


public class TestHue {

	public static void main(String[] args) {
		try {

			//String username = "1028d66426293e821ecfd9ef1a0731df";
			String username = "3FCBC5219152E94C7B998679E5FCCA15";
			String url = "http://10.0.0.2/api/";

			System.out.println("beginning test");
			HueControl hue = new HueControl(username, url);

			//Status
			hue.printStatus();
			
			hue.setSat("5", "254");
			hue.setBri("5", "254");
			hue.setHue("5", "0"); // red
			
			//blinking		
			int blinkCounter = 2;
			System.out.println("\nwill  now turn light 5 on/off " + blinkCounter + " times");
			while (true) {
				System.out.println("run "+blinkCounter);
				hue.turnLightOn("5");
				Thread.sleep(1000);
				hue.turnLightOff("5");
				Thread.sleep(1000);
				blinkCounter--;
				if (blinkCounter <= 0)
					break;
			}

			// colors
			hue.turnLightOn("5");
			System.out.println("\nchanging through colors");
			int[] colors = { 0, 12750, 25500, 46920, 56100, 65280 };
			for (int i = 0; i < colors.length; i++) {
				System.out.println("setting color "+ String.valueOf(colors[i]));
				hue.setHue("5", String.valueOf(colors[i])); // red
				Thread.sleep(1000);
			}
			
			System.out.println("config:");
			hue.printConfig();
			
			Thread.sleep(1000);
			System.out.println("\n3 alerts");
			hue.alert("5");
			Thread.sleep(1000);
			hue.alert("5");
			Thread.sleep(1000);
			hue.alert("5");
			Thread.sleep(1000);
			
			System.out.println("Colorloop for 30 sec");
			hue.colorloopOn("5");
			Thread.sleep(30000);
			hue.colorloopOff("5");
			
			System.out.println("done");
		} catch (

		Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
