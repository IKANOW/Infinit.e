package com.ikanow.infinit.e.shared.util
{
	import mx.core.UIComponent;
	
	public class StateUtil
	{
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 *
		 * Constructor
		 *
		 */
		public function StateUtil()
		{
		
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Overrides the state names for a UIComponent
		 *
		 * @return
		 *
		 */
		public static function setStates( component:UIComponent, states:Array, currentState:String = "" ):void
		{
			if ( component.states.length != states.length )
			{
				throw new Error( "Incorrect Number of State Parameters" );
				return;
			}
			
			for ( var i:int = 0; i < states.length; ++i )
			{
				component.states[ i ].name = states[ i ];
			}
			
			component.currentState = currentState;
		}
	}
}
