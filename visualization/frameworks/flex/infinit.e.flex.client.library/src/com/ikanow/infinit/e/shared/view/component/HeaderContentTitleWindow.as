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
	import spark.components.TitleWindow;
	
	public class HeaderContentTitleWindow extends TitleWindow
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
		
		public function HeaderContentTitleWindow()
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
