package com.ikanow.infinit.e.utility
{
	import com.ikanow.infinit.e.widget.library.components.WidgetModule;
	
	import mx.core.UIComponent;

	public class Utils
	{
		/**
		 * Takes a component (usually the current one you are in) and climbs up
		 * it's parents looking for a WidgetModule, will return that modules name if
		 * it finds it or null
		 * 
		 */
		public static function getModuleName(uicomponent:UIComponent):String
		{
			var module:WidgetModule = getWidgetModule(uicomponent, 0, 30);
			if ( module != null )
				return module.title;
			else
				return null;
		}
		
		/**
		 * Recursively climbs up the ui tree until it finds a widgetmodule, no parent, or hits max_level
		 **/
		private static function getWidgetModule( root:Object, curr_level:int, max_level:int ):WidgetModule
		{			
			if ( curr_level < max_level )
			{
				if ( root != null )
				{
					var module:WidgetModule = root as WidgetModule;
					if ( module != null )
					{
						return module;
					}
					else
					{
						return getWidgetModule(root.parent as UIComponent, curr_level++, max_level);
					}
				}
			}
			return null;
		}
	}
}