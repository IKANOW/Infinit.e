package com.ikanow.infinit.e.shared.model.presentation.base
{
	import flash.events.EventDispatcher;
	import flash.events.IEventDispatcher;
	
	/**
	 * Presentation Model
	 * Base class for presentation models
	 */
	public class PresentationModel extends EventDispatcher
	{
		//======================================
		// public properties 
		//======================================
		
		[Dispatcher]
		public var dispatcher:IEventDispatcher;
		
		//======================================
		// constructor 
		//======================================
		
		public function PresentationModel()
		{
			super();
		}
	}
}

