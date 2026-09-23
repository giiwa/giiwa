package org.giiwa.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.giiwa.bean.Temp;
import org.giiwa.dao.Comment;
import org.giiwa.dao.X;
import org.giiwa.misc.Exporter;

public class JSONS extends ArrayList<JSON> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static enum TYPE {
		GROUP, LIST
	};

	private Map<String, JSON> _meta = null;

	private String[] selected;
	private TYPE type = TYPE.LIST;

	public void setMeta(List<JSON> meta) {

		if (meta == null || meta.isEmpty()) {
			return;
		}

		this._meta = new HashMap<String, JSON>();
		for (JSON e : meta) {
			this._meta.put(e.getString(X.NAME), e);
		}
	}

	public static JSONS create(List<JSON> l1) {
		JSONS e = new JSONS();
		e.addAll(l1);
		return e;
	}

	public static JSONS create() {
		return new JSONS();
	}

	public static JSONS create(TYPE type) {
		JSONS l1 = new JSONS();
		l1.type = type;
		return l1;
	}

	@Comment(text = "结果总数 >= size()")
	public Object total;

	@Comment(text = "结果总数 >= size()")
	public Object total() {
		if (total == null) {
			if (TYPE.LIST.equals(type)) {
				total = this.size();
			} else {
				int n = 0;
				for (JSON e : this) {
					List<JSON> l2 = e.getList(X.LIST);
					if (l2 != null) {
						n += l2.size();
					}
				}
				total = n;
			}
		}
		return total;
	}

	@Comment(text = "排序")
	public JSONS sort(@Comment(text = "names") String... names) {
		Collections.sort(this, new Comparator<JSON>() {

			@Override
			public int compare(JSON o1, JSON o2) {
				for (String name : names) {
					int n = X.compareTo(o1.get(name), o2.get(name));
					if (n != 0) {
						return n;
					}
				}
				return 0;
			}

		});
		return this;
	}

	@Comment(text = "排序， asc=1增序，asc=-1倒序")
	public JSONS sort(@Comment(text = X.NAME) String name, @Comment(text = "asc") int asc) {
		Collections.sort(this, new Comparator<JSON>() {

			@Override
			public int compare(JSON o1, JSON o2) {
				if (asc >= 0) {
					return X.compareTo(o1.get(name), o2.get(name));
				} else {
					return -X.compareTo(o1.get(name), o2.get(name));
				}
			}

		});
		return this;
	}

	@Comment(text = "截取TOPn")
	public JSONS limit(@Comment(text = X.N) int n) {
		return top(n);
	}

	@Comment(text = "截取TOPn")
	public JSONS top(@Comment(text = X.N) int n) {
		return sublist(0, n);
	}

	@Comment(text = "截取子串")
	public JSONS sublist(@Comment(text = X.S) int s, @Comment(text = X.N) int n) {
		JSONS l0 = this.data();
		JSONS l1 = JSONS.create();
		for (int i = 0; i < n && (i + s) < l0.size(); i++) {
			l1.add(l0.get(s + i));
		}
		return l1;
	}

	@Comment(text = "截取子串, 用name，value过滤")
	public JSONS sublist(@Comment(text = X.NAME) String name, @Comment(text = "value") Object value) {
		JSONS l0 = this.data();
		JSONS l1 = JSONS.create();
		for (int i = 0; i < l0.size(); i++) {
			JSON j1 = l0.get(i);
			if (X.isSame(j1.get(name), value)) {
				l1.add(j1);
			}
		}
		return l1;
	}

	@Comment(text = "求和")
	public double sum(@Comment(text = "字段") String name) {
		double s = 0;
		JSONS l0 = this.data();
		for (int i = 0; i < l0.size(); i++) {
			JSON j1 = l0.get(i);
			s += j1.getDouble(name);
		}
		return s;
	}

	@Comment(text = "均值")
	public double mean(@Comment(text = "字段") String name) {
		double s = 0;
		JSONS l0 = this.data();
		for (int i = 0; i < l0.size(); i++) {
			JSON j1 = l0.get(i);
			s += j1.getDouble(name);
		}
		return s / l0.size();
	}

	@Comment(text = "翻转队列")
	public JSONS reverse() {
		Collections.reverse(this);
		return this;
	}

	@Comment(text = "去列", demo = ".unselect('a','b')")
	public JSONS unselect(String... names) {
		if (type == TYPE.LIST) {
			for (int i = 0; i < this.size(); i++) {
				JSON e1 = this.get(i);
				e1.remove(names);
			}
		} else {
			for (JSON e : this) {
				List<JSON> l1 = e.getList(X.LIST);
				if (l1 != null) {
					for (int i = 0; i < l1.size(); i++) {
						JSON e1 = l1.get(i);
						e1.remove(names);
					}
				}
			}
		}
		return this;
	}

	@Comment(text = "选择列", demo = ".select('a','b')")
	public JSONS select(String... names) {
		selected = names;
		if (type == TYPE.LIST) {
			for (int i = 0; i < this.size(); i++) {
				JSON e1 = this.get(i).copy(names);
				this.set(i, e1);
			}
		} else {
			for (JSON e : this) {
				List<JSON> l1 = e.getList(X.LIST);
				if (l1 != null) {
					for (int i = 0; i < l1.size(); i++) {
						JSON e1 = l1.get(i).copy(names);
						l1.set(i, e1);
					}
				}
			}
		}
		return this;
	}

	@Comment(text = "输出为json数组", demo = ".jsons()")
	public JSONS jsons() {
		return this.data();
	}

	@Comment(text = "输出为CSV文件", demo = ".csv()")
	public String csv() throws IOException {
		Temp t = Temp.create("a.csv");
		X.IO.mkdirs(t.getFile().getParentFile());
		t.getFile().createNewFile();
		Exporter<?> ex = Exporter.create(t.getFile(), Exporter.FORMAT.csv);

		// print head
		String[] cc = null;
		if (selected != null && !selected.toString().isEmpty()) {
			cc = selected;
		} else {
			if (!this.isEmpty()) {
				Set<String> names = this.get(0).keySet();
				cc = names.toArray(new String[names.size()]);
			}
		}
		if (cc != null) {
			if (_meta != null) {
				String[] ss = new String[cc.length];
				for (int i = 0; i < cc.length; i++) {
					String s = cc[i];
					JSON c = _meta.get(s);
					if (c == null) {
						ss[i] = s;
					} else {
						ss[i] = c.getString("display");
					}
				}
				ex.print(ss);
			} else {
				String[] ss = new String[cc.length];
				for (int i = 0; i < cc.length; i++) {
					String[] s1 = X.split(cc[i], "[:：]");
					if (s1.length < 2) {
						ss[i] = s1[0];
					} else {
						ss[i] = s1[1];
						cc[i] = s1[0];
					}
				}
				ex.print(ss);
			}

			String[] cc1 = cc;

			List<JSON> l1 = this.data();

			for (JSON j1 : l1) {
				Object[] v = new Object[cc1.length];
				for (int i = 0; i < cc1.length; i++) {
					v[i] = j1.get(cc1[i]);
				}
				ex.print(v);
			}
		}

		ex.close();
		return X.IO.read(t.getFile(), X.UTF8);
	}

	private JSONS data() {
		if (type == TYPE.LIST) {
			return this;
		}

		List<JSON> l1 = JSON.createList();
		int i = 0;
		boolean add = true;
		while (add) {
			add = false;
			for (JSON e : this) {
				List<JSON> l2 = e.getList(X.LIST);
				if (l2 != null && i < l2.size()) {
					l1.add(l2.get(i));
					add = true;
				}
			}
			i++;
		}

		return JSONS.create(l1);
	}

	@Comment(text = "输出为CSV文件", demo = ".csv('a','b')")
	public String csv(@Comment(text = "names") String... names) throws IOException {
		selected = names;
		return csv();
	}

	@Comment(text = "合并列表")
	public JSONS merge(@Comment(text = "l2") List<JSON> l2, @Comment(text = X.NAME) String name) {
		for (JSON j2 : l2) {
			JSON j1 = X.get(this, name, j2.get(name));
			if (j1 == null) {
				this.add(j2);
			}
		}
		return this;
	}
}
