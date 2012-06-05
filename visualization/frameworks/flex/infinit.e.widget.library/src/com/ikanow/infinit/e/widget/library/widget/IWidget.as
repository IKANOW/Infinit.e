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
package com.ikanow.infinit.e.widget.library.widget
{
	import com.ikanow.infinit.e.widget.library.data.WidgetSaveObject;
	import flash.utils.ByteArray;
	import mx.collections.ArrayCollection;
	import org.alivepdf.pdf.PDF;
	
	/**
	 * All widgets must adhere to this interface, provides methods
	 * for receiving data from framework, and sending filter
	 * events to the framework.  Also contains event for resizing.
	 */
	public interface IWidget
	{
		
		/**
		 * Allow users to export the widget contents in the specified format
		 * @format filename the filename+path to which the data will be written (in case it needs to be embedded)
		 * @param format the format from the "supportedFormats" call
		 *
		 * @return a ByteArray containing the data to output
		 */
		
		function onGenerateExportData( filename:String, format:String ):ByteArray;
		
		/**
		 * This function gets called when the user clicks to output
		 * data to a PDF. Return null if custom PDF generation is
		 * not desired.
		 *
		 * @return a new alivePdf Page containing the converted data
		 */
		function onGeneratePDF( printPDF:PDF, title:String ):PDF;
		/**
		 * Once the widget has loaded it receives a reference
		 * to the data object via this function.
		 *
		 * @param context The reference to the data model.
		 */
		function onInit( context:IWidgetContext ):void;
		
		/**
		 * If a save object has been saved from 'onSaveWidgetOptions' then
		 * when the app gets reloaded the last save string
		 * will be passed to this function.
		 *
		 * @param widgetOptions the last save object or null if there was none
		 */
		function onLoadWidgetOptions( widgetOptions:WidgetSaveObject ):void;
		/**
		 * This function gets called when the parent resizeWindow gets dragged
		 * or changes in size.  This allows the widget to adjust its internal
		 * width/height so scaling can occur.  This function should be
		 * used as follows:
		 *
		 * this.width = newWidth;
		 * this.height = newHeight;
		 *
		 * Because of the way the parent container resizes this function is necessary
		 * to allow proper widget resizing (change size events do not get fired inside
		 * when the outer container changes.
		 *
		 * @param newHeight The new height of the parent container
		 * @param newWidth The new width of the parent container
		 */
		function onParentResize( newHeight:Number, newWidth:Number ):void;
		/**
		 * When a new filter event occurs, the framework will
		 * fire a call to this function, letting the widget know that
		 * new filtered data is available in the IWidgetContext object.
		 */
		function onReceiveNewFilter():void;
		/**
		 * When new queries are issued the framework will
		 * fire a call to this function, letting the widget know
		 * that new data is available in the IWidgetContext object.
		 */
		function onReceiveNewQuery():void;
		
		/**
		 * This function gets called when the workspace is being saved.
		 * return null if no save object is needed.
		 *
		 * @return an object this widget can use to reload state
		 */
		function onSaveWidgetOptions():Object;
		
		// Export functions:
		
		/**
		 * @return A list of supported formats, displayed in a context menu in the format
		 * "Export &lt;string&gt;" - these are called with "generateExportData"
		 * Note this doesn't cover the "built-in" Alive PDF export.
		 * However if the developer specifies PDF and generatePdf() returns non-null then this will be used.
		 */
		
		function supportedExportFormats():ArrayCollection;
	}
}
