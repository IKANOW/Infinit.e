package com.ikanow.infinit.e.widget.library.components.DocViewer
{	
	import flash.events.Event;
	import flash.events.MouseEvent;
	
	import mx.controls.LinkButton;
	import mx.core.IVisualElement;
	import mx.utils.ObjectUtil;
	
	import spark.components.Button;
	import spark.components.Label;
	import spark.components.List;
	
	[Event(name="selectionChange", type="SelectionChangedEvent")]
	
	public class DeselectList extends List
	{
		
		
		public function DeselectList()
		{
			this.allowMultipleSelection = true;
			super();
		}
		
		override protected function item_mouseDownHandler(event:MouseEvent):void
		{		
			
			var newIndex:Number = dataGroup.getElementIndex(event.currentTarget as IVisualElement);
			
			//if a link button was clicked and it is already selected, do nothing (leave selected)
			if ( (event.target is LinkButton) && selectedIndices.indexOf(newIndex) != -1 )
				//do nothing
				return;
			else
			{
				selectedIndices = calculateSelectedIndices(newIndex,false,true);
				var newSelectedIndices:Vector.<int> = new Vector.<int>();
				var found:Boolean = false;
				for each ( var index:int in selectedIndices )
				{
					if ( index != newIndex )
						newSelectedIndices.push(index);
					else
						found = true;
				}
				if ( !found )
					newSelectedIndices.push(newIndex);
				
				this.dispatchEvent(new SelectionChangedEvent("selectionChange",selectedIndices, newSelectedIndices, newIndex, !found));
			}
			
		}
	}
}