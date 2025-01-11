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
package org.giiwa.net.client;

import java.net.InetAddress;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;
import org.giiwa.misc.Url;

public class NTP {

	private static Log log = LogFactory.getLog(NTP.class);

	/**
	 * 
	 * @param url ntp://g01:123
	 * 
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
