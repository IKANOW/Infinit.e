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
package com.ikanow.infinit.e.shared.model.presentation.base
{
	import com.ikanow.infinit.e.shared.model.vo.ui.INavigationItem;
	import flash.events.IEventDispatcher;
	import mx.collections.ArrayCollection;
	
	public interface INavigator
	{
		function get registered():Boolean;
		function set registered( value:Boolean ):void;
		
		function get navigatorId():String;
		function set navigatorId( value:String ):void;
		
		function get parentNavigatorId():String;
		function set parentNavigatorId( value:String ):void;
		
		function get defaultState():String;
		function set defaultState( value:String ):void;
		
		function get defaultView():String;
		function set defaultView( value:String ):void;
		
		[Bindable( event = "navigationItemsChange" )]
		function get navigationItems():ArrayCollection;
		function set navigationItems( value:ArrayCollection ):void;
		
		[Bindable( event = "statesChange" )]
		function get states():ArrayCollection;
		function set states( value:ArrayCollection ):void;
		
		[Bindable( event = "viewsChange" )]
		function get views():ArrayCollection;
		function set views( value:ArrayCollection ):void;
		
		[Bindable( event = "dialogsChange" )]
		function get dialogs():ArrayCollection;
		function set dialogs( value:ArrayCollection ):void;
		
		[Bindable( event = "actionsChange" )]
		function get actions():ArrayCollection;
		function set actions( value:ArrayCollection ):void;
		
		[Bindable( event = "selectedIndexChange" )]
		function get selectedIndex():int;
		function set selectedIndex( value:int ):void;
		
		[Bindable( event = "currentStateChange" )]
		function get currentState():String;
		function set currentState( value:String ):void;
		
		[Bindable( event = "currentStateIndexChange" )]
		function get currentStateIndex():int;
		function set currentStateIndex( value:int ):void;
		
		[Bindable( event = "previousStateChange" )]
		function get previousState():String;
		function set previousState( value:String ):void;
		
		[Bindable( event = "selectedDialogIndexChange" )]
		function get selectedDialogIndex():int;
		function set selectedDialogIndex( value:int ):void;
		
		[Bindable( event = "selectedActionIndexChange" )]
		function get selectedActionIndex():int;
		function set selectedActionIndex( value:int ):void;
		
		[Dispatcher]
		function get dispatcher():IEventDispatcher;
		function set dispatcher( value:IEventDispatcher ):void;
		
		function changeAction( navigationItem:INavigationItem ):Boolean;
		
		function changeDialog( navigationItem:INavigationItem ):Boolean;
		
		function changeState( state:String ):Boolean;
		
		function changeView( navigationItem:INavigationItem ):Boolean;
		
		function getItemById( navigationItemId:String ):INavigationItem;
		
		function getItemByState( state:String ):INavigationItem;
		
		function getItemIndex( navigationItem:INavigationItem ):int;
		
		function initialize():void;
		
		function navigate( navigationItem:* ):void;
		
		function navigateById( navigationItemId:String ):void;
		
		function navigateToDefaultState():void;
		
		function navigateToDefaultView():void;
		
		function navigateToFirstView( navigatorId:String ):void;
		
		function register():void;
		
		function reset():void;
		
		function resetItemsToDefault( itemType:String ):void;
		
		function resetToDefault( navigationItemId:String, itemType:String ):void;
	}
}
