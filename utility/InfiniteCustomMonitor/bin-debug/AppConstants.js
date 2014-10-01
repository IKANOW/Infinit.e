function getEndPointUrl() {
	//url = "https://odin.ikanow.com/api/";
	//url = "http://dev.ikanow.com/api/";
	//url = "https://dev.ikanow.com/api/";
	url = "http://localhost:8080/api/";
	//url = "https://infinite.ikanow.com/api/";
	//url = "http://localhost:8184/";
    return url;
}

function getDomainUrl() {
	domainURL = "DOMAIN_URL";
	return domainURL;
}

function getForgottenPasswordUrl() {
	forgotUrl = getEndPointUrl() + "auth/forgotpassword"; //app.saas=false
	forgotUrl = getDomainUrl() + "FORGOT_PASSWORD_URL"; // app.saas=true
	return forgotUrl;
}

function getDomainLogoutUrl() {
	logoutURL = ""; //app.saas=false
	logoutURL = getDomainUrl() + "LOGOUT_URL"; // app.saas=true
	return logoutURL;
}

function getMapLicenseKey() {
	key =  "GOOGLE_MAPS_API_KEY";
	return key;
}

function getTimeoutSeconds() {
	return 1800;
}

function getSearchProvider() {
	return "google";
}

// (Note, have to use '' not "" here because of substitution logic elsewhere)
function getSearchUrl(encodedSearchTerms) {
	return 'https://www.google.com/search?hl=en&q=' + encodedSearchTerms; // ui.logo.url=default
	return EXTERNAL_SEARCH_URL;
}

function getLogoUrl() {
	return null;
}

function getCaseManagerApiUrl() {
	return "CASE_MANAGER_API_URL";	
}

function getCaseManagerUrl() {
	return "CASE_MANAGER_URL";
}
