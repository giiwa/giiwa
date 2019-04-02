package org.giiwa.core.net;

import java.net.InetAddress;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;
import org.giiwa.core.base.Url;

public class NTP {

	private static Log log = LogFactory.getLog(NTP.class);

	/**
	 * 
	 * @param url
	 *            ntp://g01:123
	 * 
	 * @param command
	 * @return
	 */
	public static void run(Url url) {
		try {
			NTPUDPClient timeClient = new NTPUDPClient();
			String timeServerUrl = url.getIp();
			InetAddress timeServerAddress = InetAddress.getByName(timeServerUrl);
			TimeInfo timeInfo = timeClient.getTime(timeServerAddress);
			TimeStamp timestamp = timeInfo.getMessage().getTransmitTimeStamp();

			String time = new SimpleDateFormat("hh:mm:ss").format(timestamp.getDate());

			String osName = System.getProperty("os.name");

			if (osName.matches("^(?i)Windows.*$")) {// Window 系统
				String date = new SimpleDateFormat("yyyy-MM-dd").format(timestamp.getDate());
				// 格式 HH:mm:ss
				String cmd = "  cmd /c time " + time;
				Runtime.getRuntime().exec(cmd);
				// 格式：yyyy-MM-dd
				cmd = " cmd /c date " + date;
				Runtime.getRuntime().exec(cmd);
			} else {
				// Linux 系统
				// 格式：yyyyMMdd
				String date = new SimpleDateFormat("yyyyMMdd").format(timestamp.getDate());
				String cmd = "  date -s " + date;
				Runtime.getRuntime().exec(cmd);
				// 格式 HH:mm:ss
				cmd = "  date -s " + time;
				Runtime.getRuntime().exec(cmd);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

}
