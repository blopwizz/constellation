package constellation;

import java.util.ArrayList;
import constellation.SpeechUnit.Preset;

public class LightUnit {

	private boolean hueActivated = true;
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
				return hue.getJsonStatus();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return "";
	}

	public void setJsonState(String payload) {
		System.out.println("Triggered all lights with a specific state.");
		if (hueActivated) {
			try {
				hue.setJsonStatus(payload);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String getJsonState(int id) {
		if (hueActivated) {
			try {
				return hue.getLightJson(id);
			} catch (Exception e) {
				e.printStackTrace();

			}
		}
		return "";
	}

	public void setJsonState(int id, String payload) {
		System.out.println("Triggered lights: " + id + " with setting a specific setting.");

		if (hueActivated) {
			try {
				hue.setLightJson(id, payload);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void changeBrightness(int id, String change) {
		System.out.println("Triggered lights: " + id + " with Command Change Brightness by " + change + " .");
		if (hueActivated) {
			try {
				hue.incBri(id, change);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void alertLight(int id) {
		System.out.println("Triggered lights: " + id + " with Command alert");
		if (hueActivated) {
			try {
				hue.alert(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void startColorloop(int id) {
		System.out.println("Triggered lights: " + id + " with Command colorloop on");
		if (hueActivated) {
			try {
				hue.colorloopOn(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void stopColorloop(int id) {
		System.out.println("Triggered lights: " + id + " with Command colorloop off");
		if (hueActivated) {
			try {
				hue.colorloopOff(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void loadPreset(ArrayList<Integer> lightIds, Preset preset) {
		System.out.println("Triggered lights: " + lightIds + " with Preset" + preset);
		if (hueActivated) {
			try {
				for (int id : lightIds) {
					switch (preset) {
					case CLEANING: // maximum white
						hue.setAll(id, "0", "0", "254");
						break;
					case PARTY: // random changing colors at half brightness
						hue.setAll(id, (int) (Math.random() * 65535) + "", "254", "125");
						hue.colorloopOn(id);
						break;
					case ROMANCE: // red half brightness
						hue.setAll(id, "0", "200", "120");
						break;
					case WORK: // mostly white with a little yellow
						hue.setAll(id, "12750", "50", "254");
						break;
					default:
						System.out.println("unrecognized preset");
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
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
						hue.setSat(light.intValue(), "0");
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
