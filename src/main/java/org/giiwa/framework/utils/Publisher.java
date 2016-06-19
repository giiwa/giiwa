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
package org.giiwa.framework.utils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.Base64;
import org.giiwa.core.base.DES;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.DBMapping;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.utils.Http;
import org.giiwa.framework.utils.Http.Response;

// TODO: Auto-generated Javadoc
/**
 * the {@code Publisher} Class lets publish the data to remote by "manual"
 * 
 * @author joe
 *
 */
public class Publisher {

    static Log log = LogFactory.getLog(Publisher.class);

    /**
     * Publish.
     *
     * @param b
     *          the b
     * @return the int
     * @throws Exception
     *           the exception
     */
    public static int publish(Bean b) throws Exception {

        String url = Global.s("sync.url", null);
        String appkey = Global.s("sync.appkey", null);
        String secret = Global.s("sync.secret", null);
        if (!X.isEmpty(url) && !X.isEmpty(appkey) && !X.isEmpty(secret)) {

            // b.toJSON(jo);

            JSONObject jo = new JSONObject();
            b.toJSON(jo);

            /**
             * get the require annotation onGet
             */
            DBMapping mapping = (DBMapping) b.getClass().getAnnotation(DBMapping.class);
            if (mapping == null) {
                String collection = b.getString("collection");
                if (X.isEmpty(collection)) {
                    log.error("mapping missed in [" + b.getClass() + "] declaretion", new Exception("nothing but log"));
                    return 0;
                }
            } else {
                if (!X.isEmpty(mapping.collection())) {
                    jo.put("collection", mapping.collection());
                } else {
                    jo.put("table", mapping.table());
                }
            }

            return publish(jo);

        }

        return 0;
    }

    /**
     * Publish.
     *
     * @param jo
     *          the jo
     * @return the int
     * @throws Exception
     *           the exception
     */
    public static int publish(JSONObject jo) throws Exception {
        String url = Global.s("sync.url", null);
        String appkey = Global.s("sync.appkey", null);
        String secret = Global.s("sync.secret", null);
        if (!X.isEmpty(url) && !X.isEmpty(appkey) && !X.isEmpty(secret)) {

            // JSONObject j1 = new JSONObject();
            // for (Object name : jo.keySet()) {
            // Object o = jo.get(name);
            // j1.put(name, o.getClass().getName());
            // }
            //
            // jo.put("definition", j1);
            JSONArray arr = new JSONArray();
            arr.add(jo);

            JSONObject req = new JSONObject();
            req.put("list", arr);
            req.put("_time", System.currentTimeMillis());

            // try {
            String data = Base64.encode(DES.encode(req.toString().getBytes(), secret.getBytes()));

            Response r = Http.post(url, null, new String[][] {{ "m", "set" } }, new String[][] { { "appkey", appkey }, { "data", data } });

            log.debug("synced: resp=" + r.body + ", request=" + jo);
            JSONObject j = JSONObject.fromObject(r.body);
            if (j.getInt("state") == 200 && j.getInt("updated") > 0) {
                return 1;
            }
            // } catch (Exception e) {
            // log.error(e.getMessage(), e);
            // OpLog.warn("sync", e.getMessage(), e.getMessage());
            // }

        }

        return 0;

    }
}
