package com.ikanow.infinit.e.shared.view.component.textInput
{
	import com.ikanow.infinit.e.shared.view.component.common.InfButton;
	import flash.events.Event;
	import flash.events.MouseEvent;
	import mx.events.FlexEvent;
	import spark.components.TextInput;
	import spark.components.supportClasses.SkinnableComponent;
	
	/**
	 *  Dispatched when the clear button is clicked.
	 */
	[Event( name = "clear", type = "mx.events.FlexEvent" )]
	/**
	 *  Empty state - results found in the filtered collection
	 */
	[SkinState( "resultsFound" )]
	/**
	 *  Error state - no results found in the filtered collection
	 */
	[SkinState( "noResultsFound" )]
	/**
	 * Component for filter input
	 */
	public class FilterTextInput extends TextInput
	{
		
		//======================================
		// public properties 
		//======================================
		
		[SkinPart( required = "false" )]
		/**
		 * Filter Button
		 */
		public var filterButton:InfButton;
		
		[SkinPart( required = "false" )]
		/**
		 * Clear Button
		 */
		public var clearButton:InfButton;
		
		/**
		 *  @private
		 */
		private var _resultsFound:Boolean;
		
		/**
		 *  Results found in the filtered collection
		 *  Used to determine the skin state.
		 */
		public function get resultsFound():Boolean
		{
			return _resultsFound;
		}
		
		/**
		 *  @private
		 */
		public function set resultsFound( value:Boolean ):void
		{
			if ( value == _resultsFound )
				return;
			
			_resultsFound = value;
			invalidateSkinState();
		}
		
		[Bindable]
		public var showFilterButton:Boolean = true;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function FilterTextInput()
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
		 *  @private
		 */
		override protected function getCurrentSkinState():String
		{
			var stateName:String = "";
			
			if ( text.length == 0 )
			{
				stateName = super.getCurrentSkinState();
			}
			else if ( resultsFound )
			{
				stateName = "resultsFound";
			}
			else
			{
				stateName = "noResultsFound";
			}
			
			return stateName;
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
					textDisplay.addEventListener( Event.CHANGE, textDisplay_changeHandler );
					break;
				case clearButton:
					clearButton.addEventListener( MouseEvent.CLICK, clearButton_clickHandler );
					break;
			}
		}
		
		//======================================
		// private methods 
		//======================================
		
		private function clearButton_clickHandler( event:MouseEvent ):void
		{
			text = "";
			dispatchEvent( new FlexEvent( "clear" ) );
		}
		
		private function textDisplay_changeHandler( event:Event ):void
		{
			invalidateSkinState();
		}
	}
}
