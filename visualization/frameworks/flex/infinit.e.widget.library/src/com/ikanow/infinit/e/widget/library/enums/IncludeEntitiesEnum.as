/**
 * <p>Infinit.e</p>
 *
 * <p>Copyright (c) 2011 IKANOW, llc.</p>
 * <p>http://www.ikanow.com</p> 
 *
 * <p>NOTICE:  IKANOW permits you to use this this file in accordance with the terms of the license agreement 
 * accompanying it.  For information about the licensing and copyright of this Plug-In please contact IKANOW, llc. 
 * at support&#64;ikanow.com.</p>
 *
 * <p>http://www.ikanow.com/terms-conditions/</p>
 * 
 */
package com.ikanow.infinit.e.widget.library.enums
{
	/**
	 * Enum for what entities should be returned in a resulting filtered dataset.
	 */
	public final class IncludeEntitiesEnum
	{
		/** After a filter event, will include all entities of a document, not just matching entities **/
		public static const INCLUDE_ALL_ENTITIES:IncludeEntitiesEnum = create("INCLUDE_ALL_ENTITIES");
		/** After a filter event, will only include entities of a document that matched the filter **/
		public static const INCLUDE_SELECTED_ENTITIES:IncludeEntitiesEnum = create("INCLUDE_SELECTED_ENTITIES");
		
		private var includeEntities:String;
		
		private static function create(includeEntities:String):IncludeEntitiesEnum
		{
			var enum:IncludeEntitiesEnum = new IncludeEntitiesEnum();
			enum.includeEntities = includeEntities;
			return enum;
		}
	}
}