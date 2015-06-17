function printIndex(el) {
	print(el.gazateer_index);
}
var steps = [ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ];
var size = db.entity.count({"groupids":"4c927585d591d31d7b37097a"});
for (step in steps) {
//	print(Math.floor(size*step/10));
	db.entity.find({"groupids":"4c927585d591d31d7b37097a"}).skip(Math.floor(size*step/10)).limit(1000).forEach(printIndex)
}
