package util
{
	import flash.external.ExternalInterface;

	public class DateConverter
	{
		public static function parseDate(originalDate:String):Date
		{		
			//Call javascript to parse the date out
			var dateinmilli:String = ExternalInterface.call("parseDate",originalDate);
			return new Date(Number(dateinmilli));
		}
	}
}