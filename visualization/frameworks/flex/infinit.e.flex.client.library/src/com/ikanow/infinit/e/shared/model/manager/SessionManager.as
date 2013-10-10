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
package com.ikanow.infinit.e.shared.model.manager
{
	import com.ikanow.infinit.e.shared.event.CommunityEvent;
	import com.ikanow.infinit.e.shared.event.NavigationEvent;
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.event.SessionEvent;
	import com.ikanow.infinit.e.shared.event.SetupEvent;
	import com.ikanow.infinit.e.shared.event.SourceEvent;
	import com.ikanow.infinit.e.shared.event.UserEvent;
	import com.ikanow.infinit.e.shared.event.WidgetEvent;
	import com.ikanow.infinit.e.shared.event.WorkspaceEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResponse;
	import com.ikanow.infinit.e.shared.util.BrowserUtil;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.shared.util.ServiceUtil;
	import flash.display.Sprite;
	import flash.events.Event;
	import flash.events.EventDispatcher;
	import flash.events.TimerEvent;
	import flash.net.URLRequest;
	import flash.net.navigateToURL;
	import flash.utils.Timer;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	import mx.core.Application;
	import mx.core.FlexGlobals;
	import mx.core.UIComponent;
	import mx.events.CloseEvent;
	import mx.logging.ILogger;
	import mx.logging.Log;
	import mx.resources.ResourceManager;
	
	/**
	 * Session Manager
	 */
	public class SessionManager extends InfiniteManager
	{
		
		//======================================
		// private static properties 
		//======================================
		
		private static const logger:ILogger = Log.getLogger( "session" );
		
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		/**
		 * The cookie response
		 */
		public var cookieResponse:ServiceResponse;
		
		[Bindable]
		/**
		 * The login response
		 */
		public var loginResponse:ServiceResponse;
		
		[Bindable]
		/**
		 * Message for the busy indicator label
		 */
		public var busyIndicatorMessage:String;
		
		//======================================
		// protected properties 
		//======================================
		
		/**
		 * A dictionary used to store the message queue
		 */
		protected var messages:ArrayCollection = new ArrayCollection();
		
		//======================================
		// private properties 
		//======================================
		
		private var keepAliveTimer:Timer;
		
		private var lastMousePositionX:Number = 0;
		
		private var lastMousePositionY:Number = 0;
		
		private var currentMousePositionX:Number = 0;
		
		private var currentMousePositionY:Number = 0;
		
		private var idleTime:Number = 0;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Handles the data loading event
		 * @param message
		 */
		public function dataLoadinghandler( dialogControl:DialogControl ):void
		{
			if ( dialogControl && dialogControl.show && dialogControl.message )
			{
				messages.addItem( dialogControl );
				
				showNextMessage();
			}
		}
		
		/**
		 * Handles the data ready event
		 * @param message
		 */
		public function dataReadyhandler( dialogControl:DialogControl ):void
		{
			logger.info( dialogControl.description + " service call duration:  " + dialogControl.duration + "ms" );
			
			if ( dialogControl && dialogControl.show && dialogControl.message )
			{
				CollectionUtil.removeItemById( messages, dialogControl._id );
				
				if ( dialogControl.message == ServiceConstants.SERVICE_FAULT )
				{
					
					setBusyIndicatorMessage( dialogControl.message );
					messages.removeAll();
				}
				else
				{
					showNextMessage();
				}
			}
		}
		
		/**
		 * On successful return on keep alive service, checks if user
		 * cookie is no longer active, or if they have not moved the mouse in last 15 minutes.
		 *
		 * If they are inactive/invalid then display logout alert.
		 *
		 * Saves new mouse position.
		 */
		public function keepAlive_resultHandler( value:ServiceResponse ):void
		{
			if ( value.responseSuccess )
			{
				// save the setup
				saveSetup();
				
				// check if mouse has moved
				if ( lastMousePositionX == currentMousePositionX && lastMousePositionY == currentMousePositionY )
				{
					// increment counter for how long user has been idle
					idleTime += Constants.SESSION_KEEP_ALIVE_TIMER;
					
					// check if user has been idle for > 15min
					if ( idleTime > Constants.SESSION_TIMEOUT )
					{
						showLogoutAlert( ResourceManager.getInstance().getString( 'infinite', 'sessionManager.sessionTimoutMessage' ) );
					}
				}
				else
				{
					// reset counter for idle user
					idleTime = 0;
				}
				
				lastMousePositionX = currentMousePositionX;
				lastMousePositionY = currentMousePositionY;
			}
			else
			{
				showLogoutAlert( value.message );
			}
		}
		
		/**
		 * Logout response handler
		 * @param value
		 */
		public function logoutResponseHandler():void
		{
			var urlRequest:URLRequest = new URLRequest( ServiceUtil.getLogoutDomain( ServiceConstants.DOMAIN_LOGOUT_URL ) );
			navigateToURL( urlRequest, ServiceConstants.SELF_URL );
			
			return;
		}
		
		/**
		 * Mouse Move Handler
		 * @param value
		 */
		public function mouseMoveHandler( mouseX:Number, mouseY:Number ):void
		{
			currentMousePositionX = mouseX;
			currentMousePositionY = mouseY;
		}
		
		/**
		 * Get Cookie response from server
		 * @param value
		 */
		public function setGetCookieResponse( value:ServiceResponse ):void
		{
			cookieResponse = value;
			
			// setup the keep alive timer that will kill the session if cookies die	
			if ( value.responseSuccess )
				startKeepAliveTimer();
		}
		
		/**
		 * Login response from server
		 * @param value
		 */
		public function setLoginResponse( value:ServiceResponse ):void
		{
			loginResponse = value;
			
			// setup the keep alive timer that will kill the session if cookies die	
			if ( value.responseSuccess )
				startKeepAliveTimer();
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * When timer expires, dispatch keep alive event
		 */
		protected function keepAliveTimerHandler( event:TimerEvent ):void
		{
			var sessionEvent:SessionEvent = new SessionEvent( SessionEvent.KEEP_ALIVE );
			sessionEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'sessionService.keepAlive' ) );
			dispatcher.dispatchEvent( sessionEvent );
		}
		
		/**
		 * Logout Alert Close Handler
		 * Dispatch a logout event
		 */
		protected function logoutAlertCloseHandler( event:CloseEvent ):void
		{
			var sessionEvent:SessionEvent = new SessionEvent( SessionEvent.LOGOUT );
			sessionEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'sessionService.logout' ) );
			dispatcher.dispatchEvent( sessionEvent );
		}
		
		/**
		 * Reset Application
		 */
		protected function resetApplication():void
		{
			dispatcher.dispatchEvent( new NavigationEvent( NavigationEvent.RESET ) );
			dispatcher.dispatchEvent( new SetupEvent( SetupEvent.RESET ) );
			dispatcher.dispatchEvent( new CommunityEvent( CommunityEvent.RESET ) );
			dispatcher.dispatchEvent( new SourceEvent( SourceEvent.RESET ) );
			dispatcher.dispatchEvent( new QueryEvent( QueryEvent.RESET ) );
			dispatcher.dispatchEvent( new WidgetEvent( WidgetEvent.RESET ) );
			dispatcher.dispatchEvent( new WorkspaceEvent( WorkspaceEvent.RESET ) );
			dispatcher.dispatchEvent( new UserEvent( UserEvent.RESET ) );
		}
		
		/**
		 * Save the Setup
		 */
		protected function saveSetup():void
		{
			var setupEvent:SetupEvent = new SetupEvent( SetupEvent.SAVE_SETUP );
			setupEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'setupService.saveSetup' ) );
			dispatcher.dispatchEvent( setupEvent );
		}
		
		/**
		 * Set the busy indicator message
		 * @param message
		 */
		protected function setBusyIndicatorMessage( message:String ):void
		{
			busyIndicatorMessage = message;
		}
		
		/**
		 * Show Login View
		 */
		protected function showLoginView():void
		{
			var navigationEvent:NavigationEvent;
			
			navigationEvent = new NavigationEvent( NavigationEvent.NAVIGATE_BY_ID );
			navigationEvent.navigationItemId = NavigationConstants.MAIN_LOGIN_ID;
			dispatcher.dispatchEvent( navigationEvent );
			
			navigationEvent = new NavigationEvent( NavigationEvent.NAVIGATE_BY_ID );
			navigationEvent.navigationItemId = NavigationConstants.LOGIN_FORM_ID;
			dispatcher.dispatchEvent( navigationEvent );
		}
		
		/**
		 * Show Logout Alert
		 * Used for when cookies die.
		 * @param message
		 */
		protected function showLogoutAlert( message:String ):void
		{
			keepAliveTimer.stop();
			Alert.show( message, ResourceManager.getInstance().getString( 'infinite', 'sessionManager.logoutAlertTitle' ), 4, FlexGlobals.topLevelApplication as Sprite, logoutAlertCloseHandler );
		}
		
		/**
		 * Set the busy indicator message to the next message in messages
		 */
		protected function showNextMessage():void
		{
			if ( messages.length > 0 )
			{
				var dialogControl:DialogControl = messages.getItemAt( 0 ) as DialogControl;
				setBusyIndicatorMessage( dialogControl.message );
			}
			else
			{
				setBusyIndicatorMessage( Constants.BLANK );
			}
		}
		
		/**
		 * Start Keep Alive Timer
		 */
		protected function startKeepAliveTimer():void
		{
			keepAliveTimer = new Timer( Constants.SESSION_KEEP_ALIVE_TIMER );
			keepAliveTimer.addEventListener( TimerEvent.TIMER, keepAliveTimerHandler );
			keepAliveTimer.start();
		}
	}
}
