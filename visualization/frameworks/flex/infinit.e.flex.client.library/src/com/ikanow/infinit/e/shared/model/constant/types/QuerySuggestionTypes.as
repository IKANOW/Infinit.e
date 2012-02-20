package com.ikanow.infinit.e.shared.model.constant.types
{
	import mx.resources.ResourceManager;
	import assets.EmbeddedAssets;
	
	/**
	 * Query Suggestion Type Constants
	 */
	public class QuerySuggestionTypes
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const EXACT_TEXT:String = ResourceManager.getInstance().getString( 'infinite', 'suggestionType.exactText' );
		
		public static const FREE_TEXT:String = ResourceManager.getInstance().getString( 'infinite', 'suggestionType.freeText' );
		
		public static const PERSON:String = ResourceManager.getInstance().getString( 'infinite', 'suggestionType.person' );
		
		public static const COMPANY:String = ResourceManager.getInstance().getString( 'infinite', 'suggestionType.company' );
		
		public static const THING:String = ResourceManager.getInstance().getString( 'infinite', 'suggestionType.thing' );
		
		public static const PLACE:String = ResourceManager.getInstance().getString( 'infinite', 'suggestionType.place' );
		
		public static const DATE:String = ResourceManager.getInstance().getString( 'infinite', 'suggestionType.date' );
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * function to get a suggestion type icon
		 *
		 * @param type The type of the suggestion
		 *
		 * @return The icon of the suggestion
		 */
		public static function getIcon( suggestionType:String ):Class
		{
			switch ( suggestionType )
			{
				case EXACT_TEXT:
					return EmbeddedAssets.ENTITY_TEXT_EXACT;
					break;
				case FREE_TEXT:
					return EmbeddedAssets.ENTITY_TEXT_FREE;
					break;
				case PERSON:
					return EmbeddedAssets.ENTITY_PERSON;
					break;
				case COMPANY:
					return EmbeddedAssets.ENTITY_COMPANY;
					break;
				case THING:
					return EmbeddedAssets.ENTITY_GENERIC;
					break;
				case PLACE:
					return EmbeddedAssets.ENTITY_GEO_LOCATION;
					break;
				case DATE:
					return EmbeddedAssets.ENTITY_TEMPORAL;
					break;
				default:
					return EmbeddedAssets.ENTITY_GENERIC;
					break;
			}
		}
		
		/**
		 * function to get the suggestion typ sort order
		 *
		 * @param type The type of the suggestion
		 *
		 * @return The sort order of the suggestion
		 */
		public static function getSortOrder( suggestionType:String ):int
		{
			switch ( suggestionType )
			{
				case EXACT_TEXT:
					return 0;
					break;
				case FREE_TEXT:
					return 1;
					break;
				case PERSON:
					return 2;
					break;
				case COMPANY:
					return 3;
					break;
				case THING:
					return 4;
					break;
				case PLACE:
					return 5;
					break;
				case DATE:
					return 6;
					break;
				default:
					return 3;
					break;
			}
		}
	}
}

