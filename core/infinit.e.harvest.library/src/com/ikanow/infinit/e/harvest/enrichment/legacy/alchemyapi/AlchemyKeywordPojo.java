package com.ikanow.infinit.e.harvest.enrichment.legacy.alchemyapi;


public class AlchemyKeywordPojo 
{
	public String relevance;
	public String text;
	static public class AlchemySentiment {
		public String type;
		public String score;
	}
	public AlchemySentiment sentiment;
}
