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

import com.ikanow.infinit.e.data_model.api.BaseApiPojo;

// Fakes the elasticsearch search reply (specifically for dashboards)

public class DashboardProxyResultPojo extends BaseApiPojo {
	public Boolean found;
	public Boolean created;
	public String _index = "kibana-int";
	public String _type = "dashboard";
	public String _id;
	public Integer _version = 1;
	public DashboardPojo _source; 
}
