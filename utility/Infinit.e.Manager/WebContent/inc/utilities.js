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
		strResponseMessage = strResponseMessage.replace(/\+/gi, "%20");
		var response = decodeURIComponent(strResponseMessage);
		strHarvesterOutput = strHarvesterOutput.replace(/\+/gi, "%20");
		var harvester = decodeURIComponent(strHarvesterOutput);
		testSourcePage = testSourcePage.replace(/XXXXXXXX/gi, response);
		testSourcePage = testSourcePage.replace(/ZZZZZZZZ/gi, harvester);
		testSourceWindow = window.open('','','width=1024,height=800');
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
"	<title>Source Test Output</title>" +
"	<script type=\"text/javascript\" src=\"inc/utilities.js\"></script>" +

"	<script type=\"text/javascript\" src=\"lib/codemirror.js\"></script>" +
"   	<script type=\"text/javascript\" src=\"lib/languages/javascript.js\"></script>" +
"	<link rel=\"stylesheet\" type=\"text/css\" href=\"lib/codemirror.css\" />" +
"    <script src=\"lib/codemirror_extra/dialog/dialog.js\"></script>" +
"    <link rel=\"stylesheet\" href=\"lib/codemirror_extra/dialog/dialog.css\"/>" +
"    <script src=\"lib/codemirror_extra/search/searchcursor.js\"></script>" +
"    <script src=\"lib/codemirror_extra/search/search.js\"></script>" +
"<script src=\"lib/codemirror_extra/fold/foldcode.js\"></script>"+
"<script src=\"lib/codemirror_extra/fold/brace-fold.js\"></script>"+

"<style>"+
".CodeMirror { border-width:1px; border-style: solid; border-color:#DBDFE6; }"+
".CodeMirror-foldmarker { color: blue;"+
"    text-shadow: #b9f 1px 1px 2px, #b9f -1px -1px 2px, #b9f 1px -1px 2px, #b9f -1px 1px 2px;"+
"    font-family: arial; line-height: .3; cursor: pointer;"+
"  }"+
"</style>"+

"</head>" +
"<body>" +
"<!-- Begin header.jsp  -->" +
"	<table bgcolor=\"black\" cellpadding=\"4\" cellspacing=\"1\" width=\"100%\" >" +
"		<tr>" +
"			<td style='font-family: sans-serif; font-weight: bold; background-color : #6E7476; color : #ffffff'>Source Test Output</td>" +
"		</tr>" +
"		<tr bgcolor=\"white\">" +
"			<td>" +
"				<table width=\"100%\" cellpadding=\"0\">" +
"					<tr>" +
"						<td>" +
"<!-- End header.jsp  -->" +
"<form id=\"testSourceForm\" name=\"testSourceForm\">" +
"<table cellpadding=\"2\">" +
"	<tr>" +
"		<td>" +
"			<textarea cols=\"120\" rows=\"5\" id=\"responseMessage\" name=\"responseMessage\" readonly=\"readonly\">XXXXXXXX</textarea>" +
"		</td>" +
"	</tr>" +
"	<tr>" +
"		<td>" +
"			<textarea cols=\"120\" id=\"harvesterOutput\" name=\"harvesterOutput\" readonly=\"readonly\">ZZZZZZZZ</textarea>" +
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
"			<td style='font-family: sans-serif; font-weight: bold; background-color : #E8E8E8; color : #ffffff'>&nbsp;</td>" +
"		</tr>" +
"	</table>" +

"<script>"+
"var foldFunc = CodeMirror.newFoldFunction(CodeMirror.braceRangeFinder);"+
"var sourceJsonEditor = CodeMirror.fromTextArea(document.getElementById(\"harvesterOutput\"), {"+
"	mode: \"application/json\","+
"	lineNumbers: true,"+
"	matchBrackets: true"+
"});"+
"sourceJsonEditor.setSize(\"1000px\", \"600px\");"+
"sourceJsonEditor.on(\"gutterClick\", foldFunc);"+
"</script>"+

"</body>" +
"</html>";

