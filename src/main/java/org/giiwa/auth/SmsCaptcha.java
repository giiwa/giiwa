package org.giiwa.auth;

import org.giiwa.bean.User;

/**
 * 短信发送验证码
 * 
 * @author joe
 *
 */
public class SmsCaptcha implements ICaptcha {

	public static void init() {
		ICaptcha.register("sms", new SmsCaptcha());
	}

	@Override
	public boolean init(User user) {
		// TODO send email
		return false;
	}

	@Override
	public boolean verify(User user, String code) {
		return true;
	}

}
