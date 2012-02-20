package com.ikanow.infinit.e.model.presentation
{
	import com.ikanow.infinit.e.shared.event.SessionEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	
	/**
	 *  Main Presentation Model
	 */
	public class MainModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:MainNavigator;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Mouse Move Handler
		 * @param username
		 * @param password
		 */
		public function mouseMoveHandler( mouseX:Number, mouseY:Number ):void
		{
			var sessionEvent:SessionEvent = new SessionEvent( SessionEvent.MOUSE_MOVE );
			sessionEvent.mouseX = mouseX;
			sessionEvent.mouseY = mouseY;
			dispatcher.dispatchEvent( sessionEvent );
		}
	}
}

