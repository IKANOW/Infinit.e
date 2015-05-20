function printIndex(el) {

	if ((null == el.entity1_index) && (null == el.entity2_index)) {
		return; // (shouldn't ever occur)
	}
	if ((null == el.entity1_index) || ((null != el.entity2_index) 
				&& (0 == (el.entity1_index[0].charCodeAt() % 2)))) {
		print('qt[0].event.verb=\"' + el.verb_category +
			'\"&qt[0].event.entity2.entity=\"' + el.entity2_index +'\"');
	}
	else if ((null == el.entity2_index) || ((null != el.entity1_index) 
				&& (0 == (el.entity2_index[0].charCodeAt() % 2)))) {
		print('qt[0].event.verb=\"' + el.verb_category +
			'\"&qt[0].event.entity1.entity=\"' + el.entity1_index +'\"');
	}
	else if ((null == el.verb_category) || (0 == (el.verb_category[0].charCodeAt() % 2))) {
		print('qt[0].event.entity1.entity=\"' + el.entity1_index +
			'\"&qt[0].event.entity2.entity=\"' + el.entity2_index +'\"');
	}
	else {
		print('qt[0].event.entity1.entity=\"' + el.entity1_index +
			'\"&qt[0].event.verb=\"' + el.verb_category +
			'\"&qt[0].event.entity2.entity=\"' + el.entity2_index +'\"');
	}
}
var steps = [ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ];
var size = db.event.count({'groupids':'4c927585d591d31d7b37097a'});
for (step in steps) {
//	print(Math.floor(size*step/10));
	db.event.find({'groupids':'4c927585d591d31d7b37097a'}).skip(Math.floor(size*step/10)).limit(1000).forEach(printIndex)
}
