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
package views
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class DialogControl
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var _id:Number;
		
		public var show:Boolean;
		
		public var title:String;
		
		public var message:String;
		
		public var description:String;
		
		public var startTime:Date
		
		public var endTime:Date;
		
		public function get duration():int
		{
			return endTime.time - startTime.time;
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		public static function create( showDilaog:Boolean, dialogMessage:String = "", dialogTitle:String = "" ):DialogControl
		{
			var dialogControl:DialogControl = new DialogControl();
			dialogControl.show = showDilaog;
			dialogControl.message = dialogMessage;
			dialogControl.title = dialogTitle;
			return dialogControl;
		}
	}
}
