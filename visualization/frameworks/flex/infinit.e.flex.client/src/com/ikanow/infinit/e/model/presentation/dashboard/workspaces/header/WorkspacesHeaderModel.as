package com.ikanow.infinit.e.model.presentation.dashboard.workspaces.header
{
	import com.ikanow.infinit.e.shared.event.WidgetEvent;
	import com.ikanow.infinit.e.shared.event.WorkspaceEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.DelegateConstants;
	import com.ikanow.infinit.e.shared.model.constant.ExportConstants;
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryOperatorTypes;
	import com.ikanow.infinit.e.shared.model.manager.WidgetModuleManagerCopy;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutputRequest;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceStatistics;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.shared.util.JSONUtil;
	import com.ikanow.infinit.e.shared.util.PasswordUtil;
	import com.ikanow.infinit.e.shared.util.QueryUtil;
	import com.ikanow.infinit.e.shared.util.ServiceUtil;
	import com.ikanow.infinit.e.widget.library.data.WidgetContext;
	import com.ikanow.infinit.e.widget.library.frameworkold.QueryResults;
	import com.ikanow.infinit.e.widget.library.widget.IResultSet;
	import flash.events.Event;
	import flash.net.FileReference;
	import flash.net.URLRequest;
	import flash.net.navigateToURL;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.collections.ListCollectionView;
	import mx.resources.ResourceManager;
	import spark.formatters.NumberFormatter;
	
	/**
	 *  Workspaces Header Presentation Model
	 */
	public class WorkspacesHeaderModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Indicates if the busy indicator has been shown at least once
		 */
		public var busyIndicatorActive:Boolean;
		
		[Bindable]
		[Inject]
		/**
		 *
		 * @default
		 */
		public var navigator:WorkspacesHeaderNavigator;
		
		[Bindable]
		/**
		 * The statistics returned from a query
		 */
		public var queryStatistics:ServiceStatistics = new ServiceStatistics();
		
		[Bindable]
		/**
		 * The results count message
		 */
		public var resultsCountMessage:String = ResourceManager.getInstance().getString( 'infinite', 'workspacesHeaderModel.runInitialQuery' );
		
		[Bindable]
		/**
		 * Show the results count
		 */
		public var showResultsCount:Boolean = true;
		
		[Bindable]
		/**
		 * The message for the busy indicator
		 */
		public var busyIndicatorMessage:String;
		
		[Bindable]
		[Inject( "workspaceManager.selectedWidgetsSorted", bind = "true" )]
		/**
		 *
		 * @default
		 */
		public var selectedWidgetsSorted:ListCollectionView;
		
		[Bindable]
		[Inject( "workspaceManager.maximizedWidget", bind = "true" )]
		/**
		 *
		 * @default
		 */
		public var maximizedWidget:Widget;
		
		[Bindable]
		[Inject( "workspaceManager.maximized", bind = "true" )]
		/**
		 *
		 * @default
		 */
		public var maximized:Boolean;
		
		[Inject( "widgetModuleManager.context", bind = "true" )]
		/**
		 *
		 * @default
		 */
		public var context:WidgetContext;
		
		[Inject( "queryManager.queryResultString", bind = "true" )]
		/**
		 * A copy of the results returned from a query
		 */
		public var queryResultString:String;
		
		[Inject( "queryManager.currentQueryStringRequest", bind = "true" )]
		/**
		 * The current query string that is being modified for a new query
		 */
		public var currentQueryStringRequest:QueryStringRequest;
		
		[Bindable]
		[Inject( "widgetModuleManager.filterActive", bind = "true" )]
		/**
		 *
		 * @default
		 */
		public var filterActive:Boolean;
		
		[Bindable]
		[Inject( "widgetModuleManager.filterToolTip", bind = "true" )]
		/**
		 * The tooltip for the current filter
		 */
		public var filterToolTip:String;
		
		[Inject( "queryManager.lastQueryStringRequest", bind = "true" )]
		/**
		 * The last query string that was performed
		 */
		public var lastQueryStringRequest:QueryStringRequest;
		
		[Inject( "communityManager.selectedCommunities", bind = "true" )]
		/**
		 * The collection of selected communities
		 */
		public var selectedCommunities:ArrayCollection;
		
		//======================================
		// protected properties 
		//======================================
		
		/**
		 *
		 * @default
		 */
		protected var numberFormatter:NumberFormatter = new NumberFormatter();
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Clear Widget Filters
		 */
		public function clearWidgetFilters():void
		{
			dispatcher.dispatchEvent( new WidgetEvent( WidgetEvent.CLEAR_WIDGET_FILTERS ) );
		}
		
		/**
		 * Export JSON
		 */
		public function exportJSON():void
		{
			if ( !queryResultString )
				return;
			
			var fileRef:FileReference = new FileReference();
			var tmpManager:WidgetModuleManagerCopy = new WidgetModuleManagerCopy();
			var tmpQueryResult:Object = new Object();
			var tmpResultSet:IResultSet;
			var results:String;
			var toOutput:Object = new Object();
			
			tmpManager.setCurrentQueryString( currentQueryStringRequest );
			
			// deserialize json string to Object
			tmpQueryResult = JSONUtil.decode( queryResultString );
			
			tmpManager.setQueryResult( tmpQueryResult );
			
			tmpResultSet = tmpManager.context.getQuery_AllResults();
			
			toOutput[ ExportConstants.DOCUMENTS ] = tmpResultSet.getTopDocuments() == null ? null : tmpResultSet.getTopDocuments().source;
			toOutput[ ExportConstants.ENTITIES ] = tmpResultSet.getEntities() == null ? null : tmpResultSet.getEntities().source;
			toOutput[ ExportConstants.EVENTS ] = tmpResultSet.getEvents() == null ? null : tmpResultSet.getEvents().source;
			toOutput[ ExportConstants.FACTS ] = tmpResultSet.getFacts() == null ? null : tmpResultSet.getFacts().source;
			toOutput[ ExportConstants.EVENTS_TIMELINE ] = tmpResultSet.getEventsTimeline() == null ? null : tmpResultSet.getEventsTimeline().source;
			toOutput[ ExportConstants.GEO ] = tmpResultSet.getGeoCounts() == null ? null : tmpResultSet.getGeoCounts().source;
			toOutput[ ExportConstants.TIMES ] = tmpResultSet.getTimeCounts() == null ? null : tmpResultSet.getTimeCounts().source;
			toOutput[ ExportConstants.SOURCES ] = tmpResultSet.getSourceKeyCounts() == null ? null : tmpResultSet.getSourceKeyCounts().source;
			toOutput[ ExportConstants.SOURCES_META_TAGS ] = tmpResultSet.getSourceTagCounts() == null ? null : tmpResultSet.getSourceTagCounts().source;
			toOutput[ ExportConstants.SOURCES_META_TYPES ] = tmpResultSet.getSourceTypeCounts() == null ? null : tmpResultSet.getSourceTypeCounts().source;
			
			results = JSONUtil.encode( toOutput );
			
			fileRef.save( JSONUtil.formatJson( results ), ExportConstants.JSON_FILENAME );
		}
		
		/**
		 * Export PDF
		 */
		public function exportPDF():void
		{
			dispatcher.dispatchEvent( new WidgetEvent( WidgetEvent.EXPORT_PDF ) );
		}
		
		/**
		 * Export RSS
		 */
		public function exportRSS():void
		{
			var queryStringRequest:QueryStringRequest = lastQueryStringRequest.clone();
			
			queryStringRequest.output.format = ExportConstants.RSS;
			
			var urlString:String = ServiceConstants.QUERY_URL + CollectionUtil.getStringFromArrayCollectionField( selectedCommunities );;
			var json:String = JSONUtil.encode( QueryUtil.getQueryStringObject( queryStringRequest ) );
			var encJson:String = ExportConstants.JSON_ENCODE + ServiceUtil.urlEncode( json );
			
			// This url is only going to be allowed for this query 
			var hashed:String = PasswordUtil.hashPassword( json );
			var key:String = ExportConstants.KEY_ENCODE + ServiceUtil.urlEncode( hashed );
			
			var url:URLRequest = new URLRequest( urlString + encJson + key );
			navigateToURL( url, ServiceConstants.BLANK_URL );
		}
		
		/**
		 * Maximize the next widget
		 * @param widget
		 */
		public function maximizeNextWidget():void
		{
			if ( maximized && selectedWidgetsSorted && maximizedWidget )
			{
				var index:int = selectedWidgetsSorted.getItemIndex( maximizedWidget );
				
				if ( index == selectedWidgetsSorted.length - 1 )
					maximizeWidget( selectedWidgetsSorted.getItemAt( 0 ) as Widget );
				else
					maximizeWidget( selectedWidgetsSorted.getItemAt( index + 1 ) as Widget );
			}
		}
		
		/**
		 * Maximize the previous widget
		 * @param widget
		 */
		public function maximizePreviousWidget():void
		{
			if ( maximized && selectedWidgetsSorted && maximizedWidget )
			{
				var index:int = selectedWidgetsSorted.getItemIndex( maximizedWidget );
				
				if ( index == 0 )
					maximizeWidget( selectedWidgetsSorted.getItemAt( selectedWidgetsSorted.length - 1 ) as Widget );
				else
					maximizeWidget( selectedWidgetsSorted.getItemAt( index - 1 ) as Widget );
			}
		}
		
		/**
		 * Select a new widget to display maximized
		 * @param widget
		 */
		public function maximizeWidget( widget:Widget ):void
		{
			dispatcher.dispatchEvent( new WorkspaceEvent( WorkspaceEvent.MAXIMIZE_WIDGET, widget ) );
		}
		
		/**
		 * Message for the busy indicator label
		 * @param value
		 */
		[Inject( "sessionManager.busyIndicatorMessage", bind = "true" )]
		/**
		 *
		 * @param value
		 */
		public function setBusyIndicatorMessage( value:String ):void
		{
			if ( value && value != Constants.BLANK )
			{
				busyIndicatorActive = true;
				filterActive = false;
				
				if ( value == ServiceConstants.SERVICE_FAULT && busyIndicatorMessage )
				{
					busyIndicatorMessage = null;
					resultsCountMessage = ResourceManager.getInstance().getString( 'infinite', 'workspacesHeader.searchError' );
					setShowResultsCount( true );
					return;
				}
				else
				{
					setShowResultsCount( false );
					resultsCountMessage = Constants.BLANK;
				}
			}
			else
			{
				if ( busyIndicatorActive )
				{
					resultsCountMessage = ResourceManager.getInstance().getString( 'infinite', 'workspacesHeader.translatingSearchResults' );
					setShowResultsCount( true );
				}
			}
			
			busyIndicatorMessage = value;
		}
		
		/**
		 * Set the query statistics
		 * @param value
		 */
		[Inject( "queryManager.queryStatistics", bind = "true" )]
		/**
		 *
		 * @param value
		 */
		public function setQueryStatistics( value:ServiceStatistics ):void
		{
			queryStatistics = value;
			
			numberFormatter.fractionalDigits = 0;
			
			if ( value )
			{
				if ( isNaN( value.avgScore ) )
				{
					resultsCountMessage = Constants.BLANK;
				}
				else
				{
					resultsCountMessage = ResourceManager.getInstance().getString( 'infinite', 'workspacesHeader.resultsCount', [ numberFormatter.format( queryStatistics.found ) ] );
				}
			}
			else
			{
				resultsCountMessage = ResourceManager.getInstance().getString( 'infinite', 'workspacesHeaderModel.runInitialQuery' );
			}
			
			setShowResultsCount( resultsCountMessage != Constants.BLANK );
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 *
		 * @param value
		 */
		protected function setShowResultsCount( value:Boolean ):void
		{
			showResultsCount = value;
		}
	}
}

