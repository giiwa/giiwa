package org.giiwa.app.web.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.AuthToken;
import org.giiwa.framework.bean.GLog;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

public class profile extends Model {

	private static List<String> names = new ArrayList<String>();
	private static Map<String, Class<? extends profile>> settings = new HashMap<String, Class<? extends profile>>();

	final public static void register(int seq, String name, Class<? extends profile> m) {
		if (seq < 0 || seq >= names.size()) {
			names.add(name);
		} else {
			names.add(seq, name);
		}
		settings.put(name, m);
	}

	final public static void register(String name, Class<? extends profile> m) {
		register(-1, name, m);
	}

	@Path(path = "get/(.*)", login = true)
	final public Object get(String name) {
		Class<? extends profile> c = settings.get(name);
		log.debug("/get/" + c);
		if (c != null) {
			try {
				profile s = c.newInstance();
				s.copy(this);
				s.get();

				s.set("lang", lang);
				s.set("module", module);
				s.set("name", name);
				s.set("settings", names);
				s.show("/admin/profile.html");

			} catch (Exception e) {
				log.error(name, e);
				GLog.oplog.error(setting.class, "get", e.getMessage(), e, login, this.getRemoteHost());

				this.show("/admin/profile.html");
			}
		}

		return null;
	}

	/**
	 * Sets the.
	 *
	 * @param name
	 *            the name
	 */
	@Path(path = "set/(.*)", login = true, log = Model.METHOD_POST)
	final public void set(String name) {
		Class<? extends profile> c = settings.get(name);
		log.debug("/set/" + c);
		if (c != null) {
			try {
				profile s = c.newInstance();
				s.copy(this);
				s.set();

				s.set("lang", lang);
				s.set("module", module);
				s.set("name", name);
				s.set("settings", names);
				s.show("/admin/profile.html");
			} catch (Exception e) {
				log.error(name, e);
				GLog.oplog.error(setting.class, "set", e.getMessage(), e, login, this.getRemoteHost());

				this.show("/admin/profile.html");
			}
		}
	}

	/**
	 * invoked when post setting form.
	 */
	public void set() {

	}

	public void settingPage(String view) {
		this.set("page", view);
	}

	/**
	 * invoked when get the setting form.
	 */
	public void get() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	@Path(login = true)
	public final void onGet() {

		if (!names.isEmpty()) {
			String name = names.get(0);
			this.set("name", name);
			get(name);
			return;
		}

		this.println("not find page");

	}

	public static class my extends profile {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting#set()
		 */
		@Override
		public void set() {

			String password = this.getString("password");
			if (!X.isEmpty(password)) {
				login.update(V.create("password", password));

				this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
				return;
			} else {
				login.update(V.create().copy(this, "nickname", "title", "email", "phone"));

				login = User.dao.load(login.getId());
				AuthToken.delete(login.getId());
				this.setUser(login);

				this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("save.success")));
				return;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting#get()
		 */
		@Override
		public void get() {
			this.settingPage("/admin/profile.my.html");
		}

	}

}
