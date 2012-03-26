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
package com.ikanow.infinit.e.core.execute_harvest.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

// Pauses or exists instead of scheduling threads with no available pool entries

public class BlockingExecutor {  
	private final Executor _executor;  
	private final Semaphore _semaphore;  
	private static class Binding {
		private int nTargetBound;
		private int nCurrBound;
	} 
	private final Binding _binding = new Binding();
	public BlockingExecutor(Executor exec, int nBound) {
		this._executor = exec;  
		this._semaphore = new Semaphore(nBound);  
		this._binding.nTargetBound = nBound;
		this._binding.nCurrBound = nBound;
	}  
	public void updateBlockingSettings(int nNewBound) {
		this._binding.nTargetBound = nNewBound;
		while (nNewBound > this._binding.nCurrBound) {
			_semaphore.release();
			this._binding.nCurrBound++;
		}
		// otherwise need to wait for the existing semaphores to go away 
	}
	public boolean submitTask(final Runnable command, boolean bBlock)  
		throws InterruptedException
	{  
		if (bBlock) {
			_semaphore.acquire();
		}
		else if (!_semaphore.tryAcquire()) {
			return false;
		}
		try {  
			_executor.execute(new Runnable() {  
				public void run() {  
					try {  
						command.run();  
					} 
					finally {  
						if (_binding.nTargetBound == _binding.nCurrBound) {
							_semaphore.release();
						}
						else {
							_binding.nCurrBound--;
						}
					}  
				}  
			});  
		} 
		catch (RejectedExecutionException e) {  
			if (_binding.nTargetBound == _binding.nCurrBound) {
				_semaphore.release();
			}
			else {
				_binding.nCurrBound--;
			}
		}  
		return true;
	}  
}  
