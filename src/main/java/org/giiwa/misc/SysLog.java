package org.giiwa.misc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.web.Language;
import org.graylog2.syslog4j.Syslog;
import org.graylog2.syslog4j.SyslogConstants;
import org.graylog2.syslog4j.SyslogIF;
import org.graylog2.syslog4j.impl.net.udp.UDPNetSyslogConfig;

public class SysLog {

	private static Log log = LogFactory.getLog(SysLog.class);

	private static SyslogIF syslog = null;

	private static Language lang = Language.getLanguage();

	private static String ip = null;

	private static int Facility = 3; // daemon

	private static long pid = -1;

	/**
	 * 发送syslog日志
	 * 
	 * @param level   - 0:紧急， 1:警报，2:严重，3:错误，4:警告， 5:通知， 6:信息， 7:调试
	 * @param time    - 时间
	 * @param tag     - 模块
	 * @param content - 内容
	 */
	public static void log(int level, String tag, Object content) {

		try {
			if (Global.getInt("glog.rsyslog", 0) == 1) {
				// enabled rsyslog

				if (syslog == null) {
					syslog = Syslog.getInstance(SyslogConstants.UDP);
					UDPNetSyslogConfig config = (UDPNetSyslogConfig) syslog.getConfig();

					ip = Host.getLocalip();

					config.setHost(Global.getString("glog.rsyslog.host", "127.0.0.1"));
					config.setPort(X.toInt(Global.getLong("glog.rsyslog.port", 32376)));
//				config.setFacility(Facility);

					pid = Host.getPid();
				}

				// <PRI>TIMESTAMP HOST TAG: MESSAGE

				StringBuilder sb = new StringBuilder();

				int pri = Facility + level;

				long time = Global.now();

				sb.append("<" + pri + ">1"); // pri
				sb.append(" " + lang.format(time, "yyyy-MM-dd") + "T" + lang.format(time, "HH:mm:ss.S")); // timestamp
				sb.append(" " + ip); // ip
				sb.append(" " + tag); // appname
				sb.append(" " + pid); // pid
				sb.append(" - "); // messageid
				sb.append(" " + escapeNewLine(content));// message

				syslog.log(level, sb.toString());

			}
		} catch (Throwable err) {
			log.error(err.getMessage(), err);
		}

	}

	// 转义换行符，解决多行堆栈拆分问题
	private static String escapeNewLine(Object content) {
		if (content == null)
			return "";
		return content.toString().replaceAll("\r", "\\x0D").replaceAll("\n", "\\x0A");
	}

}
