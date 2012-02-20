package actionscript
{
	import org.un.cava.birdeye.ravis.components.renderers.edgeLabels.BaseEdgeLabelRenderer;
	
	public class EdgeLabelTooltipRenderer extends BaseEdgeLabelRenderer
	{
		public function EdgeLabelTooltipRenderer()
		{
			super();
		}
		
		/**
		 * @inheritDoc
		 * */
		override protected function initTopPart():void {
			
			/* create the top part using super class */
			super.initTopPart();
			
			this.toolTip = this.data.data.@tooltip;
		}
	}
}