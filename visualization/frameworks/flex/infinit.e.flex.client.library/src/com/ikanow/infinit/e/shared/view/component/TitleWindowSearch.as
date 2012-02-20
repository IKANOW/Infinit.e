package com.ikanow.infinit.e.shared.view.component
{
	import spark.components.Group;
	import spark.components.TitleWindow;
	
	public class TitleWindowSearch extends TitleWindow
	{
		
		//======================================
		// public properties 
		//======================================
		
		[SkinPart( required = "false" )]
		/** @private */
		public var headerGroup:Group;
		
		private var _headerContent:Array;
		
		public function get headerContent():Array
		{
			return _headerContent;
		}
		
		[ArrayElementType( "mx.core.IVisualElement" )]
		/**
		 * The content to include in the panel header
		 */
		public function set headerContent( value:Array ):void
		{
			_headerContent = value;
			
			if ( headerGroup )
			{
				headerGroup.mxmlContent = _headerContent;
			}
		}
		
		//======================================
		// constructor 
		//======================================
		
		public function TitleWindowSearch()
		{
			super();
		}
		
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * @inheritDoc
		 */
		override protected function getCurrentSkinState():String
		{
			return super.getCurrentSkinState();
		}
		
		/**
		 * @inheritDoc
		 */
		override protected function partAdded( partName:String, instance:Object ):void
		{
			super.partAdded( partName, instance );
			
			switch ( instance )
			{
				case headerGroup:
					headerGroup.mxmlContent = _headerContent;
					break;
			}
		}
		
		/**
		 * @inheritDoc
		 */
		override protected function partRemoved( partName:String, instance:Object ):void
		{
			super.partRemoved( partName, instance );
			
			switch ( instance )
			{
				case headerGroup:
					headerGroup.mxmlContent = null;
					break;
			}
		}
	}
}
