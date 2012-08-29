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
package com.ikanow.infinit.e.shared.util
{
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import mx.core.FlexGlobals;
	import mx.managers.BrowserManager;
	import mx.managers.IBrowserManager;
	
	public class BrowserUtil
	{
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 *
		 * Constructor
		 *
		 */
		public function BrowserUtil()
		{
		
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Clears the URL parameters of the URL
		 *
		 * @return
		 *
		 */
		public static function clearBrowserURLFragment():void
		{
			var browserManager:IBrowserManager = BrowserManager.getInstance();
			browserManager.setFragment( Constants.BLANK );
		}
		
		/**
		 * Gets the browser url
		 *
		 * @return String
		 *
		 */
		public static function getBrowserDomain():String
		{
			var local:Boolean;
			var root:String = FlexGlobals.topLevelApplication.url;
			var appHTML:String;
			
			if ( root )
			{
				local = root.indexOf( ServiceConstants.BIN_DEBUG ) != -1;
				root = root.slice( 0, root.lastIndexOf( Constants.FORWARD_SLASH ) + 1 );
			}
			
			return root;
		}
		
		/**
		 * Gets the browser url
		 *
		 * @return String
		 *
		 */
		public static function getBrowserURL():String
		{
			var local:Boolean;
			var root:String = FlexGlobals.topLevelApplication.url;
			var appHTML:String;
			
			if ( root )
			{
				local = root.indexOf( ServiceConstants.BIN_DEBUG ) != -1;
				root = root.slice( 0, root.lastIndexOf( Constants.FORWARD_SLASH ) + 1 );
				appHTML = local ? ServiceConstants.APP_HTML_LOCAL : ServiceConstants.APP_HTML_SERVER;
			}
			
			return root + appHTML;
		}
		
		/**
		 * Sets the URL parameters of the URL
		 *
		 * @return
		 *
		 */
		public static function setBrowserURLFragment( path:String ):void
		{
			var browserManager:IBrowserManager = BrowserManager.getInstance();
			browserManager.setFragment( path );
		}
		
		/**
		 * Sets the Title of the browser window
		 *
		 * @return
		 *
		 */
		public static function setBrowserWindowTitle( title:String ):void
		{
			var browserManager:IBrowserManager = BrowserManager.getInstance();
			browserManager.setTitle( title );
		}
	}
}
