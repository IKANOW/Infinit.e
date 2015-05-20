function om2js(doc){
	var d = null;
	dprint("om2js:"+doc );
	if(doc!=null){
		d = cloneObject(doc);
	}
	return d;
}

function cloneObject(obj) {
    var clone = {};
    dprint("obj-class:"+"type="+typeof(obj));
    if(obj!=null && typeof(obj)=="object" && !(obj instanceof java.lang.Class)){
		if(obj instanceof java.util.Map){
			dprint("Map instance");
	    	var it = obj.entrySet().iterator();
	    	while(it.hasNext()){
	    		var e = it.next();    		
	    		dprint("e.key:"+e.getKey()+",e.value:"+e.getValue());
	    		clone[e.getKey()]=cloneObject(e.getValue());
	    	}
	    }else
    		if(com.ikanow.infinit.e.script_visible.ScriptUtil.isObjectArray(obj)){
    	    dprint("Object array instance");
    		clone = [];
    	    for(var i in obj) {
        		dprint("Object array["+i+"]"+obj[i]);
        		clone.push(cloneObject(obj[i]));
    	    }
    	} // is array
    	else
		if(obj instanceof java.util.Set){
			dprint("Set instance");
    		clone = [];
	    	var it = obj.iterator();
	    	while(it.hasNext()){
	    		var e = it.next();    		
        		dprint("Set member:"+e);
        		clone.push(cloneObject(e));
	    	}
	    }else	    
		if(obj instanceof java.util.List){
			dprint("List instance");
    		clone = [];
	    	var it = obj.iterator();
	    	while(it.hasNext()){
	    		var e = it.next();    		
        		dprint("List member:"+e);
        		clone.push(cloneObject(e));
	    	}
	    }
		else
		if(obj instanceof java.util.Date){
			dprint("Date instance:"+obj);
			clone = com.ikanow.infinit.e.script_visible.ScriptUtil.format(obj);
		}
		else
		if(typeof(obj) === 'function'){
			dprint("Function instance ignoring:"+obj);
		}
    	else
		if(obj instanceof java.lang.Object){
			dprint("Object instance");
			var properties = com.ikanow.infinit.e.script_visible.ScriptUtil.extractFields(obj);
			dprint("properties:"+properties);
			var propNamesLower = {};
		    for(var propName in properties) {
		    	var prop = properties[propName];
		    	if(propName!="class" && propName!="declaringClass" && prop!=null && !propNamesLower[propName.toLowerCase()] && (typeof(prop)!='function'))
		    	{
		    		propNamesLower[propName.toLowerCase()] = true;
		    		dprint("clone Object properties("+propName+")");
		    		clone[propName] = cloneObject(prop);
		    	} // if prop 
		    } // for
		 } // Object
		else{
			dprint("instance not recognized:"+obj+":type:"+typeof(obj) );			
		}
    } else{
    	clone = obj;
    }
    return clone;	
}


function dprint(debugMessage){
	if(typeof(_debug)!="undefined" && _debug==true){
		print(debugMessage);
	}
}

function dprintJSON(debugObject){
	if(typeof(_debug)!="undefined" && _debug==true){
		dprint(JSON.stringify(debugObject));
	}
}
// only use this function outside during debugging
function printState(){
		print("ENGINE STATE _doc:");
		if(typeof(_doc)!="undefined"){
			print(JSON.stringify(_doc));
		}else{
			print("_doc=undefined");
		}
		print("ENGINE STATE _docPojo:");
		if(typeof(_docPojo)!="undefined"){
			print(com.ikanow.infinit.e.script_visible.ScriptUtil.toJson(_docPojo));				
		}else{
			print("_docPojo=undefined");				
		}
		print("ENGINE STATE _metadata:");
		if(typeof(_metadata)!="undefined"){
			print(JSON.stringify(_metadata));
		}else{
			print("_metadata=undefined");				
		}
		print("ENGINE STATE text:");
		if(typeof(text)!="undefined"){
			print(text);
		}else{
			print("text=undefined");				
		}
}