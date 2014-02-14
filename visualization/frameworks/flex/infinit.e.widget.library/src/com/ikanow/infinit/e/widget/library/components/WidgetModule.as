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
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetModuleSkin;
	import com.ikanow.infinit.e.widget.library.data.WidgetDragObject;
	import com.ikanow.infinit.e.widget.library.events.WidgetDropEvent;
	import com.ikanow.infinit.e.widget.library.utility.WidgetDragUtil;
	import com.ikanow.infinit.e.widget.library.widget.IWidgetModule;
	
	import flash.display.BitmapData;
	import flash.display.DisplayObject;
	import flash.events.DataEvent;
	import flash.events.Event;
	import flash.events.KeyboardEvent;
	import flash.events.MouseEvent;
	import flash.events.TimerEvent;
	import flash.external.ExternalInterface;
	import flash.geom.Matrix;
	import flash.geom.Point;
	import flash.ui.Keyboard;
	import flash.utils.Timer;
	import flash.utils.getQualifiedClassName;
	
	import mx.charts.chartClasses.DataTip;
	import mx.controls.Alert;
	import mx.core.IVisualElement;
	import mx.core.UIComponent;
	import mx.events.CloseEvent;
	import mx.events.DragEvent;
	import mx.events.FlexEvent;
	import mx.events.ResizeEvent;
	import mx.graphics.codec.JPEGEncoder;
	import mx.managers.DragManager;
	import mx.managers.ISystemManager;
	import mx.managers.ToolTipManager;
	import mx.utils.Base64Encoder;
	
	import spark.components.BorderContainer;
	import spark.components.Group;
	import spark.components.HGroup;
	import spark.events.ElementExistenceEvent;
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
		
		/////////////////////////////////////LEFT HEADER/////////////////////////
		private var _leftHeaderContent:Array;
		
		public function get leftHeaderContent():Array
		{
			return _leftHeaderContent;
		}
		
		[ArrayElementType( "mx.core.IVisualElement" )]
		/**
		 * The user defined content to include in the header
		 */
		public function set leftHeaderContent( value:Array ):void
		{
			_leftHeaderContent = value;
			if ( leftHeaderContentGroup )
			{
				leftHeaderContentGroup.mxmlContent = _leftHeaderContent;
				if ( value != null && value.length > 0 )
				{
					toolbarVisibleChanged = true;
					toolbarVisible = true;
					invalidateProperties();
				}
			}
		}
		
		[SkinPart( required = "false" )]
		/**
		 *  The header group
		 */
		public var leftHeaderGroup:HGroup; //TODO not sure if we need this, have to see what normal one is used for
		
		[SkinPart( required = "false" )]
		/**
		 * The group that contains the user defined header content
		 */
		public var leftHeaderContentGroup:Group;
		/////////////////////////////////////END LEFT HEADER/////////////////////////				
		
		
		
		/////////////////////////////////////CENTER HEADER RIGHT SCROLL/////////////////////////
		[SkinPart( required = "false" )]
		/**
		 *  The header group
		 */
		public var secondScroller:ScrollerInvis;		
		/////////////////////////////////////END CENTER HEADER RIGHT SCROLL/////////////////////////

		/////////////////////////////////////HELP CONTENT/////////////////////////
		private var _helpContent:IVisualElement;
		
		public function get helpContent():IVisualElement
		{
			return _helpContent;
		}
		
		/**
		 * The user defined content to include in the header
		 */
		public function set helpContent( value:IVisualElement ):void
		{
			_helpContent = value;
			if ( value != null )
			{
				helpButtonVisible = true; //show cause the help button to show when content is set
			}
			else
			{
				helpButtonVisible = false;	
			}
			//TODO figure out how to display help (use the legend in case viz as inspiration)
			/*if ( leftHeaderContentGroup )
			{
				leftHeaderContentGroup.mxmlContent = _leftHeaderContent;
				if ( value != null && value.length > 0 )
				{
					toolbarVisibleChanged = true;
					toolbarVisible = true;
					invalidateProperties();
				}
			}*/
		}
		
		[SkinPart( required = "false" )]
		/**
		 *  The header group
		 */
		public var helpContentGroupHolder:BorderContainer; //TODO not sure if we need this, have to see what normal one is used for
		
		[SkinPart( required = "false" )]
		/**
		 * The group that contains the user defined header content
		 */
		public var helpContentGroup:Group;
		/////////////////////////////////////END HELP CONTENT/////////////////////////		
		
		/////////////////////////////////////helpButton BUTTON/////////////////////////
		[SkinPart( required = "false" )]
		public var helpButton:WidgetToggleButton;
		
		/**
		 * @private
		 */
		private var helpButtonVisibleChanged:Boolean = false;
		
		/**
		 * @private
		 */
		private var _helpButtonVisible:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Controls the visibility of closeButton
		 *
		 *  @default "false"
		 */
		public function get helpButtonVisible():Boolean
		{
			return _helpButtonVisible;
		}
		
		/**
		 * @private
		 */
		public function set helpButtonVisible( value:Boolean ):void
		{
			_helpButtonVisible = value;
			
			helpButtonVisibleChanged = true;
			invalidateProperties();
		}
		/////////////////////////////////////END helpButton BUTTON/////////////////////////
		
		/////////////////////////////////////SECOND TOOLBAR BUTTON/////////////////////////
		[SkinPart( required = "false" )]
		public var secondToolbarButton:WidgetOptionsToggleButton;
		
		/**
		 * @private
		 */
		private var secondToolbarButtonVisibleChanged:Boolean = false;
		
		/**
		 * @private
		 */
		private var _secondToolbarButtonVisible:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Controls the visibility of closeButton
		 *
		 *  @default "false"
		 */
		public function get secondToolbarButtonVisible():Boolean
		{
			return _secondToolbarButtonVisible;
		}
		
		/**
		 * @private
		 */
		public function set secondToolbarButtonVisible( value:Boolean ):void
		{
			_secondToolbarButtonVisible = value;
			
			secondToolbarButtonVisibleChanged = true;
			invalidateProperties();
		}
		/////////////////////////////////////END SECOND TOOLBAR BUTTON/////////////////////////
		
		/////////////////////////////////////SECOND TOOLBAR HEADER/////////////////////////
		private var _secondHeaderContent:Array;
		private function get secondHeaderContent():Array
		{
			return _secondHeaderContent;
		}		
		
		[ArrayElementType( "mx.core.IVisualElement" )]
		/**
		 * The user defined content to include in the header
		 */
		public function set secondHeaderContent( value:Array ):void
		{
			_secondHeaderContent = value;
			for ( var i:int = 0; i < value.length; i++ )			
			{
				var elem:IVisualElement = value[i] as IVisualElement;
				if ( elem )
					elem.addEventListener(ResizeEvent.RESIZE, header_resizeHandler);
			}
			if ( secondHeaderContentGroup )
			{
				secondHeaderContentGroup.mxmlContent = _secondHeaderContent;
				if ( value != null && value.length > 0 )
				{
					toolbarVisibleChanged = true;
					toolbarVisible = true;
					invalidateProperties();
				}
			}
		}
		
		[SkinPart( required = "false" )]
		public var secondHeaderHolder:Group;
		
		[SkinPart( required = "false" )]
		/**
		 *  The header group
		 */
		public var secondHeaderGroup:HGroup; //TODO not sure if we need this, have to see what normal one is used for
		
		[SkinPart( required = "false" )]
		/**
		 * The group that contains the user defined header content
		 */		
		public var secondHeaderContentGroup:Group;
		
		private var _secondToolbarVisible:Boolean;
		private var secondToolbarVisibleChanged:Boolean = false;
		
		private function get secondToolbarVisible():Boolean
		{
			return _secondToolbarVisible;
		}
		
		private function set secondToolbarVisible( value:Boolean ):void
		{
			_secondToolbarVisible = value;
			
			secondToolbarVisibleChanged = true;
			invalidateProperties();
		}
		/////////////////////////////////////END SECOND TOOLBAR HEADER/////////////////////////
		
		/////////////////////////////////////TOOLBAR HEADER VIZ/////////////////////////
		private var toolbarVisibleChanged:Boolean = false;
		private var toolbarVisible:Boolean = false;
		/////////////////////////////////////END TOOLBAR HEADER VIZ/////////////////////////
		
		/////////////////////////////////////SECOND HEADER LEFT SCROLL/////////////////////////
		[SkinPart( required = "false" )]
		/**
		 *  The header group
		 */
		public var secondHeaderLeftScrollButton:ScrollerLeftButton;		
		/////////////////////////////////////END SECOND HEADER LEFT SCROLL/////////////////////////
		
		/////////////////////////////////////SECOND HEADER RIGHT SCROLL/////////////////////////
		[SkinPart( required = "false" )]
		/**
		 *  The header group
		 */
		public var secondHeaderRightScrollButton:ScrollerRightButton;		
		/////////////////////////////////////END SECOND HEADER RIGHT SCROLL/////////////////////////	
		
		
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
			this.addEventListener ( KeyboardEvent.KEY_DOWN, widget_keyDownHandler );
			//this.addEventListener( ResizeEvent.RESIZE, widget_resizeHandler );
		}
		
		protected function widget_resizeHandler(event:ResizeEvent):void
		{			
			calculateSecondToolbarContent();
		}
		
		protected function header_resizeHandler(event:Event):void
		{
			calculateSecondToolbarContent();				
		}
		
		protected function widget_keyDownHandler(event:KeyboardEvent):void
		{
			if (event.ctrlKey && event.shiftKey) 
			{
				if (event.keyCode == Keyboard.C)
				{
					snapshotButton_clickHandler(null);
				}				
			}			
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
		
		protected function commitHelpButtonsVisible():void
		{
			if ( helpButton )
			{
				helpButton.visible = helpButtonVisible;
				helpButton.enabled = helpButtonVisible;
				helpButton.includeInLayout = helpButtonVisible;
			}
		}
		
		protected function commitSecondToolbarButtonsVisible():void
		{
			if ( secondToolbarButton )
			{
				secondToolbarButton.visible = secondToolbarButtonVisible;
				secondToolbarButton.enabled = secondToolbarButtonVisible;
				secondToolbarButton.includeInLayout = secondToolbarButtonVisible;
			}
		}
		
		protected function commitSecondToolbarVisible():void
		{
			if ( secondHeaderHolder )
			{
				secondHeaderHolder.visible = secondToolbarVisible;
				secondHeaderHolder.enabled = secondToolbarVisible;
				secondHeaderHolder.includeInLayout = secondToolbarVisible;
			}
		}
		
		protected function commitToolbarVisible():void
		{
			leftHeaderContentGroup.visible = toolbarVisible;
			leftHeaderContentGroup.enabled = toolbarVisible;
			leftHeaderContentGroup.includeInLayout = toolbarVisible;
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
			
			if ( helpButtonVisibleChanged )
			{
				commitHelpButtonsVisible();
				helpButtonVisibleChanged = false;
			}
			
			if ( secondToolbarButtonVisibleChanged )
			{
				commitSecondToolbarButtonsVisible();
				secondToolbarButtonVisibleChanged = false;
			}
			
			if ( secondToolbarVisibleChanged )
			{
				commitSecondToolbarVisible();
				secondToolbarVisibleChanged = false;
			}	
			
			if ( toolbarVisibleChanged )
			{
				commitToolbarVisible();
				toolbarVisibleChanged = false;
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
				
		private var leftSecondTimer:Timer = new Timer( 100, 0 );
		protected function left_second_button_mouseOverHandler( event:MouseEvent ):void
		{
			leftSecondTimer.addEventListener(TimerEvent.TIMER, leftSecondTimer_handler);
			leftSecondTimer.start();	
		}
		
		protected function left_second_button_mouseOutHandler( event:MouseEvent ):void
		{
			leftSecondTimer.stop();
			leftSecondTimer.removeEventListener(TimerEvent.TIMER, leftSecondTimer_handler);
		}
		
		protected function leftSecondTimer_handler(event:TimerEvent):void
		{
			secondHeaderContentGroup.horizontalScrollPosition -= 20;
			leftSecondTimer.start();
		}
		
		protected function left_second_button_clickHandler(event:MouseEvent):void
		{
			secondHeaderContentGroup.horizontalScrollPosition -= 100;
		}
		
		private var rightSecondTimer:Timer = new Timer( 100, 0 );
		protected function right_second_button_mouseOverHandler( event:MouseEvent ):void
		{
			rightSecondTimer.addEventListener(TimerEvent.TIMER, rightSecondTimer_handler);
			rightSecondTimer.start();					
		}
		
		protected function right_second_button_mouseOutHandler( event:MouseEvent ):void
		{
			rightSecondTimer.stop();
			rightSecondTimer.removeEventListener(TimerEvent.TIMER, rightSecondTimer_handler);
		}
		
		protected function rightSecondTimer_handler(event:TimerEvent):void
		{
			secondHeaderContentGroup.horizontalScrollPosition += 20;
			rightSecondTimer.start();
		}
		
		protected function right_second_button_clickHandler(event:MouseEvent):void
		{
			secondHeaderContentGroup.horizontalScrollPosition += 100;
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
				case leftHeaderContentGroup:
					leftHeaderContentGroup.mxmlContent = _leftHeaderContent;
					if ( _leftHeaderContent && _leftHeaderContent.length > 0 )
					{
						leftHeaderContentGroup.visible = true;
						leftHeaderContentGroup.enabled = true;
						leftHeaderContentGroup.includeInLayout = true;
					}
					leftHeaderContentGroup.addEventListener( MouseEvent.MOUSE_OVER, header_mouseOverHandler );
					leftHeaderContentGroup.addEventListener( MouseEvent.MOUSE_OUT, header_mouseOutHandler );
					leftHeaderContentGroup.addEventListener( MouseEvent.MOUSE_UP, header_mouseUpHandler );
					break;	
				case helpButton:
					helpButton.addEventListener(MouseEvent.CLICK, helpButton_clickHandler);
					break;
				case helpContentGroup:
					if ( _helpContent )
					{
						helpContentGroup.mxmlContent = [_helpContent];
					}
					break;
				case secondToolbarButton:
					secondToolbarButton.addEventListener(MouseEvent.CLICK, secondToolbarButton_clickHandler);
					break;
				case secondHeaderContentGroup:
					secondHeaderContentGroup.mxmlContent = _secondHeaderContent;
					secondHeaderContentGroup.addEventListener( MouseEvent.MOUSE_OVER, header_mouseOverHandler );
					secondHeaderContentGroup.addEventListener( MouseEvent.MOUSE_OUT, header_mouseOutHandler );
					secondHeaderContentGroup.addEventListener( MouseEvent.MOUSE_UP, header_mouseUpHandler );
					secondHeaderContentGroup.addEventListener( ResizeEvent.RESIZE, header_resizeHandler );
					break;
				case secondHeaderLeftScrollButton:
					secondHeaderLeftScrollButton.addEventListener(MouseEvent.CLICK, left_second_button_clickHandler);
					secondHeaderLeftScrollButton.addEventListener(MouseEvent.MOUSE_OVER, left_second_button_mouseOverHandler);
					secondHeaderLeftScrollButton.addEventListener(MouseEvent.MOUSE_OUT, left_second_button_mouseOutHandler);
					break;
				case secondHeaderRightScrollButton:
					secondHeaderRightScrollButton.addEventListener(MouseEvent.CLICK, right_second_button_clickHandler);
					secondHeaderRightScrollButton.addEventListener(MouseEvent.MOUSE_OVER, right_second_button_mouseOverHandler);
					secondHeaderRightScrollButton.addEventListener(MouseEvent.MOUSE_OUT, right_second_button_mouseOutHandler);
					break;
				case secondHeaderHolder:
					secondHeaderHolder.addEventListener(FlexEvent.CREATION_COMPLETE, header_resizeHandler);
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
				case leftHeaderContentGroup:
					leftHeaderContentGroup.mxmlContent = null;
					leftHeaderContentGroup.visible = false;
					leftHeaderContentGroup.enabled = false;
					leftHeaderContentGroup.includeInLayout = false;
					leftHeaderContentGroup.removeEventListener( MouseEvent.MOUSE_OVER, header_mouseOverHandler );
					leftHeaderContentGroup.removeEventListener( MouseEvent.MOUSE_OUT, header_mouseOutHandler );
					leftHeaderContentGroup.removeEventListener( MouseEvent.MOUSE_UP, header_mouseUpHandler );
					break;				
				case secondToolbarButton:
					secondToolbarButton.removeEventListener(MouseEvent.CLICK, secondToolbarButton_clickHandler);
					break;
				case helpButton:
					helpButton.removeEventListener(MouseEvent.CLICK, helpButton_clickHandler);
					break;
				case helpContentGroup:
					helpContentGroup.mxmlContent = null;
					break;
				case secondHeaderContentGroup:
					secondHeaderContentGroup.mxmlContent = null;
					secondHeaderContentGroup.removeEventListener( MouseEvent.MOUSE_OVER, header_mouseOverHandler );
					secondHeaderContentGroup.removeEventListener( MouseEvent.MOUSE_OUT, header_mouseOutHandler );
					secondHeaderContentGroup.removeEventListener( MouseEvent.MOUSE_UP, header_mouseUpHandler );
					secondHeaderContentGroup.removeEventListener( ResizeEvent.RESIZE, header_resizeHandler );
					secondHeaderContentGroup.removeEventListener( ElementExistenceEvent.ELEMENT_ADD, elementAdd_handler );
					secondHeaderContentGroup.removeEventListener( ElementExistenceEvent.ELEMENT_REMOVE, elementRemove_handler);
					break;
				case secondHeaderLeftScrollButton:
					secondHeaderLeftScrollButton.addEventListener(MouseEvent.CLICK, left_second_button_clickHandler);
					secondHeaderLeftScrollButton.addEventListener(MouseEvent.MOUSE_OVER, left_second_button_mouseOverHandler);
					secondHeaderLeftScrollButton.addEventListener(MouseEvent.MOUSE_OUT, left_second_button_mouseOutHandler);
					break;
				case secondHeaderRightScrollButton:
					secondHeaderRightScrollButton.addEventListener(MouseEvent.CLICK, right_second_button_clickHandler);
					secondHeaderRightScrollButton.addEventListener(MouseEvent.MOUSE_OVER, right_second_button_mouseOverHandler);
					secondHeaderRightScrollButton.addEventListener(MouseEvent.MOUSE_OUT, right_second_button_mouseOutHandler);
					break;
			}
		}
		
		protected function elementAdd_handler(event:ElementExistenceEvent):void
		{			
			if ( !event.element.hasEventListener(ResizeEvent.RESIZE) )
			{
				event.element.addEventListener(ResizeEvent.RESIZE, header_resizeHandler);	
			}
		}
		
		protected function elementRemove_handler(event:ElementExistenceEvent):void
		{			
			event.element.removeEventListener(ResizeEvent.RESIZE, header_resizeHandler);	
		}
		
		protected function helpButton_clickHandler(event:MouseEvent):void
		{
			helpContentGroupHolder.visible = helpButton.selected;	
		}
		
		protected function secondToolbarButton_clickHandler(event:MouseEvent):void
		{
			if ( secondToolbarButton.selected )
			{
				calculateSecondToolbarContent();
				secondToolbarVisible = true;
			}
			else
			{
				secondToolbarVisible = false;
			}
		}
		
		private var isResizing:Boolean = false;
		private function calculateSecondToolbarContent():void
		{
			if ( !isResizing )
			{
				isResizing = true;
			
				if ( secondToolbarButton )
				{
					var headerWidth:Number = secondScroller.width;
					//calculate what the components want (i dont know how to check the horizontal space otherwise)
					var wantedWidth:Number = -secondHeaderLeftScrollButton.width;
					for ( var j:int = 0; j < secondHeaderContentGroup.numElements; j++ )
					{
						var elem:IVisualElement = secondHeaderContentGroup.getElementAt(j);
						if (elem.visible)
						{
							wantedWidth += elem.width;
						}
						if ( wantedWidth > headerWidth )
						{
							break; //no need to keep calculating
						}
					}
					
					if ( wantedWidth > headerWidth )
					{
						//we are larger than the header, display clicky arrows
						//TODO turn on 
						secondHeaderLeftScrollButton.visible = true;
						secondHeaderLeftScrollButton.enabled = true;
						secondHeaderLeftScrollButton.includeInLayout = true;
						secondHeaderRightScrollButton.visible = true;
						secondHeaderRightScrollButton.enabled = true;
						secondHeaderRightScrollButton.includeInLayout = true;
					}
					else
					{
						//TODO turn off scrollers
						secondHeaderLeftScrollButton.visible = false;
						secondHeaderLeftScrollButton.enabled = false;
						secondHeaderLeftScrollButton.includeInLayout = false;
						secondHeaderRightScrollButton.visible = false;
						secondHeaderRightScrollButton.enabled = false;
						secondHeaderRightScrollButton.includeInLayout = false;
					}
				}
				
				isResizing = false;
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
		
		private function findDatatips(container:ISystemManager):DisplayObject
		{
			for (var i:int = 0; i < container.rawChildren.numChildren; i++) {
				var thisChild:DisplayObject = container.rawChildren.getChildAt(i);
				
				if (thisChild is DataTip) {
					return thisChild;
				}
			}			
			return null;
		}
		
		protected function snapshotButton_clickHandler( event:MouseEvent ):void
		{
			try {
				var bmpData:BitmapData = new BitmapData(this.width, this.height);
				bmpData.draw(this);
				
				// If a tooltip is enabled, then draw it:
				if ((null == event) && (null != ToolTipManager.currentToolTip))
				{
					// Translate tooltip, adjust for screen clip and render: 
					var point:Point = new Point();
					point.x = ToolTipManager.currentToolTip.x;
					point.y = ToolTipManager.currentToolTip.y;
					point = this.globalToLocal(point);					
					if (point.x + ToolTipManager.currentToolTip.width > this.width)
					{
						point.x = this.width - ToolTipManager.currentToolTip.width;
					}					
					if (point.x < 0) 
						point.x = 0;
					if (point.y + ToolTipManager.currentToolTip.height > this.height)
					{
						point.y = this.height - ToolTipManager.currentToolTip.height;
					}
					if (point.y < 0) 
						point.y = 0;
					var translateMatrix:Matrix = new Matrix();
					translateMatrix.tx = point.x;
					translateMatrix.ty = point.y;
					bmpData.draw(ToolTipManager.currentToolTip, translateMatrix);					
				}
				if (null == event) {
					var datatip:DisplayObject = findDatatips(this.systemManager.getTopLevelRoot() as ISystemManager);
					if (null != datatip) {
						// Translate datatip, adjust for screen clip and render: 
						point = new Point();
						point.x = datatip.x;
						point.y = datatip.y;
						point = this.globalToLocal(point);					
						if (point.x + datatip.width > this.width)
						{
							point.x = this.width - datatip.width;
						}					
						if (point.x < 0) 
							point.x = 0;
						if (point.y + datatip.height > this.height)
						{
							point.y = this.height - datatip.height;
						}
						if (point.y < 0) 
							point.y = 0;
						translateMatrix = new Matrix();
						translateMatrix.tx = point.x;
						translateMatrix.ty = point.y;
						bmpData.draw(datatip, translateMatrix);											
					}
				}				
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
