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
package com.ikanow.infinit.e.shared.service.base
{
	import flash.events.Event;
	import flash.events.EventDispatcher;
	import flash.events.IEventDispatcher;
	import flash.utils.Dictionary;
	
	import mx.rpc.AsyncToken;
	import mx.rpc.Fault;
	import mx.rpc.IResponder;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.events.ResultEvent;
	
	/**
	 * The Delegate class is to be used as a base class for all business delegates
	 * within a Swiz project.  The main purpose of this design is to transparently
	 * allow for service interaction and data translation within the delegate.  For
	 * example, if you are dealing with a REST service, the actual service call and
	 * JSON translation happens within the delegate.  If at a later time the service
	 * becomes a RemoteObject call, no code outside of the delegate should have to be
	 * changed for the functionality.
	 *
	 * ONLY the delegate should deal in non-application domain code (such as JSON).
	 */
	public class Delegate extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Dispatcher]
		public var dispatcher:IEventDispatcher;
		
		//======================================
		// protected properties 
		//======================================
		
		/**
		 * A dictionary used to store a relationship between the actual AsyncToken
		 * from the service call with the AsyncToken used by responders outside of
		 * the delegate.
		 */
		protected var tokenLookup:Dictionary;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor.
		 */
		public function Delegate()
		{
			tokenLookup = new Dictionary();
		}
		
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Method which forms a FaultEvent to be used with the external responders
		 * of a delegate.
		 *
		 * @param result The result data
		 * @param token The external AsyncToken
		 */
		protected function createFaultEvent( fault:Fault, token:AsyncToken ):FaultEvent
		{
			return new FaultEvent( FaultEvent.FAULT, false, true, fault, token );
		}
		
		/**
		 * Method which forms a ResultEvent to be used with the external responders
		 * of a delegate.
		 *
		 * @param result The result data
		 * @param token The external AsyncToken
		 */
		protected function createResultEvent( result:Object, token:AsyncToken ):ResultEvent
		{
			return new ResultEvent( ResultEvent.RESULT, false, true, result, token );
		}
		
		/**
		 * Method which is called after a delegate's internal result or fault
		 * handler has been called and we need to notify the external responders.
		 *
		 * @param event The result or fault event to send to the responders
		 */
		protected function notifyResponders( event:Event ):void
		{
			var responder:IResponder;
			
			if ( event is ResultEvent )
			{
				for each ( responder in ResultEvent( event ).token.responders )
				{
					responder.result( event );
				}
			}
			else if ( event is FaultEvent )
			{
				for each ( responder in FaultEvent( event ).token.responders )
				{
					responder.fault( event );
				}
			}
		}
	}
}
