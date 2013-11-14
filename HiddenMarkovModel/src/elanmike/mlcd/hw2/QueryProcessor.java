package elanmike.mlcd.hw2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import elanmike.mlcd.hw2.Factor.FactorException;
import elanmike.mlcd.hw2.Factor.FactorIndexException;

public class QueryProcessor {
	public static final int NO_EVIDENCE = -1;
	private static boolean USE_INCREMENTAL_UPDATES = true;
	private static boolean DEBUG = false;
	private Bump _bump;
	/**
	 * Track if we have run bump at least once to calibrate
	 */
	private boolean _calibrated;
	/**
	 * A map from variable name to variable value, transformed into integers
	 */
	Map<Integer, Integer> _queryContexts;

	public QueryProcessor(Bump b) {
		this._bump = b;
		_queryContexts = new HashMap<Integer, Integer>();
		_calibrated = false;
	}
	
	public static void setUseIncrementalUpdates(boolean u) {
		USE_INCREMENTAL_UPDATES = u;
		if(DEBUG) System.out.println((u?"":"not")+"using incremental updates");
	}
	public static void setDebug(boolean u) {
		DEBUG = u;
		if(DEBUG) System.out.println("using debug mode");
	}

	public void resetTreeForQueries() {
		_queryContexts.clear();
		_bump.resetTreeForQueries();
	}

	public String query(String[] lhs, String[] contexts, boolean useSumProduct) {
		//TODO fix
		if (!_calibrated || (useSumProduct != _bump.useSumProduct())) {
			// set appropriate method, and run bump to calibrate
			_bump.setUseSumProduct(useSumProduct);
			_bump.runBump();
			_calibrated = true;
			resetTreeForQueries();
		}
		return query(lhs, contexts);
	}

	/**
	 * queries the structure for p(lhs|contexts)
	 */
	String query(String[] lhs, String[] contexts) {
		if(DEBUG) {
			System.out.println("our current query tree has the following contexts:");
			System.out.println(_queryContexts);
		}
		ArrayList<Integer> vars, values;
		if(USE_INCREMENTAL_UPDATES) {
			// check if evidence is incremental or retractive
			boolean retractive = false;
			// then take action
			// check number of variables in rhs
			if (contexts.length < _queryContexts.size()) {
				// retractive -- less evidence than before. reset and treat as
				// incremental
				if(DEBUG) System.out.println("we have less contexts than query contexts. retractive.");
				retractive = true;
			}
			// if it's the same number in rhs, check each variable
			if (!retractive) {
				try {
					for (String s : contexts) {
						String[] varValue = s.split("=");
						String var = varValue[0], value = varValue[1];
						int varInt = Factor.getVariableIndex(var), valueInt = Factor
								.getVariableValueIndex(varInt, value);
						if (_queryContexts.containsKey(varInt)
								&& _queryContexts.get(varInt) != valueInt) {
							// query context variable has other value. reset.
							if(DEBUG) System.out.println("query context variable has other value. reset.");
							retractive = true;
							break;
						}
					}
				} catch (ArrayIndexOutOfBoundsException ex) {
					// context variable undefined in scope
					return "undefined";
				}
			} // and reset if we have retractive evidence
			if(retractive) {
				resetTreeForQueries(); // clears our evidence
				if(DEBUG) System.out.println("resetting - clearing evidence.");
			}
			// treat all new evidence as incremental
			// note if we cleared, all evidence is new
			vars = new ArrayList<Integer>();
			values = new ArrayList<Integer>();
			int numberNewEvidence = 0;
			try {
				for (int i = 0; i < contexts.length; i++) {
					String[] varValue = contexts[i].split("=");
					String var = varValue[0], value = varValue[1];
					int varInt = Factor.getVariableIndex(var), valueInt = Factor
							.getVariableValueIndex(varInt, value);
					if (!_queryContexts.containsKey(varInt)) {
						// additional evidence - we've never seen it before
						if (DEBUG) {
							System.out.printf("\ni:%d add'l evidence:%s=%s\n",
									i, var, value);
						}
						vars.add(varInt);
						values.add(valueInt);
						_queryContexts.put(varInt, valueInt);
						numberNewEvidence++;
					} else { // do nothing with repeat evidence
						if (DEBUG) {
							System.out.printf("\ni:%d repeat evidence:%s=%s\n",
									i, var, value);
						}
					}
				}
			} catch (ArrayIndexOutOfBoundsException ex) {
				// context variable undefined in scope
				return "undefined";
			}
			if (numberNewEvidence > 0) {
				try {
					_bump.incorporateQueryEvidence(vars, values,
							numberNewEvidence);
				} catch (FactorException e) {
					e.printStackTrace();
					return e.getMessage();
				}
			}
		} else { // incremental updates off.
					// for every query, reset and treat evidence as incremental.
					// treat all new evidence as incremental
			if(DEBUG) System.out.println("not using incremental updates.");
			resetTreeForQueries(); // clears our evidence
			vars = new ArrayList<Integer>();
			values = new ArrayList<Integer>();
			int numberNewEvidence = 0;
			try {
				for (int i = 0; i < contexts.length; i++) {
					String[] varValue = contexts[i].split("=");
					String var = varValue[0], value = varValue[1];
					int varInt = Factor.getVariableIndex(var), valueInt = Factor
							.getVariableValueIndex(varInt, value);
					if (!_queryContexts.containsKey(varInt)) {
						// additional evidence - we've never seen it before
						if (DEBUG) {
							System.out.printf("\ni:%d add'l evidence:%s=%s\n",
									i, var, value);
						}
						vars.add(varInt);
						values.add(valueInt);
						_queryContexts.put(varInt, valueInt);
						numberNewEvidence++;
					} else { // do nothing with repeat evidence
						if (DEBUG) {
							System.out.printf("\ni:%d repeat evidence:%s=%s\n",
									i, var, value);
						}
					}
				}
			} catch (ArrayIndexOutOfBoundsException ex) {
				// context variable undefined in scope
				return "undefined";
			}
			if (numberNewEvidence > 0) {
				try {
					_bump.incorporateQueryEvidence(vars, values,
							numberNewEvidence);
				} catch (FactorException e) {
					e.printStackTrace();
					return e.getMessage();
				}
			}
		}
		// now process lhs
		vars = new ArrayList<Integer>();
		values = new ArrayList<Integer>();
		try {
			for (String s : lhs) {
				String[] varValue = s.split("=");
				String var = varValue[0], value = "";
				int varInt = Factor.getVariableIndex(var), valueInt = NO_EVIDENCE;
				if (varInt == -1) {
					throw new ArrayIndexOutOfBoundsException(
							"variable not recognized, or factors not initialized");
				}
				if (varValue.length > 1) {
					value = varValue[1];
					valueInt = Factor.getVariableValueIndex(varInt, value);
				}
				vars.add(varInt);
				values.add(valueInt);
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			return "undefined"; // we are being asked about a factor that
								// doesn't exist.
		}
		Factor result;
		try {
			if(_bump.useSumProduct()) {
				result = _bump.getQueryResult(vars, values);
			}
			else {
				result = _bump.getQueryResultMaxProduct(vars, values);
			}
			if (DEBUG) {
				System.out.println("\n******result!!!******\n");
			}
			if (result != null) {
				for (int i = 0; i < result.data.size(); i++) {
					// System.out.println(Math.exp(result.data.get(i)));
					if (Math.exp(result.data.get(i)) < 0
							|| Math.exp(result.data.get(i)) > 1) {
						System.err
								.println("uh oh!! invalid probability in our result!");
					}
				}
				return result.toString();
			} else
				return "out of clique inference";
		} catch (FactorIndexException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	/**
	 * Process queries in a query file according to a semiring
	 * 
	 * @param queryFile
	 * @param useSumProduct
	 *            true if sum product semiring, false if max product
	 * @throws IOException
	 */
	public void processQueries(String queryFile, boolean useSumProduct)
			throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(queryFile));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.length() == 0) {
				continue;
			}
			if(DEBUG) System.out.println("\nquery:\n'"+line+"'\n");
			String[] stuff = line.split(" ");
			String[] lhs = stuff[0].split(",");
			String[] rhs = new String[0];
			if (stuff.length > 1) {
				rhs = stuff[1].split(",");
			}
			System.out.println(query(lhs, rhs, useSumProduct));
		}
		br.close();
	}

	public static void main(String[] args) {

	}
}
