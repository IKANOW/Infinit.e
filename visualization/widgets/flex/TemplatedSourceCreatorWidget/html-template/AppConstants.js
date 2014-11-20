function getEndPointUrl() {
	url = "https://infinite.ikanow.com/api/";
	//url = "http://dev.ikanow.com/api/";
	//url = "http://localhost:8184/"
	//url = "http://absolutepin.ikanow.com/api/";
    return url;
}

function getDomainUrl() {
	domainURL = "http://www.ikanow.com/";
	return domainURL;
}

function getForgottenPasswordUrl() {
//	forgotUrl = getEndPointUrl() + "auth/forgotpassword"; //app.saas=false
	forgotUrl = getDomainUrl() + "forgot-password/"; // app.saas=true
	return forgotUrl;
}

function getDomainLogoutUrl() {
//	logoutURL = ""; //app.saas=false
	logoutURL = getDomainUrl() + "?action=logout"; // app.saas=true
	return logoutURL;
}

function getMapLicenseKey() {
	key =  "dummy";
	return key;
}

function getTimeoutSeconds() {
	return 1800;
}

function getSearchProvider() {
	return "Google";
}

function getSearchUrl(encodedSearchTerms) {
	return "https://www.google.com/search?hl=en&q=" + encodedSearchTerms;
}

function getCaseManagerApiUrl() {
    return "http://infinite.ikanow.com/caseserver/";
//	return "http://localhost:8185/";
}

function getCaseManagerUrl() {
    return "http://infinite.ikanow.com:8090/casemanager/";
	//	return "http://ec2-107-22-151-153.compute-1.amazonaws.com/nuxeo/";
}
