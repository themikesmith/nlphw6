package mcsmith.nlp.hw6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TagDict {
	private static boolean debugMode = false;
	public static void setDebugMode(boolean b) {debugMode = b;}
	/**
	 * Marker for the boundary between sentences.  It is its own tag.
	 */
	public static final String SENTENCE_BOUNDARY = "###";
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
	private int numberTagTokens;
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
	private Map<String, Integer> countTagPrevTag;
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
	private Map<String, Integer> countPrevTag;
	/**
	 * Count used for emission probabilities.  Shares parameters across time steps.
	 * 
	 * Used for computing p(word | tag) = p(word, tag) / p(tag)
	 * 
	 * p(word, tag) = number of times we saw this combo / number combos
	 * p(tag) = number of times we saw this tag / number tag tokens
	 * 
	 * Stores number of word tokens
	 */
	private int numberWordTagTokens;
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
	private Map<String, Integer> countWordTag;
	/**
	 * Table for going from words to integer keys.
	 */
	private Map<String, Integer> wordsToInts;
	/**
	 * Table for going from words to integer keys.
	 */
	private Map<String, Integer> tagsToInts; 
	/**
	 * Table for going from words to integer keys.
	 */
	private Map<Integer, String> intsToWords; 
	/**
	 * Table for going from words to integer keys.
	 */
	private Map<Integer, String> intsToTags;
	private Map<String, ArrayList<Integer> > wordTagDictionary;
	
	/**
	 * Initializes a TagDict.
	 * Adds the SENTENCE_BOUNDARY word to the dict.
	 */
	public TagDict() {
		numberTagTokens = 0;
		countTagPrevTag = new HashMap<String, Integer>();
		countPrevTag = new HashMap<String, Integer>();
		numberWordTagTokens = 0;
		countWordTag = new HashMap<String, Integer>();
		wordsToInts = new HashMap<String, Integer>();
		tagsToInts = new HashMap<String, Integer>();
		intsToWords = new HashMap<Integer, String>();
		intsToTags = new HashMap<Integer, String>();
		wordTagDictionary = new HashMap<String, ArrayList<Integer> >();
		addWordToDict(SENTENCE_BOUNDARY);
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
	 * Get the tag dictionary for a given word
	 * @param word
	 * @return the list of specific tags if word is known, or list of all tags if unknown
	 */
	public List<Integer> getTagDictForWord(int word) {
		String key = makeKey(word);
		if(wordTagDictionary.containsKey(key))
			return wordTagDictionary.get(key);
		else
			return new ArrayList<Integer>(intsToTags.keySet());
	}
	
	/**
	 * Add a word to the dictionary.
	 * Checks for duplicates - will not add a duplicate.
	 * Stores it in the words to ints, and ints to words tables.
	 * Increments the next integer to be used as a converter key.
	 * @param word
	 */
	public void addWordToDict(String word) {
		if(!wordsToInts.containsKey(word)) {
			int number = wordsToInts.keySet().size();
			wordsToInts.put(word, number);
			intsToWords.put(number, word);
			if(debugMode) System.out.printf("adding word %d:'%s'\n", number, word);
			if(debugMode) System.out.printf("num words now:%d\n", intsToWords.keySet().size());
			if(debugMode) System.out.printf("words now:%s\n", intsToWords);
		}
	}
	/**
	 * Add a tag to the dictionary.
	 * Checks for duplicates - will not add a duplicate.
	 * Stores it in the tags to ints, and ints to tags tables.
	 * Increments the next integer to be used as a converter key.
	 * @param tag
	 */
	public void addTagToDict(String tag) {
		if(!tagsToInts.containsKey(tag)) {
			int number = tagsToInts.keySet().size();
			tagsToInts.put(tag, number);
			intsToTags.put(number, tag);
			if(debugMode) System.out.printf("adding tag %d:'%s'\n", number, tag);
			if(debugMode) System.out.printf("num tags now:%d\n", intsToTags.keySet().size());
			if(debugMode) System.out.printf("tags now:%s\n", intsToTags);
		}
	}
	/**
	 * Given a key, get the word string
	 * @param key
	 * @return the word or null if not in dict
	 */
	public String getWordFromKey(int key) {
		return intsToWords.get(key);
	}
	/**
	 * Given a key, get the word string
	 * @param key
	 * @return the word or null if not in dict
	 */
	public Integer getKeyFromWord(String word) {
		return wordsToInts.get(word);
	}
	/**
	 * checks if we've seen this word
	 * @param word
	 * @return true if known, false otherwise
	 */
	public boolean knowsWord(String word) {
		return wordsToInts.containsKey(word);
	}
	/**
	 * checks if we've seen this word
	 * @param word
	 * @return true if known, false otherwise
	 */
	public boolean knowsWord(int word) {
		return intsToWords.containsKey(makeKey(word));
	}
	/**
	 * 
	 * @return map of all ints to words
	 */
	public Map<Integer, String> getWords() {
		return intsToWords;
	}
	/**
	 * 
	 * @return map of all ints to words
	 */
	public Map<Integer, String> getTags() {
		return intsToTags;
	}
	/**
	 * 
	 * @return map of all word, tag counts
	 */
	public Map<String, Integer> getCountsWordTag() {
		return countWordTag;
	}
	/**
	 * 
	 * @return map of all tag, prev tag counts
	 */
	public Map<String, Integer> getCountsTagPrevTag() {
		return countTagPrevTag;
	}
	/**
	 * Given a key, get the word string
	 * @param key
	 * @return the word or null if not in dict
	 */
	public String getTagFromKey(int key) {
		return intsToTags.get(key);
	}
	/**
	 * Given a key, get the word string
	 * @param key
	 * @return the word or null if not in dict
	 */
	public int getKeyFromTag(String tag) {
		return tagsToInts.get(tag);
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
			countTagPrevTag.put(key, 0);
		}
		countTagPrevTag.put(key, countTagPrevTag.get(key) + 1);
		if(debugMode) System.out.printf("count(%d aka %s, %d aka %s) = %d\n", currTag, getTagFromKey(currTag), prevTag, getTagFromKey(prevTag), countTagPrevTag.get(key));
	}
	/**
	 * Increments the observed transmission count 
	 * c(word, tag)
	 * @param word
	 * @param tag
	 */
	public void incrementObservedEmissionCount(int word, int tag) {
		String key = makeKey(word, tag);
		if(debugMode) System.out.println("incrementing emission count:"+key);
		if(!countWordTag.containsKey(key)) {
			countWordTag.put(key, 0);
			if(debugMode) System.out.printf("adding (%d = %d) aka (%s = %s) to dict\n", word, tag, getWordFromKey(word), getTagFromKey(tag));
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
		countWordTag.put(key, countWordTag.get(key) + 1);
		if(debugMode) System.out.printf("key:%s count(%s, %s) = %d\n", key, getWordFromKey(word), getTagFromKey(tag), countWordTag.get(key));
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
	 * Increments the count of this tag
	 * @param tag
	 */
	public void incrementTimesSeenTagContext(int tag) {
		String key = makeKey(tag);
		if(!countPrevTag.containsKey(key)) {
			countPrevTag.put(key, 0);
		}
		countPrevTag.put(key, countPrevTag.get(key) + 1);
		if(debugMode) System.out.printf("count(%d aka %s) = %d\n", tag, getTagFromKey(tag), countPrevTag.get(key));
	}
	/**
	 * Get the count of times the current tag was followed by the previous tag
	 * @param currTag
	 * @param prevTag
	 * @return the count
	 */
	public int getTransmissionCount(int currTag, int prevTag) {
		return countTagPrevTag.get(makeKey(currTag, prevTag));
	}
	/**
	 * Set the count of times the current tag was followed by the previous tag
	 * @param currTag
	 * @param prevTag
	 * @param newCount the count
	 */
	public void setTransmissionCount(int currTag, int prevTag, int newCount) {
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
	public int getTagContextCount(int tag) {
		return countPrevTag.get(makeKey(tag));
	}
	/**
	 * Set the count of times we observed the tag
	 * @param tag
	 * @param count the new count
	 */
	public void setTagContextCount(int tag, int newCount) {
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
	public int getEmissionCount(int word, int tag) {
		return countWordTag.get(makeKey(word, tag));
	}
	/**
	 * Set the count of times the word was tagged with the tag
	 * @param word
	 * @param tag
	 * @param newCount
	 */
	public void setEmissionCount(int word, int tag, int newCount) {
		String key = makeKey(word, tag);
		if(countWordTag.containsKey(key)) {
			countWordTag.put(key, newCount);
		}
	}
	/**
	 * Gets the number of word/tag tokens
	 * @return the number
	 */
	public int getNumberWordTagTokens() {
		return numberWordTagTokens;
	}
	/**
	 * Gets the number of tag tokens
	 * @return the number
	 */
	public int getNumberTagTokens() {
		return numberTagTokens;
	}
	/**
	 * Decrement our count of context for the sentence boundary - don't count the last one as context
	 */
	public void decrementBoundaryContextCount() {
		int tagKey = getKeyFromTag(SENTENCE_BOUNDARY), wordKey = getKeyFromWord(SENTENCE_BOUNDARY);
		if(debugMode) System.out.printf("decrementing boundary count:\n%d aka %s\n", tagKey, SENTENCE_BOUNDARY);
		String key = makeKey(tagKey), wordTagKey = makeKey(wordKey, tagKey);
		if(debugMode) System.out.printf("key:%s\n", key);
		if(debugMode) System.out.printf("old value:%d\n", countPrevTag.get(key));
		if(debugMode) System.out.printf("new value:%d\n", countPrevTag.get(key) - 1);
		countPrevTag.put(key, countPrevTag.get(key) - 1);
		countWordTag.put(wordTagKey, countWordTag.get(wordTagKey) - 1);
	}
	/**
	 * Computes p(tag | previous tag) = the transmission probability.
	 * Requires that we've trained so far.
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
	public String toString() {
		// debug purposes
		StringBuilder sb = new StringBuilder();
		// prints out all words
		sb.append("====== Words ======\n");
		for(int i : intsToWords.keySet()) {
			sb.append("word ").append(i).append(" aka '").append(getWordFromKey(i)).append("'\n");
		}
		// prints out all tags
		sb.append("====== Tags ======\n");
		for(int i : intsToTags.keySet()) {
			sb.append("tag ").append(i).append(" aka '").append(getTagFromKey(i)).append("'\n");
		}
		// prints out all emission probabilities
		/**
		 * p(word | tag) = p(word, tag) / p(tag)
		 * 
		 * p(word, tag) = number of times we saw this combo / number combos
		 * p(tag) = number of times we saw this tag / number tag tokens
		 */
		for(String wordTag : countWordTag.keySet()) {
			String[] split = wordTag.split(" ");
			int word = Integer.parseInt(split[0]), tag = Integer.parseInt(split[1]);
			sb.append("p(").append(getWordFromKey(word))
				.append("|").append(getTagFromKey(tag)).append(") = ")
				.append(getProbWordGivenTag(word, tag)).append("\n");
		}
		// and then all transmission probabilities
		/**
		 * p(tag | prev tag) = p(tag, prev tag) / p(prev tag)
		 * 
		 * p(tag, prev tag) = number of times we saw this combo / number tag tokens	
		 * p(prev tag) = number of times we saw this prev tag context / number tag tokens
		 */
		for(String tagPrevTag : countTagPrevTag.keySet()) {
			String[] split = tagPrevTag.split(" ");
			int tag = Integer.parseInt(split[0]), prevTag = Integer.parseInt(split[1]);
			sb.append("p(").append(getTagFromKey(tag))
				.append("|").append(getTagFromKey(prevTag)).append(") = ")
				.append(getProbTagGivenPrevTag(tag, prevTag)).append("\n");
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
		return sb.toString();
	}
}
