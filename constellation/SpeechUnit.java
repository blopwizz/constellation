package constellation;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;

import java.io.IOException;

public class SpeechUnit implements Runnable {

	private static final long STATE_TIMEOUT = 30 * 1000;
	private final Launcher main;
	private State state;

	private final static String ACOUSTIC_MODEL_PATH = "resource:/edu/cmu/sphinx/models/en-us/en-us";
	private final static String DICTIONARY_PATH = "file:src/cmudict-mi.dict";
	private final static String LANGUAGE_MODEL_PATH = "resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin";

	private final static String[] ACTIVATION_STRING = { "constellation" };
	private final static String[] CLOSE_STRING = { "close" };
	private final static String[] SINGLE_LIGHT_SELECTER = { "switch that", "switch this", "select that", "select that",
			"make that", "make this" };
	private final static String[] ALL_LIGHTS_SELECTER = { "switch all", "make all", "turn all", "selecct all" };
	private final static String[] COPY_STRING = { "copy" };
	private final static String[] COPY_FINISH = { "there" };
	private final static String[] COPY_ADDITIONAL = { "and there" };
	private final static String[] UNDO_STRINGS = { "undo", "revert" };
	private final static String[] CORRECTION_STRINGS = { "no that", "no this" };
	private final static String[] ADD_STRING = { "and that", "and this" };
	private final static String[] LOOP_START = { "loop" };
	private final static String[] LOOP_END = { "stop" };
	private final static String[] LOAD_STRING = { "load" };

	private boolean lastActionCopy = false;

	private enum State {
		IDLE, ACTIVATED, LIGHT_CHOSEN, COPY_CHOSEN, COLORLOOP, LOAD;
	}

	public enum Preset {
		WORK("work"), CLEANING("cleaning"), ROMANCE("romance"), PARTY("party"), UNDEFINED("undefined");
		private final String word;

		Preset(String word) {
			this.word = word;
		}

		public String getWord() {
			return word;
		}
	}

	public enum Command {
		BLUE("blue"), RED("red"), GREEN("green"), YELLOW("yellow"), WHITE("white"), PURPLE("purple"), ON("on"), OFF(
				"off"), BRIGHTER("brighter"), DARKER(
						"darker"), ADD("and that"), RANDOM("random"), UNDEFINED_COMMAND("undefined");

		private final String word;

		Command(String word) {
			this.word = word;
		}

		public String getWord() {
			return word;
		}
	}

	@Override
	public void run() {
		Configuration configuration = new Configuration();

		configuration.setAcousticModelPath(ACOUSTIC_MODEL_PATH);
		configuration.setDictionaryPath(DICTIONARY_PATH);
		configuration.setLanguageModelPath(LANGUAGE_MODEL_PATH);

		System.out.print("Starting recognition....");

		try {
			LiveSpeechRecognizer recognizer = new LiveSpeechRecognizer(configuration);
			recognizer.startRecognition(true);
			System.out.println("Done");
			long lastStateChange = getCurrentMillis();
			while (true) {
				if (shouldSwitchToIdle(lastStateChange)) {
					System.out.println(
							"After " + STATE_TIMEOUT + " milliseconds of no command, we switch back to Idle state.");
					onClose();
				}
				SpeechResult result = recognizer.getResult();
				System.out.println(result.getHypothesis());
				switch (state) {
				case IDLE:
					lastActionCopy = false;
					if (isActivation(result)) {
						switchState(State.ACTIVATED);
					}
					break;
				case ACTIVATED:
					if (isSingleLightSelection(result)) {
						lastActionCopy = false;
						if (onSelectionTrigger()) {
							switchState(State.LIGHT_CHOSEN);
						}
					} else if (isAllLightSelection(result)) {
						lastActionCopy = false;
						switchState(State.LIGHT_CHOSEN);
						onAllSelectionTrigger();
					} else if (isUndo(result)) {
						lastActionCopy = false;
						switchState(State.ACTIVATED);
						onUndoTrigger();
					} else if (isCopy(result)) {
						lastActionCopy = false;
						switchState(State.COPY_CHOSEN);
						onCopyTrigger();
					} else if (isLoad(result)) {
						lastActionCopy = false;
						onLoadTrigger();
						switchState(State.LOAD);
					} else if (lastActionCopy && isCopyAdditional(result)) {
						onCopyAgain();
					}
					break;
				case LIGHT_CHOSEN:
					lastActionCopy = false;
					if (isAdd(result)) {
						onSelectionTrigger();
						break;
					} else if (isCorrection(result)) {
						onCorrectionTrigger();
						switchState(State.COLORLOOP);
						break;
					} else if (isLoopStart(result)) {
						onLoopTrigger();
						break;
					}
					Command returnCommand = getCommand(result);
					if (Command.UNDEFINED_COMMAND != returnCommand) {
						if (returnCommand == Command.ADD) {
							onSelectionTrigger();
						} else {
							onCommand(returnCommand);
							switchState(State.ACTIVATED);
						}
					}
					break;
				case COPY_CHOSEN:
					lastActionCopy = false;
					if (isCopyFinish(result)) {
						onPasteTrigger();
						switchState(State.ACTIVATED);
						lastActionCopy = true;
					}
					break;
				case COLORLOOP:
					lastActionCopy = false;
					if (isLoopStop(result)) {
						onLoopStopTrigger();
						switchState(State.ACTIVATED);
					}
					break;
				case LOAD:
					lastActionCopy = false;
					Preset returnPreset = getPreset(result);
					if (Preset.UNDEFINED != returnPreset) {
						onPreset(returnPreset);
						switchState(State.ACTIVATED);
					}
					break;
				default:
					break;
				}

				if (containsClose(result)) {
					onClose();
				}

			}
			// recognizer.stopRecognition();

		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("SpeechUnit closed");
	}

	private boolean shouldSwitchToIdle(long lastStateChange) {
		return STATE_TIMEOUT < (getCurrentMillis() - lastStateChange);
	}

	private long getCurrentMillis() {
		return Math.round(System.nanoTime() / 10 ^ 6);
	}

	private void onLoadTrigger() {
		main.onLoadTrigger();
	}

	private boolean isLoad(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, LOAD_STRING);
	}

	private void onPreset(Preset preset) {
		System.out.println("You have chosen the preset: " + preset);
		main.onPreset(preset);
	}

	private Preset getPreset(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		for (Preset preset : Preset.values()) {
			if (hypothesis.contains(preset.getWord())) {
				return preset;
			}
		}
		return Preset.UNDEFINED;
	}

	private void onLoopStopTrigger() {
		main.onLoopStop();
	}

	private boolean isLoopStop(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, LOOP_END);
	}

	private void onLoopTrigger() {
		main.onLoopStart();
	}

	private boolean isLoopStart(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, LOOP_START);
	}

	private void onCorrectionTrigger() {
		main.onCorrectionTrigger();
	}

	private void onCopyAgain() {
		main.onCopyAgain();
	}

	private void onPasteTrigger() {
		main.onPasteTrigger();
	}

	public void onCopyTrigger() {
		main.onCopyTrigger();
	}

	private void onUndoTrigger() {
		main.undoLast();
	}

	private Command getCommand(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		for (Command command : Command.values()) {
			if (hypothesis.contains(command.getWord())) {
				return command;
			}
		}
		return Command.UNDEFINED_COMMAND;
	}

	private void switchState(SpeechUnit.State state) {
		System.out.println("SpeechUnit: switching to State " + state.name());
		this.state = state;
	}

	private boolean isSingleLightSelection(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, SINGLE_LIGHT_SELECTER);
	}

	private boolean isAllLightSelection(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, ALL_LIGHTS_SELECTER);
	}

	private boolean isActivation(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, ACTIVATION_STRING);
	}

	private boolean isCopy(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, COPY_STRING);
	}

	private boolean isAdd(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, ADD_STRING);
	}

	private boolean isUndo(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, UNDO_STRINGS);
	}

	private boolean isCopyFinish(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, COPY_FINISH);
	}

	private boolean isCopyAdditional(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, COPY_ADDITIONAL);
	}

	private boolean isCorrection(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, CORRECTION_STRINGS);
	}

	private boolean stringContainsArrayEntry(String hypothesis, String[] array) {
		for (String word : array) {
			if (hypothesis.contains(word)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsClose(SpeechResult result) {
		String hypothesis = result.getHypothesis();
		return stringContainsArrayEntry(hypothesis, CLOSE_STRING);
	}

	private boolean onSelectionTrigger() {
		System.out.println("You have triggered the light selection.");
		return main.onSelectionTrigger();
	}

	private void onAllSelectionTrigger() {
		System.out.println("You have selected all lights.");
		main.onAllSelectionTrigger();
	}

	private void onCommand(Command command) {
		System.out.println("You have chosen the command: " + command);
		main.onCommand(command);
	}

	private void onClose() {
		System.out.println("Received close. Aborting command.");
		switchState(State.IDLE);
		main.onClose();
	}

	public SpeechUnit(Launcher main) {
		switchState(State.IDLE);
		this.main = main;
	}

}