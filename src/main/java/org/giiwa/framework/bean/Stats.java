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
package org.giiwa.framework.bean;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.X;
import org.giiwa.framework.bean.Stat.IConvertor;
import org.giiwa.framework.web.Language;

// TODO: Auto-generated Javadoc
public class Stats {

    static Log log = LogFactory.getLog(Stats.class);

    public static final int F0 = 0;
    public static final int F1 = 1;
    public static final int F2 = 2;
    public static final int F3 = 3;
    public static final int F4 = 4;

    private String type;

    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_NONE = "none";
    public static final String TYPE_HOUR = "hour";
    public static final String TYPE_DAY = "day";
    public static final String TYPE_WEEK = "week";
    public static final String TYPE_MONTH = "month";
    public static final String TYPE_YEAR = "year";

    public static final int STAT_TOTAL = 1;
    public static final int STAT_MAX = 2;

    // private static Log log = LogFactory.getLog(Stats.class);

    // private static Map<String, Map<String, String>> converts = new
    // HashMap<String, Map<String, String>>();

    private List<? extends Stat> list;

    private TreeMap<Object, List<Stat>> dates = new TreeMap<Object, List<Stat>>();
    private TreeMap<String, List<Stat>> names = new TreeMap<String, List<Stat>>();
    private List<Stat> summary = null;

    private IConvertor convertor;

    // public void setConvertor(IConvertor convertor) {
    // this.convertor = convertor;
    // if (list != null) {
    // for (Stat s : list) {
    // s.setConvertor(convertor);
    // }
    // }
    // }

    /**
     * Clear.
     */
    public void clear() {
        dates.clear();
        names.clear();
    }

    /**
     * Sets the.
     * 
     * @param date
     *            the date
     * @param list
     *            the list
     */
    public void set(String date, List<Stat> list) {
        if (Stats.TYPE_NUMBER.equals(type)) {
            dates.put(Bean.toInt(date), list);
        } else {
            dates.put(date, list);
        }
    }

    /**
     * summary all date's Stat
     * 
     * @return List
     */
    public List<Stat> getSummary() {
        if (summary == null) {
            summary = new ArrayList<Stat>();
            for (String name : names.keySet()) {
                List<Stat> list = names.get(name);
                Stat s = new Stat();
                s.set(name, 0);
                for (Stat s1 : list) {
                    s.add(s1.getCount());
                }
                summary.add(s);
            }

            Collections.sort(summary, comparator);

            while (summary.size() > top) {
                summary.remove(summary.size() - 1);
            }
        }
        return summary;
    }

    /**
     * Gets the.
     *
     * @param date
     *          the date
     * @return the string
     */
    // public static String get(String module, String parent) {
    // Map<String, String> m = converts.get(module);
    // if (m != null) {
    // return m.get(parent);
    // }
    // return null;
    // }

    /**
     * Sets the.
     * 
     * @param module
     *            the module
     * @param parent
     *            the parent
     * @param name
     *            the name
     */
    // public static void set(String module, String parent, String name) {
    // Map<String, String> m = converts.get(module);
    // if (m == null) {
    // m = new HashMap<String, String>();
    // converts.put(module, m);
    // }
    // m.put(parent, name);
    // }

    /**
     * Dates.
     * 
     * @param date
     *            the date
     * @return the list
     */
    public List<Stat> dates(Object date) {
        return dates.get(date);
    }

    private Stats(List<? extends Stat> list, IConvertor convertor) {
        this.list = list;
        this.convertor = convertor;
        if (list != null) {
            for (Stat s : list) {
                s.setConvertor(convertor);
            }
        }
    }

    /**
     * Display.
     * 
     * @param name
     *            the name
     * @return the string
     */
    public String display(String name) {
        List<Stat> list = names.get(name);
        if (list != null && list.size() > 0) {
            return list.get(0).toString();// .getDisplay();
        }
        return X.EMPTY;
    }

    /**
     * full empty for dates.
     *
     * @return Stats
     */
    public Stats fullZero() {
        if (dates != null && dates.size() > 0) {
            // private Map<String, List<Stat>> dates = new TreeMap<String,
            // List<Stat>>();
            // private Map<String, List<Stat>> names = new TreeMap<String,
            // List<Stat>>();
            String firstdate = (String) dates.firstKey();
            String lastdate = (String) dates.lastKey();

            String nextdate = nextdate(firstdate);
            List<String> ds = new ArrayList<String>();
            while (nextdate != null && lastdate.compareTo(nextdate) > 0) {
                ds.add(nextdate);
                nextdate = nextdate(nextdate);
            }

            if (ds.size() > 0) {
                fullZero(ds.toArray(new String[ds.size()]));
            }
        }
        return this;
    }

    /**
     * Full zero.
     *
     * @param ds
     *          the ds
     * @return the stats
     */
    public Stats fullZero(Object[] ds) {
        // private Map<String, List<Stat>> dates = new TreeMap<String,
        // List<Stat>>();
        // private Map<String, List<Stat>> names = new TreeMap<String,
        // List<Stat>>();

        if (ds == null) {
            ds = dates.keySet().toArray();
        }

        if (ds != null && ds.length > 0) {
            for (Object nextdate : ds) {

                // for each in names and date
                List<Stat> list = dates.get(nextdate);
                if (list == null) {
                    list = new ArrayList<Stat>();
                    // put empty in dates
                    if (Stats.TYPE_NUMBER.equals(type)) {
                        dates.put(Bean.toInt(nextdate), list);
                    } else {
                        dates.put(nextdate, list);
                    }
                }
                Set<String> nameslist = names.keySet();
                for (String name : nameslist) {
                    // put date in each names
                    boolean found = false;
                    List<Stat> datelist = names.get(name);
                    for (int i = 0; i < datelist.size(); i++) {
                        Stat s = datelist.get(i);

                        int ii = 0;

                        if (Stats.TYPE_NUMBER.equals(type)) {
                            ii = (Integer) nextdate - Bean.toInt(s.getDate());
                        } else {
                            ii = ((String) nextdate).compareTo(s.getDate());
                        }

                        if (ii == 0) {
                            found = true;
                            break;
                        } else if (ii > 0) {
                            continue;
                        } else {
                            // not found, put empty
                            datelist.add(i, Stat.create(X.EMPTY, nextdate.toString(), 0, 0));
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // append in the last
                        datelist.add(Stat.create(X.EMPTY, nextdate.toString(), 0, 0));
                    }
                }
            }
        }

        return this;
    }

    private String nextdate(String date) {
        Language lang = Language.getLanguage();
        if (Stats.TYPE_DAY.equals(type)) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(lang.parse(date, "yyyyMMdd"));
            c.add(Calendar.DAY_OF_MONTH, 1);
            return lang.format(c.getTimeInMillis(), "yyyyMMdd");
        } else if (Stats.TYPE_MONTH.equals(type)) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(lang.parse(date, "yyyyMM"));
            c.add(Calendar.MONTH, 1);
            return lang.format(c.getTimeInMillis(), "yyyyMM");
        } else if (Stats.TYPE_YEAR.equals(type)) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(lang.parse(date, "yyyy"));
            c.add(Calendar.YEAR, 1);
            return lang.format(c.getTimeInMillis(), "yyyy");
        }
        return null;
    }

    /**
     * Gets the.
     * 
     * @param name
     *            the name
     * @return the list
     */
    public List<Stat> get(String name) {
        return names(name);
    }

    /**
     * Names.
     * 
     * @param name
     *            the name
     * @return the list
     */
    public List<Stat> names(String name) {
        return names.get(name);
    }

    public Set<Object> getDates() {
        return dates.keySet();
    }

    public Set<String> getNames() {
        return names.keySet();
    }

    /**
     * Creates the.
     *
     * @param list
     *          the list
     * @param fields
     *          the fields
     * @param convertor
     *          the convertor
     * @return the stats
     */
    public static Stats create(List<? extends Stat> list, List<Integer> fields, IConvertor convertor) {
        return create(list, fields, null, convertor);
    }

    /**
     * Creates the.
     *
     * @param list
     *          the list
     * @param fields
     *          the fields
     * @param type
     *          the type
     * @param convertor
     *          the convertor
     * @return the stats
     */
    public static Stats create(List<? extends Stat> list, List<Integer> fields, String type, IConvertor convertor) {
        return create(list, fields, type, STAT_TOTAL, convertor);
    }

    /**
     * Creates the.
     *
     * @param list
     *          the list
     * @param fields
     *          the fields
     * @param type
     *          the type
     * @param stat
     *          the stat
     * @param convertor
     *          the convertor
     * @return the stats
     */
    public static Stats create(List<? extends Stat> list, List<Integer> fields, String type, int stat, IConvertor convertor) {
        Stats s = new Stats(list, convertor);

        s.type = type;

        /**
         * merge the by date first
         */
        s.convertDateBy(type);

        /**
         * merge data first
         */
        s.merge(fields, stat);

        /**
         * group and sort
         */
        s.group(fields);

        return s;
    }

    /**
     * merge data according the field(0,1,2, 3, 4)
     */
    private void merge(List<Integer> fs, int stat) {
        if (fs == null || fs.size() == 0)
            return;

        // date, f
        Map<String, Stat> map = new HashMap<String, Stat>();

        if (list == null || list.size() == 0)
            return;

        for (Stat s : list) {
            String group = s.getDate() + "_" + s.getFullname(fs);
            Stat s1 = map.get(group);
            if (s1 == null) {
                map.put(group, s);
            } else {
                if (stat == STAT_TOTAL) {
                    s1.add(s.getCount());
                } else if (stat == STAT_MAX) {
                    if (s.getCount() > s1.getCount()) {
                        s1.setCount(s.getCount());
                    }
                }
            }
        }

        list = new ArrayList<Stat>(map.values());
    }

    private void group(List<Integer> fs) {
        if (list == null || list.size() == 0)
            return;

        if (fs == null || fs.size() == 0)
            return;

        Comparator<Stat> sorter = null;

        if (Stats.TYPE_NUMBER.equals(type)) {
            sorter = new Comparator<Stat>() {

                public int compare(Stat o1, Stat o2) {
                    int d1 = Bean.toInt(o1.getDate());
                    int d2 = Bean.toInt(o2.getDate());

                    log.debug("d1=" + d1 + ", d2=" + d2);
                    if (d1 > d2) {
                        return 1;
                    } else if (d1 < d2) {
                        return -1;
                    }
                    return 0;
                }
            };
        }

        for (Stat s : list) {
            String date = s.getDate();

            /**
             * put to dates
             */

            List<Stat> l1 = null;
            if (Stats.TYPE_NUMBER.equals(type)) {
                l1 = dates.get(Bean.toInt(date));
            }

            if (l1 == null) {
                l1 = new ArrayList<Stat>();
                if (Stats.TYPE_NUMBER.equals(type)) {
                    dates.put(Bean.toInt(date), l1);
                } else {
                    dates.put(date, l1);
                }
            }
            l1.add(s);

            if (sorter == null) {
                Collections.sort(l1);
            } else {
                Collections.sort(l1, sorter);
            }

            String name = s.getName(fs);
            if (name == null)
                continue;

            /**
             * put to names
             */
            l1 = names.get(name);
            if (l1 == null) {
                l1 = new ArrayList<Stat>();
                names.put(name, l1);
            }
            l1.add(s);

            if (sorter == null) {
                Collections.sort(l1);
            } else {
                Collections.sort(l1, sorter);
            }
        }

        // log.debug("dates=" + dates);
        // log.debug("names=" + names);
    }

    private Comparator<Stat> comparator;

    /**
     * Sort.
     * 
     * @param sort
     *            the sort
     * @return the stats
     */
    public Stats sort(final int sort) {

        comparator = new Comparator<Stat>() {

            public int compare(Stat o1, Stat o2) {
                if (o1 == o2)
                    return 0;

                if (sort == 0) {
                    if (o1.getCount() > o2.getCount()) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else {
                    if (o1.getCount() > o2.getCount()) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }
        };

        for (String name : names.keySet()) {
            List<Stat> list = names.get(name);
            Collections.sort(list, comparator);
        }

        for (Object date : dates.keySet()) {
            List<Stat> list = dates.get(date);
            Collections.sort(list, comparator);
        }
        return this;
    }

    private int top = 10;

    /**
     * Top.
     * 
     * @param topn
     *            the topn
     * @return the stats
     */
    public Stats top(int topn) {
        this.top = topn;

        for (String name : names.keySet()) {
            List<Stat> list = names.get(name);
            for (int i = list.size() - 1; i >= topn; i--) {
                list.remove(i);
            }
        }

        for (Object date : dates.keySet()) {
            List<Stat> list = dates.get(date);
            for (int i = list.size() - 1; i >= topn; i--) {
                list.remove(i);
            }
        }

        return this;
    }

    private void convertDateBy(String type) {
        try {
            this.type = type;

            if (TYPE_HOUR.equals(type)) {
                _convertDateByHour();
            } else if (TYPE_DAY.equals(type)) {
                _convertDateByDay();
            } else if (TYPE_WEEK.equals(type)) {
                _convertDateByWeek();
            } else if (TYPE_MONTH.equals(type)) {
                _convertDateByMonth();
            } else if (TYPE_YEAR.equals(type)) {
                _convertDateByYear();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void _convertDateByWeek() {

        if (list != null && list.size() > 0) {
            Calendar cal = Calendar.getInstance();
            for (Stat s : list) {
                // int date = Bean.toInt(s.getDate().substring(0, 8));
                cal.setTimeInMillis(Language.getLanguage().parse(s.getDate().substring(0, 8), "yyyyMMdd"));
                // Bean.date2Millis(date));
                int year = cal.get(Calendar.YEAR);
                int week = cal.get(Calendar.WEEK_OF_YEAR);
                s.setDate(year + "W" + week);
            }
        }
    }

    private void _convertDateByHour() {
        if (list != null && list.size() > 0) {
            for (Stat s : list) {
                if (s.getDate().length() > 9) {
                    s.setDate(s.getDate().substring(0, 10));
                }
            }
        }
    }

    private void _convertDateByDay() {
        if (list != null && list.size() > 0) {
            for (Stat s : list) {
                if (s.getDate().length() > 7) {
                    s.setDate(s.getDate().substring(0, 8));
                }
            }
        }
    }

    private void _convertDateByMonth() {
        if (list != null && list.size() > 0) {
            for (Stat s : list) {
                if (s.getDate().length() > 5) {
                    s.setDate(s.getDate().substring(0, 6));
                }
            }
        }
    }

    private void _convertDateByYear() {
        if (list != null && list.size() > 0) {
            for (Stat s : list) {
                if (s.getDate().length() > 3) {
                    s.setDate(s.getDate().substring(0, 4));
                }
            }
        }
    }

}
