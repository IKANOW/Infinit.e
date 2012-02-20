package assets
{
	
	[Bindable]
	public class WidgetThumbnails
	{
		
		//======================================
		// public static properties 
		//======================================
		
		[Embed( source = "/assets/images/widgetThumbnails/Doc_Browser.png" )]
		public static const DOC_BROWSER:Class;
		
		[Embed( source = "/assets/images/widgetThumbnails/Event_Graph.png" )]
		public static const EVENT_GRAPH:Class;
		
		[Embed( source = "/assets/images/widgetThumbnails/Event_Timeline.png" )]
		public static const EVENT_TIMELINE:Class;
		
		[Embed( source = "/assets/images/widgetThumbnails/Graph.png" )]
		public static const GRAPH:Class;
		
		[Embed( source = "/assets/images/widgetThumbnails/Map.png" )]
		public static const MAP:Class;
		
		[Embed( source = "/assets/images/widgetThumbnails/Radar.png" )]
		public static const RADAR:Class;
		
		[Embed( source = "/assets/images/widgetThumbnails/Sentiment.png" )]
		public static const SENTIMENT:Class;
		
		[Embed( source = "/assets/images/widgetThumbnails/Stats.png" )]
		public static const STATS:Class;
		
		[Embed( source = "/assets/images/widgetThumbnails/Timeline.png" )]
		public static const TIMELINE:Class;
		
		
		//======================================
		// public static methods 
		//======================================
		
		public static function forTitle( widgetTitle:String ):Class
		{
			switch ( widgetTitle )
			{
				case "Doc Browser":
					return DOC_BROWSER;
				
				case "Event Graph":
					return EVENT_GRAPH;
				
				case "Event Timeline":
					return EVENT_TIMELINE;
				
				case "Link Analysis":
					return GRAPH;
				
				case "Maps":
					return MAP;
				
				case "Radar":
					return RADAR;
				
				case "Sentiment":
					return SENTIMENT;
				
				case "Significance":
					return STATS;
				
				case "Timeline":
					return TIMELINE;
			}
			
			return DOC_BROWSER;
		}
	}
}

