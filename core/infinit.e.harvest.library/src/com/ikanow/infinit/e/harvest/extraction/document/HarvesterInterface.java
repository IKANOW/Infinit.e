package com.ikanow.infinit.e.harvest.extraction.document;

import java.util.List;

import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.HarvestContext;

public interface HarvesterInterface 
{
	boolean canHarvestType(int sourceType);
	void executeHarvest(HarvestContext context, SourcePojo source, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate, List<DocumentPojo> toRemove);
}
