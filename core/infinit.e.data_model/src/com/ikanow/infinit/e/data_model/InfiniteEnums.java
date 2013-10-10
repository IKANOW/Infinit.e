/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.data_model;

@SuppressWarnings("serial")
public class InfiniteEnums 
{
	/*********HARVESTER FLAGS**********/
	public static final int FEEDS = 2;
	public static final int DATABASE = 4;
	public static final int FILES = 8;
	
	public static int castExtractType(String type)
	{
		if ( type.toLowerCase().equals("feed"))
			return FEEDS;
		else if ( type.toLowerCase().equals("database"))
			return DATABASE;
		else if ( type.toLowerCase().equals("file"))
			return FILES;
		return 0;
	}
	
	/*********CLEANSER FLAGS**********/	
	public static final int QUICKCLEANSE = 2;
	public static final int FULLCLEANSE = 4;
	
	
	public enum HarvestEnum {
		success,
		success_iteration, // (exceed max docs per harvest)
		error,
		in_progress // (used to "lock" sources across multiple harvesters)
	}

	public enum CommitType {
		insert,
		update
	}
	public enum EntityType
	{
		anniversary,
		automobile,
		city,
		company,
		continent,
		country,
		currency,
		date,
		drug,
		emailaddress,
		entertainmentaward,
		entertainmentawardevent,
		facility,
		faxnumber,
		fieldterminology,
		financialmarketindex,
		geographicfeature,
		healthcondition,
		holiday,
		industryterm,
		marketindex,
		medicalcondition,
		medicaltreatment,
		movie,
		musicalbum,
		musicgroup,
		naturaldisaster,
		naturalfeature,
		operatingsystem,
		organization,
		person,
		phonenumber,
		politicalevent,
		position,
		printmedia,
		product,
		programminglanguage,
		provinceorstate,
		publishedmedium,
		radioprogram,
		radiostation,
		region,
		sport,
		sportingevent,
		sportsevent,
		sportsgame,
		sportsleague,
		stateorcounty,
		technology,
		televisionshow,
		televisionstation,
		tvshow,
		tvstation,
		url
	}
	
	/**
	 * Account status for users
	 */
	public enum AccountStatus
	{
		DISABLED,
		ACTIVE,
		PENDING
	}
	
	/**
	 * ShareType Enum
	 * @author craigvitter
	 *
	 */
	public enum ShareType 
	{ 
		QUERY, 
		DATASET, 
		SOURCES, 
		URL, 
		DOC_METADATA, 
		ENTITY_FEATURE, 
		EVENT_FEATURE, 
		GEO_FEATURE, 
		COMMUNITY, 
		PERSON, 
		SHARE 
	}
	
	/**
	 * ShareSearchBy Enum
	 * @author craigvitter
	 *
	 */
	public enum ShareSearchBy
	{
		PERSON,
		COMMUNITY
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	
	// Exceptions for entity extractions (equivalent to enums)
	
	// Top level exceptions (don't call these)
	// 1] A single document within the source has failed
	public static class ExtractorDocumentLevelException extends Exception {
		public ExtractorDocumentLevelException(String message) { super(message); }
		public ExtractorDocumentLevelException() { super(); }
	}
	// 2] The source is temporarily unavailable, it may become available later, do nothing for the rest of the day
	public static class ExtractorSourceLevelException extends Exception {
		public ExtractorSourceLevelException(String message) { super(message); }
		public ExtractorSourceLevelException() { super(); }
	}
	// 3] The source is likely permanently unavailable
	public static class ExtractorSourceLevelMajorException extends Exception {
		public ExtractorSourceLevelMajorException(String message) { super(message); }
		public ExtractorSourceLevelMajorException() { super(); }
	}
	// 4] Some unknown problem occurred with the source, try again later
	public static class ExtractorSourceLevelTransientException extends Exception {
		public ExtractorSourceLevelTransientException(String message) { super(message); }
		public ExtractorSourceLevelTransientException() { super(); }
	}
	// 5] The extractor is unavailable for the remainder of the day
	public static class ExtractorDailyLimitExceededException extends Exception {
		public ExtractorDailyLimitExceededException(String message) { super(message); }
		public ExtractorDailyLimitExceededException() { super(); }
	}
}
