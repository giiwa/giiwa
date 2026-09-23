package org.giiwa.auth;

import org.giiwa.bean.User;

/**
 * 邮件发送验证码认证方式
 * 
 * @author joe
 *
 */
public class EmailCaptcha implements ICaptcha {

	public static void init() {
		ICaptcha.register("email", new EmailCaptcha());
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
