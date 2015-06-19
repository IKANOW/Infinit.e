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
package com.ikanow.infinit.e.data_model.index;

import java.util.TreeMap;

// The classes needed to create programmatic mapping objects for ElasticSearch "schemas"

public class ElasticSearchPojos {

	// See "http://www.elasticsearch.org/guide/reference/mapping/"
	
	public static class IdPojo { // "_id"
		public String store = null;
		public IdPojo(String store_) { store = store_; }
	}
	
	public static class TypePojo { // "_type"
		public String store = null;
		public TypePojo(String store_) { store = store_; }
	}
	
	public static class SourcePojo { // "_source"
		public Boolean enabled = null;
		public String[] includes = null;
		public String[] excludes = null;
		public SourcePojo(Boolean enabled_) { enabled = enabled_; } 
		public SourcePojo(Boolean enabled_, String[] includes_, String[] excludes_) 
			{ enabled = enabled_; includes = includes_; excludes = excludes_; } 
		public SourcePojo(Boolean enabled_, String... includes_) 
			{ enabled = enabled_; includes = includes_; } 
	}
	
	public static class AllPojo { // "_all"
		public Boolean enabled = null;
		public AllPojo(Boolean enabled_) { enabled = enabled_; }
	}
	
	public static class AnalyzerPojo { // "_analyzer"
		public String path = null;
		public String index = null;
		public AnalyzerPojo(String path_, String index_) { path = path_; index = index_; }
	}

	public static class BoostPojo { // "_boost"
		public String name = null;
		public Double null_value = null;
		public BoostPojo(String name_, Double null_value_) { name = name_; null_value = null_value_; }
	}

	public static class ParentPojo { // "_parent"
		public String type = null;
		public ParentPojo(String type_) { type = type_; }
	}
	
	public static class RoutingPojo { // "_routing"
		public Boolean required = null;
		public String path = null;
		public RoutingPojo(Boolean required_, String path_) { required = required_; path = path_; }
	}

	public static class IndexPojo { // "_index"
		public Boolean enabled = null;
		public IndexPojo(Boolean enabled_) { enabled = enabled_; }
	}
	
	public static class BiFieldPojo<T> { // "multi_field", with at most 2 fields ("name" and "exact")
		public BiFieldPojo(T pri_, T alt_) { fields.pri = pri_; fields.alt = alt_; }
		public final String type = "multi_field";
		public static class Fields<T> {
			public T pri; // accessed with the BiFieldPojo name, eg "myname"
			public T alt; // accessed with the BiFieldPojo name + ".alt", eg "myname.alt"
		}
		public Fields<T> fields = new Fields<T>();
	}
	
	public static class FieldStringPojo { // "string"
		public final String type = "string";
		public String index_name = null;
		public String store = null;
		public String index = null;
		public String term_vector = null;
		public Double boost = null;
		public String null_value = null;
		public Boolean omit_norms = null;
		public Boolean omit_term_freq_and_positions = null;
		public String analyzer = null;
		public String index_analyzer = null;
		public String search_analyzer = null;
		public Boolean include_in_all = null;
		public Boolean doc_values = null;
		
		public FieldStringPojo(String store_, String index_, Double boost_) { this(store_, index_, boost_, null); }
		public FieldStringPojo(String store_, String index_, Double boost_, Boolean doc_values_) { store = store_; index = index_; boost = boost_; doc_values = doc_values_; }
			// Anything more complicated, do it by hand
		
		public FieldStringPojo excludeFromAll() { include_in_all = false; return this; }
		public FieldStringPojo setTermVector(String term_vector_) { term_vector = term_vector_; return this; }
		public FieldStringPojo setAnalyzer(String analyzer_) { analyzer = analyzer_; return this; }
	}
	
	public static class FieldFloatPojo { // "float"
		public final String type = "float";
		public String index_name = null;
		public String store = null;
		public String index = null;
		public Integer precision_step = null;
		public Double boost = null;
		public Float null_value = null;
		public Boolean include_in_all = null;
		
		public FieldFloatPojo(String store_, String index_, Double boost_) { store = store_; index = index_; boost = boost_; }
				// Anything more complicated, do it by hand
		
		public FieldFloatPojo excludeFromAll() { include_in_all = false; return this; }
	}

	public static class FieldDoublePojo { // "double"
		public final String type = "double";
		public String index_name = null;
		public String store = null;
		public String index = null;
		public Integer precision_step = null;
		public Double boost = null;
		public Double null_value = null;
		public Boolean include_in_all = null;
		
		public FieldDoublePojo(String store_, String index_, Double boost_) { store = store_; index = index_; boost = boost_; }
				// Anything more complicated, do it by hand
		
		public FieldDoublePojo excludeFromAll() { include_in_all = false; return this; }
	}

	public static class FieldIntegerPojo { // "integer"
		public final String type = "integer";
		public String index_name = null;
		public String store = null;
		public String index = null;
		public Integer precision_step = null;
		public Double boost = null;
		public Integer null_value = null;
		public Boolean include_in_all = null;
		
		public FieldIntegerPojo(String store_, String index_, Double boost_) { store = store_; index = index_; boost = boost_; }
				// Anything more complicated, do it by hand

		public FieldIntegerPojo excludeFromAll() { include_in_all = false; return this; }
	}

	public static class FieldLongPojo { // "long"
		public final String type = "long";
		public String index_name = null;
		public String store = null;
		public String index = null;
		public Integer precision_step = null;
		public Double boost = null;
		public Long null_value = null;
		public Boolean include_in_all = null;
		
		public FieldLongPojo(String store_, String index_, Double boost_) { store = store_; index = index_; boost = boost_; }
				// Anything more complicated, do it by hand

		public FieldLongPojo excludeFromAll() { include_in_all = false; return this; }
	}

	
	public static class FieldDatePojo { // "date"
		public final String type = "date";
		public String index_name = null;
		public String format = null;
		public String store = null;
		public String index = null;
		public Integer precision_step = null;
		public Double boost = null;
		public String null_value = null;
		public Boolean include_in_all = null;
		
		public FieldDatePojo(String store_, String index_, Double boost_, String format_) 
			{ store = store_; index = index_; boost = boost_; format = format_; }
			// Anything more complicated, do it by hand
		
		public FieldDatePojo excludeFromAll() { include_in_all = false; return this; }
	}
	
	public static class FieldBooleanPojo { // "boolean"
		public final String type = "boolean";
		public String index_name = null;
		public String store = null;
		public String index = null;
		public Double boost = null;
		public Boolean null_value = null;
		public Boolean include_in_all = null;
		
		public FieldBooleanPojo(String store_, String index_, Double boost_) { store = store_; index = index_; boost = boost_; }
			// Anything more complicated, do it by hand

		public FieldBooleanPojo excludeFromAll() { include_in_all = false; return this; }
	}

	public static class FieldBinaryPojo { // "binary"
		public final String type = "binary";
		public String index_name = null;
		
		public FieldBinaryPojo() {}
			// Anything more complicated, do it by hand
	}
	
	public static class FieldGeoPointPojo { // "geo_point"
		public final String type = "geo_point";
		public String store = null;
		public Boolean lat_lon = null;
		public Boolean geohash = null;
		public Integer geohash_precision = null;
		
		public FieldGeoPointPojo(String store_) { store = store_; }
		public FieldGeoPointPojo(Boolean lat_lon_, Boolean geohash_, Integer geohash_precision_) 
			{ lat_lon = lat_lon_; geohash = geohash_; geohash_precision = geohash_precision_; } 
	}
	
	// A slightly less configurable dynamic type mapping:

	@SuppressWarnings("serial")
	public static class DynamicTemplateList extends TreeMap<String, DynamicTemplateList.FieldGenericTemplate> {
		public DynamicTemplateList(String name_, FieldGenericTemplate field_) {
			this.put(name_, field_);
		}
		
		public static DynamicTemplateList[] generateDefaultTemplates() {
			return generateDefaultTemplates((DynamicTemplateList[])null);
		}
		public static DynamicTemplateList[] generateDefaultTemplates(DynamicTemplateList... extra) {
			int nExtra = (null != extra)?extra.length:0;
			int nBaseFields = 12;
			DynamicTemplateList fields[] = new DynamicTemplateList[nBaseFields + nExtra];
			nBaseFields--; // (turn into an index) 
			
			fields[0] = new DynamicTemplateList("template_text", 
							new FieldGenericTemplate("*__text", 
									new FieldGenericTemplate.Mapping("string", "yes", "analyzed", null, null, null)));		
			fields[1] = new DynamicTemplateList("template_term", 
					new FieldGenericTemplate("*__term", 
							new FieldGenericTemplate.Mapping("string", "yes", "not_analyzed", null, null, null)));
			fields[2] = new DynamicTemplateList("template_comment", 
					new FieldGenericTemplate("*__comment", 
							new FieldGenericTemplate.Mapping("string", "yes", "no", null, null, null)));
			fields[3] = new DynamicTemplateList("template_discard", 
					new FieldGenericTemplate("*__discard", 
							new FieldGenericTemplate.Mapping("string", "no", "no", null, null, null)));
			
			fields[4] = new DynamicTemplateList("template_double", 
					new FieldGenericTemplate("*__double", 
							new FieldGenericTemplate.Mapping("double", "yes", null, null, null, null)));
			
			fields[5] = new DynamicTemplateList("template_long", 
					new FieldGenericTemplate("*__long", 
							new FieldGenericTemplate.Mapping("long", "yes", null, null, null, null)));
			
			fields[6] = new DynamicTemplateList("template_bool", 
					new FieldGenericTemplate("*__bool", 
							new FieldGenericTemplate.Mapping("boolean", "yes", null, null, null, null)));
			
			fields[7] = new DynamicTemplateList("template_dateISO", 
					new FieldGenericTemplate("*__dateISO", 
							new FieldGenericTemplate.Mapping("date", "yes", null, null, null, null)));
			fields[8] = new DynamicTemplateList("template_dateTimeJava", 
					new FieldGenericTemplate("*__dateTimeJava", 
							new FieldGenericTemplate.Mapping("date", "yes", null, null, null, null, 
						"MM/dd/yy hh:mm a||MM/dd/yy||MMM dd, yyyy hh:mm:ss a||MMM dd, yyyy")));
			fields[9] = new DynamicTemplateList("template_dateYYYYMMDD", 
					new FieldGenericTemplate("*__dateYYYYMMDD", 
							new FieldGenericTemplate.Mapping("date", "yes", null, null, null, null, "yyyyMMdd")));			
			fields[10] = new DynamicTemplateList("template_dateRFC822", 
					new FieldGenericTemplate("*__dateRFC822", 
							new FieldGenericTemplate.Mapping("date", "yes", null, null, null, null, "EEE, dd MMM yyyy HH:mm:ss Z")));
			fields[11] = new DynamicTemplateList("tempate_dateGMT",
					new FieldGenericTemplate("*__dateGMT",
							new FieldGenericTemplate.Mapping("date", "yes", null, null, null, null, "dd MMM yyyy HH:mm:ss 'GMT'")));
			
			// Add user fields to mapping
			if (0 != nExtra) {
				for (int i = 1; i <= nExtra; ++i) {
					fields[nBaseFields + i] = extra[i-1];
				}
			}
			
			return fields;
		}

		public static class FieldGenericTemplate {
			public String match = null;
			public String path_match = null;
			public String match_mapping_type = null;
			
			public static class Mapping {
				public String type = null;
				public String store = null;
				public String index = null;
				public Double boost = null;
				public Boolean include_in_all = null;
				public Object null_value = null;
				public String format = null; //(date only)
				public Integer ignore_above = null;
				public TreeMap<String, Mapping> fields;
				
				public Mapping(String type_, String store_, String index_, Double boost_, Boolean include_in_all_, Object null_value_) {
					type = type_; store = store_; index = index_; boost = boost_; include_in_all = include_in_all_; null_value = null_value_;
				}
				public Mapping(String type_, String store_, String index_, Double boost_, Boolean include_in_all_, Object null_value_, String format_) {
					type = type_; store = store_; index = index_; boost = boost_; include_in_all = include_in_all_; null_value = null_value_; format = format_;
				}
				public Mapping(String type_, String store_, String index_, Double boost_, Boolean include_in_all_, Object null_value_, String format_, Integer ignore_above_) {
					type = type_; store = store_; index = index_; boost = boost_; include_in_all = include_in_all_; null_value = null_value_; format = format_; ignore_above = ignore_above_;
				}
				public Mapping(String type_, String store_, String index_, Double boost_, Boolean include_in_all_, Object null_value_, String format_, Integer ignore_above_, TreeMap<String, Mapping> fields_) {
					type = type_; store = store_; index = index_; boost = boost_; include_in_all = include_in_all_; null_value = null_value_; format = format_; ignore_above = ignore_above_; fields =fields_;
				}
			}
			public Mapping mapping = null;
			
			public FieldGenericTemplate(String match_, Mapping mapping_) { match = match_; mapping = mapping_; }			
			public FieldGenericTemplate(String match_, String pathMatch_, Mapping mapping_) { match = match_; path_match = pathMatch_; mapping = mapping_; }			
			public FieldGenericTemplate(String match_, String pathMatch_, String matchMappingType_, Mapping mapping_) { match = match_; path_match = pathMatch_; match_mapping_type = matchMappingType_; mapping = mapping_; }			
		}
	}
}
