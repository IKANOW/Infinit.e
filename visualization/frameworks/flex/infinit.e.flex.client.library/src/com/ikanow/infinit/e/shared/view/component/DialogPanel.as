/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.shared.view.component
{
	import spark.components.Group;
	import spark.components.supportClasses.SkinnableComponent;
	
	[Style( name = "panelColor", type = "uint", format = "Color", inherit = "yes", theme = "spark" )]
	/**
	 * This component provides layout for the Advanced, Sources, and History popups
	 */
	public class DialogPanel extends SkinnableComponent
	{
		
		//======================================
		// public properties 
		//======================================
		
		[SkinPart( required = "false" )]
		/** @private */
		public var headerGroup:Group;
		
		[SkinPart( required = "false" )]
		/** @private */
		public var contentGroup:Group;
		
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
		
		private var _content:Array;
		
		public function get content():Array
		{
			return _content;
		}
		
		[ArrayElementType( "mx.core.IVisualElement" )]
		/**
		 * The content to include in the body of the panel
		 */
		public function set content( value:Array ):void
		{
			_content = value;
			
			if ( contentGroup )
			{
				contentGroup.mxmlContent = _content;
			}
		}
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function DialogPanel()
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
				case contentGroup:
					contentGroup.mxmlContent = _content;
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
				case contentGroup:
					contentGroup.mxmlContent = null;
					break;
			}
		}
	}
}
