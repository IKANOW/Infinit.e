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
package com.ikanow.infinit.e.data_model;

@SuppressWarnings("serial")
public class InfiniteEnums 
{
	/*********HARVESTER FLAGS**********/
	public static final int FEEDS = 2;
	public static final int DATABASE = 4;
	public static final int FILES = 8;
	public static final int STRUCTUREDANALYSIS = 10;
	public static final int UNSTRUCTUREDANALYSIS = 12;
	
	public static int castExtractType(String type)
	{
		if ( type.toLowerCase().equals("feed"))
			return FEEDS;
		else if ( type.toLowerCase().equals("database"))
			return DATABASE;
		else if ( type.toLowerCase().equals("file"))
			return FILES;
		else if ( type.toLowerCase().equals("structuredanalysis"))
			return STRUCTUREDANALYSIS;
		else if( type.toLowerCase().equals("unstructuredanalysis"))
			return UNSTRUCTUREDANALYSIS;
		return 0;
	}
	
	/*********CLEANSER FLAGS**********/	
	public static final int QUICKCLEANSE = 2;
	public static final int FULLCLEANSE = 4;
	
	
	public enum ClusterType {
		harvest,
		cleanse,
		frequency
	}
	public enum LoaderInstallType {
		fresh,
		custom
	}
	public enum HarvestEnum {
		success,
		error,
		in_progress
	}

	public enum CommitType {
		insert,
		update
	}
	public enum DatabaseType {
		db2,
		mssqlserver,
		mysql,
		oracle,
		sybase
	}
	public enum SearchCores {
		knowledge,
		groups,
		people,
		dimensions
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
	 * 
	 * @author cburch
	 *	This enum contains all the actions that might occur in program that we want to record
	 * update as necessary.
	 */
	public enum EnumAction
	{
		ALL,
		QUERY,
		QUERYSUGGEST,
		GROUPSEARCH,
		GROUPADD,
		GROUPREMOVE,
		GROUPJOIN,
		GROUPLEAVE,
		GROUPUPDATE,
		GROUPCREATE,
		GROUPINFO,
		PERSONUPDATE,
		PERSONINFO,
		PERSONGROUPS,
		PERSONWPUPDATE,
		PERSONWPREGISTER,
		PERSONCREATE,
		ACTIVITYGROUP,
		ACTIVITYPERSON,
		ACTIVITYALL,
		FEEDINFO,
		ALIASADD,
		ALIASAPPROVE,
		ALIASDECLINE,
		ALIASPENDING,
		GAZATEERINFO,
		SOURCEADD,
		SOURCEAPPROVE,
		SOURCEDECLINE,
		SOURCEINFO,
		SOURCEGOOD,
		SOURCEBAD,
		SOURCEPENDING,
		UIMODULEINFO,
		UIMODULESEARCH,
		UISETUP,
		UISETUPUPDATE,
		UIMODULEUSERGET,
		UIMODULEUSERSAVE,
		ALIASSEARCH
		
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
