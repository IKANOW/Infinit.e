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
package com.ikanow.infinit.e.widget.library.utility
{
	import flash.external.ExternalInterface;

	public class DateConverter
	{
		/**
		 * Converts dates from the format:
		 * publishedDate: "Mon Jan 10 08:30:00 EST 2011"
		 * to date objects.
		 * 
		 * Note: It appears that DateField only understands
		 * month day year and not hours etc so we first convert to
		 * MM DD YYY, then turn it into date and add in extra data.
		 * 
		 * @param originalDate Date object that needs converted
		 */
		public static function parseDate(originalDate:String):Date
		{		
			//Call javascript to parse the date out
			var dateinmilli:String = ExternalInterface.call("parseDate",originalDate);
			return new Date(Number(dateinmilli));
		}
	}
}
