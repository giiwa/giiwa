/*
 *   giiwa, a java web foramewrok.
 *   Copyright (C) <2014>  <giiwa.org>
 *
 */
package org.giiwa.app.web.admin;

import org.giiwa.framework.bean.*;
import org.giiwa.framework.web.*;

/**
 * web api: <a href='/admin' target='_blank'>/admin</a> <br>
 * used to show home of admin
 * 
 * @author joe
 *
 */
public class index extends Model {

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onGet()
   */
  @Override
  @Path(login = true, method = Model.METHOD_GET)
  public void onGet() {
    /**
     * let's post method to handle it
     */
    onPost();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onPost()
   */
  @Path(login = true, method = Model.METHOD_POST)
  public void onPost() {

    User me = this.getUser();
    /**
     * put the user in mode
     */
    this.put("me", me);

    /**
     * show view ...
     */
    this.show("/admin/index.html");

  }

}
