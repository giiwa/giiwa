/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.core.noti;

import java.io.*;
import java.util.Properties;

import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.GLog;

/**
 * send the email
 * 
 * @author wujun
 *
 */
public class Email {

	static Log log = LogFactory.getLog(Email.class);

	private static class auth extends Authenticator {
		public PasswordAuthentication getPasswordAuthentication() {
			String user = Global.getString("mail.user", "service@giisoo.com");
			String passwd = Global.getString("mail.passwd", "123456");
			if (log.isDebugEnabled())
				log.debug("user=" + user + ", passwd=" + passwd);
			return new PasswordAuthentication(user, passwd);
		}
	}

	/**
	 * Send.
	 *
	 * @param subject
	 *            the subject
	 * @param body
	 *            the body
	 * @param to
	 *            the to
	 * @param attachments
	 *            the attachments
	 * @param names
	 *            the names
	 * @param contents
	 *            the contents
	 * @return true, if successful
	 */
	public static boolean send(String subject, String body, String to, InputStream[] attachments, String[] names,
			String[] contents) {

		if (X.isEmpty(to))
			return false;

		Properties props = new Properties();

		props.setProperty("mail.transport.protocol", Global.getString("mail.protocol", "smtp").toLowerCase());
		props.setProperty("mail.host", Global.getString("mail.host", "smtp.exmail.qq.com"));
		props.setProperty("mail.smtp.auth", "true");
		props.setProperty("mail.smtps.auth", "true");

		try {

			Session mailSession = Session.getDefaultInstance(props, new auth());
			Transport transport = mailSession.getTransport();

			MimeMessage message = new MimeMessage(mailSession);
			message.setSubject(subject, "utf-8");

			BodyPart messageBodyPart = new MimeBodyPart();
			body = body.replaceAll("\r", "<br/>").replaceAll(" ", "&nbsp;");
			messageBodyPart.setContent(body, "text/html; charset=utf-8");
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			if (attachments != null) {

				for (int i = 0; i < attachments.length; i++) {
					InputStream in = attachments[i];
					BodyPart attachmentPart = new MimeBodyPart();
					DataSource source = new ByteArrayDataSource(in, contents[i]);
					attachmentPart.setDataHandler(new DataHandler(source));
					attachmentPart.setFileName(names[i]);
					multipart.addBodyPart(attachmentPart);
				}
			}

			message.setContent(multipart);
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			InternetAddress f = new InternetAddress(Global.getString("mail.email", "service@giiwa.com"));
			f.setPersonal(Global.getString("mail.email", X.EMPTY));
			message.setFrom(f);

			transport.connect();
			transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
			transport.close();

			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);

			GLog.applog.error("email", "send", e.getMessage(), e, null, null);
		}

		return false;
	}

	/**
	 * Send.
	 *
	 * @param subject
	 *            the subject
	 * @param body
	 *            the body
	 * @param to
	 *            the to
	 * @return true, if successful
	 */
	public static boolean send(String subject, String body, String to) {
		return send(subject, body, to, null, null, null);
	}

	/**
	 * Send.
	 *
	 * @param subject
	 *            the subject
	 * @param body
	 *            the body
	 * @param to
	 *            the to
	 * @param from
	 *            the from
	 * @param displayname
	 *            the displayname
	 * @return true, if successful
	 */
	public static boolean send(String subject, String body, String to, String from, String displayname) {
		if (X.isEmpty(to))
			return false;

		Properties props = new Properties();

		props.setProperty("mail.transport.protocol", Global.getString("mail.protocol", "smtp"));
		props.setProperty("mail.host", Global.getString("mail.host", "smtp.exmail.qq.com"));
		props.setProperty("mail.smtp.auth", "true");
		props.setProperty("mail.smtps.auth", "true");

		try {

			Session mailSession = Session.getDefaultInstance(props, new auth());
			Transport transport = mailSession.getTransport();

			MimeMessage message = new MimeMessage(mailSession);
			message.setSubject(subject);
			message.setContent(body, "text/html; charset=UTF-8");
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			InternetAddress f = new InternetAddress(from);
			f.setPersonal(displayname);
			message.setFrom(f);

			transport.connect();
			transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
			transport.close();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return false;
	}

}
