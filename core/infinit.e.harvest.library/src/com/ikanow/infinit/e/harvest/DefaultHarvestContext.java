/*******************************************************************************
 * Copyright 2015, The Infinit.e Open Source Project.
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
package com.ikanow.infinit.e.harvest;

import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.harvest.extraction.document.DuplicateManager;
import com.ikanow.infinit.e.harvest.extraction.document.HarvestStatus;
import com.ikanow.infinit.e.harvest.extraction.document.HarvestStatus_Integrated;

public class DefaultHarvestContext implements HarvestContext {
	private HarvestStatus _harvestStatus = new HarvestStatus_Integrated(); // (can either be standalone or integrated, defaults to standalone)

	@Override
	public DuplicateManager getDuplicateManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HarvestStatus getHarvestStatus() {
		// TODO Auto-generated method stub
		return _harvestStatus;
	}

	@Override
	public boolean isStandalone() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public int getStandaloneMaxDocs() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IkanowSecurityManager getSecurityManager() {
		// TODO Auto-generated method stub
		return null;
	}

}
