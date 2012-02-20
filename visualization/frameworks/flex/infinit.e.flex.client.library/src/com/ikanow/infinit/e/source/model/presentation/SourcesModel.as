package com.ikanow.infinit.e.source.model.presentation
{
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.event.SourceEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryOperatorTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.Setup;
	import com.ikanow.infinit.e.shared.model.vo.Source;
	import com.ikanow.infinit.e.shared.model.vo.ui.ColumnSelector;
	import com.ikanow.infinit.e.shared.model.vo.ui.ColumnSelectorItem;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.shared.util.FilterUtil;
	import com.ikanow.infinit.e.source.model.constant.SourceConstants;
	import flash.events.Event;
	import flash.utils.Dictionary;
	import mx.collections.ArrayCollection;
	import mx.collections.SortField;
	import mx.events.CollectionEvent;
	import mx.resources.ResourceManager;
	import mx.utils.ObjectUtil;
	import mx.utils.StringUtil;
	import spark.components.gridClasses.GridColumn;
	
	/**
	 *  Sources Presentation Model
	 */
	public class SourcesModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:SourcesNavigator;
		
		/**
		 * The sources master collection
		 */
		public var sourcesMaster:ArrayCollection;
		
		[Bindable]
		/**
		 * The sources collection
		 */
		public var sources:ArrayCollection;
		
		[Bindable]
		/**
		 * The search tem used for fitering the sources
		 */
		public var searchTerm:String = Constants.BLANK;
		
		[Bindable]
		/**
		 * Filtered Count String
		 */
		public var filteredCountString:String;
		
		[Bindable]
		/**
		 * The number of selected sources
		 */
		public var selectedSourcesCount:int;
		
		[Bindable]
		/**
		 * The selectable columns used for filtering
		 */
		public var selectableColumns:ArrayCollection = SourceConstants.getSelectableColumns( true );
		
		/**
		 * Array of data grid columns
		 */
		public var columns:Array;
		
		/**
		 * A dictionary of the communities
		 */
		public var communitiesDictionary:Dictionary;
		
		/**
		 * A dictionary of the media types
		 */
		public var mediaTypesDictionary:Dictionary;
		
		/**
		 * A dictionary of the source titles
		 */
		public var titlesDictionary:Dictionary;
		
		/**
		 * A dictionary of the tags
		 */
		public var tagsDictionary:Dictionary;
		
		//======================================
		// protected properties 
		//======================================
		
		/**
		 * Filter the sources to show only the selected sources
		 */
		protected var showSelectedSourcesOnly:Boolean;
		
		/**
		 * Filter columns
		 */
		protected var filterColumns:Array;
		
		/**
		 * Description Sort Field
		 */
		protected var descriptionSortField:SortField = new SortField( SourceConstants.FIELD_DESCRIPTION );
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Clear sources search
		 */
		public function clearSourcesSearch():void
		{
			setSearchTerm( Constants.BLANK );
			
			refreshSources();
		}
		
		/**
		 * Run the advanced query
		 */
		public function runAdvancedQuery():void
		{
			dispatcher.dispatchEvent( new QueryEvent( QueryEvent.RUN_ADVANCED_QUERY ) );
		}
		
		/**
		 * Search for column selector item
		 */
		public function searchForColumnSelectorItem( columnSelectorItem:ColumnSelectorItem ):void
		{
			var columnSelector:ColumnSelector = CollectionUtil.getItemById( selectableColumns, columnSelectorItem.type ) as ColumnSelector;
			
			clearSelectedColumns();
			
			toggleColumnSelected( columnSelector );
			
			setSearchTerm( columnSelectorItem.description );
		}
		
		/**
		 * Search sources
		 */
		public function searchSources( term:String ):void
		{
			setSearchTerm( term );
		}
		
		/**
		 * Set all sources as selected
		 */
		public function selectAllSources():void
		{
			CollectionUtil.setAllSelected( sources, true );
			
			refreshSources();
		}
		
		/**
		 * Set all sources as not selected
		 */
		public function selectNoneSources():void
		{
			CollectionUtil.setAllSelected( sources, false );
			
			refreshSources();
		}
		
		/**
		 * Method for setting columns
		 * @param columns - an array of DataGridColumns
		 */
		public function setColumns( cols:Array ):void
		{
			columns = cols;
			
			updateFilterColumns();
		}
		
		/**
		 * Setter method for setting the current searchTerm
		 * @param value - string to search for
		 */
		public function setSearchTerm( value:String ):void
		{
			searchTerm = value;
			
			if ( sources )
				sources.refresh();
			
			refreshSources();
		}
		
		/**
		 * Sources
		 * @param value
		 */
		[Inject( "sourceManager.sources", bind = "true" )]
		public function setSources( value:ArrayCollection ):void
		{
			sources = value;
			
			addFilterFunction();
			
			refreshSources();
			
			// create the column selector collections
			createColumnSelectorCollections();
		}
		
		/**
		 * Show Available Sources
		 */
		public function showAvailableSources():void
		{
			showSelectedSourcesOnly = false;
			
			refreshSources();
		}
		
		/**
		 * Show Selected Sources
		 */
		public function showSelectedSources():void
		{
			showSelectedSourcesOnly = true;
			
			clearSourcesSearch();
			
			refreshSources();
		}
		
		/**
		 * Toggle column selected
		 */
		public function toggleColumnSelected( columnSelector:ColumnSelector ):void
		{
			var columnsSelectedCount:int = CollectionUtil.getSelectedCount( selectableColumns );
			
			if ( columnSelector.selected && columnsSelectedCount == selectableColumns.length )
			{
				clearSelectedColumns();
				columnSelector.selected = true;
			}
			else
			{
				columnSelector.selected = !columnSelector.selected;
			}
			
			columnsSelectedCount = CollectionUtil.getSelectedCount( selectableColumns );
			
			if ( columnsSelectedCount == 0 )
				selectAllColumns();
			
			updateFilterColumns();
			
			refreshSources();
		}
		
		/**
		 * Toggle source selected
		 */
		public function toggleSourceSelected( rowIndex:int ):void
		{
			var source:Source = sources.getItemAt( rowIndex ) as Source;
			source.selected = !source.selected;
			
			refreshSources( false );
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Adds the filter function
		 */
		protected function addFilterFunction():void
		{
			if ( sources )
			{
				sources.filterFunction = sourcesFilterFunction;
				sources.refresh();
			}
		}
		
		/**
		 * Clear the selected columns
		 */
		protected function clearSelectedColumns():void
		{
			for each ( var columnSelector:ColumnSelector in selectableColumns )
			{
				columnSelector.selected = false;
			}
		}
		
		/**
		 * Create the item collections for the column selectors
		 */
		protected function createColumnSelectorCollections():void
		{
			communitiesDictionary = new Dictionary( true );
			mediaTypesDictionary = new Dictionary( true );
			titlesDictionary = new Dictionary( true );
			tagsDictionary = new Dictionary( true );
			
			if ( sources )
			{
				for each ( var source:Source in sources.source )
				{
					// communities
					if ( source.community && source.community != "" )
					{
						if ( communitiesDictionary[ source.community ] == null )
							communitiesDictionary[ source.community ] = new ColumnSelectorItem( SourceConstants.FIELD_COMMUNITY, source.community, source.community, 1 );
						else
							ColumnSelectorItem( communitiesDictionary[ source.community ] ).count++;
					}
					
					// media types
					if ( source.mediaType && source.mediaType != "" )
					{
						if ( mediaTypesDictionary[ source.mediaType ] == null )
							mediaTypesDictionary[ source.mediaType ] = new ColumnSelectorItem( SourceConstants.FIELD_MEDIA_TYPE, source.mediaType, source.mediaType, 1 );
						else
							ColumnSelectorItem( mediaTypesDictionary[ source.mediaType ] ).count++;
					}
					
					// titles
					if ( source.title && source.title != "" )
					{
						if ( titlesDictionary[ source.title ] == null )
							titlesDictionary[ source.title ] = new ColumnSelectorItem( SourceConstants.FIELD_TITLE, source._id, source.title, 1 );
						else
							ColumnSelectorItem( titlesDictionary[ source.title ] ).count++;
					}
					
					// tags
					if ( source.tags && source.tags.length > 0 )
					{
						for each ( var tag:String in source.tags )
						{
							tag = StringUtil.trim( tag.toLowerCase() );
							
							if ( tagsDictionary[ tag ] == null )
								tagsDictionary[ tag ] = new ColumnSelectorItem( SourceConstants.FIELD_TAG, tag, tag, 1 );
							else
								ColumnSelectorItem( tagsDictionary[ tag ] ).count++;
						}
					}
				}
				
				// update the column selector dataproviders
				updateColumnSelectorDataProviders();
			}
		}
		
		/**
		 * Get the column selector dataprovider and apply sort and filter function
		 */
		protected function getColumnSelectorDataProvider( dictionary:Dictionary ):ArrayCollection
		{
			var columnSelectorDataProvider:ArrayCollection = new ArrayCollection();
			
			for each ( var columnsSelectorItem:ColumnSelectorItem in dictionary )
			{
				columnSelectorDataProvider.addItem( columnsSelectorItem );
			}
			
			CollectionUtil.applySort( columnSelectorDataProvider, [ descriptionSortField ] );
			
			columnSelectorDataProvider.refresh();
			
			return columnSelectorDataProvider;
		}
		
		/**
		 * Get filter function columns
		 * Return an array of data grid columns that have been
		 * selected for filtering
		 * @return
		 */
		protected function getFilterFunctionColumns( filterType:String = null ):Array
		{
			var filterFunctionColumnsNew:Array = [];
			var columnsSelectedCount:int = CollectionUtil.getSelectedCount( selectableColumns );
			
			for each ( var column:GridColumn in columns )
			{
				for each ( var columnSelector:ColumnSelector in selectableColumns )
				{
					if ( filterType )
					{
						if ( columnSelector.filterType == filterType && columnSelector.selected && columnSelector.dataField == column.dataField )
							filterFunctionColumnsNew.push( column );
					}
					else
					{
						if ( columnSelector.selected && columnSelector.dataField == column.dataField )
							filterFunctionColumnsNew.push( column );
					}
				}
			}
			
			return filterFunctionColumnsNew;
		}
		
		/**
		 * Refresh the column totals
		 */
		protected function refreshColumnSelectorItemCounts():void
		{
			for each ( var columnSelector:ColumnSelector in selectableColumns )
			{
				columnSelector.dataProvider.refresh();
			}
		}
		
		/**
		 * Refresh Sources
		 */
		protected function refreshSources( refresh:Boolean = true ):void
		{
			if ( sources && refresh )
				sources.refresh();
			
			// update the selected count
			updateSelectedSourcesCount();
			
			// set the filtered source count string
			setFilteredCountString();
		}
		/**
		 * Select all of the Columns
		 */
		protected function selectAllColumns():void
		{
			for each ( var columnSelector:ColumnSelector in selectableColumns )
			{
				columnSelector.selected = true;
			}
		}
		
		/**
		 * Set the filtered count string
		 */
		protected function setFilteredCountString():void
		{
			if ( sources && sources.length != sources.source.length && searchTerm && searchTerm != Constants.BLANK )
				filteredCountString = Constants.PARENTHESIS_LEFT + sources.length + Constants.PARENTHESIS_RIGHT;
			else
				filteredCountString = Constants.BLANK;
		}
		
		/**
		 * Sources filter function
		 * Used for searching the sources
		 * @param item
		 * @return
		 */
		protected function sourcesFilterFunction( item:Object ):Boolean
		{
			var searchFound:Boolean = false;
			var andSearchFound:Boolean = false;
			var orSearchFound:Boolean = false;
			var andFilterColumns:Array = getFilterFunctionColumns( QueryOperatorTypes.AND );
			var orFilterColumns:Array = getFilterFunctionColumns( QueryOperatorTypes.OR );
			
			// if showing only selected sources and the source is not selected, return false
			if ( showSelectedSourcesOnly && !item.selected )
				return false;
			
			// source data grid columns contain the search term
			if ( searchTerm == null || searchTerm.length == 0 )
			{
				searchFound = true;
			}
			else
			{
				if ( andFilterColumns.length > 0 )
					andSearchFound = FilterUtil.checkAllSearchTerms( searchTerm, FilterUtil.buildSearchValuesArrayForGridColumns( item, andFilterColumns ) );
				
				if ( orFilterColumns.length > 0 )
					orSearchFound = FilterUtil.checkAnySearchTerms( searchTerm, FilterUtil.buildSearchValuesArrayForGridColumns( item, orFilterColumns ) );
				
				searchFound = andSearchFound || orSearchFound;
			}
			
			return searchFound;
		}
		
		/**
		 * Update the column selectors dataproviders
		 */
		protected function updateColumnSelectorDataProviders():void
		{
			if ( selectableColumns && communitiesDictionary && mediaTypesDictionary && tagsDictionary && titlesDictionary )
			{
				for each ( var columnSelector:ColumnSelector in selectableColumns )
				{
					switch ( columnSelector.dataField )
					{
						case SourceConstants.FIELD_COMMUNITY:
							columnSelector.dataProvider = getColumnSelectorDataProvider( communitiesDictionary );
							break;
						case SourceConstants.FIELD_MEDIA_TYPE:
							columnSelector.dataProvider = getColumnSelectorDataProvider( mediaTypesDictionary );
							break;
						case SourceConstants.FIELD_TAGS_STRING:
							columnSelector.dataProvider = getColumnSelectorDataProvider( tagsDictionary );
							break;
						case SourceConstants.FIELD_TITLE:
							columnSelector.dataProvider = getColumnSelectorDataProvider( titlesDictionary );
							break;
					}
				}
			}
		}
		
		/**
		 * Update filter columns
		 */
		protected function updateFilterColumns():void
		{
			filterColumns = getFilterFunctionColumns();
		}
		
		/**
		 * Update the count of selected sources
		 */
		protected function updateSelectedSourcesCount( event:CollectionEvent = null ):void
		{
			if ( sources )
				selectedSourcesCount = CollectionUtil.getSelectedCount( new ArrayCollection( sources.source ) );
			
			if ( navigator )
				navigator.updateSourcesCounts();
		}
	}
}

