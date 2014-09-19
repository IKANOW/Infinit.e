from javax.net.ssl import TrustManager, X509TrustManager
from jarray import array
from javax.net.ssl import SSLContext
class TrustAllX509TrustManager(X509TrustManager):
    '''Define a custom TrustManager which will blindly accept all certificates'''
 
    def checkClientTrusted(self, chain, auth):
            pass
 
    def checkServerTrusted(self, chain, auth):
            pass
 
    def getAcceptedIssuers(self):
                return None
# Create a static reference to an SSLContext which will use
# our custom TrustManager
trust_managers = array([TrustAllX509TrustManager()], TrustManager)
TRUST_ALL_CONTEXT = SSLContext.getInstance("SSL")
TRUST_ALL_CONTEXT.init(None, trust_managers, None)

from javax.net.ssl import SSLContext
SSLContext.setDefault(TRUST_ALL_CONTEXT)
    
from javax.net.ssl import HostnameVerifier, HttpsURLConnection
class AllHostsVerifier(HostnameVerifier):
    def verify(self, urlHostname, session):
        return True
        
HttpsURLConnection.setDefaultHostnameVerifier(AllHostsVerifier())