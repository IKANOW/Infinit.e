import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

public class CreatePDFServlet extends HttpServlet {

public void doPost(HttpServletRequest req, HttpServletResponse resp)
throws ServletException, IOException
{
doGet(req, resp);
}

public void doGet(HttpServletRequest req, HttpServletResponse resp)
throws ServletException, IOException
{
int i = 0;
int k = 0;
int maxLength = req.getContentLength();
byte[] bytes = new byte[maxLength];
String method = req.getParameter("method");
String name = req.getParameter("name");
ServletInputStream si = req.getInputStream();
while (true)
{
k = si.read(bytes,i,maxLength);
i += k;
if (k <= 0)
break;
}
if (bytes != null)
{
ServletOutputStream stream = resp.getOutputStream();
resp.setContentType("application/pdf");
resp.setContentLength(bytes.length);
resp.setHeader("Content-Disposition",method + ";filename=" + name);
stream.write(bytes);
stream.flush();
stream.close();
}
else
{
resp.setContentType("text");
resp.getWriter().write("bytes is null");
}
}
}
