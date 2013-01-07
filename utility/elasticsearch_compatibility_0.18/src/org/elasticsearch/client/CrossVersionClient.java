package org.elasticsearch.client;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountRequest;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequest;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.mlt.MoreLikeThisRequest;
import org.elasticsearch.action.percolate.PercolateRequest;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.action.count.CountRequestBuilder;
import org.elasticsearch.client.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.client.action.get.GetRequestBuilder;
import org.elasticsearch.client.action.get.MultiGetRequestBuilder;
import org.elasticsearch.client.action.index.IndexRequestBuilder;
import org.elasticsearch.client.action.mlt.MoreLikeThisRequestBuilder;
import org.elasticsearch.client.action.percolate.PercolateRequestBuilder;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.client.action.search.SearchScrollRequestBuilder;

public class CrossVersionClient //implements org.elasticsearch.client.Client (USE TO GET THE methods to override) 
{

	protected Client _superClient;
	public CrossVersionClient(Client superClient) { this._superClient = superClient; };
	public Client getRawClient() { return _superClient; }
	
	//@Override
	public AdminClient admin() {
		return _superClient.admin();
	}

	//@Override
	public ActionFuture<BulkResponse> bulk(BulkRequest arg0) {
		return _superClient.bulk(arg0);
	}

	//@Override
	public void bulk(BulkRequest arg0, ActionListener<BulkResponse> arg1) {
		_superClient.bulk(arg0, arg1);
	}

	//@Override
	public void close() {
		_superClient.close();		
	}

	//@Override
	public ActionFuture<CountResponse> count(CountRequest arg0) {
		return _superClient.count(arg0);
	}

	//@Override
	public void count(CountRequest arg0, ActionListener<CountResponse> arg1) {
		_superClient.count(arg0, arg1);
	}

	//@Override
	public ActionFuture<DeleteResponse> delete(DeleteRequest arg0) {
		return _superClient.delete(arg0);
	}

	//@Override
	public void delete(DeleteRequest arg0, ActionListener<DeleteResponse> arg1) {
		_superClient.delete(arg0, arg1);
	}

	//@Override
	public ActionFuture<DeleteByQueryResponse> deleteByQuery(
			DeleteByQueryRequest arg0) {
		return _superClient.deleteByQuery(arg0);
	}

	//@Override
	public void deleteByQuery(DeleteByQueryRequest arg0,
			ActionListener<DeleteByQueryResponse> arg1) {
		_superClient.deleteByQuery(arg0, arg1);
	}

	//@Override
	public ActionFuture<GetResponse> get(GetRequest arg0) {
		return _superClient.get(arg0);
	}

	//@Override
	public void get(GetRequest arg0, ActionListener<GetResponse> arg1) {
		_superClient.get(arg0, arg1);
	}

	//@Override
	public ActionFuture<IndexResponse> index(IndexRequest arg0) {
		return _superClient.index(arg0);
	}

	//@Override
	public void index(IndexRequest arg0, ActionListener<IndexResponse> arg1) {
		_superClient.index(arg0, arg1);
	}

	//@Override
	public ActionFuture<SearchResponse> moreLikeThis(MoreLikeThisRequest arg0) {
		return _superClient.moreLikeThis(arg0);
	}

	//@Override
	public void moreLikeThis(MoreLikeThisRequest arg0,
			ActionListener<SearchResponse> arg1) {
		_superClient.moreLikeThis(arg0, arg1);
	}

	//@Override
	public ActionFuture<MultiGetResponse> multiGet(MultiGetRequest arg0) {
		return _superClient.multiGet(arg0);
	}

	//@Override
	public void multiGet(MultiGetRequest arg0,
			ActionListener<MultiGetResponse> arg1) {
		_superClient.multiGet(arg0, arg1);
	}

	//@Override
	public ActionFuture<PercolateResponse> percolate(PercolateRequest arg0) {
		return _superClient.percolate(arg0);
	}

	//@Override
	public void percolate(PercolateRequest arg0,
			ActionListener<PercolateResponse> arg1) {
		_superClient.percolate(arg0, arg1);
	}

	//@Override
	public BulkRequestBuilder prepareBulk() {
		return _superClient.prepareBulk();
	}

	//@Override
	public CountRequestBuilder prepareCount(String... arg0) {
		return _superClient.prepareCount(arg0);
	}

	//@Override
	public DeleteRequestBuilder prepareDelete() {
		return _superClient.prepareDelete();
	}

	//@Override
	public DeleteRequestBuilder prepareDelete(String arg0, String arg1,
			String arg2) {
		return _superClient.prepareDelete(arg0, arg1, arg2);
	}

	//@Override
	public DeleteByQueryRequestBuilder prepareDeleteByQuery(String... arg0) {
		return _superClient.prepareDeleteByQuery(arg0);
	}

	//@Override
	public GetRequestBuilder prepareGet() {
		return _superClient.prepareGet();
	}

	//@Override
	public GetRequestBuilder prepareGet(String arg0, String arg1, String arg2) {
		return _superClient.prepareGet(arg0, arg1, arg2);
	}

	//@Override
	public IndexRequestBuilder prepareIndex() {
		return _superClient.prepareIndex();
	}

	//@Override
	public IndexRequestBuilder prepareIndex(String arg0, String arg1) {
		return _superClient.prepareIndex(arg0, arg1);
	}

	//@Override
	public IndexRequestBuilder prepareIndex(String arg0, String arg1,
			String arg2) {
		return _superClient.prepareIndex(arg0, arg1, arg2);
	}

	//@Override
	public MoreLikeThisRequestBuilder prepareMoreLikeThis(String arg0,
			String arg1, String arg2) {
		return _superClient.prepareMoreLikeThis(arg0, arg1, arg2);
	}

	//@Override
	public MultiGetRequestBuilder prepareMultiGet() {
		return _superClient.prepareMultiGet();
	}

	//@Override
	public PercolateRequestBuilder preparePercolate(String arg0, String arg1) {
		return _superClient.preparePercolate(arg0, arg1);
	}

	//@Override
	public SearchRequestBuilder prepareSearch(String... arg0) {
		return _superClient.prepareSearch(arg0);
	}

	//@Override
	public SearchScrollRequestBuilder prepareSearchScroll(String arg0) {
		return _superClient.prepareSearchScroll(arg0);
	}

	//@Override
	public ActionFuture<SearchResponse> search(SearchRequest arg0) {
		return _superClient.search(arg0);
	}

	//@Override
	public void search(SearchRequest arg0, ActionListener<SearchResponse> arg1) {
		_superClient.search(arg0, arg1);
	}

	//@Override
	public ActionFuture<SearchResponse> searchScroll(SearchScrollRequest arg0) {
		return _superClient.searchScroll(arg0);
	}

	//@Override
	public void searchScroll(SearchScrollRequest arg0,
			ActionListener<SearchResponse> arg1) {
		_superClient.searchScroll(arg0, arg1);
	}

}
