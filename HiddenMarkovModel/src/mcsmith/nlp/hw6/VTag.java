package mcsmith.nlp.hw6;

import java.io.IOException;

import mcsmith.nlp.hw6.TagDict.SMOOTHING;

public class VTag {
	/**
	 * Prints an example of proper usage of the program
	 */
	private static void usage(String[] args) {
		System.err.println("usage: requires 2 arguments.\n" +
				"arg[0] = training_file\n" +
				"arg[1] = test_file\n" +
				"Defaults to off, but one may specify an optional 3rd argument:\n" +
				" arg[2] = -d to enable debug mode.");
		System.err.printf("\nyou submitted:\n");
		for(String s : args) System.err.printf("%s\n",s);
	}

	public static void main(String[] args) {
		// check arguments
		if (args.length < 2 || args.length > 4) {
			usage(args);
			return;
		}
		boolean debugMode = false;
		if (args.length > 2) { // check our optional arguments.
			// check the 3rd argument
			if (args[2].equals("-d")) {
				debugMode = true;
			} else {
				usage(args);
				return;
			}
		}
		// now that we have our arguments...
		ViterbiTagger vtag = new ViterbiTagger();
		vtag.setDebugMode(debugMode);
		TagDict.setDebugMode(debugMode);
		vtag.getTagDict().setSmoother(SMOOTHING.oneCountSmoothing);
		try {
			vtag.train(args[0]);
		} catch (IOException e) {
			System.err.println("error training!\n");
			e.printStackTrace();
		}
		// debug train!
		if(debugMode) System.out.println(vtag.getTagDict().toString());
		try {
			// viterbi
			vtag.test(args[1], false);
			// forward backward
			vtag.test(args[1], true);
		} catch (IOException e) {
			System.err.println("error testing!\n");
			e.printStackTrace();
		}
	}
}

