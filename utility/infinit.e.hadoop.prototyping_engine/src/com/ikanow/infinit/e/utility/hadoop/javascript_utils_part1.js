
//////////////////////////////////////////////////////

// INPUT/INTERMEDIATE UTILITIES

var _emit_list = [];
var _outContext = null;

function emit(_key, _val)
{
	if (null != _outContext) { // memory optimized
		_outContext.write( s3(_key, 0), s3(_val, 0)) ;
	}
	else {
		if (null != _val) {
			_val._id = null;
		}
		_emit_list.push({key:_key, val:_val});
	}
}

//////////////////////////////////////////////////////

// OUTPUT UTILITIES

// (s1 is called on [{key,value},...]
function s1(el) {
	if (el == null) {}
	else if (el instanceof Array) {
		s2(el, 1);
	}
	else if (typeof el == 'object') {
		outList.add(s3(el, 0));
	}
	else if (typeof el == 'number') {
		outList.add(el);
	}
	else {
		outList.add(el.toString());
	}
}
function s2(el, master_list) {
	var list = (1 == master_list)?outList:listFactory.clone();
	for (var i = 0; i < el.length; ++i) {
		var subel = el[i];
		if (subel == null) {}
		else if (subel instanceof Array) {
			list.add(s2(subel, 0));
		}
		else if (typeof subel == 'object') {
			list.add(s3(subel, master_list));
		}
		else if (typeof subel == 'number') {
			list.add(subel);
		}
		else {
			list.add(subel.toString());
		}
	}
	return list;
}
function s3(el, depth) {
	el.constructor.toString();
	
	// MongoDB specific:
	if (null != el['$oid']) {
		return new org.bson.types.ObjectId(el['$oid']);
	}
	else if (null != el['$date']) {
		var ISOFORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		return ISOFORMAT.parse(el['$date'].replace('Z','-0000').toString());
		
	}
	
	// If called with depth==0 or 1 (1 from master list, 0 from s3 from master list, -1 if called from within a nested object or array)
	var currObj = (depth >= 0)  ? objFactory.clone(true) : objFactory.clone(false);		

	for (var prop in el) {
		var subel = el[prop];
		if (subel == null) {}
		else if (subel instanceof Array) {
			currObj.put(prop, s2(subel, depth - 1));
		}
		else if (typeof subel == 'object') {
			currObj.put(prop, s3(subel, depth - 1));
		}
		else if (typeof subel == 'number') {
			currObj.put(prop, subel);
		}
		else {
			currObj.put(prop, subel.toString());
		}
	}
	return currObj;
}

