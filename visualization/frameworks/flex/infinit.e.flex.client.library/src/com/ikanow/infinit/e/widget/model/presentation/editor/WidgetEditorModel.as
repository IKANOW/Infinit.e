package com.ikanow.infinit.e.widget.model.presentation.editor
{
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	
	/**
	 *  Widget Editor Presentation Model
	 */
	public class WidgetEditorModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:WidgetEditorNavigator;
	}
}

