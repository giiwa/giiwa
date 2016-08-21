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
package org.giiwa.core.noti;

import java.util.ArrayList;
import java.util.List;

import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.OpLog;

/**
 * @author wujun
 *
 */
public class Sms {

  static final List<ISender> senders = new ArrayList<ISender>();

  /**
   * register a sms sender.
   *
   * @param seq
   *          the lower first
   * @param sender
   *          the sender
   */
  public static void register(int seq, ISender sender) {

    if (sender != null && !senders.contains(sender)) {
      senders.add(seq, sender);
    }

  }

  /**
   * send the sms.
   *
   * @param mobile
   *          the mobile
   * @param template
   *          the internal template code
   * @param jo
   *          the jo
   * @return true: success <br>
   *         false: failed
   */
  public static boolean send(String mobile, String template, JSON jo) {
    jo.put("template", template);

    OpLog.info("sms", "send", jo.toString(), null, null);

    for (ISender s : senders) {
      if (s.send(mobile, jo)) {
        return true;
      }
    }
    return false;
  }

  public static interface ISender {

    /**
     * send sms.
     *
     * @param mobile
     *          the mobile number of receiver
     * @param jo
     *          key value data <br>
     *          sign: the sign of the sms <br>
     *          templatecode: the templatecode of the sms, e.g. registeruser,
     *          forgetpassword <br>
     * @return true: success <br>
     *         false: failed
     */
    public boolean send(String mobile, JSON jo);
  }
}
