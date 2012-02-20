package com.ikanow.infinit.e.data_model.index;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterIndexHealth;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.settings.UpdateSettingsRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.client.action.get.GetRequestBuilder;
import org.elasticsearch.client.action.index.IndexRequestBuilder;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import org.elasticsearch.index.get.GetField;
import org.elasticsearch.index.query.BaseFilterBuilder;
import org.elasticsearch.index.query.BaseQueryBuilder;
import org.elasticsearch.node.NodeBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.utils.PropertiesManager;

///////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////

//
// Class to encapsulate away the worst of the ElasticSearch complexity
//

public class ElasticSearchManager {
	
	///////////////////////////////////////////////////////////////////////////////////////

	// Get a pointer to the index
	// @param sIndexName - the index name (or optionally, a comma separated list of index names followed by "/" followed by the index type
	// @param hostAndPort - optional hostname and port "hostname/port" to connect to remotely (else gets from properties)
	
	public static synchronized ElasticSearchManager getIndex(String sIndexName, String hostAndPort)
	{
		ElasticSearchManager toReturn = _indexes.get(sIndexName);		
		if (null == toReturn) {
			String[] nameAndType = sIndexName.split("\\s*/\\s*");
			if (nameAndType.length > 1) {				
				String[] multiIndex = nameAndType[0].split("\\s*,\\s*");
				if (multiIndex.length > 1) {
					toReturn = new ElasticSearchManager(multiIndex, nameAndType[1], true, hostAndPort);
				}
				else {
					toReturn = new ElasticSearchManager(multiIndex[0], nameAndType[1], true, hostAndPort, null, null);
				}
			}
			else {
				toReturn = new ElasticSearchManager(sIndexName, sIndexName, true, hostAndPort, null, null);
			}
			_indexes.put(sIndexName, toReturn);			
		}				
		return toReturn;
	}//TESTED
	
	//////////////////////////////////////////////////////////////

	// (Tidier version)
	
	public static ElasticSearchManager getIndex(String sIndexName) {
		return getIndex(sIndexName, null);
	}//TESTED
	
	//////////////////////////////////////////////////////////////

	// (Child version)
	
	public synchronized ElasticSearchManager getChildIndex(String sIndexName) {
		if (null == _childIndexes) {
			_childIndexes = new TreeMap<String, ElasticSearchManager>();
		}
		ElasticSearchManager childToReturn = _childIndexes.get(sIndexName);
		if (null == childToReturn) {
			if (null != _multiIndex) {
				childToReturn = new ElasticSearchManager(_multiIndex, sIndexName, _elasticClient, null);								
			}
			else {
				childToReturn = new ElasticSearchManager(_sIndexName, sIndexName, _elasticClient, null);				
			}
			_childIndexes.put(sIndexName, childToReturn);
		}
		return childToReturn;
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////////////////

	// Get a pointer to the index (don't use this one, it only works if the cluster is not running on the machine itself but is on the LAN)
	
	public static synchronized ElasticSearchManager getIndexCluster(String sIndexName) {
		ElasticSearchManager toReturn = _indexes.get(sIndexName);		
		if (null == toReturn) {
			toReturn = new ElasticSearchManager(sIndexName, sIndexName, false, null, null, null);
			_indexes.put(sIndexName, toReturn);			
		}				
		return toReturn;		
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////////////////
	
	public boolean pingIndex() {
		try {
			_elasticClient.admin().cluster().health(new ClusterHealthRequest(_sIndexName).waitForYellowStatus()).actionGet();
		}
		catch (Exception e) { // Index not alive...
			return false;
		}
		return true;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////

	// Encapsulation: Add a document to the index (takes away JSON unpleasantness!)
	// @param doc: the POJO 
	// @param _id: the key (normally from docJson) to be used as the primary key within the index (if null, auto-created)
	// @param bAllowOverwrite: if false will fail if document alreadty exists
	// @param sParentId: (optional) if child document then use this to pass parent ID in
	
	public <U> boolean addDocument(U doc, BasePojoIndexMap<U> docMap, String _id, boolean bAllowOverwrite) {
		return addDocument(doc, docMap, _id, bAllowOverwrite, null);
	}
	public <U> boolean addDocument(U doc, BasePojoIndexMap<U> docMap, String _id, boolean bAllowOverwrite, String sParentId) {
		
		JsonElement docJson = IndexManager.mapToIndex(doc, docMap); 
		return this.addDocument(docJson, _id, bAllowOverwrite, sParentId);
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////////////////

	// Encapsulation: Add a document to the index
	// @param sDocJson: the JSON document to index
	// @param _id: the key (normally from docJson) to be used as the primary key within the index (if null, auto-created)
	// @param bAllowOverwrite: if false will fail if document alreadty exists
	// @param sParentId: (optional) if child document then use this to pass parent ID in
	
	public boolean addDocument(JsonElement docJson, String _id, boolean bAllowOverwrite) {
		return addDocument(docJson, _id, bAllowOverwrite, null);
	}
	public boolean addDocument(JsonElement docJson, String _id, boolean bAllowOverwrite, String sParentId) {
		
		if (null != _multiIndex) {
			throw new RuntimeException("addDocument not supported on multi-index manager");
		}
		IndexRequestBuilder irb = _elasticClient.prepareIndex(_sIndexName, _sIndexType).setSource(docJson.toString());
		if (null != _id) {
			irb.setId(_id);
		}//TESTED
		else { // If an _id is already specified use that
			JsonElement _idJson = docJson.getAsJsonObject().get("_id");
			if (null != _idJson) {
				_id = _idJson.getAsString();
				if (null != _id) {
					irb.setId(_id);
				}				
			}
		}//TOTEST
		
		if (!bAllowOverwrite) {
			irb.setOpType(OpType.CREATE);
		}//TESTED
		
		if (null != sParentId) {
			irb.setParent(sParentId);
		}
		
		// This ensures that the write goes through if I can write to any nodes, which seems sensible
		// You could always check the response and handle minimal success like failure if you want
		irb.setConsistencyLevel(WriteConsistencyLevel.ONE);
	
		try {
			irb.execute().actionGet();
		}
		catch (org.elasticsearch.transport.RemoteTransportException e) {
			if (!e.contains(org.elasticsearch.index.engine.DocumentAlreadyExistsEngineException.class)) {
				throw e;
			}
			return false;
		}
		return true;
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////////////////

	// Encapsulation: Get a document from the index by ID
	// @param _id: the document primary key
	// @param sFields: the list of fields to return (can be null - will just return null if doc doesn't exist)
	// @returns: A map containing the requested fields (null if empty)
	
	public Map<String, GetField> getDocument(String _id, String... sFields) {
		GetRequestBuilder grb = _elasticClient.prepareGet(_sIndexName, _sIndexType, _id);
		if (null != sFields) {
			grb.setFields(sFields);
		}
		GetResponse gr = grb.execute().actionGet();
		Map<String, GetField> fieldsMap = gr.fields();
		if (null != fieldsMap) {
			if (fieldsMap.isEmpty()) {
				fieldsMap = null;
			}
		}
		return fieldsMap;
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////////////////

	// Encapsulation: Remove a document from the index by ID
	// @param _id: the document primary key
	// @param sParentId: (optional) the parent ID, if deleting a child document
	// @returns: true if successfully deleted, false if not found
	
	public boolean removeDocument(String _id) {
		return removeDocument(_id, null);
	}
	public boolean removeDocument(String _id, String sParentId) {
		if (null != _multiIndex) {
			throw new RuntimeException("removeDocument not supported on multi-index manager");
		}
		DeleteRequestBuilder drb = _elasticClient.prepareDelete(_sIndexName, _sIndexType, _id);
		if (null != sParentId) {
			drb.setRouting(sParentId);
		}
		drb.setConsistencyLevel(WriteConsistencyLevel.ONE);
		DeleteResponse dr = drb.execute().actionGet();
		return !dr.notFound();
		
	}//TESTED (including children)
		
	///////////////////////////////////////////////////////////////////////////////////////

	// Slightly less encapsulated: the query call
	// @param XContentQueryBuilder - a JSON representation of the query, obtained from QueryBuilders
	// @param SearchRequestBuilder - search options obtained from getSearchParams() - can be null
	// @returns nasty search object - you're on your own with that... (null if fails i guess?)
	
	public SearchResponse doQuery(BaseQueryBuilder queryJsonObj)
	{
		return doQuery(queryJsonObj, null, null);
	}
	public SearchResponse doQuery(BaseQueryBuilder queryJsonObj, BaseFilterBuilder filterJsonObj)
	{
		return doQuery(queryJsonObj, filterJsonObj, null);
	}
	public SearchResponse doQuery(BaseQueryBuilder queryJsonObj, SearchRequestBuilder queryOptions)
	{
		return doQuery(queryJsonObj, null, queryOptions);
	}
	public SearchResponse doQuery(BaseQueryBuilder queryJsonObj, BaseFilterBuilder filterJsonObj, SearchRequestBuilder queryOptions)
	{
		if (null == queryOptions) {
			if (null != _multiIndex) {
				queryOptions = _elasticClient.prepareSearch().setIndices(_multiIndex).setTypes(_sIndexType);
			}
			else {
				queryOptions = _elasticClient.prepareSearch(_sIndexName).setTypes(_sIndexType);				
			}
		}
		if (null != queryJsonObj) { // (power users can do this themselves)
			queryOptions.setQuery(queryJsonObj);
		}
		if (null != filterJsonObj) {
			queryOptions.setFilter(filterJsonObj);
		}
		return queryOptions.execute().actionGet();
	}//TESTED
	
	public SearchRequestBuilder getSearchOptions() {
		if (null != _multiIndex) {
			return _elasticClient.prepareSearch().setIndices(_multiIndex).setTypes(_sIndexType);			
		}
		else {
			return _elasticClient.prepareSearch(_sIndexName).setTypes(_sIndexType);
		}
	}//TESTED
	
	// Scrolling search:
	
	public SearchResponse doScrollingQuery(String sScrollId, String sKeepAlive) {
		return _elasticClient.prepareSearchScroll(sScrollId).setScroll(sKeepAlive).execute().actionGet();
	}
	
	///////////////////////////////////////////////////////////////////////////////////////

	// Encapsulation: Bulk add documents
	// @param docs - list of documents
	// @param idFieldName - the fieldname within the JSON of the id (can be null)
	// @param idHashMap - map of ids vs doc if there's no field name (can be null - id auto generated)
	// @param sParentId: (optional) the parent ID, if adding child documents
	// @returns: an ElasticSearch BulkResponse
	
	public <T> BulkResponse bulkAddDocuments(List<T> docs, BasePojoIndexMap<T> mapper, TypeToken<? extends List<T>> typeToken, String idFieldName, boolean bAllowOverwrite)
	{
		return bulkAddDocuments(docs, mapper, typeToken, idFieldName, null, bAllowOverwrite);
	}
	public <T> BulkResponse bulkAddDocuments(List<T> docs, BasePojoIndexMap<T> mapper, TypeToken<? extends List<T>> typeToken, String idFieldName, String sParentId, boolean bAllowOverwrite)
	{
		JsonElement jsonDocs = IndexManager.mapListToIndex(docs, typeToken, mapper);
		return bulkAddDocuments(jsonDocs, idFieldName, sParentId, bAllowOverwrite);
	}
	public BulkResponse bulkAddDocuments(JsonElement docsJson, String idFieldName, String sParentId, boolean bAllowOverwrite)
	{
		if (null != _multiIndex) {
			throw new RuntimeException("bulkAddDocuments not supported on multi-index manager");
		}
		if (!docsJson.isJsonArray()) {
			throw new RuntimeException("bulkAddDocuments - not a list");
		}
		BulkRequestBuilder brb = _elasticClient.prepareBulk();
		
		JsonArray docJsonArray = docsJson.getAsJsonArray();
		for (JsonElement docJson: docJsonArray) {
			IndexRequest ir = new IndexRequest();
			ir.index(_sIndexName);
			ir.type(_sIndexType);
			if (null != sParentId) {
				ir.parent(sParentId);
			}
			if (!bAllowOverwrite) {
				ir.opType(OpType.CREATE);
			}//TESTED
			
			// Some _id unpleasantness
			if (null != idFieldName) {
				
				String id = docJson.getAsJsonObject().get(idFieldName).getAsString();
				ir.id(id);
				ir.source(docJson.toString());
			}//TESTED
			
			brb.add(ir);
		}
		brb.setConsistencyLevel(WriteConsistencyLevel.ONE);
		return brb.execute().actionGet();
	}//TESTED (including children and id hashmap)
	
	///////////////////////////////////////////////////////////////////////////////////////

	// Encapsulation: Bulk delete documents
	// @param ids: list of IDs to delete
	// @param sParentId: (optional) the parent ID, if deleing child documents
	// @returns: an ElasticSearch BulkResponse
	
	public BulkResponse bulkDeleteDocuments(List<String> ids)
	{
		return bulkDeleteDocuments(ids, null);
	}
	public BulkResponse bulkDeleteDocuments(List<String> ids, String sParentId)
	{
		if (null != _multiIndex) {
			throw new RuntimeException("bulkDeleteDocuments not supported on multi-index manager");
		}
		BulkRequestBuilder brb = _elasticClient.prepareBulk();
		for (String id: ids) {
			
			DeleteRequest dr = new DeleteRequest();
			dr.index(_sIndexName);
			dr.type(_sIndexType);
			dr.id(id);
			if (null != sParentId) {
				dr.parent(sParentId);
			}
			brb.add(dr);
		}
		brb.setConsistencyLevel(WriteConsistencyLevel.ONE);
		
		return brb.execute().actionGet();
	}//TESTED (inc children)
	
	///////////////////////////////////////////////////////////////////////////////////////

	// Delete by query
	// @param XContentQueryBuilder - a JSON representation of the query, obtained from QueryBuilders
	// @param DeleteByQueryRequestBuilder - search options obtained from getSearchOptionsForDelete() - can be null
	// @param sParentId - for child objects, allows more efficient routing to the individual shard
	// @returns nasty search object - you're on your own with that... (null if fails i guess?)
	
	public DeleteByQueryResponse doDeleteByQuery(BaseQueryBuilder queryJsonObj) {
		return doDeleteByQuery(queryJsonObj, null, null);
	}
	public DeleteByQueryResponse doDeleteByQuery(BaseQueryBuilder queryJsonObj, DeleteByQueryRequestBuilder queryOptions)
	{
		return doDeleteByQuery(queryJsonObj, queryOptions, null);		
	}
	public DeleteByQueryResponse doDeleteByQuery(BaseQueryBuilder queryJsonObj, String sParentId) {
		return doDeleteByQuery(queryJsonObj, null, sParentId);
	}
	public DeleteByQueryResponse doDeleteByQuery(BaseQueryBuilder queryJsonObj, DeleteByQueryRequestBuilder queryOptions, String sParentId)
	{
		if (null == queryOptions) {
			if (null != _multiIndex) {
				queryOptions = _elasticClient.prepareDeleteByQuery(_multiIndex).setTypes(_sIndexType);				
			}
			else {
				queryOptions = _elasticClient.prepareDeleteByQuery(_sIndexName).setTypes(_sIndexType);
			}
		}
		if (null != sParentId) {
			queryOptions.setRouting(sParentId);
		}
		queryOptions.setQuery(queryJsonObj);
		queryOptions.setConsistencyLevel(WriteConsistencyLevel.ONE);
		return queryOptions.execute().actionGet();
	}//TESTED
	
	public DeleteByQueryRequestBuilder getSearchOptionsForDelete() {
		if (null != _multiIndex) {
			return _elasticClient.prepareDeleteByQuery(_multiIndex).setTypes(_sIndexType);
		}
		else {
			return _elasticClient.prepareDeleteByQuery(_sIndexName).setTypes(_sIndexType);
		}
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////////////////

	// Encapsulation: Create an index if not already present
	// (like getIndex+...)
	// @param  sIndexType - if different to sIndexName
	// @param indexSettings - the ElasticSearch settings to use (eg "number_of_replicas", "number_of_shards")
	// @param sMapping - optional mapping object to define how Pojos are added (else does something sensible)
	// @param bJoinCluster - ALWAYS set to false unless running on a standalone machine in the cluster
	// @returns - null if the index exists or the newly created manager
	
	public static synchronized ElasticSearchManager createIndex(String sIndexName, String sIndexType, boolean bJoinCluster, String hostAndPort, 
			String sMapping, Builder indexSettings)
	{
		if (null == sIndexType) { // defaults
			sIndexType = sIndexName;
		}
		ElasticSearchManager toReturn = _indexes.get(sIndexName);
		if (null == toReturn) {
			if (null == indexSettings) {
				indexSettings = ImmutableSettings.settingsBuilder();
			}
			toReturn = new ElasticSearchManager(sIndexName, sIndexType, !bJoinCluster, hostAndPort, indexSettings, sMapping);
			
			_indexes.put(sIndexName, toReturn);
			return toReturn;
		}
		else { // This way, lets you know if you're trying to overwrite an existing index, just use getIndex
				// to get the pointer if you don't care
			return null; 
		}
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////////////////
	
	// As above, but creates a child index to the specified parent
	
	public ElasticSearchManager createChildIndex(String sChildIndex, String sMapping, Builder indexSettings)
	{
		if (null != _multiIndex) {
			throw new RuntimeException("createChildIndex not supported on multi-index manager");
		}
		if (null == _childIndexes) {
			_childIndexes = new TreeMap<String, ElasticSearchManager>();
		}
		
		ElasticSearchManager toReturn;
		if (null != (toReturn = _childIndexes.get(sChildIndex))) {
			return null;			
		}
		else { // Create a child index
			
			toReturn = new ElasticSearchManager(_sIndexName, sChildIndex, _elasticClient, sMapping);
			_childIndexes.put(sChildIndex, toReturn);
		}
		return toReturn;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////

	// Encapsulation: Deletes an index if present
	
	public synchronized boolean deleteMe() {
		if (null != _multiIndex) {
			throw new RuntimeException("createChildIndex not supported on multi-index manager");
		}
		try {
			if (null != _childIndexes) {
				_childIndexes.clear();
			}
			_indexes.remove(_sIndexName);
			_elasticClient.admin().indices().delete(new DeleteIndexRequest(_sIndexName)).actionGet();
		}
		catch (Exception e) {
			//Probably fine, just that the index doesn't exist...
			return false;
		}
		return true;
	}//TESTED
	
	///////////////////////////////////////////////////////////////////////////////////////
	
	// Add and remove aliases:
	
	public void createAlias(String sAliasName) {
		try {
			IndicesAliasesRequest iar = new IndicesAliasesRequest();
			iar.addAlias(_sIndexName, sAliasName);
			_elasticClient.admin().indices().aliases(iar).actionGet();
		}
		catch (Exception e) {
			// Don't worry if this fails, probably just already exists
		}
	}
	
	public void removeAlias(String sAliasName) {
		try {
			IndicesAliasesRequest iar = new IndicesAliasesRequest();
			iar.removeAlias(_sIndexName, sAliasName);
			_elasticClient.admin().indices().aliases(iar).actionGet();
		}
		catch (Exception e) {
			// Don't worry if this fails, probably just doesn't exist
		}
	}
	//TESTED
	
	///////////////////////////////////////////////////////////////////////////////////////

	// The raw interface to the ElasticSearch client, to allow arbitrary accesses:
	// Index creation/deletion
	// Document indexing/update/deletion
	// Query
	
	public Client getRawClient() { return _elasticClient; }
	public String getIndexName() { return _sIndexName; }
	
	// (Will normally just leave as default - note only really supports 1 cluster per process)
	public static synchronized String getClusterName() { return _clusterName; }
	public static synchronized void setClusterName(String clusterName) { _clusterName = clusterName; }
	
	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////

	// Utility code
	
	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	// Static state
	
	private static Map<String, ElasticSearchManager> _indexes = new HashMap<String, ElasticSearchManager>();
	private static String _clusterName = null;
	private static String _defaultClusterName = "infinite-cluster";
	public static void setDefaultClusterName(String defaultClusterName) { _defaultClusterName = defaultClusterName; }
	
	private static boolean _bLocalMode = false;
	public static void setLocalMode(boolean bLocalMode) { _bLocalMode = bLocalMode; }
	public static boolean getLocalMode() { return _bLocalMode; }
	
	// Per index state
	
	private Client _elasticClient = null;
	private String _sIndexName = null;
	private String _sIndexType = null;
	private String _multiIndex[] = null;
	
	// Child index state (first for parents, second for children)
	private Map<String, ElasticSearchManager> _childIndexes = null; 
	
	///////////////////////////////////////////////////////////////////////////////////////
	
	// Construction
	
	protected ElasticSearchManager() { } // Not allowed
	
	// Standard single-index code
	
	private ElasticSearchManager(String sIndexName, String sIndexType, boolean bRemote, String hostAndPort, 
			Builder settings, String sMapping) {
		
	// (0. cluster management)
		
		PropertiesManager properties = new PropertiesManager(); 
		
		if (null == _clusterName) {
			try {
				_clusterName = properties.getElasticCluster();
				if (null == _clusterName) {
					_clusterName = _defaultClusterName;
				}
			}
			catch (Exception e) {
				_clusterName = _defaultClusterName;				
			}
		}
		
	// 1. Link to client (or cluster)
			
		if (_bLocalMode) {
			NodeBuilder nBuilder = NodeBuilder.nodeBuilder().local(true);
			_elasticClient = nBuilder.node().client();
		}
		else if (bRemote) {
			
			String sHostname = null;
			String sPort = null;
			
			if (null == hostAndPort) {
				hostAndPort = new PropertiesManager().getElasticUrl();
			}		
			String[] hostPort = hostAndPort.split("[:/]");
			sHostname = hostPort[0];
			sPort = hostPort[1];

			Builder globalSettings = ImmutableSettings.settingsBuilder();
			Settings snode = globalSettings.put("cluster.name", _clusterName).build();
			TransportClient tmp = new TransportClient(snode);
			_elasticClient = tmp.addTransportAddress(new InetSocketTransportAddress(sHostname, Integer.parseInt(sPort)));
			
		} //TESTED
		else { // Create a "no data" cluster 
			
			Builder globalSettings = ImmutableSettings.settingsBuilder();
			Settings snode = globalSettings.put("cluster.name", _clusterName).build();

			NodeBuilder nBuilder = NodeBuilder.nodeBuilder().settings(snode);
			nBuilder.data(false); // Don't store your own data
			_elasticClient = nBuilder.build().start().client();				
		}//TOTEST
		
		_sIndexName = sIndexName;	
		_sIndexType = sIndexType;	
		
	// 2. Create the index if necessary	
		
		if (null != settings) { // Need to create the index
			
			try {
				CreateIndexRequest cir = new CreateIndexRequest(_sIndexName);
				String sCachePolicy = properties.getElasticCachePolicy();
				if (null != sCachePolicy) {
					settings.put("index.cache.field.type", sCachePolicy);
				}
				if (null != sMapping) {
					cir.mapping(_sIndexType, sMapping);
				}
				cir.settings(settings.build());
				_elasticClient.admin().indices().create(cir).actionGet();
			
				//(Wait for above operation to be completed)
				_elasticClient.admin().cluster().health(new ClusterHealthRequest(_sIndexName).waitForYellowStatus()).actionGet();
			}
			catch (Exception e) {
				// Fine, index probably just exists
			}
			
		} //TESTED - normal NOT clustered
		
		// Either check index exists or wait for above operation to be completed
		
		_elasticClient.admin().cluster().health(new ClusterHealthRequest(_sIndexName).waitForYellowStatus()).actionGet();
		//TESTED - throws a horrible slow exception, but does fail
		
	// 3. Sort out replication, if necessary 
		
		// First time through, check the replication factor is correct...
		ClusterHealthResponse health = 
			_elasticClient.admin().cluster().health(new ClusterHealthRequest(sIndexName)).actionGet();
		
		ClusterIndexHealth indexStatus = health.indices().get(sIndexName);
		if ((null != indexStatus) && (1 == indexStatus.getShards().size())) { // 1 shard => this is a "data local" index
						
			int nNumNodes = health.getNumberOfDataNodes();
			Builder localSettings = ImmutableSettings.settingsBuilder();
			if (nNumNodes > 1) {
				localSettings.put("number_of_replicas", nNumNodes - 1); // (ie shard=1 + replicas==num_nodes)
			}
			else {
				localSettings.put("number_of_replicas", 1); // (System doesn't work very well if has no replicas?)				
			}
			_elasticClient.admin().indices().updateSettings(
					new UpdateSettingsRequest(sIndexName).settings(localSettings.build())).actionGet();
			
			//(Wait for above operation to be completed)
			_elasticClient.admin().cluster().health(new ClusterHealthRequest(sIndexName).waitForYellowStatus()).actionGet();
		}
		else if ((null != indexStatus) && (indexStatus.getNumberOfReplicas() > 1)) { // Multi shard index, just need to check there aren't too many replicas for nodes
			
			int nNumNodes = health.getNumberOfDataNodes();
			int nReplicas = indexStatus.getNumberOfReplicas();
			int nNodesPerReplica = properties.getElasticNodesPerReplica();
			if ((nNumNodes > 0) && (nNodesPerReplica > 0)) {
				int nNewReplicas = (nNumNodes + nNodesPerReplica-1)/nNodesPerReplica;
					// (ie round up)
				int nMaxReplicas = properties.getElasticMaxReplicas();
				if (nNewReplicas > nMaxReplicas) {
					nNewReplicas = nMaxReplicas;
				}
				
				if (nNewReplicas != nReplicas) { // Change the number of replicas
					Builder localSettings = ImmutableSettings.settingsBuilder();
					localSettings.put("number_of_replicas", nNewReplicas); 
					_elasticClient.admin().indices().updateSettings(
							new UpdateSettingsRequest(sIndexName).settings(localSettings.build())).actionGet();
					
					//(Wait for above operation to be completed)
					_elasticClient.admin().cluster().health(new ClusterHealthRequest(sIndexName).waitForYellowStatus()).actionGet();				
				}//TESTED
			}
		}
		//TESTED
	}
	//TOTEST: local client code - the concept doesn't really work though
	
	// Multi-index constructor (can't create the index, so no settings or maps)
	
	private ElasticSearchManager(String[] sIndexNames, String sIndexType, boolean bRemote, String hostAndPort) {
		
	// (0. cluster management)
		
		PropertiesManager properties = new PropertiesManager(); 
		
		if (null == _clusterName) {
			try {
				_clusterName = properties.getElasticCluster();
				if (null == _clusterName) {
					_clusterName = _defaultClusterName;
				}
			}
			catch (Exception e) {
				_clusterName = _defaultClusterName;				
			}
		}
		
	// 1. Link to client (or cluster)
			
		if (_bLocalMode) {
			NodeBuilder nBuilder = NodeBuilder.nodeBuilder().local(true);
			_elasticClient = nBuilder.node().client();
		}
		else if (bRemote) {
			
			String sHostname = null;
			String sPort = null;
			
			if (null == hostAndPort) {
				hostAndPort = new PropertiesManager().getElasticUrl();
			}		
			String[] hostPort = hostAndPort.split("[:/]");
			sHostname = hostPort[0];
			sPort = hostPort[1];

			Builder globalSettings = ImmutableSettings.settingsBuilder();
			Settings snode = globalSettings.put("cluster.name", _clusterName).build();
			TransportClient tmp = new TransportClient(snode);
			_elasticClient = tmp.addTransportAddress(new InetSocketTransportAddress(sHostname, Integer.parseInt(sPort)));
			
		} //TESTED
		else { // Create a "no data" cluster 
			
			Builder globalSettings = ImmutableSettings.settingsBuilder();
			Settings snode = globalSettings.put("cluster.name", _clusterName).build();

			NodeBuilder nBuilder = NodeBuilder.nodeBuilder().settings(snode);
			nBuilder.data(false); // Don't store your own data
			_elasticClient = nBuilder.build().start().client();				
		}//TOTEST
		
	// 2. Just store the index information 	
		
		_multiIndex = sIndexNames;	
		_sIndexType = sIndexType;	
	}
		
	///////////////////////////////////////////////////////////////////////////////////////
	
	// Create a child
	
	private ElasticSearchManager(String sParent, String sChild, Client client, String sMapping) 
	{ 
		_sIndexName = sParent;
		_sIndexType = sChild;
		_elasticClient  = client;
		
		try {			
			if (null != sMapping) {
				PutMappingRequest pmr = new PutMappingRequest(_sIndexName).type(_sIndexType).source(sMapping);
				
				// Add mapping to index
				_elasticClient.admin().indices().putMapping(pmr).actionGet();
			
				//(Wait for above operation to be completed)
				_elasticClient.admin().cluster().health(new ClusterHealthRequest(_sIndexName).waitForYellowStatus()).actionGet();
			}
		}
		catch (Exception e) {
			// Fine, index probably just exists
		}
	}//TESTED
	
	// Create a child of a multi index

	private ElasticSearchManager(String[] sParentIndices, String sChild, Client client, String sMapping) 
	{ 
		_sIndexName = null;
		_multiIndex = sParentIndices;
		_sIndexType = sChild;
		_elasticClient  = client;
		
		if (null != sMapping) {
			throw new RuntimeException("createChildIndex not supported on multi-index manager");
		}
	}//TESTED
		
	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////

	//Test code (See MongoGazateerTxfer.java, "TEST" comments)
	
	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	
} // end class ElasticSearchManager
