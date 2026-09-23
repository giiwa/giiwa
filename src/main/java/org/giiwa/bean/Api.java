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
package org.giiwa.bean;

import org.apache.commons.logging.Log;

import org.apache.commons.logging.LogFactory;
import org.giiwa.crypto.SM3;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;

/**
 * table="gi_api"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_api", memo = "GI-Restful 接口")
public final class Api extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Api.class);

	public static final BeanDAO<String, Api> dao = BeanDAO.create(Api.class);

	@Column(memo = "主键", unique = true, size = 64)
	public String id;

	@Column(memo = "请求链接", size = 255)
	public String uri;

	@Column(memo = "辅助开启")
	public int enabled;

//	@Column(memo = "名称1", size = 64)
//	private String s1;
//
//	@Column(memo = "名称2", size = 64)
//	private String s2;

	@Column(memo = "模块", size = 64)
	public String module;

//	@Column(memo = "模型", size = 128)
//	private String model;

	@Column(memo = "方法", size = 10)
	public String method;

	@Column(memo = "是否登录")
	public int login;

	@Column(memo = "日志级别", size = 10)
	private String loglevel;

	@Column(memo = "权限令牌", size = 50)
	public String access;

	@Column(memo = "输入参数", size = 2048)
	public String in;

	@Column(memo = "输出", size = 2048)
	public String out;

	@Column(memo = "备注", size = 2048)
	public String memo;

	public static String update(String uri, V v) {
		try {
			String id = SM3.hash(uri);

			Api e = dao.load(id);
			if (e != null) {
				e.clean(v);
				if (!v.isEmpty()) {
					v.force("_indexed", 0);
					dao.update(id, v);
				}
			} else {
				v.force("_indexed", 0);
				v.append(X.ID, id).append("uri", uri);// .append("s1", s1).append("s2", s2);
				dao.insert(v);
			}
			return id;
		} catch (Exception err) {
			log.error(err.getMessage(), err);
		}
		return null;
	}

}
