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
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.QueryInput;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutput;
	import com.ikanow.infinit.e.shared.model.vo.QueryScoreOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryString;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.QueryTerm;
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
			var qs:Object = ObjectUtil.copy( queryManager.saveAdvancedQuery() );
			qs[ "tempCommIDs" ] = qs.queryString.communityIds.source;
			qs[ "tempQT" ] = qs.queryString.qt.source;
			
			if ( qs.queryString.input != null )
				qs[ "tempInputSources" ] = qs.queryString.input.sources.source;
			else
				qs[ "tempInputSources" ] = null;
			saveQueryString = JSONEncoder.encode( qs );
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
			var json:String = loadFileReference.data.toString();
			var jsonObject:Object = JSONDecoder.decode( json );
			var tempQueryString:Object = ObjectUtil.copy( jsonObject.queryString );
			
			//try to convert object, the arraycollections do not get converted correctly :( do by hand
			var typedQueryString:TypedQueryString = ObjectTranslatorUtil.translateObject( jsonObject, new TypedQueryString, null, false, true ) as TypedQueryString;
			//TRY TO CONVERT QUERYSTRING BY HAND
			typedQueryString.queryString.communityIds = checkNullOrTranslateArray( jsonObject.tempCommIDs, new String() );
			typedQueryString.queryString.qt = checkNullOrTranslateArray( jsonObject.tempQT, new QueryTerm() );
			
			if ( typedQueryString.queryString.input != null )
				typedQueryString.queryString.input.sources = checkNullOrTranslateArray( jsonObject.tempInputSources, new String() );
			
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.RUN_HISTORY_QUERY );
			queryEvent.typedQueryString = typedQueryString;
			dispatcher.dispatchEvent( queryEvent );
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

