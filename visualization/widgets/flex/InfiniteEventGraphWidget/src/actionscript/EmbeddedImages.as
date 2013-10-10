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
package actionscript
{
	import mx.controls.Alert;
	
	public class EmbeddedImages
	{
		// For consistency with framework, use the same iconography
		
		[Embed(source="/assets/nodeIcons/EntityTypeIcon_Entity_Company.png")] 
		[Bindable] public static var iconCompany:Class;
		
		[Embed(source="/assets/nodeIcons/EntityTypeIcon_Entity_Generic.png")] 
		[Bindable] public static var iconGeneric:Class;
		
		[Embed(source="/assets/nodeIcons/EntityTypeIcon_Entity_Person.png")] 
		[Bindable] public static var iconPerson:Class;
		
		[Embed(source="/assets/nodeIcons/EntityTypeIcon_Event.png")] 
		[Bindable] public static var iconEvent:Class;
		
		[Embed(source="/assets/nodeIcons/EntityTypeIcon_GeoLocation.png")] 
		[Bindable] public static var iconGeolocation:Class;
		
		[Embed(source="/assets/nodeIcons/EntityTypeIcon_Text_Exact.png")] 
		[Bindable] public static var iconTextExact:Class;
		
		[Embed(source="/assets/nodeIcons/EntityTypeIcon_Temporal.png")] 
		[Bindable] public static var iconTemporal:Class;
		
		public static function findIcon(iconName:String, iconDimension:String):Class
		{
			iconName = iconName.toLowerCase();
			iconDimension = iconDimension.toLowerCase();
			
			if (iconDimension == "where") {
				return iconGeolocation;
			}
			else if (iconDimension == "who") {
				if ((-1 != iconName.indexOf("company") || (-1 != iconName.indexOf("organization"))) || (-1 != iconName.indexOf("facility"))) {
					return iconCompany;
				}
				else {
					return iconPerson;
				}
			}
			else if (iconDimension == "when") {
				return iconTemporal;
			}
			else { // "what"
				if ((-1 != iconName.indexOf("company")) || (-1 != iconName.indexOf("organization")) || (-1 != iconName.indexOf("facility"))) {
					return iconCompany;
				}
				else if ((-1 != iconName.indexOf("location")) || (-1 != iconName.indexOf("city")) || (-1 != iconName.indexOf("country")))  {
					return iconGeolocation;
				}
				else if ((-1 != iconName.indexOf("person")) || (-1 != iconName.indexOf("position")) || (-1 != iconName.indexOf("twitterhandle")))  {
					return iconPerson;
				}
				else if (-1 != iconName.indexOf("keyword")) {
					return iconTextExact;
				}
				else {
					return iconGeneric;
				}				
			}
		}
	}
}
