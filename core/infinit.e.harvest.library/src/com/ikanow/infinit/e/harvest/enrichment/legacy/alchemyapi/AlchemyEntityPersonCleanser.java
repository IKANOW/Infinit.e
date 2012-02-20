package com.ikanow.infinit.e.harvest.enrichment.legacy.alchemyapi;

import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.google.gson.Gson;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;

//______________________________________________________________________________________

public class AlchemyEntityPersonCleanser {

	// Stats:
	private int _nDeduplications = 0;
	public int getDeduplications() { return _nDeduplications; }
	private int _nOneWordAssignments = 0;
	public int getOneWordAssignments() { return _nOneWordAssignments; }
	private int _nOneWordConversions = 0; // (a subset of the above)
	public int getOneWordConversions() { return _nOneWordConversions; }
	private int _nOneWordDeletions = 0;
	public int getOneWordDeletions() { return _nOneWordDeletions; }
	private int _nDocusModified = 0;
	public int getDocusModified() { return _nDocusModified; }
	private int _nDocusProcessed = 0;
	public int getDocusProcessed() { return _nDocusProcessed; }
	
	// Debug:
	private int _nDebugLevel = 0;
	public void setDebugLevel(int nDebugLevel) { //1==replacements, 2=feeds/candidate entities, 3=entities, 4=decomposition
		_nDebugLevel = nDebugLevel;
	}
	
	//______________________________________________________________________________________
	
	// Processing code
	//______________________________________________________________________________________

	// Top level logic
	// For running remotely
	// For cleaning local feeds, just call cleansePeopleInDocu(feed)
	// Host/Port - obvious
	// HexSlice - sub-samples somewhat efficiently, on last specified digits of _id
	// userQuery - lets the calling function decide what data to run on (probably for debugging)
	// nLimit - the max number of entries returned (for debugging)
	// bAlterDB - writes the results back to the DB (else it's just for debugging)
	
	public void doProcessing(int nSkip, BasicDBObject userQuery, int nLimit, boolean bAlterDB) 
		throws NumberFormatException, UnknownHostException, MongoException
	{
		
		// Initialization (regexes and stuff)
		this.initialize();
		
		// Launch MongoDB query
		
		BasicDBObject query = userQuery; 
		if (null == query) {
			new BasicDBObject();
		}		
		
		// Just get the entity list out to save a few CPU cycles
		BasicDBObject outFields = new BasicDBObject(); 
		outFields.append("entities", 1); 
		outFields.append("title", 1); // (help with debugging) 
		
		DBCursor dbc = null;
		if (nLimit > 0) {
			dbc = docsDB.find(query, outFields).limit(nLimit).skip(nSkip); 
		}
		else { // Everything!
			dbc = docsDB.find(query, outFields).skip(nSkip); 			
		}
		
		// Create POJO array of documents (definitely not the most efficient, but 
		// will make integration with the harvester easier)

		List<DocumentPojo> docus = DocumentPojo.listFromDb(dbc, DocumentPojo.listType()); 
				
		// Loop over array and invoke the cleansing function for each one

		for (DocumentPojo docu: docus) {
			if (this.cleansePeopleInDocu(docu)) {
				this._nDocusModified++;

				if (bAlterDB) {
					
					BasicDBObject inner0 = new BasicDBObject("entities", 
							(DBObject)com.mongodb.util.JSON.parse(new Gson().toJson(docu.getEntities())));
					BasicDBObject inner1 = new BasicDBObject("$set", inner0);
					
					// Overwrite the existing entities list with the new one 
					docsDB.update(new BasicDBObject("_id", docu.getId()), inner1);
					
				}//TESTED: checked on "Feed: Japan's Three Elections / 4c92863751cc2e59d612000b / 30"
			}
			this._nDocusProcessed++;
		}
	}

	//________________________________________________	

	// Initialization variables
	
	static final private String _namePrefixes = 
		"(?:(?:ms|miss|mrs|mr|master|rev|reverand|fr|father|dr|doctor|atty|prof|professor|hon|" +
		 "pres|president|gov|governor|coach|ofc|supt|rep|representative|sen|senator|amb|ambassador|" +
		 "pm|p\\.m|prime minister|judge|chief judge|" +
		 "pvt|private|cpl|corporal|sgt|sargent|seargant|maj|major|cpt|captain|cmdr|command|lt|lieutenant|" +
		 "lt col|lieutenant colonel|gen|general)\\.?\\s+)?";

	static final private String _weakPrefixes = "^\\s*(?:ms|miss|mrs|mr)\\.?\\s+";
	private Pattern _weakPrefixPattern = null;
	
	private Pattern _namePattern = null;
	private Pattern _nickNamePattern = null;
	
	//private Map<String, Set<String> > _nicknameListHash = new HashMap<String, Set<String> >();

	private DBCollection docsDB = null;
	
	//________________________________________________

	// Initialization code
	// Call with null/null to act on local objects vs fetching them from the DB
	
	public void initialize() throws NumberFormatException, UnknownHostException, MongoException {
	
		// MongoDB
		
		docsDB = MongoDbManager.getDocument().getMetadata(); 
		
		// Regex
		// (prefix) (first-name) [various names or nicknames] (last-name) \(disambiguation\)

		_namePattern = Pattern.compile(_namePrefixes +  
				"(?:([^\\s()]+)\\s+)?" + // first-name, ws (capture)
				"((?:(?:(?:\\\"(?:[^\"]+)\\\")|(?:(?:[^\\s()]+)))\\s+)*)?" + // nick-names OR middle-names (capture the whole thing)
				"(?:([^\\s()]+[^\\s(),.;:])[,.;:]?\\s*)" + // last-name, ws (capture) - also remove closing punct
				"(?:\\((?:[^)]+)\\)?)?\\s*" // disambiguation
				);		
		//TESTED: seen all of these clauses work
			// (note this "fails" with jr|jr.|iii etc - needs to be sorted out in the decomp fn below)

		_nickNamePattern = Pattern.compile("(?:(?:(?:\\\"([^\"]+)\\\")|(?:([^\\s()]+)))\\s+)");
			// (individual components within nick name)
		
		_weakPrefixPattern = Pattern.compile(_weakPrefixes);
		
	}

	//________________________________________________
		
	// Utility class used in processing function below
	
	static private class EntityInfo {
		EntityPojo entity = null;
		String firstName = "";
		String lastName = "";
		Set<String> middleOrNickNames = new HashSet<String>();
		
		//________________________________________________

		// Constructor - where most of the processing logic occurs, using the regexes defined above
		
		// One word case:
		EntityInfo(EntityPojo e) {
			this.entity = e;
			this.firstName = null; // (slightly hacky way of differentiating between one word and weak prefix cases...)
			this.lastName = e.getActual_name().toLowerCase();
		}
		
		// Complex case:
		EntityInfo(EntityPojo e, Matcher m, Pattern p) {
			this.entity = e;
			int nCount = m.groupCount(); 
			if (nCount > 0) {
				this.firstName = m.group(1);
				if (null == this.firstName) {
					this.firstName = "";
				}
				if (nCount > 1) {
					boolean bNeedNewLastName = false;
					this.lastName = m.group(nCount);
					if (null == this.lastName) {
						this.lastName = "";
					}
					if (this.lastName.matches("jr.?|[ivx]+.?")) {
						bNeedNewLastName = true;
						this.lastName = null;
					}
					
					LinkedList<String> lNicks = null;
					for (int i = 2; i < nCount; ++i) {	// Should only be 1: the set of middle names
						
						String middleOrNick = m.group(i);
						if (null != middleOrNick) { // Have to decompose further, sigh
							lNicks = new LinkedList<String>();
							Matcher mN = p.matcher(middleOrNick);
							while (mN.find()) {
								String sNick = mN.group(1);
								if (null != sNick) {
									lNicks.add(sNick);
								}//TESTED: see below
								sNick = mN.group(2);
								if (null != sNick) {
									lNicks.add(sNick);
								}//TESTED: see below
								if (bNeedNewLastName) {
									this.lastName = sNick;
								}//TESTED: "julian clifton lewis jr."
							}
						}//TESTED: got "g. j. siegle, g. d. stetten", "drs. ghassan k. abou-alfa"
						// Also: teodore "ted" kaczynski, rep. anh "joseph" cao/person, spc. jason dean "j.d." hunt/person
						
						if (bNeedNewLastName && (null != this.lastName)) {
							lNicks.removeLast();
						} ///TESTED: "julian clifton lewis jr.", "charles allen, iii/person"
						
					} // (end loop over (1) un-decomposed set of middle names)
					
					for (String middleOrNick: lNicks) { // Loop over decomposed nicknames
						if (middleOrNick.endsWith(".")) { // Remove abbreviations from nickname
							middleOrNick = middleOrNick.substring(0, middleOrNick.length() - 1);
							if (0 == middleOrNick.length()) {
								middleOrNick = null;
							}
						}//TESTED: with "George W. Bush" etc
						
						if (null != middleOrNick) {
							this.middleOrNickNames.add(middleOrNick);
						}
					}// (end loop over decomposed middle names)
					
					if (null == this.lastName) {
						if (!this.firstName.isEmpty()) {
							this.lastName = this.firstName;
							this.firstName = "";
						}
						else {
							this.lastName = "Junior"; // (not expecting to see this ever)
						}
					}//TESTED: john chakwin jr./person
					if (bNeedNewLastName) {
						this.lastName = this.lastName.replaceFirst("[.,;:]+$", "");
						if (0 == this.lastName.length()) {
							this.lastName = "Junior"; // (not expecting to see this ever)							
						}
					}//TESTED " martin luther king, jr./person", "charles allen, iii/person"
					
				} //(end if several names)
			} //(end if any names)
		}//TESTED: seen all these clauses work
		
		//________________________________________________
		
		// More utility
		private static List<EntityInfo> loadStringInfoMap(String s, Map<String, List<EntityInfo>> m) {
			List<EntityInfo> l = m.get(s); 
			if (null == l) {
				l = new LinkedList<EntityInfo>();
			}			
			m.put(s, l);
			return l;
		}
		public void loadEntityInfoIntoMap(Map<String, List<EntityInfo>> m) {
			if (!this.firstName.isEmpty()) {
				loadStringInfoMap(this.firstName, m).add(this);
			}
			if (!this.lastName.isEmpty()) {
				loadStringInfoMap(this.lastName, m).add(this);
			}
			for (String s: this.middleOrNickNames) {
				loadStringInfoMap(s, m).add(this);				
			}
		}
		
		// Yet more utility:
		private boolean contains(String sName) {
			// We'll say true if info not interesting:
			if (this.firstName.isEmpty() && this.middleOrNickNames.isEmpty()) {
				return true;
			}
			return this.firstName.equals(sName) || this.lastName.equals(sName) || 
					this.middleOrNickNames.contains(sName);
		}//TESTED

		// More utility:
		public static void assimilate(EntityPojo changingToEnt, EntityPojo toChangeEnt) {
			changingToEnt.setFrequency(changingToEnt.getFrequency() + toChangeEnt.getFrequency());
			double dRelToDel = toChangeEnt.getRelevance();
			double dRelToInc = changingToEnt.getRelevance();
			dRelToInc = dRelToInc + 0.5*(1.0 - dRelToInc)*dRelToDel;
				// 0.5* just to dampen the effect
			
			changingToEnt.setRelevance(dRelToInc);
			
		}//TESTED: manually
		
		// Debug/Utility
		void print(PrintStream out) {
			out.print("Decomposition: " + this.firstName);
			for (String sName: this.middleOrNickNames) {
				out.print(" / ");
				out.print(sName);
			}
			out.print(" : " + this.middleOrNickNames.size());
			out.print(" | ");
			out.print(this.lastName);
			out.println();
		}
	};
	
	//________________________________________________

	// Inner loop processing logic
	// This gets quite involved ... get your code reading boots on...
	
	public boolean cleansePeopleInDocu(DocumentPojo doc) {

		boolean bChangedAnything = false;
		
		//Debug
		if (_nDebugLevel >= 2) {
			System.out.println("+++++++ Feed: " + doc.getTitle() + " / " + doc.getId() + " / " + doc.getEntities().size());
		}
		
		List<EntityInfo> oneWordEntities = new LinkedList<EntityInfo>();
		List<EntityInfo> decentQualityEntities = new LinkedList<EntityInfo>();
		Map<String, List<EntityInfo>> possibleMatches = new HashMap<String, List<EntityInfo>>();
		Map<String, List<EntityInfo>> qualityPossibleMatches = new HashMap<String, List<EntityInfo>>();
		Map<String, List<EntityInfo>> weakPrefixMatches = new HashMap<String, List<EntityInfo>>();
		Map<String, Set<EntityPojo>> possibleWhoMatches = new HashMap<String, Set<EntityPojo>>();
		
// 1] First time through the array, extract the various components of the disambiguous and actual names
		
		if (null != doc.getEntities()) for (EntityPojo ent: doc.getEntities()) {

			// People: decompose names
			if (ent.getType().toLowerCase().equals("person")) {
				
				//Debug
				if (_nDebugLevel >= 3) {
					System.out.println("Entity1: " + ent.getIndex() + " - "+ ent.getActual_name() + " / " + ent.getDisambiguatedName());
				}
				
				if (ent.getActual_name().contains(" ")) { // else is definitely a "bad" entity
					boolean bNastyWeakPrefixCase = false;
					
					// Look for "Mr Whatever" mapped to something other than "Mr Whatever" (and other weak prefixes)
					if (!ent.getActual_name().equals(ent.getDisambiguatedName())) {
						String sActName = ent.getActual_name().toLowerCase();
						Matcher weakPrefixNameMatcher = _weakPrefixPattern.matcher(sActName);
						if (weakPrefixNameMatcher.find()) { // Starts with a weak prefix - problem candidate
							// Decompose the rest of the name to clarify
							Matcher actualNameMatcher = _namePattern.matcher(ent.getActual_name().toLowerCase());
							EntityInfo actualNameMatches = null;
							if (actualNameMatcher.matches()) {
								actualNameMatches = new EntityInfo(ent, actualNameMatcher, this._nickNamePattern);
							}		
							if (null != actualNameMatches) {
								if (actualNameMatches.firstName.isEmpty() && actualNameMatches.middleOrNickNames.isEmpty()) {
									bNastyWeakPrefixCase = true;
									// Treat like a single word:
									oneWordEntities.add(actualNameMatches);

									//Debug
									if (_nDebugLevel >= 2) {
										System.out.println("Entity1.1: " + ent.getIndex() + " - "+ ent.getActual_name() + " / " + ent.getDisambiguatedName());
										if (null != actualNameMatches) actualNameMatches.print(System.out);
									}								
								}
							}							
						}//TESTED: "Mr. Gates / Bill Gates", "MR. GIBBS: / MR. GIBBS: " (etc), "Mr. James Snyder / James Snyder, Jr."
						//(also "Mr. B / Brandon Miller (lacrosse)", fails to match)
					}
					
					// Decompose dis name
					Matcher disNameMatcher = _namePattern.matcher(ent.getDisambiguatedName().toLowerCase());
					EntityInfo disNameMatches = null; 
					if (disNameMatcher.matches()) {
						disNameMatches = new EntityInfo(ent, disNameMatcher, this._nickNamePattern);
						
						if (!bNastyWeakPrefixCase) {
							// Only save dis name for later matching - it's the most reliable
							disNameMatches.loadEntityInfoIntoMap(possibleMatches);
							
							// If the entity has a first and last name, then it's a candidate 
							// for overwriting other entries
							if (!disNameMatches.firstName.isEmpty() && !disNameMatches.lastName.isEmpty()) {
								disNameMatches.loadEntityInfoIntoMap(qualityPossibleMatches);
							}						
							decentQualityEntities.add(disNameMatches);
						} // end if not nasty weak prefix case
						else { // Save in a map to fix annoying 1-word case 
							disNameMatches.loadEntityInfoIntoMap(weakPrefixMatches);							
						}
					}
					// Some debug code:
					if (_nDebugLevel >= 4) {
						if (null != disNameMatches) disNameMatches.print(System.out);
					}
				}
				else {
					// Put this somewhere to be analyzed further
					oneWordEntities.add(new EntityInfo(ent));
				}
			}//TESTED: all these clauses			
			else if (EntityPojo.Dimension.Who == ent.getDimension()) { // Others, see below
				
				// People can get confused with companies, so we'll allow some simple matching to occur
				
				String sWhoName = ent.getDisambiguatedName().toLowerCase();
				String sDecomposedWho[] = sWhoName.split("\\s+");
				for (String sWho: sDecomposedWho) {
					sWho = sWho.replaceFirst("[.;:,]+$", "");
					if (sWho.length() >= 3) { // Min allowed length, I think
						Set<EntityPojo> le = possibleWhoMatches.get(sWho);
						if (null == le) {
							le = new HashSet<EntityPojo>();
							possibleWhoMatches.put(sWho, le);
						}
						le.add(ent);
					}
				}//TESTED: by eye, pretty simple code
				
				//DEBUG
				if (_nDebugLevel >= 3) {				
					System.out.println("Entity2: " + ent.getIndex() + " - "+ ent.getActual_name() + " / " + ent.getDisambiguatedName() + ": " + sDecomposedWho.length);
				}
			}//TESTED: see above
			
		} // (end first loop over entities)

// 2.1] Loop over all the decent entries - are these possible duplicates? 
			
		Map<EntityInfo, EntityInfo> changeDuplicateFromToMap = new HashMap<EntityInfo, EntityInfo>(); 
		for (EntityInfo info: decentQualityEntities) {
			
			if ((null != info.entity.getSemanticLinks()) && !info.entity.getSemanticLinks().isEmpty()) {
				continue; // (see below, "this" will always win out)
			}  // TESTED (naoto kan, PM of Japan)
			
						
			List<EntityInfo> l1stName = qualityPossibleMatches.get(info.firstName);
			List<EntityInfo> lSurName = qualityPossibleMatches.get(info.lastName);
			Set<EntityInfo> candidateSet = new HashSet<EntityInfo>();
			Set<String> candidateFirstNames = new HashSet<String>();
			Set<String> candidateLastNames = new HashSet<String>();
			
			// Add the current entity being investigated:
			candidateSet.add(info);
			if (!info.firstName.isEmpty()) {
				candidateFirstNames.add(info.firstName);
			}
			candidateLastNames.add(info.lastName);
			
			if (null != l1stName) {
				for (EntityInfo possDup: l1stName) {
					if (possDup == info) continue;

					if (info.lastName.equals(possDup.lastName)
							||
						(info.firstName.equals(possDup.firstName) 
								&& (info.contains(possDup.lastName)
								    || possDup.contains(info.lastName)))
							)
					{
						// A] First name is somewhere in the possDup, and last names match - pretty good...
						// B] First names match, they both contain each others last names
						candidateSet.add(possDup);
						candidateLastNames.add(possDup.lastName);
						if (!possDup.firstName.isEmpty() && info.middleOrNickNames.isEmpty()) {
							candidateFirstNames.add(possDup.firstName);						
						}
					} //TESTED: [A] "kim cocklin" vs "kim r. cocklin" - no failures seen
					// [B] "julian lewis" vs "julan clifton lewis BLAH", lots of others
				}
			} // (end matching 1st names)
			
			if (null != lSurName) {
				for (EntityInfo possDup: lSurName) {
					if (possDup == info) continue;

					if ((info.firstName.equals(possDup.firstName))
							||
						(info.firstName.isEmpty() && info.lastName.equals(possDup.lastName)))
					{
						// A] Last name is somewhere in the possDup, and first names match - pretty good...
						// B] we have no first name and our last names match
						candidateSet.add(possDup);
						candidateLastNames.add(possDup.lastName);
						if (!possDup.firstName.isEmpty() && info.middleOrNickNames.isEmpty()) {
							candidateFirstNames.add(possDup.firstName);						
						}
					} //TESTED: seen [A] ("frances k oldham" vs "frances oldham kelsey"), [B] (Dr Lang vs Daniel Lang) - also no false positives
				}
			} // (end matching last names)
			
// 2.2] Now check out candidates...
			
			// Rule will be: if there's more than one "first name" or "last name" available
			// (Taking into account that a firstname can be last (no other names)
			//  and a firstname can appear as a middle name (eg unknown prefix)), then do nothing (too much risk of getting things wrong)
			
			// Otherwise pick an entry with linkdata
			// Otherwise pick the highest relevance that has both first+last name
			if (candidateSet.size() > 1) { // (ie more than just "info")

				// Debug info
				if (_nDebugLevel >= 2) {				
					{System.out.println("*** Candidates for " + info.entity.getIndex() + ": " + candidateFirstNames.size() + ", " + candidateLastNames.size());				
					for (EntityInfo candidate: candidateSet) {
						System.out.println("...... Candidate: " + candidate.entity.getIndex());
					}}
				}
				
				boolean bTooConfusedToContinue = false;

				// In both these cases, allow multiple first/last names but only if 
				// the 1st/last name is a nickname in every non-trivial case *bar one*
				// (ie we allow one non-matching name)
				if (candidateLastNames.size() > 1) { 
					int nNonMatchingNames = 0;
					for (String sLastName: candidateLastNames) {
						for (EntityInfo possDup: candidateSet) {
							if (!possDup.contains(sLastName)) { 
								
								nNonMatchingNames++;
								if (nNonMatchingNames > 1) {
									bTooConfusedToContinue = true;
									break;
								}
							}								
						}
						if (bTooConfusedToContinue) {
							break;
						}
					} // (end loop over candidate first names)
				}//TESTED: "frances oldham kelsey" vs "frances kathleen oldham", works because
				// of only 1 non-matching name; for failure cases see identical code below
				
				//FAILURE:  julian lewis/person VS julian clifton lewis jr.
				
				if (candidateFirstNames.size() > 1) {
					int nNonMatchingNames = 0;
					for (String s1stName: candidateFirstNames) {
						for (EntityInfo possDup: candidateSet) {
							if (!possDup.contains(s1stName)) {
								nNonMatchingNames++;
								if (nNonMatchingNames > 1) {
									bTooConfusedToContinue = true;
									break;
								}
							}								
						}
						if (bTooConfusedToContinue) {
							break;
						}
					} // (end loop over candidate first names)
				} //TESTED: "dr lang" vs "daniel lang" and "david lang" 
				
				if (!bTooConfusedToContinue) {
					EntityInfo chosenDup = null;
					double highestRel = 0.0;
					boolean bLinkedEntityFound = false;
					for (EntityInfo possDup: candidateSet) {
						if (possDup == info) continue; // Don't consider myself until later
						
						if ((null != possDup.entity.getSemanticLinks()) && !possDup.entity.getSemanticLinks().isEmpty()) {
							if (!bLinkedEntityFound) highestRel = 0.0;
							bLinkedEntityFound = true;
						}
						else if (bLinkedEntityFound) {
							continue; // (not allowed to compare unlinked vs linked)
						}
						double rel = possDup.entity.getRelevance(); 
						if (rel > highestRel) {
							highestRel = rel;
							chosenDup = possDup;
						}
					}//TESTED: seen highest rel in linked and unlinked cases
					
					if (!bLinkedEntityFound && !info.firstName.isEmpty()) { 
						double rel = info.entity.getRelevance(); 
						if (rel > highestRel) {
							// Compare myself vs best unlinked
							if (_nDebugLevel >= 2) {							
								System.out.println("KEEP " + info.entity.getIndex() + " OVER " + chosenDup.entity.getIndex());
							}
							chosenDup = null;
						}						
					}//TESTED: eg "REPLACE dr. gish/person WITH dr. robert g. gish/person: 0.322918, false"/KEEP dr. robert g. gish/person OVER robert g. gish/person
					
					if (null != chosenDup) { // Need to change the entity...						
						//(make it recursive to handle a->b, b->c, ie a,b->c (only hop once: so if a->b,b->c,c->a, just do a,b->c) 
						if (_nDebugLevel >= 1) {							
							System.out.println("REPLACE " + info.entity.getIndex() + " WITH " + chosenDup.entity.getIndex() + ": " + chosenDup.entity.getRelevance() + ", " + bLinkedEntityFound);
						}
						changeDuplicateFromToMap.put(info, chosenDup);
					}
				}//TESTED: (see non-trivial clauses above)
				//else System.out.println("Too confused......");
				
			} // end there are duplication candidates
		} // end loop over entities I'm checking for duplication
		
// 2.3] Finally, actually make the duplication changes:
		
		for (Map.Entry<EntityInfo, EntityInfo> changePair: changeDuplicateFromToMap.entrySet()) {
			EntityInfo toChange = changePair.getKey();
			EntityInfo changingTo = changePair.getValue();
			
			// Handle "1-hop" recursion as discussed above
			if (null == (changingTo = changeDuplicateFromToMap.get(changingTo))) {
				changingTo = changePair.getValue(); // (change back again)				
			}

			// Make the change:
			EntityPojo toChangeEnt = toChange.entity;
			EntityPojo changingToEnt = changingTo.entity;
			
			// Preferred option, improve stats of "change to" and then delete "to change"
			EntityInfo.assimilate(changingToEnt, toChangeEnt);
			toChange.entity = changingTo.entity; // (need to support 1-word replacement below)
			doc.getEntities().remove(toChangeEnt);
			//TESTED: 1-hop and dedup-and-1-word-replace
			
			// Other option: swap the important fields over - the problem with
			// this is that you get multiple entities with the same name
			// so we'll not go with that
//			toChangeEnt.getGazateer_index() = changingToEnt.getGazateer_index();
//			toChangeEnt.getDisambiguous_name() = changingToEnt.getDisambiguous_name();
//			toChangeEnt.linkdata = changingToEnt.linkdata;
			// Leave the stats alone ... it's all a little bit confusing
			
			this._nDeduplications++;
			bChangedAnything = true;

					
		}//TESTED: "HOPPING: miss oldham/person TO frances oldham kelsey/person TO frances oldham kelsey/person"
		
// 3.1] The easiest case is one-word person entities, whether they've been 
		// mapped to an actual dis-name or not....
		// If the one-word actual name maps to the first name, surname, nickname of a 
		// "well qualified" entity (ie reasonable quality actual name)
		// (and there's only option, ie ignore "chao" vs "albert chao" and "anne chao") 
		
		for (EntityInfo entInfo: oneWordEntities) {
			EntityPojo ent = entInfo.entity;
			List<EntityInfo> l = possibleMatches.get(entInfo.lastName);
			if (null != l) {

				// Debug:
				if (_nDebugLevel >= 2) {							
					System.out.println("Candidate matches for " + ent.getActual_name() + " / " + ent.getDisambiguatedName() + ": ");
				}
				String disName = null;
				EntityInfo changeTo = null;
				boolean bMultipleDisNames = false;
					// (If there are multiple dis names, but one of them is mine, then I'm good to go
					//  else I'm going to delete the one-word entity...)
				
				for (EntityInfo info: l) {
					
					// Debug:
					if (_nDebugLevel >= 2) {							
						System.out.println("\tEntity3: " + info.entity.getIndex() + " - "+ info.entity.getActual_name() + " / " + info.entity.getDisambiguatedName()
							+ ": " + info.entity.getRelevance() + " / " + info.entity.getFrequency() + " / " + info.entity.getTotalfrequency());
					}
					
					if (ent.getDisambiguatedName().equals(info.entity.getDisambiguatedName())) {
						// Found my man - dis name match between Alchemy and a candidate
						changeTo = info;
						bMultipleDisNames = true; // (Make debug printing easier)
						break;
					} //TESTED: (Couldn't actually find an example of this, but it's simple enough!)
					
					if (null == disName) {
						disName = info.entity.getDisambiguatedName();
						changeTo = info;
					} 
					else if (!bMultipleDisNames) {
						if (!disName.equals(info.entity.getDisambiguatedName())) {
							
							
							bMultipleDisNames = true;
							changeTo = null; // Not going to be able to assign a better version
						}
					}
					
				} // end loop over 1-word candidates
				
				if (null != changeTo) {		
					
					//Debug code
					if (_nDebugLevel >= 1) {							
						System.out.println("1REPLACE/" + bMultipleDisNames + ": "+ ent.getActual_name() + " WITH " + changeTo.entity.getIndex() + " - "+ changeTo.entity.getActual_name() + " / " + changeTo.entity.getDisambiguatedName()
							+ ": " + changeTo.entity.getRelevance() + " / " + changeTo.entity.getFrequency() + " / " + changeTo.entity.getTotalfrequency());
					}
					// Preferred option, improve stats of "change to" and then delete "to change"
					EntityInfo.assimilate(changeTo.entity, ent);
					doc.getEntities().remove(ent);
					//TESTED
					
					// Other option: swap the important fields over - the problem with
					// this is that you get multiple entities with the same name
					// so we'll not go with that
//					ent.getGazateer_index() = changeTo.entity.getGazateer_index();
//					ent.getDisambiguous_name() = changeTo.entity.getDisambiguous_name();
//					ent.linkdata = changeTo.entity.linkdata;
					// Leave the stats alone ... it's all a little bit confusing
					
					this._nOneWordAssignments++;
					bChangedAnything = true;
				}
				else if (null == changeTo) { // Expensive, but hopefully don't need to do that often
					
					//Debug
					if (_nDebugLevel >= 1) {							
						System.out.println("DELETE " + ent.getActual_name() + " " + ent.getDisambiguatedName());
					}
					
					this._nOneWordDeletions++;
					doc.getEntities().remove(ent);
					bChangedAnything = true;

				}
			}//TESTED: various cases, changed and unchanged
			
			else { // No candidate matches, compare against other "who"s
				
				//Debug:
				if (_nDebugLevel >= 2) {							
					System.out.println("No person candidate matches for " + ent.getActual_name());				
				}
// 3.2] also need to compare against companies ie other "who's" (if no people matches)
				
				String gazIndex = null;
				EntityPojo changeTo = null;
				Set<EntityPojo> possWhoSet = null;
				
				if (null == entInfo.firstName) { // Won't try to replace weak prefix entities with company names, obviously
					
					String stripActualName = ent.getActual_name().replaceAll("[;;.,]+$", "");
					
					possWhoSet = possibleWhoMatches.get(stripActualName.toLowerCase());
					
					if (null != possWhoSet) {
						// Check there's only 1 candidate, else nothing to be done
						for (EntityPojo possWho: possWhoSet) {
	
							//Debug
							if (_nDebugLevel >= 2) {							
								System.out.println("Candidate company match: " + possWho.getIndex());
							}
							if (null == gazIndex) {
								gazIndex = possWho.getIndex();
								changeTo = possWho;
							}
							else if (!gazIndex.equals(possWho.getIndex())) {
								gazIndex = null;
								break;
							}
						}
					}//TESTED: "gala/person" vs "buckyball discovery gala/organization"
					//TESTED: multiple companies case: "Sinclair" vs "Sinclair Technologies Inc" and "Sinclair Holdings Inc"
				} //(end if a one-word vs weak prefix)
				
				if (null != gazIndex) { // Convert
					
					//Debug code
					if (_nDebugLevel >= 1) {							
						System.out.println("COREPLACE " + ent.getActual_name() + " WITH "+ changeTo.getActual_name() + " / " + changeTo.getDisambiguatedName()
							+ ": " + changeTo.getRelevance() + " / " + changeTo.getFrequency() + " / " + changeTo.getTotalfrequency());					
					}
					// Preferred option, improve stats of "change to" and then delete "to change"
					EntityInfo.assimilate(changeTo, ent);
					doc.getEntities().remove(ent);
					//TESTED
					
					// Other option: swap the important fields over - the problem with
					// this is that you get multiple entities with the same name
					// so we'll not go with that
//					ent.getGazateer_index() = changeTo.getGazateer_index();
//					ent.getDisambiguous_name() = changeTo.getDisambiguous_name();
//					ent.type = changeTo.type;
//					ent.linkdata = changeTo.linkdata;
					// Leave the stats alone ... it's all a little bit confusing
					
					this._nOneWordAssignments++;
					this._nOneWordConversions++;					
					bChangedAnything = true;
				}//TESTED: cut and paste of code from above, with type added
				else { // No candidate names or companies... (or too many companies)
					// Either stick with Alchemy's suggestion, if there is one
					// Or delete (always do this if many candidate companies)
					boolean bDelete = true;
					if ((null == possWhoSet) && !ent.getDisambiguatedName().matches("[^ -]*")) {
						bDelete = false;
						
						// Extra bit of logic needed:
						List<EntityInfo> lTmp = weakPrefixMatches.get(entInfo.lastName);
							// (Do I match a name from the weak prefix set, if so then delete me after all)
						
						if (null != lTmp) {
							if (lTmp.size() > 1) {
								bDelete = true;
							}
							else {
								EntityInfo tmpEntInfo = lTmp.get(0);
								if (tmpEntInfo.entity != entInfo.entity) {
									bDelete = true;	
								}
							}
						}//TESTED: "Mrs. Obama / Barack Obama"	(obv wrong but the logic is right!)		
						//TESTED Ghodrat-ol-Ein
					}
					if (bDelete) {
						// Multiple possible companies OR no Alchemy suggestion
						// Expensive, but hopefully don't need to do that often
						this._nOneWordDeletions++;
						doc.getEntities().remove(ent);
						bChangedAnything = true;
						
						//Debug
						if (_nDebugLevel >= 1) {							
							System.out.println("DELETE " + ent.getActual_name() + " " + ent.getDisambiguatedName());
						}
					} //TESTED: " Mrs. Clinton / Hillary Rodham Clinton" and "Hillary Edmund Hillary"
					else { // Leave Alchemy suggestion, nothing to do
						//Debug
						if (_nDebugLevel >= 2) {							
							System.out.println("DON'T DELETE " + ent.getActual_name() + " " + ent.getDisambiguatedName());
						}
					}
				}//TESTED: by eye (eg deleted "Sinclair" in above "multiple companies case", "John" no mappingl didn't delete "Hitler")

			}// end if people candidates or other who candidates
			//TESTED: see above clauses
			
		} // end loop over 1 word entries

		return bChangedAnything;
	}
	
} // end class AlchemyEntityPersonCleaner
