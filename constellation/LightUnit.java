package constellation;

import java.util.ArrayList;

public class LightUnit {

	private boolean hueActivated = false;
	private HueControl hue;
	
	

	public LightUnit() {
		String username = "3FCBC5219152E94C7B998679E5FCCA15";
		String url = "http://10.0.0.2/api/";

		hue = new HueControl(username, url);
		System.out.print("Light Unit initialized ");
		if (hueActivated) {
			System.out.println("with Hue");
		} else {
			System.out.println("without Hue");
		}
	}

	public String getJsonState() {
		if (hueActivated) {
			try {
				System.out.println("debug 1");
				return hue.getJsonStatus();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("debug 2");

			}
		}
		return "";
	}

	public void setJsonState(String payload) {
		if (hueActivated) {
			try {
				hue.setJsonStatus(payload);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public String getJsonState(int id) {
		System.out.println("debug 3");

		if (hueActivated) {
			try {
				return hue.getLightJson(id);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
		}
		return "";
	}

	public void setJsonState(int id, String payload) {
		System.out.println("debug 4");

		if (hueActivated) {
			try {
				hue.setLightJson(id, payload);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void changeBrightness(int id, String change) {
		System.out.println("Adding "+change+" to brightnesslevel.");
		try {
			hue.incBri(id, change);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void alertLight(int id) {
		if (hueActivated) {
			try {
				hue.alert(id);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	public void performAction(ArrayList<Integer> selectedLights, SpeechUnit.Command command) {
		System.out.println("Triggered lights: " + selectedLights + " with Command " + command + " .");
		if (hueActivated) {
			for (Integer light : selectedLights) {
				System.out.println("debug 5 "+ light);

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
						hue.incBri(light, "+50");
						break;
					case DARKER:
						hue.incBri(light, "-50");
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
