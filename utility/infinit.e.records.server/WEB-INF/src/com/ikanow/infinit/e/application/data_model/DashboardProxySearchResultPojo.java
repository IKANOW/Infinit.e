/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project sponsored by IKANOW.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.application.data_model;

import java.util.List;

import com.ikanow.infinit.e.data_model.api.BaseApiPojo;

// Fakes the elasticsearch search reply (specifically for dashboards)

public class DashboardProxySearchResultPojo extends BaseApiPojo {
	public static class Hits {
		public long total;
		public double max_score = 1.0;
		public static class HitElement {
			public String _id; // title
			public String _index = "kibana-int";
			public String _type = "dashboard";
			public double _score = 1.0;
			public static class HitElementSource {
				public String user = "guest"; // (hardwired for now)
				public String group = "guest"; // (hardwired for now)
				public String title;
				public String dashboard; // share json
			}
			public DashboardPojo _source = new DashboardPojo("guest", "guest"); // (hardwired for know) 
		}
		public List<HitElement> hits;
	}
	public Hits hits; 
}
