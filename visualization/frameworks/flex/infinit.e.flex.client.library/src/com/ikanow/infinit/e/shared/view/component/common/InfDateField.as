package com.ikanow.infinit.e.shared.view.component.common
{
	import mx.controls.DateField;
	import mx.core.mx_internal;
	
	public class InfDateField extends DateField
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function InfDateField()
		{
			super();
		}
		
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 *  @private
		 *  Create subobjects in the component.
		 */
		override protected function createChildren():void
		{
			super.createChildren();
			
			mx_internal::downArrowButton.buttonMode = true;
		}
	}
}
