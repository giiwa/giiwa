package org.giiwa.core.net;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;
import org.giiwa.core.base.Url;
import org.giiwa.framework.bean.GLog;

public class FTP {

	private static Log log = LogFactory.getLog(FTP.class);

	/**
	 * 
	 * @param url ftp://g01:21?username=，passwd=
	 * 
	 * @param url the command
	 * @return the FTPClient
	 */
	public static FTPClient login(Url url) {

		FTPClient ftp = new FTPClient();
		FTPClientConfig config = new FTPClientConfig();
		// config.setXXX(YYY); // change required options
		// for example config.setServerTimeZoneId("Pacific/Pitcairn")
		ftp.configure(config);
		try {
			int reply;

			ftp.connect(url.getIp(), url.getPort(21));
			log.debug("replaystring=" + ftp.getReplyString());

			// After connection attempt, you should check the reply code to verify
			// success.
			reply = ftp.getReplyCode();

			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				return null;
			}

			if (ftp.login(url.get("username"), url.get("password"))) {
				return ftp;
			} else {
				GLog.applog.error("backup", "auto", "login failed", null, null);
			}
		} catch (IOException e) {
			log.error(url, e);
		}
		return null;
	}

}
