package constellation;

public class LightUnit {

	private boolean hueActivated = false;
	private HueControl hue;

	public LightUnit() {
		String username = "3FCBC5219152E94C7B998679E5FCCA15";
		String url = "http://10.0.0.2/api/";

		hue = new HueControl(username, url);
		System.out.println("Light Unit initialized");
	}

	public void performAction(int lightID, SpeechUnit.Command command) {
		System.out.println("Triggered light: " + lightID + " with Command " + command + " .");
		if (hueActivated) {
			try {

				switch (command) {
				case BLUE:
					hue.setHue(lightID, "46920");
					break;
				case RED:
					hue.setHue(lightID, "0");
					break;
				case GREEN:
					hue.setHue(lightID, "25500");
					break;
				case YELLOW:
					hue.setHue(lightID, "12750");
					break;
				case WHITE:
					hue.setHue(lightID, "46920");// TODO
					break;
				case PURPLE:
					hue.setHue(lightID, "46920");
					break;
				case ON:
					hue.turnLightOn(lightID);
					break;
				case OFF:
					hue.turnLightOff(lightID);
					break;
				case BRIGHTER:
					// TODO
					break;
				case LIGHTER:
					// TODO
					break;
				default:
					System.out.println("unknown command");
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}
