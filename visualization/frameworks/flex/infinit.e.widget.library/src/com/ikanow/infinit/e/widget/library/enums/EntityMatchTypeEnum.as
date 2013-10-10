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
