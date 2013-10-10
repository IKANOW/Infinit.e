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
