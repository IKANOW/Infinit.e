<%!

// (Note saved this as ".inc" fragment to avoid Eclipse errors, since you can't nested imports sensibly in JSPs)

private String getUserName(HttpServletRequest request, HttpServletResponse response)
{
	//try to call person/get to get username
	String name = "";
	try
	{
		JSONObject personResponse = new JSONObject( getPerson( request, response) );
		if (personResponse.has("data")) 
		{
			JSONObject person = personResponse.getJSONObject("data");		
			String firstName = person.getString("firstName");
			String lastName = person.getString("lastName");
			name = firstName + " " + lastName;
		}
	} 
	catch (Exception e) 
	{
		System.out.println(e.getMessage());
	}
	return name;
}
%>

<table width="100%" cellpadding="0" cellspacing="0" >
<tr valign="middle">
	<td width="100%" background="image/infinite_logo_bg.png">
		<table width="100%" cellpadding="0" cellspacing="0" >
			<tr valign="bottom">
				<td width="200"><a href="index.jsp"><img height="50" width="200" src="<%=getLogoUrl() %>" border="0"></a></td>
				<td>
					<a href="people.jsp" class="headerLink" title="<fmt:message key='header.people.description' />"><fmt:message key='header.people.title' /></a> &nbsp; &nbsp;
					<a href="communities.jsp" class="headerLink" title="<fmt:message key='header.communities.description' />"><fmt:message key='header.communities.title' /></a> &nbsp; &nbsp;
					<a href="sources.jsp" class="headerLink" title="<fmt:message key='header.source_editor.description' />"><fmt:message key='header.source_editor.title' /></a> &nbsp; &nbsp;
					<a href="index.jsp" class="headerLink" title="<fmt:message key='header.home.description' />"><fmt:message key='header.home.title' /></a> &nbsp; &nbsp;
					<a href="<% out.println(SSO_LOGOUT_URL); %>" class="headerLink" title="<fmt:message key='header.logout.description' />"><fmt:message key='header.logout.title' /></a>
				</td>
				<td align="right">
					<a class="headerLink"><%=getUserName(request, response) %></a> &nbsp; &nbsp;
				</td>
				<td align="right" width="120" style="<%=showPoweredByIkanowLogo() %>">
					<a href="/"><img src="image/ikanow_logo_smaller_bg.png" border="0"></a>
				</td>
				
			</tr>
		</table>
	</td>
</tr>
<tr>
	<td bgcolor="#ffffff">

	
