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
package com.ikanow.infinit.e.shared.view.component.dialog
{
	import com.ikanow.infinit.e.shared.event.DialogCloseEvent;
	import com.ikanow.infinit.e.shared.view.component.dialog.action.DialogUserAction;
	import flash.display.DisplayObject;
	import flash.events.Event;
	import flash.net.URLVariables;
	import mx.core.EventPriority;
	import mx.core.FlexGlobals;
	import mx.logging.ILogger;
	import mx.logging.Log;
	import mx.managers.PopUpManager;
	import mx.rpc.AsyncToken;
	
	/**
	 * Dialog Manager
	 */
	public class DialogManager
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static var defaultAsyncActionDialog:Class = ServicePopUpDialog;
		
		//======================================
		// protected static properties 
		//======================================
		
		protected static var currentDialogMessage:DialogMessage;
		
		protected static var dialog:IDialog;
		
		protected static var dialogOpen:Boolean;
		
		protected static var queue:Array = [];
		
		//======================================
		// private static properties 
		//======================================
		
		private static var logger:ILogger = Log.getLogger( "DialogManager" );
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor.
		 */
		public function DialogManager()
		{
			logger.warn( "This class is not meant to be instantiated.  All interactions should use the static methods." );
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		
		/**
		 * This method will create a popup that is tied to an async token.  When the token has either
		 * a result or fault event, the popup will close.
		 *
		 * @param message The message to display in the popup
		 * @param token The token of the async call
		 * @param dialog The class of the dialog to use if different from default
		 */
		public static function addAsyncActionDialog( message:String, token:AsyncToken, dialog:Class = null ):void
		{
			var dialogMessage:AsyncCallDialogMessage = new AsyncCallDialogMessage();
			setupDialogMessage( dialogMessage, message, dialog, defaultAsyncActionDialog );
			dialogMessage.token = token;
			
			addDialogActionToQueue( dialogMessage );
		}
		
		/**
		 * This method will centers a popup when the application is resized.
		 *
		 */
		public static function centerDialogWhenApplicationResized():void
		{
			if ( dialog != null )
			{
				PopUpManager.centerPopUp( dialog );
			}
		
		}
		
		//======================================
		// protected static methods 
		//======================================
		
		protected static function addDialogActionToQueue( value:DialogMessage ):void
		{
			queue.push( value );
			
			// If this is an AsyncCallDialogMessage, then we need to add a responder to close the dialog
			if ( value is AsyncCallDialogMessage )
			{
				AsyncCallDialogMessage( value ).token.addResponder( new mx.rpc.Responder( closeDialog, closeDialog ) );
			}
			
			runQueue();
			logger.info( "Added item to dialog queue" );
		}
		
		protected static function closeDialog( event:Event = null ):void
		{
			// check to see if an async call has returned while a user action dialog is currently displayed.
			// we don't want the async call to close the user action dialog, instead we will not show the corresponding async diolog
			// when it is up to display
			if ( event && !( currentDialogMessage is AsyncCallDialogMessage ) )
				return;
			
			dialog.removeEventListener( DialogCloseEvent.DIALOG_CLOSE, dialog_closeHandler, false );
			
			if ( dialog is ServicePopUpDialog )
			{
				ServicePopUpDialog( dialog ).busyIndicator.removeFromStage();
			}
			
			PopUpManager.removePopUp( dialog );
			dialogOpen = false;
			currentDialogMessage = null;
			
			logger.info( "Dialog Closed" );
			
			// Run any additional dialogs in the queue
			runQueue();
		}
		
		protected static function dialog_closeHandler( event:DialogCloseEvent ):void
		{
			logger.info( "DialogCloseEvent -> Label: " + event.label );
			
			if ( currentDialogMessage is UserActionDialogMessage )
			{
				executeCallback( UserActionDialogMessage( currentDialogMessage ).actions, event.label );
			}
			else if ( currentDialogMessage is UserActionInputDialogMessage )
			{
				executeCallback( UserActionInputDialogMessage( currentDialogMessage ).actions, event.label, event.additionalArguments );
			}
			
			closeDialog();
		}
		
		protected static function hasAsyncCallReturned( message:AsyncCallDialogMessage ):Boolean
		{
			var token:AsyncToken = message.token;
			return ( token.result ) ? true : false;
		}
		
		protected static function openDialog():void
		{
			dialog = null;
			var dialogClass:Class = currentDialogMessage.dialog;
			
			if ( dialogClass )
			{
				dialog = new dialogClass();
				
				dialog.setDialogMessage( currentDialogMessage );
				dialog.addEventListener( DialogCloseEvent.DIALOG_CLOSE, dialog_closeHandler, false, EventPriority.DEFAULT, true );
				PopUpManager.addPopUp( dialog, DisplayObject( FlexGlobals.topLevelApplication ), currentDialogMessage.modal );
				PopUpManager.centerPopUp( dialog );
				dialogOpen = true;
			}
			else
			{
				logger.error( "DialogClass is null." );
			}
		}
		
		protected static function runQueue():void
		{
			if ( queue.length > 0 && !dialogOpen )
			{
				// Grab next item in the queue
				currentDialogMessage = DialogMessage( queue.shift() );
				var showNext:Boolean = true;
				
				// If message is AsyncCallDialog Message.  If it is check to see
				// if call has already returned.  If so, proceed to next queue item.
				if ( currentDialogMessage is AsyncCallDialogMessage )
				{
					var asyncMessage:AsyncCallDialogMessage = AsyncCallDialogMessage( currentDialogMessage );
					
					if ( hasAsyncCallReturned( asyncMessage ) )
						showNext = false;
				}
				
				if ( showNext )
					openDialog();
				else
					runQueue();
			}
			else
			{
				logger.info( "Reached end of Queue" );
			}
		}
		
		protected static function setupDialogMessage( dialogMessage:DialogMessage, message:String, dialog:Class, defaultDialog:Class ):void
		{
			dialogMessage.message = message;
			
			if ( dialog != null )
			{
				dialogMessage.dialog = dialog;
			}
			else if ( defaultAsyncActionDialog != null )
			{
				dialogMessage.dialog = defaultDialog;
			}
			else
			{
				logger.error( "There is no dialog or default dialog defined." );
				return;
			}
		}
		
		//======================================
		// private static methods 
		//======================================
		
		private static function executeCallback( actions:Array, label:String, args:Array = null ):void
		{
			for each ( var action:DialogUserAction in actions )
			{
				if ( action.name == label )
				{
					if ( Boolean( action.callback ) )
					{
						var arguments:Array = [];
						
						if ( args )
							arguments = arguments.concat( args );
						
						if ( action.args )
							arguments = arguments.concat( action.args );
						
						action.callback.apply( action.scope, arguments );
						
					}
					return;
				}
			}
		}
	}
}

