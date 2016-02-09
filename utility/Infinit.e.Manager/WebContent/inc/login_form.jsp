<!-- Begin login_form.jsp  -->

<% if (null != SSO_LOGIN_URL) { 

   // New location to be redirected
	final String curr_page_enc = 
	(null == request.getQueryString())
	? java.net.URLEncoder.encode(request.getRequestURL().toString())
	: java.net.URLEncoder.encode(request.getRequestURL().toString() + "?" + request.getQueryString())
	;
   String site = SSO_LOGIN_URL.replace("$curr_page_enc", curr_page_enc);
   response.setStatus(response.SC_MOVED_TEMPORARILY);
   response.setHeader("Location", site); 

} else { 	
%>

<br />
<br />
<br />
<br />
<center>
<form method="post" name="login_form">
<table class="standardTable" cellpadding="5" cellspacing="1" width="35%" >
	<tr>
		<td colspan="2" align="center">
			<font color="white"><b>Login to Infinit.e.Manager</b></font>
		</td>
	</tr>
	<tr>
		<td bgcolor="white" width="40%">User Name:</td>
		<td bgcolor="white" width="60%"><input type="text" name="username" size="40"></td>
	</tr>
	<tr>
		<td bgcolor="white" width="40%">Password:</td>
		<td bgcolor="white" width="60%"><input type="password" name="password" size="40"></td>
	</tr>
	<tr>
		<td colspan="2" align="right"><input type="submit"></td>
	</tr>
</table>
</form>
</center>
<br />
<br />
<br />
<br />
<% }%>

<!-- End login_form.jsp  -->