function getEndPointUrl() {
	url = "";
	// Overrides:
	url = "http://infinite.ikanow.com/api/"; // Cloud
    //url = "http://ec2-50-17-85-27.compute-1.amazonaws.com/api/" // Beta dev machine
	//url = "http://localhost:8184/"; // Local dev
	//url = "http://infinite.wfot.ikanow.com/api/";
	
	// You'll probably never want:
	//url ="http://InfiniteTomcatLB-2025482046.us-east-1.elb.amazonaws.com/api/"; // Load Balancer
	//if (window.location.protocol == "file:") {
	//	url = "http://betadev.ikanow.com/api/"; // beta dev server
	//}
	//else {
	//	url = "http://beta.ikanow.com/api/"; // beta cloud
	//}
	return url;
}

// Note tidied to the domain, so need to regenerate when goes onto a new server
function getMapLicenseKey() {
	url = 
	"ABQIAAAAPG0G6825cvmxfkO4sLnuNxTYkENnCsJoLpowsVPNC5bT4IvfARSFTUkvReEMUsu3mpCi83eID8s4JA" 
		// (infinite.ikanow.com ... Cloud)
	//"ABQIAAAALYmuOFhO0edlPjL4Gu6CphQ543n4lNQ9uDfwtxyBjbeWU5Z6dxTkigScr_AQwynZyNCcbaBd1xVw8Q"
		//("http://ec2-50-17-85-27.compute-1.amazonaws.com ... beta DEV)
	//"ABQIAAAArPGON6qlHQAwximiB9CbXhTdRtpTo2le0IFfZOy7co2oKvs2_hTDTtwZdMCnVm6DDksOefTCUt6zLA";
	    // (localhost)
		
	// You'll probably never want:
		
	//"ABQIAAAALYmuOFhO0edlPjL4Gu6CphRzmZgyY5bUp2W2JDJNb2gCOiKbnhRmmx6d04MEmEmMikHDmrt5Of2jVA"
		//(http://beta.ikanow.com ... temp beta deployment)
	//"ABQIAAAALYmuOFhO0edlPjL4Gu6CphScrcd-rBFrQcpZVO7-aLX1kfEcQBSn1EI4GFVKwvBX4iULq1D7-kwy1Q";
	    //(http://InfiniteTomcatLB-2025482046.us-east-1.elb.amazonaws.com .. load balancer)
	return url;
}

function getApiVersion() {
	// return 1; // Alpha
	return 2; // Beta
}

function getTimeoutSeconds() {
	return 1800;
}

// function to return browser height
function getBrowserHeight() {
	return newHeight;
}

// function to return browser width
function getBrowserWidth() {
	return newWidth;
}

// function to return which properties were changed
function getChanged() {
	return changed;
}

// function to update the screen properties
function updateScreen() {
	// getFlexApp('index').getResolution();
	if (screen.width != screenWidth || screen.height != screenHeight) {
		screenWidth = screen.width;
		screenHeight = screen.height;

		newWidth = screen.width;
		newHeight = screen.height;

		changed = "screenResolution";
	} else if (window.innerWidth != browserWidth
			|| window.innerHeight != browserHeight) {
		newWidth = window.innerWidth;
		newHeight = window.innerHeight;

		browserWidth = window.innerWidth;
		browserHeight = window.innerHeight;

		changed = "browserResolution";
	}
}