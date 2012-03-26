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
package com.ikanow.infinit.e.workspace.model.presentation.settings
{
	import com.ikanow.infinit.e.query.model.manager.QueryManager;
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutputAggregationOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutputDocumentOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryScoreOptions;
	import flash.events.Event;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.resources.ResourceManager;
	import mx.utils.ObjectUtil;
	
	/**
	 *  Workspace Settings Presentation Model
	 */
	public class WorkspaceSettingsModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		public var isClean:Boolean = true;
		
		[Bindable]
		[Inject]
		public var navigator:WorkspaceSettingsNavigator;
		
		[Inject]
		public var queryManager:QueryManager;
		
		protected var _documentOptions:QueryOutputDocumentOptions;
		
		[Bindable( "documentOptionsChanged" )]
		[Inject( "queryManager.documentOptions", bind = "true" )]
		public function get documentOptions():QueryOutputDocumentOptions
		{
			return _documentOptions;
		}
		
		public function set documentOptions( value:QueryOutputDocumentOptions ):void
		{
			if ( value == null )
				return;
			
			_documentOptions = value.clone();
			dispatchEvent( new Event( "documentOptionsChanged" ) );
		}
		
		public var documentOptionsFormValues:QueryOutputDocumentOptions;
		
		protected var _aggregationOptions:QueryOutputAggregationOptions;
		
		[Bindable( "aggregationOptionsChanged" )]
		[Inject( "queryManager.aggregationOptions", bind = "true" )]
		public function get aggregationOptions():QueryOutputAggregationOptions
		{
			return _aggregationOptions;
		}
		
		public function set aggregationOptions( value:QueryOutputAggregationOptions ):void
		{
			if ( value == null )
				return;
			
			_aggregationOptions = value.clone();
			dispatchEvent( new Event( "aggregationOptionsChanged" ) );
		}
		
		public var aggregationOptionsFormValues:QueryOutputAggregationOptions;
		
		protected var _scoreOptions:QueryScoreOptions;
		
		[Bindable( "scoreOptionsChanged" )]
		[Inject( "queryManager.scoreOptions", bind = "true" )]
		public function get scoreOptions():QueryScoreOptions
		{
			return _scoreOptions;
		}
		
		public function set scoreOptions( value:QueryScoreOptions ):void
		{
			if ( value == null )
				return;
			
			_scoreOptions = value.clone();
			dispatchEvent( new Event( "scoreOptionsChanged" ) );
		}
		
		public var scoreOptionsFormValues:QueryScoreOptions;
		
		[Bindable]
		public var rankByOptions:ArrayCollection = new ArrayCollection( [ ResourceManager.getInstance().getString( 'infinite', 'workspaceSettings.rankByScore' ),
																		  ResourceManager.getInstance().getString( 'infinite', 'workspaceSettings.rankByDate' ) ] );
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Return to Content View
		 */
		public function cancel():void
		{
			navigator.showContentView();
			
			resetClones();
			
			documentOptionsFormValues.apply( documentOptions );
			aggregationOptionsFormValues.apply( aggregationOptions );
			scoreOptionsFormValues.apply( scoreOptions );
		}
		
		public function checkForChanges():void
		{
			var docOptionsClean:Boolean = ObjectUtil.compare( documentOptionsFormValues, documentOptions ) == 0;
			var aggOptionsClean:Boolean = ObjectUtil.compare( aggregationOptionsFormValues, aggregationOptions ) == 0;
			var scoreOptionsClean:Boolean = ObjectUtil.compare( scoreOptionsFormValues, scoreOptions ) == 0;
			
			isClean = docOptionsClean && aggOptionsClean && scoreOptionsClean;
		}
		
		public function restoreDefaults():void
		{
			// apply the state of the form to our model
			// so that when we reset to the default values it actually triggers bindings
			documentOptions.apply( documentOptionsFormValues );
			aggregationOptions.apply( aggregationOptionsFormValues );
			scoreOptions.apply( scoreOptionsFormValues );
			
			documentOptions.reset();
			aggregationOptions.reset();
			scoreOptions.reset();
			
			checkForChanges();
		}
		
		public function save():void
		{
			navigator.showContentView();
			
			documentOptions.apply( documentOptionsFormValues );
			aggregationOptions.apply( aggregationOptionsFormValues );
			scoreOptions.apply( scoreOptionsFormValues );
			
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.SAVE_QUERY_ADVANCED_SETTINGS );
			queryEvent.documentOptions = documentOptions;
			queryEvent.aggregationOptions = aggregationOptions;
			queryEvent.scoreOptions = scoreOptions;
			
			setTimeout( dispatcher.dispatchEvent, 200, queryEvent );
			
			isClean = true;
		}
		
		//======================================
		// private methods 
		//======================================
		
		private function resetClones():void
		{
			documentOptions = queryManager.documentOptions.clone();
			aggregationOptions = queryManager.aggregationOptions.clone();
			scoreOptions = queryManager.scoreOptions.clone();
		}
	}
}

