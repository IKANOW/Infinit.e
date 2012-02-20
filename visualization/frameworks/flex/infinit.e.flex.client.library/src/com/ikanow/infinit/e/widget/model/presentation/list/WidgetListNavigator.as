package com.ikanow.infinit.e.widget.model.presentation.list
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.util.StateUtil;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	
	/**
	 * Widget List Navigator
	 */
	public class WidgetListNavigator extends Navigator
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const WIDGET_EDITOR_ID:String = NavigationConstants.WIDGET_EDITOR_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:WidgetListModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function WidgetListNavigator()
		{
			navigatorId = NavigationConstants.WIDGET_LIST_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_BODY_WIDGETS_ID;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Show Editor View
		 */
		public function showEditorView():void
		{
			navigateById( WIDGET_EDITOR_ID );
		}
	}
}
