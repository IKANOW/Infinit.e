package com.ikanow.infinit.e.query.view.builder
{
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.view.layout.FlowLayout;
	import flash.display.DisplayObject;
	import mx.core.EventPriority;
	import mx.core.IFactory;
	import mx.core.IVisualElement;
	import mx.events.DragEvent;
	import mx.managers.DragManager;
	import spark.components.SkinnableDataContainer;
	import spark.layouts.supportClasses.DropLocation;
	
	/**
	 * A bubbling version of the dragDrop event
	 */
	[Event( name = "queryTermDragDrop", type = "com.ikanow.infinit.e.shared.event.QueryEvent" )]
	/**
	 * Dispatched when a query term is deleted
	 */
	[Event( name = "deleteQueryTerm", type = "com.ikanow.infinit.e.shared.event.QueryEvent" )]
	/**
	 * Dispatched when a query term is selected for editing
	 */
	[Event( name = "editQueryTerm", type = "com.ikanow.infinit.e.shared.event.QueryEvent" )]
	/**
	 * Dispatched when a query operator is changed
	 */
	[Event( name = "queryOperatorChange", type = "flash.events.Event" )]
	/**
	 *  Dispatched when an item renderer is clicked
	 */
	[Event( name = "itemClick", type = "mx.events.ItemClickEvent" )]
	/**
	 *  Colors for the nested query levels
	 */
	[Style( name = "level1Color", type = "uint", format = "Color", inherit = "yes", theme = "spark" )]
	[Style( name = "level2Color", type = "uint", format = "Color", inherit = "yes", theme = "spark" )]
	[Style( name = "level3Color", type = "uint", format = "Color", inherit = "yes", theme = "spark" )]
	[Style( name = "level4Color", type = "uint", format = "Color", inherit = "yes", theme = "spark" )]
	[Style( name = "level5Color", type = "uint", format = "Color", inherit = "yes", theme = "spark" )]
	public class QueryTermSkinnableDataContainer extends SkinnableDataContainer
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var queryLevel:int;
		
		public var queryTerm:*;
		
		[Bindable]
		public var addButtonX:int;
		
		[Bindable]
		public var addButtonY:int;
		
		//----------------------------------
		//  dropIndicator
		//----------------------------------
		
		[SkinPart( required = "false", type = "flash.display.DisplayObject" )]
		
		/**
		 *  A skin part that defines the appearance of the drop indicator.
		 *  The drop indicator is resized and positioned by the layout
		 *  to outline the insert location when dragging over the List.
		 *
		 *  <p>By default, the drop indicator for a Spark control is a solid line
		 *  that spans the width of the control.
		 *  Create a custom drop indicator by creating a custom skin class for the drop target.
		 *  In your skin class, create a skin part named <code>dropIndicator</code>,
		 *  in the &lt;fx:Declarations&gt; area of the skin class.</p>
		 *
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion Flex 4
		 */
		public var dropIndicator:IFactory;
		
		/**
		 *  @private
		 *  Storage for the <code>dropEnabled</code> property.
		 */
		private var _dropEnabled:Boolean = false;
		
		[Inspectable( defaultValue = "false" )]
		
		/**
		 *  A flag that indicates whether dragged items can be dropped onto the
		 *  control.
		 *
		 *  <p>If you set this property to <code>true</code>,
		 *  the control accepts all data formats, and assumes that
		 *  the dragged data matches the format of the data in the data provider.
		 *  If you want to explicitly check the data format of the data
		 *  being dragged, you must handle one or more of the drag events,
		 *  such as <code>dragEnter</code> and <code>dragOver</code>,
		 *  and call the DragEvent's <code>preventDefault()</code> method
		 *  to customize the way the list class accepts dropped data.</p>
		 *
		 *  <p>When you set <code>dropEnabled</code> to <code>true</code>,
		 *  Flex automatically calls the <code>showDropFeedback()</code>
		 *  and <code>hideDropFeedback()</code> methods to display the drop
		 *  indicator.</p>
		 *
		 *  <p>Drag and drop is not supported on mobile devices where
		 *  <code>interactionMode</code> is set to <code>touch</code>.</p>
		 *
		 *  @default false
		 *
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion Flex 4
		 */
		public function get dropEnabled():Boolean
		{
			return _dropEnabled;
		}
		
		/**
		 *  @private
		 */
		public function set dropEnabled( value:Boolean ):void
		{
			if ( value == _dropEnabled )
				return;
			_dropEnabled = value;
			
			if ( _dropEnabled )
			{
				addEventListener( DragEvent.DRAG_ENTER, dragEnterHandler, false, EventPriority.DEFAULT_HANDLER );
				addEventListener( DragEvent.DRAG_EXIT, dragExitHandler, false, EventPriority.DEFAULT_HANDLER );
				addEventListener( DragEvent.DRAG_OVER, dragOverHandler, false, EventPriority.DEFAULT_HANDLER );
				addEventListener( DragEvent.DRAG_DROP, dragDropHandler, false, EventPriority.DEFAULT_HANDLER );
			}
			else
			{
				removeEventListener( DragEvent.DRAG_ENTER, dragEnterHandler, false );
				removeEventListener( DragEvent.DRAG_EXIT, dragExitHandler, false );
				removeEventListener( DragEvent.DRAG_OVER, dragOverHandler, false );
				removeEventListener( DragEvent.DRAG_DROP, dragDropHandler, false );
			}
		}
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryTermSkinnableDataContainer()
		{
			super();
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function calculateDropLocation( event:DragEvent ):DropLocation
		{
			// Verify data format
			if ( !enabled || !event.dragSource.hasFormat( QueryConstants.DRAG_DATA ) )
				return null;
			
			// Calculate the drop location
			return layout.calculateDropLocation( event );
		}
		
		/**
		 *  Creates and instance of the dropIndicator class that is used to
		 *  display the visuals of the drop location during a drag and drop
		 *  operation. The instance is set in the layout's
		 *  <code>dropIndicator</code> property.
		 *
		 *  <p>If you override the <code>dragEnter</code> event handler,
		 *  and call <code>preventDefault()</code> so that the default handler does not execute,
		 *  call <code>createDropIndicator()</code> to create the drop indicator.</p>
		 *
		 *  @return Returns the dropIndicator that was set in the layout.
		 *
		 *  @see #destroyDropIndicator
		 *
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion Flex 4
		 */
		public function createDropIndicator():DisplayObject
		{
			// Do we have a drop indicator already?
			if ( layout.dropIndicator )
				return layout.dropIndicator;
			
			var dropIndicatorInstance:DisplayObject;
			
			if ( dropIndicator )
			{
				dropIndicatorInstance = DisplayObject( createDynamicPartInstance( "dropIndicator" ) );
			}
			else
			{
				var dropIndicatorClass:Class = Class( getStyle( "dropIndicatorSkin" ) );
				
				if ( dropIndicatorClass )
					dropIndicatorInstance = new dropIndicatorClass();
			}
			
			if ( dropIndicatorInstance is IVisualElement )
				IVisualElement( dropIndicatorInstance ).owner = this;
			
			// Set it in the layout
			layout.dropIndicator = dropIndicatorInstance;
			return dropIndicatorInstance;
		}
		
		/**
		 *  Releases the <code>dropIndicator</code> instance that is currently set in the layout.
		 *
		 *  <p>If you override the <code>dragExit</code> event handler,
		 *  and call <code>preventDefault()</code> so that the default handler does not execute,
		 *  call <code>destroyDropIndicator()</code> to delete the drop indicator.</p>
		 *
		 *  @return Returns the dropIndicator that was removed.
		 *
		 *  @see #createDropIndicator
		 *
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion Flex 4
		 */
		public function destroyDropIndicator():DisplayObject
		{
			var dropIndicatorInstance:DisplayObject = layout.dropIndicator;
			
			if ( !dropIndicatorInstance )
				return null;
			
			// Release the reference from the layout
			layout.dropIndicator = null;
			
			// Release it if it's a dynamic skin part
			var count:int = numDynamicParts( "dropIndicator" );
			
			for ( var i:int = 0; i < count; i++ )
			{
				if ( dropIndicatorInstance == getDynamicPartAt( "dropIndicator", i ) )
				{
					// This was a dynamic part, remove it now:
					removeDynamicPartInstance( "dropIndicator", dropIndicatorInstance );
					break;
				}
			}
			return dropIndicatorInstance;
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 *  @private
		 *  Handles <code>DragEvent.DRAG_DROP events</code>. This method  hides
		 *  the drop feedback by calling the <code>hideDropFeedback()</code> method.
		 *
		 *  <p>If the action is a <code>COPY</code>,
		 *  then this method makes a deep copy of the object
		 *  by calling the <code>ObjectUtil.copy()</code> method,
		 *  and replaces the copy's <code>uid</code> property (if present)
		 *  with a new value by calling the <code>UIDUtil.createUID()</code> method.</p>
		 *
		 *  @param event The DragEvent object.
		 *
		 *  @see mx.utils.ObjectUtil
		 *  @see mx.utils.UIDUtil
		 *
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion Flex 4
		 */
		protected function dragDropHandler( event:DragEvent ):void
		{
			if ( event.isDefaultPrevented() )
				return;
			
			// re-dispatch the drag event, but have it bubble
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.QUERY_TERM_DRAG_DROP, true, true );
			queryEvent.dragEvent = event;
			dispatchEvent( queryEvent );
			
			// Hide the drop indicator
			layout.hideDropIndicator();
			destroyDropIndicator();
		}
		
		/**
		 *  @private
		 *  Handles <code>DragEvent.DRAG_ENTER</code> events.  This method
		 *  determines if the DragSource object contains valid elements and uses
		 *  the <code>DragManager.showDropFeedback()</code> method to set up the
		 *  UI feedback as well as the layout's <code>showDropIndicator()</code>
		 *  method to display the drop indicator and initiate drag scrolling.
		 *
		 *  @param event The DragEvent object.
		 *
		 *  @see spark.layouts.LayoutBase#showDropIndicator
		 *  @see spark.layouts.LayoutBase#hideDropIndicator
		 *
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion Flex 4
		 */
		protected function dragEnterHandler( event:DragEvent ):void
		{
			if ( event.isDefaultPrevented() )
				return;
			
			if ( event.dragSource.hasFormat( QueryConstants.DRAG_DATA ) && queryLevel < 5 )
			{
				var dropLocation:DropLocation = calculateDropLocation( event );
				
				if ( dropLocation )
				{
					DragManager.acceptDragDrop( this );
					
					// Create the dropIndicator instance. The layout will take care of
					// parenting, sizing, positioning and validating the dropIndicator.
					createDropIndicator();
					
					// Notify manager we can drop
					DragManager.showFeedback( event.ctrlKey ? DragManager.COPY : DragManager.MOVE );
					
					// Show drop indicator
					layout.showDropIndicator( dropLocation );
				}
				else
				{
					DragManager.showFeedback( DragManager.NONE );
				}
			}
		}
		
		/**
		 *  @private
		 *  Handles <code>DragEvent.DRAG_EXIT</code> events. This method hides
		 *  the UI feedback by calling the <code>hideDropFeedback()</code> method
		 *  and also hides the drop indicator by calling the layout's
		 *  <code>hideDropIndicator()</code> method.
		 *
		 *  @param event The DragEvent object.
		 *
		 *  @see spark.layouts.LayoutBase#showDropIndicator
		 *  @see spark.layouts.LayoutBase#hideDropIndicator
		 *
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion Flex 4
		 */
		protected function dragExitHandler( event:DragEvent ):void
		{
			if ( event.isDefaultPrevented() )
				return;
			
			// Hide if previously showing
			layout.hideDropIndicator();
			
			// Destroy the dropIndicator instance
			destroyDropIndicator();
		}
		
		/**
		 *  @private
		 *  Handles <code>DragEvent.DRAG_OVER</code> events. This method
		 *  determines if the DragSource object contains valid elements and uses
		 *  the <code>showDropFeedback()</code> method to set up the UI feedback
		 *  as well as the layout's <code>showDropIndicator()</code> method
		 *  to display the drop indicator and initiate drag scrolling.
		 *
		 *  @param event The DragEvent object.
		 *
		 *  @see spark.layouts.LayoutBase#showDropIndicator
		 *  @see spark.layouts.LayoutBase#hideDropIndicator
		 *
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion Flex 4
		 */
		protected function dragOverHandler( event:DragEvent ):void
		{
			if ( event.isDefaultPrevented() )
				return;
			
			if ( event.dragSource.hasFormat( QueryConstants.DRAG_DATA ) )
			{
				var dropLocation:DropLocation = calculateDropLocation( event );
				
				if ( dropLocation )
				{
					// Notify manager we can drop
					DragManager.showFeedback( event.ctrlKey ? DragManager.COPY : DragManager.MOVE );
					
					// Show drop indicator
					layout.showDropIndicator( dropLocation );
				}
				else
				{
					// Hide if previously showing
					layout.hideDropIndicator();
					
					// Notify manager we can't drop
					DragManager.showFeedback( DragManager.NONE );
				}
			}
		}
		
		override protected function updateDisplayList( unscaledWidth:Number, unscaledHeight:Number ):void
		{
			super.updateDisplayList( unscaledWidth, unscaledHeight );
			
			if ( layout && layout is FlowLayout )
			{
				layout.updateDisplayList( unscaledWidth, unscaledHeight );
				
				addButtonX = FlowLayout( layout ).addButtonX;
				addButtonY = FlowLayout( layout ).addButtonY;
			}
		}
	}
}
