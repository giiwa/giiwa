/*
 *   giiwa, a java web foramewrok.
 *   Copyright (C) <2014>  <giiwa.org>
 *
 */
package org.giiwa.app.web.admin;

import org.apache.commons.configuration.Configuration;
import org.giiwa.core.conf.Local;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/dashboard
 * <br>
 * used to show dashboard
 * 
 * @author yjiang
 * 
 */
public class dashboard extends Model {

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.framework.web.Model#onGet()
     */
    @Override
    @Path(login = true, access = "access.config.admin")
    public void onGet() {

        Configuration conf = Local.getConfig();

        this.set("me", this.getUser());
        this.set("uptime", lang.format(Model.UPTIME, "yy-MM-dd"));
        this.set("past", lang.past(Model.UPTIME));
        this.set("node", conf.getString("node", ""));
        this.set("release", Module.load("default").getVersion());
        this.set("build", Module.load("default").getBuild());

        show("admin/dashboard.html");
    }

}
