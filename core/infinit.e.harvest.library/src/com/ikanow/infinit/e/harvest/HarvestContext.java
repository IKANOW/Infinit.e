package com.ikanow.infinit.e.harvest;

import com.ikanow.infinit.e.harvest.extraction.document.DuplicateManager;
import com.ikanow.infinit.e.harvest.extraction.document.HarvestStatus;

public interface HarvestContext {
	DuplicateManager getDuplicateManager();
	HarvestStatus getHarvestStatus();
}
