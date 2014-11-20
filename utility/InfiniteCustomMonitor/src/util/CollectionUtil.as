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
package util
{
	import mx.collections.ArrayCollection;
	import mx.collections.IList;
	import mx.collections.ListCollectionView;
	import mx.logging.ILogger;
	import mx.logging.Log;
	import mx.utils.ObjectUtil;
	import spark.collections.Sort;
	import spark.collections.SortField;
	import util.Constants;
	import util.ISelectable;
	
	public class CollectionUtil
	{
		
		//======================================
		// protected static properties 
		//======================================
		
		protected static var logger:ILogger = Log.getLogger( "CollectionUtil" );
		
		
		//======================================
		// public static methods 
		//======================================
		
		public static function addItemAt( collection:IList, item:Object, index:int ):void
		{
			if ( collection == null || item == null )
				return;
			
			collection.addItemAt( item, index );
		
		}
		
		public static function addItemIfUnique( collection:IList, newItem:Object, idField:String = "_id" ):void
		{
			if ( idField != null &&
				!doesCollectionContainItem( collection, newItem, idField ) &&
				collection != null &&
				newItem != null )
			{
				collection.addItem( newItem );
				logger.info( "addItemIfUnique - Item was added to collection" );
			}
			logger.info( "addItemIfUnique - Matching item found.  NOT added to collection" );
		}
		
		/**
		 *
		 */
		public static function appendCollectionWithArray( collection:IList, newValues:Array ):void
		{
			if ( collection is ArrayCollection )
			{
				ArrayCollection( collection ).source = ArrayCollection( collection ).source.concat( newValues );
			}
			else if ( collection != null )
			{
				for each ( var newValue:Object in newValues )
				{
					collection.addItem( newValue );
				}
			}
		}
		
		/**
		 * Apply a sort to an array collection
		 *
		 * @param collection ArrayCollection to sort
		 * @param sortFields Array of SortFields to apply
		 *
		 */
		public static function applySort( collection:ArrayCollection, sortFields:Array ):void
		{
			if ( collection == null || sortFields == null || sortFields.length == 0 )
				return;
			
			if ( sortFields[ 0 ] is SortField )
			{
				var sort:Sort = new Sort();
				sort.fields = sortFields;
				collection.sort = sort;
				collection.refresh();
			}
		}
		
		
		public static function doesCollectionContainItem( collection:IList, item:Object, idField:String = "_id" ):Boolean
		{
			var currentItem:Object;
			
			if ( collection && item )
			{
				for ( var i:uint = 0; i < collection.length; i++ )
				{
					currentItem = collection.getItemAt( i );
					
					if ( currentItem.hasOwnProperty( idField ) && item.hasOwnProperty( idField ) && currentItem[ idField ] == item[ idField ] )
					{
						return true;
					}
				}
			}
			return false;
		}
		
		/**
		 *
		 */
		public static function getArrayCollectionFromString( collectionString:String, delimeter:String = "," ):ArrayCollection
		{
			var collection:ArrayCollection = new ArrayCollection();
			var collectionArray:Array = collectionString.split( delimeter );
			
			if ( collectionArray )
				collection.source = collectionArray;
			
			return collection;
		}
		
		/**
		 *
		 */
		public static function getArrayFromString( collectionString:String, delimeter:String = "," ):Array
		{
			return collectionString.split( delimeter );
		}
		
		/**
		 *
		 */
		public static function getItemById( collection:IList, id:Object, idField:String = "_id" ):Object
		{
			var currentItem:Object;
			
			if ( collection )
			{
				var len:int = collection.length;
				
				for ( var i:uint = 0; i < len; i++ )
				{
					currentItem = collection.getItemAt( i );
					
					if ( currentItem.hasOwnProperty( idField ) && String( currentItem[ idField ] ) == String( id ) )
					{
						return currentItem;
					}
				}
			}
			return null;
		}
		
		public static function getItemIndex( collection:IList, item:Object, idField:String = "_id" ):int
		{
			if ( item != null && collection != null )
			{
				var len:int = collection.length;
				
				for ( var i:int = 0; i < len; i++ )
				{
					var collectionItem:Object = collection.getItemAt( i );
					
					if ( collectionItem != null && item.hasOwnProperty( idField ) && collectionItem.hasOwnProperty( idField ) && item[ idField ] == collectionItem[ idField ] )
					{
						return i;
					}
				}
			}
			
			return -1;
		}
		
		public static function getMostRecentValue( collection:IList, dateField:String = "timestamp" ):Object
		{
			var view:ListCollectionView = new ListCollectionView( collection );
			
			if ( view.length )
			{
				var sortField:SortField = new SortField( dateField );
				sortField.descending = true;
				var sort:Sort = new Sort();
				sort.fields = [ sortField ];
				view.sort = sort;
				view.refresh();
				
				return view.getItemAt( 0 );
			}
			
			return null;
		}
		
		public static function getSelectedCount( collection:IList ):int
		{
			var count:int;
			
			for each ( var item:ISelectable in collection )
			{
				if ( ISelectable( item ).selected )
				{
					count += 1;
				}
			}
			
			return count;
		}
		
		public static function getSelectedItems( collection:IList, selected:Boolean = true ):ArrayCollection
		{
			var selectedItems:ArrayCollection = new ArrayCollection();
			
			for each ( var item:ISelectable in collection )
			{
				if ( ISelectable( item ).selected == selected )
				{
					selectedItems.addItem( item );
				}
			}
			
			return selectedItems;
		}
		
		/**
		 *
		 */
		public static function getStringFromArray( array:Array, delimeter:String = "," ):String
		{
			return array.join( delimeter );
		}
		
		/**
		 *
		 */
		public static function getStringFromArrayCollectionField( collection:ArrayCollection, collectionField:String = "_id", delimeter:String = "," ):String
		{
			var collectionFieldString:String = "";
			var currentItem:Object;
			
			if ( collection )
			{
				var len:int = collection.length;
				
				for ( var i:uint = 0; i < len; i++ )
				{
					if ( collectionFieldString != "" )
						collectionFieldString += delimeter;
					
					currentItem = collection.getItemAt( i );
					
					if ( currentItem.hasOwnProperty( collectionField ) )
					{
						collectionFieldString += currentItem[ collectionField ].toString();
					}
				}
			}
			
			return collectionFieldString;
		}
		
		public static function removeChildCollectionItemById( collection:IList, childCollection:String, id:int, idField:String = "_id" ):void
		{
			for each ( var item:Object in collection )
			{
				if ( childCollection != null && idField != null )
				{
					var len:int = item[ childCollection ].length;
					
					for ( var i:uint = 0; i < len; i++ )
					{
						var childItem:Object = ( item[ childCollection ] as IList ).getItemAt( i );
						
						if ( childItem[ idField ] == id )
						{
							item[ childCollection ].removeItemAt( i );
							logger.info( "removeChildCollectionItemById - item removed - idField - " + idField );
							return;
						}
					}
				}
			}
			
			logger.info( "removeChildCollectionItemById - item not found" );
			return;
		}
		
		public static function removeChildCollectionItemsById( collection:IList, childCollection:String, id:int, idField:String = "_id" ):void
		{
			var itemsFound:Boolean = false;
			
			for each ( var item:Object in collection )
			{
				if ( item[ childCollection ] )
				{
					var len:int = item[ childCollection ].length;
					
					for ( var i:int = len - 1; i > -1; i-- )
					{
						var childItem:Object = ( item[ childCollection ] as IList ).getItemAt( i );
						
						if ( childItem[ idField ] == id )
						{
							item[ childCollection ].removeItemAt( i );
							logger.info( "removeChildCollectionItemById - item removed - idField - " + idField );
							itemsFound = true;
						}
					}
				}
			}
			
			if ( !itemsFound )
				logger.info( "removeChildCollectionItemById - item not found" );
		}
		
		public static function removeItem( collection:IList, item:Object ):void
		{
			//var index:int = collection.getItemIndex( item );
			//collection.removeItemAt( index );
			var currentItem:Object;
			
			if ( collection && item )
			{
				var len:int = collection.length;
				
				for ( var i:int = 0; i < len; i++ )
				{
					currentItem = collection.getItemAt( i );
					
					if ( currentItem.hasOwnProperty( Constants.DEFAULT_ID_PROPERTY ) && item.hasOwnProperty( Constants.DEFAULT_ID_PROPERTY ) && currentItem._id == item._id )
					{
						collection.removeItemAt( i );
						break;
					}
				}
			}
		}
		
		/**
		 * This method removes an item from a collection based on its ID value.  In this case,
		 * it finds the value that has the same ID as the item argument and removes it from the
		 * collection.  You can also define the idfield for the collection.
		 *
		 * @param collection The collection of values that contains the item to be removed
		 * @param id The id of the item you want to remove
		 * @param idField The name of the idField both on the item and the collection items
		 */
		public static function removeItemById( collection:IList, id:*, idField:String = "_id" ):void
		{
			var currentItem:Object;
			
			if ( collection )
			{
				var len:int = collection.length;
				
				for ( var i:uint = 0; i < len; i++ )
				{
					currentItem = collection.getItemAt( i );
					
					if ( currentItem && currentItem.hasOwnProperty( idField ) && currentItem[ idField ] == id )
					{
						collection.removeItemAt( i );
						logger.info( "removeItemById - item removed - idField - " + idField );
						return;
					}
				}
			}
			
			logger.info( "removeItemById - item not found" );
			return;
		}
		
		/**
		 * This method takes a array of items to be removed and removes them from the provided
		 * collection.
		 *
		 * @param collection The collection of values that contains the items to be removed
		 * @param items The list of items to remove
		 * @param idField The id field to match each item
		 *
		 */
		public static function removeItemsById( collection:IList, items:Array, idField:String = "_id" ):void
		{
			if ( collection && items )
			{
				for ( var i:int = 0; i < items.length; i++ )
				{
					var item:Object = items[ i ];
					
					if ( item && item.hasOwnProperty( idField ) )
						removeItemById( collection, item[ idField ], idField );
				}
			}
		}
		
		/**
		 * This method replaces one item in a collection with an updated version.  The id is used
		 * to do the matching.
		 *
		 * @param collection The collection containing the original item
		 * @param item The updated item
		 * @param idField The field used to match the items
		 */
		public static function replaceItemById( collection:IList, item:Object, idField:String = "_id" ):void
		{
			var currentItem:Object;
			
			if ( collection && item )
			{
				var len:int = collection.length;
				
				for ( var i:uint = 0; i < len; i++ )
				{
					currentItem = collection.getItemAt( i );
					
					if ( currentItem[ idField ] == item[ idField ] )
					{
						collection.setItemAt( item, i );
						logger.info( "replaceItemById - item replaced - idField - " + idField );
						return;
					}
				}
			}
			logger.info( "replaceItemById - item not found" );
			return;
		}
		
		public static function replaceItemsById( collection:IList, items:Array, idField:String = "_id" ):void
		{
			if ( collection && items )
			{
				for ( var i:int = 0; i < items.length; i++ )
				{
					var item:Object = items[ i ];
					
					if ( item && item.hasOwnProperty( idField ) )
						replaceItemById( collection, item, idField );
				}
			}
		}
		
		public static function replaceOrAddItem( collection:IList, item:Object, idField:String = "_id" ):void
		{
			if ( collection )
			{
				if ( doesCollectionContainItem( collection, item, idField ) )
				{
					replaceItemById( collection, item, idField );
				}
				else
				{
					collection.addItem( item );
				}
			}
		}
		
		public static function setAllSelected( collection:ArrayCollection, selected:Boolean ):void
		{
			collection.disableAutoUpdate();
			
			for each ( var item:ISelectable in collection )
			{
				item.selected = selected;
			}
			
			collection.enableAutoUpdate();
			collection.refresh();
		}
		
		public static function setPropertyById( collection:IList, newItem:Object, property:String, idValue:Object, idField:String = "_id" ):void
		{
			var currentItem:Object;
			
			if ( collection && newItem )
			{
				
				for ( var i:uint = 0; i < collection.length; i++ )
				{
					currentItem = collection.getItemAt( i );
					
					if ( currentItem.hasOwnProperty( idField ) && currentItem[ idField ] == idValue )
					{
						currentItem[ property ] = newItem;
						logger.info( "setPropertyById - new property value set for collection item - " + idValue );
						return;
					}
				}
			}
			logger.info( "setPropertyById - item with " + idValue + " not found in collection" );
			return;
		}
		
		//======================================
		// protected static methods 
		//======================================
		
		/**
		 * Check if filter key words exist in an array of values
		 * @param searchTerm
		 * @param values
		 * @return
		 */
		protected static function checkFilterTerm( filterTerm:String, values:Array ):Boolean
		{
			if ( filterTerm == null || filterTerm == "" )
				return true;
			
			filterTerm = filterTerm.toLowerCase();
			
			var keyWords:Array = filterTerm.split( " " );
			
			for each ( var value:String in values )
			{
				for each ( var keyWord:String in keyWords )
				{
					keyWord.replace( /\s+/g, "" );
					
					if ( value != null && keyWord != "" && value.toLowerCase().indexOf( keyWord ) != -1 )
					{
						return true;
					}
				}
			}
			
			return false;
		}
	}
}
