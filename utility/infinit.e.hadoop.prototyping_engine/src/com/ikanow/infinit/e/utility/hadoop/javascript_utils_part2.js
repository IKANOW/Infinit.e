
//////////////////////////////////////////////////////

// INPUT/INTERMEDIATE UTILITIES

var _map_input_key = null;
var _map_input_value = null;
var _combine_input_key = null;
var _combine_input_values = null;
var _reduce_input_key = null;
var _reduce_input_values = null;

function internal_mapper()
{
	_emit_list = [];
	var map_input_value = eval('(' + _map_input_value + ')');
	map(_map_input_key, map_input_value);
}

function internal_combiner()
{
	_emit_list = [];
	var combine_input_key = eval('(' + _combine_input_key + ')');
	var combine_input_values = eval('(' + _combine_input_values + ')');
	combine(combine_input_key, combine_input_values);
}

function internal_reducer()
{
	_emit_list = [];
	var reduce_input_key = eval('(' + _reduce_input_key + ')');
	var reduce_input_values = eval('(' + _reduce_input_values + ')');
	reduce(reduce_input_key, reduce_input_values);
}

