if ((typeof(_dirtyDoc)=="undefined") || _dirtyDoc) {
var _doc = om2js(_docPojo);
var _metadata = _doc.metadata;
var text = _docPojo.getFullText();
dprintJSON(_doc);
_dirtyDoc=false;
}
