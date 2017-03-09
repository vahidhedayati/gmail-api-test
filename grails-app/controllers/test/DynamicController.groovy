package test

import gmail.GmailAccount

import javax.mail.internet.MimeMessage

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message

/**
 * When google secret file folder location is provided 
 * In GmailAccount dataStoragePath a file is created that stores credentials.
 * 
 * This can at any / all times be one file - so you can't override accounts 
 * 
 * Additional params.account and params.folder needs to be set when controller called to override 
 * underlying gmail accounts.
 * http://localhost:8080/gmail-api-test/dynamic?account=client_secret.json?folder=account1
 * http://localhost:8080/gmail-api-test/dynamic?account=vh.json&folder=account2
 * 
 * where c:\gmail-test\accounts\account1\client_secret.json already exists and  c:\gmail-test\accounts\account1\vh will be created to store credentials
 * where c:\gmail-test\accounts\account2\vh.json already exists and  c:\gmail-test\accounts\account2\vh will be created to store credentials
 * 
 * 
 * @author vahid
 *
 */
class DynamicController {
	
	def gmailService

	private String emailBox='****@gmail.com' // to above verified account / person sending email
	private String to ='someUser@xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.com'

	private String view = '/test/index'
	
	def index() {
		render view : view
	}
	def attachment() {
		render view : '/test/attachment'
	}
	
	def inlineImage() {
		flash.message= "Click send standard send email to trigger inline attachment check service it is looking for c:\\gmail-test\\images.jpg"
		params.forwardAction='sendInlineImage'
		params.html='<img src="cid:myimage" /> <h1>inline image</h1>'
		render view: '/test/index'
	}
	
	/**
	 * Can be given ?account=client_secret.json and override authorization account to point to another 
	 * secret file
	 * if not provided will use defaults in GmailAccount
	 * @return
	 */
	def verifyBounces() {
		GmailAccount account=new GmailAccount()
		if (params.account && params.folder) {
			account.secretFolder=params.folder
			account.secretFile=params.account
		}
		def bounces = gmailService.verifyBounceList(account.gmail)
		render view:'/test/bounces', model:[instance:bounces]
	}
	
	def sendHTML() {
		GmailAccount account=new GmailAccount()
		if (params.account && params.folder) {
			account.secretFolder=params.folder
			account.secretFile=params.account
		}
		to=params.to?:to
		String subject=params.subject?:'gmail test'
		String body=params.message?:'testing gmail via app'
		String html=params.html?:"<html><body><table><tr><td><b>aa</b></td><td>bb</td></tr></table><h1>html content</h1></body></html>"
		MimeMessage content = gmailService.createHTMLEmail(to,emailBox,'gmail test','testing gmail via app',html)
		def message = gmailService.sendMessage(account.gmail,'me',content)
		flash.message= "Message sent id was: ${message.id}"
		render view:view
	}
	
	def sendInlineImage() {
		GmailAccount account=new GmailAccount()
		if (params.account && params.folder) {
			account.secretFolder=params.folder
			account.secretFile=params.account
		}
		to=params.to?:to
		String subject=params.subject?:'gmail test'
		String body=params.message?:'testing gmail via app'
		String html=params.html?:"<html><body><table><tr><td><b>aa</b></td><td>bb</td></tr></table><h1>html content</h1></body></html>"
		MimeMessage content = gmailService.createInlineEmail(to,emailBox,subject,body,html)
		try {
			Message message = gmailService.sendMessage(account.gmail,'me',content)
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
		GmailAccount account=new GmailAccount()
		if (params.account && params.folder) {
			account.secretFolder=params.folder
			account.secretFile=params.account
		}
		to=params.to?:to
		String subject=params.subject?:'gmail test'
		String body=params.message?:'testing gmail via app'
		MimeMessage content = gmailService.createEmail(to,emailBox,subject,body)		
		try {
			Message message = gmailService.sendMessage(account.gmail,'me',content)
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
		GmailAccount account=new GmailAccount()
		if (params.account && params.folder) {
			account.secretFolder=params.folder
			account.secretFile=params.account
		}
		to=params.to?:to
		String subject=params.subject?:'gmail test'
		String body=params.message?:'testing gmail via app'
		MimeMessage content1 = gmailService.createEmails([to,emailBox],emailBox,subject,body)
		try {
			Message message = gmailService.sendMessage(account.gmail,'me',content1)
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
		GmailAccount account=new GmailAccount()
		if (params.account && params.folder) {
			account.secretFolder=params.folder
			account.secretFile=params.account
		}		
		to=params.to?:to
		String subject=params.subject?:'gmail test'
		String body=params.message?:'testing gmail via app'
		def attachment = request.getFile('attachment')
		def MimeMessage content1 = gmailService.createEmailWithAttachment(to,emailBox,subject,body,attachment)
		try {
			Message message = gmailService.sendMessage(account.gmail,'me',content1)
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
