// utilities.js
// --------------------------------------------------------------------------------

function publishSourceAlert()
{
	var x = window.confirm("Do you really wish to publish this source?");
	if (x)
	{
		setDispatchAction("publish-source");
	}
}



// showMessageToDisplay
function showMessageToDisplay()
{
	var message = document.getElementById("messageToDisplay").value;
	message.replace("'","\'");
	alert(message);
} // TESTED


// openTestSourceWindow - 
function openTestSourceWindow(strResponseMessage, strHarvesterOutput)
{
	try
	{
		var response = decodeURIComponent(strResponseMessage);
		strHarvesterOutput = strHarvesterOutput.replace(/\+/gi, "%20");
		var harvester = decodeURIComponent(strHarvesterOutput);
		testSourcePage = testSourcePage.replace(/XXXXXXXX/gi, response);
		testSourcePage = testSourcePage.replace(/ZZZZZZZZ/gi, harvester);
		testSourceWindow = window.open('','','width=950,height=700');
		testSourceWindow.document.write(testSourcePage);
		testSourceWindow.focus();
	}
	catch (err)
	{
		alert(err.message);
	}
} // TESTED


//forwardTo -
function forwardTo(url)
{
	window.location = url;
} // TESTED


// setDispatchAction -
function setDispatchAction(val)
{
	document.getElementById("dispatchAction").setAttribute("value", val);
}


// String var with HTML for openTestSourceWindow function above
// ----------------------------------------------------------------------------
var testSourcePage = ""+
"<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" +
"<html>" +
"<head>" +
"	<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">" +
"	<title>Infinit.e Source.Builder - V0.1</title>" +
"	<script type=\"text/javascript\" src=\"inc/utilities.js\"></script>" +
"</head>" +
"<body>" +
"<!-- Begin header.jsp  -->" +
"	<table bgcolor=\"black\" cellpadding=\"0\" cellspacing=\"1\" width=\"100%\" >" +
"		<tr>" +
"			<td><a href=\"index.jsp\"><img src=\"img/ikanow_logo.png\" border=\"0\" /></a></td>" +
"		</tr>" +
"		<tr bgcolor=\"white\">" +
"			<td>" +
"				<table width=\"100%\" cellpadding=\"5\">" +
"					<tr>" +
"						<td>" +
"<!-- End header.jsp  -->" +
"<form id=\"testSourceForm\" name=\"testSourceForm\">" +
"<table>" +
"	<tr>" +
"		<td>" +
"			<textarea cols=\"125\" rows=\"5\" id=\"responseMessage\" name=\"responseMessage\" readonly=\"readonly\">XXXXXXXX</textarea>" +
"		</td>" +
"	</tr>" +
"	<tr>" +
"		<td>" +
"			<textarea cols=\"125\" rows=\"35\" id=\"harvesterOutput\" name=\"harvesterOutput\" readonly=\"readonly\">ZZZZZZZZ</textarea>" +
"		</td>" +
"	</tr>" +
"</table>" +
"</form>" +
"<!-- Begin footer.jsp  -->" +
"						</td>" +
"					</tr>" +
"				</table>" +
"			</td>" +
"		</tr>" +
"		<tr>" +
"			<td>&nbsp;</td>" +
"		</tr>" +
"	</table>" +
"</body>" +
"</html>";

