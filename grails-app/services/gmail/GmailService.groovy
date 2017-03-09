package gmail

import grails.transaction.Transactional

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import javax.activation.DataHandler
import javax.activation.DataSource
import javax.activation.FileDataSource
import javax.mail.BodyPart
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

import org.springframework.web.multipart.MultipartFile

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.Message

@Transactional
class GmailService {

	/**
	 * Bind BounceRecord which contains
	 * key = String = messageId (from gmail Api)
	 * Value = BounceRecord object =  all addressees of the email plus date sent and messageId 
	 */
	private static final ConcurrentMap<String, BounceRecord> messageMap = new ConcurrentHashMap<String, BounceRecord>()

	/**
	 * Triggered action from controller to inspect mailbox for bounced email messages 
	 * from mailer-daemon and match this against the messageMap
	 * if found return result back to controller where it is rendered in bounce.gsp  
	 * @param service
	 * @return
	 */
	List verifyBounceList (Gmail service) {
		//cleanOldMessagesFromMap()
		List foundResults=[]
		def bounceRecords = listMessagesMatchingQuery(service,'me','mailer-daemon@googlemail.com')
		bounceRecords?.each {
			BounceRecord foundBounced = messageMap.get(it.threadId)
			if (!foundBounced) {
				foundBounced= messageMap.get(it.id)
			}
			if (foundBounced) {
				//cleanOldMessagesFromMap(it.id)
				foundResults<<[bouncedRecord:it,mapRecord:foundBounced]
			}
		}
		return foundResults
	}

	/**
	 * May need more work - to remove older recorders than an hour from 
	 * running concurrent hashMap - something should bounce within an 
	 * defaulted to an ..Hour
	 * @param messageId
	 */
	private void cleanOldMessagesFromMap() {
		Date checkDate = new Date()
		use (groovy.time.TimeCategory) {
			checkDate= checkDate - 1.hour
		}
		messageMap.findAll{it.value<checkDate}.remove()
	}


	/**
	 * Given a messageId remove it from concurrentMap
	 * Maybe this needs to be done as part of ui front end check verified they have a bounce
	 * from bounce.gsp
	 * @param messageId
	 */
	private void removeMessageFromMap(String messageId) {
		if (messageMap.get(messageId)) {
			messageMap.remove(messageId)
		}
	}

	/**
	 * Create a MimeMessage using the parameters provided.
	 *
	 * @param to email address of the receiver
	 * @param from email address of the sender, the mailbox account
	 * @param subject subject of the email
	 * @param bodyText body text of the email
	 * @return the MimeMessage to be used to send email
	 * @throws MessagingException
	 */
	MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
		Properties props = new Properties()
		Session session = Session.getDefaultInstance(props, null)
		MimeMessage email = new MimeMessage(session)
		email.setFrom(new InternetAddress(from))
		email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to))
		email.setSubject(subject)
		email.setText(bodyText)
		return email
	}

	/**
	 * Send an email from the user's mailbox to its recipient.
	 *
	 * @param service Authorized Gmail API instance.
	 * @param userId User's email address. The special value "me"
	 * can be used to indicate the authenticated user.
	 * @param emailContent Email to be sent.
	 * @return The sent message
	 * @throws MessagingException
	 * @throws IOException
	 */

	Message sendMessage(Gmail service,String userId,MimeMessage emailContent) throws MessagingException, IOException {
		try {
			Message message = createMessageWithEmail(emailContent)
			message = service.users().messages().send(userId, message).execute()
			BounceRecord record = new BounceRecord(emailContent.getAllRecipients(),new Date())
			messageMap.put(message.id,record )
			return message
		} catch (Exception e) {
			//log.error "${e}"
		}
	}

	/**
	 * Create a message from an email.
	 *
	 * @param emailContent Email to be set to raw of message
	 * @return a message containing a base64url encoded email
	 * @throws IOException
	 * @throws MessagingException
	 */
	Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream()
		emailContent.writeTo(buffer)
		byte[] bytes = buffer.toByteArray()
		String encodedEmail = Base64.encodeBase64URLSafeString(bytes)
		Message message = new Message()
		message.setRaw(encodedEmail)
		return message
	}

	/**
	 * Create HTML email 
	 * @param to
	 * @param from
	 * @param subject
	 * @param text
	 * @param html
	 * @return MimeMessage email (binded)
	 */
	MimeMessage createHTMLEmail(String to, String from, String subject, String text, String html) {
		Properties props = new Properties()
		Session session = Session.getDefaultInstance(props, null)

		MimeMessage email = new MimeMessage(session)
		Multipart multiPart = new MimeMultipart("alternative")
		email.setFrom(new InternetAddress(from))
		email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to))
		email.setSubject(subject)

		MimeBodyPart textPart = new MimeBodyPart()
		textPart.setText(text, "utf-8")

		MimeBodyPart htmlPart = new MimeBodyPart()
		htmlPart.setContent(html, "text/html; charset=utf-8")

		multiPart.addBodyPart(textPart)
		multiPart.addBodyPart(htmlPart)
		email.setContent(multiPart)
		return email
	}
	

	/**
	 * Creates an email with inline Image - refer to gsp which has a single image with html
	 * <img src="cid:myimage" />
	 * 
	 * This myimage is then translated locally in this app to c:\temp\images.jpg and sent with 
	 * html content along side html tag above.	
	 * @param to
	 * @param from
	 * @param subject
	 * @param text
	 * @param html
	 * @return
	 */
	MimeMessage createInlineEmail(String to, String from, String subject, String text, String html) {
		Properties props = new Properties()
		Session session = Session.getDefaultInstance(props, null)
		MimeMessage email = new MimeMessage(session)
		// This mail has 2 part, the BODY and the embedded image
		MimeMultipart multiPart = new MimeMultipart("related")
		email.setFrom(new InternetAddress(from))
		email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to))
		email.setSubject(subject)

		MimeBodyPart textPart = new MimeBodyPart()
		textPart.setText(text, "utf-8")

		BodyPart htmlPart = new MimeBodyPart()
		htmlPart.setContent(html, "text/html; charset=utf-8")
		multiPart.addBodyPart(htmlPart)
		htmlPart = new MimeBodyPart()
		DataSource fds = new FileDataSource("c:\\\\temp\\\\images.jpg");

		htmlPart.setDataHandler(new DataHandler(fds))
		htmlPart.setHeader("Content-ID", "<myimage>")
		// add image to the multipart
		multiPart.addBodyPart(htmlPart)
		email.setContent(multiPart)
		return email
	}
	

	/**
	 * Create a MimeMessage using the parameters provided.
	 *
	 * @param to Email address of the receiver.
	 * @param from Email address of the sender, the mailbox account.
	 * @param subject Subject of the email.
	 * @param bodyText Body text of the email.
	 * @param file Path to the file to be attached.
	 * @return MimeMessage to be used to send email.
	 * @throws MessagingException
	 */
	MimeMessage createEmailWithAttachment(String to, String from, String subject, String bodyText, File file)
	throws MessagingException, IOException {
		Properties props = new Properties()
		Session session = Session.getDefaultInstance(props, null)

		MimeMessage email = new MimeMessage(session)

		email.setFrom(new InternetAddress(from))
		email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to))
		email.setSubject(subject)

		MimeBodyPart mimeBodyPart = new MimeBodyPart()
		mimeBodyPart.setContent(bodyText, "text/plain")

		Multipart multipart = new MimeMultipart()
		multipart.addBodyPart(mimeBodyPart)

		mimeBodyPart = new MimeBodyPart()
		DataSource source = new FileDataSource(file)

		mimeBodyPart.setDataHandler(new DataHandler(source))
		mimeBodyPart.setFileName(file.getName())

		multipart.addBodyPart(mimeBodyPart)
		email.setContent(multipart)

		return email
	}

	/**
	 * Receives a grails uploaded multiPart file
	 * runs convert process to convert to real file
	 * and then passes to default createEmailWithAttachment 
	 * @param to
	 * @param from
	 * @param subject
	 * @param bodyText
	 * @param file
	 * @return
	 * @throws MessagingException
	 * @throws IOException
	 */
	
	MimeMessage createEmailWithAttachment(String to, String from, String subject, String bodyText, MultipartFile file)
	throws MessagingException, IOException {
		createEmailWithAttachment(to, from, subject, bodyText, convert(file))
	}
	
	/**
	 * Converts multiPart file to a real file
	 * @param file
	 * @return
	 */
	private File convert(MultipartFile file) {
		File convFile = new File(file.getOriginalFilename());
		convFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(convFile);
		fos.write(file.getBytes());
		fos.close();
		return convFile;
	}
	
	private File multipartToFile(MultipartFile multipart) throws IllegalStateException, IOException {
		File convFile = new File( multipart.getOriginalFilename());
		multipart.transferTo(convFile);
		return convFile;
	}

	/**
	 * Simply does a query in given mailbox 
	 * used to query for mail failures
	 * @param service
	 * @param userId
	 * @param query
	 * @return
	 * @throws IOException
	 */
	List<Message> listMessagesMatchingQuery(Gmail service, String userId, String query) throws IOException {
		ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute()

		List<Message> messages = new ArrayList<Message>()
		while (response.getMessages() != null) {
			messages.addAll(response.getMessages())
			if (response.getNextPageToken() != null) {
				String pageToken = response.getNextPageToken()
				response = service.users().messages().list(userId).setQ(query).setPageToken(pageToken).execute()
			} else {
				break;
			}
		}
		for (Message message : messages) {
			//System.out.println(message.toPrettyString());
		}

		return messages;
	}


	/**
	 * Same as createEmail just iterates through a list to add to: 'To' field
	 * @param to
	 * @param from
	 * @param subject
	 * @param bodyText
	 * @return
	 * @throws MessagingException
	 * Not used was just an example of many users to one email ------------------------------------
	 */
	MimeMessage createEmails(List to,String from,String subject,String bodyText) throws MessagingException {
		Properties props = new Properties()
		Session session = Session.getDefaultInstance(props, null)
		MimeMessage email = new MimeMessage(session)
		email.setFrom(new InternetAddress(from))
		to?.each { t->
			email.addRecipient(javax.mail.Message.RecipientType.TO,new InternetAddress(t))
		}
		email.setSubject(subject)
		email.setText(bodyText)
		return email
	}



	/**
	 * List all Messages of the user's mailbox with labelIds applied.
	 *
	 * @param service Authorized Gmail API instance.
	 * @param userId User's email address. The special value "me"
	 * can be used to indicate the authenticated user.
	 * @param labelIds Only return Messages with these labelIds applied.
	 * @throws IOException
	 * 
	 * Not used ------------------------------------
	 */
	List<Message> listMessagesWithLabels(Gmail service, String userId, List<String> labelIds) throws IOException {
		ListMessagesResponse response = service.users().messages().list(userId).setLabelIds(labelIds).execute()

		List<Message> messages = new ArrayList<Message>()
		while (response.getMessages() != null) {
			messages.addAll(response.getMessages())
			if (response.getNextPageToken() != null) {
				String pageToken = response.getNextPageToken()
				response = service.users().messages().list(userId).setLabelIds(labelIds).setPageToken(pageToken).execute()
			} else {
				break;
			}
		}

		for (Message message : messages) {
			//		System.out.println(message.toPrettyString());
		}

		return messages;
	}


}
