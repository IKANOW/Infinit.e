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
package com.ikanow.infinit.e.data_model.api.knowledge;

import java.util.HashMap;

import org.bson.types.ObjectId;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

import com.ikanow.infinit.e.data_model.api.BaseApiPojo;

//import com.ibm.db2.jcc.sqlj.e;

/**
 * Used for establishing query statistics
 * @author root
 *
 */
public class StatisticsPojo extends BaseApiPojo {

	public Long found = 0L;
	public Long start = 0L;
	public float maxScore = (float)0.0;
	public float avgScore = (float)0.0;
	
	public static class Score {
		public double score;
		public double decay = -1.0;
	}
	
	private HashMap<ObjectId, Score> scoring = null;
	private ObjectId ids[] = null;
	
	public HashMap<ObjectId, Score> getScore() {
		return this.scoring;
	}
	public ObjectId[] getIds() {
		return ids;		
	}
	public void resetArrays() {
		ids = null;
		scoring = null;
	}
	
	
	public void setScore(SearchHits elasticHits, boolean bDecay) {
        if (Float.isNaN(maxScore)) {
        	maxScore = (float) 0.0;
        }
		long nHits = elasticHits.hits().length;
		scoring = new HashMap<ObjectId, Score>();
		if (nHits > 0) { 
			int i = 0;
			ids = new ObjectId[(int)nHits];
			for(SearchHit hit: elasticHits) {
				String idStr = hit.getId();
				Score scoreObj = new Score();
				scoreObj.score = (double)hit.getScore();
			    if (Double.isNaN(scoreObj.score)) {
			    	scoreObj.score = (double) 0.0;
			    }					
			    avgScore += scoreObj.score;
			    if (bDecay) {
			    	SearchHitField decay = hit.getFields().get("decay");
			    	if (null != decay) {
			    		scoreObj.decay = (Double)decay.value();
			    	}
			    }
			    ObjectId id = new ObjectId(idStr);
				scoring.put(id, scoreObj);
				ids[i++] = id;
	        }
			avgScore /= nHits;
		}
	}	
}


