/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package actionscript
{
	import com.ibm.ilog.elixir.timeline.components.TimelineDataGroup;
	import com.ibm.ilog.elixir.timeline.components.supportClasses.Timeline;
	import com.ibm.ilog.elixir.timeline.components.supportClasses.TimelineBandBase;
	import com.ibm.ilog.elixir.timeline.supportClasses.EventRenderer;
	
	import flash.display.DisplayObjectContainer;
	
	import mx.formatters.Formatter;
	
	import spark.components.supportClasses.TextBase;
	
	
	public class TimelineExpandCollapseRenderer extends EventRenderer
	{		
		/**
		 * Constructor.
		 */
		public function TimelineExpandCollapseRenderer()
		{
			super();
		}
		
		[Bindable]
		
		/**
		 * function to override the owner
		 * 
		 * @param value The current owner
		*/
		override public function set owner(value:DisplayObjectContainer):void
		{
			super.owner = value;
		}
		
		private var _expanded:Boolean = false;
		private var _highlighted:Boolean = false; 
		
		/**
		 * function to get the expanded value
		 * 
		 * @return The expanded value
		*/
		public function get expanded():Boolean
		{
			return _expanded;
		}
		
		/**
		 * function to get whether the skin is highlited
		 * 
		 * @return The highlighted value
		*/
		[Bindable]
		public function get highlighted():Boolean
		{
			return _highlighted;
		}
		
		/**
		 * function to set the highligted value
		 * 
		 * @param value The value to set highlighted to
		*/
		public function set highlighted(value:Boolean):void
		{
			_highlighted = value;
		}
		
		/**
		 * function to set the expanded value
		 * 
		 * @param value The value to set for expanded
		*/
		public function set expanded(value:Boolean):void
		{
			if (value != _expanded) {
				_expanded = value;
				if (value) {
					if ( InfiniteTimelineEventWidget.lastExpanded != null) {
						InfiniteTimelineEventWidget.lastExpanded.expanded = false;
					}
					InfiniteTimelineEventWidget.lastExpanded = this;
				} else if (InfiniteTimelineEventWidget.lastExpanded == this) {
					InfiniteTimelineEventWidget.lastExpanded = null;
				}
				TimelineBandBase(owner).showDataTips = !_expanded; 
				setCurrentState(getCurrentRendererState());
			}
		}
		
		/**
		 * function to get the current rendered state of the skin
		 * 
		 * @return The current state
		*/
		override protected function getCurrentRendererState():String
		{
			if (expanded) {
				// as soon as we are expanded we don't care anymore about
				// other states, that is our main state
				return "expanded";
			} else
				if(!expanded)
				{
					return "baseState";
				}
			return super.getCurrentRendererState();
		}
	}
}
