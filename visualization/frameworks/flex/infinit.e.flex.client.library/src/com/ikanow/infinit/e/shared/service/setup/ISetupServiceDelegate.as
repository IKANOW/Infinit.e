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
package com.ikanow.infinit.e.shared.service.setup
{
	import com.ikanow.infinit.e.shared.event.SetupEvent;
	import mx.rpc.AsyncToken;
	
	public interface ISetupServiceDelegate
	{
		
		/**
		 * Get Modules All
		 * @param event
		 * @return AsyncToken
		 */
		function getModulesAll( event:SetupEvent ):AsyncToken
		
		/**
		 * Get Modules User
		 * @param event
		 * @return AsyncToken
		 */
		function getModulesUser( event:SetupEvent ):AsyncToken
		
		/**
		 * Get Setup
		 * This will return the saved values for the UI
		 * @param event
		 * @return AsyncToken
		 */
		function getSetup( event:SetupEvent ):AsyncToken;
		
		function getWidgetOptions( event:SetupEvent ):AsyncToken
		
		/**
		 * Set Modules User
		 * @param event
		 * @return AsyncToken
		 */
		function setModulesUser( event:SetupEvent ):AsyncToken
		
		/**
		 * Update Setup
		 * @param event
		 * @return AsyncToken
		 */
		function updateSetup( event:SetupEvent ):AsyncToken
	}
}
