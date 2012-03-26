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
	import flash.external.ExternalInterface;
	
	public class ExternalInterfaceUtility
	{
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Function to add external javascript events to outside javascript page.
		 * This function does not return what actionscript will return (i.e. the
		 * results of a function in AS3 do not get spit back to javascript currently)
		 * To change this change the jsBindEvent to: function(){return "+jsExecuteCallBack+"};}";
		 *
		 * @param qualifiedEventName name of the event to bind to (i.e. beforeunloadevent)
		 * @param callback AS3 function to call when the javascript function is fired
		 * @param callBackAlias Name of this event listener (not used specifically, think it has to be unique though)
		 */
		public static function addExternalEventListener( qualifiedEventName:String, callback:Function, callBackAlias:String ):void
		{
			// 1. Expose the callback function via the callBackAlias
			ExternalInterface.addCallback( callBackAlias, callback );
			
			// 2. Build javascript to execute
			var jsExecuteCallBack:String = "document.getElementsByName('" + ExternalInterface.objectID + "')[0]." + callBackAlias + "()";
			var jsBindEvent:String = "function(){" + qualifiedEventName + "= function(){ " + jsExecuteCallBack + "};}";
			
			// 3. Execute the composed javascript to perform the binding of the external event to the specified callBack function
			ExternalInterface.call( jsBindEvent );
		}
	}
}
