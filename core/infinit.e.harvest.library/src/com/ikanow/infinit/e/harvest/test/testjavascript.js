importPackage(java.net);
importPackage(java.util);
importPackage(java.io);
importPackage(java.lang); 
importPackage(net.sf.json); 
importPackage(net.sf.json.JSONSerializer); 
var myURL = new java.net.URL('http://dev.ikanow.com/api/auth/login/ping/ping'); 
var urlConnect = myURL.openConnection(); 
var retVal = urlConnect.getInputStream(); 
var is = new InputStreamReader(retVal);
var sb = new StringBuilder();
var br = new BufferedReader(is);
var read = br.readLine();
while ( read != null ) { sb.append(read); read = br.readLine(); }
var json = sb.toString();			
retVal.close();
json;		