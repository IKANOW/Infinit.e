/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
