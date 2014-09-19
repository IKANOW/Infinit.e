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
package com.ikanow.infinit.e.api.test;

import java.util.Iterator;

import com.ikanow.infinit.e.api.knowledge.processing.ScoringUtils.TempDocBucket;

public class ScoringUtilsTestCode {

	public static void main(String[] args) {
		
		// 1] Test the custom use of a TreeSet as a PriorityQueue replacement (with approximate score matching)
		
		int n1Down = 0;
		java.util.TreeSet<TempDocBucket> pq = new java.util.TreeSet<TempDocBucket>();
		for (int i = 0; i < 100; i += 10) {
			TempDocBucket tdb = new TempDocBucket();
			tdb.nTieBreaker = n1Down--;
			tdb.totalScore = (double)i;
			tdb.url = ((Integer)i).toString();
			pq.add(tdb);
		}
		
		for (int i = 0; i < 100; i += 5) {
			System.out.println("Inserting " + i);
			TempDocBucket tdb = new TempDocBucket();
			tdb.nTieBreaker = n1Down--;
			tdb.totalScore = (double)i - 0.8;
			tdb.url = ((Integer)i).toString();
			if (pq.add(tdb)) {
				System.out.println("DUPS 2=" + i + "/" + pq.size());
			}
			if (pq.size() > 10) {
				System.out.println("Remove item: " + pq.first().url);
				Iterator<TempDocBucket> it = pq.iterator();				
				System.out.println("Double check: " + it.next().url);
				it.remove();
				System.out.println("First item is now: " + pq.first().url);
			}
		}

		for (int i = 0; i < 100; i += 20) {
			System.out.println("Inserting " + i);
			TempDocBucket tdb = new TempDocBucket();
			tdb.nTieBreaker = n1Down--;
			tdb.totalScore = (double)i - 0.8;
			tdb.url = ((Integer)i).toString();
			if (pq.add(tdb)) {
				System.out.println("DUPS 2=" + "i" + pq.size());
			}
		}
		System.out.println("FINAL OBJECT = " + new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(pq));
	}
}
