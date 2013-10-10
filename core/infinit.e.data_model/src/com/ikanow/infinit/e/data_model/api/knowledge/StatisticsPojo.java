/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
		public Object explain;
		public double score;
		public double decay = -1.0;
		public int nIndex;
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
	
	
	public void setScore(SearchHits elasticHits, boolean bDecay, boolean bExplain) {
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
				if (bExplain) {
					scoreObj.explain = hit.getExplanation();
				}
				scoreObj.nIndex = i;
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


