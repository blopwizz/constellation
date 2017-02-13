package constellation;

import java.util.ArrayList;

public class LightUnit {

	private boolean hueActivated = false;
	private HueControl hue;

	public LightUnit() {
		String username = "3FCBC5219152E94C7B998679E5FCCA15";
		String url = "http://10.0.0.2/api/";

		hue = new HueControl(username, url);
		System.out.println("Light Unit initialized");
	}

	public String getJsonState() {
		try {
			return hue.getJsonStatus();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	public String getJsonState(int id) {
		try {
			return hue.getLightJson(id);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}

	public void setJsonState(int id, String payload) {
		try {
			hue.setLightJson(id, payload);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void alertLight(int id) {
		try {
			hue.alert(id);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public void performAction(ArrayList<Integer> selectedLights, SpeechUnit.Command command) {
		System.out.println("Triggered lights: " + selectedLights + " with Command " + command + " .");
		if (hueActivated) {
			for (Integer light : selectedLights) {
				try {

					switch (command) {
					case BLUE:
						hue.setHue(light.intValue(), "46920");
						hue.setSat(light.intValue(), "255");
						break;
					case RED:
						hue.setHue(light.intValue(), "0");
						hue.setSat(light.intValue(), "255");
						break;
					case GREEN:
						hue.setHue(light.intValue(), "25500");
						hue.setSat(light.intValue(), "255");
						break;
					case YELLOW:
						hue.setHue(light.intValue(), "12750");
						hue.setSat(light.intValue(), "255");
						break;
					case WHITE:
						hue.setSat(light.intValue(), "0");// TODO
						break;
					case PURPLE:
						hue.setHue(light.intValue(), "46920");
						hue.setSat(light.intValue(), "255");
						break;
					case RANDOM:
						hue.setHue(light.intValue(), "" + (int) (Math.random() * 65280));
						hue.setSat(light.intValue(), "255");
						break;
					case ON:
						hue.turnLightOn(light.intValue());
						break;
					case OFF:
						hue.turnLightOff(light.intValue());
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
}
