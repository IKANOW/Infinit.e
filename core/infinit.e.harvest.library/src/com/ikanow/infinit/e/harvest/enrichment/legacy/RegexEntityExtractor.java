package com.ikanow.infinit.e.harvest.enrichment.legacy;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVParser;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.interfaces.harvest.EntityExtractorEnum;
import com.ikanow.infinit.e.data_model.interfaces.harvest.IEntityExtractor;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo.Dimension;
import com.ikanow.infinit.e.data_model.utils.DimensionUtility;

//TODO: implement and document ... if printRegexThenOutput: true then just exist with the output in a runtime exception

public class RegexEntityExtractor implements IEntityExtractor {

	private static boolean _DEBUG = false;
	
	@Override
	public String getName() {
		return "regex";
	}

	@Override
	public void extractEntities(DocumentPojo partialDoc)
			throws ExtractorDailyLimitExceededException,
			ExtractorDocumentLevelException {
		// TODO Auto-generated method stub

	}

	@Override
	public void extractEntitiesAndText(DocumentPojo partialDoc)
			throws ExtractorDailyLimitExceededException,
			ExtractorDocumentLevelException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getCapability(EntityExtractorEnum capability) {
		// TODO Auto-generated method stub
		return null;
	}

///////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////

	// TOP LEVEL LOGIC - INITIALIZATION
	
	public static class RegexEntityConfig {
		String regexSpec; // Source pattern for regex
		private Pattern regex = null; // Compiled regex object (from regexSpece + flags)
		int flags; // Source flags for regex
		
		Pattern getOrCompileRegex() {
			if (null == regex) {
				return Pattern.compile(regexSpec, flags);
			}
			else {
				return regex;
			}
		}
		String replace; // The replacement string (null if in compressed mode - run over "compressed" to get matches in this case)
		String type; // Entity type
		String dimension; // Entity dimension
		RegexEntityFieldSet fieldSet; // Set of fields to search over
		List<RegexEntityConfig> compressed; // The list of regexes that have been compressed into this object
		int numCompressed = 0;
	}
	public static class RegexEntityFieldSet {
		TreeSet<String> fields; // making it a tree set lets us do prefix matching
		HashMap<Integer, Pattern> fieldRegexes; // (more complex - regexes)
	}

	protected HashMultimap<String, RegexEntityConfig> _fullRegexList, _compressedRegexList;
	protected RegexEntityFieldSet _defaultFieldSet = null;
	protected String _defaultFieldSpec = null;

	private static final Pattern DIMENSIONMATCHER = Pattern.compile("Who|Where|What|When");

	////////////////////////

	protected HashMultimap<String, RegexEntityConfig> intializeConfig(Map<String, String> config) {
		//(probably just for debugging)
		if (null != _fullRegexList) {
			_fullRegexList = null;
		}
		if (null != _compressedRegexList) {
			_compressedRegexList = null;
		}
		
		// Phase 1 ... creste field spec
		for (Map.Entry<String, String> kv: config.entrySet()) {
			try {		
				//DEBUG
				if (_DEBUG) System.out.println("iC1: Phase1: " + kv.getKey() + ": " + kv.getValue());
				
				parseKeyVal(kv.getKey(), kv.getValue(), true);
			}
			catch (Exception e) {}
		}
		if (null == _defaultFieldSet) {
			try {
				parseKeyVal("$", "fullText", true);
			}
			catch (Exception e) {
				if (_DEBUG) e.printStackTrace();								
			}
		}
		// Phase 2 ... create regexes
		for (Map.Entry<String, String> kv: config.entrySet()) {
			try {
				//DEBUG
				if (_DEBUG) System.out.println("iC2: Phase2: " + kv.getKey() + ": " + kv.getValue());
				
				parseKeyVal(kv.getKey(), kv.getValue(), false);
			}
			catch (Exception e) {
				if (_DEBUG) e.printStackTrace();				
			}
		}
		if (null != this._fullRegexList) {
			this.compressRegexes();			
		}
		//DEBUG
		if (_DEBUG) System.out.println("iC3: Compressed from " + _fullRegexList.size() + " to " + _compressedRegexList.size());
		
		return _compressedRegexList;
	}//TESTED (test1 etc)
	
	protected void parseKeyVal(String keyStr, String valueStr, boolean isPhase1) throws IOException
	{
		//step 1) parse key

		String dim = null;
		String fieldSpec = null;
		RegexEntityFieldSet fieldSet = null;
		String type = null;
		
		int entStart = keyStr.lastIndexOf('/');
		if (entStart <= 0) {
			if ('$' == keyStr.charAt(0)) { // saved type, do all these first				
				if (isPhase1) {
					RegexEntityFieldSet savedFieldSet = parseFieldSpec(valueStr, keyStr);
					
					//DEBUG
					if (_DEBUG) System.out.println("pKV1: $ DEFAULT: " + valueStr);
					
					if (null == savedFieldSet) { //(invalid just ignore)
						return;
					}
					if (1 == keyStr.length()) {
						_defaultFieldSet = savedFieldSet;
						_defaultFieldSpec = valueStr;
					}
				}
				return;
			}//TESTED (test1 - default, test2 - fields specified)
			else if (!isPhase1) { // just ent type				
				fieldSet = _defaultFieldSet;
				fieldSpec = _defaultFieldSpec;
				type = keyStr;
				
				//DEBUG
				if (_DEBUG) System.out.println("pKV2: SIMPLE KEY: " + type);							
			}//TESTED (test1)
			else return;
		}
		else { // one of "dim/type" or "fields/dim/type" or "fields/type"
			if (isPhase1) { // phase 1, just saved fields
				return;
			}
			type = keyStr.substring(entStart + 1);
			
			String preEntTypeKeyStr = keyStr.substring(0, entStart);
			int dimStart = preEntTypeKeyStr.lastIndexOf('/');
			if (dimStart > 0) { // 1 or 2 fields (if 1 contains a /)
				//DEBUG
				if (_DEBUG) System.out.println("pKV3a: Most complex key: " + dimStart + " vs " + preEntTypeKeyStr);											
				
				String candidateDim = preEntTypeKeyStr.substring(dimStart + 1);
				if (DIMENSIONMATCHER.matcher(candidateDim).matches()) { // 2nd field is valid dim
					dim = candidateDim;
					fieldSpec = preEntTypeKeyStr.substring(0, dimStart);
				}
				else { // 2nd field isn't valid
					fieldSpec = preEntTypeKeyStr;						
				}
			}//TODO (TOTEST - both cases TEST4/patt2 - valid dimension)
			else if (DIMENSIONMATCHER.matcher(preEntTypeKeyStr).matches()) { // only 1 field, it's a valid dimension, ie default spec
				dim = preEntTypeKeyStr;
				fieldSet = _defaultFieldSet;
				fieldSpec = _defaultFieldSpec;
			}//TESTED (test1, test2)
			else { // Only 1 field, not a valid dim, so must be a spec
				
				fieldSpec = preEntTypeKeyStr;
			}//TESTED (test4, pattern 1)
			//DEBUG
			if (_DEBUG) System.out.println("pKV3+: MORE COMPLEX KEY " + fieldSpec + " from " + keyStr + " /DIM = " + dim);			
		}
		if (null == fieldSet) {
			fieldSet = parseFieldSpec(fieldSpec, null);
			if (null == fieldSet) { // (invalid)
				return;
			}
		}
		if (isPhase1) {
			return;
		}

		//step 2) parse the value

		CSVParser regexParser = new CSVParser('/', (char)0x0, '\\');

		String[] parsedRegex = regexParser.parseLine(valueStr);

		//DEBUG
		if (_DEBUG) System.out.println("pKV4: parsed regex = " + Arrays.toString(parsedRegex));
		
		//0 is "" or s
		//1 is the regex
		//2 is the replace
		//3 are the flags

		String replace = null;
		int flags = 0;
		if (parsedRegex[0].isEmpty()) { // /regex/flags
			if (parsedRegex.length > 2) {
				flags = parseFlags(parsedRegex[2]);
			}			
		}//TESTED (test2)
		else { // s/regex/replace/flags
			if (parsedRegex.length > 3) {
				flags = parseFlags(parsedRegex[3]);
			}
			if ((parsedRegex.length > 2) && !parsedRegex[2].isEmpty()) {
				replace = parsedRegex[2];
			}
		}//TESTED (test1)
		
		RegexEntityConfig regexConfig = new RegexEntityConfig();
		regexConfig.regexSpec = parsedRegex[1];
		regexConfig.flags = flags;
		regexConfig.replace = replace;
		regexConfig.fieldSet = fieldSet;
		regexConfig.compressed = null;
		regexConfig.type = type;
		if (null == dim) {
			Dimension x = DimensionUtility.getDimensionByType(type);
			if (null == x) {
				x = Dimension.What;
			}
			dim = x.toString();
			
			//DEBUG
			if (_DEBUG) System.out.println("pKV5: Guess dim = " + dim + " from " + type);
		}//TESTED (test1)
		regexConfig.dimension = dim;

		if (null == _fullRegexList) {
			_fullRegexList = HashMultimap.create();
		}
		_fullRegexList.put(fieldSpec + "/" + flags, regexConfig);
	}
	
////////////////////////

	protected final static int MAX_REGEXES_TO_COMPRESS = 10;
	protected void compressRegexes() {
		if (null == _compressedRegexList) {
			_compressedRegexList = HashMultimap.create();
		}

		String prevKey = null;
		RegexEntityConfig regexesWithGroups = null, directRegexes = null;
		for (Map.Entry<String, RegexEntityConfig> kv: this._fullRegexList.entries()) {

			//DEBUG
			if (_DEBUG) System.out.println("cR1: key=" + kv.getKey() + ", spec=" + kv.getValue().regexSpec + ", replace=" + kv.getValue().replace + " | PREV_KEY = " + prevKey);
			
			if ((null != prevKey) && prevKey.equals(kv.getKey())) {
				if (null == kv.getValue().replace) {
					directRegexes = combineRegexConfigs(kv.getKey(), directRegexes, kv.getValue());
				}
				else {
					regexesWithGroups = combineRegexConfigs(kv.getKey(), regexesWithGroups, kv.getValue());					
				}
			}//TODO: TOTEST
			else { // key change
				if ((null != prevKey) && (null != regexesWithGroups) && (null == regexesWithGroups.compressed)) {
					// (didn't manage to compress)
					this._compressedRegexList.put(prevKey, regexesWithGroups);

					//DEBUG
					if (_DEBUG) System.out.println("cR2a: added regexesWithGroups");
				}//TESTED (test1)
				if ((null != prevKey) && (null != directRegexes) && (null == directRegexes.compressed)) {
					// (didn't manage to compress)
					this._compressedRegexList.put(prevKey, directRegexes);
					
					//DEBUG
					if (_DEBUG) System.out.println("cR3a: added directRegexes");
				}
				
				prevKey = kv.getKey();
				directRegexes = null;
				regexesWithGroups = null;
				if (null == kv.getValue().replace) {
					directRegexes = kv.getValue();
				}
				else {
					regexesWithGroups = kv.getValue();
				}
			}
		}//end loop over uncompressed regexes
		
		// Handle any uncompressed keys at the end
		if ((null != prevKey) && (null != regexesWithGroups) && (null == regexesWithGroups.compressed)) {
			// (didn't manage to compress)
			this._compressedRegexList.put(prevKey, regexesWithGroups);

			//DEBUG
			if (_DEBUG) System.out.println("cR2b: added regexesWithGroups");
		}
		if ((null != prevKey) && (null != directRegexes) && (null == directRegexes.compressed)) {
			// (didn't manage to compress)
			this._compressedRegexList.put(prevKey, directRegexes);

			//DEBUG
			if (_DEBUG) System.out.println("cR3b: added directRegexes");
		}//TESTED (test1)
	}
	
////////////////////////

	private static final int MAX_COMPRESSED_REGEXES = 10;
	private RegexEntityConfig combineRegexConfigs(String key, RegexEntityConfig regexList, RegexEntityConfig newRegex) {
		if ((null == regexList.compressed) || 
				(regexList.numCompressed >= MAX_COMPRESSED_REGEXES))
		{
			RegexEntityConfig newRegexList = new RegexEntityConfig();
			newRegexList.compressed = new LinkedList< RegexEntityConfig>();
			newRegexList.replace = regexList.replace; // (just care if it's null or not, ie direct/groups)
			newRegexList.flags = regexList.flags; // (the same across all commpressions by construction of key)
			newRegexList.fieldSet = regexList.fieldSet; // (the same across all commpressions by construction of key)

			_compressedRegexList.put(key, newRegexList);
			if (null == regexList.compressed) {
				newRegexList.regexSpec = regexList.regexSpec;
				newRegexList.compressed.add(regexList);
				regexList.numCompressed = 1;
			}
			else {
				newRegexList.regexSpec = newRegex.regexSpec;
				regexList.numCompressed = 0;
			}
			regexList = newRegex;
		}
		else {
			regexList.regexSpec = regexList.regexSpec + "|" + newRegex.regexSpec;			
		}
		regexList.compressed.add(newRegex);
		regexList.numCompressed++;
		return regexList;
	}//TODO: TOTEST

////////////////////////

	private int parseFlags(String flagsStr) {
		int flags = 0;
		for (int i = 0; i < flagsStr.length(); ++i) {
			switch (flagsStr.charAt(i)) {
			case 'i':
				flags |= Pattern.CASE_INSENSITIVE;
				break;
			case 'x':
				flags |= Pattern.COMMENTS;
				break;
			case 's':
				flags |= Pattern.DOTALL;
				break;
			case 'm':
				flags |= Pattern.MULTILINE;
				break;
			case 'u':
				flags |= Pattern.UNICODE_CASE;
				break;
			case 'd':
				flags |= Pattern.UNIX_LINES;
				break;
			}
		}
		return flags;
	}//TESTED (test1, etc)

	private RegexEntityFieldSet combineFieldSets(RegexEntityFieldSet comboSet, RegexEntityFieldSet set) {
		if (null != set.fields) {
			if (null == comboSet.fields) {
				comboSet.fields = new TreeSet<String>();
			}
			comboSet.fields.addAll(set.fields);
		}
		if (null != set.fieldRegexes) {
			if (null == comboSet.fieldRegexes) {
				comboSet.fieldRegexes = set.fieldRegexes;
			}
			else {
				HashMap<Integer, Pattern> newPatterns = new HashMap<Integer, Pattern>();			
				for (Map.Entry<Integer, Pattern> kv: set.fieldRegexes.entrySet()) {
					Pattern comboPattern = comboSet.fieldRegexes.get(kv.getKey());
					if (null == comboPattern) {
						comboPattern = kv.getValue();
					}
					else {
						comboPattern = Pattern.compile(comboPattern.pattern() + "|" + kv.getValue().pattern(), kv.getKey());
					}
					newPatterns.put(kv.getKey(), comboPattern);
				}
				comboSet.fieldRegexes.putAll(newPatterns);
			}
		}
		return comboSet;
	}//TODO: TOTEST

	private HashMap<String, RegexEntityFieldSet> _cachedFieldSets = new HashMap<String, RegexEntityFieldSet>();
	
	private RegexEntityFieldSet parseFieldSpec(String fieldSpec, String savedName) {
		try {
			//DEBUG
			if (_DEBUG) System.out.println("pFS1: Building for spec " + fieldSpec);
			
			RegexEntityFieldSet cached = null;
			if (null == savedName) {
				cached = _cachedFieldSets.get(fieldSpec);
			}
			if (null != cached) {
				//DEBUG
				if (_DEBUG) System.out.println("pFS2: Found cached spec " + fieldSpec);
				
				return cached;
			}//TODO: TOTEST
			RegexEntityFieldSet fieldSet = new RegexEntityFieldSet();
			if ('/' == fieldSpec.charAt(0)) {
				int secondSlash = fieldSpec.lastIndexOf('/');
				if (0 == secondSlash) {
					return null;
				}
				String flags = fieldSpec.substring(secondSlash + 1);
				int regexFlags = 0;
				if (flags.isEmpty()) {
					regexFlags = this.parseFlags(flags);
				}
				fieldSet.fieldRegexes = new HashMap<Integer, Pattern>();
				fieldSet.fieldRegexes.put(regexFlags, Pattern.compile(fieldSpec.substring(1, secondSlash)));
				
				//DEBUG
				if (_DEBUG) System.out.println("pFS3: ADDED " + regexFlags + ": " + Pattern.compile(fieldSpec.substring(1, secondSlash)));				
			}//TESTED (test3)
			else { // parse fields
				String[] fields = fieldSpec.split("\\s*,\\s*");
				for (String field: fields) {
					//DEBUG
					if (_DEBUG) System.out.println("pFS4: FIELD: " + field);				
					
					if ('$' == field.charAt(0)) {
						cached = _cachedFieldSets.get(field);
						if (null != cached) {
							//DEBUG
							if (_DEBUG) System.out.println("pFS5: Combine");				
							
							combineFieldSets(fieldSet, cached);
						}
					}//TODO: TOTEST
					else { // just a doc field
						if (null == fieldSet.fields) {
							fieldSet.fields = new TreeSet<String>();
						}
						fieldSet.fields.add(field);
					}//TESTED (test2)
				}
				//DEBUG
				if (_DEBUG) if (null != fieldSet.fields) System.out.println("pFS6: fields " + Arrays.toString(fieldSet.fields.toArray()));				
			}
			return fieldSet;
		}
		catch (Exception e) {
			if (_DEBUG) e.printStackTrace();
			
			return null;			
		}
	}//TODO: TOTEST

	/////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	
	// TEST CODE

	private static LinkedHashMap<String, String> createEngineConfig(String ...kvs) {
		LinkedHashMap<String, String> config = new LinkedHashMap<String, String>();
		for (int i = 0; i < kvs.length; i += 2) {
			config.put(kvs[i+0], kvs[i+1]);
		}
		return config;
	}
	private static String serializeResult(HashMultimap<String, RegexEntityConfig> result, boolean prettyPrint, boolean encodedForStrings) {
		StringBuffer sb = new StringBuffer();
		GsonBuilder gb = new GsonBuilder();
		if (prettyPrint) {
			gb.setPrettyPrinting();
		}
		gb.registerTypeAdapter(Pattern.class, new RegexSerializer());
		Gson gson = gb.create();
		for (Map.Entry<String, RegexEntityConfig> entry: result.entries()) {
			sb.append(gson.toJson(entry)).append('\n');
		}
		String s = sb.toString();
		if (encodedForStrings) { //put in a format that can be pasted into a string
			return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
		}
		return s;
	}
	
	public static void main(String[] args) {
		//(tests ascertained that pattern() does not include flags, integrated flags apply everywhere, eg across |s)		
		
		RegexEntityExtractor moduleUnderTest = new RegexEntityExtractor();
		
		Map<String, String> config;
		String result;
		String expectedResult;
		HashMultimap<String, RegexEntityConfig> resultMap;
		
		// TEST 1 - simple regexes
		RegexEntityExtractor._DEBUG = false;
		//DEBUG
		//RegexEntityExtractor._DEBUG = true;
		
		config = RegexEntityExtractor.createEngineConfig(
				"Sha256Hash", "/[0-9a-fA-F]{64}/",
				"Who/ExternalIp", "s/(?:^|[^0-9a-z])([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)($|[^0-9a-z])/hash:$1/i"
				);
		
		resultMap = moduleUnderTest.intializeConfig(config);
		
		result = RegexEntityExtractor.serializeResult(resultMap, true, false);
		expectedResult = "{\n  \"key\": \"fullText/2\",\n  \"value\": {\n    \"regexSpec\": \"(?:^|[^0-9a-z])([0-9]+.[0-9]+.[0-9]+.[0-9]+)($|[^0-9a-z])\",\n    \"flags\": 2,\n    \"replace\": \"hash:$1\",\n    \"type\": \"ExternalIp\",\n    \"dimension\": \"Who\",\n    \"fieldSet\": {\n      \"fields\": [\n        \"fullText\"\n      ]\n    },\n    \"numCompressed\": 0\n  }\n}\n{\n  \"key\": \"fullText/0\",\n  \"value\": {\n    \"regexSpec\": \"[0-9a-fA-F]{64}\",\n    \"flags\": 0,\n    \"type\": \"Sha256Hash\",\n    \"dimension\": \"What\",\n    \"fieldSet\": {\n      \"fields\": [\n        \"fullText\"\n      ]\n    },\n    \"numCompressed\": 0\n  }\n}\n";
		if (!result.equals(expectedResult)) {
			System.out.println("TEST1 FAIL\n" + expectedResult + "\n...VS...\n" + result);			
		}
		else {
			System.out.println("(TEST1 passed)");
		}
		
		// TESTs 2 and 3 - define the default fields over which to search		
		RegexEntityExtractor._DEBUG = false;
		//DEBUG
		//RegexEntityExtractor._DEBUG = true;
		
		config = RegexEntityExtractor.createEngineConfig(
				"$", "fullText,description,title",
				"Where/StreetAddress", "/[0-9]+ [a-z_-]+ (?:Road|Street|Avenue)/i"
				);
		
		resultMap = moduleUnderTest.intializeConfig(config);
		
		result = RegexEntityExtractor.serializeResult(resultMap, true, false);
		expectedResult = "{\n  \"key\": \"fullText,description,title/2\",\n  \"value\": {\n    \"regexSpec\": \"[0-9]+ [a-z_-]+ (?:Road|Street|Avenue)\",\n    \"flags\": 2,\n    \"type\": \"StreetAddress\",\n    \"dimension\": \"Where\",\n    \"fieldSet\": {\n      \"fields\": [\n        \"description\",\n        \"fullText\",\n        \"title\"\n      ]\n    },\n    \"numCompressed\": 0\n  }\n}\n";
		if (!result.equals(expectedResult)) {
			System.out.println("TEST2 FAIL\n" + expectedResult + "\n...VS...\n" + result);			
		}
		else {
			System.out.println("(TEST2 passed)");
		}
		
		//(TEST3)
		RegexEntityExtractor._DEBUG = false;
		//DEBUG
		//RegexEntityExtractor._DEBUG = true;
		
		config = RegexEntityExtractor.createEngineConfig(
				"$", "/(?:fullText|description|metadata\\..*\\.address.*)/",
				"Where/StreetAddress", "/[0-9]+ *,? *[a-z_-]+ *(?:Road|Street|Avenue)/i"
				);
		
		resultMap = moduleUnderTest.intializeConfig(config);
		
		result = RegexEntityExtractor.serializeResult(resultMap, true, false);
		expectedResult = "{\n  \"key\": \"/(?:fullText|description|metadata\\\\..*\\\\.address.*)//2\",\n  \"value\": {\n    \"regexSpec\": \"[0-9]+ *,? *[a-z_-]+ *(?:Road|Street|Avenue)\",\n    \"flags\": 2,\n    \"type\": \"StreetAddress\",\n    \"dimension\": \"Where\",\n    \"fieldSet\": {\n      \"fieldRegexes\": {\n        \"0\": \"/(?:fullText|description|metadata\\\\..*\\\\.address.*)/0\"\n      }\n    },\n    \"numCompressed\": 0\n  }\n}\n";
		if (!result.equals(expectedResult)) {
			System.out.println("TEST3 FAIL\n" + expectedResult + "\n...VS...\n" + result);			
		}
		else {
			System.out.println("(TEST3 passed)");
		}
		
		// TEST 4 - Specify different regexes for different fields
		RegexEntityExtractor._DEBUG = false;
		/**/
		//DEBUG
		RegexEntityExtractor._DEBUG = true;
		
		config = RegexEntityExtractor.createEngineConfig(
					"url,sourceUrl/FileType", "s/\\.([a-z]{3})$/$1/i",
					"/[^.]*url$|metadata\\..*filename.*/i/What/FileName", "s/[^\\/]+\\.[a-z]{3}$/i"
				);
		
		resultMap = moduleUnderTest.intializeConfig(config);
		
		result = RegexEntityExtractor.serializeResult(resultMap, true, false);
		expectedResult = "";
		if (!result.equals(expectedResult)) {
			System.out.println("TEST4 FAIL\n" + expectedResult + "\n...VS...\n" + result);			
		}
		else {
			System.out.println("(TEST4 passed)");
		}
		
		//TODO: other tests ... compression big and small
		//TODO: "dimension-like-but-not-dimension-spec"
		
	}
	protected static class RegexSerializer implements JsonSerializer<Pattern> 
	{
		@Override
		public JsonElement serialize(Pattern pattern, Type typeOfT, JsonSerializationContext context)
		{
			return new JsonPrimitive("/" + pattern.pattern() + "/" + Integer.toHexString(pattern.flags()));
		}
	}
}
