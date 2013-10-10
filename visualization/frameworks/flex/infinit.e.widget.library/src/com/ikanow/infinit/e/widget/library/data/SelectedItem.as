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
/**
 * <p>Infinit.e</p>
 *
 * <p>Copyright (c) 2011 IKANOW, llc.</p>
 * <p>http://www.ikanow.com</p> 
 *
 * <p>NOTICE:  IKANOW permits you to use this this file in accordance with the terms of the license agreement 
 * accompanying it.  For information about the licensing and copyright of this Plug-In please contact IKANOW, llc. 
 * at support&#64;ikanow.com.</p>
 *
 * <p>http://www.ikanow.com/terms-conditions/</p>
 * 
 */
package com.ikanow.infinit.e.widget.library.data
{
	import mx.collections.ArrayCollection;

	/**
	 * DEPRICATED: This class builds and retrieves selected items from a component
	*/
	
	public class SelectedItem
	{
		private var selectedItems:ArrayCollection = new ArrayCollection();
		private var filterDescription:String = null;
		
		/**
		 * Constructor
		 * 
		 * @param _selectedItems The array collection of selected items from the module
		*/
		public function SelectedItem(_selectedItems:ArrayCollection=null)
		{
			if (null != _selectedItems) {
				selectedItems = _selectedItems;
			}
			//(else starts empty)
		}
		
		public function setDescription(description:String):void
		{
			this.filterDescription = description;
		}
		public function getDescription():String
		{
			return this.filterDescription;
		}
		
		/**
		 * function to add selected instances for the module
		 * 
		 * @param instance The current selected item instance
		*/ 
		public function addSelectedInstance(instance:SelectedInstance):void
		{
			if ( selectedItems == null )
				selectedItems = new ArrayCollection();
			selectedItems.addItem(instance);
		}
		
		/**
		 * function to set selected instances for the module 
		 * 
		 * @param _selectedItems The array collection of currently selected items
		*/
		public function setSelectedInstances(_selectedItems:ArrayCollection):void
		{
			selectedItems = _selectedItems;
		}
		
		/**
		 * function to get the selected instances passed from the module
		 * 
		 * @return The selected items from the module
		*/
		public function getSelectedInstances():ArrayCollection
		{
			return selectedItems;
		}
	}
}
