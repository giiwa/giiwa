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

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.KeyField;
import org.giiwa.core.bean.X;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
/**
 * web api: /admin/keyfield
 * <br>
 * used to manage the key field of each collection, that may need to create
 * index for them
 * 
 * @author joe
 *
 */
public class keyfield extends Model {

    /* (non-Javadoc)
     * @see org.giiwa.framework.web.Model#onGet()
     */
    @Path(login = true, access = "access.config.admin")
    public void onGet() {

        int s = this.getInt("s");
        int n = this.getInt("n", 10, "number.per.page");
        BasicDBObject q = new BasicDBObject();

        if (X.isEmpty(this.path)) {
            String collection = this.getString("collection");
            if (!X.isEmpty(collection)) {
                q.append("collection", collection);
            }
            String status = this.getString("status");
            if (!X.isEmpty(status)) {
                q.append("status", status);
            } else {
                q.append("status", new BasicDBObject("$ne", "done"));
            }

            this.set(this.getJSON());
        } else {
            q.append("status", new BasicDBObject("$ne", "done"));
        }

        Beans<KeyField> bs = KeyField.load(q, new BasicDBObject("collection", 1).append("q", 1), s, n);
        this.set(bs, s, n);

        this.query.path("/admin/keyfield");
        this.show("/admin/keyfield.index.html");
    }

    /**
     * Run.
     */
    @Path(path = "run", login = true, access = "access.config.admin")
    public void run() {
        String id = this.getString("id");
        KeyField k = KeyField.load(id);
        if (k != null) {
            k.run();
            this.set(X.MESSAGE, "索引成功！");
        }

        onGet();
    }

    /**
     * Deleteall.
     */
    @Path(path = "deleteall", login = true, access = "access.config.admin")
    public void deleteall() {
        KeyField.deleteAll();
        onGet();
    }

    /**
     * Delete.
     */
    @Path(path = "delete", login = true, access = "access.config.admin")
    public void delete() {
        String id = this.getString("id");
        KeyField.delete(id);
        onGet();
    }

}
