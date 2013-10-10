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
	import com.ikanow.infinit.e.shared.event.WidgetEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.PDFConstants;
	import com.ikanow.infinit.e.shared.model.vo.User;
	import com.ikanow.infinit.e.shared.util.PDFGenerator;
	import com.ikanow.infinit.e.widget.library.components.WidgetModule;
	import com.ikanow.infinit.e.widget.library.widget.IWidget;
	import com.ikanow.infinit.e.widget.library.widget.IWidgetModule;
	import flash.display.DisplayObject;
	import flash.events.DataEvent;
	import flash.events.ErrorEvent;
	import flash.events.Event;
	import flash.events.IOErrorEvent;
	import flash.events.MouseEvent;
	import flash.events.SecurityErrorEvent;
	import flash.events.UncaughtErrorEvent;
	import flash.net.FileReference;
	import flash.net.URLLoader;
	import flash.net.URLLoaderDataFormat;
	import flash.net.URLRequest;
	import flash.net.URLRequestHeader;
	import flash.utils.ByteArray;
	import flash.utils.clearTimeout;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	import mx.events.CloseEvent;
	import mx.events.ModuleEvent;
	import mx.events.ResizeEvent;
	import mx.modules.ModuleLoader;
	
	/**
	 *  Dispatched when the user selects the close button.
	 *
	 *  @eventType mx.events.CloseEvent.CLOSE
	 */
	[Event( name = "close", type = "mx.events.CloseEvent" )]
	/**
	 *  Dispatched when the user selects the maximise button.
	 *
	 *  @eventType flase.events.Event
	 */
	[Event( name = "maximize", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the user selects the minimize button.
	 *
	 *  @eventType flase.events.Event
	 */
	[Event( name = "minimize", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the user selects the next button.
	 *
	 *  @eventType flase.events.Event
	 */
	[Event( name = "next", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the user selects the previous button.
	 *
	 *  @eventType flase.events.Event
	 */
	[Event( name = "previous", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the mouse is over the header
	 *
	 *  @eventType flase.events.Event
	 */
	[Event( name = "headerMouseOver", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the mouse leaves the header
	 *
	 *  @eventType flase.events.Event
	 */
	[Event( name = "headerMouseOut", type = "flash.events.Event" )]
	/**
	 *  Dispatched when the mouse is released when over the header
	 *
	 *  @eventType flase.events.Event
	 */
	[Event( name = "headerMouseUp", type = "flash.events.Event" )]
	public class ModuleContainer extends ModuleLoader
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
		 *  Indictes if the module may be dragged
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
			
			if ( child && child is IWidgetModule )
				IWidgetModule( child ).allowDrag = _allowDrag;
			
			invalidateDisplayList();
		}
		
		/**
		 * @private
		 */
		private var _closeButtonVisible:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Controls the visibility of the close button
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
			
			if ( child && child is IWidgetModule )
				IWidgetModule( child ).closeButtonVisible = _closeButtonVisible;
			
			invalidateDisplayList();
		}
		
		/**
		 * @private
		 */
		private var _exportButtonVisible:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Controls the visibility of the export button
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
			
			if ( child && child is IWidgetModule )
				IWidgetModule( child ).exportButtonVisible = _exportButtonVisible;
			
			invalidateDisplayList();
		}
		
		/**
		 * @private
		 */
		private var _maximized:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Indicates if the module is maximized
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
			
			if ( child && child is IWidgetModule )
				IWidgetModule( child ).maximized = _maximized;
			
			invalidateDisplayList();
		}
		
		/**
		 * @private
		 */
		private var _navigationButtonsVisible:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Controls the visibility of the navigation buttons
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
			
			if ( child && child is IWidgetModule )
				IWidgetModule( child ).navigationButtonsVisible = _navigationButtonsVisible;
			
			invalidateDisplayList();
		}
		
		/**
		 * @private
		 */
		private var _resizeButtonVisible:Boolean;
		
		[Bindable]
		[Inspectable( category = "General", defaultValue = "false", enumeration = "false,true" )]
		
		/**
		 *  Controls the visibility of the maizimize and minimize buttons
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
			
			if ( child && child is IWidgetModule )
				IWidgetModule( child ).resizeButtonVisible = _resizeButtonVisible;
			
			invalidateDisplayList();
		}
		
		private var _title:String = Constants.BLANK;
		
		
		[Bindable( event = "titleChange" )]
		[Inspectable( category = "General", defaultValue = "" )]
		
		/**
		 *  The title or caption of the module
		 *
		 *  @default ""
		 */
		public function get title():String
		{
			return _title;
		}
		
		public function set title( value:String ):void
		{
			if ( _title != value )
			{
				_title = value;
				dispatchEvent( new Event( "titleChange" ) );
				
				if ( child && child is IWidgetModule )
					IWidgetModule( child ).title = title;
			}
		}
		
		/**
		 *  The current user
		 */
		[Inject( "userManager.currentUser", bind = "true" )]
		public var currentUser:User;
		
		//======================================
		// private properties 
		//======================================
		
		private var resizing:Boolean;
		
		private var moduleUrl:String;
		
		private var loader:URLLoader;
		
		private var timeoutID:uint;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function load( moduleUrl:String ):void
		{
			if ( !this.moduleUrl )
			{
				this.moduleUrl = moduleUrl;
				
				loader = new URLLoader();
				loader.dataFormat = URLLoaderDataFormat.BINARY;
				loader.addEventListener( Event.COMPLETE, onComplete );
				loader.addEventListener( flash.events.IOErrorEvent.IO_ERROR, onIoError );
				loader.addEventListener( flash.events.IOErrorEvent.NETWORK_ERROR, onNetworkError );
				loader.addEventListener( flash.events.SecurityErrorEvent.SECURITY_ERROR, onSecurityError );
				var request:URLRequest = new URLRequest( moduleUrl );
				request.requestHeaders.push( new URLRequestHeader( 'pragma', 'no-cache' ) );
				setTimeout( loader.load, 400, request );
			}
		}
		
		public function resizeModule( newWidth:Number, newHeight:Number ):void
		{
			if ( child )
			{
				IWidget( child ).onParentResize( newHeight, newWidth );
			}
		}
		
		//======================================
		// protected methods 
		//======================================
		
		protected function closeHandler( event:CloseEvent = null ):void
		{
			if ( child is IWidgetModule )
			{
				child.removeEventListener( CloseEvent.CLOSE, closeHandler );
				child.removeEventListener( "minimize", minimizeHandler );
				child.removeEventListener( "maximize", maximizeHandler );
				child.removeEventListener( "previous", previousHandler );
				child.removeEventListener( "next", nextHandler );
				child.removeEventListener( "headerMouseOver", headerMouseOverHandler );
				child.removeEventListener( "headerMouseOut", headerMouseOutHandler );
				child.removeEventListener( "headerMouseUp", headerMouseUpHandler );
				child.removeEventListener( "export", export );
			}
			
			dispatchEvent( new CloseEvent( CloseEvent.CLOSE ) );
		}
		
		protected function export( event:DataEvent ):void
		{
			var format:String = event.data;
			
			if ( format.toLowerCase() == PDFConstants.PDF )
			{
				var printPDF:PDFGenerator = new PDFGenerator();
				printPDF.widgetToPdf( child, title );
				printPDF.saveToPdf();
			}
			else
			{
				var current_time:Date = new Date();
				var suggestedFilename:String = currentUser.displayName + current_time.getMonth() + current_time.getDay() + current_time.getFullYear();
				var bytes:ByteArray = IWidget( child ).onGenerateExportData( suggestedFilename, format );
				var fileRef:FileReference = new FileReference();
				fileRef.save( bytes, suggestedFilename + Constants.PERIOD + format );
			}
		}
		
		protected function headerMouseOutHandler( event:Event ):void
		{
			dispatchEvent( new Event( "headerMouseOut" ) );
		}
		
		protected function headerMouseOverHandler( event:Event ):void
		{
			dispatchEvent( new Event( "headerMouseOver" ) );
		}
		
		protected function headerMouseUpHandler( event:Event ):void
		{
			dispatchEvent( new Event( "headerMouseUp" ) );
		}
		
		protected function maximizeHandler( event:Event ):void
		{
			dispatchEvent( new Event( "maximize" ) );
		}
		
		protected function minimizeHandler( event:Event ):void
		{
			dispatchEvent( new Event( "minimize" ) );
		}
		
		protected function nextHandler( event:Event ):void
		{
			dispatchEvent( new Event( "next" ) );
		}
		
		protected function onComplete( event:Event = null ):void
		{
			var bytes:ByteArray = loader.data as ByteArray;
			
			addEventListener( ModuleEvent.ERROR, function( e:ModuleEvent ):void
			{
				Alert.show( e.errorText );
				dispatchEvent( new CloseEvent( CloseEvent.CLOSE ) );
			} );
			
			addEventListener( ModuleEvent.SETUP, function( e:ModuleEvent ):void
			{
				DisplayObject( e.module.factory ).loaderInfo.uncaughtErrorEvents.addEventListener( UncaughtErrorEvent.UNCAUGHT_ERROR, uncaughtErrorHandler );
			} );
			
			addEventListener( ModuleEvent.READY, function( e:ModuleEvent ):void
			{
				child.height = height;
				child.width = width;
				IWidget( child ).onParentResize( height, width );
				
				child.addEventListener( Constants.WIDGET_DONE_LOADING, widgetDoneLoadingHandler );
				
				if ( child is IWidgetModule )
				{
					child.addEventListener( CloseEvent.CLOSE, closeHandler );
					child.addEventListener( "minimize", minimizeHandler );
					child.addEventListener( "maximize", maximizeHandler );
					child.addEventListener( "previous", previousHandler );
					child.addEventListener( "next", nextHandler );
					child.addEventListener( "headerMouseOver", headerMouseOverHandler );
					child.addEventListener( "headerMouseOut", headerMouseOutHandler );
					child.addEventListener( "headerMouseUp", headerMouseUpHandler );
					child.addEventListener( "export", export );
					IWidgetModule( child ).title = title;
					IWidgetModule( child ).allowDrag = allowDrag;
					IWidgetModule( child ).closeButtonVisible = closeButtonVisible;
					IWidgetModule( child ).maximized = maximized;
					IWidgetModule( child ).resizeButtonVisible = resizeButtonVisible;
					IWidgetModule( child ).navigationButtonsVisible = navigationButtonsVisible;
					IWidgetModule( child ).exportButtonVisible = false;
				}
			} );
			
			if ( null != bytes )
			{
				loadModule( moduleUrl + "?nocache=" + bytes.length.toString(), bytes );
			}
		}
		
		protected function onIoError( event:IOErrorEvent ):void
		{
			Alert.show( event.text );
			dispatchEvent( new CloseEvent( CloseEvent.CLOSE ) );
		}
		
		protected function onNetworkError( event:SecurityErrorEvent ):void
		{
			Alert.show( event.text );
			dispatchEvent( new CloseEvent( CloseEvent.CLOSE ) );
		}
		
		protected function onSecurityError( event:SecurityErrorEvent ):void
		{
			Alert.show( event.text );
			dispatchEvent( new CloseEvent( CloseEvent.CLOSE ) );
		}
		
		protected function previousHandler( event:Event ):void
		{
			dispatchEvent( new Event( "previous" ) );
		}
		
		protected function uncaughtErrorHandler( e:UncaughtErrorEvent ):void
		{
			e.preventDefault();
			
			var s:String;
			
			if ( e.error is Error )
			{
				var error:Error = e.error as Error;
				s = "Uncaught Error in Module: " + title + "  -  " + error.errorID + ", " + error.name + ", " + error.message;
			}
			else
			{
				var errorEvent:ErrorEvent = e.error as ErrorEvent;
				s = "Uncaught Error in Module: " + title + "  -  " + errorEvent.errorID;
			}
			
			trace( s );
		}
		
		protected function widgetDoneLoadingHandler( event:Event = null ):void
		{
			dispatchEvent( new WidgetEvent( WidgetEvent.WIDGET_LOADED, IWidget( child ), this.moduleUrl ) );
			
			var exportFormats:ArrayCollection = IWidget( child ).supportedExportFormats();
			
			if ( exportFormats != null && exportFormats.length > 0 )
			{
				WidgetModule( child ).exportDropDownList.dataProvider = exportFormats;
				IWidgetModule( child ).exportButtonVisible = true;
			}
		}
	}
}
