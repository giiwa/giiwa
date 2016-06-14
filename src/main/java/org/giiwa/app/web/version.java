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
package org.giiwa.app.web;

import net.sf.json.JSONObject;

import org.giiwa.core.bean.X;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Module;
import org.giiwa.framework.web.Path;

public class version extends Model {

    /* (non-Javadoc)
     * @see org.giiwa.framework.web.Model#onGet()
     */
    @Path()
    @Override
    public void onGet() {
        JSONObject jo = new JSONObject();
        jo.put(X.STATE, 200);
        jo.put("version", Module.load("default").getVersion());
        jo.put("build", Module.load("default").getBuild());
        this.response(jo);
    }

}
