package mcsmith.nlp.hw6;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mcsmith.nlp.hw6.TagDict.SMOOTHING;

public class ViterbiTagger {
	public static final String WORD_TAG_DELIMITER = "/";
	private boolean debugMode;
	/**
	 * Stores our training data
	 */
	private TagDict td;
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
	private Map<String, Integer> viterbiBackpointers;
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
	/**
	 * Constructor
	 */
	public ViterbiTagger() {
		forwardValues = new HashMap<String, Probability>();
		td = new TagDict();
		debugMode = false;
		viterbiBackpointers = new HashMap<String, Integer>();
		backwardValues = new HashMap<String, Probability>();
//		td.setSmoother(SMOOTHING.oneCountSmoothing);
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
			if(debugMode) System.out.println(": "+line);
			// ensure we have these in our tag dictionary
			td.addTagToDict(tag);
			td.increaseVocab(word);
			// and increment the appropriate counts
			int tagKey = td.getKeyFromTag(tag), wordKey = td.getKeyFromWord(word);
			if(debugMode) System.out.printf("word:%s tag:%s prevTag:%s\n", word, tag, td.getTagFromKey(prevTagKey));
			if(prevTagKey == -1) {
				if(debugMode) System.out.println("skipping first line.");
				prevTagKey = tagKey;
				continue;
			}
			td.incrementCountOfWord(word);
			td.incrementNumberTagTokens();
			td.incrementNumberTaggedWordTokens();
			td.incrementTimesSeenTagContext(tagKey);
			td.incrementObservedEmissionCount(wordKey, tagKey);
			if(prevTagKey != -1) {
				td.incrementObservedTransmissionCount(tagKey, prevTagKey);
			}
			prevTagKey = tagKey;
		}
//		td.decrementBoundaryContextCount(); // correct our counts for the ### context.
		if(debugMode) System.out.println("number word tag tokens:"+td.getNumberWordTagTokens());
		br.close();
	}
	private ArrayList<String> readTestData(String testFilename) throws IOException {
		if(debugMode) System.out.println("reading test file from:"+testFilename);
		BufferedReader br = new BufferedReader(new FileReader(testFilename));
		ArrayList<String> testData = new ArrayList<String>();
		String line;
		while ((line = br.readLine()) != null) {
			// TODO is this all?
			// TODO add in adding to vocabulary size
			// read line
			String[] wordTag = line.split(WORD_TAG_DELIMITER);
			if (wordTag.length != 2) {
				br.close();
				throw new IOException("error! unable to parse line:" + line);
			}
			// process line. - increase vocab size if necessary
			String word = wordTag[0];
			if (debugMode) System.out.println(": " + line);
			td.increaseVocab(word);
			// read test data into memory
			testData.add(line);
		}
		br.close();
		td.increaseVocabForOOV();
		if(debugMode) System.out.println("vocab size:"+td.getVocabSize());
		return testData;
	}
	public void test(String testFilename, boolean useSumProduct) throws IOException {
		ArrayList<String> testData = readTestData(testFilename);
		// we have read the test data into memory now.
		// initialize at 0 for max
		initializeForward(testData);
		if(debugMode) {
			System.out.println("\nafter init!\n");
			printData(testData, forwardValues);
		}
		runPass(testData, true, useSumProduct);
		if(debugMode) {
			System.out.println("\nafter forward pass!\n");
			printData(testData, forwardValues);
		}
		// follow back pointers and compare to our given test data
		int[] result = getCompareResultFromBackpointers(testData, viterbiBackpointers);
		if(debugMode) {
			System.out.printf("viterbi produced this tagging!\n");
			for (int i = 2; i < result.length; i++) {
				System.out.printf("i:%d tag:%s\n", i - 1,
						td.getTagFromKey(result[i]));
			}
		}
	}

	private void printData(ArrayList<String> testData, Map<String, Probability> states) {
		// for all datum in test:
		for (int i = 0; i < testData.size(); i++) {
			String datum = testData.get(i);
			String[] split = datum.split(WORD_TAG_DELIMITER);
			String word = split[0];
			int wordKey = td.getKeyFromWord(word);
			// for each possible tag of this datum, we have one state.
			List<Integer> tags = td.getTagDictForWord(wordKey);
			Collections.sort(tags);
			for (int possibleTag : tags) {
				String stateKey = TagDict.makeKey(possibleTag, i);
				// print
				System.out.printf("mu(%s, %d) = %s\n", 
						td.getTagFromKey(possibleTag), i, states.get(stateKey));
			}
		}
	}
	/**
	 * Initialize the HMM with states specified by the tags in the test data
	 * and with the value specified.
	 * @param testData
	 * @param value
	 */
	private void initializeForward(ArrayList<String> testData) {
		// initialize start state at 1...
		String startKey = TagDict.makeKey(td.getKeyFromTag(TagDict.SENTENCE_BOUNDARY), 0);
		forwardValues.put(startKey, new Probability(1));
		// ... and all other states at value specified
		// for all datum in test:
		for (int i = 1; i < testData.size(); i++) {
			String datum = testData.get(i);
			if(debugMode) System.out.printf("i:%d data:%s\n", i, datum);
			String[] split = datum.split(WORD_TAG_DELIMITER);
			String word = split[0];
			int wordKey = td.getKeyFromWord(word);
			// for each possible tag of this datum, we have one state.
			for(int possibleTag : td.getTagDictForWord(wordKey)) {
				if(debugMode) System.out.printf("possible tag:%d aka %s word:%s\n", possibleTag, td.getTagFromKey(possibleTag), word);
				String stateKey = TagDict.makeKey(possibleTag, i);
				// initialize all states with value specified
				forwardValues.put(stateKey, new Probability(0));
			}
		}
	}
	/**
	 * Initialize the HMM with states specified by the tags in the test data
	 * and with the value specified.
	 * @param testData
	 * @param value
	 */
	private void initializeBackward(ArrayList<String> testData) {
		// initialize end state at 1...
		String endKey = TagDict.makeKey(td.getKeyFromWord(TagDict.SENTENCE_BOUNDARY), testData.size()-1);
		backwardValues.put(endKey, new Probability(1));
		// ... and all other states at value specified
		// for all datum in test:
		for (int i = testData.size() - 1; i > 0; i--) {
			String datum = testData.get(i);
			String[] split = datum.split(WORD_TAG_DELIMITER);
			String word = split[0];
			int wordKey = td.getKeyFromWord(word);
			// for each possible tag of this datum, we have one state.
			for (int possibleTag : td.getTagDictForWord(wordKey)) {
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
		// for int i = 1 to n (ranges over all test data(
		// key = tag,time
		Map<String, Probability> stateValues;
		if(forward) stateValues = forwardValues;
		else stateValues = backwardValues;
		// for all datum in test:
		for (int i = 1; i < testData.size(); i++) {
			String datum = testData.get(i), prevDatum = testData.get(i-1);
			String[] split = datum.split(WORD_TAG_DELIMITER), prevSplit = prevDatum.split(WORD_TAG_DELIMITER);
			String word = split[0], prevWord = prevSplit[0];
			int wordKey = td.getKeyFromWord(word), prevWordKey = td.getKeyFromWord(prevWord);
			// for each possible tag of this datum, we have one state.
			for(int possibleTag : td.getTagDictForWord(wordKey)) {
				String stateKey = TagDict.makeKey(possibleTag, i);
				// for each possible tag of the previous datum...
				for(int prevPossibleTag : td.getTagDictForWord(prevWordKey)) {
					// arc prob = p(tag | prev tag) * p(word | tag)
					Probability arcProb = td.getBackoffProbTagGivenPrevTag(possibleTag, prevPossibleTag);
					arcProb = arcProb.product(td.getBackoffProbWordGivenTag(wordKey, possibleTag));
					// mu = mu t-1 (i-1) * arc prob
					Probability prevBest = stateValues.get(TagDict.makeKey(prevPossibleTag, i-1));
					Probability mu = prevBest.product(arcProb);
					// get current best and compare
					Probability currentBest = stateValues.get(stateKey);
					if(mu.getLogProb() > currentBest.getLogProb()) {
						// we have a new max!
						stateValues.put(stateKey, mu);
						// and store back pointer
						viterbiBackpointers.put(stateKey, prevPossibleTag);
					}
				}
			}
		}  
	}
	/**
	 * Given the test data length, and the backpointers map,
	 * follow backpointers and assemble the best tagging.
	 * Prints the comparison results at the same time.
	 * Overall accuracy considers all tokens except ###
	 * Known-word accuracy considers all words except ### that appears in training data
	 * Novel-word accuracy considers all words except ### that don't appear in training data
	 * 
	 * @param testDataLength
	 * @param backpointers a map of (tag, time) -> prev tag backpointers
	 * @return the resulting array of tag keys, in order from 1 to n
	 */
	private int[] getCompareResultFromBackpointers(ArrayList<String> testData, Map<String, Integer> backpointers) {
		// init count of correct tags (note we know the number of total tags is N)
		double totalCorrectTags = 0, totalKnownWords = 0, totalKnownTaggedCorrectly = 0,
				totalNovelWords = 0, totalNovelTaggedCorrectly = 0, numberSentenceBoundaries = 0;
		// declare array and follow back pointers
		int[] result = new int[testData.size()];
		// begin at the ending boundary
		int currTag = td.getKeyFromTag(TagDict.SENTENCE_BOUNDARY);
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
				return null;
			}
			// update counts
//			Integer obsPrevWord = td.getKeyFromWord(datum[0]); // may be null if unknown word
			int obsPrevTag = td.getKeyFromTag(datum[1]); // should never be null - we assume we've seen all tags
			if(obsPrevTag == prevTag) {
				totalCorrectTags++;
			}
			// was the word known?
			if(td.knowsWord(datum[0])) {
				totalKnownWords++;
				if(obsPrevTag == prevTag) {
					totalKnownTaggedCorrectly++;
				}
			}
			else {
				totalNovelWords++;
				if(obsPrevTag == prevTag) {
					totalNovelTaggedCorrectly++;
				}
			}
			// was the word our sentence boundary? track this number, to subtract later
			if(obsPrevTag == td.getKeyFromTag(TagDict.SENTENCE_BOUNDARY)) {
				numberSentenceBoundaries++;
			}
			// and continue following
			currTag = prevTag;
		}
		if(debugMode) System.out.println("num ###:"+numberSentenceBoundaries);
		// now that we've got all our counts, compute the accuracy scores
		// don't count the sentence boundaries when we score
		double overallAccuracy = totalCorrectTags / (n-numberSentenceBoundaries), 
				knownAccuracy = totalKnownTaggedCorrectly / (totalKnownWords - numberSentenceBoundaries),
				novelAccuracy = totalNovelTaggedCorrectly / totalNovelWords;
		if(testData.size() == 0) {
			overallAccuracy = 0;
		}
		if(totalKnownWords == 0) {
			knownAccuracy = 0;
		}
		if(totalNovelWords == 0) {
			novelAccuracy = 0;
		}
		// and get the probability of the sequence, for perplexity per word
		String endKey = TagDict.makeKey(td.getKeyFromWord(TagDict.SENTENCE_BOUNDARY), testData.size()-1);
		Probability finalProb = forwardValues.get(endKey);
		// and print
		System.out.printf("Tagging accuracy (Viterbi decoding): %.4f%% " +
				"(known: %.4f%% " +
				"novel: %.4f%%)\n"
					+"Perplexity per Viterbi-tagged test word: %.4f\n",
					overallAccuracy, knownAccuracy, novelAccuracy, 
					getPerplexityPerTaggedWord(finalProb, n));
		return result;
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
		return Math.exp(-1 * prob.getLogProb() / n);
	}
	public TagDict getTagDict() {return td;}
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
