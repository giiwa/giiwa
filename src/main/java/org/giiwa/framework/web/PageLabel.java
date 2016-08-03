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
package org.giiwa.framework.web;

// TODO: Auto-generated Javadoc
/**
 * 
 * @author yjiang
 * 
 */
public class PageLabel implements Comparable<PageLabel> {
  String  label;
  int     pn;
  int     s;
  int     n;
  boolean curr;
  int     seq;

  public String toString() {
    return label;
  }

  public int getPn() {
    return pn;
  }

  public boolean getCurr() {
    return curr;
  }

  public String getLabel() {
    return label;
  }

  public int getS() {
    return s;
  }

  public int getN() {
    return n;
  }

  /**
   * Sets the pn.
   * 
   * @param pn
   *          the pn
   * @return the page label
   */
  public PageLabel setPn(int pn) {
    this.pn = pn;
    return this;
  }

  /**
   * Instantiates a new page label.
   * 
   * @param label
   *          the label
   * @param s
   *          the s
   * @param n
   *          the n
   * @param seq
   *          the seq
   * @param iscur
   *          the iscur
   */
  public PageLabel(String label, int s, int n, int seq, boolean iscur) {
    this.label = label;
    this.s = s;
    this.n = n;
    this.curr = iscur;
    this.seq = seq;
  }

  /**
   * Instantiates a new page label.
   * 
   * @param label
   *          the label
   * @param s
   *          the s
   * @param n
   *          the n
   * @param seq
   *          the seq
   */
  public PageLabel(String label, int s, int n, int seq) {
    this.label = label;
    this.s = s;
    this.n = n;
    this.seq = seq;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(PageLabel o) {
    if (seq < o.seq) {
      return -1;
    } else {
      return 1;
    }
  }
}