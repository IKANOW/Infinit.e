package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.io.*;
import java.util.*;
import java.net.*;
import com.google.gson.Gson;
import org.apache.commons.io.*;
import sun.misc.BASE64Encoder;
import java.security.*;;

public final class moduleUploader_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {


	static String API_ROOT = "http://127.0.0.1:8184/";
	static CookieManager cm = new CookieManager();

	static class keepAlive
	{
		static class ka
		{
			String action;
			Boolean success;
			String message;
			int time;
		
		}
		public ka response;
		
	}
	static class logIn
	{
		static class loginData
		{
			public String action;
			public Boolean success;
			public int time;
			
		}
		public loginData response;
	}
	static class modResponse
	{
		static class moduleResponse
		{
			public String action;
			public Boolean success;
			public String message;
			public int time;
			
		}
		public moduleResponse response;
		public String data;
	}
	
	public static String stringOfUrl(String addr)
	{
		CookieHandler.setDefault(cm);
        try
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
        	URL url = new URL(addr);
        	IOUtils.copy(url.openStream(), output);
        	return output.toString();
        }
        catch(IOException e)
        {
        	return null;
        }
    }
	private static String encrypt(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{	
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(password.getBytes("UTF-8"));			
		return URLEncoder.encode((new BASE64Encoder()).encode(md.digest()));	
	}
	private Boolean logMeIn(String username, String pword) throws IOException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException 
    {
		String json = stringOfUrl(API_ROOT + "auth/login/"+username+"/"+encrypt(pword));
		logIn login = new Gson().fromJson(json, logIn.class);
		if (login == null)
			return false;
		
		return login.response.success;
    }
	
	public Boolean isLoggedIn()
	{
		String json = stringOfUrl(API_ROOT + "auth/keepalive");
		if (json != null)
		{
			keepAlive keepA = new Gson().fromJson(json, keepAlive.class);
			return keepA.response.success;
		}
		else
		{
			return null;
		}
	}
	private String addModule(String title, String description, byte[] bytes) throws UnsupportedEncodingException
	{
		String charset = "UTF-8";
		String url = API_ROOT + "share/add/binary/" + URLEncoder.encode(title,charset) + "/" + URLEncoder.encode(description,charset) + "/";
		return AddUpdateModule(url, bytes);
	}
	private String updateModule(String title, String description, String shareid , byte[] bytes) throws UnsupportedEncodingException
	{
		String charset = "UTF-8";
		String url = API_ROOT + "share/update/binary/" + shareid + "/" + URLEncoder.encode(title,charset) + "/" + URLEncoder.encode(description,charset) + "/";
		return AddUpdateModule(url, bytes);
	}
	private String AddUpdateModule(String fullURL, byte[] bytes)
	{
		try{
			String charset = "UTF-8";
			URLConnection connection = new URL(fullURL).openConnection();
			connection.setDoOutput(true);
	        connection.setRequestProperty("Accept-Charset",charset);
	        //connection.setRequestProperty("Content-Type", "application/x-shockwave-flash;charset=" + charset);
	        DataOutputStream output = new DataOutputStream(connection.getOutputStream());
	        DataInputStream response = new DataInputStream(connection.getInputStream());
            output.write(bytes);
            
            
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = response.read(data,0,data.length)) != -1)
            {
                buffer.write(data,0,nRead);
            }
            
            String json = buffer.toString();
            buffer.flush();
            buffer.close();
            output.close();
            response.close();
            
            modResponse mr = new Gson().fromJson(json, modResponse.class);
    		if (mr == null)
    			return json;
    		if (mr.response.success == true)
    		{
    			if (mr.data == null)
    				return "Module Updated Successfully<br>";
    			else
    				return "Module Uploaded Successfully! Please make note of the Share ID below for future updates<br> Share ID: " + mr.data;
    		}
    		else
    		{
    			return "Upload Failed: " + mr.response.message;
    		}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return e.toString();
		}
		
	}

  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  private javax.el.ExpressionFactory _el_expressionfactory;
  private org.apache.AnnotationProcessor _jsp_annotationprocessor;

  public Object getDependants() {
    return _jspx_dependants;
  }

  public void _jspInit() {
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
  }

  public void _jspDestroy() {
  }

  public void _jspService(HttpServletRequest request, HttpServletResponse response)
        throws java.io.IOException, ServletException {

    PageContext pageContext = null;
    HttpSession session = null;
    ServletContext application = null;
    ServletConfig config = null;
    JspWriter out = null;
    Object page = this;
    JspWriter _jspx_out = null;
    PageContext _jspx_page_context = null;


    try {
      response.setContentType("text/html; charset=utf-8");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			"", true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
      out.write("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
      out.write("<head>\n");
      out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n");
      out.write("<title>Infinit.e Module Upload Tool</title>\n");
      out.write("<style media=\"screen\" type=\"text/css\">\n");
      out.write("\n");
      out.write("body \n");
      out.write("{\n");
      out.write("\tfont: 14px Arial,sans-serif;\n");
      out.write("}\n");
      out.write("h2\n");
      out.write("{\n");
      out.write("\tfont-family: \"Times New Roman\";\n");
      out.write("\tfont-style: italic;\n");
      out.write("\tfont-variant: normal;\n");
      out.write("\tfont-weight: normal;\n");
      out.write("\tfont-size: 24px;\n");
      out.write("\tline-height: 29px;\n");
      out.write("\tfont-size-adjust: none;\n");
      out.write("\tfont-stretch: normal;\n");
      out.write("\t-x-system-font: none;\n");
      out.write("\tcolor: #d2331f;\n");
      out.write("\tmargin-bottom: 25px;\n");
      out.write("}\n");
      out.write("\n");
      out.write("</style>\n");
      out.write("</head>\n");
      out.write("\n");
      out.write("<body>\n");

Boolean isLoggedIn = isLoggedIn();

if (isLoggedIn == null)
{
	out.print("The Infinit.e API cannot be reached");
}

else if (isLoggedIn == true)
{ 
	String contentType = request.getContentType();
	if ((contentType != null) && (contentType.indexOf("multipart/form-data") >= 0 ))
	{
		
//		Create a new file upload handler
 		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
//		Parse the request
 		FileItemIterator iter = upload.getItemIterator(request);
		byte[] bytes = null;
 		while (iter.hasNext()) {
 		    FileItemStream item = iter.next();
 		    String name = item.getFieldName();
 		    InputStream stream = item.openStream();
 		    if (item.isFormField()) {
 		    	request.setAttribute(name, Streams.asString(stream));
 		    } else 
 		    {
 		        bytes = IOUtils.toByteArray(stream);
 		    }
 		}
		
		
		if(request.getAttribute("title") != null && request.getAttribute("description") != null && bytes != null)
		{
			out.println("<div style=\" text-align: center;\">");
			if (!request.getAttribute("shareid").toString().equals("")) //Update Existing Module
			{
				out.println(updateModule(request.getAttribute("title").toString(),request.getAttribute("description").toString(),request.getAttribute("shareid").toString(), bytes));
			}
			else
			{
				out.println(addModule(request.getAttribute("title").toString(),request.getAttribute("description").toString(), bytes));
			}
			out.println("</div>");
		}
	}
	else
	{
	}


      out.write("\n");
      out.write("\n");
      out.write("<script>\n");
      out.write("\tfunction hideShareId(hide)\n");
      out.write("\t{\n");
      out.write("\t\tif (hide == true)\n");
      out.write("\t\t{\n");
      out.write("\t\t\tdocument.getElementById('shareIdRow').style.display = 'none';\n");
      out.write("\t\t}\n");
      out.write("\t\telse\n");
      out.write("\t\t{\n");
      out.write("\t\t\tdocument.getElementById('shareIdRow').style.display = '';\n");
      out.write("\t\t}\n");
      out.write("\t}\n");
      out.write("\tfunction validate_fields()\n");
      out.write("\t{\n");
      out.write("\t\tshareid = document.getElementById('shareid').value;\n");
      out.write("\t\ttitle = document.getElementById('title').value;\n");
      out.write("\t\tdescription = document.getElementById('description').value;\n");
      out.write("\t\tfile = document.getElementById('file').value;\n");
      out.write("\t\tstate_button = document.getElementsByName('state');\n");
      out.write("\t\tstate = \"\";\n");
      out.write("\t\tfor(var i = 0; i < state_button.length; i++) {\n");
      out.write("\t\t\tif(state_button[i].checked) {\n");
      out.write("\t\t\t\tstate = state_button[i].value;\n");
      out.write("\t\t\t}\n");
      out.write("\t\t}\n");
      out.write("\t\tif (state == \"update\" && shareid == \"\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\talert('Please provide the Module\\'s Share ID');\n");
      out.write("\t\t\treturn false;\n");
      out.write("\t\t}\n");
      out.write("\t\tif (title == \"\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\talert('Please provide a title.');\n");
      out.write("\t\t\treturn false;\n");
      out.write("\t\t}\n");
      out.write("\t\tif (description == \"\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\talert('Please provide a description.');\n");
      out.write("\t\t\treturn false;\n");
      out.write("\t\t}\n");
      out.write("\t\tif (file == \"\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\talert('Please provide a description.');\n");
      out.write("\t\t\treturn false;\n");
      out.write("\t\t}\n");
      out.write("\t}\n");
      out.write("</script>\n");
      out.write("\t<div id=\"uploader_outter_div\" name=\"uploader_outter_div\" align=\"center\" style=\"width:100%\" >\n");
      out.write("    \t<div id=\"uploader_div\" name=\"uploader_div\" style=\"border-style:solid; border-color:#999999; border-radius: 10px; width:450px; margin:auto\">\n");
      out.write("        \t<h2>Module Uploader</h2>\n");
      out.write("            <form id=\"upload_form\" name=\"upload_form\" method=\"post\" enctype=\"multipart/form-data\" onsubmit=\"javascript:return validate_fields();\" >\n");
      out.write("                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding-left:10px; padding-right:10px\">\n");
      out.write("                  <tr>\n");
      out.write("                    <td colspan=\"2\" align=\"center\"><input type=\"radio\" name=\"state\" id=\"state\" onchange=\"hideShareId(true);\" value=\"new\" checked=\"checked\" />Upload New Module <input type=\"radio\" name=\"state\" id=\"state\" onchange=\"hideShareId(false);\" value=\"update\" />Update Module</td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr id=\"shareIdRow\" style=\"display: none;\">\n");
      out.write("                    <td>Share Id:</td>\n");
      out.write("                    <td><input type=\"text\" name=\"shareid\" id=\"shareid\" /></td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr>\n");
      out.write("                    <td>Title:</td>\n");
      out.write("                    <td><input type=\"text\" name=\"title\" id=\"title\" /></td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr>\n");
      out.write("                    <td>Description:</td>\n");
      out.write("                    <td><textarea rows=\"4\" cols=\"30\" name=\"description\" id=\"description\" ></textarea></td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr>\n");
      out.write("                    <td>File:</td>\n");
      out.write("                    <td><input type=\"file\" name=\"file\" id=\"file\" /></td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr>\n");
      out.write("                    <td colspan=\"2\" style=\"text-align:right\"><input type=\"submit\" value=\"Submit\" /></td>\n");
      out.write("                  </tr>\n");
      out.write("                </table>\n");
      out.write("\n");
      out.write("\t\t\t</form>\n");
      out.write("        </div>\n");
      out.write("    </div>\n");
      out.write("\n");
	
}
else if (isLoggedIn == false)
{
	String errorMsg = "";
	if (request.getParameter("logintext") != null || request.getParameter("passwordtext") != null)
	{
		if(logMeIn(request.getParameter("logintext"),request.getParameter("passwordtext")))
		{
			out.println("<meta http-equiv=\"refresh\" content=\"0\">");
			out.println("Login Success");
		}
		else
		{
			errorMsg = "Login Failed, Please Try again";
		}
		
	}
	

      out.write("\n");
      out.write("\n");
      out.write("<script>\n");
      out.write("\tfunction validate_fields()\n");
      out.write("\t{\n");
      out.write("\t\tuname = document.getElementById('logintext').value;\n");
      out.write("\t\tpword = document.getElementById('passwordtext').value;\n");
      out.write("\t\t\n");
      out.write("\t\tif (uname == \"\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\talert('Please provide your username.');\n");
      out.write("\t\t\treturn false;\n");
      out.write("\t\t}\n");
      out.write("\t\tif (pword == \"\")\n");
      out.write("\t\t{\n");
      out.write("\t\t\talert('Please provide your password.');\n");
      out.write("\t\t\treturn false;\n");
      out.write("\t\t}\n");
      out.write("\t}\n");
      out.write("\n");
      out.write("\n");
      out.write("</script>\n");
      out.write("\t<div id=\"login_outter_div\" name=\"login_outter_div\" align=\"center\" style=\"width:100%\" >\n");
      out.write("    \t<div id=\"login_div\" name=\"login_div\" style=\"border-style:solid; border-color:#999999; border-radius: 10px; width:450px; margin:auto\">\n");
      out.write("        \t<h2>Login</h2>\n");
      out.write("            <form id=\"login_form\" name=\"login_form\" method=\"post\" onsubmit=\"javascript:return validate_fields();\" >\n");
      out.write("                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding-left:10px\">\n");
      out.write("                  <tr>\n");
      out.write("                    <td>User Name</td>\n");
      out.write("                    <td>&nbsp;</td>\n");
      out.write("                    <td>Password</td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr>\n");
      out.write("                    <td><input type=\"text\" name=\"logintext\" id=\"logintext\" width=\"190px\" /></td>\n");
      out.write("                    <td>&nbsp;</td>\n");
      out.write("                    <td><input type=\"password\" name=\"passwordtext\" id=\"passwordtext\" width=\"190px\" /></td>\n");
      out.write("                  </tr>\n");
      out.write("                  <tr>\n");
      out.write("                    <td colspan=\"3\" align=\"right\"><input name=\"Login\" type=\"submit\" value=\"Login\" /></td>\n");
      out.write("                  </tr>\n");
      out.write("                </table>\n");
      out.write("\t\t\t</form>\n");
      out.write("        </div>\n");
      out.write("    </div>\n");
      out.write("\t<div style=\"color: red; text-align: center;\"> ");
      out.print(errorMsg );
      out.write(" </div>\n");
 
}

      out.write("\n");
      out.write("    \n");
      out.write("    \n");
      out.write("</body>\n");
      out.write("</html>\n");
    } catch (Throwable t) {
      if (!(t instanceof SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          try { out.clearBuffer(); } catch (java.io.IOException e) {}
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
      }
    } finally {
      _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}
