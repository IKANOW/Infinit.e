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
package com.ikanow.infinit.e.model.presentation
{
	import com.ikanow.infinit.e.shared.event.SessionEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	
	/**
	 *  Main Presentation Model
	 */
	public class MainModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:MainNavigator;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Mouse Move Handler
		 * @param username
		 * @param password
		 */
		public function mouseMoveHandler( mouseX:Number, mouseY:Number ):void
		{
			var sessionEvent:SessionEvent = new SessionEvent( SessionEvent.MOUSE_MOVE );
			sessionEvent.mouseX = mouseX;
			sessionEvent.mouseY = mouseY;
			dispatcher.dispatchEvent( sessionEvent );
		}
	}
}

