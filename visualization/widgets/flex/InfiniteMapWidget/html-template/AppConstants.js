function getEndPointUrl() {
	url = "http://infinite.ikanow.com/api/"
	//"http://InfiniteTomcatLB-2025482046.us-east-1.elb.amazonaws.com/api/"
	//url = "http://localhost:8184/";
	//url = window.location.protocol + "//" + window.location.host + "/api/";
	return url;
}

//Note tidied to the domain, so need to regenerate when goes onto a new server
function getMapLicenseKey() {
	url = "ABQIAAAAPG0G6825cvmxfkO4sLnuNxTYkENnCsJoLpowsVPNC5bT4IvfARSFTUkvReEMUsu3mpCi83eID8s4JA"  // (infinite.ikanow.com)
		//"ABQIAAAALYmuOFhO0edlPjL4Gu6CphScrcd-rBFrQcpZVO7-aLX1kfEcQBSn1EI4GFVKwvBX4iULq1D7-kwy1Q"; //(http://InfiniteTomcatLB-2025482046.us-east-1.elb.amazonaws.com)
		// "ABQIAAAArPGON6qlHQAwximiB9CbXhTdRtpTo2le0IFfZOy7co2oKvs2_hTDTtwZdMCnVm6DDksOefTCUt6zLA"; // (localhost) 
		// "ABQIAAAArPGON6qlHQAwximiB9CbXhTp_kEhD959kw1WcqycWZWWuotVwBTxHRItEsr74Pge7SWZjWpU_iVi1A"; // (unknown)
	//alert{url};
	return url;
}

//function to return browser height
function getBrowserHeight()
{
	return newHeight;
}

//function to return browser width
function getBrowserWidth()
{
	return newWidth;
}

//function to return which properties were changed
function getChanged()
{
	return changed;
}

//function to update the screen properties
function updateScreen()
{
	//getFlexApp('index').getResolution();
	if(screen.width != screenWidth || screen.height != screenHeight)
	{
		screenWidth = screen.width;
		screenHeight = screen.height;
		
		newWidth = screen.width;
		newHeight = screen.height;
		
		changed = "screenResolution";
	}else
		if(window.innerWidth != browserWidth || window.innerHeight != browserHeight)
		{
			newWidth = window.innerWidth;
			newHeight = window.innerHeight;
			
			browserWidth = window.innerWidth;
			browserHeight = window.innerHeight;
			
			changed = "browserResolution";
		}	
}