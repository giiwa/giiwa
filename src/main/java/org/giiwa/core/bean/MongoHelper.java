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
package org.giiwa.core.bean;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.giiwa.core.base.StringFinder;
import org.giiwa.core.bean.Helper.Cursor;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Config;
import org.giiwa.core.json.JSON;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
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

	private static Log log = LogFactory.getLog(MongoHelper.class);

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
	 * @param collection the collection
	 * @param q          the q
	 * @param db         the db
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
	 * @param database the name of database, if "" or null, then "default"
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
				String user = conf.getString("mongo[" + database + "].user", X.EMPTY);
				String passwd = conf.getString("mongo[" + database + "].passwd", X.EMPTY);
				int conns = conf.getInt("mongo[" + database + "].conns", 50);
				int timeout = conf.getInt("mongo[" + database + "].timeout", 30000);

				if (!X.isEmpty(url) && !X.isEmpty(dbname)) {

					g = getDB(url, dbname, user, passwd, conns, timeout);

					mongo.put(database, g);
				}
			}
		}

		return g;
	}

	private MongoClient client = null;

	private MongoDatabase getDB(String url, String db, String user, String passwd, int conns, int timeout) {
		url = url.trim();
		db = db.trim();

		if (!url.startsWith("mongodb://")) {
			url = "mongodb://" + url;
		}

		MongoClientOptions.Builder opts = new MongoClientOptions.Builder().socketTimeout(timeout)
				.serverSelectionTimeout(timeout).maxConnectionIdleTime(10000).connectionsPerHost(conns);

		if (X.isEmpty(user)) {

			client = new MongoClient(new MongoClientURI(url, opts));

			return client.getDatabase(db);

		} else {

			// url=mongodb://host1:27017,host2:27017
			List<ServerAddress> servers = new ArrayList<ServerAddress>();
			String s = url.replaceFirst("mongodb://", X.EMPTY);
			String[] ss = X.split(s, ",");
			for (String s1 : ss) {
				String[] s2 = X.split(s1, ":");
				if (s2.length == 1) {
					servers.add(new ServerAddress(s2[0], 27017));
				} else if (s2.length > 1) {
					servers.add(new ServerAddress(s2[0], X.toInt(s2[1])));
				}
			}
			List<MongoCredential> users = Arrays
					.asList(MongoCredential.createCredential(user, db, passwd.toCharArray()));

			client = new MongoClient(servers, users, opts.build());

			log.info("mongodb.user=" + user + ", client=" + client);

			return client.getDatabase(db);
		}
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
	 * @param database   the database
	 * @param collection the collection
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
	 * @param name the name of the collection
	 * @return DBCollection
	 */
	public MongoCollection<Document> getCollection(String name) {
		return getCollection(Helper.DEFAULT, name);
	}

	/**
	 * Load the data by the query.
	 *
	 * @param <T>        the generic type
	 * @param collection the collection
	 * @param query      the query
	 * @param clazz      the clazz
	 * @return the Bean
	 */
	public <T extends Bean> T load(String collection, Bson query, Class<T> clazz) {
		try {
			return load(collection, query, clazz.newInstance());
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		}

		return null;
	}

	/**
	 * load the data by the query.
	 *
	 * @param <T>        the subclass of Bean
	 * @param collection the collection name
	 * @param query      the query
	 * @param b          the Bean
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
						b.load(d1, null);
						return b;
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		}

		return null;
	}

	/**
	 * load the data by the query.
	 *
	 * @param <T>        the subclass of Bean
	 * @param collection the collection name
	 * @param query      the query
	 * @param order      the order
	 * @param b          the Bean
	 * @param db         the db
	 * @return the Bean
	 */
	public <T extends Bean> T load(String collection, String[] fields, Bson query, Bson order, T b, String db) {
		TimeStamp t = TimeStamp.create();
		try {
			MongoCollection<Document> db1 = getCollection(db, collection);
			if (db1 != null) {

				FindIterable<Document> d = db1.find(query);
				if (order != null) {
					d.sort(order);
				}

				if (d != null) {

					Document d1 = d.first();

					if (log.isDebugEnabled())
						log.debug("load - cost=" + t.pastms() + "ms, collection=" + collection + ", db=" + db
								+ ", query=" + query + ", order=" + order + ", d=" + (d1 != null ? 1 : 0));

					if (d1 != null) {
						b.load(d1, fields);
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
			log.error("query=" + query + ", order=" + order, e);

			// bad connection ? close the it ?
		}

		return null;
	}

	/**
	 * get the data from the collection.
	 * 
	 * @param <T>   the generic Bean Class
	 * @param query the query
	 * @param order the order query
	 * @param obj   the Bean Class
	 * @return T
	 */
	public <T extends Bean> T load(Bson query, Bson order, T obj) {
		String collection = getCollection(obj.getClass());
		if (collection != null) {
			return load(collection, null, query, order, obj, Helper.DEFAULT);
		}
		return null;

	}

	/**
	 * load the data list.
	 *
	 * @param <T>        the generic Bean Class
	 * @param collection the collection name
	 * @param q          the query
	 * @param offset     the offset
	 * @param limit      the limit
	 * @param clazz      the Bean Class
	 * @param db         the db
	 * @return Beans
	 */
	public <T extends Bean> Beans<T> load(String collection, String[] fields, W q, int offset, int limit,
			final Class<T> clazz, String db) throws SQLException {

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

				long rowid = offset;
				MongoCursor<Document> it = cur.iterator();
				while (it.hasNext() && limit > 0) {
					// log.debug("hasnext=" + t.past() + "ms, count=" + bs.total);
					Document d = it.next();
					// log.debug("next=" + t.past() +"ms, count=" + bs.total);
					if (d != null) {
						T b = clazz.newInstance();
						b.load(d, fields);
						b._rowid = rowid++;
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
			throw new SQLException(e);
		}

		return null;
	}

	/**
	 * load the data full into the t.
	 * 
	 * @param <T>   the generic Bean Class
	 * @param query the query
	 * @param t     the Bean Class
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
	 * @param <T>   the generic Bean Class
	 * @param query the query
	 * @param t     the Bean Class
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
	 * @param <T>   the generic Bean Class
	 * @param query the query
	 * @param order the orderby
	 * @param t     the Class Bean
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
	 * @param <T>        the generic type
	 * @param collection the collection
	 * @param q          the q
	 * @param t          the t
	 * @param db         the db
	 * @return the t
	 */
	public <T extends Bean> T load(String collection, String[] fields, W q, Class<T> t, String db) {
		try {
			T obj = t.newInstance();
			return load(collection, fields, q.query(), q.order(), obj, db);
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		}
		return null;
	}

	/**
	 * Load the data, and return the DBObject.
	 *
	 * @param db         the db
	 * @param collection the collection
	 * @param query      the query
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
			log.error(query, e);

		}
		return null;
	}

	/**
	 * get the collection name that associated with the Bean.
	 * 
	 * @param clazz the Bean Class
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
	 * @param collection the collection name
	 * @param v          the values
	 * @param db         the db
	 * @return int
	 */
	public int insertTable(String collection, V v, String db) {

		MongoCollection<Document> c = getCollection(db, collection);
		if (c != null) {
			Document d = new Document();

			Object id = v.value(X.ID);
			if (!X.isEmpty(id)) {
				v.append("_id", v.value(X.ID));
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
				log.error(d.toString(), e);

			}
		}
		return 0;
	}

	/**
	 * update mongo collection.
	 *
	 * @param collection the collection name
	 * @param q          the update query
	 * @param v          the value
	 * @param db         the db
	 * @return int of updated
	 */
	final public int updateTable(String collection, W q, V v, String db) {

		TimeStamp t = TimeStamp.create();
		Document set = new Document();
		Document unset = new Document();

		// int len = v.size();
		for (String name : v.names()) {
			Object v1 = v.value(name);
			if (v1 == null) {
				unset.append(name, X.EMPTY);
			} else {
				set.append(name, v1);
			}
		}

		try {
			// log.debug("data=" + d);
			MongoCollection<Document> c = getCollection(db, collection);
			Document d = new Document();
			if (!set.isEmpty()) {
				d.append("$set", set);
			}
			if (!unset.isEmpty()) {
				d.append("$unset", unset);
			}
			UpdateResult r = c.updateMany(q.query(), d);

			if (log.isDebugEnabled())
				log.debug("updated, cost=" + t.past() + ", collection=" + collection + ", query=" + q + ", d=" + set
						+ ", n=" + r.getModifiedCount() + ",result=" + r);

			// r.getN();
			// r.getField("nModified");
			return (int) r.getModifiedCount();
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		}
		return 0;
	}

	/**
	 * test the data exists ?.
	 *
	 * @param collection the collection name
	 * @param q          the q
	 * @param db         the db
	 * @return true: if exists, false: not exists
	 */
	public boolean exists(String collection, W q, String db) {
		TimeStamp t1 = TimeStamp.create();
		boolean b = false;
		try {
			b = load(db, collection, q.query()) != null;
		} catch (Exception e) {
			throw e;
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
	 * @param cmd the command
	 * @return Document, return true if "ok"
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
	 * @param collection the collection
	 */
	@SuppressWarnings("unused")
	public void clear(String collection) {
		try {
			MongoCollection<Document> c = getCollection(Helper.DEFAULT, collection);
			if (c != null) {
				TimeStamp t = TimeStamp.create();
				DeleteResult d = c.deleteMany(new BasicDBObject());

			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);

		}
	}

	/**
	 * 
	 */
	public List<?> distinct(String collection, String key, W q, String db) {

		TimeStamp t1 = TimeStamp.create();
		try {

			MongoDatabase g = getDB(db);
			Document d = g.runCommand(
					new BasicDBObject("distinct", collection).append("key", key).append("query", q.query()));
			if (d.containsKey("values")) {
				return (List<?>) (d.get("values"));
			}

		} catch (Exception e) {
			log.error(q.query(), e);

		} finally {
			if (log.isDebugEnabled())
				log.debug("distinct[" + key + "] cost=" + t1.pastms() + "ms,  collection=" + collection + ", query="
						+ q.query());
		}
		return null;
	}

	/**
	 * count the number by the query.
	 *
	 * @param collection the collection name
	 * @param q          the query and order
	 * @param db         the db
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
		} catch (Exception e) {
			throw e;
		} finally {
			if (log.isDebugEnabled())
				log.debug("count, cost=" + t1.pastms() + "ms,  collection=" + collection + ", query=" + q.query()
						+ ", n=" + n);
		}
		return n;
	}

	/**
	 * backup the whole data from file.
	 *
	 * @param filename the file name
	 * @param cc       the tables
	 */
	public void backup(ZipOutputStream zip, String[] cc) {

		try {
			zip.putNextEntry(new ZipEntry("mongo.db"));
			PrintStream out = new PrintStream(zip);
			if (cc == null) {
				Set<String> ss = MongoHelper.getCollections();
				cc = ss.toArray(new String[ss.size()]);
			}
			for (String table : cc) {
				_backup(out, table);
			}

			zip.closeEntry();

		} catch (Exception e) {
			log.error(e.getMessage(), e);

		}
	}

	private void _backup(PrintStream out, String tablename) {
		if (log.isDebugEnabled())
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
//			out.println(jo.toString());
			out.println(Base64.getEncoder().encodeToString(jo.toString().getBytes()));

			if (rows % 1000 == 0) {
				if (log.isDebugEnabled())
					log.debug("backup " + tablename + ", rows=" + rows);
			}
		}
	}

	/**
	 * recover the database from the file, the old data will be erased, index will
	 * be keep.
	 *
	 * @param file the mongo.dmp file
	 */
	public void recover(InputStream zip) {

		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(zip));

			String line = in.readLine();
			while (line != null) {
				_recover(line);
				line = in.readLine();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	private void _recover(String json) {
		try {
//			JSON jo = JSON.fromObject(json);
			JSON jo = JSON.fromObject(Base64.getDecoder().decode(json));
			V v = V.create().copy(jo);
			String tablename = jo.getString("_table");
			v.remove("_table");
			inst.delete(tablename, W.create(X.ID, jo.get(X.ID)), Helper.DEFAULT);
			inst.insertTable(tablename, v, Helper.DEFAULT);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * inc.
	 *
	 * @param table the table
	 * @param q     the q
	 * @param name  the name
	 * @param n     the n
	 * @param db    the db
	 * @return the int
	 */
	public int inc(String table, W q, String name, int n, V v, String db) {
		Document d = new Document();

		try {
			d.put(name, n);
			MongoCollection<Document> c = getCollection(db, table);
			Document d2 = new Document("$inc", d);

			Document d1 = null;
			if (v != null && !v.isEmpty()) {
				d1 = new Document();
				for (String s : v.names()) {
					Object v1 = v.value(s);
					d1.append(s, v1);
				}
				d2.append("$set", d1);
			}

			UpdateResult r = c.updateMany(q.query(), d2);

			if (log.isDebugEnabled())
				log.debug("updated collection=" + table + ", query=" + q + ", d2=" + d2 + ", n=" + r.getModifiedCount()
						+ ",result=" + r);

			return (int) r.getModifiedCount();
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		}
		return 0;
	}

	public void createIndex(String table, LinkedHashMap<String, Integer> ss, String db) {
		MongoCollection<Document> c = getCollection(db, table);
		BasicDBObject q = new BasicDBObject();
		int n = 0;
		for (String s : ss.keySet()) {
			int i = ss.get(s);
			if (i == 2) {
				q.append(s, "2d");
			} else {
				q.append(s, ss.get(s));
			}
			n++;
			if (n > 4) {
				break;
			}
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
					v.append("_id", v.value(X.ID));
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
		if (client != null && this != Helper.primary) {
			client.close();
			client = null;
		}

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
					b.load(cur.next(), null);
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
	 * @param url, "mongodb://host:port/dbname"
	 * @return the MongoHelper
	 */
	public static MongoHelper create(String url, String user, String passwd) {
		//
		int i = url.lastIndexOf("/");
		if (i < 0)
			return null;

		String dbname = url.substring(i + 1);
		url = url.substring(0, i);
		MongoHelper h = new MongoHelper();
		MongoDatabase g = h.getDB(url, dbname, user, passwd, 10, 30000);
		h.gdb = g;
		return h;
	}

	@Override
	public List<JSON> getMetaData(String tablename) {
		MongoCollection<Document> c = getCollection(tablename);
		List<JSON> list = new ArrayList<JSON>();
		list.add(JSON.create().append("name", tablename).append("size", c.count()));
		return list;
	}

	public static void main(String[] args) {

		MongoHelper h = MongoHelper.create("mongodb://127.0.0.1:27018/demo", "giiwa", "j123123");
		long i = h.count("gi_user", W.create(), Helper.DEFAULT);
		System.out.println("count=" + i);

		List<JSON> l1 = h.count("gi_user", W.create().and("id", 1, W.OP.gt).sort("count", -1), new String[] { "a" },
				Helper.DEFAULT);
		System.out.println("count=" + l1);

		l1 = h.max("gi_user", W.create().sort("max", -1), "b", new String[] { "a" }, Helper.DEFAULT);
		System.out.println("count=" + l1);

		l1 = h.aggregate("gi_user", new String[] { "count(b)", "min(a)" }, W.create(), new String[] { "a" },
				Helper.DEFAULT);
		System.out.println("count=" + l1);

	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T sum(String collection, W q, String name, String db) {

		TimeStamp t1 = TimeStamp.create();
		Object n = 0;
		try {

			MongoCollection<Document> c = getCollection(db, collection);
			if (c != null) {
				BasicDBObject match = new BasicDBObject("$match", q == null ? new BasicDBObject() : q.query());
				BasicDBObject group = new BasicDBObject();
				group.append("$group", new BasicDBObject().append("_id", name).append(name,
						new BasicDBObject().append("$sum", "$" + name)));

				List<BasicDBObject> l1 = Arrays.asList(match, group);

				if (log.isDebugEnabled())
					log.debug("l1=" + l1);

				MongoCursor<Document> it = c.aggregate(l1).iterator();
				if (it != null && it.hasNext()) {
					Document d = it.next();
					if (d != null) {
						n = d.get(name);
					}
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (log.isDebugEnabled())
				log.debug("sum, cost=" + t1.pastms() + "ms,  collection=" + collection + ", query=" + q + ", n=" + n);
		}
		return (T) n;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T max(String collection, W q, String name, String db) {
		TimeStamp t1 = TimeStamp.create();
		Object n = 0;
		try {

			MongoCollection<Document> c = getCollection(db, collection);
			if (c != null) {
				BasicDBObject match = new BasicDBObject("$match", q == null ? new BasicDBObject() : q.query());
				BasicDBObject group = new BasicDBObject();
				group.append("$group", new BasicDBObject().append("_id", name).append(name,
						new BasicDBObject().append("$max", "$" + name)));

				List<BasicDBObject> l1 = Arrays.asList(match, group);

				// System.out.println(l1);

				MongoCursor<Document> it = c.aggregate(l1).iterator();
				if (it != null && it.hasNext()) {
					Document d = it.next();
					if (d != null) {
						n = d.get(name);
					}
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (log.isDebugEnabled())
				log.debug("max, cost=" + t1.pastms() + "ms,  collection=" + collection + ", query=" + q + ", n=" + n);
		}
		return (T) n;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T min(String collection, W q, String name, String db) {
		TimeStamp t1 = TimeStamp.create();
		Object n = 0;
		try {

			MongoCollection<Document> c = getCollection(db, collection);
			if (c != null) {
				BasicDBObject match = new BasicDBObject("$match", q == null ? new BasicDBObject() : q.query());
				BasicDBObject group = new BasicDBObject();
				group.append("$group", new BasicDBObject().append("_id", name).append(name,
						new BasicDBObject().append("$min", "$" + name)));

				List<BasicDBObject> l1 = Arrays.asList(match, group);

				// System.out.println(l1);

				MongoCursor<Document> it = c.aggregate(l1).iterator();
				if (it != null && it.hasNext()) {
					Document d = it.next();
					if (d != null) {
						n = d.get(name);
					}
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (log.isDebugEnabled())
				log.debug("min, cost=" + t1.pastms() + "ms,  collection=" + collection + ", query=" + q + ", n=" + n);
		}
		return (T) n;
	}

	@SuppressWarnings("unchecked")
	public <T> T avg(String collection, W q, String name, String db) {
		TimeStamp t1 = TimeStamp.create();
		Object n = 0;
		try {

			MongoCollection<Document> c = getCollection(db, collection);
			if (c != null) {
				BasicDBObject match = new BasicDBObject("$match", q == null ? new BasicDBObject() : q.query());
				BasicDBObject group = new BasicDBObject();
				group.append("$group", new BasicDBObject().append("_id", name).append(name,
						new BasicDBObject().append("$avg", "$" + name)));

				List<BasicDBObject> l1 = Arrays.asList(match, group);

				// System.out.println(l1);

				MongoCursor<Document> it = c.aggregate(l1).iterator();
				if (it != null && it.hasNext()) {
					Document d = it.next();
					if (d != null) {
						n = d.get(name);
					}
				}
			}

		} catch (Exception e) {
			throw e;
		} finally {
			if (log.isDebugEnabled())
				log.debug("avg, cost=" + t1.pastms() + "ms,  collection=" + collection + ", query=" + q + ", n=" + n);
		}
		return (T) n;
	}

	@Override
	public void repair() {

		MongoDatabase g = getDB();
		g.runCommand(new BasicDBObject().append("repairDatabase", 1));

	}

	@Override
	public void drop(String table, String db) {
		MongoCollection<Document> c = getCollection(db, table);
		c.drop();
	}

	/**
	 * group by name and count
	 * 
	 * @param collection
	 * @param name
	 * @param q
	 * @param db
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<JSON> count(String collection, W q, String[] name, String db) {

		TimeStamp t = TimeStamp.create();
		MongoCollection<Document> db1 = null;

		BasicDBObject query = q.query();
		BasicDBObject order = q.order();

		try {
			if (X.isEmpty(collection)) {
				log.error("bad collection=" + collection, new Exception("bad collection=" + collection));
				return null;
			}

			if (name == null || name.length == 0) {
				log.error("bad name", new Exception("bad name"));
				return null;
			}

			db1 = getCollection(db, collection);
			if (db1 != null) {

				List<Bson> l1 = new ArrayList<Bson>();

				if (!query.isEmpty()) {
					l1.add(new BasicDBObject().append("$match", query));
				}

				BasicDBObject group = new BasicDBObject();
				for (String s : name) {
					group.append(s, "$" + s);
				}
				l1.add(new BasicDBObject().append("$group", new BasicDBObject().append("_id", group).append("count",
						new BasicDBObject().append("$sum", 1))));

				if (!order.isEmpty()) {
					BasicDBObject ord = new BasicDBObject();
					for (Map.Entry<String, Object> s : order.entrySet()) {
						if (X.isSame(s.getKey(), "count")) {
							ord.append(s.getKey(), s.getValue());
						} else {
							ord.append("_id." + s.getKey(), s.getValue());
						}
					}
					l1.add(new BasicDBObject().append("$sort", ord));
				}

				AggregateIterable<Document> a1 = db1.aggregate(l1);

				List<JSON> l2 = JSON.createList();
				for (Document d : a1) {
					JSON j1 = JSON.fromObject(d);
					Object o = j1.remove("_id");
					if (o instanceof Map) {
						j1.putAll((Map) o);
						l2.add(j1);
					}
				}

				if (log.isDebugEnabled())
					log.debug("count " + collection + ", cost=" + t.past() + ", query=" + l1 + ", result=" + l2);
//				System.out.println("count, cost=" + t.past() + ", query=" + l1 + ", result=" + l2);

				return l2;
			}
		} catch (Exception e) {
			log.error("query=" + query + ", order=" + order, e);

		}

		return null;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	/**
	 * 
	 * @param table the table name
	 * @param func  the func list, sum(n), count(a), min(b)
	 * @param q     the query
	 * @param group the group
	 * @param db    the db name
	 * @return
	 */
	public List<JSON> aggregate(String table, String[] func, W q, String[] group, String db) {

		TimeStamp t = TimeStamp.create();
		MongoCollection<Document> db1 = null;

		BasicDBObject query = q.query();
		BasicDBObject order = q.order();

		List<Bson> l1 = new ArrayList<Bson>();

		try {
			if (X.isEmpty(table)) {
				log.error("bad table=" + table, new Exception("bad table=" + table));
				return null;
			}

			if (group == null || group.length == 0) {
				log.error("bad group", new Exception("bad group"));
				return null;
			}

			db1 = getCollection(db, table);
			if (db1 != null) {

				if (!query.isEmpty()) {
					l1.add(new BasicDBObject().append("$match", query));
				}

				BasicDBObject g1 = new BasicDBObject();
				for (String s : group) {
					g1.append(s, "$" + s);
				}

				BasicDBObject g2 = new BasicDBObject().append("_id", g1);
				for (String f1 : func) {
					StringFinder sf = StringFinder.create(f1);
					sf.trim();
					String fc = sf.nextTo("(");
					sf.skip(1);
					sf.trim();
					String name = sf.nextTo(")");
					if (X.isSame(fc, "count")) {
						g2.append(f1, new BasicDBObject().append("$sum", 1));
					} else {
						g2.append(f1, new BasicDBObject().append("$" + fc, "$" + name));
					}
				}

				l1.add(new BasicDBObject().append("$group", g2));

				if (!order.isEmpty()) {
					l1.add(new BasicDBObject().append("$sort", order));
				}

				AggregateIterable<Document> a1 = db1.aggregate(l1);

				List<JSON> l2 = JSON.createList();
				for (Document d : a1) {
					JSON j1 = JSON.fromObject(d);
					Object o = j1.remove("_id");
					if (o instanceof Map) {
						j1.putAll((Map) o);
						l2.add(j1);
					}
				}

				if (log.isDebugEnabled())
					log.debug("count, cost=" + t.past() + ", query=" + l1 + ", result=" + l2);

				return l2;
			}
		} catch (Exception e) {
			log.error("l1=" + l1 + ", query=" + query, e);

		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<JSON> sum(String table, W q, String name, String[] group, String db) {

		TimeStamp t = TimeStamp.create();
		MongoCollection<Document> db1 = null;

		BasicDBObject query = q.query();
		BasicDBObject order = q.order();

		try {
			if (X.isEmpty(table)) {
				log.error("bad table=" + table, new Exception("bad table=" + table));
				return null;
			}

			if (group == null || group.length == 0) {
				log.error("bad group", new Exception("bad group"));
				return null;
			}

			db1 = getCollection(db, table);
			if (db1 != null) {

				List<Bson> l1 = new ArrayList<Bson>();

				if (!query.isEmpty()) {
					l1.add(new BasicDBObject().append("$match", query));
				}

				BasicDBObject g1 = new BasicDBObject();
				for (String s : group) {
					g1.append(s, "$" + s);
				}
				l1.add(new BasicDBObject().append("$group", new BasicDBObject().append("_id", g1).append("sum",
						new BasicDBObject().append("$sum", "$" + name))));

				if (!order.isEmpty()) {
					l1.add(new BasicDBObject().append("$sort", order));
				}

				AggregateIterable<Document> a1 = db1.aggregate(l1);

				List<JSON> l2 = JSON.createList();
				for (Document d : a1) {
					JSON j1 = JSON.fromObject(d);
					Object o = j1.remove("_id");
					if (o instanceof Map) {
						j1.putAll((Map) o);
						l2.add(j1);
					}
				}

				if (log.isDebugEnabled())
					log.debug("count, cost=" + t.past() + ", query=" + l1 + ", result=" + l2);

				return l2;
			}
		} catch (Exception e) {
			log.error("query=" + query + ", order=" + order, e);

		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<JSON> max(String table, W q, String name, String[] group, String db) {
		TimeStamp t = TimeStamp.create();
		MongoCollection<Document> db1 = null;

		BasicDBObject query = q.query();
		BasicDBObject order = q.order();

		try {
			if (X.isEmpty(table)) {
				log.error("bad table=" + table, new Exception("bad table=" + table));
				return null;
			}

			if (group == null || group.length == 0) {
				log.error("bad group", new Exception("bad group"));
				return null;
			}

			db1 = getCollection(db, table);
			if (db1 != null) {

				List<Bson> l1 = new ArrayList<Bson>();

				if (!query.isEmpty()) {
					l1.add(new BasicDBObject().append("$match", query));
				}

				BasicDBObject g1 = new BasicDBObject();
				for (String s : group) {
					g1.append(s, "$" + s);
				}
				l1.add(new BasicDBObject().append("$group", new BasicDBObject().append("_id", g1).append("max",
						new BasicDBObject().append("$max", "$" + name))));

				if (!order.isEmpty()) {
					l1.add(new BasicDBObject().append("$sort", order));
				}

				AggregateIterable<Document> a1 = db1.aggregate(l1);

				List<JSON> l2 = JSON.createList();
				for (Document d : a1) {
					JSON j1 = JSON.fromObject(d);
					Object o = j1.remove("_id");
					if (o instanceof Map) {
						j1.putAll((Map) o);
						l2.add(j1);
					}
				}

				if (log.isDebugEnabled())
					log.debug("max, cost=" + t.past() + ", query=" + l1 + ", result=" + l2);
//				System.out.println("max, cost=" + t.past() + ", query=" + l1 + ", result=" + l2);

				return l2;
			}
		} catch (Exception e) {
			log.error("query=" + query + ", order=" + order, e);

		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<JSON> min(String table, W q, String name, String[] group, String db) {
		TimeStamp t = TimeStamp.create();
		MongoCollection<Document> db1 = null;

		BasicDBObject query = q.query();
		BasicDBObject order = q.order();

		try {
			if (X.isEmpty(table)) {
				log.error("bad table=" + table, new Exception("bad table=" + table));
				return null;
			}

			if (group == null || group.length == 0) {
				log.error("bad group", new Exception("bad group"));
				return null;
			}

			db1 = getCollection(db, table);
			if (db1 != null) {

				List<Bson> l1 = new ArrayList<Bson>();

				if (!query.isEmpty()) {
					l1.add(new BasicDBObject().append("$match", query));
				}

				BasicDBObject g1 = new BasicDBObject();
				for (String s : group) {
					g1.append(s, "$" + s);
				}
				l1.add(new BasicDBObject().append("$group", new BasicDBObject().append("_id", g1).append("min",
						new BasicDBObject().append("$min", "$" + name))));

				if (!order.isEmpty()) {
					l1.add(new BasicDBObject().append("$sort", order));
				}

				AggregateIterable<Document> a1 = db1.aggregate(l1);

				List<JSON> l2 = JSON.createList();
				for (Document d : a1) {
					JSON j1 = JSON.fromObject(d);
					Object o = j1.remove("_id");
					if (o instanceof Map) {
						j1.putAll((Map) o);
						l2.add(j1);
					}
				}

				if (log.isDebugEnabled())
					log.debug("min, cost=" + t.past() + ", query=" + l1 + ", result=" + l2);

				return l2;
			}
		} catch (Exception e) {
			log.error("query=" + query + ", order=" + order, e);

		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<JSON> avg(String table, W q, String name, String[] group, String db) {
		TimeStamp t = TimeStamp.create();
		MongoCollection<Document> db1 = null;

		BasicDBObject query = q.query();
		BasicDBObject order = q.order();

		try {
			if (X.isEmpty(table)) {
				log.error("bad table=" + table, new Exception("bad table=" + table));
				return null;
			}

			if (group == null || group.length == 0) {
				log.error("bad group", new Exception("bad group"));
				return null;
			}

			db1 = getCollection(db, table);
			if (db1 != null) {

				List<Bson> l1 = new ArrayList<Bson>();

				if (!query.isEmpty()) {
					l1.add(new BasicDBObject().append("$match", query));
				}

				BasicDBObject g1 = new BasicDBObject();
				for (String s : group) {
					g1.append(s, "$" + s);
				}
				l1.add(new BasicDBObject().append("$group", new BasicDBObject().append("_id", g1).append("avg",
						new BasicDBObject().append("$avg", "$" + name))));

				if (!order.isEmpty()) {
					l1.add(new BasicDBObject().append("$sort", order));
				}

				AggregateIterable<Document> a1 = db1.aggregate(l1);

				List<JSON> l2 = JSON.createList();
				for (Document d : a1) {
					JSON j1 = JSON.fromObject(d);
					Object o = j1.remove("_id");
					if (o instanceof Map) {
						j1.putAll((Map) o);
						l2.add(j1);
					}
				}

				if (log.isDebugEnabled())
					log.debug("avg, cost=" + t.past() + ", query=" + l1 + ", result=" + l2);

				return l2;
			}
		} catch (Exception e) {
			log.error("query=" + query + ", order=" + order, e);

		}

		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T std_deviation(String collection, W q, String name, String db) {

		TimeStamp t = TimeStamp.create();
		MongoCollection<Document> db1 = null;

		BasicDBObject query = q.query();

		try {
			if (X.isEmpty(collection)) {
				log.error("bad collection=" + collection, new Exception("bad collection=" + collection));
				return null;
			}

			db1 = getCollection(db, collection);
			if (db1 != null) {

				List<Bson> l1 = new ArrayList<Bson>();

				if (!query.isEmpty()) {
					l1.add(new BasicDBObject().append("$match", query));
				}

				BasicDBObject group = new BasicDBObject();
				group.append(name, "$" + name);
				l1.add(new BasicDBObject().append("$group", new BasicDBObject().append("_id", "").append("e",
						new BasicDBObject().append("$stdDevPop", "$" + name))));

				AggregateIterable<Document> a1 = db1.aggregate(l1);

				if (log.isDebugEnabled())
					log.debug("std_deviation, cost=" + t.past() + ", query=" + l1 + ", result=" + a1);

				if (a1 != null) {
					return (T) a1.first().get("e");
				}

			}
		} catch (Exception e) {
			log.error("query=" + query, e);

		}

		return null;

	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T median(String table, W q, String name, String db) {
		try {
			long n = this.count(table, q, db);
			if (n % 2 == 1) {
				q.sort(name, 1);
				Beans<Bean> b = this.load(table, new String[] { name }, q, (int) (n / 2), 1, Bean.class, db);
				if (b != null && !b.isEmpty()) {
					return b.get(0).get(name);
				}
			} else if (n > 0) {
				q.sort(name, 1);
				Beans<Bean> b = this.load(table, new String[] { name }, q, (int) (n / 2), 2, Bean.class, db);
				if (b != null && !b.isEmpty()) {
					Object o1 = b.get(0).get(name);
					Object o2 = b.get(1).get(name);

					if (o1 instanceof String) {
						return (T) o1;
					}

					Double d = (X.toDouble(o1) + X.toDouble(o2)) / 2f;
					return (T) d;
				}
			}
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public List<JSON> listDB() {
		return null;
	}

	@Override
	public List<JSON> listOp() {
		return null;
	}

}
