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
package com.ikanow.infinit.e.shared.model.constant
{
	import mx.resources.ResourceManager;
	
	/**
	 * PDF Constants
	 */
	public class PDFConstants
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const PDF:String = "pdf";
		
		public static const INDENT:String = "        ";
		
		public static const MAX_IMAGE_HEIGHT:int = 190;
		
		public static const MAX_IMAGE_WIDTH:int = 260;
		
		public static const FILE_NAME:String = "test.pdf";
		
		public static const CREATOR:String = "IKANOW Infinit.e";
		
		public static const AUTHOR:String = "IKANOW";
		
		public static const USER:String = ResourceManager.getInstance().getString( 'infinite', 'pdfConstants.user' );
		
		public static const DATE:String = ResourceManager.getInstance().getString( 'infinite', 'pdfConstants.date' );
		
		public static const APPENDIX:String = ResourceManager.getInstance().getString( 'infinite', 'pdfConstants.appendix' );
		
		public static const URL:String = ResourceManager.getInstance().getString( 'infinite', 'pdfConstants.url' );
		
		public static const JSON:String = ResourceManager.getInstance().getString( 'infinite', 'pdfConstants.json' );
		
		public static const GENERAL_INFORMATION:String = ResourceManager.getInstance().getString( 'infinite', 'pdfConstants.generalInformation' );
		
		public static const QUERY:String = ResourceManager.getInstance().getString( 'infinite', 'pdfConstants.query' );
		
		public static const FILTER:String = ResourceManager.getInstance().getString( 'infinite', 'pdfConstants.filter' );
		
		public static const COMMUNITIES:String = ResourceManager.getInstance().getString( 'infinite', 'pdfConstants.communities' );
	}
}
