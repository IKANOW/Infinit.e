package com.ikanow.infinit.e.source.model.manager
{
	import com.adobe.utils.StringUtil;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.Community;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.Source;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.source.model.constant.SourceConstants;
	import flash.events.Event;
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	import mx.collections.SortField;
	
	/**
	 * Source Manager
	 */
	public class SourceManager extends InfiniteManager
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * The sources master collection
		 */
		public var sourcesMaster:ArrayCollection;
		
		[Bindable]
		/**
		 * The sources collection in the selected communities
		 */
		public var sources:ArrayCollection;
		
		//======================================
		// protected properties 
		//======================================
		
		/**
		 * The current query string that is being modified for a new query
		 */
		protected var currentQueryStringRequest:QueryStringRequest;
		
		/**
		 * The collection of communities
		 */
		protected var communities:ArrayCollection;
		
		/**
		 * The collection of selected communities
		 */
		protected var selectedCommunities:ArrayCollection;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Reset
		 * Used to reset on logout
		 */
		public function reset():void
		{
			sources = null;
			sourcesMaster = null;
		}
		
		/**
		 * Public Communities Collection
		 * @param value
		 */
		[Inject( "communityManager.communities", bind = "true" )]
		public function setCommunities( value:ArrayCollection ):void
		{
			communities = value;
			
			if ( value && sourcesMaster )
			{
				updateSourceCommunityNames();
				updateSourcesFromQueryString();
			}
		}
		
		[Inject( "queryManager.currentQueryStringRequest", bind = "true" )]
		/**
		 * set the current query string
		 * @param value
		 */
		public function setCurrentQueryString( value:QueryStringRequest ):void
		{
			currentQueryStringRequest = value;
			
			if ( value && sourcesMaster )
			{
				updateSourcesFromQueryString();
			}
		}
		
		/**
		 * Public Selected Communities Collection
		 * @param value
		 */
		[Inject( "communityManager.selectedCommunities", bind = "true" )]
		public function setSelectedCommunities( value:ArrayCollection ):void
		{
			selectedCommunities = value;
			
			if ( value && sourcesMaster )
			{
				updateSourcesFromQueryString();
			}
		}
		
		/**
		 * Sources Master Collection
		 * @param value
		 */
		public function setSources( value:ArrayCollection ):void
		{
			if ( sourcesMaster == null )
			{
				sourcesMaster = value;
				updateSourcesFromQueryString();
			}
			else
			{
				refreshSources( value );
			}
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Determines if the source is contained in the selected communities
		 * @param source
		 * @return Boolean
		 */
		protected function isSelectedCommunitySource( source:Source ):Boolean
		{
			var includeFlag:Boolean;
			
			if ( selectedCommunities )
			{
				for each ( var community:Community in selectedCommunities )
				{
					var communityIds:ArrayCollection = source.communityIds;
					
					for each ( var communityID:String in communityIds )
					{
						if ( communityID == community._id )
						{
							includeFlag = true;
						}
					}
				}
			}
			
			return includeFlag;
		}
		
		
		/**
		 * Determines if the source is contained in the setup queryString source keys
		 * @param source
		 * @return Boolean
		 */
		protected function isSelectedSource( source:Source ):Boolean
		{
			var includeFlag:Boolean = true;
			
			// if no source keys are returned in the current query string, default all sources to current
			if ( currentQueryStringRequest == null || currentQueryStringRequest.input == null || currentQueryStringRequest.input.sources == null )
				return true;
			
			// check to see if the last query included or excluded sources
			if ( currentQueryStringRequest.input[ QueryConstants.SRC_INCLUDE ] != null )
				includeFlag = currentQueryStringRequest.input[ QueryConstants.SRC_INCLUDE ];
			
			// include or exclude if found in the input sources
			for each ( var key:String in currentQueryStringRequest.input.sources )
			{
				if ( source.key == key )
					return includeFlag;
			}
			
			return !includeFlag;
		}
		
		/**
		 * Adds new sources to the collections
		 */
		protected function refreshSources( value:ArrayCollection ):void
		{
			var newSources:ArrayCollection = new ArrayCollection();
			
			for each ( var source:Source in value )
			{
				source.title = StringUtil.ltrim( source.title );
				
				if ( !CollectionUtil.doesCollectionContainItem( sourcesMaster, source ) )
				{
					newSources.addItem( source );
				}
			}
			
			sourcesMaster.addAll( newSources );
			sourcesMaster.refresh();
			
			updateSourcesFromQueryString();
		}
		
		/**
		 * Updates community name property for the sources
		 */
		protected function updateSourceCommunityNames():void
		{
			var communityNames:String;
			
			if ( sourcesMaster && communities )
			{
				for each ( var source:Source in sourcesMaster )
				{
					var communityIds:ArrayCollection = source.communityIds;
					
					communityNames = Constants.BLANK;
					
					for each ( var communityId:String in communityIds )
					{
						for each ( var community:Community in communities )
						{
							if ( community._id == communityId )
							{
								if ( communityNames != Constants.BLANK )
								{
									communityNames += Constants.COMMA;
									communityNames += Constants.BLANK;
								}
								
								communityNames += community.name;
							}
						}
					}
					
					source.community = communityNames;
				}
			}
		}
		
		/**
		 * Updates the sources using the source keys from the current queryString
		 */
		protected function updateSourcesFromQueryString():void
		{
			var sourcesNew:ArrayCollection = new ArrayCollection();
			
			// update the source community names
			updateSourceCommunityNames();
			
			if ( sourcesMaster )
			{
				for each ( var source:Source in sourcesMaster )
				{
					if ( source.title )
						source.title = StringUtil.ltrim( source.title );
					else
						source.title = Constants.BLANK;
					
					// check if source is in selected communities and selected
					if ( isSelectedCommunitySource( source ) )
					{
						source.selected = isSelectedSource( source );
						sourcesNew.addItem( source );
					}
					else
					{
						source.selected = false;
					}
				}
			}
			
			// sort by title
			CollectionUtil.applySort( sourcesNew, [ new SortField( SourceConstants.FIELD_TITLE, true ), new SortField( SourceConstants.FIELD_TAGS_STRING, true ) ] );
			sources = sourcesNew;
		}
	}
}
