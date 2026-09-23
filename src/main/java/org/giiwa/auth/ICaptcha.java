package org.giiwa.auth;

import java.util.HashMap;
import java.util.Map;

import org.giiwa.bean.User;

/**
 * 多种方式认证
 * 
 * @author joe
 *
 */
public interface ICaptcha {

	static Map<String, ICaptcha> _handler = new HashMap<String, ICaptcha>();

	/**
	 * 注册一种认证方式
	 * 
	 * @param name
	 * @param hander
	 */
	public static void register(String name, ICaptcha hander) {
		_handler.put(name, hander);
	}

	/**
	 * 获取认证方式
	 * 
	 * @param option
	 * @return
	 */
	public static ICaptcha get(String option) {
		return _handler.get(option);
	}

	/**
	 * 初始化一个用户的认证方式
	 * 
	 * @param user
	 * @return
	 */
	public boolean init(User user);

	/**
	 * 验证用户鉴权
	 * 
	 * @param user
	 * @param code
	 * @return
	 */
	public boolean verify(User user, String code);

}
