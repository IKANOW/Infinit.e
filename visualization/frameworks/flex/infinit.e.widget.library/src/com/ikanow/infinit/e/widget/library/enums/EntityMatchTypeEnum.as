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
	 * Enums for how filters should match entities.
	 */
	public final class EntityMatchTypeEnum
	{
		/** Matching documents must contain ALL entities in a given set **/
		public static const ALL:EntityMatchTypeEnum = create("ALL");
		/** Matching documents must contain atleast ONE entity in a given set **/
		public static const ANY:EntityMatchTypeEnum = create("ANY");
		
		private var entityMatchType:String;
		
		private static function create(entityMatchType:String):EntityMatchTypeEnum
		{
			var enum:EntityMatchTypeEnum = new EntityMatchTypeEnum();
			enum.entityMatchType = entityMatchType;
			return enum;
		}
	}
}