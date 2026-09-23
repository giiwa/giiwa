package org.giiwa.auth;

import org.giiwa.bean.User;

/**
 * 2fa 发送验证码
 * 
 * @author joe
 *
 */
public class TfaCaptcha implements ICaptcha {

	public static void init() {
		ICaptcha.register("2fa", new TfaCaptcha());
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
