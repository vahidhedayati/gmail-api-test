package test

import grails.util.Holders

import javax.mail.internet.MimeMessage

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
import com.google.api.services.gmail.model.Message
class TestingController {
	
	def gmailService
	
	/*
	 * You must get authorisation key from:
	 *
	 * https://console.developers.google.com/flows/enableapi?apiid=gmail&credential=client_key
	 * explained here : https://developers.google.com/gmail/api/auth/web-server
	 *
	 * Once you have the file store it in a location as per configuration
	 */
	private final String GOOGLEFILE = Holders.config.grailsApplication.config.gmailSecurityFile ?:  'client_secret.json'
	private final  File SECURITY_FILE = new File(Holders.config.grailsApplication.config.gmailSecurityPath ?: 'c:\\\\gmail-test\\\\'+GOOGLEFILE);
	private final java.io.File DATA_STORE_DIR = new java.io.File(Holders.config.grailsApplication.config.gmailStoragePath ?: 'c:\\\\gmail-test\\\\vh')
	private  FileDataStoreFactory DATA_STORE_FACTORY= new FileDataStoreFactory(DATA_STORE_DIR);
	private final JsonFactory JSON_FACTORY =JacksonFactory.getDefaultInstance()
	
	private HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
	private final List<String> SCOPES = Arrays.asList(GmailScopes.MAIL_GOOGLE_COM) //GmailScopes.MAIL_GOOGLE_COM, GmailScopes.GMAIL_SEND, GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_COMPOSE, GmailScopes.GMAIL_INSERT, GmailScopes.GMAIL_MODIFY);
	
	//Your Google API Application Name
	private final String APPLICATION_NAME = 'TEST'

	private String emailBox='*****@gmail.com' // to above verified account / person sending email
	private String to ='someUser@xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.com'

	private String view = '/test/index'
	
	def index() {
		render view : view
	}
	
	def attachment() {
		render view : '/test/attachment'
	}
	
	
	def inlineImage() {
		flash.message= "Click send standard send email to trigger inline attachment check service it is looking for c:\\temp\\images.jpg"
		params.forwardAction='sendInlineImage'
		params.html='<img src="cid:myimage" /> <h1>inline image</h1>'
		render view: '/test/index'
	}
	/**
	 * Creates an authorized Credential object.
	 * @return an authorized Credential object.
	 * @throws IOException
	 */
	private Credential authorize() throws IOException {
		InputStream in1 = new FileInputStream(SECURITY_FILE);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in1));
		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
			.setDataStoreFactory(DATA_STORE_FACTORY)
			.setAccessType("offline")
			.build()
		//LocaleServerReceiver randomises ports for incoming /Callback url 
	    //This has been hardcoded as app port running on localhost
						
		LocalServerReceiver receiver = new LocalServerReceiver()
		receiver.port=9091		
		Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
		//println "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath()	
		return credential
	}

	/**
	 * Build and return an authorized Gmail client service.
	 * @return an authorized Gmail client service
	 * @throws IOException
	 */
	private Gmail getGmail() throws IOException {
		Credential credential =  authorize()
		return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build()
	}
	
	
	def verifyBounces() {
		def bounces = gmailService.verifyBounceList(gmail)
		render view:'/test/bounces', model:[instance:bounces]
	}
	
	def sendHTML() {
		to=params.to?:to
		String subject=params.subject?:'gmail test'
		String body=params.message?:'testing gmail via app'
		String html=params.html?:"<html><body><table><tr><td><b>aa</b></td><td>bb</td></tr></table><h1>html content</h1></body></html>"
		MimeMessage content = gmailService.createHTMLEmail(to,emailBox,subject,body,html)
		def message = gmailService.sendMessage(gmail,'me',content)
		flash.message= "Message sent id was: ${message.id}"
		render view:view
	}
	
	
	def sendInlineImage() {
		to=params.to?:to
		String subject=params.subject?:'gmail test'
		String body=params.message?:'testing gmail via app'
		String html=params.html?:"<html><body><table><tr><td><b>aa</b></td><td>bb</td></tr></table><h1>html content</h1></body></html>"
		MimeMessage content = gmailService.createInlineEmail(to,emailBox,subject,body,html)
		try {
			Message message = gmailService.sendMessage(gmail,'me',content)
			flash.message="Message sent id was: ${message.id}"
			render view:view
			return
		} catch (Throwable t) {
			//	only there is a network/provider issue - not related to validity of email/recipient delivery
		}
		flash.message="Issue sending email"
		render view:view
	}
	
	def sendEmail() {
		to=params.to?:to
		String subject=params.subject?:'gmail test'
		String body=params.message?:'testing gmail via app'
		MimeMessage content = gmailService.createEmail(to,emailBox,subject,body)		
		try {
			Message message = gmailService.sendMessage(gmail,'me',content)
			flash.message="Message sent id was: ${message.id}"
			render view:view
			return
		} catch (Throwable t) {
			//	only there is a network/provider issue - not related to validity of email/recipient delivery			
		}
		flash.message="Issue sending email"
		render view:view
	}
	
	def sendEmails() {
		to=params.to?:to
		String subject=params.subject?:'gmail test'
		String body=params.message?:'testing gmail via app'
		MimeMessage content1 = gmailService.createEmails([to,emailBox],emailBox,subject,body)
		try {
			Message message = gmailService.sendMessage(gmail,'me',content1)
			flash.message="Message sent id was: ${message.id}"
			render view:view
			return
		} catch (Throwable t) {
			//println "-- $t errors ${t}"
		}
		flash.message="Issue sending email"
		render view:view
	}
	
	def sendAttachment() {
		to=params.to?:to
		String subject=params.subject?:'gmail test'
		String body=params.message?:'testing gmail via app'
		def attachment = request.getFile('attachment')
		def MimeMessage content1 = gmailService.createEmailWithAttachment(to,emailBox,subject,body,attachment)
		try {
			Message message = gmailService.sendMessage(gmail,'me',content1)
			flash.message="Message sent id was: ${message.id}"
			render view:view
			return
		} catch (Throwable t) {
			//println "-- $t errors ${t}"
		}
		flash.message="Issue sending email"
		render view:view = request.getFile('attachment')
		
	}
}
