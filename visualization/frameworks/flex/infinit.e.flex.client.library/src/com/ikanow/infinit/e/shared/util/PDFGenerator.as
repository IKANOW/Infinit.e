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
package com.ikanow.infinit.e.shared.util
{
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.PDFConstants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.widget.library.data.WidgetContext;
	import com.ikanow.infinit.e.widget.library.frameworkold.ModuleInterface;
	import com.ikanow.infinit.e.widget.library.utility.HtmlConvert;
	import com.ikanow.infinit.e.widget.library.utility.JSONEncoder;
	import com.ikanow.infinit.e.widget.library.widget.IWidget;
	import flash.display.DisplayObject;
	import flash.display.DisplayObjectContainer;
	import flash.utils.Endian;
	import mx.collections.ArrayCollection;
	import mx.controls.DataGrid;
	import mx.resources.IResourceManager;
	import mx.resources.ResourceManager;
	import mx.utils.StringUtil;
	import spark.components.HGroup;
	import spark.components.VGroup;
	import org.alivepdf.colors.RGBColor;
	import org.alivepdf.data.Grid;
	import org.alivepdf.data.GridColumn;
	import org.alivepdf.display.Display;
	import org.alivepdf.fonts.FontFamily;
	import org.alivepdf.fonts.Style;
	import org.alivepdf.images.ImageFormat;
	import org.alivepdf.images.PNGImage;
	import org.alivepdf.images.ResizeMode;
	import org.alivepdf.layout.Align;
	import org.alivepdf.layout.Layout;
	import org.alivepdf.layout.Orientation;
	import org.alivepdf.layout.Size;
	import org.alivepdf.layout.Unit;
	import org.alivepdf.pages.Page;
	import org.alivepdf.pdf.PDF;
	import org.alivepdf.saving.Download;
	import org.alivepdf.saving.Method;
	import system.Char;
	
	public class PDFGenerator
	{
		
		//======================================
		// protected properties 
		//======================================
		
		protected var printPDF:PDF = null;
		
		protected var sectionNumber:uint = 1;
		
		protected var MAX_IMAGE_HEIGHT:int = PDFConstants.MAX_IMAGE_HEIGHT;
		
		protected var MAX_IMAGE_WIDTH:int = PDFConstants.MAX_IMAGE_WIDTH;
		
		protected var resourceManager:IResourceManager = ResourceManager.getInstance();
		
		//======================================
		// constructor 
		//======================================
		
		public function PDFGenerator()
		{
			printPDF = new PDF( Orientation.LANDSCAPE, Unit.MM, Size.LETTER );
			printPDF.setDisplayMode( Display.FULL_PAGE, Layout.SINGLE_PAGE );
			printPDF.setCreator( PDFConstants.CREATOR );
			printPDF.setAuthor( PDFConstants.AUTHOR );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function generateAppendix( communityIds:String, context:WidgetContext ):void
		{
			printPDF.addPage( new Page( Orientation.PORTRAIT, Unit.MM, Size.LETTER ) );
			printPDF.setFont( FontFamily.ARIAL, Style.UNDERLINE, 14 );
			printPDF.textStyle( new RGBColor( 0x000000 ) ) //black
			printPDF.writeText( 14, resourceManager.getString( 'infinite', 'pdfGenerator.sectionHeader', [ sectionNumber, PDFConstants.APPENDIX ] ) );
			printPDF.newLine( 10 );
			var jsonQuery:String = JSONEncoder.encode( context.getLastQuery() );
			
			var url:String = ServiceConstants.QUERY_URL + communityIds;
			printPDF.setFont( FontFamily.ARIAL, Style.UNDERLINE, 14 );
			printPDF.writeText( 14, resourceManager.getString( 'infinite', 'pdfGenerator.sectionSubHeader', [ sectionNumber, '1', PDFConstants.URL ] ) );
			printPDF.setFont( FontFamily.ARIAL, Style.NORMAL, 12 );
			printPDF.newLine( 10 );
			printPDF.writeText( 14, url, url );
			printPDF.newLine( 10 );
			
			
			//JSON
			printPDF.setFont( FontFamily.ARIAL, Style.UNDERLINE, 14 );
			printPDF.writeText( 14, resourceManager.getString( 'infinite', 'pdfGenerator.sectionSubHeader', [ sectionNumber, '2', PDFConstants.JSON ] ) );
			printPDF.setFont( FontFamily.ARIAL, Style.NORMAL, 12 );
			printPDF.newLine( 10 );
			printFormattedJson( jsonQuery )
		}
		
		public function generateQueryPage( username:String, enabledCommunities:String, context:WidgetContext ):void
		{
			var humanReadableQuery:String = context.getQuery_AllResults().getDescription();
			printPDF.setKeywords( humanReadableQuery );
			
			
			//Filter description
			var filterString:String = resourceManager.getString( 'infinite', 'pdfConstants.noFilter' );
			
			if ( null != context.getQuery_FilteredResults() &&
				( context.getQuery_FilteredResults() != context.getQuery_TopResults() ) )
			{
				filterString = context.getQuery_FilteredResults().getDescription();
			}
			
			
			printPDF.addPage( new Page( Orientation.PORTRAIT, Unit.MM, Size.LETTER ) );
			var dp:ArrayCollection = new ArrayCollection();
			var boolString:String = Constants.BLANK;
			var keywordString:String = Constants.BLANK;
			var i:int = 0;
			var myDate:Date = new Date();
			
			printPDF.setFont( FontFamily.ARIAL, Style.NORMAL, 12 );
			printPDF.textStyle( new RGBColor( 0x000000 ) ) //black
			
			printPDF.addText( resourceManager.getString( 'infinite', 'pdfConstants.user' ) + username, 160, printPDF.getY() + 7 );
			printPDF.newLine( 8 );
			printPDF.addText( resourceManager.getString( 'infinite', 'pdfConstants.date' ) + myDate.toDateString(), 160, printPDF.getY() + 5 );
			printPDF.newLine( 10 );
			
			//General Information
			printPDF.setFont( FontFamily.ARIAL, Style.UNDERLINE, 14 );
			printPDF.writeText( 14, resourceManager.getString( 'infinite', 'pdfGenerator.sectionHeader', [ sectionNumber, PDFConstants.GENERAL_INFORMATION ] ) );
			printPDF.newLine( 10 );
			printPDF.writeText( 14, resourceManager.getString( 'infinite', 'pdfGenerator.sectionSubHeader', [ sectionNumber, '1', PDFConstants.QUERY ] ) );
			printPDF.setFont( FontFamily.ARIAL, Style.NORMAL, 12 );
			printPDF.newLine( 10 );
			printPDF.writeText( 14, humanReadableQuery );
			printPDF.newLine( 10 );
			
			printPDF.newLine( 10 );
			printPDF.setFont( FontFamily.ARIAL, Style.UNDERLINE, 14 );
			printPDF.writeText( 14, resourceManager.getString( 'infinite', 'pdfGenerator.sectionSubHeader', [ sectionNumber, '2', PDFConstants.FILTER ] ) );
			printPDF.setFont( FontFamily.ARIAL, Style.NORMAL, 12 );
			printPDF.newLine( 10 );
			
			printPDF.writeText( 12, filterString );
			printPDF.newLine( 10 );
			
			//Communities
			printPDF.setFont( FontFamily.ARIAL, Style.UNDERLINE, 14 );
			printPDF.newLine( 10 );
			printPDF.writeText( 14, resourceManager.getString( 'infinite', 'pdfGenerator.sectionSubHeader', [ sectionNumber, '3', PDFConstants.COMMUNITIES ] ) );
			printPDF.setFont( FontFamily.ARIAL, Style.NORMAL, 12 );
			printPDF.newLine( 10 );
			
			if ( !enabledCommunities )
				printPDF.writeText( 12, resourceManager.getString( 'infinite', 'pdfConstants.noCommunitiesEnabled' ) );
			else
				printPDF.writeText( 12, enabledCommunities );
			
			
			sectionNumber++;
		
		}
		
		public function saveToPdf():void
		{
			var url:String = ServiceConstants.SERVER_URL.replace( ServiceConstants.API_URL, Constants.BLANK );
			printPDF.save( Method.REMOTE, url + ServiceConstants.PDF_URL, Download.INLINE, PDFConstants.FILE_NAME );
		}
		
		
		public function widgetToPdf( widget:DisplayObject, title:String ):void
		{
			if ( widget != null )
			{
				var mod:ModuleInterface = widget as ModuleInterface;
				var iwid:IWidget = widget as IWidget;
				var tempPDF:PDF = null;
				
				if ( mod != null )
				{
					tempPDF = mod.generatePdf( printPDF, resourceManager.getString( 'infinite', 'pdfGenerator.sectionHeader', [ sectionNumber, title ] ) );
				}
				else if ( iwid != null )
				{
					tempPDF = iwid.onGeneratePDF( printPDF, resourceManager.getString( 'infinite', 'pdfGenerator.sectionHeader', [ sectionNumber, title ] ) );
				}
				
				if ( tempPDF == null )
				{
					screenshotWidgetToPdf( widget, resourceManager.getString( 'infinite', 'pdfGenerator.sectionHeader', [ sectionNumber, title ] ) );
				}
				else
				{
					printPDF = tempPDF;
				}
				
				sectionNumber++;
			}
		}
		
		//======================================
		// private methods 
		//======================================
		
		private function pdfIndent( depth:int ):void
		{
			for ( var i:int = 0; i < depth; i++ )
			{
				printPDF.writeText( 12, PDFConstants.INDENT );
			}
		}
		
		private function printFormattedJson( input:String ):void
		{
			var quote:String;
			var depth:int = 0;
			
			for ( var i:int = 0; i < input.length; i++ )
			{
				var ch:String = input.charAt( i );
				
				switch ( ch )
				{
					case '{':
					case '[':
						printPDF.writeText( 12, ch.toString() );
						if ( quote == null )
						{
							printPDF.newLine( 10 );
							depth++;
							pdfIndent( depth );
						}
						break;
					case '}':
					case ']':
						if ( quote != null )
							printPDF.writeText( 12, ch.toString() );
						else
						{
							printPDF.newLine( 10 );
							depth--;
							pdfIndent( depth );
							printPDF.writeText( 12, ch.toString() );
						}
						break;
					case '"':
						printPDF.writeText( 12, ch.toString() );
						if ( quote != null )
						{
							//if (!output.IsEscaped(i))
							quote = null;
						}
						else
							quote = ch;
						break;
					case ',':
						printPDF.writeText( 12, ch.toString() );
						if ( quote == null )
						{
							printPDF.newLine( 10 );
							pdfIndent( depth );
						}
						break;
					case ':':
						if ( quote != null )
							printPDF.writeText( 12, ch.toString() );
						else
							printPDF.writeText( 12, ": " );
						break;
					default:
						if ( quote != null || ch.toString() != " " )
							printPDF.writeText( 12, ch.toString() );
						break;
				}
			}
		
		}
		
		private function screenshotWidgetToPdf( doc:DisplayObject, title:String ):void
		{
			doc.cacheAsBitmap = true;
			printPDF.addPage( new Page( Orientation.LANDSCAPE, Unit.MM, Size.LETTER ) );
			printPDF.setFont( FontFamily.ARIAL, Style.UNDERLINE, 14 );
			printPDF.writeText( 14, title );
			printPDF.setFont( FontFamily.ARIAL, Style.NORMAL, 12 );
			printPDF.newLine( 10 );
			
			var imageWidth:Number = doc.width;
			var imageHeight:Number = doc.height;
			
			if ( imageWidth >= imageHeight )
			{
				imageHeight = ( ( imageHeight / imageWidth ) * MAX_IMAGE_WIDTH );
				imageWidth = MAX_IMAGE_WIDTH;
			}
			else
			{
				imageWidth = ( ( imageWidth / imageHeight ) * MAX_IMAGE_HEIGHT );
				imageHeight = MAX_IMAGE_HEIGHT;
			}
			
			printPDF.addImage( doc, printPDF.getX(), printPDF.getY(), imageWidth, imageHeight, ImageFormat.PNG, 100, 1, ResizeMode.NONE );
		
		}
		
		private function trim( s:String ):String
		{
			return s.replace( /^([\s|\t|\n]+)?(.*)([\s|\t|\n]+)?$/gm, "$2" );
		}
	}
}
