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
package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.constant.settings.QueryAdvancedSettingsConstants;
	import com.ikanow.infinit.e.shared.util.FormatterUtil;
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class TimeProximity extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var decay:String;
		
		public var time:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function TimeProximity()
		{
			super();
			reset();
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function apply( options:TimeProximity ):void
		{
			decay = options.decay;
			time = options.time;
			
			var today:String = FormatterUtil.getFormattedDateString( new Date() );
			
			if ( time == today || time == Constants.BLANK )
				time = QueryConstants.NOW;
		}
		
		public function clone():TimeProximity
		{
			var clone:TimeProximity = new TimeProximity();
			
			clone.decay = decay;
			clone.time = time;
			
			return clone;
		}
		
		public function reset():void
		{
			decay = QueryAdvancedSettingsConstants.SCORING_TIME_PROX_DECAY;
			time = QueryConstants.NOW;
		}
	}
}
