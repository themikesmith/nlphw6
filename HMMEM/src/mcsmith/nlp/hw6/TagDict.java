package mcsmith.nlp.hw6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class TagDict {
	public enum SMOOTHING {
		none, add1, oneCountSmoothing
	}
	private static boolean debugMode = false;
	public static void setDebugMode(boolean b) {debugMode = b;}
	/**
	 * Marker for the boundary between sentences.  It is its own tag.
	 */
	public static final String SENTENCE_BOUNDARY = "###", OOV = "[==OOV==]";
	/**
	 * Table used to store the global vocab.
	 * this is the training and test and raw vocab.
	 * 
	 * This table stores as keys the plain strings
	 */
	private static Set<String> globalVocab = new HashSet<String>();
	/**
	 * Table used to store the global vocab.
	 * this is the training and test and raw vocab.
	 * 
	 * This table stores as keys the plain strings
	 */
	private static Set<String> vocab = new HashSet<String>();
	static {
		globalVocab.add(OOV);
		vocab.add(OOV);
	}
	/**
	 * 
	 * @return the size of the global vocab
	 */
	public static int getGlobalVocabSize() {
		return globalVocab.size();
	}
	/**
	 * 
	 * @return the size of the global vocab
	 */
	public static int getVocabSize() {
		return vocab.size();
	}
	/**
	 * Table for going from words to integer keys.
	 */
	private static Map<String, Integer> wordsToInts = new HashMap<String, Integer>();
	/**
	 * Table for going from words to integer keys.
	 */
	private static Map<String, Integer> tagsToInts = new HashMap<String, Integer>();
	/**
	 * Table for going from words to integer keys.
	 */
	private static Map<Integer, String> intsToWords = new HashMap<Integer, String>();
	/**
	 * Table for going from words to integer keys.
	 */
	private static Map<Integer, String> intsToTags = new HashMap<Integer, String>();
	/**
	 * Add a word to the dictionary.
	 * Checks for duplicates - will not add a duplicate.
	 * Stores it in the words to ints, and ints to words tables.
	 * Increments the next integer to be used as a converter key.
	 * If the word is not in test data, increment our vocab size for calculations.
	 * @param word
	 * @param inTestData
	 * @return true if changes made, false otherwise
	 */
	public static boolean addWordToDict(String word, boolean inTestData) {
		if(!wordsToInts.containsKey(word)) {
			int number = wordsToInts.keySet().size();
			wordsToInts.put(word, number);
			intsToWords.put(number, word);
			if(debugMode) System.out.printf("adding word to global vocab %d:'%s'\n", number, word);
//			if(debugMode) System.out.printf("num words now:%d\n", intsToWords.keySet().size());
//			if(debugMode) System.out.printf("words now:%s\n", intsToWords);
//			if(debugMode) System.out.printf("ints now:%s\n", wordsToInts);
			globalVocab.add(word);
			if(!inTestData) {
				vocab.add(word);
				if(debugMode) System.out.printf("adding word to vocab %d:'%s'\n", number, word);
			}
			return true;
		}
		return false;
	}
	/**
	 * Add a tag to the dictionary.
	 * Checks for duplicates - will not add a duplicate.
	 * Stores it in the tags to ints, and ints to tags tables.
	 * Increments the next integer to be used as a converter key.
	 * @param tag
	 */
	public static void addTagToDict(String tag) {
		if(!tagsToInts.containsKey(tag)) {
			int number = tagsToInts.keySet().size();
			tagsToInts.put(tag, number);
			intsToTags.put(number, tag);
//			if(debugMode) System.out.printf("adding tag %d:'%s'\n", number, tag);
//			if(debugMode) System.out.printf("num tags now:%d\n", intsToTags.keySet().size());
//			if(debugMode) System.out.printf("tags now:%s\n", intsToTags);
		}
	}
	/**
	 * Given a key, get the word string
	 * @param key
	 * @return the word or null if not in dict
	 */
	public static String getWordFromKey(int key) {
		return intsToWords.get(key);
	}
	/**
	 * Given a key, get the word string
	 * @param key
	 * @return the word or null if not in dict
	 */
	public static Integer getKeyFromWord(String word) {
		return wordsToInts.get(word);
	}
	/**
	 * checks if we've seen this word
	 * @param word
	 * @return true if known, false otherwise
	 */
	public static boolean globallyKnowsWord(String word) {
		return wordsToInts.containsKey(word);
	}
	/**
	 * checks if we've seen this word
	 * @param word
	 * @return true if known, false otherwise
	 */
	public static boolean globallyKnowsWord(int word) {
		return intsToWords.containsKey(makeKey(word));
	}
	/**
	 * 
	 * @return map of all ints to words
	 */
	public static Map<Integer, String> getWords() {
		return intsToWords;
	}
	/**
	 * 
	 * @return map of all ints to words
	 */
	public static Map<Integer, String> getTags() {
		return intsToTags;
	}
	/**
	 * Given a key, get the word string
	 * @param key
	 * @return the word or null if not in dict
	 */
	public static String getTagFromKey(int key) {
		return intsToTags.get(key);
	}
	public static Map<Integer, String> getIntsToTags() {
		return intsToTags;
	}
	public static Map<Integer, String> getIntsToWords() {
		return intsToWords;
	}
	/**
	 * Given a key, get the word string
	 * @param key
	 * @return the word or null if not in dict
	 */
	public static int getKeyFromTag(String tag) {
		return tagsToInts.get(tag);
	}

	/**
	 * Takes a variable number of index integers and returns a concatenation, for use as a key.
	 * @param vars
	 * @return the ordered concatenation as a string, space separated.
	 */
	public static String makeKey(Integer... vars) {
		List<Integer> varList = new ArrayList<Integer>(Arrays.asList(vars));
		return makeKey(varList);
	}
	/**
	 * Takes a list of index integers and returns a concatenation, for use as a key.
	 * @param vars
	 * @return the ordered concatenation as a string, space separated.
	 */
	public static String makeKey(List<Integer> vars) {
		StringBuilder sb = new StringBuilder();
		Iterator<Integer> it = vars.iterator();
		while(it.hasNext()) {
			int i = it.next();
			sb.append(i).append(" ");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	
	

	/**
	 * Tracks which smoothing method to use.
	 */
	private SMOOTHING smoother;
	/**
	 * Count used for transmission probabilities.  Shares parameters across time steps.
	 * 
	 * Used for computing p(tag | prev tag) = p(tag, prev tag) / p(prev tag)
	 * 
	 * p(tag, prev tag) = number of times we saw this combo / number tag tokens
	 * p(prev tag) = number of times we saw the tag / number tag tokens
	 * 
	 * Stores number of tag tokens
	 */
	private double numberTagTokens;
	private double numberTagTokensOriginal;
	private double numberTagTokensNew;
	/**
	 * Table used for transmission probabilities.  Shares parameters across time steps.
	 * Used for computing p(tag | prev tag) = p(tag, prev tag) / p(prev tag)
	 * 
	 * p(tag, prev tag) = number of times we saw this combo / number tag tokens
	 * p(prev tag) = number of times we saw this prev tag context / number tag tokens
	 * 
	 * Stores c(tag, prev tag)
	 * 
	 * This table stores as keys the integer keys for 'tag t, tag t-1' 
	 * and as values the number of times we observed the first tag to follow the second.
	 */
	private Map<String, Probability> countTagPrevTag;
	private Map<String, Probability> countTagPrevTagOriginal;
	private Map<String, Probability> countTagPrevTagNew;
	/**
	 * Table used for transmission probabilities.  Shares parameters across time steps.
	 * Used for computing p(tag | prev tag) = p(tag, prev tag) / p(prev tag)
	 * 
	 * p(tag, prev tag) = number of times we saw this combo / number tag tokens
	 * p(prev tag) = number of times we saw this prev tag context / number tag tokens
	 * 
	 * Stores c(prev tag)
	 * This table stores as keys the integer keys for 'tag t-1' 
	 * and as values the number of times we observed the first tag to follow the second.
	 */
	private Map<String, Probability> countPrevTag;
	private Map<String, Probability> countPrevTagOriginal;
	private Map<String, Probability> countPrevTagNew;
	/**
	 * Count used for emission probabilities.  Shares parameters across time steps.
	 * 
	 * Used for computing p(word | tag) = p(word, tag) / p(tag)
	 * 
	 * p(word, tag) = number of times we saw this combo / number combos
	 * p(tag) = number of times we saw this tag / number tag tokens
	 * 
	 * Stores number of word tag tokens
	 */
	private double numberWordTagTokens;
	private double numberWordTagTokensOriginal;
	private double numberWordTagTokensNew;
	/**
	 * Table used for emission probabilities.  Shares parameters across time steps.
	 * 
	 * Used for computing p(word | tag) = p(word, tag) / p(tag)
	 * 
	 * p(word, tag) = number of times we saw this combo / number combos
	 * p(tag) = number of times we saw this tag / number tag tokens
	 * 
	 * Stores c(word, tag)
	 * This table stores as keys the integer keys for 'word t, tag t' 
	 * and as values the number of times we observed the word to receive the tag.
	 */
	private Map<String, Probability> countWordTag;
	private Map<String, Probability> countWordTagOriginal;
	private Map<String, Probability> countWordTagNew;
	/**
	 * Table used for backed off emission probabilities.  Shares parameters across time steps.
	 * 
	 * Used for computing p(word | tag) = (count(word) + 1) / (n + V)
	 * 
	 * Stores count(word)
	 * This table stores as keys the integer keys for 'word t' 
	 * and as values the number of times we observed the word.
	 */
	private Map<String, Probability> countWord;
	private Map<String, Probability> countWordOriginal;
	private Map<String, Probability> countWordNew;
	/**
	 * Counts the number of singleton tag types for each prev tag
	 * stores the number of tag types t such that count(t, t-1) = 1
	 */
	private Map<String, Integer> countSingletonTagPrevTag;
	/**
	 * Counts the number of singleton word types, for each tag
	 * stores the number of word types t such that count(w, t) = 1
	 */
	private Map<String, Integer> countSingletonWordTags;
	/**
	 * Map for going from a word to all tags that were observed with the word
	 */
	private Map<String, ArrayList<Integer> > wordTagDictionary;
	
	/**
	 * Initializes a TagDict.
	 * Adds the SENTENCE_BOUNDARY word to the dict.
	 */
	public TagDict() {
		numberTagTokens = 0;
		numberTagTokensOriginal = 0;
		numberTagTokensNew = 0;
		countTagPrevTag = new HashMap<String, Probability>();
		countTagPrevTagOriginal = new HashMap<String, Probability>();
		countTagPrevTagNew = new HashMap<String, Probability>();
		countSingletonTagPrevTag = new HashMap<String, Integer>();
		countSingletonWordTags = new HashMap<String, Integer>();
		countPrevTag = new HashMap<String, Probability>();
		countPrevTagOriginal = new HashMap<String, Probability>();
		countPrevTagNew = new HashMap<String, Probability>();
		numberWordTagTokens = 0;
		numberWordTagTokensOriginal = 0;
		numberWordTagTokensNew = 0;
		countWordTag = new HashMap<String, Probability>();
		countWordTagOriginal = new HashMap<String, Probability>();
		countWordTagNew = new HashMap<String, Probability>();
		countWord = new HashMap<String, Probability>();
		countWordOriginal = new HashMap<String, Probability>();
		countWordNew = new HashMap<String, Probability>();
		wordTagDictionary = new HashMap<String, ArrayList<Integer> >();
		smoother = SMOOTHING.none;
	}
	public void setSmoother(SMOOTHING sm) {
		smoother = sm;
	}
	
	private Probability getWordCount(int word) {
		String key = makeKey(word);
		if(countWord.containsKey(key)) {
			return countWord.get(key);
		}
		else return Probability.ZERO;
	}
	/**
	 * Adds a tag to a word's dictionary of possible tags.
	 * Does NOT check for duplicates.
	 * @param word
	 * @param tag
	 */
	private void addPossibleTagForWord(int word, int tag) {
		String key = makeKey(word);
		if(!wordTagDictionary.containsKey(key)) { // init with empty list if necessary
			wordTagDictionary.put(key, new ArrayList<Integer>());
		}
		wordTagDictionary.get(key).add(tag);
	}
	
	/**
	 * Adds the word to the dict if necesary.
	 * Then Increments our count of the given word.
	 * @param word
	 */
	public void incrementCountOfWord(String word) {
		initWordCounter(word);
		String key = makeKey(getKeyFromWord(word));
		countWord.put(key, countWord.get(key).add(Probability.ONE));
	}
	
	/**
	 * Increases our vocab size if need be, 
	 * by initializing the count of the word and incrementing our vocab size
	 * @param word the word string
	 */
	public void initWordCounter(String word) {
		int w = getKeyFromWord(word);
		String key = makeKey(w);
		if(!countWord.containsKey(key)) { // init with 0 if necessary
			countWord.put(key, Probability.ZERO);
		}
	}
	/**
	 * Get the tag dictionary for a given word
	 * @param word
	 * @return the list of specific tags if word is known, or list of (all tags - ###) if unknown word
	 */
	public List<Integer> getTagDictForWord(int word) {
		String key = makeKey(word);
		if(wordTagDictionary.containsKey(key)) {
			return wordTagDictionary.get(key);
		}
		else {
			Set<Integer> allTags = new TreeSet<Integer>(intsToTags.keySet());
			allTags.remove(getKeyFromTag(SENTENCE_BOUNDARY));
			return new ArrayList<Integer>(allTags);
		}
	}
	
	

	/**
	 * Checks if we've seen this tag / prev tag pair
	 * @param tag
	 * @param prevTag
	 * @return true if seen, false otherwise
	 */
	public boolean seenTagPair(int tag, int prevTag) {
		String key = makeKey(tag, prevTag);
		return countTagPrevTag.containsKey(key);
	}
	/**
	 * Checks if we've seen this word / tag pair 
	 * @param word
	 * @param tag
	 * @return true if seen, false otherwise
	 */
	public boolean seenWordWithTag(int word, int tag) {
		String key = makeKey(word, tag);
		return countWordTag.containsKey(key);
	}
	/**
	 * 
	 * @return map of all word, tag counts
	 */
	public Map<String, Probability> getCountsWordTag() {
		return countWordTag;
	}
	/**
	 * 
	 * @return map of all tag, prev tag counts
	 */
	public Map<String, Probability> getCountsTagPrevTag() {
		return countTagPrevTag;
	}
	/**
	 * Increments the observed transmission count 
	 * c(curr tag, prev tag)
	 * @param currTag
	 * @param prevTag
	 */
	public void incrementObservedTransmissionCount(int currTag, int prevTag) {
		String key = makeKey(currTag, prevTag);
		if(!countTagPrevTag.containsKey(key)) {
			countTagPrevTag.put(key, Probability.ZERO);
		}
		countTagPrevTag.put(key, countTagPrevTag.get(key).add(Probability.ONE));
//		if(debugMode) System.out.printf("count(%d aka %s, %d aka %s) = %s\n", currTag, getTagFromKey(currTag), prevTag, getTagFromKey(prevTag), countTagPrevTag.get(key));
		// and now check singletons
		if(countTagPrevTag.get(key).equals(Probability.ONE)) {
//			if(debugMode) System.out.printf("increment prev tag singleton:(%s)\n",getTagFromKey(prevTag));
			// increment appropriate singleton count
			incrementSingletonTagPrevTag(prevTag);
		}
		else if(countTagPrevTag.get(key).equals(Probability.TWO)) {
//			if(debugMode) System.out.printf("decrement prev tag singleton:(%s)\n",getTagFromKey(prevTag));
			// decrement appropriate singleton count
			decrementSingletonTagPrevTag(prevTag);
		}
	}
	/**
	 * Increments the singleton prev tag count for this tag
	 * @param prevTag
	 */
	private void incrementSingletonTagPrevTag(int prevTag) {
		String singletonKey = makeKey(prevTag);
		if(!countSingletonTagPrevTag.containsKey(singletonKey)) {
			countSingletonTagPrevTag.put(singletonKey, 0);
		}
		countSingletonTagPrevTag.put(singletonKey, countSingletonTagPrevTag.get(singletonKey) + 1);
	}
	/**
	 * Decrements the singleton prev tag count for this tag
	 * @param prevTag
	 */
	private void decrementSingletonTagPrevTag(int prevTag) {
		String singletonKey = makeKey(prevTag);
		if(!countSingletonTagPrevTag.containsKey(singletonKey)) {
			countSingletonTagPrevTag.put(singletonKey, 0);
		}
		countSingletonTagPrevTag.put(singletonKey, countSingletonTagPrevTag.get(singletonKey) - 1);
		if(countSingletonTagPrevTag.get(singletonKey) < 0) {
			countSingletonTagPrevTag.put(singletonKey, 0);			
		}
	}
	private int getSingletonTagPrevTagCount(int prevTag) {
		String key = makeKey(prevTag);
		if(countSingletonTagPrevTag.containsKey(key)) {
			return countSingletonTagPrevTag.get(key);
		}
		else return 0;
	}

	/**
	 * Increments the singleton prev tag count for this tag
	 * @param prevTag
	 */
	private void incrementSingletonWordTags(int tag) {
		String singletonKey = makeKey(tag);
		if(!countSingletonWordTags.containsKey(singletonKey)) {
			countSingletonWordTags.put(singletonKey, 0);
		}
		countSingletonWordTags.put(singletonKey, countSingletonWordTags.get(singletonKey) + 1);
	}
	/**
	 * Decrements the singleton prev tag count for this tag
	 * @param tag
	 */
	private void decrementSingletonWordTags(int tag) {
		String singletonKey = makeKey(tag);
		if(!countSingletonWordTags.containsKey(singletonKey)) {
			countSingletonWordTags.put(singletonKey, 0);
		}
		countSingletonWordTags.put(singletonKey, countSingletonWordTags.get(singletonKey) - 1);
		if(countSingletonWordTags.get(singletonKey) < 0) {
			countSingletonWordTags.put(singletonKey, 0);			
		}
	}
	private int getSingletonWordTagCount(int tag) {
		String key = makeKey(tag);
		if(countSingletonWordTags.containsKey(key)) {
			return countSingletonWordTags.get(key);
		}
		else return 0;
	}
	
	/**
	 * Increments the observed transmission count 
	 * c(word, tag)
	 * @param word
	 * @param tag
	 */
	public void incrementObservedEmissionCount(int word, int tag) {
		String key = makeKey(word, tag);
//		if(debugMode) System.out.println("incrementing emission count:"+key);
		if(!countWordTag.containsKey(key)) {
			countWordTag.put(key, Probability.ZERO);
//			if(debugMode) System.out.printf("adding (%d = %d) aka (%s = %s) to dict\n", word, tag, getWordFromKey(word), getTagFromKey(tag));
			// and add tag to list of possible tags for word
			addPossibleTagForWord(word, tag); // add once
			if(debugMode) {
				List<Integer> tagsForWord = wordTagDictionary.get(makeKey(word));
				System.out.println("tag dict now:");
				System.out.printf("%d -> ",word);
				System.out.println(tagsForWord.toString());
				System.out.printf("aka:\n %s -> [", getWordFromKey(word));
				for(int i : tagsForWord) {
					System.out.printf("%s, ",getTagFromKey(i));
				}
				System.out.println("]");
			}
		}
		countWordTag.put(key, countWordTag.get(key).add(Probability.ONE));
//		if(debugMode) System.out.printf("key:%s count(%s, %s) = %s\n", key, getWordFromKey(word), getTagFromKey(tag), countWordTag.get(key));
		// and now check singletons
		if(countWordTag.get(key).equals(Probability.ONE)) {
//			if(debugMode) System.out.printf("increment word tag singleton:(%s)\n",getTagFromKey(tag));
			incrementSingletonWordTags(tag);
		}
		else if(countWordTag.get(key).equals(Probability.TWO)) {
//			if(debugMode) System.out.printf("decrement word tag singleton:(%s)\n",getTagFromKey(tag));
			decrementSingletonWordTags(tag);
		}
	}
	/**
	 * Increments the number of word/tag tokens we've seen
	 */
	public void incrementNumberTaggedWordTokens() {
		numberWordTagTokens++;
	}
	/**
	 * Increments the number of tag tokens we've seen
	 */
	public void incrementNumberTagTokens() {
		numberTagTokens++;
	}
	/**
	 * Increments the number of word/tag tokens we've seen
	 */
	public void incrementNumberTaggedWordTokensNew() {
		numberWordTagTokensNew++;
	}
	/**
	 * Increments the number of tag tokens we've seen
	 */
	public void incrementNumberTagTokensNew() {
		numberTagTokensNew++;
	}
	/**
	 * Increments the count of this tag
	 * @param tag
	 */
	public void incrementTimesSeenTagContext(int tag) {
		String key = makeKey(tag);
		if(!countPrevTag.containsKey(key)) {
			countPrevTag.put(key, Probability.ZERO);
		}
		countPrevTag.put(key, countPrevTag.get(key).add(Probability.ONE));
//		if(debugMode) System.out.printf("count(%d aka %s) = %s\n", tag, getTagFromKey(tag), countPrevTag.get(key));
	}
	/**
	 * Get the count of times the current tag was followed by the previous tag
	 * @param currTag
	 * @param prevTag
	 * @return the count
	 */
	public Probability getTransmissionCount(int currTag, int prevTag) {
		String key = makeKey(currTag, prevTag);
		if(countTagPrevTag.containsKey(key))
			return countTagPrevTag.get(makeKey(currTag, prevTag));
		else return Probability.ZERO;
	}
	/**
	 * Set the count of times the current tag was followed by the previous tag
	 * @param currTag
	 * @param prevTag
	 * @param newCount the count
	 */
	public void setTransmissionCount(int currTag, int prevTag, Probability newCount) {
		String key = makeKey(currTag, prevTag);
		if(countTagPrevTag.containsKey(key)) {
			countTagPrevTag.put(key, newCount);
		}
	}
	/**
	 * Get the count of times we observed the tag
	 * @param tag
	 * @return the count
	 */
	public Probability getTagContextCount(int tag) {
		if(countPrevTag.containsKey(makeKey(tag))) {
			return countPrevTag.get(makeKey(tag));
		}
		else return Probability.ZERO;
	}
	/**
	 * Set the count of times we observed the tag
	 * @param tag
	 * @param count the new count
	 */
	public void setTagContextCount(int tag, Probability newCount) {
		if(countPrevTag.containsKey(makeKey(tag))) {
			countPrevTag.put(makeKey(tag), newCount);	
		}
	}
	/**
	 * Get the count of times the word was tagged with the tag
	 * @param word
	 * @param tag
	 * @return the count
	 */
	public Probability getEmissionCount(int word, int tag) {
		String key = makeKey(word, tag);
		if(countWordTag.containsKey(key))
			return countWordTag.get(makeKey(word, tag));
		else return Probability.ZERO;
	}
	/**
	 * Set the count of times the word was tagged with the tag
	 * @param word
	 * @param tag
	 * @param newCount
	 */
	public void setEmissionCount(int word, int tag, Probability newCount) {
		String key = makeKey(word, tag);
		if(countWordTag.containsKey(key)) {
			countWordTag.put(key, newCount);
		}
	}
	/**
	 * Gets the number of word/tag tokens
	 * @return the number
	 */
	public double getNumberWordTagTokens() {
		return numberWordTagTokens;
	}
	/**
	 * Gets the number of tag tokens
	 * @return the number
	 */
	public double getNumberTagTokens() {
		return numberTagTokens;
	}
	public double getNumberTagTokensOriginal() {
		return numberTagTokensOriginal;
	}
	public double getNumberTagTokensNew() {
		return numberTagTokensNew;
	}
	public double getNumberWordTagTokensOriginal() {
		return numberWordTagTokensOriginal;
	}
	public double getNumberWordTagTokensNew() {
		return numberWordTagTokensNew;
	}
	
	public void setNumberTagTokens(double d) {
		numberTagTokens = d;
	}
	public void setNumberTagTokensOriginal(double  d) {
		numberTagTokensOriginal = d;
	}
	public void setNumberTagTokensNew(double  d) {
		numberTagTokensNew = d;
	}
	public void setNumberWordTagTokens(double d) {
		numberWordTagTokens = d;
	}
	public void setNumberWordTagTokensOriginal(double  d) {
		numberWordTagTokensOriginal = d;
	}
	public void setNumberWordTagTokensNew(double  d) {
		numberWordTagTokensNew = d;
	}
	
	/**
	 * 
	 * @param word
	 * @return true if this tag dict has seen the word, else false
	 */
	public boolean seenWord(int word) {
		return countWord.containsKey(makeKey(word));
	}
	/**
	 * 
	 * @param word
	 * @return true if this tag dict has seen the word, else false
	 */
	public boolean seenWord(String word) {
		int wordKey = getKeyFromWord(word);
		return countWord.containsKey(makeKey(wordKey));
	}
	/**
	 * 
	 * @param word
	 * @return true if this tag dict has seen the word, else false
	 */
	public boolean seenWordOriginal(String word) {
		int wordKey = getKeyFromWord(word);
		return countWordOriginal.containsKey(makeKey(wordKey));
	}
	/**
	 * Computes p(tag | previous tag) = the transmission probability.
	 * Requires that we've trained.
	 * 
	 * Adds the possibility of backoff.
	 * 
	 */
	public Probability getSmoothedProbTagGivenPrevTag(int tag, int prevTag) {
		if(smoother.equals(SMOOTHING.oneCountSmoothing)) {
			if(debugMode) System.out.printf("backoff prob tag prev tag\np(%s | %s)\n", getTagFromKey(tag), getTagFromKey(prevTag));
			// really small number for lambda -> 1e-100, or 1
//			Probability lambda = new Probability(0);
			Probability lambda = new Probability(Math.pow(10, -100));
//			Probability lambda = new Probability(1);
//			if(debugMode) System.out.println("lambda:"+lambda);
			// lambda = count of singletons of prev tag
//			if(debugMode) System.out.printf("singleton tag count for %s:%s\n", getTagFromKey(prevTag), getSingletonTagPrevTagCount(prevTag));
			lambda = lambda.add(new Probability(getSingletonTagPrevTagCount(prevTag)));
//			if(debugMode) System.out.println("lambda = lambda + singleton count\nlambda:"+lambda);
			// p backoff = p unsmoothed = (count (tag)) / (n)
			Probability n = new Probability(numberTagTokens);
//			Probability n = new Probability(numberTagTokens - 1);
			Probability pttBackoff = new Probability(getTagContextCount(tag));
//			if(debugMode) System.out.printf("count(%s):%s\n",getTagFromKey(tag), pttBackoff);
//			if(debugMode) System.out.printf("n:%s\n",n);
			pttBackoff = pttBackoff.divide(n);
//			if(debugMode) System.out.printf("pttbackoff:%s\n", pttBackoff);
			// p = (count(tag, prevtag) + lambda * pttbackoff) / (count(prevtag) + lambda)
			Probability numerator = lambda.product(pttBackoff);
//			if(debugMode) System.out.println("lambda * backoff:"+numerator);
//			if(debugMode) System.out.println("c(tag,prevtag):"+new Probability(getTransmissionCount(tag, prevTag)));
			numerator = numerator.add(new Probability(getTransmissionCount(tag, prevTag)));
//			if(debugMode) System.out.println("c(tag,prevtag) + lambda * backoff:"+numerator);
			Probability denominator = lambda.add(new Probability(getTagContextCount(prevTag)));
//			if(debugMode) System.out.println("lambda + count(prevTag):"+denominator);
			if(debugMode) System.out.println(" = "+numerator.divide(denominator));
			return numerator.divide(denominator);
		}
		else {
			System.out.println("smoother not set");
			// no other smoothing implemented. return normal prob
			return getProbTagGivenPrevTag(tag, prevTag);
		}
	}
	/**
	 * Computes p(word | tag) = the emission probability.
	 * Requires that we've trained.
	 * 
	 * Adds the possibility of backoff.
	 * 
	 */
	public Probability getSmoothedProbWordGivenTag(int word, int tag) {
		if(smoother.equals(SMOOTHING.oneCountSmoothing)) {
			if(debugMode) System.out.printf("backoff prob word given tag\np(%s | %s)\n", getWordFromKey(word), getTagFromKey(tag));
			// really small number for lambda -> 1e-100, or 1
//			Probability lambda = new Probability(0);
			Probability lambda = new Probability(Math.pow(10, -100));
//			Probability lambda = new Probability(1);
//			if(debugMode) System.out.println("lambda:"+lambda);
			// lambda = singleton count - backoff proportional to the open-ness of a tag class
//			if(debugMode) System.out.printf("singleton word count for %s:%s\n", getTagFromKey(tag), new Probability(getSingletonWordTagCount(tag)));
			lambda = lambda.add(new Probability(getSingletonWordTagCount(tag)));
//			if(debugMode) System.out.println("lambda = lambda + singleton count\nlambda:"+lambda);
			if(tag == getKeyFromTag(SENTENCE_BOUNDARY)) {
//				if(debugMode) System.out.println("but set lambda = 0 if "+SENTENCE_BOUNDARY);
				// force lambda to be 0 if ### so we never smooth
				lambda = new Probability(0);
			}
			// we assume we always will have observed the tag set. this will never return null
			// p backoff = p unsmoothed = (count (word) + 1) / (n + V)
			// n = number tag tokens in training
			double n = numberTagTokens;
//			int n = numberTagTokens - 1;
			Probability ptwBackoff = new Probability( getWordCount(word).add(Probability.ONE) );
//			if(debugMode) System.out.printf("count(%s):%s\n",getWordFromKey(word), ptwBackoff);
//			if(debugMode) System.out.printf("n:%.1f V:%d n+V:%.1f\n",n, getVocabSize(), n+getVocabSize());
			ptwBackoff = ptwBackoff.divide(new Probability(n + getVocabSize()));
//			if(debugMode) System.out.printf("ptw backoff:%s\n", ptwBackoff);
			// now we have:
			// (count(word, tag) + lambda * pttbackoff) / (count(tag) + lambda)
			Probability numerator = lambda.product(ptwBackoff);
//			if(debugMode) System.out.println("lambda times backoff:"+numerator);
//			if(debugMode) System.out.println("c(word,tag):"+new Probability(getEmissionCount(word, tag)));
			numerator = numerator.add(new Probability(getEmissionCount(word, tag)));
//			if(debugMode) System.out.println("c(word,tag) + lambda times backoff:"+numerator);
			Probability denominator = new Probability(getTagContextCount(tag));
			denominator = denominator.add(lambda);
//			if(debugMode) System.out.println("count(tag) + lambda:"+denominator);
			if(debugMode) System.out.println(" = "+numerator.divide(denominator));
			return numerator.divide(denominator);
		}
		else if(smoother.equals(SMOOTHING.add1)) {
			if(debugMode) System.out.println("add 1 smoothing");
			// we have initialized at 1
			Probability pWordTag = new Probability(getEmissionCount(word, tag));
			pWordTag = pWordTag.divide(new Probability(numberWordTagTokens));
			Probability pTag = new Probability(getTagContextCount(tag));
			pTag = pTag.divide(new Probability(numberTagTokens));
			Probability pWordGivenTag = pWordTag.divide(pTag);
			return pWordGivenTag;
			
		}
		else {
			if(debugMode) System.out.println("smoother not set");
			// no other smoothing implemented. return normal prob
			return getProbWordGivenTag(word, tag);
		}
	}
	/**
	 * Computes p(tag | previous tag) = the transmission probability.
	 * Requires that we've trained so far.
	 * 
	 * p(tag | prev tag) = p(tag  ^ prevtag) / p(prev tag)
	 * p(tag ^ prev tag) = transmission count (tag, prev tag) / ntagtokens
	 * p(prev tag) = tag context count(prev tag) / ntagtokens
	 * p(tag ^ prev tag) / p(prev tag) = transmission count (tag, prev tag) / tag context count(prev tag) 
	 * 
	 * @param tag
	 * @param prevTag
	 * @return the probability
	 */
	public Probability getProbTagGivenPrevTag(int tag, int prevTag) {
		Probability pTagPrevTag = new Probability(getTransmissionCount(tag, prevTag));
		pTagPrevTag = pTagPrevTag.divide(new Probability(numberTagTokens));
		Probability pPrevTag = new Probability(getTagContextCount(prevTag));
		pPrevTag = pPrevTag.divide(new Probability(numberTagTokens));
		Probability pTagGivenPrevTag = pTagPrevTag.divide(pPrevTag);
		return pTagGivenPrevTag;
	}
	/**
	 * Computes p(word | tag) = the emission probability.
	 * Requires that we've trained so far.
	 * p(word|tag) = p(word ^ tag) / p(tag)
	 * p(word ^ tag) = emission count (word,tag) / nwordtokens
	 * p(tag) = tag context count(tag) / ntagtokens
	 * p(word ^ tag) / p(tag) = emission count(word,tag) / tag context count(tag)
	 * @param word
	 * @param tag
	 * @return the probability
	 */
	public Probability getProbWordGivenTag(int word, int tag) {
		Probability pWordTag = new Probability(getEmissionCount(word, tag));
		pWordTag = pWordTag.divide(new Probability(numberWordTagTokens));
		Probability pTag = new Probability(getTagContextCount(tag));
		pTag = pTag.divide(new Probability(numberTagTokens));
		Probability pWordGivenTag = pWordTag.divide(pTag);
		return pWordGivenTag;
	}
	public String toString() {
		// debug purposes
		StringBuilder sb = new StringBuilder();
		// prints out all words
		sb.append("\n====== TagDict ======\n");
		sb.append("====== Words ======\n");
		for(int i : intsToWords.keySet()) {
			sb.append("word ").append(i).append(" aka '").append(getWordFromKey(i)).append("'\n");
		}
		// prints out all tags
		sb.append("====== Tags ======\n");
		for(int i : intsToTags.keySet()) {
			sb.append("tag ").append(i).append(" aka '").append(getTagFromKey(i)).append("'\n");
		}
		// print out tag dictionary - for each word, list of possible tags
		sb.append("====== Tag Dictionary ======\n");
		for(String s : wordTagDictionary.keySet()) {
			int word = Integer.parseInt(s);
			sb.append("== word:").append(word).append(" aka ").append(getWordFromKey(word)).append(" ==\n");
			List<Integer> wordTags = wordTagDictionary.get(s);
			sb.append(wordTags.toString()).append("\n");
			sb.append("aka:[");
			for(int i : wordTags) {
				sb.append(getTagFromKey(i)).append(",");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append("]\n");
		}
		sb.append("====== Number word tag tokens ======\n");
		sb.append(numberWordTagTokens);
		sb.append("\n====== Number tag tokens ======\n");
		sb.append(numberTagTokens);
		sb.append("\n====== Global vocab size (including OOV) ======\n");
		sb.append(getGlobalVocabSize()).append(" :: ").append(globalVocab);
		sb.append("\n====== Vocab size (including OOV) ======\n");
		sb.append(getVocabSize()).append(" :: ").append(vocab);
		sb.append("\n====== Word Counts ======\n");
		for(String s : countWord.keySet()) {
			int word = Integer.parseInt(s);
			sb.append("-> word:").append(word).append(" aka ").append(getWordFromKey(word))
			.append(" :: ").append(countWord.get(s)).append("\n");
		}
		sb.append("\n====== Tag Counts ======\n");
		for(String s : countPrevTag.keySet()) {
			int tag = Integer.parseInt(s);
			sb.append("-> tag:").append(tag).append(" aka ").append(getTagFromKey(tag))
			.append(" :: ").append(countPrevTag.get(s)).append("\n");
		}
		sb.append("====== Singleton Tag Counts for Prev Tags ======\n");
		for(String s : countSingletonTagPrevTag.keySet()) {
			int tag = Integer.parseInt(s);
			sb.append("-> tag:").append(tag).append(" aka ").append(getTagFromKey(tag))
			.append(" :: ").append(countSingletonTagPrevTag.get(s)).append("\n");
		}
		sb.append("====== Singleton Word Counts for Tags ======\n");
		for(String s : countSingletonWordTags.keySet()) {
			int tag = Integer.parseInt(s);
			sb.append("-> tag:").append(tag).append(" aka ").append(getTagFromKey(tag))
			.append(" :: ").append(countSingletonWordTags.get(s)).append("\n");
		}
		sb.append("====== (Tag, word) Counts ======\n");
		for(String s : countWordTag.keySet()) {
			String[] split = s.split(" ");
			int word = Integer.parseInt(split[0]), tag = Integer.parseInt(split[1]);
			sb.append("-> word,tag:").append(getWordFromKey(word)).append(",").append(getTagFromKey(tag))
				.append(" :: ").append(countWordTag.get(s)).append("\n");
		}
		sb.append("====== (Tag, prev tag) Counts ======\n");
		for(String s : countTagPrevTag.keySet()) {
			String[] split = s.split(" ");
			int tag = Integer.parseInt(split[0]), prevTag = Integer.parseInt(split[1]);
			sb.append("-> tag,prevTag:").append(getTagFromKey(tag)).append(",").append(getTagFromKey(prevTag))
				.append(" :: ").append(countTagPrevTag.get(s)).append("\n");
		}
		// prints out all emission probabilities
		/**
		 * p(word | tag) = p(word, tag) / p(tag)
		 * 
		 * p(word, tag) = number of times we saw this combo / number combos
		 * p(tag) = number of times we saw this tag / number tag tokens
		 */
		sb.append("====== Emission probabilities ======\n");
		for (String wordTag : countWordTag.keySet()) {
			String[] split = wordTag.split(" ");
			int word = Integer.parseInt(split[0]), tag = Integer
					.parseInt(split[1]);
			sb.append("p(").append(getWordFromKey(word)).append("|")
					.append(getTagFromKey(tag)).append(") = ")
					.append(getProbWordGivenTag(word, tag)).append("\n");
		}
		// and then all transmission probabilities
		/**
		 * p(tag | prev tag) = p(tag, prev tag) / p(prev tag)
		 * 
		 * p(tag, prev tag) = number of times we saw this combo / number tag
		 * tokens p(prev tag) = number of times we saw this prev tag context /
		 * number tag tokens
		 */
		sb.append("====== Transmission probabilities ======\n");
		for (String tagPrevTag : countTagPrevTag.keySet()) {
			String[] split = tagPrevTag.split(" ");
			int tag = Integer.parseInt(split[0]), prevTag = Integer
					.parseInt(split[1]);
			sb.append("p(").append(getTagFromKey(tag)).append("|")
					.append(getTagFromKey(prevTag)).append(") = ")
					.append(getProbTagGivenPrevTag(tag, prevTag)).append("\n");
		}
		return sb.toString();
	}
	/**
	 * Copies the counts from our 'new' tables into the 'current' tables
	 */
	public void setCurrentCountsToNew() {
//		System.out.println("\n...setting current counts to new\n");
		countPrevTag = new HashMap<String, Probability>(countPrevTagNew);
		countTagPrevTag = new HashMap<String, Probability>(countTagPrevTagNew);
		countWord = new HashMap<String, Probability>(countWordNew);
		countWordTag = new HashMap<String, Probability>(countWordTagNew);
		numberTagTokens = numberTagTokensNew;
		numberWordTagTokens = numberWordTagTokensNew;
	}
	/**
	 * Copies the counts from our 'original' tables into the 'new' tables.
	 */
	public void setNewCountsToOriginal() {
//		System.out.println("\n...setting new counts to original\n");
		countPrevTagNew = new HashMap<String, Probability>(countPrevTagOriginal);
		countTagPrevTagNew = new HashMap<String, Probability>(countTagPrevTagOriginal);
		countWordNew = new HashMap<String, Probability>(countWordOriginal);
		countWordTagNew = new HashMap<String, Probability>(countWordTagOriginal);
		numberTagTokensNew = numberTagTokensOriginal;
		numberWordTagTokensNew = numberWordTagTokensOriginal;
	}
	/**
	 * Sets new counts to 0.
	 */
	public void setOriginalCountsToZero() {
//		System.out.println("\n...setting new counts to 0's\n");
		for(String k : countPrevTagOriginal.keySet()) {
			countPrevTagOriginal.put(k, Probability.ZERO);
		}
		for(String k : countTagPrevTagOriginal.keySet()) {
			countTagPrevTagOriginal.put(k, Probability.ZERO);
		}
		for(String k : countWordOriginal.keySet()) {
			countWordOriginal.put(k, Probability.ZERO);
		}
		for(String k : countWordTagOriginal.keySet()) {
			countWordTagOriginal.put(k, Probability.ZERO);
		}
		numberTagTokensOriginal = 0;
		numberWordTagTokensOriginal = 0;
	}
	/**
	 * Copies the counts from our 'current' tables into the 'original' tables
	 */
	public void setOriginalCountsToCurrent() {
//		System.out.println("\n...setting original counts to current\n");
		countPrevTagOriginal = new HashMap<String, Probability>(countPrevTag);
		countTagPrevTagOriginal = new HashMap<String, Probability>(countTagPrevTag);
		countWordOriginal = new HashMap<String, Probability>(countWord);
		countWordTagOriginal = new HashMap<String, Probability>(countWordTag);
		numberTagTokensOriginal = numberTagTokens;
		numberWordTagTokensOriginal = numberWordTagTokens;
	}
	/**
	 * Adds the probabilistic emission count specified
	 * @param wordKey
	 * @param possibleTag
	 * @param pUnigram
	 */
	public void incrementNewObservedEmissionCount(int wordKey, int possibleTag,
			Probability pUnigram) {
//		System.out.printf("incrementing new obs emission count: -> %s,%s  c:%s\n", 
//				getTagFromKey(possibleTag), getWordFromKey(wordKey), pUnigram);
		String key = makeKey(wordKey, possibleTag);
		if(!countWordTagNew.containsKey(key)) {
			if(debugMode) System.out.printf("novel new emission:%s->%s\n", getTagFromKey(possibleTag), getWordFromKey(wordKey));
			countWordTagNew.put(key, Probability.ZERO);
		}
		countWordTagNew.put(key, countWordTagNew.get(key).add(pUnigram));
		// will throw null pointer - make sure we init correctly
	}
	/**
	 * Adds the probabilistic transmission count specified
	 * @param possibleTag
	 * @param prevPossibleTag
	 * @param pBigram
	 */
	public void incrementNewObservedTransmissionCount(int possibleTag,
			int prevPossibleTag, Probability pBigram) {
//		System.out.printf("incrementing new obs transmission count: %s -> %s c:%s\n",
//				getTagFromKey(prevPossibleTag), getTagFromKey(possibleTag), pBigram);
		String key = makeKey(possibleTag, prevPossibleTag);
		if(!countTagPrevTagNew.containsKey(key)) {
			if(debugMode) System.out.printf("novel new transmission:%s->%s\n", getTagFromKey(prevPossibleTag), getTagFromKey(possibleTag));
			countTagPrevTagNew.put(key, Probability.ZERO);
		}
		countTagPrevTagNew.put(key, countTagPrevTagNew.get(key).add(pBigram));
		// will throw null pointer - make sure we init correctly
	}
	/**
	 * Adds the time seen the tag context to our new counts.
	 * Probabilistic counting.
	 * @param prevPossibleTag
	 * @param current
	 */
	public void incrementNewTimesSeenTagContext(int prevPossibleTag, Probability current) {
		String key = makeKey(prevPossibleTag);
//		System.out.printf("incrementing new tag context count: -> %s c:%s\n", 
//				getTagFromKey(prevPossibleTag), current);
		// don't init - we should always have seen this tag first.
		countPrevTagNew.put(key, countPrevTagNew.get(key).add(current));
		// will throw null pointer - make sure we init correctly
	}
	/**
	 * Adds the time seen the word to our new counts.
	 * Always incrementing by 1 - either we see a word or we don't.
	 * @param word
	 */
	public void incrementNewCountOfWord(String word) {
		String key = makeKey(getKeyFromWord(word));
//		System.out.printf("incrementing new word count: word:%s c:%s\n", word, Probability.ONE);
		if(!countWordNew.containsKey(key)) {
			if(debugMode) System.out.println("novel new word:"+word);
			countWordNew.put(key, Probability.ZERO);
		}
		countWordNew.put(key, countWordNew.get(key).add(Probability.ONE));
		// will throw null pointer - make sure we init correctly
	}
	public Map<String, Probability> getCountWordNew() {
		return countWordNew;
	}
	public Map<String, Probability> getCountWord() {
		return countWord;
	}
	public Map<String, Probability> getCountWordOriginal() {
		return countWordOriginal;
	}
	public void printOriginalCounts() {
		System.out.println("\n==== Original Counts ====\n");
		System.out.println(countPrevTagOriginal);
		System.out.println(countTagPrevTagOriginal);
		System.out.println(countWordOriginal);
		System.out.println(countWordTagOriginal);
	}
	public void printCurrentCounts() {
		System.out.println("\n==== Current Counts ====\n");
		System.out.println(countPrevTag);
		System.out.println(countTagPrevTag);
		System.out.println(countWord);
		System.out.println(countWordTag);
	}
	public void printNewCounts() {
		System.out.println("\n==== New Counts ====\n");
		System.out.println(countPrevTagNew);
		System.out.println(countTagPrevTagNew);
		System.out.println(countWordNew);
		System.out.println(countWordTagNew);
	}
}
