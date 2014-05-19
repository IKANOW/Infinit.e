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

// Fakes the Kibana Dashboard format

public class DashboardPojo extends BaseApiPojo {
	public DashboardPojo() {}
	public DashboardPojo(String user_, String group_) { user = user_; group = group_; }
	public String user;
	public String group;
	public String title;
	public String dashboard; // share json
}
