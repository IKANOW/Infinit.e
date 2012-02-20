package com.ikanow.infinit.e.harvest.enrichment.legacy.alchemyapi;
import java.util.ArrayList;
import java.util.List;


public class AlchemyEntityPojo 
{
	public String type;
	public String relevance;
	public String count;
	public String text;
	static public class AlchemySentiment {
		public String type;
		public String score;
	}
	public AlchemySentiment sentiment;
	public static class AlchemyDisambig 
	{
		public String name;
		public String geo;
		public String dbpedia;
		public String census;
		public String ciaFactbook;
		public String freebase;
		public String umbel;
		public String opencyc;
		public String yago;
	}
	public AlchemyDisambig disambiguated;
	public List<String> quotations = new ArrayList<String>();
}
