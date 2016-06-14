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
package org.giiwa.core.mq;

import net.sf.json.JSONObject;

// TODO: Auto-generated Javadoc
/**
 * The {@code IStub} Interface is used to handle the request message come in or
 * a response come in in Distributed System
 * 
 * @author joe
 *
 */
public interface IStub {

    /**
     * a request come in.
     *
     * @param seq
     *          the seq
     * @param to
     *          the to
     * @param from
     *          the from
     * @param src
     *          the src
     * @param header
     *          the header
     * @param msg
     *          the msg
     * @param attachment
     *          the attachment
     */
    public void onRequest(long seq, String to, String from, String src, JSONObject header, JSONObject msg, byte[] attachment);

    /**
     * a response come in.
     *
     * @param seq
     *          the seq
     * @param to
     *          the to
     * @param from
     *          the from
     * @param src
     *          the src
     * @param header
     *          the header
     * @param msg
     *          the msg
     * @param attachment
     *          the attachment
     */
    public void onResponse(long seq, String to, String from, String src, JSONObject header, JSONObject msg, byte[] attachment);

}
