package com.ikanow.infinit.e.widget.model.presentation
{
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	
	import mx.collections.ArrayCollection;
	
	/**
	 *  Widgets Presentation Model
	 */
	public class WidgetsModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:WidgetsNavigator;
		
		[Bindable]
		[Inject( "widgetManager.widgets", bind="true" )]
		public var widgets:ArrayCollection;
	}
}

