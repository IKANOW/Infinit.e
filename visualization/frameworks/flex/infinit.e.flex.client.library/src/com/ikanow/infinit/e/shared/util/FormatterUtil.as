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
