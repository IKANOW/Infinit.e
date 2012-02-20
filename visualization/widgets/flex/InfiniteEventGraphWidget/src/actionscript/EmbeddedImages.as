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
				if ((-1 != iconName.indexOf("company") || (-1 != iconName.indexOf("organization"))) || (-1 != iconName.indexOf("facility"))) {
					return iconCompany;
				}
				else if ((-1 != iconName.indexOf("person")) || (-1 != iconName.indexOf("position")))  {
					return iconPerson;
				}
				else if ((-1 != iconName.indexOf("keyword"))) {
					return iconTextExact;
				}
				else {
					return iconGeneric;
				}				
			}
		}
	}
}