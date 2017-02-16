package constellation;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;

import java.io.IOException;

public class SpeechUnit implements Runnable {

	private final Launcher main;
	private State state;

	private final static String ACOUSTIC_MODEL_PATH = "resource:/edu/cmu/sphinx/models/en-us/en-us";
	private final static String DICTIONARY_PATH = "file:src/cmudict-mi.dict";
	private final static String LANGUAGE_MODEL_PATH = "resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin";

	private final static String[] ACTIVATION_STRING = { "constellation" };
	private final static String CLOSE_STRING = "close";
	private final static String[] SINGLE_LIGHT_SELECTER = { "switch that", "switch this", "select that", "select that",
			"make that", "make this" };
	private final static String[] ALL_LIGHTS_SELECTER = { "switch all", "make all", "turn all", "selecct all" };
	private final static String COPY_STRING = "copy";
	private final static String[] COPY_FINISH = { "there" };
	private final static String[] COPY_ADDITIONAL = { "and there" };
	private final static String[] UNDO_STRINGS = { "undo", "revert" };
	private final static String[] CORRECTION_STRINGS = { "no that9", "no this" };
	private final static String[] ADD_STRING = { "and that", "and this" };

	private boolean lastActionCopy = false;
	
	private enum State {
		IDLE, ACTIVATED, LIGHT_CHOSEN, COPY_CHOSEN;
	}

	public enum Command {
		BLUE("blue"), RED("red"), GREEN("green"), YELLOW("yellow"), WHITE("white"), PURPLE("purple"), ON("on"), OFF(
				"off"), BRIGHTER("brighter"), LIGHTER(
						"lighter"), ADD("and that"), RANDOM("random"), UNDEFINED_COMMAND("undefined");

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
			while (true) {
				SpeechResult result = recognizer.getResult();
				System.out.println(result.getHypothesis());
				switch (state) {
				case IDLE:
					if (isActivation(result)) {
						lastActionCopy = false;
						this.state = State.ACTIVATED;
					}
					break;
				case ACTIVATED:
					if (isSingleLightSelection(result)) {
						lastActionCopy = false;
						if (onSelectionTrigger())
							this.state = State.LIGHT_CHOSEN;
					} else if (isAllLightSelection(result)) {
						lastActionCopy = false;
						this.state = State.LIGHT_CHOSEN;
						onAllSelectionTrigger();
					} else if (isUndo(result)) {
						lastActionCopy = false;
						this.state = State.ACTIVATED;
						onUndoTrigger();
					} else if (isCopy(result)) {
						lastActionCopy = false;
						this.state = State.COPY_CHOSEN;
						onCopyTrigger();
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
						break;
					}
					Command returnCommand = getCommand(result);
					if (Command.UNDEFINED_COMMAND != returnCommand) {
						if (returnCommand == Command.ADD) {
							onSelectionTrigger();
						} else {
							onCommand(returnCommand);
							this.state = State.ACTIVATED;
						}
					}
					break;
				case COPY_CHOSEN:
					if (isCopyFinish(result)) {
						main.onCopy2Trigger();
						this.state = State.ACTIVATED;
						lastActionCopy = true;
					}
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

	private void onCorrectionTrigger() {
		main.onCorrectionTrigger();		
	}

	private void onCopyAgain() {
		main.onCopyAgain();	
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
		return result.getHypothesis().contains(COPY_STRING);
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
		// TODO: replace with Java8 Streams
		for (String word : array) {
			if (hypothesis.contains(word)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsClose(SpeechResult result) {
		return result.getHypothesis().contains(CLOSE_STRING);
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
		this.state = State.IDLE;
		main.onClose();
	}

	public SpeechUnit(Launcher main) {
		this.state = State.IDLE;
		this.main = main;
	}

}