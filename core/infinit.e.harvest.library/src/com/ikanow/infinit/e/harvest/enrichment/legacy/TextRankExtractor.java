package com.ikanow.infinit.e.harvest.enrichment.legacy;

import java.util.ArrayList;
import java.util.Collection;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.interfaces.harvest.EntityExtractorEnum;
import com.ikanow.infinit.e.data_model.interfaces.harvest.IEntityExtractor;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.sharethis.textrank.MetricVector;
import com.sharethis.textrank.TextRank;

public class TextRankExtractor implements IEntityExtractor {

	public ThreadLocal<TextRank> processor = new ThreadLocal<TextRank>() {
        @Override protected TextRank initialValue() {
        	try {
				return new TextRank(Globals.getConfigLocation(), "en");
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
        }
	}; 
	
	@Override
	public String getName() {
		return "textrank";
	}

	@Override
	public void extractEntities(DocumentPojo partialDoc)
	{
		try {
			
			processor.get().prepCall(partialDoc.getFullText(), false);			
			Collection<MetricVector> results = processor.get().call();
			int nSize = results.size();
			if (null == partialDoc.getEntities()) {
				partialDoc.setEntities(new ArrayList<EntityPojo>(nSize));
			}
			
			for (MetricVector res: results) {
				if ((nSize > 50) && (res.metric < 0.1)) {
					continue; // some very basic filtering
				}
				if ((nSize > 100) && (res.metric < 0.2)) {
					continue; // more aggressive filtering
				}
				if (Double.isInfinite(res.metric) || Double.isNaN(res.metric)) {
					continue;
				}
				EntityPojo entity = new EntityPojo();
				entity.setDimension(EntityPojo.Dimension.What);
				entity.setType("Keyword");
				entity.setDisambiguatedName(res.value.text);
				entity.setActual_name(res.value.text);
				entity.setFrequency(1L);
				entity.setRelevance(res.metric);
				partialDoc.getEntities().add(entity);
				
				//DEBUG
				//System.out.println(res.value.text + ": " + res.metric + "/" + res.link_rank + "/" + res.count_rank);
			}
		}
		catch (Exception e) {

			//DEBUG
			//e.printStackTrace();
		}
	}

	@Override
	public void extractEntitiesAndText(DocumentPojo partialDoc)
			throws ExtractorDailyLimitExceededException,
			ExtractorDocumentLevelException {
		//cannot extract from url, not implemented
	}

	@Override
	public String getCapability(EntityExtractorEnum capability) {
		return null;
	}

}
