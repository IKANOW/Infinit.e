package com.ikanow.infinit.e.widget.model.presentation.editor
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	
	/**
	 * Widget Editor Navigator
	 */
	public class WidgetEditorNavigator extends Navigator
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const WIDGET_LIST_ID:String = NavigationConstants.WIDGET_LIST_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:WidgetEditorModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function WidgetEditorNavigator()
		{
			navigatorId = NavigationConstants.WIDGET_EDITOR_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_BODY_WIDGETS_ID;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Show List View
		 */
		public function showListView():void
		{
			navigateById( WIDGET_LIST_ID );
		}
	}
}
