package com.ikanow.infinit.e.harvest.enrichment.legacy.alchemyapi;
import java.util.List;



public class AlchemyPojo 
{
	public String status;
	public String usage;
	public String url;
	public String language;
	public String text;
	//List of entities
	public List<AlchemyEntityPojo> entities;
	public List<AlchemyKeywordPojo> keywords;
	
}
