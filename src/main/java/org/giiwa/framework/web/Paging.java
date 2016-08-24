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
package org.giiwa.framework.web;

import java.util.*;

// TODO: Auto-generated Javadoc
/**
 * 
 * @author yjiang
 * 
 */
public class Paging {

  /**
   * Pages.
   * 
   * @param url
   *          the url
   * @param total
   *          the total
   * @param curr
   *          the curr
   * @param numofPage
   *          the numof page
   * @return the array list
   */
  public static ArrayList<PageLink> pages(QueryString url, int total, int curr, int numofPage) {
    if (numofPage <= 0)
      return null;

    url = url.copy();

    if (curr < 1)
      curr = 1;

    ArrayList<PageLink> list = new ArrayList<PageLink>();

    list.add(new PageLink(curr, Integer.toString(curr)));

    if (curr > 1) {
      list.add(new PageLink(-2, "&lt;", url.set("s", ((curr - 2) * numofPage)).toString()));
    }

    int pages = (total / numofPage);
    if (total % numofPage > 0)
      pages++;

    if (curr < pages) {
      list.add(new PageLink(pages + 10, "&gt;", url.set("s", curr * numofPage).toString()));
    }

    int start = Math.max(1, curr - 5);
    int end = Math.min(pages, start + 10);
    if (end - start < 11) {
      start = Math.max(1, end - 10);
    }

    for (int i = start; i <= end; i++) {
      if (i == curr)
        continue;
      list.add(new PageLink(i, Integer.toString(i), url.set("s", (i - 1) * numofPage).toString()));
    }

    // if (end < pages - 1) {
    // list.add(new PageLink(pages, ">>[" + pages + "]", url.set("s", (pages
    // - 1) * numofPage).toString()));
    // }
    //
    // if (start > 1) {
    // list.add(new PageLink(1, "<<[1]", url.set("s", 0).toString()));
    // }

    // int delta = 1;
    // int m = Math.min(5, curr);
    // for (int i = curr + 1; i < total / numofPage + 2; i += delta) {
    //
    // list.add(new PageLink(i, Integer.toString(i), url.set("s", (i - 1) *
    // numofPage).toString()));
    // m++;
    // if (m > 9) {
    // m = 0;
    // delta *= 10;
    //
    // if (i < (total / numofPage)) {
    // list.add(new PageLink(total / numofPage + 1, ">>", url.set("s", total
    // - numofPage).toString()));
    // break;
    // }
    // }
    // }
    //
    // delta = 1;
    // if (list.size() < 6) {
    // m = list.size() - 1;
    // } else {
    // m = 5;
    // }
    //
    // for (int i = curr - 1; i > 0; i -= delta) {
    // list.add(new PageLink(i, Integer.toString(i), url.set("s", (i - 1) *
    // numofPage).toString()));
    // m++;
    // if (m > 9) {
    // m = 0;
    // delta *= 10;
    //
    // if (i > 1) {
    // list.add(new PageLink(1, "<<", url.set("s", 0).toString()));
    // break;
    // }
    // }
    // }

    Collections.sort(list);

    return list;
  }

  /**
   * Paging.
   * 
   * @param url
   *          the url
   * @param total
   *          the total
   * @param curr
   *          the curr
   * @param numofPage
   *          the numof page
   * @return the array list
   */
  public static ArrayList<PageLink> paging(QueryString url, int total, int curr, int numofPage) {

    if (numofPage <= 0)
      return null;

    url = url.copy();
    ArrayList<PageLink> list = new ArrayList<PageLink>();

    list.add(new PageLink(curr, Integer.toString(curr)));
    if (curr > 1) {
      list.add(new PageLink(-2, "&lt;", url.set("s", ((curr - 2) * numofPage)).toString()));
    }
    if (curr < (total / numofPage + 1)) {
      list.add(new PageLink(-1, "&gt;", url.set("s", curr * numofPage).toString()));
    }

    int delta = 1;
    int m = Math.min(5, curr);
    for (int i = curr + 1; i < total / numofPage + 1; i += delta) {

      list.add(new PageLink(i, Integer.toString(i), url.set("s", (i - 1) * numofPage).toString()));
      m++;
      if (m > 9) {
        m = 0;
        delta *= 10;

        if (i < (total / numofPage)) {
          list.add(new PageLink(i + 1, "..."));
        }
      }
    }

    delta = 1;
    if (list.size() < 6) {
      m = list.size() - 1;
    } else {
      m = 5;
    }

    for (int i = curr - 1; i > 0; i -= delta) {
      list.add(new PageLink(i, Integer.toString(i), url.set("s", (i - 1) * numofPage).toString()));
      m++;
      if (m > 9) {
        m = 0;
        delta *= 10;

        if (i > 2) {
          list.add(new PageLink(i - 1, "..."));
        }
      }
    }

    // add first page in list
    PageLink p = new PageLink(1, "1", url.set("s", 0).toString());
    if (!list.contains(p)) {
      list.add(p);
    }

    // add last page
    int n = total / numofPage;
    p = new PageLink(n + 1, Integer.toString(n + 1), url.set("s", n * numofPage).toString());
    if (!list.contains(p)) {
      list.add(p);
    }

    Collections.sort(list);

    return list;
  }

  /**
   * Creates the.
   * 
   * @param total
   *          the total
   * @param s
   *          the s
   * @param ITEM_NUMBER
   *          the item number
   * @return the list
   */
  public static List<PageLabel> create(int total, int s, int ITEM_NUMBER) {
    return create(total, s, ITEM_NUMBER, 2);
  }

  /**
   * Creates the.
   * 
   * @param total
   *          the total
   * @param s
   *          the s
   * @param ITEM_NUMBER
   *          the item number
   * @param pages
   *          the pages
   * @return the list
   */
  public static List<PageLabel> create(int total, int s, int ITEM_NUMBER, int pages) {

    if (ITEM_NUMBER <= 0)
      return null;

    if (s < 0) {
      s = 0;
    }

    // if (total <= ITEM_NUMBER) {
    // return null;
    // }

    List<PageLabel> list = new ArrayList<PageLabel>(pages * 2 + 4);
    int prev = s - ITEM_NUMBER;
    for (int i = 0; i < pages && prev >= 0; i++) {
      list.add(new PageLabel(Integer.toString(prev / ITEM_NUMBER + 1), prev, ITEM_NUMBER, prev));
      prev -= ITEM_NUMBER;
    }

    int next = s + ITEM_NUMBER;
    if (total > 0) {
      while (list.size() < 2 * pages && next < total) {
        list.add(new PageLabel(Integer.toString(next / ITEM_NUMBER + 1), next, ITEM_NUMBER, next));
        next += ITEM_NUMBER;
      }
    }

    while (list.size() < 2 * pages && prev >= 0) {
      list.add(new PageLabel(Integer.toString(prev / ITEM_NUMBER + 1), prev, ITEM_NUMBER, prev));
      prev -= ITEM_NUMBER;
    }

    if (s > 0) {
      list.add(new PageLabel("&lt;", s - ITEM_NUMBER, ITEM_NUMBER, Integer.MIN_VALUE + 1));
    }

    if (prev >= 0) {
      list.add(new PageLabel("&lt;&lt;", 0, ITEM_NUMBER, Integer.MIN_VALUE));
    }

    list.add(new PageLabel(Integer.toString(s / ITEM_NUMBER + 1), s, ITEM_NUMBER, s, true));

    if (total < 0 || s < total - ITEM_NUMBER) {
      list.add(new PageLabel("&gt;", s + ITEM_NUMBER, ITEM_NUMBER, Integer.MAX_VALUE - 1));

      if (next < total) {
        list.add(new PageLabel("&gt;&gt;", total / ITEM_NUMBER * ITEM_NUMBER, ITEM_NUMBER, Integer.MAX_VALUE));
      }
    }

    Collections.sort(list);

    return list;
  }

  public static void main(String[] args) {
    System.out.println(create(10, 0, 5, 2));
    System.out.println(create(10, 4, 5, 2));
    System.out.println(create(-1, 0, 5, 2));
    System.out.println(create(-1, 20, 5, 2));

  }
}
