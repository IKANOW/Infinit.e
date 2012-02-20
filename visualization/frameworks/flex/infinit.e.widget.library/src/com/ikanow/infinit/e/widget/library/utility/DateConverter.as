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