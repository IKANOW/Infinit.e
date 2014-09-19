
//////////////////////////////////////////////////////

// INPUT/INTERMEDIATE UTILITIES

var _map_input_key = null;
var _map_input_value = null;
var _combine_input_key = null;
var _combine_input_values = null;
var _reduce_input_key = null;
var _reduce_input_values = null;
var _streaming_input_value = null;
var _inContext = null; // (gets replaced with Java object)
var _streaming_context = {		
		next: function() {
			_streaming_input_value = _inContext.next();
			if (null == _streaming_input_value) {
				return null;
			}
			else { // convert to JSON
				return eval('(' + _streaming_input_value + ')');
			}
		},
		hasNext: function() {
			return _inContext.hasNext();
		}
}
var _internalError = true;

function internal_mapper()
{
	_emit_list = [];
	_internalError = true;
	var map_input_value = eval('(' + _map_input_value + ')');
	_internalError = false;
	map(_map_input_key, map_input_value);
}

function internal_combiner()
{
	_emit_list = [];
	var combine_input_key = eval('(' + _combine_input_key + ')');
	var combine_input_values = null;
	if (_inContext) {
		combine_input_values = _streaming_context;
	}
	else {
		combine_input_values = eval('(' + _combine_input_values + ')');
	}
	combine(combine_input_key, combine_input_values);
}

function internal_reducer()
{
	_emit_list = [];
	var reduce_input_key = eval('(' + _reduce_input_key + ')');
	var reduce_input_values = null;
	if (_inContext) {
		reduce_input_values = _streaming_context;
	}
	else {
		reduce_input_values = eval('(' + _reduce_input_values + ')');
	}
	reduce(reduce_input_key, reduce_input_values);
}

