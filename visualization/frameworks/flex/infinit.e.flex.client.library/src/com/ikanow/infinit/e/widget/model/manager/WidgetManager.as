package com.ikanow.infinit.e.widget.model.manager
{
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.Setup;
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	
	/**
	 * Widget Manager
	 */
	public class WidgetManager extends InfiniteManager
	{
		
		//======================================
		// public properties 
		//======================================
		
		private var _widgets:ArrayCollection;
		
		[Bindable( event = "widgetsChange" )]
		/**
		 * Widget master collection
		 */
		public function get widgets():ArrayCollection
		{
			return _widgets;
		}
		
		/**
		 * @private
		 */
		public function set widgets( value:ArrayCollection ):void
		{
			_widgets = value;
		}
		
		[Bindable]
		/**
		 * Collection of widgets that the user has
		 * chosen to view in the workspace
		 */
		public var widgetSummaries:ArrayCollection;
		
		
		//======================================
		// public methods 
		//======================================
		
		[Inject( "setupManager.setup", bind = "true" )]
		/**
		 * Set the widget summaries from the UI setup openModules
		 */
		public function setWidgetSummariesFromSetup( value:Setup ):void
		{
			if ( value )
				widgetSummaries = value.openModules;
			else
				widgetSummaries = new ArrayCollection();
		}
		
		[Inject( "setupManager.widgets", bind = "true" )]
		/**
		 * Widgets Master Collection
		 * @param value
		 */
		public function setWidgets( value:ArrayCollection ):void
		{
			widgets = value;
			
			dispatchEvent( new Event( "widgetsChange" ) );
		}
	}
}
