function setAPI(url)
{
	localStorage["api"] = url;
}

function getAPI()
{
	return localStorage["api"];
}

function sendHttpRequestToInfinite(url, callback, data)
{		
	var httpRequest = {
			"url": localStorage["api"] + url, 
			"callback": callback,
			"data": data,
			"xhr": new XMLHttpRequest(),
			"httptimeout": null
				};
	httpRequest.httptimeout = setTimeout( function(){ httpRequest.xhr.abort(); }, localStorage.timeout );
	
	
	httpRequest.xhr.onreadystatechange = function() 
	{
		if ( httpRequest.xhr.readyState == 4 )
		{
			clearTimeout(httpRequest.httptimeout);
			//debugMessage("Response Received")
			httpRequest.callback(httpRequest.xhr.responseText,httpRequest);
		}
	}
		
	if ( data )
	{ 
		httpRequest.xhr.open("POST", httpRequest.url, true);
		httpRequest.xhr.send(httpRequest.data);
	}
	else
	{
		httpRequest.xhr.open("GET", httpRequest.url, true);
		httpRequest.xhr.send();
	}	
}