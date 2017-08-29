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
package org.giiwa.core.bean.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.Cursor;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Config;
import org.giiwa.core.json.JSON;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListCollectionsIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * The {@code MongoHelper} Class is base class for all class that database
 * access, it almost includes all methods that need for database <br>
 * all data access MUST be inherited from it
 * 
 */
public class MongoHelper implements Helper.DBHelper {

	static Log log = LogFactory.getLog(MongoHelper.class);

	/** The mongo. */
	private static volatile Map<String, MongoDatabase> mongo = new HashMap<String, MongoDatabase>();

	public static MongoHelper inst = new MongoHelper();

	public boolean isConfigured() {
		getDB();
		return mongo.size() > 0;
	}

	/**
	 * Delete the data in mongo by the query.
	 *
	 * @param collection
	 *            the collection
	 * @param q
	 *            the q
	 * @param db
	 *            the db
	 * @return the long
	 */
	public int delete(String collection, W q, String db) {
		int n = -1;
		try {
			MongoCollection<Document> db1 = getCollection(db, collection);
			if (db != null) {
				DeleteResult r = db1.deleteMany(q.query());
				// db.remove(query);
				n = (int) r.getDeletedCount();
			}

			if (log.isDebugEnabled()) {
				log.debug("delete, collection=" + collection + ", q=" + q + ", deleted=" + n);
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
		}
		return n;
	}

	/**
	 * get Mongo DB connection
	 * 
	 * @return DB
	 */
	public MongoDatabase getDB() {
		return (MongoDatabase) getDB(Helper.DEFAULT);
	}

	private MongoDatabase gdb = null;

	/**
	 * get Mongo DB connection <br>
	 * the configuration including:
	 * 
	 * <pre>
	 * mongo[database].url=
	 * mongo[database].db=
	 * mongo[database].conns=(50)
	 * mongo[database].user=(null)
	 * mongo[database].password=(null)
	 * </pre>
	 * 
	 * @param database
	 *            the name of database, if "" or null, then "default"
	 * @return DB
	 */
	public MongoDatabase getDB(String database) {
		if (gdb != null)
			return gdb;

		MongoDatabase g = null;
		if (X.isEmpty(database)) {
			database = Helper.DEFAULT;
		}
		database = database.trim();

		g = mongo.get(database);
		if (g != null) {
			return g;
		}

		synchronized (mongo) {
			g = mongo.get(database);

			if (g == null) {
				Configuration conf = Config.getConf();

				String url = conf.getString("mongo[" + database + "].url", X.EMPTY);
				String dbname = conf.getString("mongo[" + database + "].db", X.EMPTY);

				if (!X.isEmpty(url) && !X.isEmpty(dbname)) {

					g = getDB(url, dbname, conf.getInt("mongo[" + database + "].conns", 50));

					mongo.put(database, g);
				}
			}
		}

		return g;
	}

	private MongoClient client = null;

	private MongoDatabase getDB(String url, String db, int conns) {
		url = url.trim();
		db = db.trim();

		if (!url.startsWith("mongodb://")) {
			url = "mongodb://" + url;
		}

		MongoClientOptions.Builder opts = new MongoClientOptions.Builder().socketTimeout(30000)
				.serverSelectionTimeout(1000).connectionsPerHost(conns);
		client = new MongoClient(new MongoClientURI(url, opts));
		return client.getDatabase(db);
	}

	/**
	 * Gets the collection using the database connection
	 * 
	 * <br>
	 * the configuration including:
	 * 
	 * <pre>
	 * mongo[database].url=
	 * mongo[database].db=
	 * mongo[database].conns=(50)
	 * mongo[database].user=(null)
	 * mongo[database].password=(null)
	 * </pre>
	 * 
	 * @param database
	 *            the database
	 * @param collection
	 *            the collection
	 * @return DBCollection
	 */
	public MongoCollection<Document> getCollection(String database, String collection) {
		MongoDatabase g = getDB(database);

		MongoCollection<Document> d = null;

		if (g != null) {
			d = g.getCollection(collection);
		}

		if (d == null) {
			if (log.isErrorEnabled())
				log.error(database + " was miss configured, please access http://[host:port]/setup to configure");
		}
		return d;
	}

	/**
	 * Gets the collection using "default", if the same thread required twice, then
	 * return same connection but reference "+1" <br>
	 * the configuration including:
	 * 
	 * <pre>
	 * mongo[default].url=
	 * mongo[default].db=
	 * mongo[default].conns=(50)
	 * mongo[default].user=(null)
	 * mongo[default].password=(null)
	 * </pre>
	 * 
	 * @param name
	 *            the name of the collection
	 * @return DBCollection
	 */
	public MongoCollection<Document> getCollection(String name) {
		return getCollection(Helper.DEFAULT, name);
	}

	/**
	 * Load the data by the query.
	 *
	 * @param <T>
	 *            the generic type
	 * @param collection
	 *            the collection
	 * @param query
	 *            the query
	 * @param clazz
	 *            the clazz
	 * @return the Bean
	 */
	public <T extends Bean> T load(String collection, Bson query, Class<T> clazz) {
		try {
			return load(collection, query, clazz.newInstance());
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * load the data by the query.
	 *
	 * @param <T>
	 *            the subclass of Bean
	 * @param collection
	 *            the collection name
	 * @param query
	 *            the query
	 * @param b
	 *            the Bean
	 * @return the Bean
	 */
	public <T extends Bean> T load(String collection, Bson query, T b) {
		try {
			MongoCollection<Document> db = getCollection(collection);
			if (db != null) {
				FindIterable<Document> d = db.find(query);
				if (d != null) {
					Document d1 = d.first();
					if (d1 != null) {
						b.load(d1);
						return b;
					}
				}
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * load the data by the query.
	 *
	 * @param <T>
	 *            the subclass of Bean
	 * @param collection
	 *            the collection name
	 * @param query
	 *            the query
	 * @param order
	 *            the order
	 * @param b
	 *            the Bean
	 * @param db
	 *            the db
	 * @return the Bean
	 */
	public <T extends Bean> T load(String collection, Bson query, Bson order, T b, String db) {
		TimeStamp t = TimeStamp.create();
		try {
			MongoCollection<Document> db1 = getCollection(db, collection);
			if (db1 != null) {

				FindIterable<Document> d = db1.find(query);
				if (order != null) {
					d.sort(order);
				}

				if (d != null) {
					if (log.isDebugEnabled())
						log.debug("load - cost=" + t.pastms() + "ms, collection=" + collection + ", query=" + query
								+ ", order=" + order);

					Document d1 = d.first();
					if (d1 != null) {
						b.load(d1);
						return b;
					}

					// MongoCursor<Document> it = d.iterator();
					// if (it.hasNext()) {
					// b.load(it.next());
					// return b;
					// }
				} else {
					if (log.isDebugEnabled())
						log.debug("load - cost=" + t.pastms() + "ms, collection=" + collection + ", query=" + query
								+ ", order=" + order + ", result=" + null);
				}
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("query=" + query + ", order=" + order, e);
		}

		return null;
	}

	/**
	 * get the data from the collection.
	 * 
	 * @param <T>
	 *            the generic Bean Class
	 * @param query
	 *            the query
	 * @param order
	 *            the order query
	 * @param obj
	 *            the Bean Class
	 * @return T
	 */
	public <T extends Bean> T load(Bson query, Bson order, T obj) {
		String collection = getCollection(obj.getClass());
		if (collection != null) {
			return load(collection, query, order, obj, Helper.DEFAULT);
		}
		return null;

	}

	/**
	 * load the data list.
	 *
	 * @param <T>
	 *            the generic Bean Class
	 * @param collection
	 *            the collection name
	 * @param q
	 *            the query
	 * @param offset
	 *            the offset
	 * @param limit
	 *            the limit
	 * @param clazz
	 *            the Bean Class
	 * @param db
	 *            the db
	 * @return Beans
	 */
	public <T extends Bean> Beans<T> load(String collection, W q, int offset, int limit, final Class<T> clazz,
			String db) {

		TimeStamp t = TimeStamp.create();
		MongoCollection<Document> db1 = null;
		FindIterable<Document> cur = null;
		Bson query = q.query();
		Bson orderBy = q.order();

		try {

			db1 = getCollection(db, collection);
			if (db1 != null) {
				cur = db1.find(query);

				if (orderBy != null) {
					cur.sort(orderBy);
				}

				final Beans<T> bs = new Beans<T>();

				// TODO, ignore this as big performance
				// bs.total = (int) db.count(query);
				// log.debug("cost=" + t.past() + "ms, count=" + bs.total);

				cur = cur.skip(offset);
				// log.debug("skip=" + t.past() +"ms, count=" + bs.total);

				if (limit < 0) {
					limit = 1000;
				}
				cur = cur.limit(limit);

				MongoCursor<Document> it = cur.iterator();
				while (it.hasNext() && limit > 0) {
					// log.debug("hasnext=" + t.past() + "ms, count=" + bs.total);
					Document d = it.next();
					// log.debug("next=" + t.past() +"ms, count=" + bs.total);
					if (d != null) {
						T b = clazz.newInstance();
						b.load(d);
						bs.add(b);
						limit--;
					}
				}

				if (log.isDebugEnabled())
					log.debug("load - cost=" + t.pastms() + "ms, collection=" + collection + ", query=" + query
							+ ", order=" + orderBy + ", offset=" + offset + ", limit=" + limit + ", result="
							+ bs.size());

				if (t.pastms() > 10000) {
					log.warn("load - cost=" + t.pastms() + "ms, collection=" + collection + ", query=" + query
							+ ", order=" + orderBy + ", result=" + bs.size());
				}
				bs.setCost(t.pastms());
				return bs;
			}
		} catch (Exception e) {
			log.error("query=" + query + ", order=" + orderBy, e);
		}

		return null;
	}

	/**
	 * load the data full into the t.
	 * 
	 * @param <T>
	 *            the generic Bean Class
	 * @param query
	 *            the query
	 * @param t
	 *            the Bean Class
	 * @return Bean if failed, return null
	 */
	public <T extends Bean> T load(Bson query, T t) {
		String collection = getCollection(t.getClass());
		if (collection != null) {
			try {
				return load(query, null, t);
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error(e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * load the data by the query.
	 *
	 * @param <T>
	 *            the generic Bean Class
	 * @param query
	 *            the query
	 * @param t
	 *            the Bean Class
	 * @return Bean the instance of the Class
	 */
	public <T extends Bean> T load(Bson query, Class<T> t) {

		String collection = getCollection(t);
		if (collection != null) {
			try {
				T obj = t.newInstance();
				return load(query, null, obj);
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error(e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * load the Bean by the query, and order.
	 *
	 * @param <T>
	 *            the generic Bean Class
	 * @param query
	 *            the query
	 * @param order
	 *            the orderby
	 * @param t
	 *            the Class Bean
	 * @return Bean
	 */
	public <T extends Bean> T load(Bson query, Bson order, Class<T> t) {
		String collection = getCollection(t);
		if (collection != null) {
			try {
				T obj = t.newInstance();
				return load(query, order, obj);
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error(e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * Load.
	 *
	 * @param <T>
	 *            the generic type
	 * @param collection
	 *            the collection
	 * @param q
	 *            the q
	 * @param t
	 *            the t
	 * @param db
	 *            the db
	 * @return the t
	 */
	public <T extends Bean> T load(String collection, W q, Class<T> t, String db) {
		try {
			T obj = t.newInstance();
			return load(collection, q.query(), q.order(), obj, db);
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Load the data, and return the DBObject.
	 *
	 * @param db
	 *            the db
	 * @param collection
	 *            the collection
	 * @param query
	 *            the query
	 * @return the DB object
	 */
	public Document load(String db, String collection, Bson query) {
		/**
		 * create the sql statement
		 */
		try {
			MongoCollection<Document> c = getCollection(db, collection);
			if (c != null) {

				return c.find(query).first();
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(query, e);
		}
		return null;
	}

	/**
	 * get the collection name that associated with the Bean.
	 * 
	 * @param clazz
	 *            the Bean Class
	 * @return String
	 */
	final public String getCollection(Class<? extends Bean> clazz) {
		/**
		 * get the require annotation onGet
		 */
		Table mapping = (Table) clazz.getAnnotation(Table.class);
		if (mapping == null) {
			if (log.isErrorEnabled())
				log.error("mapping missed in [" + clazz + "] declaretion");
			return null;
		} else {
			return mapping.name();
		}
	}

	/**
	 * insert into the collection.
	 *
	 * @param collection
	 *            the collection name
	 * @param v
	 *            the values
	 * @param db
	 *            the db
	 * @return int
	 */
	public int insertTable(String collection, V v, String db) {

		MongoCollection<Document> c = getCollection(db, collection);
		if (c != null) {
			Document d = new Document();

			Object id = v.value(X.ID);
			if (!X.isEmpty(id)) {
				v.set("_id", v.value(X.ID));
			}
			for (String name : v.names()) {
				Object v1 = v.value(name);
				d.append(name, v1);
			}

			try {

				c.insertOne(d);

				if (log.isDebugEnabled())
					log.debug("inserted collection=" + collection + ", d=" + d);
				return 1;
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error(d.toString(), e);
			}
		}
		return 0;
	}

	/**
	 * update mongo collection.
	 *
	 * @param collection
	 *            the collection name
	 * @param q
	 *            the update query
	 * @param v
	 *            the value
	 * @param db
	 *            the db
	 * @return int of updated
	 */
	final public int updateTable(String collection, W q, V v, String db) {

		Document d = new Document();

		// int len = v.size();
		for (String name : v.names()) {
			Object v1 = v.value(name);
			d.append(name, v1);
		}

		try {
			// log.debug("data=" + d);
			MongoCollection<Document> c = getCollection(db, collection);
			UpdateResult r = c.updateMany(q.query(), new Document("$set", d));

			if (log.isDebugEnabled())
				log.debug("updated collection=" + collection + ", query=" + q + ", d=" + d + ", n="
						+ r.getModifiedCount() + ",result=" + r);

			// r.getN();
			// r.getField("nModified");
			return (int) r.getModifiedCount();
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
		}
		return 0;
	}

	/**
	 * test the data exists ?.
	 *
	 * @param collection
	 *            the collection name
	 * @param q
	 *            the q
	 * @param db
	 *            the db
	 * @return true: if exists, false: not exists
	 */
	public boolean exists(String collection, W q, String db) {
		TimeStamp t1 = TimeStamp.create();
		boolean b = false;
		try {
			b = load(db, collection, q.query()) != null;
		} finally {
			if (log.isDebugEnabled())
				log.debug("exists cost=" + t1.pastms() + "ms,  collection=" + collection + ", query=" + q + ", result="
						+ b);
		}
		return b;
	}

	/**
	 * run the command of Mongo.
	 *
	 * @param cmd
	 *            the command
	 * @return boolean, return true if "ok"
	 */
	public Document run(Bson cmd) {
		MongoDatabase d = getDB();
		if (d != null) {
			return d.runCommand(cmd);
		}
		return null;
	}

	/**
	 * get all collections
	 * 
	 * @return Set
	 */
	public static Set<String> getCollections() {
		return getCollections(Helper.DEFAULT);
	}

	public static Set<String> getCollections(String dbname) {
		MongoDatabase d = mongo.get(dbname);
		if (d != null) {

			MongoIterable<String> it = d.listCollectionNames();
			MongoCursor<String> ii = it.iterator();
			Set<String> r = new TreeSet<String>();
			while (ii.hasNext()) {
				r.add(ii.next());
			}
			return r;
		}
		return null;
	}

	/**
	 * remove all the data from the collection.
	 *
	 * @param collection
	 *            the collection
	 */
	public void clear(String collection) {
		try {
			MongoCollection<Document> c = getCollection(Helper.DEFAULT, collection);
			if (c != null) {
				c.deleteMany(new BasicDBObject());
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
		}
	}

	/**
	 * get distinct value for key by the query.
	 *
	 * @param <T>
	 *            the base object
	 * @param collection
	 *            the collection name
	 * @param key
	 *            the key that contain the value
	 * @param q
	 *            the query
	 * @param t
	 *            the class
	 * @param db
	 *            the db
	 * @return List of the value
	 */
	public <T> List<T> distinct(String collection, String key, W q, Class<T> t, String db) {

		TimeStamp t1 = TimeStamp.create();
		try {

			MongoCollection<Document> c = getCollection(db, collection);
			if (c != null) {
				Iterator<T> it = c.distinct(key, q.query(), t).iterator();
				List<T> list = new ArrayList<T>();
				while (it.hasNext()) {
					list.add(it.next());
				}

				return (List<T>) list;
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
		} finally {
			if (log.isDebugEnabled())
				log.debug(
						"disinct[" + key + "] cost=" + t1.pastms() + "ms,  collection=" + collection + ", query=" + q);
		}
		return null;
	}

	/**
	 * count the number by the query.
	 *
	 * @param collection
	 *            the collection name
	 * @param q
	 *            the query and order
	 * @param db
	 *            the db
	 * @return long
	 */
	public long count(String collection, W q, String db) {
		TimeStamp t1 = TimeStamp.create();
		long n = 0;
		try {

			MongoCollection<Document> c = getCollection(db, collection);
			if (c != null) {
				n = c.count(q.query());
			}

		} finally {
			if (log.isDebugEnabled())
				log.debug("count, cost=" + t1.pastms() + "ms,  collection=" + collection + ", query=" + q + ", n=" + n);
		}
		return n;
	}

	/**
	 * backup the whole data from file.
	 *
	 * @param filename
	 *            the file name
	 */
	public void backup(String filename) {
		File f = new File(filename);
		f.getParentFile().mkdirs();

		try {
			ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(f));
			zip.putNextEntry(new ZipEntry("db"));
			PrintStream out = new PrintStream(zip);

			Set<String> c1 = getCollections();
			log.debug("collections=" + c1);
			for (String table : c1) {
				_backup(out, table);
			}

			zip.closeEntry();
			zip.close();

		} catch (Exception e) {
			log.debug(e.getMessage(), e);
		}
	}

	private void _backup(PrintStream out, String tablename) {
		log.debug("backuping " + tablename);
		MongoCollection<Document> d1 = getCollection(Helper.DEFAULT, tablename);
		MongoCursor<Document> c1 = d1.find().iterator();
		int rows = 0;
		while (c1.hasNext()) {
			rows++;

			Document d2 = c1.next();
			JSON jo = new JSON();
			jo.put("_table", tablename);
			for (String name : d2.keySet()) {
				jo.put(name, d2.get(name));
			}
			out.println(jo.toString());
			if (rows % 1000 == 0)
				log.debug("backup " + tablename + ", rows=" + rows);
		}
	}

	/**
	 * recover the database from the file, the old data will be erased, index will
	 * be keep.
	 *
	 * @param file
	 *            the mongo.dmp file
	 */
	public void recover(File file) {

		try {
			ZipInputStream zip = new ZipInputStream(new FileInputStream(file));
			zip.getNextEntry();
			BufferedReader in = new BufferedReader(new InputStreamReader(zip));

			Set<String> c1 = getCollections();
			log.debug("collections=" + c1);
			for (String table : c1) {
				MongoCollection<Document> c2 = getCollection(Helper.DEFAULT, table);
				try {
					c2.drop();
				} catch (Exception e) {
					log.error("table=" + table, e);
				}
			}

			String line = in.readLine();
			while (line != null) {
				_recover(line);
				line = in.readLine();
			}
			zip.closeEntry();
			in.close();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	private void _recover(String json) {
		try {
			JSON jo = JSON.fromObject(json);
			V v = V.create().copy(jo);
			String tablename = jo.getString("_table");
			v.remove("_table");
			inst.insertTable(tablename, v, Helper.DEFAULT);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * inc.
	 *
	 * @param table
	 *            the table
	 * @param q
	 *            the q
	 * @param name
	 *            the name
	 * @param n
	 *            the n
	 * @param db
	 *            the db
	 * @return the int
	 */
	public int inc(String table, W q, String name, int n, V v, String db) {
		Document d = new Document();

		try {
			d.put(name, n);
			MongoCollection<Document> c = getCollection(db, table);
			Document d2 = new Document("$inc", d);

			Document d1 = null;
			if (v != null) {
				d1 = new Document();
				for (String s : v.names()) {
					Object v1 = v.value(s);
					d1.append(s, v1);
				}
				d2.append("$set", d1);
			}

			UpdateResult r = c.updateMany(q.query(), d2);

			if (log.isDebugEnabled())
				log.debug("updated collection=" + table + ", query=" + q + ", d=" + d + ", n=" + r.getModifiedCount()
						+ ",result=" + r);

			return (int) r.getModifiedCount();
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
		}
		return 0;
	}

	public void createIndex(String table, LinkedHashMap<String, Integer> ss, String db) {
		MongoCollection<Document> c = getCollection(db, table);
		BasicDBObject q = new BasicDBObject();
		for (String s : ss.keySet()) {
			q.append(s, ss.get(s));
		}
		try {
			c.createIndex(q);
		} catch (Exception e) {
			log.error(q, e);
		}
	}

	@Override
	public void dropIndex(String table, String name, String db) {
		MongoCollection<Document> c = getCollection(db, table);
		c.dropIndex(name);
	}

	@Override
	public List<Map<String, Object>> getIndexes(String table, String db) {

		List<Map<String, Object>> l1 = new ArrayList<Map<String, Object>>();

		MongoCollection<Document> c = getCollection(db, table);
		MongoCursor<Document> i1 = c.listIndexes().iterator();
		while (i1.hasNext()) {
			Document d1 = i1.next();
			l1.add(d1);
		}

		return l1;
	}

	@Override
	public int insertTable(String collection, List<V> values, String db) {

		if (X.isEmpty(values))
			return 0;

		MongoCollection<Document> c = getCollection(db, collection);
		if (c != null) {

			TimeStamp t = TimeStamp.create();
			List<Document> l1 = new ArrayList<Document>();

			for (V v : values) {
				Document d = new Document();

				Object id = v.value(X.ID);
				if (!X.isEmpty(id)) {
					v.set("_id", v.value(X.ID));
				}
				for (String name : v.names()) {
					Object v1 = v.value(name);
					d.append(name, v1);
				}
				l1.add(d);
			}

			try {

				c.insertMany(l1);

				if (log.isDebugEnabled())
					log.debug("inserted collection=" + collection + ", cost=" + t.pastms() + ", size=" + l1.size());
				return l1.size();
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("cost=" + t.pastms() + "ms", e);
			}
		}
		return 0;
	}

	@Override
	public List<JSON> listTables(String db) {
		MongoDatabase g = getDB(db);

		List<JSON> list = new ArrayList<JSON>();
		if (g != null) {
			ListCollectionsIterable<Document> it = g.listCollections();
			for (Document d : it) {
				JSON j = JSON.create();
				j.put("table_name", d.getString("name"));
				j.putAll(d);
				list.add(j);
			}
		}

		Collections.sort(list, new Comparator<JSON>() {

			@Override
			public int compare(JSON o1, JSON o2) {
				return o1.getString("table_name").compareToIgnoreCase(o2.getString("table_name"));
			}

		});
		return list;
	}

	@Override
	public void close() {
		// forget this, the client may used in other helper
		// if (client != null && this != Helper.primary) {
		// client.close();
		// }
	}

	@Override
	public <T extends Bean> Cursor<T> query(String table, W q, int s, int n, Class<T> t, String db) {

		MongoCollection<Document> db1 = null;
		FindIterable<Document> cur = null;
		Bson query = q.query();
		Bson orderBy = q.order();

		try {

			db1 = getCollection(db, table);
			if (db1 != null) {
				cur = db1.find(query);

				if (orderBy != null) {
					cur.sort(orderBy);
				}
				if (s > 0) {
					cur.skip(s);
				}
				if (n > 0) {
					cur.limit(n);
				}

				MongoCursor<Document> it = cur.iterator();
				return _cursor(t, it);

			}
		} catch (Exception e) {
			log.error("query=" + query + ", order=" + orderBy, e);
		}

		return null;
	}

	private <T extends Bean> Cursor<T> _cursor(final Class<T> t, final MongoCursor<Document> cur) {
		return new Cursor<T>() {

			@Override
			public boolean hasNext() {
				return cur.hasNext();
			}

			@Override
			public T next() {

				try {
					T b = t.newInstance();
					b.load(cur.next());
					return b;
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				return null;
			}

			@Override
			public void close() {
				cur.close();
			}

		};
	}

	/**
	 * 
	 * @param url,
	 *            "mongodb://host:port/dbname"
	 * @return
	 */
	public static MongoHelper create(String url) {
		//
		int i = url.lastIndexOf("/");
		if (i < 0)
			return null;

		String dbname = url.substring(i + 1);
		url = url.substring(0, i);
		MongoHelper h = new MongoHelper();
		MongoDatabase g = h.getDB(url, dbname, 10);
		h.gdb = g;
		return h;
	}

	@Override
	public List<JSON> getMetaData(String tablename) {
		MongoCollection<Document> c = getCollection(tablename);
		List<JSON> list = new ArrayList<JSON>();
		list.add(JSON.create().append("name", tablename).append("size", c.count()));
		log.debug("");
		return list;
	}

	public static void main(String[] args) {
		MongoHelper h = MongoHelper.create("mongodb://127.0.0.1:27018/demo");
		int t = h.sum("gi_user", W.create(), "logintimes", "default");
		System.out.println("t=" + t);
	}

	@Override
	public <T> T sum(String collection, W q, String name, String db) {

		TimeStamp t1 = TimeStamp.create();
		Object n = 0;
		try {

			MongoCollection<Document> c = getCollection(db, collection);
			if (c != null) {
				BasicDBObject group = new BasicDBObject();
				group.append("$group", new BasicDBObject().append("_id", name).append(name,
						new BasicDBObject().append("$sum", "$" + name)));

				List<BasicDBObject> l1 = Arrays.asList(group,
						new BasicDBObject("$match", q == null ? new BasicDBObject() : q.query()));

				System.out.println(l1);

				MongoCursor<Document> it = c.aggregate(l1).iterator();
				if (it != null && it.hasNext()) {
					Document d = it.next();
					if (d != null) {
						n = d.get(name);
					}
				}
			}

		} finally {
			if (log.isDebugEnabled())
				log.debug("count, cost=" + t1.pastms() + "ms,  collection=" + collection + ", query=" + q + ", n=" + n);
		}
		return (T) n;
	}

}
