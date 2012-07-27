/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.harvest.enrichment.legacy.alchemyapi;

import java.lang.reflect.Type;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.WordUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.feature.geo.GeoFeaturePojo;

//______________________________________________________________________________________

public class AlchemyEntityGeoCleanser {

	// Stats:
	private int _nDocusModified = 0;
	private int _nDocusProcessed = 0;
	private int _nStayedWithOriginal = 0;
	private int _nMovedToRegion = 0;
	private int _nMovedToLargeCity = 0;
	private int _nMovedToForeignCity = 0;
	
	public int getDocusModified() { return _nDocusModified; }
	public int getDocusProcessed() { return _nDocusProcessed; }	
	public int getStayedWithOriginal() { return _nStayedWithOriginal; }
	public int getMovedToRegion() { return _nMovedToRegion; }
	public int getMovedToLargeCity() { return _nMovedToLargeCity;	}
	public int getMovedToForeignCity() { return _nMovedToForeignCity;	}
	
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
		outFields.append(DocumentPojo.entities_, 1); 
		outFields.append(DocumentPojo.url_, 1);  // (help with debugging)
		outFields.append(DocumentPojo.title_, 1); // (help with debugging) 
		
		DBCursor dbc = null;
		if (nLimit > 0) {
			dbc = _docsDB.find(query, outFields).limit(nLimit).skip(nSkip); 
		}
		else { // Everything!
			dbc = _docsDB.find(query, outFields).skip(nSkip); 			
		}
		
		// Create POJO array of documents (definitely not the most efficient, but 
		// will make integration with the harvester easier)

		List<DocumentPojo> docus = DocumentPojo.listFromDb(dbc, DocumentPojo.listType());		
				
		// Loop over array and invoke the cleansing function for each one

		for (DocumentPojo docu: docus) {
			if (this.cleanseGeoInDocu(docu)) {
				this._nDocusModified++;

				if (bAlterDB) {
					
					BasicDBObject inner0 = new BasicDBObject(DocumentPojo.entities_, 
							(DBObject)com.mongodb.util.JSON.parse(new Gson().toJson(docu.getEntities())));
					BasicDBObject inner1 = new BasicDBObject(MongoDbManager.set_, inner0);
					
					// Overwrite the existing entities list with the new one 
					_docsDB.update(new BasicDBObject(DocumentPojo._id_, docu.getId()), inner1);
					
				}//TESTED
			}
			this._nDocusProcessed++;
		}		
	}
	//________________________________________________	

	// Initialization variables
	
	private DBCollection _docsDB = null;
	private DBCollection _georefDB = null;
	
	private static final String _stateList = 
		"Alabama|Alaska|American Samoa|Arizona|Arkansas|California|Colorado|Connecticut|Delaware|D\\.C\\.|District of Columbia|Florida|Georgia|Guam|Hawaii|Idaho|Illinois|Indiana|Iowa|Kansas|Kentucky|Louisiana|Maine|Maryland|Massachusetts|Michigan|Minnesota|Mississippi|Missouri|Montana|Nebraska|Nevada|New Hampshire|New Jersey|New Mexico|New York|North Carolina|North Dakota|Northern Marianas Islands|Ohio|Oklahoma|Oregon|Pennsylvania|Puerto Rico|Rhode Island|South Carolina|South Dakota|Tennessee|Texas|Utah|Vermont|Virginia|Virgin Islands|Washington|West Virginia|Wisconsin|Wyoming";
	private Pattern _statesRegex = null;

	private static final String _abbrStateList = "(?:m\\.d|n\\.j|n.m|conn|mich|al\\.|d\\.c|vt|calif|wash\\.|ore\\.|ind\\.)\\.?";
	private Pattern _abbrStateRegex = null;
	
	//________________________________________________

	// Initialization code
	// Call with null/null to act on local objects vs fetching them from the DB
	
	public void initialize() throws NumberFormatException, UnknownHostException, MongoException {
	
		// MongoDB		
		_docsDB = MongoDbManager.getDocument().getMetadata();
		_georefDB = MongoDbManager.getFeature().getGeo();
		
		// Regex of US states
		_statesRegex = Pattern.compile(_stateList);
		_abbrStateRegex = Pattern.compile(_abbrStateList);
	}
	//________________________________________________

	// Inner loop processing logic
	
	public static class Candidate {
		EntityPojo entity;
		LinkedList<GeoFeaturePojo> candidates;
		String state;
		Candidate(EntityPojo ent, LinkedList<GeoFeaturePojo> cands, String st) 
			{ entity = ent; candidates = cands; state = st; }
	}
	
	public boolean cleanseGeoInDocu(DocumentPojo doc) {

		boolean bChangedAnything = false;
		
		Map<String, Candidate> dubiousLocations = new HashMap<String, Candidate>();
		
		Set<String> otherRegions = new HashSet<String>();
		Set<String> otherCountries = new HashSet<String>();
		Set<String> otherCountriesOrRegionsReferenced = new HashSet<String>();
		
		//Debug
		if (_nDebugLevel >= 2) {
			System.out.println("+++++++ Doc: " + doc.getTitle() + " / " + doc.getId() + " / " + doc.getEntities().size());
		}

// 1] First off, let's find anything location-based and also determine if it's bad or not 
		
		if (null != doc.getEntities()) for (EntityPojo ent: doc.getEntities()) {

			boolean bStrongCandidate = false;
			
			// People: decompose names
			if (EntityPojo.Dimension.Where == ent.getDimension()) {
				
				// So locations get disambiguated to one of:
				// "<city-etc>, <region-or-country>", or "<region-or-country>"
				// though can also just be left as they are.
				
				String sActualName = ent.getActual_name().toLowerCase();
				if (!ent.getDisambiguatedName().toLowerCase().equals(sActualName)) {
					// It's been disambiguated
					
					//Debug
					if (_nDebugLevel >= 3) {
						System.out.println("disambiguous candidate: " + ent.getDisambiguatedName() + " VS " + ent.getActual_name()
								+ " (" + ((null!=ent.getSemanticLinks())?ent.getSemanticLinks().size():0) + ")"
								);						
					}

					// OK next step, is it a disambiguation to a US town?
					String splitMe[] = ent.getDisambiguatedName().split(", ");
					if (2 == splitMe.length) {
						
						String stateOrCountry = splitMe[1];
						Matcher m = _statesRegex.matcher(stateOrCountry);
						if (m.find()) { // This is a US disambiguation - high risk case
							// Short cut if state is already directly mentioned?
							stateOrCountry = stateOrCountry.toLowerCase();

							if (!otherRegions.contains(stateOrCountry)) { // See list below - no need to go any further
							
								// OK next step - is it a possible ambiguity:
								ArrayList<BasicDBObject> x = new ArrayList<BasicDBObject>();
								BasicDBObject inner0_0 = new BasicDBObject(MongoDbManager.not_, Pattern.compile("US"));
								BasicDBObject inner1_0 = new BasicDBObject("country_code", inner0_0);
								x.add(inner1_0);

								BasicDBObject inner0_1 = new BasicDBObject(MongoDbManager.gte_, 400000);
								BasicDBObject inner1_1 = new BasicDBObject("population", inner0_1);
								x.add(inner1_1);

								BasicDBObject dbo = new BasicDBObject();
								dbo.append("search_field", sActualName);
								dbo.append(MongoDbManager.or_, x);
								
								DBCursor dbc = _georefDB.find(dbo);
								if (dbc.size() >= 1) { // Problems!
									
									//Create list of candidates
									
									Type listType = new TypeToken<LinkedList<GeoFeaturePojo>>() {}.getType();
									LinkedList<GeoFeaturePojo> grpl = new Gson().fromJson(dbc.toArray().toString(), listType);
									
									//Debug
									if (_nDebugLevel >= 2) {
										System.out.println("\tERROR CANDIDATE: " + ent.getDisambiguatedName() + " VS " + ent.getActual_name()
												+ " (" + dbc.count() + ")");
										
										if (_nDebugLevel >= 3) {
											for (GeoFeaturePojo grp: grpl) {
												System.out.println("\t\tCandidate:" + grp.getCity() + " / " + grp.getRegion() + " / " + grp.getCountry());							
											}
										}
									}
									
									Candidate candidate = new Candidate(ent, grpl, stateOrCountry); 
									dubiousLocations.put(ent.getIndex(), candidate);
									bStrongCandidate = true;
									
								} // if strong candidate
							}//TESTED ("reston, virginia" after "virginia/stateorcounty" mention)
							// (end if can't shortcut past all this)
							
						} // end if a US town
					} // end if in the format "A, B"
					
				} // if weak candidate
				//TESTED
				
				if (!bStrongCandidate) { // Obv can't count on a disambiguous candidate:					
					String type = ent.getType().toLowerCase();
					
					if (type.equals("stateorcounty")) {
						String disName = ent.getDisambiguatedName().toLowerCase();						
						if (_abbrStateRegex.matcher(disName).matches()) {
							otherRegions.add(getStateFromAbbr(disName));							
						}
						else {
							otherRegions.add(ent.getDisambiguatedName().toLowerCase());
						}
						otherCountriesOrRegionsReferenced.add("united states");
					}//TESTED: "mich./stateorcounty"
					else if (type.equals("country")) {
						String disName = ent.getDisambiguatedName().toLowerCase();
						
						// Translation of known badly transcribed countries:
						// (England->UK)
						if (disName.equals("england")) {
							otherCountries.add("united kingdom");							
						}//TESTED
						else {
							otherCountries.add(ent.getDisambiguatedName().toLowerCase());
						}
					}
					else if (type.equals("region")) {
						otherRegions.add(ent.getDisambiguatedName().toLowerCase());						
					}
					else if (type.equals("city")) {
						String splitMe[] = ent.getDisambiguatedName().split(",\\s*");
						if (2 == splitMe.length) {
							otherCountriesOrRegionsReferenced.add(splitMe[1].toLowerCase());
							if (this._statesRegex.matcher(splitMe[1]).find()) {
								otherCountriesOrRegionsReferenced.add("united states");								
							}//TESTED: "lexingon, kentucky/city"
						}
					}
				}//TESTED: just above clauses
				
			} // if location
			
		} // (end loop over entities)
		
		// Debug:
		if ((_nDebugLevel >= 3) && (!dubiousLocations.isEmpty())) {
			for (String s: otherRegions) {
				System.out.println("Strong region: " + s);
			}
			for (String s: otherCountries) {
				System.out.println("Strong countries: " + s);
			}
			for (String s: otherCountriesOrRegionsReferenced) {
				System.out.println("Weak regionscountries: " + s);
			}			
		}

// 2] The requirements and algorithm are discussed in 
		// http://ikanow.jira.com/wiki/display/INF/Beta...+improving+AlchemyAPI+extraction+%28geo%29
		// Canonical cases:
		// Darfur -> Darfur, MN even though Sudan and sometimes Darfur, Sudan are present
		// Shanghai -> Shanghai, WV even though China is mentioned (and not WV)
		// Manchester -> Manchester village, NY (not Manchester, UK)
		// Philadelphia -> Philadelphia (village), NY (though NY is mentioned and not PA) 
		
		// We're generating the following order
//		 10] Sitting tenant with strong direct
//		 15] Large city with strong direct		
//		 20] Region with direct
//		 30] Large city with strong indirect
//		 40] Sitting tenant with strong indirect 
//		 50] Region with indirect
//		 60] Another foreign possibility with strong direct 
//		 70] Large city with weak direct
//		 72] Large city with weak indirect
//		 75] Large city with no reference 
//		 78] Another foreign possibility with strong indirect (>100K population - ie not insignificant) 
//		 80] Sitting tenant with any weak (US) direct or indirect 
//		 90] Another foreign possibility with strong indirect 
//		100] Another foreign possibility with weak direct 
//		110] Another foreign possibility with weak indirect 
//		120] Region with no reference, if there is only 1
//		130] Sitting tenant with none of the above (ie default)
//		140] Anything else!
			
		
		for (Map.Entry<String, Candidate> pair: dubiousLocations.entrySet()) {
			EntityPojo ent = pair.getValue().entity;
			Candidate candidate = pair.getValue();
			
// 2.1] Let's analyse the "sitting tenant"
			
			int nPrio = 130;
			GeoFeaturePojo currLeader = null;
			int nCase = 0; // (just for debugging, 0=st, 1=large city, 2=region, 3=other)
			
			if (otherRegions.contains(candidate.state)) { // Strong direct ref, winner!
				nPrio = 10; // winner!
			}//TESTED: "san antonio, texas/city" vs "texas"
			else if (otherCountriesOrRegionsReferenced.contains(candidate.state)) {
				// Indirect ref
				nPrio = 40; // good, but beatable...
			}//TESTED: "philadelphia (village), new york/city" 
			else if (otherCountries.contains("united states")) { // Weak direct ref
				nPrio = 80; // better than nothing...				
			}//TESTED: "apache, oklahoma/city"
			else if (otherCountriesOrRegionsReferenced.contains("united states")) { // Weak indirect ref
				nPrio = 80; // better than nothing...				
			}//TESTED: "washington, d.c." have DC as stateorcounty, but US in countries list
			
			// Special case: we don't like "village":
			if ((80 != nPrio) && ent.getDisambiguatedName().contains("village") && !ent.getActual_name().contains("village"))
			{				
				nPrio = 80;				
			}//TESTED: "Downvoted: Philadelphia (village), New York from Philadelphia"
			
			// Debug
			if (_nDebugLevel >= 2) {
				System.out.println(pair.getKey() + " SittingTenantScore=" + nPrio);
			}
			
			// Alternatives
			if (nPrio > 10) {
				
				LinkedList<GeoFeaturePojo> geos = pair.getValue().candidates;
				for (GeoFeaturePojo geo: geos) {
					
					int nAltPrio = 140;
					int nAltCase = -1;
					String city = (null != geo.getCity()) ? geo.getCity().toLowerCase() : null;
					String region = (null != geo.getRegion()) ? geo.getRegion().toLowerCase() : null;
					String country = (null != geo.getCountry()) ? geo.getCountry().toLowerCase() : null;
					
// 2.2] CASE 1: I'm a city with pop > 1M (best score 15)
//					 15] Large city with strong direct		
//					 30] Large city with strong indirect
//					 70] Large city with weak direct
//					 72] Large city with weak indirect
//					 75] Large city with no reference 					
					
					if ((null != city) && (geo.getPopulation() >= 400000) && (nPrio > 15)) {
						nAltCase = 1;
						
						if ((null != region) && (otherRegions.contains(region))) {
							nAltPrio = 15; // strong direct
						}//TESTED: "dallas / Texas / United States = 15"
						else if ((null != region) && (otherCountriesOrRegionsReferenced.contains(region))) {
							nAltPrio = 30; // strong indirect
						}//TESTED: "sacramento / California / United State"
						else if ((null != country) && (otherCountries.contains(country))) {
							nAltPrio = 70; // weak direct 
						}//TESTED: "berlin, germany", with "germany" directly mentioned
						else if ((null != country) && (otherCountriesOrRegionsReferenced.contains(country))) {
							nAltPrio = 72; // weak indirect 
						}//TESTED: "los angeles / California / United States = 72"
						else {
							nAltPrio = 75; // just for being big!
						}//TESTED: "barcelona, spain"
					}

// 2.3] CASE 2: I'm a region (best score=20, can beat current score)
//					 20] Region with direct
//					 50] Region with indirect
//					120] Region with no reference, if there is only 1
					
					else if ((null == city) && (nPrio > 20)) {
						nAltCase = 2;
						
						if ((null != country) && (otherCountries.contains(country))) {
							nAltPrio = 20; // strong direct 
						}//TESTED: (region) "Berlin, Germany" with "Germany" mentioned
						else if ((null != country) && (otherCountriesOrRegionsReferenced.contains(country))) {
							nAltPrio = 50; // strong indirect 
						}//(haven't seen, but we'll live)
						else {
							nAltPrio = 120; // (just for being there)
						}//TESTED: "null / Portland / Jamaica = 120", also "Shanghai / China"
					}
					
// 2.4] CASE 3: I'm any foreign possibility (best score=60)
//					 60] Another foreign possibility with strong direct 
//					 78] Another foreign possibility with strong indirect (>100K population - ie not insignificant) 
//					 90] Another foreign possibility with strong indirect 
//					100] Another foreign possibility with weak direct 
//					110] Another foreign possibility with weak indirect 
					
					else if (nPrio > 60) {
						nAltCase = 3;
						
						if ((null != region) && (otherRegions.contains(region))) {
							nAltPrio = 60; // strong direct
							
							// Double check we're not falling into the trap below:
							if (!geo.getCountry_code().equals("US")) {
								Matcher m = this._statesRegex.matcher(geo.getRegion());
								if (m.matches()) { // non US state matching against (probably) US state, disregard)
									nAltPrio = 140;
								}
							}//TESTED (same clause as below)
							
						}//TESTED: lol "philadelphia / Maryland / Liberia = 60" (before above extra clause)
							
						if (nAltPrio > 60) { // (may need to re-run test)
							if ((null != country) && (otherCountries.contains(country))) {
								if (geo.getPopulation() < 100000) {
									nAltPrio = 90; // strong indirect
								} //TESTED: "washington / Villa Clara / Cuba"
								else {
									nAltPrio = 78; // strong indirect, with boost!								
								} //TESTED: "geneva, Geneve, Switzerland", pop 180K
							}
							else if ((null != region) && (otherCountriesOrRegionsReferenced.contains(region))) {
								nAltPrio = 100; // weak direct
							}//TESTED: "lincoln / Lincolnshire / United Kingdom = 100"
							else if ((null != country) && (otherCountriesOrRegionsReferenced.contains(country))) {
								nAltPrio = 110; // weak indirect
							}//(haven't seen, but we'll live)						
						}
					}
					// Debug:
					if ((_nDebugLevel >= 2) && (nAltPrio < 140)) {
						System.out.println("----Alternative: " + geo.getCity() + " / " + geo.getRegion() + " / " + geo.getCountry() + " score=" + nAltPrio);
					}
					
					// Outcome of results:
					
					if (nAltPrio < nPrio) {
						currLeader = geo;
						nPrio = nAltPrio;
						nCase = nAltCase;
					}
				} // end loop over alternativse
				
				if (null != currLeader) { // Need to change
					
					if (1 == nCase) {
						this._nMovedToLargeCity++;
						
						//(Cities are lower case in georef DB for some reason)
						 String city = WordUtils.capitalize(currLeader.getCity());
						
						if (currLeader.getCountry_code().equals("US")) { // Special case: is this just the original?
							
							String region = currLeader.getRegion();
							if (region.equals("District of Columbia")) { // Special special case
								region = "D.C.";
							}
							String sCandidate = city + ", " + region;
							
							if (!sCandidate.equals(ent.getDisambiguatedName())) {
								ent.setDisambiguatedName(sCandidate);							
								ent.setIndex(ent.getDisambiguatedName() + "/city");
								ent.setSemanticLinks(null);
								bChangedAnything = true;
							}//TESTED (lots, eg "Philadelphia (village), New York" -> "Philadelphia, PA"; Wash, Ill. -> Wash DC)
							else {
								this._nMovedToLargeCity--;
								_nStayedWithOriginal++;								
							}//TESTED ("Washington DC", "San Juan, Puerto Rico")
						}//TESTED (see above)
						else {
							ent.setDisambiguatedName(city + ", " + currLeader.getCountry());							
							ent.setIndex(ent.getDisambiguatedName() + "/city"); 
							ent.setSemanticLinks(null);
							bChangedAnything = true;
						}//TESTED: "london, california/city to London, United Kingdom"
					}
					else if (2 == nCase) {
						this._nMovedToRegion++;
						ent.setDisambiguatedName(currLeader.getRegion() + ", " + currLeader.getCountry());
						ent.setIndex(ent.getDisambiguatedName() + "/region"); 
						ent.setSemanticLinks(null);
						bChangedAnything = true;
						
					}//TESTED: "Moved madrid, new york/city to Madrid, Spain" (treats Madrid as region, like Berlin see above)
					else {
						//(Cities are lower case in georef DB for some reason)
						 String city = WordUtils.capitalize(currLeader.getCity());
						 
						this._nMovedToForeignCity++;
						ent.setDisambiguatedName(city + ", " + currLeader.getCountry());
						ent.setIndex(ent.getDisambiguatedName() + "/city"); 
						ent.setSemanticLinks(null);
						bChangedAnything = true;

					}//TESTED: "Moved geneva, new york/city to Geneva, Switzerland"
					
					if ((_nDebugLevel >= 1) && (null == ent.getSemanticLinks())) {
						System.out.println("++++ Moved " + pair.getKey() + " to " + ent.getDisambiguatedName());
					}
				}
				else {
					_nStayedWithOriginal++;
				}				
				
			} // (if sitting tenant not holder)
			
		} // (end loop over candidates)		
				
		if ((_nDebugLevel >= 1) && bChangedAnything) {
			System.out.println("\t(((Doc: " + doc.getTitle() + " / " + doc.getId() + " / " + doc.getUrl() + ")))");
		}
		
		return bChangedAnything;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	// Utility: state abbrievations:
	
	private static String getStateFromAbbr(String s) {
		
		if (s.endsWith(".")) {
			s = s.substring(0, s.length() - 1);
		}
		if (s.equals("m.d")) {
			s = "maryland";
		}
		else if (s.equals("n.m")) {
			s = "new mexico";
		}
		else if (s.equals("conn")) {
			s = "connecticut";
		}
		else if (s.equals("mich")) {
			s = "michigan";
		}
		else if (s.equals("n.j")) {
			s = "new jersey";
		}
		else if (s.equals("al")) {
			s = "alabama";
		}
		else if (s.equals("d.c")) {
			s = "district of columbia";
		}
		else if (s.equals("vt")) {
			s = "vermont";
		}
		else if (s.equals("calif")) {
			s = "california";
		}
		else if (s.equals("wash")) {
			s = "washington";
		}
		else if (s.equals("ore")) {
			s = "oregon";
		}
		else if (s.equals("ind")) {
			s = "indiana";
		}		
		return s;
	}	
}
