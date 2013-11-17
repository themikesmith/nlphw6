package mcsmith.nlp.hw6;

import java.io.IOException;

public class VTag {
	/**
	 * Prints an example of proper usage of the program
	 */
	private static void usage(String[] args) {
		System.err.println("usage: requires 2 arguments.\narg[0] = training_file\n" +
				"arg[1] = test_file\n" +
				"Defaults to max-product, but one may specify an optional 3rd argument:\n" +
				" arg[2] = 's' or 'm' for sum- or max-product\n" +
				"Defaults to off, but one may specify an optional 4th argument:\n" +
				" arg[3] = -d to enable debug mode.");
		System.err.printf("\nyou submitted:\n");
		for(String s : args) System.err.printf("%s\n",s);
	}

	public static void main(String[] args) {
		// check arguments
		if (args.length < 2 || args.length > 4) {
			usage(args);
			return;
		}
		boolean useSumProduct = false, debugMode = false;
		if (args.length > 2) { // check our optional arguments.
			// check the 3rd argument
			if (args[2].equals("-s")) {
				useSumProduct = true;
			} else if (args[2].equals("-m")) {
				useSumProduct = false;
			} else {
				usage(args);
				return;
			}
			// check the 4th argument
			if (args.length > 3) {
				if (args[3].equals("-d")) {
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
		try {
			vtag.train(args[0]);
		} catch (IOException e) {
			System.err.println("error training!\n");
			e.printStackTrace();
		}
		// debug train!
		if(debugMode) System.out.println(vtag.getTagDict().toString());
		try {
			vtag.test(args[1], useSumProduct);
		} catch (IOException e) {
			System.err.println("error testing!\n");
			e.printStackTrace();
		}
	}
}
