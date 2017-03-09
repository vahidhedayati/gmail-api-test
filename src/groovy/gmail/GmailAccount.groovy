package gmail

import grails.util.Holders
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes

class GmailAccount {
	
	String secretFile = Holders.config.grailsApplication.config.gmailSecurityFile ?:  'client_secret.json'
	String securityPath = Holders.config.grailsApplication.config.gmailSecurityPath ?: 'c:\\\\gmail-test\\\\accounts\\\\'
	String secretFolder = Holders.config.grailsApplication.config.gmailSecurityFolder ?:  'account1'
	String dataStoragePath = Holders.config.grailsApplication.config.gmailStoragePath ?: 'vh'
	String appName =   Holders.config.grailsApplication.config.gmailAppName ?: 'TEST'
	
	Map initialise() {
		File SECURITY_FILE=new File(securityPath+secretFolder+File.separator+secretFile);
		File DATA_STORE_DIR = new java.io.File(securityPath +secretFolder+File.separator+dataStoragePath)
		FileDataStoreFactory DATA_STORE_FACTORY= new FileDataStoreFactory(DATA_STORE_DIR)
		JsonFactory JSON_FACTORY =JacksonFactory.getDefaultInstance()
		HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
		List<String> SCOPES = Arrays.asList(GmailScopes.MAIL_GOOGLE_COM)
		String APPLICATION_NAME = appName
		return [SECURITY_FILE:SECURITY_FILE,DATA_STORE_DIR:DATA_STORE_DIR,DATA_STORE_FACTORY:DATA_STORE_FACTORY,JSON_FACTORY:JSON_FACTORY,
			HTTP_TRANSPORT:HTTP_TRANSPORT,SCOPES:SCOPES,APPLICATION_NAME:APPLICATION_NAME]
	}
	
	
	/**
	 * Creates an authorized Credential object.
	 * @return an authorized Credential object.
	 * @throws IOException
	 */
	Credential authorize(Map init) throws IOException {
		InputStream in1 = new FileInputStream(init.SECURITY_FILE);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(init.JSON_FACTORY, new InputStreamReader(in1));
		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(init.HTTP_TRANSPORT, init.JSON_FACTORY, clientSecrets, init.SCOPES)
			.setDataStoreFactory(init.DATA_STORE_FACTORY)
			.setAccessType("offline")
			.build()
		//LocaleServerReceiver randomises ports for incoming /Callback url
		//This has been hardcoded as app port running on localhost
		LocalServerReceiver receiver = new LocalServerReceiver()
		receiver.port=9091
		Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
		return credential
	}
	/**
	 * Build and return an authorized Gmail client service.
	 * @return an authorized Gmail client service
	 * @throws IOException
	 */
	 Gmail getGmail() throws IOException {
		 Map init = initialise()
		 println "secret file is "+secretFile
		 Credential credential =  authorize(init)
		 return new Gmail.Builder(init.HTTP_TRANSPORT, init.JSON_FACTORY, credential).setApplicationName(init.APPLICATION_NAME).build()
	 }
	
}
