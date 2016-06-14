/*
 * Copyright 2015 Giiwa, Inc. and/or its affiliates.
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
package org.giiwa.app.web.admin;

import net.sf.json.JSONObject;

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.framework.bean.Repo;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
public class repo extends Model {

    /* (non-Javadoc)
     * @see org.giiwa.framework.web.Model#onGet()
     */
    @Path(login = true, access = "access.config.admin")
    @Override
    public void onGet() {

        int s = this.getInt("s");
        int n = this.getInt("n", 10, "number.per.page");

        BasicDBObject q = new BasicDBObject();
        Beans<Repo.Entity> bs = Repo.load(q, new BasicDBObject(), s, n);
        this.set(bs, s, n);
        this.show("/admin/repo.index.html");

    }

    /**
     * Delete.
     */
    @Path(path = "delete", login = true, access = "access.config.admin")
    public void delete() {
        JSONObject jo = new JSONObject();

        String id = this.getString("id");
        Repo.delete(id);

        jo.put(X.STATE, 200);
        this.response(jo);
    }

}
