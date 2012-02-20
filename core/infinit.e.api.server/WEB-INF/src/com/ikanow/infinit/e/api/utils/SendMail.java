package com.ikanow.infinit.e.api.utils;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.log4j.Logger;


/**
 * Class used to send mail notifications to recipients
 * 
 * @author cmorgan
 *
 */
public class SendMail {
	
	private static final Logger logger = Logger.getLogger(SendMail.class);
	
	private String from;
	private String to;
	private String subject;
	private String text;
	
	/**
	 * Class constructor 
	 * 
	 * @param from
	 * @param to
	 * @param subject
	 * @param text
	 */
	public SendMail(String from, String to, String subject, String text)
	{
		this.from = from;
		this.to = to;
		this.subject = subject;
		this.text = text;
	}
	
	public boolean send()
	{
		return send("text/plain");
	}
	
	/**
	 * Sends mail messages 
	 * @return
	 */
	public boolean send(String mimeType)
	{
		boolean sendMail = true;
		
		PropertiesManager propManager = new PropertiesManager();
		TextEncryption textEncryptor = new TextEncryption();
		
		String host = propManager.getProperty("mail.server");
		String username = textEncryptor.decrypt(propManager.getProperty("mail.username"));
		String password = textEncryptor.decrypt(propManager.getProperty("mail.password"));
	
		Properties props = new Properties();
		props.put("mail.smtps.auth","true");
		Session session = Session.getDefaultInstance(props);
		
		MimeMessage msg = new MimeMessage(session);
		Transport t = null;
		
		try 
		{
			t = session.getTransport("smtps");
			
			InternetAddress fromAddress = null;
			// CV: 7/6/11 - Changed to array to support multiple recipients
			InternetAddress[] toAddress = null;
			
			try 
			{
				fromAddress = new InternetAddress(from);

				// CV: 7/6/11 - Split on ; to get multiple to addresses
				if (to.split(";").length > 0)
				{
					String[] toAddresses = to.split(";");
					toAddress = new InternetAddress[toAddresses.length];
					for (int i = 0; i < toAddresses.length; i++)
					{
						toAddress[i] = new InternetAddress(toAddresses[i]);
					}
				}
				else
				{
					toAddress = new InternetAddress[1];
					toAddress[0] = new InternetAddress(to);
				}
			}
			catch (AddressException e) 
			{
				sendMail = false;
				// If an exception occurs log the error
				logger.error("Address Exception Message: " + e.getMessage(), e);
			}
			
			try 
			{
				msg.setFrom(fromAddress);
				msg.setRecipients(RecipientType.TO, toAddress);
				msg.setSubject(subject);
				msg.setContent(text, mimeType);
				
				t.connect(host, username, password);
				t.sendMessage(msg, msg.getAllRecipients());
				
			}
			catch (MessagingException e) 
			{
				sendMail = false;
				// If an exception occurs log the error
				logger.error("Messaging Exception Message: " + e.getMessage(), e);
				
			}
			finally 
			{
				try
				{
					t.close();
				}
				catch (MessagingException e) 
				{
					logger.error("Messaging Exception Message: " + e.getMessage(), e);
				}
			}
			
		} 
		catch (NoSuchProviderException e) 
		{
			sendMail = false;
			logger.error("Provider Exception Message: " + e.getMessage(), e);
		}
		
		return sendMail;
	}

	
	/**
	 * Test method
	 * @param args
	 */
	public static void main(String[] args) {
		
		String from = "system@ikanow.com";
		String to = "cvitter@ikanow.com";
		String subject = "Test";
		String message = "A test message";
		
		SendMail sendMail = new SendMail(from, to, subject, message);
		System.out.println("Was mail sent? " + sendMail.send());
	}

}
