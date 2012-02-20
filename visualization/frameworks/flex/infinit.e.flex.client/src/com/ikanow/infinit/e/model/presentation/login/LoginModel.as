package com.ikanow.infinit.e.model.presentation.login
{
	import com.ikanow.infinit.e.shared.event.CommunityEvent;
	import com.ikanow.infinit.e.shared.event.SessionEvent;
	import com.ikanow.infinit.e.shared.event.SetupEvent;
	import com.ikanow.infinit.e.shared.event.UserEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.User;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResponse;
	import flash.events.Event;
	import flash.net.URLRequest;
	import flash.net.navigateToURL;
	import flash.utils.setTimeout;
	import mx.controls.Alert;
	import mx.resources.ResourceManager;
	
	/**
	 *  Login Presentation Model
	 */
	public class LoginModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:LoginNavigator;
		
		[Bindable]
		/**
		 * Password
		 */
		public var password:String;
		
		//======================================
		// protected properties 
		//======================================
		
		/**
		 * The cookie response
		 */
		protected var cookieResponse:ServiceResponse;
		
		/**
		 * The login response
		 */
		protected var loginResponse:ServiceResponse;
		
		/**
		 * The current user
		 */
		protected var currentUser:User;
		
		/**
		 * The user name
		 */
		protected var userName:String;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Clears the password
		 * Called from navigator.showDashboardView()
		 */
		public function clearPassword():void
		{
			password = Constants.WILDCARD;
			password = Constants.BLANK;
		}
		
		/**
		 * Forgot password
		 * Opens the forgot password html page in a new browser window
		 */
		public function forgotPassword( userName:String ):void
		{
			var urlRequest:URLRequest = new URLRequest( ServiceConstants.FORGOT_PASSWORD_URL + userName );
			navigateToURL( urlRequest, ServiceConstants.BLANK_URL );
		}
		
		/**
		 * Get Cookie
		 */
		public function getCookie():void
		{
			var sessionEvent:SessionEvent = new SessionEvent( SessionEvent.GET_COOKIE );
			sessionEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'sessionService.getCookie' ) );
			dispatcher.dispatchEvent( sessionEvent );
		}
		
		/**
		 * Log In
		 * @param username
		 * @param password
		 */
		public function login( name:String, password:String ):void
		{
			userName = name;
			
			navigator.showLoadingDataView();
			
			var sessionEvent:SessionEvent = new SessionEvent( SessionEvent.LOGIN );
			sessionEvent.username = name;
			sessionEvent.password = password;
			sessionEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'sessionService.login' ) );
			dispatcher.dispatchEvent( sessionEvent );
		}
		
		/**
		 * Cookie Response
		 * @param value
		 */
		[Inject( "sessionManager.cookieResponse", bind = "true" )]
		public function setCookieResponse( value:ServiceResponse ):void
		{
			if ( value != null )
			{
				cookieResponse = value;
				
				// getCookie() call returned
				cookie_responseHandler();
			}
		}
		
		/**
		 * Current User
		 * @param value
		 */
		[Inject( "userManager.currentUser", bind = "true" )]
		public function setCurrentUser( value:User ):void
		{
			currentUser = value;
			
			// getUser() was successfull
			getUser_responseHandler();
		}
		
		/**
		 * Login Response
		 * @param value
		 */
		[Inject( "sessionManager.loginResponse", bind = "true" )]
		public function setLoginResponse( value:ServiceResponse ):void
		{
			if ( value != null )
			{
				loginResponse = value;
				
				// login() call returned
				login_responseHandler();
			}
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Validates if the cookie was successful
		 */
		protected function cookie_responseHandler():void
		{
			if ( cookieResponse.responseSuccess )
			{
				// get the default data for the application
				getApplicationData();
			}
			else
			{
				navigator.showLoginView();
			}
		}
		
		/**
		 * Get the default data for the application
		 */
		protected function getApplicationData():void
		{
			getUser();
			getSetup();
			getCommunities();
			getModules();
			getUserModules();
		}
		
		/**
		 * Get Communities
		 */
		protected function getCommunities():void
		{
			var communityEvent:CommunityEvent = new CommunityEvent( CommunityEvent.GET_COMMUNITIES_PUBLIC );
			communityEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'communityService.getCommunities' ) );
			dispatcher.dispatchEvent( communityEvent );
		}
		
		/**
		 * Get Modules
		 */
		protected function getModules():void
		{
			var setupEvent:SetupEvent = new SetupEvent( SetupEvent.GET_MODULES_ALL );
			setupEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'setupService.getModulesAll' ) );
			dispatcher.dispatchEvent( setupEvent );
		}
		
		/**
		 * Get Setup
		 */
		protected function getSetup():void
		{
			var setupEvent:SetupEvent = new SetupEvent( SetupEvent.GET_SETUP );
			setupEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'setupService.getSetup' ) );
			dispatcher.dispatchEvent( setupEvent );
		}
		
		/**
		 * Get User
		 */
		protected function getUser():void
		{
			var userEvent:UserEvent = new UserEvent( UserEvent.GET_USER );
			userEvent.userName = userName;
			userEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'userService.getUser' ) );
			dispatcher.dispatchEvent( userEvent );
		}
		
		/**
		 * Get User Modules
		 */
		protected function getUserModules():void
		{
			var setupEvent:SetupEvent = new SetupEvent( SetupEvent.GET_MODULES_USER );
			setupEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'setupService.getModulesUser' ) );
			dispatcher.dispatchEvent( setupEvent );
		}
		
		/**
		 * Called if getUser() was successful
		 */
		protected function getUser_responseHandler():void
		{
			// show the dashboard view
			if ( currentUser )
				navigator.showDashboardView();
		}
		
		/**
		 * Validates if the login was successful
		 */
		protected function login_responseHandler():void
		{
			if ( loginResponse.responseSuccess )
			{
				navigator.showLoadingDataView();
				
				// get the default data for the application
				getApplicationData();
			}
			else
			{
				navigator.showLoginView();
				
				Alert.show( ResourceManager.getInstance().getString( 'infinite', 'loginModel.loginFailedMessage' ), ResourceManager.getInstance().getString( 'infinite', 'loginModel.loginFailed' ) );
			}
		}
	}
}

