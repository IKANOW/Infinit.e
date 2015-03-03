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
package com.ikanow.infinit.e.query.model.presentation
{
	import com.ikanow.infinit.e.query.model.manager.QueryManager;
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.manager.SetupManager;
	import com.ikanow.infinit.e.shared.model.manager.UserManager;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.QueryInput;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutput;
	import com.ikanow.infinit.e.shared.model.vo.QueryScoreOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryString;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.QueryTerm;
	import com.ikanow.infinit.e.shared.model.vo.User;
	import com.ikanow.infinit.e.shared.model.vo.Setup;
	import com.ikanow.infinit.e.shared.model.vo.TypedQueryString;
	import com.ikanow.infinit.e.shared.util.ObjectTranslatorUtil;
	import com.ikanow.infinit.e.shared.util.QueryUtil;
	import com.ikanow.infinit.e.widget.library.utility.JSONDecoder;
	import com.ikanow.infinit.e.widget.library.utility.JSONEncoder;
	import com.ikanow.infinit.e.widget.library.utility.URLEncoder;
	
	import flash.events.Event;
	import flash.net.FileFilter;
	import flash.net.FileReference;
	import flash.utils.ByteArray;
	import flash.utils.getDefinitionByName;
	import flash.utils.getQualifiedClassName;
	
	import mx.collections.ArrayCollection;
	import mx.collections.Sort;
	import mx.collections.SortField;
	import mx.controls.Alert;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.events.ResultEvent;
	import mx.rpc.http.HTTPService;
	import mx.utils.ObjectUtil;
	
	/**
	 *  Query Presentation Model
	 */
	public class QueryModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject]
		public var queryManager:QueryManager;
		
		[Bindable]
		[Inject]
		public var navigator:QueryNavigator;
		
		[Inject( "queryManager.lastQueryStringRequest", bind = "true" )]
		/**
		 * The last query string that was performed
		 */
		public var lastQueryStringRequest:QueryStringRequest;
		
		
		[Inject( "userManager", bind = "true" )]
		/**
		 * The setup manager to send queries to
		 */
		public var userManager:UserManager;
		
		[Inject( "setupManager", bind = "true" )]
		/**
		 * The setup manager to send queries to
		 */
		public var setupManager:SetupManager;
		
		//======================================
		// private properties 
		//======================================
		
		private var saveFileReference:FileReference = new FileReference();
		
		private var loadFileReference:FileReference = new FileReference();
		
		private var saveQueryString:String = "empty";
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryModel()
		{
			loadFileReference.addEventListener( Event.SELECT, selectFileHandler );
			loadFileReference.addEventListener( Event.COMPLETE, completeFileHandler );			
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		[Inject( "userManager.currentUser", bind = "true" )]
		/**
		 * The setup
		 */
		public function setCurrentUser( value:User ):void
		{
			populateSavedObjects(true, true);
		}
		
		public function createAdvancedQuery():void
		{
			var queryString:Object = queryManager.createAdvancedQuery();
			saveQueryString = JSONEncoder.encode( queryString );
		}
		
		public function loadAdvancedQuery():void
		{
			loadFileReference.browse( [ new FileFilter( "Infinite Query", "*.infq;" ), new FileFilter( "All Files", "*.*;" ) ] );
		}
		
		/**
		 * Run the advanced query
		 */
		public function runAdvancedQuery():void
		{
			dispatcher.dispatchEvent( new QueryEvent( QueryEvent.RUN_ADVANCED_QUERY ) );
		}
		
		public function exportAdvancedQuery():void
		{
			saveFileReference.save( saveQueryString, "query.infq" );
		}
		
		public function savePivot(title:String):void
		{
			var share:Object = {};
			share.title = title;
			share.share = queryManager.createAdvancedQuery();
			addOrUpdateShare(share, "infinite-saved-pivot", false, true); // add, pivot
		}
		
		public function saveQuery(title:String):void
		{
			var share:Object = {};
			share.title = title;
			share.share = queryManager.createAdvancedQuery();
			addOrUpdateShare(share, "infinite-saved-query", false, false); // add, query
		}
		
		public function deletePivotOrQuery(id:String, pivotNotQuery:Boolean):void
		{
			var request:HTTPService = new HTTPService();
			request.url = ServiceConstants.SERVER_URL + "social/share/remove/" + id;
			if (pivotNotQuery)
				request.addEventListener(ResultEvent.RESULT,removePivotSuccess);
			else
				request.addEventListener(ResultEvent.RESULT,removeQuerySuccess);
			
			request.addEventListener(FaultEvent.FAULT,removePivotOrQueryFault);
			
			request.send();
		}
		
		public function applyPivotAndRunQuery(pivotShare:Object):void
		{
			// this is the current JSON
			var queryJson:Object = queryManager.createAdvancedQuery();
			
			// Apply current JSON to settings
			var newQueryJson:Object = pivotShare.shareJson;
			newQueryJson.qt = queryJson.qt;
			newQueryJson.qtOptions = queryJson.qtOptions;
			newQueryJson.logic = queryJson.logic;
			if ((null != newQueryJson.input) && (newQueryJson.input.overwrite)) { // (dummy param, is ignored except here)
				newQueryJson.input = queryJson.input;
			}
			
			launchQueryFromString(JSONEncoder.encode(newQueryJson));
		}
		
		public function applySavedQueryAndRun(queryShare:Object):void
		{
			launchQueryFromString(JSONEncoder.encode(queryShare.shareJson));
		}
		
		//======================================
		// protected methods 
		//======================================
		
		protected function addOrUpdateShare(share:Object, infiniteType:String, updateNotAdd:Boolean, pivotNotQuery:Boolean):void
		{
			var service:HTTPService = new HTTPService();					
			service.addEventListener(FaultEvent.FAULT, newOrUpdatedSavedPivotOrQueryFault);
			if (pivotNotQuery)
				service.addEventListener(ResultEvent.RESULT, newOrUpdatedSavedPivotSuccess);					
			else
				service.addEventListener(ResultEvent.RESULT, newOrUpdatedSavedQuerySuccess);		
			
			service.method = "POST";
			service.contentType = "application/json";
			if (updateNotAdd)
			{
				service.url = ServiceConstants.SERVER_URL + "social/share/update/json/" + share._id + "/" +
					infiniteType + "/" + URLEncoder.encode( share.title ) + 
					"/" + URLEncoder.encode( share.title );					
			}
			else {
				service.url = ServiceConstants.SERVER_URL + "social/share/add/json/" + 
					infiniteType + "/" + URLEncoder.encode( share.title ) + 
					"/" + URLEncoder.encode( share.title );					
				
			}							
			service.send(JSONEncoder.encode(share.share));				
		}
		
		protected function removePivotSuccess(event:ResultEvent):void
		{
			var data:Object = JSONDecoder.decode( event.result as String );
			if ( data.response.success == false)
			{				
				Alert.show(data.response.message);					
			}
			else
			{
				populateSavedObjects(true, false);			
			}
		}
		
		protected function removeQuerySuccess(event:ResultEvent):void
		{
			var data:Object = JSONDecoder.decode( event.result as String );
			if ( data.response.success == false)
			{				
				Alert.show(data.response.message);					
			}
			else
			{
				populateSavedObjects(false, true);			
			}
		}
		
		protected function removePivotOrQueryFault(event:FaultEvent):void
		{
		}
		
		protected function newOrUpdatedSavedPivotOrQueryFault(event:FaultEvent):void
		{
			//(Do nothing)
		}
		protected function newOrUpdatedSavedPivotSuccess(event:ResultEvent):void
		{
			populateSavedObjects(true, false);
		}
		protected function newOrUpdatedSavedQuerySuccess(event:ResultEvent):void
		{
			populateSavedObjects(false, true);
		}
		protected function getPivotListFault(event:FaultEvent):void
		{
			//(Do nothing)	
		}
		protected function getPivotListSuccess(event:ResultEvent):void
		{
			var newPivots:ArrayCollection = new ArrayCollection();
			var dataSortField:SortField = new SortField("title", true, false);
			var dataSort:Sort = new Sort();
			dataSort.fields=[dataSortField];			
			newPivots.sort = dataSort;			
			
			
			var data:Object = JSONDecoder.decode( event.result as String );
			if ( data.response.success == true)
			{				
				//load the shares into the pivot
				for ( var x:String in data.data )
				{
					var shareObject:Object = data.data[x];
					shareObject.shareJson = JSONDecoder.decode( shareObject.share as String );
					newPivots.addItem(shareObject);
				}
			}
			pivots = newPivots;			
			pivots.refresh();
		}
		
		protected function getQueryListFault(event:FaultEvent):void
		{
			//(Do nothing)	
		}
		protected function getQueryListSuccess(event:ResultEvent):void
		{
			var newQueries:ArrayCollection = new ArrayCollection();
			var dataSortField:SortField = new SortField("title", true, false);
			var dataSort:Sort = new Sort();
			dataSort.fields=[dataSortField];			
			newQueries.sort = dataSort;						
			
			var data:Object = JSONDecoder.decode( event.result as String );
			if ( data.response.success == true)
			{				
				//load the shares into the pivot
				for ( var x:String in data.data )
				{
					var shareObject:Object = data.data[x];
					shareObject.shareJson = JSONDecoder.decode( shareObject.share as String );
					newQueries.addItem(shareObject);
				}
			}
			queries = newQueries;			
			queries.refresh();
		}
		
		public function populateSavedObjects(getPivots:Boolean, getQueries:Boolean):void
		{
			// Find the pivots:
			if (getPivots)
			{
				var request:HTTPService = new HTTPService();
				request.url = ServiceConstants.SERVER_URL + "social/share/search?type=infinite-saved-pivot";
				request.addEventListener(ResultEvent.RESULT,getPivotListSuccess);
				request.addEventListener(FaultEvent.FAULT,getPivotListFault);
				request.send();
			}			
			// Find the saved queries:
			if (getQueries)
			{
				request = new HTTPService();
				request.url = ServiceConstants.SERVER_URL + "social/share/search?type=infinite-saved-query";
				request.addEventListener(ResultEvent.RESULT,getQueryListSuccess);
				request.addEventListener(FaultEvent.FAULT,getQueryListFault);
				request.send();			
			}
		}		
		
		protected function completeFileHandler( event:Event ):void
		{
			launchQueryFromString(loadFileReference.data.toString());
		}
		
		protected function launchQueryFromString( json:String ):void
		{
			var queryString:QueryString = null;
			queryString = ObjectTranslatorUtil.translateObject( JSONDecoder.decode( json ), new QueryString, null, false, true ) as QueryString;
			
			var tempSetup:Setup = new Setup();
			tempSetup.queryString = queryString;
			tempSetup.communityIds = queryString.communityIds.toArray();
			setupManager.setSetup( tempSetup );
		}
		
		protected function selectFileHandler( event:Event ):void
		{
			loadFileReference.load();
		}
		
		//======================================
		// private methods 
		//======================================
		
		private function checkNullOrTranslateArray( obj:Object, translateTo:Object ):ArrayCollection
		{
			if ( obj == null )
				return null;
			else
			{
				return new ArrayCollection( ObjectTranslatorUtil.translateArrayObjects( obj as Array, Class( getDefinitionByName( getQualifiedClassName( translateTo ) ) ) ) );
			}
		}
		
		/**
		 * The collection of recent queries
		 */
		private var _pivots:ArrayCollection;
		
		[Bindable( event = "pivotsChange" )]
		public function get pivots():ArrayCollection
		{
			return _pivots;
		}
		
		public function set pivots( value:ArrayCollection ):void
		{
			if ( _pivots != value )
			{
				_pivots = value;
				dispatchEvent( new Event( "pivotsChange" ) );
			}
		}
		private var _queries:ArrayCollection;
		
		[Bindable( event = "queriesChange" )]
		public function get queries():ArrayCollection
		{
			return _queries;
		}
		
		public function set queries( value:ArrayCollection ):void
		{
			if ( _queries != value )
			{
				_queries = value;
				dispatchEvent( new Event( "queriesChange" ) );
			}
		}
	}
}

