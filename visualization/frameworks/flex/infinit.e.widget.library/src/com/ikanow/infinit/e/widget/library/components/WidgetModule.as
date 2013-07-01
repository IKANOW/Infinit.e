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
package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetMaximizeButtonSkin;
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetModuleSkin;
	import com.ikanow.infinit.e.widget.library.data.WidgetDragObject;
	import com.ikanow.infinit.e.widget.library.events.WidgetDropEvent;
	import com.ikanow.infinit.e.widget.library.utility.WidgetDragUtil;
	import com.ikanow.infinit.e.widget.library.widget.IWidgetModule;
	
	import flash.desktop.Clipboard;
	import flash.desktop.ClipboardFormats;
	import flash.display.BitmapData;
	import flash.events.DataEvent;
	import flash.events.Event;
	import flash.events.MouseEvent;
	import flash.utils.getQualifiedClassName;
	import flash.external.ExternalInterface;
	import mx.graphics.codec.JPEGEncoder;
	
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	import mx.core.IUIComponent;
	import mx.core.UIComponent;
	import mx.events.CloseEvent;
	import mx.events.DragEvent;
	import mx.managers.DragManager;
	import mx.utils.Base64Encoder;
	
	import spark.components.Group;
	import spark.components.HGroup;
	import spark.events.IndexChangeEvent;
	import spark.modules.Module;
	
	/**
	 *  Dispatched when the user selects the close button.
	 *
	 *  @eventType mx.events.CloseEvent.CLOSE
	 */
	[Event( name = "close", type = "mx.events.CloseEvent" )]
	/**
	 *  Dispatched when the user select a custom format to export
	 *
	 *  @eventType flash.events.DataEvent
	 */
	[Event( name = "export", type = "flash.events.DataEvent" )]
	/**
	 *  Dispatched when the user selects the maximise button.
	 *
	 *  @eventType flash.events.Event
	 */
	[Event( name = "maximize", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the user selects the minimize button.
	 *
	 *  @eventType flash.events.Event
	 */
	[Event( name = "minimize", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the user selects the next button.
	 *
	 *  @eventType flash.events.Event
	 */
	[Event( name = "next", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the user selects the previous button.
	 *
	 *  @eventType flash.events.Event
	 */
	[Event( name = "previous", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the mouse is over the header
	 *
	 *  @eventType flash.events.Event
	 */
	[Event( name = "headerMouseOver", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the mouse leaves the header
	 *
	 *  @eventType flash.events.Event
	 */
	[Event( name = "headerMouseOut", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the mouse is released when over the header
	 *
	 *  @eventType flash.events.Event
	 */
	[Event( name = "headerMouseUp", type = "flash.events.Event" )]
	/**
	 * Dispatched when an appropriate item is dropped on widget
	 *
	 * @eventType com.ikanow.infinit.e.widget.library.events.WidgetDropEvent
	 */
	[Event( name = "widgetDrop", type = "com.ikanow.infinit.e.widget.library.events.WidgetDropEvent" )]
	/**
	 * Dispatched when a widget is being closed
	 *
	 * @eventType flash.events.Event
	 */
	[Event( name = "widgetClose", type = "flash.events.Event" )]
	public class WidgetModule extends Module implements IWidgetModule
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * @private
		 */
		private var _allowDrag:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Controls the visibility of closeButton
		 *
		 *  @default "false"
		 */
		public function get allowDrag():Boolean
		{
			return _allowDrag;
		}
		
		/**
		 * @private
		 */
		public function set allowDrag( value:Boolean ):void
		{
			_allowDrag = value;
			
			allowDragChanged = true;
			invalidateProperties();
		}
		
		/**
		 * @private
		 */
		private var _closeButtonVisible:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Controls the visibility of closeButton
		 *
		 *  @default "false"
		 */
		public function get closeButtonVisible():Boolean
		{
			return _closeButtonVisible;
		}
		
		/**
		 * @private
		 */
		public function set closeButtonVisible( value:Boolean ):void
		{
			_closeButtonVisible = value;
			
			closeButtonVisibleChanged = true;
			invalidateProperties();
		}
		
		/**
		 * @private
		 */
		private var _exportButtonVisible:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Controls the visibility of exportDropDownList
		 *
		 *  @default "false"
		 */
		public function get exportButtonVisible():Boolean
		{
			return _exportButtonVisible;
		}
		
		/**
		 * @private
		 */
		public function set exportButtonVisible( value:Boolean ):void
		{
			_exportButtonVisible = value;
			
			exportButtonVisibleChanged = true;
			invalidateProperties();
		}
		
		/**
		 * @private
		 */
		private var _maximized:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Controls the visibility of closeButton
		 *
		 *  @default "false"
		 */
		public function get maximized():Boolean
		{
			return _maximized;
		}
		
		/**
		 * @private
		 */
		public function set maximized( value:Boolean ):void
		{
			_maximized = value;
			
			maximizedChanged = true;
			invalidateProperties();
		}
		
		/**
		 * @private
		 */
		private var _navigationButtonsVisible:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Controls the visibility of previousButton and nextButton
		 *
		 *  @default "false"
		 */
		public function get navigationButtonsVisible():Boolean
		{
			return _navigationButtonsVisible;
		}
		
		/**
		 * @private
		 */
		public function set navigationButtonsVisible( value:Boolean ):void
		{
			_navigationButtonsVisible = value;
			
			navigationButtonsVisibleChanged = true;
			invalidateProperties();
		}
		
		/**
		 * @private
		 */
		private var _resizeButtonVisible:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Controls the visibility of maximizeButtonand minimizeButton
		 *
		 *  @default "false"
		 */
		public function get resizeButtonVisible():Boolean
		{
			return _resizeButtonVisible;
		}
		
		/**
		 * @private
		 */
		public function set resizeButtonVisible( value:Boolean ):void
		{
			_resizeButtonVisible = value;
			
			resizeButtonVisibleChanged = true;
			invalidateProperties();
		}
		
		/**
		 * @private
		 */
		private var _title:String = "";
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "" )]
		
		/**
		 *  Title or caption displayed in the header.
		 *
		 *  @default ""
		 */
		public function get title():String
		{
			return _title;
		}
		
		/**
		 * @private
		 */
		public function set title( value:String ):void
		{
			if ( _title == value )
				return;
			
			_title = value;
			
			titleChanged = true;
			invalidateProperties();
		}
		
		private var _headerContent:Array;
		
		public function get headerContent():Array
		{
			return _headerContent;
		}
		
		[ArrayElementType( "mx.core.IVisualElement" )]
		/**
		 * The user defined content to include in the header
		 */
		public function set headerContent( value:Array ):void
		{
			_headerContent = value;
			
			if ( headerContentGroup )
			{
				headerContentGroup.mxmlContent = _headerContent;
			}
		}
		
		[SkinPart( required = "false" )]
		/**
		 *  The header group
		 */
		public var headerGroup:HGroup;
		
		[SkinPart( required = "false" )]
		/**
		 *  The close button
		 */
		public var closeButton:WidgetHeaderIconButton;
		
		[SkinPart( required = "false" )]
		/**
		 *  The export drop down list
		 */
		public var exportDropDownList:WidgetExportDropDownList;
		
		[SkinPart( required = "false" )]
		/**
		 * The group that contains the user defined header content
		 */
		public var headerContentGroup:Group;
		
		[SkinPart( required = "false" )]
		/**
		 *  The maximize button
		 */
		public var maximizeButton:WidgetHeaderIconButton;
		
		[SkinPart( required = "false" )]
		/**
		 *  The minimize button
		 */
		public var minimizeButton:WidgetHeaderIconButton;
		
		[SkinPart( required = "false" )]
		/**
		 *  The snapshot button
		 */
		public var snapshotButton:WidgetHeaderIconButton;
		
		[SkinPart( required = "false" )]
		/**
		 *  The move button
		 */
		public var moveButton:WidgetHeaderIconButton;
		
		[SkinPart( required = "false" )]
		/**
		 *  The next button
		 */
		public var nextButton:WidgetHeaderIconButton;
		
		[SkinPart( required = "false" )]
		/**
		 *  The previous button
		 */
		public var previousButton:WidgetHeaderIconButton;
		
		[SkinPart( required = "false" )]
		/**
		 * The Label that will display the title.
		 */
		public var titleDisplay:WidgetHeaderLabel;
		
		//======================================
		// private properties 
		//======================================
		
		/**
		 * @private
		 */
		private var allowDragChanged:Boolean = false;
		
		/**
		 * @private
		 */
		private var closeButtonVisibleChanged:Boolean = false;
		
		/**
		 * @private
		 */
		private var exportButtonVisibleChanged:Boolean = false;
		
		/**
		 * @private
		 */
		private var maximizedChanged:Boolean = false;
		
		/**
		 * @private
		 */
		private var navigationButtonsVisibleChanged:Boolean = false;
		
		/**
		 * @private
		 */
		private var resizeButtonVisibleChanged:Boolean = false;
		
		/**
		 * @private
		 */
		private var titleChanged:Boolean = false;
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetModule()
		{
			super();
			this.addEventListener( DragEvent.DRAG_ENTER, widgetDragEnterHandler );
			this.addEventListener( DragEvent.DRAG_DROP, widgetDragDropHandler );
			this.addEventListener( DragEvent.DRAG_OVER, widgetDragOverHandler );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Set the default skin class
		 */
		override public function stylesInitialized():void
		{
			super.stylesInitialized();
			this.setStyle( "skinClass", Class( WidgetModuleSkin ) );
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 *  @private
		 *  Dispatches the "close" event when the closeButton
		 *  is clicked.
		 */
		protected function closeButton_clickHandler( event:MouseEvent ):void
		{
			//try to dispatch our close widget event first
			dispatchEvent( new Event( "widgetClose" ) );
			dispatchEvent( new CloseEvent( CloseEvent.CLOSE ) );
		}
		
		/**
		 * Indicates if drag is allowed
		 */
		protected function commitAllowDrag():void
		{
			commitResizeButtonVisible();
		}
		
		/**
		 * Shows / Hides closeButton
		 */
		protected function commitCloseButtonVisible():void
		{
			if ( closeButton )
			{
				closeButton.visible = closeButtonVisible;
				closeButton.enabled = closeButtonVisible;
				closeButton.includeInLayout = closeButtonVisible;
			}
		}
		
		/**
		 * Shows / Hides exportDropDownList
		 */
		protected function commitExportButtonVisible():void
		{
			if ( exportDropDownList )
			{
				exportDropDownList.visible = exportButtonVisible;
				exportDropDownList.enabled = exportButtonVisible;
				exportDropDownList.includeInLayout = exportButtonVisible;
			}
		}
		
		/**
		 * Indicates if the widget is maximized
		 */
		protected function commitMaximized():void
		{
			commitResizeButtonVisible();
			commitNavigationButtonsVisible();
		}
		
		/**
		 * Shows / Hides previousButton and nextButton
		 */
		protected function commitNavigationButtonsVisible():void
		{
			if ( previousButton && nextButton )
			{
				if ( maximized && navigationButtonsVisible )
				{
					previousButton.visible = true;
					previousButton.enabled = true;
					previousButton.includeInLayout = true;
					nextButton.visible = true;
					nextButton.enabled = true;
					nextButton.includeInLayout = true;
				}
				else
				{
					previousButton.visible = false;
					previousButton.enabled = false;
					previousButton.includeInLayout = false;
					nextButton.visible = false;
					nextButton.enabled = false;
					nextButton.includeInLayout = false;
				}
			}
		}
		
		/**
		 *  Commit Properties
		 */
		override protected function commitProperties():void
		{
			super.commitProperties();
			
			if ( titleChanged )
			{
				commitTitle();
				titleChanged = false;
			}
			
			if ( allowDragChanged )
			{
				commitAllowDrag();
				allowDragChanged = false;
			}
			
			if ( closeButtonVisibleChanged )
			{
				commitCloseButtonVisible();
				closeButtonVisibleChanged = false;
			}
			
			if ( exportButtonVisibleChanged )
			{
				commitExportButtonVisible();
				exportButtonVisibleChanged = false;
			}
			
			if ( maximizedChanged )
			{
				commitMaximized();
				maximizedChanged = false;
			}
			
			if ( resizeButtonVisibleChanged )
			{
				commitResizeButtonVisible();
				resizeButtonVisibleChanged = false;
			}
			
			if ( navigationButtonsVisibleChanged )
			{
				commitNavigationButtonsVisible();
				navigationButtonsVisibleChanged = false;
			}
			
			
			
			invalidateDisplayList();
		}
		
		/**
		 * Shows / Hides resize buttons
		 */
		protected function commitResizeButtonVisible():void
		{
			if ( maximizeButton )
			{
				if ( !maximized && allowDrag && resizeButtonVisible )
				{
					maximizeButton.visible = true;
					maximizeButton.enabled = true;
					maximizeButton.includeInLayout = true;
				}
				else
				{
					maximizeButton.visible = false;
					maximizeButton.enabled = false;
					maximizeButton.includeInLayout = false;
				}
			}
			
			if ( minimizeButton )
			{
				if ( maximized && resizeButtonVisible )
				{
					minimizeButton.visible = true;
					minimizeButton.enabled = true;
					minimizeButton.includeInLayout = true;
				}
				else
				{
					minimizeButton.visible = false;
					minimizeButton.enabled = false;
					minimizeButton.includeInLayout = false;
				}
			}
		}
		
		/**
		 * Set the title display text
		 */
		protected function commitTitle():void
		{
			if ( titleDisplay )
				titleDisplay.text = title;
		}
		
		/**
		 *  @private
		 *  Dispatches the "export" event when an export format is selected
		 */
		protected function exportDropDownList_changeHandler( event:IndexChangeEvent ):void
		{
			dispatchEvent( new DataEvent( "export", true, false, exportDropDownList.selectedItem as String ) );
			
			exportDropDownList.selectedIndex = -1;
		}
		
		/**
		 * @inheritDoc
		 */
		override protected function getCurrentSkinState():String
		{
			return super.getCurrentSkinState();
		}
		
		/**
		 *  @private
		 *  Called when the mouse leaves the header
		 */
		protected function header_mouseOutHandler( event:MouseEvent ):void
		{
			if ( allowDrag && !maximized )
			{
				dispatchEvent( new Event( "headerMouseOut" ) );
				
				if ( moveButton )
				{
					moveButton.visible = false;
					moveButton.enabled = false;
				}
			}
			if ( snapshotButton )
			{
				snapshotButton.visible = false;
				snapshotButton.enabled = false;
			}
		}
		
		/**
		 *  @private
		 *  Called when the mouse is over the header
		 */
		protected function header_mouseOverHandler( event:MouseEvent ):void
		{
			if ( allowDrag && !maximized )
			{
				dispatchEvent( new Event( "headerMouseOver" ) );
				
				if ( moveButton )
				{
					moveButton.visible = true;
					moveButton.enabled = true;
				}
			}
			if ( snapshotButton )
			{
				snapshotButton.visible = true;
				snapshotButton.enabled = true;
			}
			
		}
		
		/**
		 *  @private
		 *  Called when the mouse is release while over the header
		 */
		protected function header_mouseUpHandler( event:MouseEvent ):void
		{
			if ( allowDrag && !maximized )
			{
				dispatchEvent( new Event( "headerMouseUp" ) );
				
				if ( moveButton )
				{
					moveButton.visible = true;
					moveButton.enabled = true;
				}
			}
			if ( snapshotButton )
			{
				snapshotButton.visible = true;
				snapshotButton.enabled = true;
			}
		}
		
		/**
		 *  @private
		 *  Dispatches the "maximize" event when the maximizeButton
		 *  is clicked.
		 */
		protected function maximizeButton_clickHandler( event:MouseEvent ):void
		{
			dispatchEvent( new Event( "maximize" ) );
			maximized = true;
			
			if ( moveButton )
			{
				moveButton.visible = false;
				moveButton.enabled = false;
			}
		}
		
		/**
		 *  @private
		 *  Dispatches the "minimize" event when the minimizeButton
		 *  is clicked.
		 */
		protected function minimizeButton_clickHandler( event:MouseEvent ):void
		{
			dispatchEvent( new Event( "minimize" ) );
			maximized = false;
		}
		
		/**
		 *  @private
		 *  Dispatches the "next" event when the nextButton
		 *  is clicked.
		 */
		protected function nextButton_clickHandler( event:MouseEvent ):void
		{
			dispatchEvent( new Event( "next" ) );
		}
		
		/**
		 * @inheritDoc
		 */
		override protected function partAdded( partName:String, instance:Object ):void
		{
			super.partAdded( partName, instance );
			
			switch ( instance )
			{
				case closeButton:
					closeButton.addEventListener( MouseEvent.CLICK, closeButton_clickHandler );
					break;
				case maximizeButton:
					maximizeButton.addEventListener( MouseEvent.CLICK, maximizeButton_clickHandler );
					break;
				case minimizeButton:
					minimizeButton.addEventListener( MouseEvent.CLICK, minimizeButton_clickHandler );
					break;
				case previousButton:
					previousButton.addEventListener( MouseEvent.CLICK, previousButton_clickHandler );
					break;
				case nextButton:
					nextButton.addEventListener( MouseEvent.CLICK, nextButton_clickHandler );
					break;
				case snapshotButton:
					snapshotButton.addEventListener( MouseEvent.CLICK, snapshotButton_clickHandler );
					break;
				case moveButton:
					moveButton.addEventListener( MouseEvent.MOUSE_OVER, header_mouseOverHandler );
					moveButton.addEventListener( MouseEvent.MOUSE_OUT, header_mouseOutHandler );
					moveButton.addEventListener( MouseEvent.MOUSE_UP, header_mouseUpHandler );
					break;
				case exportDropDownList:
					exportDropDownList.addEventListener( IndexChangeEvent.CHANGE, exportDropDownList_changeHandler );
					exportDropDownList.addEventListener( MouseEvent.MOUSE_OVER, header_mouseOverHandler );
					exportDropDownList.addEventListener( MouseEvent.MOUSE_OUT, header_mouseOutHandler );
					exportDropDownList.addEventListener( MouseEvent.MOUSE_UP, header_mouseUpHandler );
					break;
				case headerContentGroup:
					headerContentGroup.mxmlContent = _headerContent;
					headerContentGroup.addEventListener( MouseEvent.MOUSE_OVER, header_mouseOverHandler );
					headerContentGroup.addEventListener( MouseEvent.MOUSE_OUT, header_mouseOutHandler );
					headerContentGroup.addEventListener( MouseEvent.MOUSE_UP, header_mouseUpHandler );
					break;
				case headerGroup:
					headerGroup.addEventListener( MouseEvent.MOUSE_OVER, header_mouseOverHandler );
					headerGroup.addEventListener( MouseEvent.MOUSE_OUT, header_mouseOutHandler );
					headerGroup.addEventListener( MouseEvent.MOUSE_UP, header_mouseUpHandler );
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
				case closeButton:
					closeButton.removeEventListener( MouseEvent.CLICK, closeButton_clickHandler );
					break;
				case maximizeButton:
					maximizeButton.removeEventListener( MouseEvent.CLICK, maximizeButton_clickHandler );
					break;
				case minimizeButton:
					minimizeButton.removeEventListener( MouseEvent.CLICK, minimizeButton_clickHandler );
					break;
				case previousButton:
					previousButton.removeEventListener( MouseEvent.CLICK, previousButton_clickHandler );
					break;
				case nextButton:
					nextButton.removeEventListener( MouseEvent.CLICK, nextButton_clickHandler );
					break;
				case snapshotButton:
					snapshotButton.removeEventListener( MouseEvent.CLICK, snapshotButton_clickHandler );
					break;
				case moveButton:
					moveButton.removeEventListener( MouseEvent.MOUSE_OVER, header_mouseOverHandler );
					moveButton.removeEventListener( MouseEvent.MOUSE_OUT, header_mouseOutHandler );
					moveButton.removeEventListener( MouseEvent.MOUSE_UP, header_mouseUpHandler );
					break;
				case exportDropDownList:
					exportDropDownList.removeEventListener( IndexChangeEvent.CHANGE, exportDropDownList_changeHandler );
					exportDropDownList.removeEventListener( MouseEvent.MOUSE_OVER, header_mouseOverHandler );
					exportDropDownList.removeEventListener( MouseEvent.MOUSE_OUT, header_mouseOutHandler );
					exportDropDownList.removeEventListener( MouseEvent.MOUSE_UP, header_mouseUpHandler );
					break;
				case headerContentGroup:
					headerContentGroup.mxmlContent = null;
					headerContentGroup.removeEventListener( MouseEvent.MOUSE_OVER, header_mouseOverHandler );
					headerContentGroup.removeEventListener( MouseEvent.MOUSE_OUT, header_mouseOutHandler );
					headerContentGroup.removeEventListener( MouseEvent.MOUSE_UP, header_mouseUpHandler );
					break;
				case headerGroup:
					headerGroup.removeEventListener( MouseEvent.MOUSE_OVER, header_mouseOverHandler );
					headerGroup.removeEventListener( MouseEvent.MOUSE_OUT, header_mouseOutHandler );
					headerGroup.removeEventListener( MouseEvent.MOUSE_UP, header_mouseUpHandler );
					break;
			}
		}
		
		/**
		 *  @private
		 *  Dispatches the "previous" event when the previousButton
		 *  is clicked.
		 */
		protected function previousButton_clickHandler( event:MouseEvent ):void
		{
			dispatchEvent( new Event( "previous" ) );
		}
		
		//======================================
		// private methods 
		//======================================
		
		protected function snapshotButton_clickHandler( event:MouseEvent ):void
		{
			try {
				var bmpData:BitmapData = new BitmapData(this.width, this.height);
				bmpData.draw(this);
				var jencoder:JPEGEncoder = new JPEGEncoder(100);				
				var encoder:Base64Encoder = new Base64Encoder();
				encoder.encodeBytes(jencoder.encode(bmpData));
				var widgetoffset:int = this.width/2;
				ExternalInterface.call("expandPhoto",encoder.flush(), widgetoffset.toString());
			}
			catch (e:Error) {
				mx.controls.Alert.show(e.message);				
			}
		}
		
		/**
		 * Recursively climbs up the ui tree until it finds a widgetmodule, no parent, or hits max_level
		 **/
		private function getWidgetModule( root:Object, curr_level:int, max_level:int ):WidgetModule
		{			
			if ( curr_level < max_level )
			{
				if ( root != null )
				{
					var module:WidgetModule = root as WidgetModule;
					if ( module != null )
					{
						return module;
					}
					else
					{
						return getWidgetModule(root.parent as UIComponent, curr_level++, max_level);
					}
				}
			}
			return null;
		}
		
		private function widgetDragDropHandler( event:DragEvent ):void
		{
			var dragObject:WidgetDragObject = event.dragSource.dataForFormat( WidgetDragUtil.WIDGET_DRAG_FORMAT ) as WidgetDragObject;
			var widgetName:String = "unknown";
			var widgetClass:String = "unknown";
			var module:WidgetModule = getWidgetModule( event.dragInitiator, 0, 30 );
			
			if ( module != null )
			{
				widgetName = module.title;
				widgetClass = flash.utils.getQualifiedClassName( module );
			}
			var widgetDropEvent:WidgetDropEvent = new WidgetDropEvent( "widgetDrop", dragObject.entities, dragObject.associations, dragObject.documents, dragObject.dragSource, widgetName, widgetClass );
			this.dispatchEvent( widgetDropEvent );
		}
		
		private function widgetDragEnterHandler( event:DragEvent ):void
		{
			if ( this.hasEventListener( "widgetDrop" ) )
			{
				if ( event.dragSource.hasFormat( WidgetDragUtil.WIDGET_DRAG_FORMAT ) )
				{
					DragManager.acceptDragDrop( this );
					return;
				}
			}
		}
		
		private function widgetDragOverHandler( event:DragEvent ):void
		{
			if ( this.hasEventListener( "widgetDrop" ) )
			{
				if ( event.dragSource.hasFormat( WidgetDragUtil.WIDGET_DRAG_FORMAT ) )
				{
					DragManager.acceptDragDrop( this );
					return;
				}
			}
		}
	}
}
