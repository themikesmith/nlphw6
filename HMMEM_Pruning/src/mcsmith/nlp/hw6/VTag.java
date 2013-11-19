package mcsmith.nlp.hw6;

import java.io.IOException;

import mcsmith.nlp.hw6.TagDict.SMOOTHING;

public class VTag {
	/**
	 * Prints an example of proper usage of the program
	 */
	private static void usage(String[] args) {
		System.err.println("usage: requires 3 arguments.\n" +
				"arg[0] = training_file\n" +
				"arg[1] = test_file\n" +
				"arg[2] = raw_file\n" +
				"Defaults to super aggresive, but one may specify an optional 4th argument:\n" +
				" arg[3] = -a to reduce aggression mode, or -aa to keep it super aggressive." + 
				"Defaults to off, but one may specify an optional 5th argument:\n" +
				" arg[4] = -d to enable debug mode.");
		System.err.printf("\nyou submitted:\n");
		for(String s : args) System.err.printf("%s\n",s);
	}

	public static void main(String[] args) {
		// check arguments
		if (args.length < 3 || args.length > 5) {
			usage(args);
			return;
		}
		boolean debugMode = false, superAggresive = true;
		if (args.length > 3) { // check our optional arguments.
			// check the 4th argument
			if (args[3].equals("-a")) {
				superAggresive = false;
			} else if(args[3].equals("-aa")) {
				superAggresive = true;
			} else {
				usage(args);
				return;
			}
			if(args.length > 4) {
				if (args[4].equals("-d")) {
					debugMode = true;
				} else {
					usage(args);
					return;
				}
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
		try {
			vtag.prepareToTest(args[1], args[2]);
			vtag.setPruningLevel(superAggresive);
			// run viterbi on test data
			vtag.test(false);
			// for this many iterations....
			int maxIterations = 11;
			for(int i = 0; i < maxIterations; i++) {
				System.out.printf("\nIteration %d: ",i);
				// then, using raw data, re-estimate training counts with forward backward EM
				vtag.test(true);
				// run viterbi on test data
				vtag.test(false);
			}
		} catch (IOException e) {
			System.err.println("error testing!\n");
			e.printStackTrace();
		}
	}
}

