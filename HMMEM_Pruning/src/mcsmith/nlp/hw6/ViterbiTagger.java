package mcsmith.nlp.hw6;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViterbiTagger {
	public static final String WORD_TAG_DELIMITER = "/";
	private boolean debugMode;
	/**
	 * Stores our training data
	 */
	private TagDict tdTrain, tdTest, tdRaw;
	/**
	 * Stores our state alpha / mu values - forward pass.
	 * 
	 * A state is uniquely identified by a tag and a time.
	 * 
	 * we have makeKey(tag,time) as the key
	 * 
	 * stores the max / sum probability of reaching this state 'tag' at time 't' from start 
	 */
	private Map<String, Probability> forwardValues;
	/**
	 * Stores the backpointers for the forward pass of the algorithm.
	 * 
	 * Backpointer (tag, time) -> previous best tag
	 */
	private Map<String, Integer> backpointers;
	/**
	 * Stores our state beta / mu values - backward pass.
	 * 
	 * A state is uniquely identified by a tag and a time.
	 * 
	 * we have makeKey(tag,time) as the key
	 * 
	 * stores the max / sum probability of reaching this state 'tag' at time 't' from end 
	 */
	private Map<String, Probability> backwardValues;
	private ArrayList<String> testData;
	private ArrayList<String> rawData;
	/**
	 * Constructor
	 */
	public ViterbiTagger() {
		forwardValues = new HashMap<String, Probability>();
		tdTrain = new TagDict();
		tdTest = new TagDict();
		tdRaw = new TagDict();
		debugMode = false;
		backpointers = new HashMap<String, Integer>();
		backwardValues = new HashMap<String, Probability>();
	}
	public void prepareToTest(String testFilename, String rawFilename) throws IOException {
		testData = readTestData(tdTest, testFilename);
		rawData = readRawData(tdRaw, rawFilename);
	}
	/**
	 * Assembles our training counts from the trainin file.
	 * @param trainFilename
	 * @throws IOException
	 */
	public void train(String trainFilename) throws IOException {
		if(debugMode) System.out.println("reading training file from:"+trainFilename);
		BufferedReader br = new BufferedReader(new FileReader(trainFilename));
		String line;
		int prevTagKey = -1;
		while ((line = br.readLine()) != null) {
			// read line
			String[] wordTag = line.split(WORD_TAG_DELIMITER);
			// first line is a sentence boundary.
			if(prevTagKey == -1) {
				// verify that it's our sentence boundary. throw error if not.
				if(!(wordTag[0].equals(TagDict.SENTENCE_BOUNDARY) && wordTag[1].equals(TagDict.SENTENCE_BOUNDARY))) {
					br.close();
					throw new IOException("error! first line of file is not sentence boundary:"+line);
				}
			}
			if(wordTag.length != 2) {
				br.close();
				throw new IOException("error! unable to parse line:"+line);
			}
			// process line.
			String word = wordTag[0], tag = wordTag[1];
//			if(debugMode) System.out.println(": "+line);
			// ensure we have these in our tag dictionary
			TagDict.addTagToDict(tag);
			TagDict.addWordToDict(word, false);
			// and increment the appropriate counts
			int tagKey = TagDict.getKeyFromTag(tag), wordKey = TagDict.getKeyFromWord(word);
//			if(debugMode) System.out.printf("word:%s tag:%s prevTag:%s\n", word, tag, TagDict.getTagFromKey(prevTagKey));
			if(prevTagKey == -1) {
//				if(debugMode) System.out.println("skipping first line.");
				prevTagKey = tagKey;
				continue;
			}
			tdTrain.incrementCountOfWord(word);
			tdTrain.incrementNumberTagTokens();
			tdTrain.incrementNumberTaggedWordTokens();
			tdTrain.incrementTimesSeenTagContext(tagKey);
			tdTrain.incrementObservedEmissionCount(wordKey, tagKey);
			if(prevTagKey != -1) {
				tdTrain.incrementObservedTransmissionCount(tagKey, prevTagKey);
			}
			prevTagKey = tagKey;
		}
		br.close();
		if(debugMode) {
			System.out.println("number word tag tokens:"+tdTrain.getNumberWordTagTokens());
			System.out.println("after training!");
			System.out.println("n of orig:"+tdTrain.getNumberTagTokensOriginal());
			System.out.println("n of raw:"+"null");
			System.out.println("n of new:"+tdTrain.getNumberTagTokensNew());
			System.out.println("n of current:"+tdTrain.getNumberTagTokens());
		}
		// and save counts in original
		tdTrain.setOriginalCountsToCurrent();
		// modified for ic data set - original = 0
//		tdTrain.setOriginalCountsToZero();
	}
	private ArrayList<String> readTestData(TagDict td, String testFilename) throws IOException {
		if(debugMode) System.out.println("reading test file from:"+testFilename);
		BufferedReader br = new BufferedReader(new FileReader(testFilename));
		ArrayList<String> testData = new ArrayList<String>();
		String line;
		while ((line = br.readLine()) != null) {
			// read line
			String[] wordTag = line.split(WORD_TAG_DELIMITER);
			if (wordTag.length != 2) {
				br.close();
				throw new IOException("error! unable to parse line:" + line);
			}
			// process line. - increase vocab size if necessary
			String word = wordTag[0];
			TagDict.addWordToDict(word, true);
			td.initWordCounter(word);
			// read test data into memory
			testData.add(line);
		}
		br.close();
		if(debugMode) System.out.println("vocab size:"+TagDict.getVocabSize());
		return testData;
	}
	private ArrayList<String> readRawData(TagDict td, String rawFilename) throws IOException {
		if(debugMode) System.out.println("reading raw file from:"+rawFilename);
		BufferedReader br = new BufferedReader(new FileReader(rawFilename));
		ArrayList<String> rawData = new ArrayList<String>();
		String line;
		while ((line = br.readLine()) != null) {
			// read line
			String[] wordTag = line.split(WORD_TAG_DELIMITER);
			if (wordTag.length == 1) {
				// good. no tags should be found
				// process line. - increase vocab size if necessary
				String word = wordTag[0];
				TagDict.addWordToDict(word, false);
				td.initWordCounter(word);
				// read raw data into memory
				rawData.add(line);
			}
			else {
				br.close();
				throw new IOException("error! unable to parse line:" + line);
			}
		}
		br.close();
		if(debugMode) System.out.println("vocab size:"+TagDict.getVocabSize());
		return rawData;
	}
	public double testViterbi(ArrayList<String> testData) {
		if (debugMode) System.out.println("\nviterbi!");
		// we use backpointers to store the tag with the max probability at each step
		backpointers.clear();
		// viterbi
		initializeForward(testData);
		runPass(testData, true, false);
		if (debugMode) {
			System.out.println("\nafter forward pass!\n");
			printData(testData, forwardValues);
		}
		// follow back pointers and compare to our given test data
		if(debugMode) {
			int[] result = getCompareResultFromBackpointers(testData, false, backpointers);
			System.out.printf("viterbi produced this tagging!\n");
			for (int i = 2; i < result.length; i++) {
				System.out.printf("i:%d tag:%s\n", i - 1,
						TagDict.getTagFromKey(result[i]));
			}
			return -1;
		}
		else {
			return getAccuracyResultTagging(testData, false, backpointers);
		}
	}

	private void reEstimateTrainingCountsFromRaw(ArrayList<String> rawData) {
		backpointers.clear();
		if (debugMode) {
			System.out.println("forward/backward!");
			System.out.println("\nbefore:");
			System.out.println("n of orig:"+tdTrain.getNumberTagTokensOriginal());
			System.out.println("n of raw:"+(rawData.size()-1));
			System.out.println("n of new:"+tdTrain.getNumberTagTokensNew());
			System.out.println("n of current:"+tdTrain.getNumberTagTokens());
		}
		// we use 'current' counts while running the algorithm
		// but we update the 'new' counts based on these current counts
		if (debugMode) tdTrain.printOriginalCounts();
		// new = original
		if (debugMode) System.out.println("init new counts with train counts");
		tdTrain.setNewCountsToOriginal();
		// forward backward - forward pass
		initializeForward(rawData);
		if (debugMode) System.out.println("current counts and probabilities:");
		if (debugMode) System.out.println(tdTrain.toString());
		runPass(rawData, true, true);
		if (debugMode) {
			System.out.println("\nafter forward pass!\n");
			printData(rawData, forwardValues);
		}
		String endKey = TagDict.makeKey(
				TagDict.getKeyFromWord(TagDict.SENTENCE_BOUNDARY),
				rawData.size() - 1);
		Probability S = forwardValues.get(endKey);
		if (debugMode) System.out.println("S:" + S);
		// backward pass
		initializeBackward(rawData);
		// in here we compute the new counts while using the current counts...
		runPass(rawData, false, true);
		if (debugMode) {
			System.out.println("\nafter backward pass!\n");
			printData(rawData, backwardValues);
		}
		// print perplexity per untagged word:
		double nRaw = new Double(rawData.size()-1);
//		tdTrain.setNumberTagTokensNew(nRaw);
//		tdTrain.setNumberWordTagTokensNew(nRaw);
		System.out.printf("Perplexity per untagged raw word:%s\n",getPerplexityPerTaggedWord(S, nRaw));
		// and now that we're finished, we set the 'new' counts computed in this iteration 
		// to 'current' for the next
		if (debugMode) {
			System.out.println("new holds train + raw counts:");
			tdTrain.printNewCounts();
			tdTrain.printCurrentCounts();
			System.out.println("after run:");
			System.out.println("n of orig:"+tdTrain.getNumberTagTokensOriginal());
			System.out.println("n of raw:"+(rawData.size()-1));
			System.out.println("n of new:"+tdTrain.getNumberTagTokensNew());
			System.out.println("n of current:"+tdTrain.getNumberTagTokens());
		}
		// current = new
		tdTrain.setCurrentCountsToNew();
		if (debugMode) {
			System.out.println("current counts and probabilities:");
			System.out.println(tdTrain.toString());
			System.out.println("after copying new to current:");
			System.out.println("n of orig:"+tdTrain.getNumberTagTokensOriginal());
			System.out.println("n of raw:"+(rawData.size()-1));
			System.out.println("n of new:"+tdTrain.getNumberTagTokensNew());
			System.out.println("n of current:"+tdTrain.getNumberTagTokens());
		}
	}
	/**
	 * 
	 * @param useSumProduct
	 * @return the tagging accuracy if viterbi, or -1 if reestimating
	 * @throws IOException
	 */
	public double test(boolean useSumProduct) throws IOException {
		if(!useSumProduct) {
			return testViterbi(testData);
		}
		else {
			reEstimateTrainingCountsFromRaw(rawData);
			return -1;
		}
	}

	private void printData(ArrayList<String> testData, Map<String, Probability> states) {
		// for all datum in test:
		for (int i = 0; i < testData.size(); i++) {
			String datum = testData.get(i);
			String[] split = datum.split(WORD_TAG_DELIMITER);
			String word = split[0];
			int wordKey = TagDict.getKeyFromWord(word);
			// for each possible tag of this datum, we have one state.
			List<Integer> tags = tdTrain.getTagDictForWord(wordKey);
			Collections.sort(tags);
			for (int possibleTag : tags) {
				String stateKey = TagDict.makeKey(possibleTag, i);
				// print
				System.out.printf("mu(%s, %d) = %s\n", 
						TagDict.getTagFromKey(possibleTag), i, states.get(stateKey));
			}
		}
	}
	/**
	 * Initialize the HMM with states specified by the tags in the test data
	 * and with the value specified.
	 * @param rawData
	 * @param value
	 */
	private void initializeForward(ArrayList<String> rawData) {
		// initialize start state at 1...
		String startKey = TagDict.makeKey(TagDict.getKeyFromTag(TagDict.SENTENCE_BOUNDARY), 0);
		forwardValues.put(startKey, new Probability(1));
		// ... and all other states at 0
		// for all datum in test:
		for (int i = 1; i < rawData.size(); i++) {
			String datum = rawData.get(i);
//			if(debugMode) System.out.printf("i:%d data:%s\n", i, datum);
			String[] split = datum.split(WORD_TAG_DELIMITER);
			String word = split[0];
			int wordKey = TagDict.getKeyFromWord(word);
			// for each possible tag of this datum, we have one state.
			for(int possibleTag : tdTrain.getTagDictForWord(wordKey)) {
//				if(debugMode) System.out.printf("possible tag:%d aka %s word:%s\n", possibleTag, TagDict.getTagFromKey(possibleTag), word);
				String stateKey = TagDict.makeKey(possibleTag, i);
				// initialize all states with value specified
				forwardValues.put(stateKey, new Probability(0));
			}
		}
	}
	/**
	 * Initialize the HMM with states specified by the tags in the test data
	 * and with the value specified.
	 * @param rawData
	 * @param value
	 */
	private void initializeBackward(ArrayList<String> rawData) {
		// initialize end state at 1...
		String endKey = TagDict.makeKey(TagDict.getKeyFromWord(TagDict.SENTENCE_BOUNDARY), rawData.size()-1);
		backwardValues.put(endKey, new Probability(1));
		// ... and all other states at 0
		// for all datum in test:
		for (int i = rawData.size() - 2; i >= 0; i--) {
			String datum = rawData.get(i);
			String[] split = datum.split(WORD_TAG_DELIMITER);
			String word = split[0];
			int wordKey = TagDict.getKeyFromWord(word);
			// for each possible tag of this datum, we have one state.
			for (int possibleTag : tdTrain.getTagDictForWord(wordKey)) {
				String stateKey = TagDict.makeKey(possibleTag, i);
				// initialize all states with value specified
				backwardValues.put(stateKey, new Probability(0));
			}
		}
	}
	/**
	 * Runs a pass of the algorithm.
	 * @param testData the test data on which to run
	 * @param forward true if forward, false if backward
	 * @param useSumProduct true if sum product, false if max product
	 */
	private void runPass(List<String> testData, boolean forward, boolean useSumProduct) {
		if(forward) runForwardPass(testData, useSumProduct);
		else runBackwardPass(testData, useSumProduct);
	}
	private void runForwardPass(List<String> testData, boolean useSumProduct) {
		// for int i = 1 to n (ranges over all test data(
		// key = tag,time
		Map<String, Probability> stateValues = forwardValues;
		// for all datum in test:
		boolean printedDot = false;
		for (int i = 1; i < testData.size(); i++) {
			if(i % 100000 == 0) {
				System.err.println(".");
				printedDot = true;
			}
			else if(i % 1000 == 0) {
				System.err.print(".");
				printedDot = true;
			}
			String datum = testData.get(i), prevDatum = testData.get(i-1);
			String[] split = datum.split(WORD_TAG_DELIMITER), prevSplit = prevDatum.split(WORD_TAG_DELIMITER);
			String word = split[0], prevWord = prevSplit[0];
			if(debugMode) System.out.printf("\n*******\nword:%s\n",word);
			int wordKey = TagDict.getKeyFromWord(word), prevWordKey = TagDict.getKeyFromWord(prevWord);
			// for each possible tag of this datum, we have one state.
			for(int possibleTag : tdTrain.getTagDictForWord(wordKey)) {
				String currentKey = TagDict.makeKey(possibleTag, i);
				// for each possible tag of the previous datum...
				for(int prevPossibleTag : tdTrain.getTagDictForWord(prevWordKey)) {
					// p = arc prob = p(tag | prev tag) * p(word | tag)
					if(debugMode) System.out.printf("\ntag:%s prevTag:%s\n", TagDict.getTagFromKey(possibleTag), TagDict.getTagFromKey(prevPossibleTag));
					Probability arcProb = tdTrain.getSmoothedProbTagGivenPrevTag(possibleTag, prevPossibleTag);
					arcProb = arcProb.product(tdTrain.getSmoothedProbWordGivenTag(wordKey, possibleTag));
					if(debugMode) System.out.println("arcprob:"+arcProb);
					if(useSumProduct) {
						// alpha[t][i] = alpha[t][i] + (alpha[t-1][i-1] * arc prob
						String prevKey = TagDict.makeKey(prevPossibleTag, i-1);
						Probability prevAlpha = stateValues.get(prevKey);
						Probability summand = prevAlpha.product(arcProb);
						// get current value
						Probability currentAlpha = stateValues.get(currentKey);
						// and add values
						stateValues.put(currentKey, currentAlpha.add(summand));
					}
					else {
						// mu = mu t-1 (i-1) * arc prob
						Probability prevBest = stateValues.get(TagDict.makeKey(prevPossibleTag, i-1));
						Probability mu = prevBest.product(arcProb);
						// get current best and compare
						Probability currentBest = stateValues.get(currentKey);
						if(mu.getLogProb() >= currentBest.getLogProb()) {
							// we have a new max!
							stateValues.put(currentKey, mu);
							// and store back pointer
							backpointers.put(currentKey, prevPossibleTag);
						}
					}
				}
			}
		}
		if(printedDot) System.err.println();
	}
	private void runBackwardPass(List<String> rawData, boolean useSumProduct) {
		// for int i = 1 to n (ranges over all test data(
		// key = tag,time
		// for all datum in test:
		boolean printedDot = false;
		for (int i = rawData.size() - 1; i > 0; i--) {
			if(i % 100000 == 0) {
				System.err.println(".");
				printedDot = true;
			}
			else if(i % 1000 == 0) {
				System.err.print(".");
				printedDot = true;
			}
			String datum = rawData.get(i), prevDatum = rawData.get(i-1);
			String[] split = datum.split(WORD_TAG_DELIMITER), prevSplit = prevDatum.split(WORD_TAG_DELIMITER);
			String word = split[0], prevWord = prevSplit[0];
			int wordKey = TagDict.getKeyFromWord(word), prevWordKey = TagDict.getKeyFromWord(prevWord);
			// update word count
			tdTrain.incrementNewCountOfWord(word);
			tdTrain.incrementNumberTaggedWordTokensNew();
			tdTrain.incrementNumberTagTokensNew();
			// for each possible tag of this datum, we have one state.
			for(int possibleTag : tdTrain.getTagDictForWord(wordKey)) {
				String currentKey = TagDict.makeKey(possibleTag, i);
				if(debugMode) System.out.printf("\ni:%d word:%s possTag:%s\n",i, word,TagDict.getTagFromKey(possibleTag));
				// ... we can compute unigram probability p(t i | w)
//				System.out.println("p(t_i | w) = alpha[t,i] * beta[t,i] / S");
				// = alpha (t, i) * beta (t,i) / S
				Probability alphaTI = forwardValues.get(currentKey);
				Probability betaTI = backwardValues.get(currentKey);
				String endKey = TagDict.makeKey(TagDict.getKeyFromWord(TagDict.SENTENCE_BOUNDARY), rawData.size()-1);
				Probability S = forwardValues.get(endKey);
//				if(debugMode) System.out.printf("alpha[t,i]=%s beta[t,i]=%s S=%s\n", alphaTI, betaTI, S);
				// punigram means we have seen the unigram probabilistically that many times
				Probability pUnigram = alphaTI.product(betaTI).divide(S);
				
				// update count of this possible tag
				// p(-> possible tag) = alpha(tag) * beta(tag) / S
//				System.out.printf("p(->%s)= %s\n", TagDict.getTagFromKey(possibleTag), pUnigram);
				// and update count of seeing the context of possible tag
				tdTrain.incrementNewTimesSeenTagContext(possibleTag, pUnigram);
				
				// and update expected count of emission
				tdTrain.incrementNewObservedEmissionCount(wordKey, possibleTag, pUnigram);
				
				// max probability we've found
				Probability maxProbFound = new Probability(0);
				// and prev tag that produced it
				int bestPrevTag = -1;
				// for each possible tag of the previous datum...
				for(int prevPossibleTag : tdTrain.getTagDictForWord(prevWordKey)) {
					// p = arc prob = p(tag | prev tag) * p(word | tag)
//					System.out.println("computing arc prob = p(tag | prev tag) * p(word | tag)");
//					System.out.printf(" = p(%s | %s) * p(%s | %s)\n", 
//							TagDict.getTagFromKey(possibleTag), TagDict.getTagFromKey(prevPossibleTag),
//							word, TagDict.getTagFromKey(possibleTag));
					Probability arcProb = tdTrain.getSmoothedProbTagGivenPrevTag(possibleTag, prevPossibleTag);
					arcProb = arcProb.product(tdTrain.getSmoothedProbWordGivenTag(wordKey, possibleTag));
					if(useSumProduct) {
//						if(debugMode) System.out.printf("\ni:%d i-1:%d prevWord:%s prevPossTag:%s\n", i, i-1, prevWord, TagDict.getTagFromKey(prevPossibleTag));
						// beta[t-1][i-1] = beta[t-1][i-1] + beta[t][i] * arc prob
						String prevKey = TagDict.makeKey(prevPossibleTag, i-1);
						Probability prevValue = backwardValues.get(prevKey);
//						if(debugMode) System.out.printf("beta[t-1,i-1]:%s\n", prevValue);
						// get current value
						Probability currentValue = backwardValues.get(currentKey);
//						if(debugMode) System.out.printf("beta[t,i]:%s\n", currentValue);
//						if(debugMode) System.out.printf("arcprob:%s\n", arcProb);
						Probability summand = currentValue.product(arcProb);
						// and add values
//						if(debugMode) System.out.println("beta[t-1][i-1] = beta[t-1][i-1] + (beta[t][i] * arc prob)");
//						if(debugMode) System.out.printf("beta[t-1,i-1]:%s\n", prevValue.add(summand));
						backwardValues.put(prevKey, prevValue.add(summand));
						// ... now we can compute bigram probability p( [t-1, i-1] ^ t i | w)
						// = alpha (t-1, i-1) * p * beta (t,i) / S
//						System.out.println("p([t-1, i-1] ^ t_i | w) = alpha[t-1, i-1] * p * beta[t,i] / S");
						Probability prevAlpha = forwardValues.get(prevKey);
//						if(debugMode) System.out.printf("alpha[t-1, i-1 : %s]=%s p=%s beta[t,i : %s]=%s S=%s\n", prevKey, prevAlpha, arcProb, currentKey, betaTI, S);
						Probability pBigram = prevAlpha.product(arcProb).product(betaTI).divide(S);
						// update count of transmission from poss tag to prev poss tag
						// p = arc prob = p(tag | prev tag) * p(word | tag)
						// alpha(prev tag) * p(tag | prev tag) * beta(tag) * p(word | tag) / S 
						try {
							tdTrain.incrementNewObservedTransmissionCount(possibleTag, prevPossibleTag, pBigram);
						}
						catch(NullPointerException ex) {
							System.err.printf("uh oh! transmission count not initalized for:%s->%s\n",
									TagDict.getTagFromKey(possibleTag), TagDict.getTagFromKey(prevPossibleTag));
							throw ex;
						}
						
						// ..to get this we divide pbigram / punigram
						Probability current = pBigram.divide(pUnigram);
						// track result with backpointers
						if(current.getLogProb() > maxProbFound.getLogProb()) {
//							if(debugMode) System.out.println("found new best!");
//							if(debugMode) System.out.println("old tag:"+bestPrevTag);
//							if(debugMode) System.out.println("old prob:"+maxProbFound);
							maxProbFound = current;
							bestPrevTag = prevPossibleTag;
//							if(debugMode) System.out.println("new tag:"+bestPrevTag);
//							if(debugMode) System.out.println("new prob:"+maxProbFound);
						}
					}
					else {
						System.err.println("\ndon't run viterbi backwards!\n");
						System.out.println("\ndon't run viterbi backwards\n");
					}
				}
				if(bestPrevTag == -1) {
					System.err.println("\nnever assigned a best!\n");
					System.out.println("\nnever assigned a best\n");
				}
				backpointers.put(currentKey, bestPrevTag);
			}
		}
		if(printedDot) System.err.println();
	}
	/**
	 * Given the test data length, and the backpointers map,
	 * follow backpointers and assemble the best tagging.
	 * Prints the comparison results at the same time.
	 * Overall accuracy considers all tokens except ###
	 * Known-word accuracy considers all words except ### that appears in training data
	 * Novel-word accuracy considers all words except ### that don't appear in training data
	 * 
	 * @param testData
	 * @param useSumProduct
	 * @param backpointers a map of (tag, time) -> prev tag backpointers
	 * @return the resulting array of tag keys, in order from 1 to n
	 */
	private int[] getCompareResultFromBackpointers(ArrayList<String> testData, boolean useSumProduct, Map<String, Integer> backpointers) {
		// init count of correct tags (note we know the number of total tags is N)
		double totalWords = 0, totalCorrectTags = 0, 
				totalKnownWords = 0, totalKnownTaggedCorrectly = 0,
				totalSeenWords = 0, totalSeenTaggedCorrectly = 0,
				totalNovelWords = 0, totalNovelTaggedCorrectly = 0;
		// declare array and follow back pointers
		int[] result = new int[testData.size()];
		// begin at the ending boundary
		int currTag = TagDict.getKeyFromTag(TagDict.SENTENCE_BOUNDARY);
		// from n down to 1 (note n = size - 1)
		int n = testData.size() - 1;
		if(debugMode) System.out.println("n:"+n);
		for (int i = n; i > 0; i--) {
			// get our previous tag
			int prevTag = backpointers.get(TagDict.makeKey(currTag, i));
			// set it in our array to return
			result[i] = prevTag;
			// compare:
			String line = testData.get(i-1);
			String[] datum = line.split(WORD_TAG_DELIMITER);			
			if(datum.length < 2) {
				System.err.println("error processing test data! couldn't parse delimiter:"+line);
				StringBuilder sb = new StringBuilder("datum:[");
				for(int d = 0; d < datum.length; d++) {
					sb.append(String.format("%s,", datum[d]));
				}
				sb.deleteCharAt(sb.length()-1);
				sb.append("]");
				System.out.println(sb);
				return null;
			}
			// update counts
			int obsPrevTag = TagDict.getKeyFromTag(datum[1]); // should never be null - we assume we've seen all tags
			// don't count our sentence boundary
			if(obsPrevTag != TagDict.getKeyFromTag(TagDict.SENTENCE_BOUNDARY)) {
				totalWords++;
				if(obsPrevTag == prevTag) {
					totalCorrectTags++;
				}
				// was the word known? seen while training and while testing
				if(tdTrain.seenWord(datum[0])) {
					// known
					totalKnownWords++;
					if(obsPrevTag == prevTag) {
						totalKnownTaggedCorrectly++;
					}
				}
				else if(tdRaw.seenWord(datum[0])) {
					// seen
					totalSeenWords++;
					if(obsPrevTag == prevTag) {
						totalSeenTaggedCorrectly++;
					}
				}
				else {
					// novel
					totalNovelWords++;
					if(obsPrevTag == prevTag) {
						totalNovelTaggedCorrectly++;
					}
				}
			}
			// and continue following
			currTag = prevTag;
		}
		if(debugMode) {
			System.out.println("num words:"+totalWords);
			System.out.println("overall correct:"+totalCorrectTags);
			System.out.println("known correct:"+totalKnownTaggedCorrectly);
			System.out.println("num known words:"+totalKnownWords);
			System.out.println("seen correct:"+totalSeenTaggedCorrectly);
			System.out.println("num seen words:"+totalSeenWords);
			System.out.println("novel correct:"+totalNovelTaggedCorrectly);
			System.out.println("num novel words:"+totalNovelWords);
		}
		// now that we've got all our counts, compute the accuracy scores
		// don't count the sentence boundaries when we score
		double overallAccuracy = totalCorrectTags / totalWords, 
				knownAccuracy = totalKnownTaggedCorrectly / totalKnownWords,
				seenAccuracy = totalSeenTaggedCorrectly / totalSeenWords,
				novelAccuracy = totalNovelTaggedCorrectly / totalNovelWords;
		String overallAccuracyS = String.format("%.2f%%", overallAccuracy * 100), 
				knownAccuracyS = String.format("%.2f%%", knownAccuracy * 100),
				seenAccuracyS = String.format("%.2f%%", seenAccuracy * 100),
				novelAccuracyS = String.format("%.2f%%", novelAccuracy * 100);
		if(overallAccuracy == Double.NaN) {
			overallAccuracyS = "N/A";
		}
		if(knownAccuracy == Double.NaN) {
			knownAccuracyS = "N/A";
		}
		if(seenAccuracy == Double.NaN) {
			seenAccuracyS = "N/A";
		}
		if(novelAccuracy == Double.NaN) {
			novelAccuracyS = "N/A";
		}
		String endKey = TagDict.makeKey(TagDict.getKeyFromWord(TagDict.SENTENCE_BOUNDARY), testData.size()-1);
		Probability finalProb = forwardValues.get(endKey);
		// and print
		if(!useSumProduct) {
			System.out.printf("Tagging accuracy (Viterbi decoding): %s " +
					"(known: %s seen: %s novel: %s)\n" +
					"Perplexity per Viterbi-tagged test word: %s\n",
					overallAccuracyS, knownAccuracyS, seenAccuracyS, novelAccuracyS, 
					getPerplexityPerTaggedWord(finalProb, n));
		}
		else {
			System.out.printf("Tagging accuracy (posterior decoding): %s " +
					"(known: %s seen: %s novel: %s)\n",
					overallAccuracyS, knownAccuracyS, seenAccuracyS, novelAccuracyS);
		}
		
		return result;
	}
	/**
	 * Given the test data length, and the backpointers map,
	 * follow backpointers and assemble the best tagging.
	 * Prints the comparison results at the same time.
	 * Overall accuracy considers all tokens except ###
	 * Known-word accuracy considers all words except ### that appears in training data
	 * Novel-word accuracy considers all words except ### that don't appear in training data
	 * 
	 * @param testData
	 * @param useSumProduct
	 * @param backpointers a map of (tag, time) -> prev tag backpointers
	 * @return the
	 */
	private Double getAccuracyResultTagging(ArrayList<String> testData, boolean useSumProduct, Map<String, Integer> backpointers) {
		// init count of correct tags (note we know the number of total tags is N)
		double totalWords = 0, totalCorrectTags = 0, 
				totalKnownWords = 0, totalKnownTaggedCorrectly = 0,
				totalSeenWords = 0, totalSeenTaggedCorrectly = 0,
				totalNovelWords = 0, totalNovelTaggedCorrectly = 0;
		// declare array and follow back pointers
		int[] result = new int[testData.size()];
		// begin at the ending boundary
		int currTag = TagDict.getKeyFromTag(TagDict.SENTENCE_BOUNDARY);
		// from n down to 1 (note n = size - 1)
		int n = testData.size() - 1;
		if(debugMode) System.out.println("n:"+n);
		for (int i = n; i > 0; i--) {
			// get our previous tag
			int prevTag = backpointers.get(TagDict.makeKey(currTag, i));
			// set it in our array to return
			result[i] = prevTag;
			// compare:
			String line = testData.get(i-1);
			String[] datum = line.split(WORD_TAG_DELIMITER);			
			if(datum.length < 2) {
				System.err.println("error processing test data! couldn't parse delimiter:"+line);
				StringBuilder sb = new StringBuilder("datum:[");
				for(int d = 0; d < datum.length; d++) {
					sb.append(String.format("%s,", datum[d]));
				}
				sb.deleteCharAt(sb.length()-1);
				sb.append("]");
				System.out.println(sb);
				return null;
			}
			// update counts
			int obsPrevTag = TagDict.getKeyFromTag(datum[1]); // should never be null - we assume we've seen all tags
			// don't count our sentence boundary
			if(obsPrevTag != TagDict.getKeyFromTag(TagDict.SENTENCE_BOUNDARY)) {
				totalWords++;
				if(obsPrevTag == prevTag) {
					totalCorrectTags++;
				}
				// was the word known? seen while training and while testing
				if(tdTrain.seenWordOriginal(datum[0])) {
					// known
					totalKnownWords++;
					if(obsPrevTag == prevTag) {
						totalKnownTaggedCorrectly++;
					}
				}
				else if(tdRaw.seenWord(datum[0])) {
					// seen
					totalSeenWords++;
					if(obsPrevTag == prevTag) {
						totalSeenTaggedCorrectly++;
					}
				}
				else {
					System.out.println("novel word:"+datum[0]);
					// novel
					totalNovelWords++;
					if(obsPrevTag == prevTag) {
						totalNovelTaggedCorrectly++;
					}
				}
			}
			// and continue following
			currTag = prevTag;
		}
		if(debugMode) {
			System.out.println("num words:"+totalWords);
			System.out.println("overall correct:"+totalCorrectTags);
			System.out.println("known correct:"+totalKnownTaggedCorrectly);
			System.out.println("num known words:"+totalKnownWords);
			System.out.println("seen correct:"+totalSeenTaggedCorrectly);
			System.out.println("num seen words:"+totalSeenWords);
			System.out.println("novel correct:"+totalNovelTaggedCorrectly);
			System.out.println("num novel words:"+totalNovelWords);
		}
		// now that we've got all our counts, compute the accuracy scores
		// don't count the sentence boundaries when we score
		double overallAccuracy = totalCorrectTags / totalWords, 
				knownAccuracy = totalKnownTaggedCorrectly / totalKnownWords,
				seenAccuracy = totalSeenTaggedCorrectly / totalSeenWords,
				novelAccuracy = totalNovelTaggedCorrectly / totalNovelWords;
		String overallAccuracyS = String.format("%.2f%%", overallAccuracy * 100), 
				knownAccuracyS = String.format("%.2f%%", knownAccuracy * 100),
				seenAccuracyS = String.format("%.2f%%", seenAccuracy * 100),
				novelAccuracyS = String.format("%.2f%%", novelAccuracy * 100);
		if(overallAccuracy == Double.NaN) {
			overallAccuracyS = "N/A";
		}
		if(knownAccuracy == Double.NaN) {
			knownAccuracyS = "N/A";
		}
		if(seenAccuracy == Double.NaN) {
			seenAccuracyS = "N/A";
		}
		if(novelAccuracy == Double.NaN) {
			novelAccuracyS = "N/A";
		}
		String endKey = TagDict.makeKey(TagDict.getKeyFromWord(TagDict.SENTENCE_BOUNDARY), testData.size()-1);
		Probability finalProb = forwardValues.get(endKey);
		// and print
		if(!useSumProduct) {
			System.out.printf("Tagging accuracy (Viterbi decoding): %s " +
					"(known: %s seen: %s novel: %s)\n" +
					"Perplexity per Viterbi-tagged test word: %s\n",
					overallAccuracyS, knownAccuracyS, seenAccuracyS, novelAccuracyS, 
					getPerplexityPerTaggedWord(finalProb, n));
		}
		else {
			System.out.printf("Tagging accuracy (posterior decoding): %s " +
					"(known: %s seen: %s novel: %s)\n",
					overallAccuracyS, knownAccuracyS, seenAccuracyS, novelAccuracyS);
		}
		
		return overallAccuracy;
	}
	/**
	 * Computes exp( -1* log( p(w1, t1, w2, t2, ..., wn, tn | w0, t0) / n) )
	 * 
	 * where t0, t1, t2, ..., tn is the winning tag sequence 
	 * that your tagger assigns to test data (with t0 = tn = w0 = wn = ###).
	 * 
	 * @return the perplexity per word
	 */
	private double getPerplexityPerTaggedWord(Probability prob, double n) {
		if(debugMode) {
			System.out.println("prob of sequence:"+prob);
//			System.out.println("logprob of sequence:"+prob.getLogProb());
			System.out.println("n:"+n);
		}
		return Math.exp(prob.getLogProb() / (-1* n));
	}
	public TagDict getTagDict() {return tdTrain;}
	public void setDebugMode(boolean b) {debugMode = b;}
	public static void main(String[] args) {
		ViterbiTagger vtag = new ViterbiTagger();
		vtag.setDebugMode(true);
		try {
			vtag.train(args[0]);
		} catch (IOException e) {
			System.err.println("error training!\n");
			e.printStackTrace();
		}
		System.out.println(vtag.getTagDict().toString());
	}
}
