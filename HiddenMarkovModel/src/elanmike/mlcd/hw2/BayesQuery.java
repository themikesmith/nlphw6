package elanmike.mlcd.hw2;

import java.io.IOException;

import elanmike.mlcd.hw2.Factor.FactorException;

public class BayesQuery {

	private static Bump b;
	/**
	 * Runs bayes-query sum product or max product depending on arguments
	 * @param args list of arguments
	 */
	public static void main(String[] args) {
		// check arguments
		if(args.length < 4 || args.length > 7) {
			usage(args);
			return;
		}
		boolean useSumProduct = true;
		boolean useIncrementalUpdates = true;
		if(args.length>4) { // check our optional arguments.
			// check the 5th argument
			if(args[4].equals("-s")) {
				useSumProduct = true;
			}
			else if(args[4].equals("-m")) {
				useSumProduct = false;
			}
			else {
				usage(args);
				return;
			}
			// check the 6th argument
			if(args.length > 5) {
				if(args[5].equals("-i")) {
					useIncrementalUpdates = true;
				}
				else if(args[5].equals("-n")) {
					useIncrementalUpdates = false;
				}
				else {
					usage(args);
					return;
				}
				// check the 7th argument
				if(args.length > 6) {
					if(args[6].equals("-d")) {
						QueryProcessor.setDebug(true);
					}
					else {
						usage(args);
						return;
					}
				}
			}
		}
		// create and init clique tree
		b = new Bump();
		try {
			b.init(args[0],args[2],args[1]);
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		} catch (FactorException e) {
			e.printStackTrace();
			return;
		}
		// make a query processor
		QueryProcessor qp = new QueryProcessor(b);
		QueryProcessor.setUseIncrementalUpdates(useIncrementalUpdates);
		try {
			// process the queries
			qp.processQueries(args[3], useSumProduct);
		} catch (IOException e) {
			System.err.println("error processing queries from:"+args[4]);
			e.printStackTrace();
		}
		// done!
	}
	
	/**
	 * Prints an example of proper usage of the program
	 */
	private static void usage(String[] args) {
		System.err.println("usage: requires 4 arguments.\narg[0] = network_file\n" +
				"arg[1] = cpd_file;\narg[2] = clique_tree_file\n" +
				"arg[3] = query_file\n" +
				"Defaults to sum-product, but one may specify an optional 5th argument:\n" +
				" arg[4] = 's' or 'm' for sum- or max-product\n" +
				"Defaults to incremental updates on, but one may specify an optional 6th argument:\n" +
				" arg[5] = '-i' or '-n' to enable or disable incremental updates.\n" +
				"Defaults to off, but oe may specify an optional 7th argument:\n" +
				" arg[6] = -d to enable debug mode.");
		System.err.printf("\nyou submitted:\n");
		for(String s : args) System.err.printf("%s\n",s);
	}
}
