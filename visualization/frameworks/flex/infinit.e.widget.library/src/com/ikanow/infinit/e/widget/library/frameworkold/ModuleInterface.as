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
package com.ikanow.infinit.e.widget.library.frameworkold
{
	import com.ikanow.infinit.e.widget.library.data.SelectedItem;
	
	import flash.events.Event;
	
	import org.alivepdf.pdf.PDF;

	/**
	 *  DEPRICATED: Use IWidget interface now.
	 *  This interface is used as a template to add module and custom widgets to the Application.
	 * The method in this interface shall be used to send and receive data from the widget and for the 
	 * application to communicate with the widgets
	 */
	
	public interface ModuleInterface
	{
		/**
		 * function to receive results from database
		 * 
		 * @param queryResults The query results returned
		*/
		function receiveQueryResults(queryResults:QueryResults):void;
		
		/**
		 * function to receive a selected item (i.e. from the map, or timeline)
		 * to access the data loop through the selectedItem(s) created a variable
		 * instance of type SelectedInstance and loop through the selectedItem.getSelectedInstances() arrayCollection
		 * (i.e for each(var instance:SelectedInstance in selectedItem.getSelectedInstances()){}
		 * use the instance variable to access its methods accordingly
		 * 
		 * @param selecteItem The selected item being passed from another module
		*/
		function receiveSelectedItem(selectedItem:SelectedItem):void;
		
		/**
		 * function to rescale the component when the parent container is being resized
		 * 
		 * @param newHeight The new height the component needs to be set to
		 * @param newWidth The new width the component needs to be set to
		*/ 
		function reScale(newHeight:Number,newWidth:Number):void;
		
		/**
		 * function to send selected items to other modules that are loaded into the environment
		 * 
		 * @param selectedItem The selected item to be sent to the other modules
		*/
		function sendSelectedItem(selectedItem:SelectedItem):void;
		
		/**
		 * function to broadcast if the module has data so it doesn't repass data to itself
		 * when new modules are being loaded with data on first load up
		 * 
		 * @return If the module has data
		*/
		function hasData():Boolean;
		
		/**
		 * function to start the components spinner when it is receiving data
		*/
		function startSpinner():void;
		
		/**
		 * function to pass an event into the module
		 *
		 * @param event The Event to pass into the module
		*/
		function receiveEvent(event:Event):void;
		
		/**
		 * This function gets called when the user clicks to output 
		 * data to a PDF. Return null if custom PDF generation is
		 * not desired.
		 * 
		 * @return a new alivePdf Page containing the converted data
		 */
		function generatePdf(printPDF:PDF, title:String):PDF;
	}
}
