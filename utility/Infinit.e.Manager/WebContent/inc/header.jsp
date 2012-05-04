<table width="100%" cellpadding="0" cellspacing="0" >
<tr valign="middle">
	<td width="100%" background="image/infinite_logo_bg.png">
		<table width="100%" cellpadding="0" cellspacing="0" >
			<tr valign="bottom">
				<td width="200"><a href="index.jsp"><img src="image/infinite_logo.png" border="0"></a></td>
				<td>
					<a href="people.jsp" class="headerLink" title="Add/Edit Users">People</a> &nbsp; &nbsp;
					<a href="communities.jsp" class="headerLink" title="Add/Edit Communities">Communities</a> &nbsp; &nbsp;
					<a href="sources.jsp" class="headerLink" title="Add/Edit Sources">Sources</a> &nbsp; &nbsp;
					<a href="files.jsp" class="headerLink" title="Add/Edit Files">Files</a> &nbsp; &nbsp;
					<a href="index.jsp" class="headerLink" title="Home">Home</a>
				</td>
				<td align="right" widht="5%">
<%
	if (isLoggedIn) 
	{
		// Note:
		// This invisible doNothingButton button below does nothing hence the name doNothingButton.
		// Its purpose for being is to keepusers from logging themselves out every time they press 
		// the return key inside a form. Do not remove or make visible.
%>
					<div style="visibility:hidden"><button name="doNothingButton" value="doNothingButton"></button></div>
					<button name="logoutButton" value="logoutButton">Logout</button>
<%
	}
%>
				</td>
			</tr>
		</table>
	</td>
</tr>
<tr>
	<td bgcolor="#ffffff">
