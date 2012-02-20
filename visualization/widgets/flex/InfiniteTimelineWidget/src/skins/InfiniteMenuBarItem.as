package skins {
	import mx.controls.MenuBar;
	import mx.controls.menuClasses.MenuBarItem;
	import mx.controls.Alert;
	
	public class InfiniteMenuBarItem extends MenuBarItem
	{
		override public function set menuBarItemState(value:String):void
		{
			super.menuBarItemState = value;
			
			if (!label)
				return;
			
			if (value == "itemOverSkin")
				label.textColor = 0x000000;
			else if (value == "itemDownSkin")
				label.textColor = 0x000000;
			else
				label.textColor = data.@color;
		}    		
	};
	
}