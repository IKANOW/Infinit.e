package com.ikanow.infinit.e.data_model.store.document;

import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class DocCountPojo extends BaseDbPojo {
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<BaseDbPojo>> listType() { return new TypeToken<List<BaseDbPojo>>(){}; }

	private String _id; // the _id of the community
	final public static String _id_ = "_id";
	private Long doccount; // the number of documents in that community	
	final public static String doccount_ = "doccount";
	
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public Long getDoccount() {
		return doccount;
	}
	public void setDoccount(Long doccount) {
		this.doccount = doccount;
	}
}
