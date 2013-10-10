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
package com.ikanow.infinit.e.source.control
{
	import com.ikanow.infinit.e.shared.control.base.InfiniteController;
	import com.ikanow.infinit.e.shared.event.SourceEvent;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.source.model.manager.SourceManager;
	import com.ikanow.infinit.e.source.service.ISourceServiceDelegate;
	import mx.collections.ArrayCollection;
	import mx.rpc.events.ResultEvent;
	
	/**
	 * Source Controller
	 */
	public class SourceController extends InfiniteController
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject]
		public var sourceServiceDelegate:ISourceServiceDelegate
		
		[Inject]
		public var sourceManager:SourceManager;
		
		
		//======================================
		// public methods 
		//======================================
		
		[EventHandler( event = "SourceEvent.GET_SOURCES_GOOD" )]
		/**
		 * Get Sources Good
		 * @param event
		 */
		public function getSourcesGood( event:SourceEvent ):void
		{
			executeServiceCall( "SourceController.getSourcesGood()", event, sourceServiceDelegate.getSourcesGood( event ), getSourcesGood_resultHandler, defaultFaultHandler );
		}
		
		/**
		 * Get Sources Good Result Handler
		 * @param event
		 */
		public function getSourcesGood_resultHandler( event:ResultEvent ):void
		{
			if ( verifyServiceResponseSuccess( "getSourcesGood()", event.result as ServiceResult ) )
				sourceManager.setSources( ServiceResult( event.result ).data as ArrayCollection );
		}
		
		[EventHandler( event = "SourceEvent.RESET" )]
		/**
		 * Reset Sources
		 * @param event
		 */
		public function resetSources( event:SourceEvent ):void
		{
			sourceManager.reset();
		}
	}
}
