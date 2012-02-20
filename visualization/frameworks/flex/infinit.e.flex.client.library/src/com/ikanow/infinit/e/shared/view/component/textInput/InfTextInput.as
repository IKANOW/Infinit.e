package com.ikanow.infinit.e.shared.view.component.textInput
{
	import com.ikanow.infinit.e.shared.view.component.ValidationStatus;
	import com.ikanow.infinit.e.shared.view.component.common.InfButton;
	import flash.events.Event;
	import flash.events.MouseEvent;
	import mx.core.mx_internal;
	import mx.events.ValidationResultEvent;
	import mx.validators.IValidator;
	import spark.components.TextInput;
	import spark.components.supportClasses.SkinnableComponent;
	
	[Event( name = "submitQuery", type = "flash.events.Event" )]
	/**
	 * Component for all text inputs
	 */
	public class InfTextInput extends TextInput
	{
		
		//======================================
		// public properties 
		//======================================
		
		[SkinPart( required = "false" )]
		/** @private */
		public var validationStatus:ValidationStatus;
		
		[SkinPart( required = "false" )]
		/**
		 * An extra button to add to the field for additional functionality
		 */
		public var auxButton:InfButton;
		
		/**
		 * Flag to denote validity of the text input
		 */
		public function get valid():Boolean
		{
			if ( _validator )
			{
				var vResult:ValidationResultEvent = _validator.validate();
				
				return vResult.type == ValidationResultEvent.VALID;
			}
			else
			{
				return false;
			}
		}
		
		private var _validator:IValidator;
		
		public function get validator():IValidator
		{
			return _validator;
		}
		
		/**
		 * Validator used for validating the data within this field
		 */
		public function set validator( value:IValidator ):void
		{
			if ( value != _validator )
			{
				_validator = value;
			}
		}
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function InfTextInput()
		{
			super();
		}
		
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * @inheritDoc
		 */
		override protected function commitProperties():void
		{
			super.commitProperties();
		}
		
		/**
		 * @inheritDoc
		 */
		override protected function partAdded( partName:String, instance:Object ):void
		{
			super.partAdded( partName, instance );
			
			switch ( instance )
			{
				case textDisplay:
					textDisplay.mx_internal::passwordChar = "●";
					textDisplay.addEventListener( Event.CHANGE, textDisplay_changeHandler );
					break;
				case validationStatus:
					validationStatus.valid = valid;
					break;
				case auxButton:
					auxButton.addEventListener( MouseEvent.CLICK, auxButton_clickHandler );
					break;
			}
		}
		
		//======================================
		// private methods 
		//======================================
		
		private function auxButton_clickHandler( event:MouseEvent ):void
		{
			dispatchEvent( new Event( "submitQuery" ) );
		}
		
		private function textDisplay_changeHandler( event:Event ):void
		{
			if ( validationStatus )
				validationStatus.valid = valid;
		}
	}
}
