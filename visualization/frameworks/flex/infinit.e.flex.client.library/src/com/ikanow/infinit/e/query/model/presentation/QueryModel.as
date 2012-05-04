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
	import com.ikanow.infinit.e.shared.model.manager.SetupManager;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.QueryInput;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutput;
	import com.ikanow.infinit.e.shared.model.vo.QueryScoreOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryString;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.QueryTerm;
	import com.ikanow.infinit.e.shared.model.vo.Setup;
	import com.ikanow.infinit.e.shared.model.vo.TypedQueryString;
	import com.ikanow.infinit.e.shared.util.ObjectTranslatorUtil;
	import com.ikanow.infinit.e.shared.util.QueryUtil;
	import com.ikanow.infinit.e.widget.library.utility.JSONDecoder;
	import com.ikanow.infinit.e.widget.library.utility.JSONEncoder;
	import flash.events.Event;
	import flash.net.FileFilter;
	import flash.net.FileReference;
	import flash.utils.ByteArray;
	import flash.utils.getDefinitionByName;
	import flash.utils.getQualifiedClassName;
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
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
		
		public function createAdvancedQuery():void
		{
			var queryString:Object = null;
			
			if ( lastQueryStringRequest != null )
			{
				queryString = QueryUtil.getQueryStringObject( lastQueryStringRequest );
			}
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
		
		public function saveAdvancedQuery():void
		{
			saveFileReference.save( saveQueryString, "query.infq" );
		}
		
		//======================================
		// protected methods 
		//======================================
		
		protected function completeFileHandler( event:Event ):void
		{
			var queryString:QueryString = null;
			var json:String = loadFileReference.data.toString();
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
	}
}

