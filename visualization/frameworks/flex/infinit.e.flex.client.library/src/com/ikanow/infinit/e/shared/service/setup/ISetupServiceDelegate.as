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
