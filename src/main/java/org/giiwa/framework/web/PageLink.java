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

/**
 * 
 * @author yjiang
 * 
 */
public class PageLink implements Comparable<PageLink> {
  int page;
  String label;
  String link;

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return obj instanceof PageLink && page == ((PageLink) obj).page;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return page;
  }

  /**
	 * Instantiates a new page link.
	 * 
	 * @param page
	 *            the page
	 * @param label
	 *            the label
	 */
  public PageLink(int page, String label) {
    this(page, label, null);
  }

  /**
	 * Instantiates a new page link.
	 * 
	 * @param page
	 *            the page
	 * @param label
	 *            the label
	 * @param url
	 *            the url
	 */
  public PageLink(int page, String label, String url) {
    this.page = page;
    if (url != null) {
      StringBuilder sb = new StringBuilder("<a ").append("id='page_");

      if ("<".equals(label)) {
        sb.append("prev");
      } else if (">".equals(label)) {
        sb.append("next");
      } else {
        sb.append(label);
      }
      sb.append("'").append(" href='").append(url).append("'>").append(label).append("</a>").toString();

      this.link = sb.toString();
    } else {
      this.label = label;
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    if (link == null) {
      return "<font color='red'> " + label + " </font>";
    } else {
      return link;
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(PageLink another) {
    return page - another.page;
  }

}
