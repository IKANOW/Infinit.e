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
package com.ikanow.infinit.e.shared.util
{
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	
	import mx.formatters.DateFormatter;
	
	public class FormatterUtil
	{
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 *
		 * Constructor
		 *
		 */
		public function FormatterUtil()
		{
		
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Get Date from String
		 *
		 * @return
		 *
		 */
		public static function getDateFromString( dateString:String ):Date
		{
			if ( dateString )
				return new Date( dateString );
			
			return null;
		}
		
		/**
		 * Get Date from Now String
		 *
		 * @return
		 *
		 */
		public static function getDateFromNowString( dateString:String ):Date
		{
			if ( dateString == QueryConstants.NOW )
				return new Date( FormatterUtil.getFormattedDateString( new Date() ) );
			else
				return new Date( dateString );
			
			return null;
		}
		
		/**
		 * Get Date String
		 *
		 * @return
		 *
		 */
		public static function getFormattedDateString( date:Date ):String
		{
			var formatter:DateFormatter = new DateFormatter();
			formatter.formatString = Constants.DEFAULT_DATE_TIME_FORMAT;
			
			return formatter.format( date );
		}
	}
}
