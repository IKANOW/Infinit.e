

function getFullText() 
{
	if (_doc.metadata.justification != null)
	{ 
		for (var i = 0; i < _doc.metadata.justification.length ; i ++)
		{
			s += _doc.metadata.justification[i].toString() + '\n';
		}
	}
	return s; 
}




function getFullText() { var s = ''; if (_doc.metadata.justification != null) { for (var i = 0; i < _doc.metadata.justification.length ; i ++) { s += _doc.metadata.justification[i].toString() + ' '; } } return s; }


function getLocationEntity() {
	var s = (_iterator.citystateprovince.city != null) ? _iterator.citystateprovince.city : '';
	s += (s.length > 0) ? ',' : '';
	s += (_iterator.citystateprovince.stateprovince != null) ? _iterator.citystateprovince.stateprovince : '';
	s+= (s.length > 0) ? ',' : '';
	s+= (_iterator.country != null) ? _iterator.country : '';
	return s;
	}

function getVictim() {
	var indicator = (_iterator.indicator != 'Unknown') ? _iterator.indicator : '';
	var victimType = (_iterator.victimtype != 'Unknown') ? _iterator.victimtype : '';
	var child = (_iterator.child == 'Yes') ? 'Child' : 'Adult';
	var combatant = (_iterator.combatant == 'Yes') ? 'Combatant' : '';
	var targeted = (_iterator.targetedcharacteristic != 'None' && _iterator.targetedcharacteristic != 'Unknown') ? _iterator.targetedcharacteristic : '';
	var defining = (_iterator.definingcharacteristic != 'None' &&_iterator.definingcharacteristic != 'Unknown') ? _iterator.definingcharacteristic : '';
	var s = indicator;
	if (victimType.length > 0) {
		if (s.length > 0) { s += ', '; }
		s += victimType; 				} 				if (s.length > 0) { 					s += ', '; 				} 				s += child; 				if (combatant.length > 0) { 					if (s.length > 0) { s += ', '; } 					s += combatant; 				} 				if (targeted.length > 0) { 					if (s.length > 0) { s += ', '; } 					s += targeted; 				} 				if (defining.length > 0) { 					if (s.length > 0) { s += ', '; } 					s += defining; 				} 				if (s.length > 0) { 					s += ' from '; 				} 				s += _iterator.nationality; 				return s; 			} 			



function getVictimCount() {
	var count = parseInt(_iterator.deadcount, 10) + parseInt(_iterator.woundedcount, 10);
	return count;
	}

function getEventType() {
	var s = _value;
	if (_doc.metadata.assassination[0] == 'Yes') s += ', Assassination';
	if (_doc.metadata.suicide[0] == 'Yes') s += ', Suicide';
	if (_doc.metadata.ied[0] == 'Yes') s += ', IED';
	return s; 			
}

function getEventTypeFull() { 				var s = _doc.metadata.eventtype[0]; 				if (_doc.metadata.assassination[0] == 'Yes') s += ', Assassination'; 				if (_doc.metadata.suicide[0] == 'Yes') s += ', Suicide'; 				if (_doc.metadata.ied[0] == 'Yes') s += ', IED';				return s;			} 			function isOrganizationSpecified() {  				if (_doc.metadata.organization != null && _doc.metadata.organization[0].toString().toLowerCase() == 'no group')					{ return false; } 				else 					{ return true; } 			}			function getOrganizationName() { 				if (_doc.metadata.organization != null && _doc.metadata.organization[0].toString().toLowerCase() != 'no group')					{ return _doc.metadata.organization[0]; } 			}		",
