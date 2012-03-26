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
package com.ikanow.infinit.e.widget.model.presentation.list
{
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.WidgetConstants;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import com.ikanow.infinit.e.shared.util.FilterUtil;
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	import mx.collections.ListCollectionView;
	import spark.collections.Sort;
	import spark.collections.SortField;
	
	/**
	 *  Widget List Presentation Model
	 */
	public class WidgetListModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:WidgetListNavigator;
		
		private var _widgets:ListCollectionView;
		
		[Bindable( event = "widgetsChange" )]
		public function get widgets():ListCollectionView
		{
			return _widgets;
		}
		
		public function set widgets( value:ListCollectionView ):void
		{
			_widgets = value;
		}
		
		[Bindable]
		public var widgetSummaries:ArrayCollection;
		
		[Bindable]
		public var searchTerm:String;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function clearSearch():void
		{
			searchTerm = Constants.BLANK;
			widgets.filterFunction = widgetIsNotSelected;
			widgets.refresh();
		}
		
		public function searchWidgets( searchTerm:String ):void
		{
			this.searchTerm = searchTerm;
			
			widgets.filterFunction = function( item:Object ):Boolean
			{
				return widgetIsNotSelected( item ) && widgetMatchesSearchTerm( item );
			}
			widgets.refresh();
		}
		
		/**
		 * Set the widget summaries
		 * @param value
		 */
		[Inject( "widgetManager.widgetSummaries", bind = "true" )]
		public function setWidgetSummaries( value:ArrayCollection ):void
		{
			widgetSummaries = value
		}
		
		/**
		 * Set the widgets
		 * @param value
		 */
		[Inject( "widgetManager.widgets", bind = "true" )]
		public function setWidgets( value:ArrayCollection ):void
		{
			var lcv:ListCollectionView = new ListCollectionView( value );
			lcv.filterFunction = widgetIsNotSelected;
			var sortOrderSortField:SortField = new SortField();
			sortOrderSortField.name = Constants.SORT_ORDER_PROPERTY;
			sortOrderSortField.numeric = true;
			sortOrderSortField.descending = false;
			lcv.sort = new Sort();
			lcv.sort.fields = [ sortOrderSortField, new SortField( WidgetConstants.TITLE ) ];
			lcv.refresh();
			
			widgets = lcv;
			
			dispatchEvent( new Event( "widgetsChange" ) );
		}
		
		//======================================
		// private methods 
		//======================================
		
		private function widgetIsNotSelected( item:Object ):Boolean
		{
			return !item.selected;
		}
		
		private function widgetMatchesSearchTerm( item:Object ):Boolean
		{
			var widget:Widget = Widget( item );
			
			return FilterUtil.checkAllSearchTerms( searchTerm, [ widget.title ].concat( widget.searchterms.toArray() ) );
		}
	}
}

