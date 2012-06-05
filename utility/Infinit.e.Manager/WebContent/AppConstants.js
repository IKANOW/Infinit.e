function getEndPointUrl() {
	//url = "";
	url = "http://localhost:8184/";
    //url = "http://50.57.133.246/api/";
    //url = "http://infinite.rr.ikanow.com/api/";
	return url;
}

function getDomainUrl() {
	domainURL = ""; // Non-cloud
	// Overrides:
	//domainURL = "http://www.ikanow.com/"; // Cloud
	//}
	return domainURL;
}

function getForgottenPasswordUrl() { 
	forgotUrl = "";
	// Overrides:
	//forgotUrl = getDomainUrl() + "forgot-password/"; // Cloud
	forgotUrl = getEndPointUrl() + "auth/forgotpassword"; // Non-cloud
	return forgotUrl;
}

function getDomainLogoutUrl() {
	logoutURL = ""; // Non-cloud
	// Overrides:
	//logoutURL = getDomainUrl() + "?action=logout"; // Cloud
	return logoutURL;

}

// Note tidied to the domain, so need to regenerate when goes onto a new server
function getMapLicenseKey() {
	url = 
	//"ABQIAAAAPG0G6825cvmxfkO4sLnuNxTYkENnCsJoLpowsVPNC5bT4IvfARSFTUkvReEMUsu3mpCi83eID8s4JA" 
		// (infinite.ikanow.com ... Cloud)
	"ABQIAAAALYmuOFhO0edlPjL4Gu6CphQ543n4lNQ9uDfwtxyBjbeWU5Z6dxTkigScr_AQwynZyNCcbaBd1xVw8Q"
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

function getTimeoutSeconds() {
	return 1800;
}
