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
package org.giiwa.dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.giiwa.bean.Data;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.sql.SQL;
import org.giiwa.json.JSON;
import org.giiwa.misc.StringFinder;
import org.giiwa.misc.Url;
import org.giiwa.task.Function;
import org.giiwa.task.SysTask;
import org.giiwa.task.Task;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoWriteException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListCollectionsIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.DeleteManyModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * The {@code MongoHelper} Class is base class for all class that database
 * access, it almost includes all methods that need for database <br>
 * all data access MUST be inherited from it
 * 
 */
public final class MongoHelper implements Helper.DBHelper {

	private static Log log = LogFactory.getLog(MongoHelper.class);

	public static MongoHelper inst = null;

//	public boolean isConfigured() {
//
//		return init();
//
//	}

	public boolean init() {

		if (gdb != null) {
			return true;
		}

		Configuration conf = Config.getConf();

		String url = conf.getString("db.url", X.EMPTY);
		String user = conf.getString("db.user", X.EMPTY);
		String passwd = conf.getString("db.passwd", X.EMPTY);
		int conns = conf.getInt("db.conns", 50);
		int timeout = conf.getInt("db.timeout", X.toInt(X.AMINUTE * 10)); // 10 minutes

		if (!X.isEmpty(url) && (url.startsWith("mongodb://") || url.startsWith("jmdb://"))) {

			Url u = Url.create(url);
			String dbname = u.getPath();

			gdb = getDB(url, dbname, user, passwd, conns, timeout);

		}

		return gdb != null;

	}

	/**
	 * Delete the data in mongo by the query.
	 *
	 * @param collection the collection
	 * @param q          the q
	 * @param db         the db
	 * @return the long
	 */
	public int delete(String collection, W q) {

		TimeStamp t = TimeStamp.create();

		int n = -1;
		try {
			MongoCollection<Document> db1 = getCollection(collection);

			DeleteManyModel<Document> dmm = new DeleteManyModel<Document>(q.query());
			List<WriteModel<Document>> req = new ArrayList<WriteModel<Document>>();
			req.add(dmm);
			BulkWriteResult r = db1.bulkWrite(req);
			n = r.getDeletedCount();

//			DeleteOption opt = StandardDeleteOption.OVERRIDE_READ_ONLY;
//			DeleteResult r = db1.deleteMany(q.query(), null);
//			n = X.toInt(r.getDeletedCount());

			if (log.isDebugEnabled()) {
				log.debug("delete, collection=" + collection + ", q=" + q + ", deleted=" + n);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		} finally {
			if (t.pastms() > 1000) {
				log.warn("delete, cost=" + t.past() + ",  collection=" + collection + ", query=" + q + ", n=" + n);
				GLog.applog.warn("sys", "db",
						"count, cost=" + t.past() + ",  collection=" + collection + ", query=" + q + ", n=" + n);
			} else if (log.isDebugEnabled()) {
				log.debug("delete, cost=" + t.past() + ",  collection=" + collection + ", query=" + q + ", n=" + n);
			}

			Helper.Stat.write(collection, t.pastms());

		}

		return n;
	}

	private MongoDatabase gdb = null;
	private MongoDatabase admin = null;

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
	public MongoDatabase getDB() {
		if (gdb != null)
			return gdb;

		Configuration conf = Config.getConf();

		String url = conf.getString("db.url", X.EMPTY);
		String user = conf.getString("db.user", X.EMPTY);
		String passwd = conf.getString("db.passwd", X.EMPTY);
		int conns = conf.getInt("db.conns", 50);
		int timeout = conf.getInt("db.timeout", X.toInt(X.AMINUTE * 10)); // 10 minutes

		if (!X.isEmpty(url) && url.startsWith("mongodb://")) {

			Url u = Url.create(url);
			String dbname = u.getPath();

			gdb = getDB(url, dbname, user, passwd, conns, timeout);

		}

		return gdb;
	}

	private MongoDatabase getAdmin() {

		if (admin != null)
			return admin;

		Configuration conf = Config.getConf();

		String url = conf.getString("db.url", X.EMPTY);
		String user = conf.getString("db.user", X.EMPTY);
		String passwd = conf.getString("db.passwd", X.EMPTY);
		int conns = conf.getInt("db.conns", 50);
		int timeout = conf.getInt("db.timeout", X.toInt(X.AMINUTE * 10)); // 10 minutes

		if (!X.isEmpty(url) && url.startsWith("mongodb://")) {

			admin = getDB(url, "admin", user, passwd, conns, timeout);

		}

		return admin;
	}

	private MongoClient client = null;

	private MongoDatabase getDB(String url, String db, String user, String passwd, int conns, int timeout) {

		url = url.trim();
		db = db.trim();

		if (X.isEmpty(user)) {
			Url u = Url.create(url);
			user = u.get("username");
			passwd = u.get("passwd");
		}
		int i = url.indexOf("?");
		if (i > 0) {
			url = url.substring(0, i);
		}
		log.warn("url=" + url + ", db=" + db + ", user=" + user + ", passwd=" + passwd);
		if (url.startsWith("jmdb://")) {
			url = "mongodb://" + url.substring(7);
		}

		MongoClientSettings.Builder setting = MongoClientSettings.builder();
		setting.applyConnectionString(new ConnectionString(url));
		setting.applyToSocketSettings(b -> {
			b.connectTimeout(timeout, TimeUnit.SECONDS);
			b.readTimeout(timeout, TimeUnit.SECONDS);
		});
		setting.applyToConnectionPoolSettings(b -> {
			b.maxConnecting(conns);
		}).retryWrites(true);

		if (!X.isEmpty(user)) {
			setting.credential(MongoCredential.createCredential(user, db, passwd.toCharArray()));
		}

		client = MongoClients.create(setting.build());
		return client.getDatabase(db);

	}

	/**
	 * Gets the collection using the database connection
	 * 
	 * <br>
	 * the configuration including:
	 * 
	 * @param database   the database
	 * @param collection the collection
	 * @return DBCollection
	 */
	public MongoCollection<Document> getCollection(String collection) {

		MongoDatabase g = getDB();

		MongoCollection<Document> d = null;

		if (g != null) {
			d = g.getCollection(collection);
		}

		if (d == null) {
			if (log.isErrorEnabled())
				log.error(" was miss configured, please access http://[host:port]/setup to configure");
		}

		return d;
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
			return load(collection, query, clazz.getDeclaredConstructor().newInstance());
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

		TimeStamp t = TimeStamp.create();

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
			log.error(e.getMessage(), e);

		} finally {
			if (t.pastms() > 1000) {
				log.warn("load, cost=" + t.past() + ",  collection=" + collection + ", query=" + query);
				GLog.applog.warn("sys", "db",
						"load, cost=" + t.past() + ",  collection=" + collection + ", query=" + query);
			} else if (log.isDebugEnabled()) {
				log.debug("load, cost=" + t.past() + ",  collection=" + collection + ", query=" + query);
			}

			Helper.Stat.read(collection, t.pastms());

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
	public <T extends Bean> T load(String collection, Bson query, Bson order, T b, boolean trace) {
		return _load(collection, query, order, b, trace, null);
	}

	private <T extends Bean> T _load(String collection, Bson query, Bson order, T b, boolean trace, String fields) {

		TimeStamp t = TimeStamp.create();
		try {
			MongoCollection<Document> db1 = getCollection(collection);
			if (db1 != null) {

				FindIterable<Document> d = db1.find(query);
				if (order != null) {
					d.sort(order);
				}

				if (d != null) {

					Document d1 = d.first();

					if (trace) {
						log.warn("trace, load - cost=" + t.past() + ", collection=" + collection + ", query=" + query
								+ ", order=" + order + ", d=" + (d1 != null ? 1 : 0));
					} else if (log.isDebugEnabled()) {
						log.debug("load - cost=" + t.past() + ", collection=" + collection + ", query=" + query
								+ ", order=" + order + ", d=" + (d1 != null ? 1 : 0));
					}

					String[] ss = X.split(fields, ",");
					if (d1 != null) {
						b.load(d1, ss);
						return b;
					}

					// MongoCursor<Document> it = d.iterator();
					// if (it.hasNext()) {
					// b.load(it.next());
					// return b;
					// }
				} else {
					if (trace) {
						log.warn("trace, load - cost=" + t.past() + ", collection=" + collection + ", query=" + query
								+ ", order=" + order + ", result=" + null);
					} else if (log.isDebugEnabled()) {
						log.debug("load - cost=" + t.past() + ", collection=" + collection + ", query=" + query
								+ ", order=" + order + ", result=" + null);
					}
				}
			}
		} catch (Exception e) {
			// bad connection ? close the it ?
			log.error("cost=" + t.past() + ", query=" + query + ", order=" + order, e);
		} finally {

			if (t.pastms() > 1000) {
				log.warn("load, cost=" + t.past() + ",  collection=" + collection + ", query=" + query + ", order="
						+ order);
				GLog.applog.warn(MongoHelper.class, "db", "load, cost=" + t.past() + ",  collection=" + collection
						+ ", query=" + query + ", order=" + order);
			} else if (log.isDebugEnabled()) {
				log.debug("load, cost=" + t.past() + ",  collection=" + collection + ", query=" + query + ", order="
						+ order);
			}

			Helper.Stat.read(collection, t.pastms());

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
			return load(collection, query, order, obj, false);
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
	@Override
	public <T extends Bean> Beans<T> load(String collection, W q, int offset, int limit, final Class<T> clazz)
			throws SQLException {

		TimeStamp t = TimeStamp.create();
		MongoCollection<Document> db1 = null;
		FindIterable<Document> cur = null;
		Bson query = q.query();
		Bson orderBy = q.order();

		try {

			db1 = getCollection(collection);
			if (db1 != null) {
				cur = db1.find(query);

				if (orderBy != null) {
					cur.sort(orderBy);
				}

				final Beans<T> bs = new Beans<T>();
				bs.q = q;
				bs.table = collection;

				// ignore this as big performance
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

				String[] ss = X.split(q.fields(), ",");

				while (it.hasNext() && limit > 0) {
					// log.debug("hasnext=" + t.past() + "ms, count=" + bs.total);
					Document d = it.next();
					// log.debug("next=" + t.past() +"ms, count=" + bs.total);
					if (d != null) {
						T b = clazz.getDeclaredConstructor().newInstance();
						if (ss == null || ss.length == 0) {
							b.load(d);
							b._rowid = rowid++;
						} else {
							b.load(d, ss);
						}
						bs.add(b);
						limit--;
					}
				}

				if (t.pastms() > 10000) {
					log.warn("load - cost=" + t.past() + ", collection=" + collection + ", query=" + query + ", order="
							+ orderBy + ", result=" + bs.size() + ", offset=" + offset);
				} else if (log.isDebugEnabled()) {
					log.debug("load - cost=" + t.past() + ", collection=" + collection + ", query=" + query + ", order="
							+ orderBy + ", offset=" + offset + ", limit=" + limit + ", result=" + bs.size());
				}

				bs.setCost(t.pastms());
				return bs;
			}
		} catch (Exception e) {
			log.error("query=" + query + ", order=" + orderBy, e);
			throw new SQLException(e.getMessage() + ", sql=" + q, e);
		} finally {

			if (t.pastms() > 1000) {
				log.warn("load, cost=" + t.past() + ",  collection=" + collection + ", query=" + query + ", order="
						+ orderBy);
				GLog.applog.warn("sys", "db", "load, cost=" + t.past() + ",  collection=" + collection + ", query="
						+ query + ", order=" + orderBy);
			} else if (log.isDebugEnabled()) {
				log.debug("load, cost=" + t.past() + ",  collection=" + collection + ", query=" + query + ", order="
						+ orderBy);
			}

			Helper.Stat.read(collection, t.pastms());

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
				T obj = t.getDeclaredConstructor().newInstance();
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
				T obj = t.getDeclaredConstructor().newInstance();
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
	@Override
	public <T extends Bean> T load(String collection, W q, Class<T> t) {
		return load(collection, q, t, false);
	}

	@Override
	public <T extends Bean> T load(String collection, W q, Class<T> t, boolean trace) {
		try {
			T obj = t.getDeclaredConstructor().newInstance();
			return _load(collection, q.query(), q.order(), obj, trace, q.fields);
		} catch (Exception e) {
			log.error(q.toString(), e);

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
	public Document load(String collection, Bson query) {
		/**
		 * create the sql statement
		 */
		try {
			MongoCollection<Document> c = getCollection(collection);
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
	 * @return int
	 */
	public int insertTable(String collection, V v) {

		if (v == null || v.isEmpty())
			return 0;

		long t1 = Global.now();
		v.force(X.CREATED, t1).force(X.UPDATED, t1);
		v.append("_node", Global.id());

		// TODO
//		v.append("_node1", Local.label());
//
		TimeStamp t = TimeStamp.create();

		try {
			MongoCollection<Document> c = getCollection(collection);
			if (c != null) {
				Document d = new Document();

				Object id = v.value(X.ID);
				if (!X.isEmpty(id)) {
					v.append("_id", v.value(X.ID));
				} else {
					v.append("_id", UUID.randomUUID().toString());
				}

				for (String name : v.names()) {
					Object v1 = v.value(name);
					if (v1 != null) {
						if (v1 == V.ignore) {
							continue;
						} else if (v1 instanceof UUID) {
							v1 = v1.toString();
						} else if (v1 instanceof ObjectId) {
							v1 = ((ObjectId) v1).toHexString();
						} else if (v1 instanceof Date) {
							v1 = ((Date) v1).getTime();
						}
						d.append(name, v1);
					}
				}

				try {

					c.insertOne(d);

					if (log.isDebugEnabled()) {
						log.debug("inserted collection=" + collection + ", d=" + d);
					}
					return 1;
				} catch (Exception e) {
					// log.error(d.toString(), e);
					// error

				}
			}
		} finally {

			Helper.Stat.write(collection, t.pastms());

			if (t.pastms() > 1000 && log.isWarnEnabled()) {
				log.warn("cost=" + t.past() + ", insert [" + collection + "], v=" + v);
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
	final public int updateTable(String collection, W q, V v) {

		if (v == null || v.isEmpty())
			return 0;

		Object o = v.value(X.UPDATED);
		if (o != V.ignore) {
			v.force(X.UPDATED, Global.now());
			if (log.isDebugEnabled()) {
				log.debug("force updated=" + v.value(X.UPDATED));
			}
		} else if (log.isDebugEnabled()) {
			log.debug("updated=" + o + ", ==ignore");
		}

		// TODO, not allow change the created
		o = v.value(X.CREATED);
		if (o != V.ignore) {
			if (X.toLong(o) == 0) {
				v.remove(X.CREATED);
			}
		} else {
			v.remove(X.CREATED);
		}

		TimeStamp t = TimeStamp.create();
		Document set = new Document();

		// int len = v.size();
		for (String name : v.names()) {
			Object v1 = v.value(name);
			if (v1 == V.ignore) {
				continue;
			}

			if (v1 instanceof UUID) {
				v1 = v1.toString();
			} else if (v1 instanceof Date) {
				v1 = ((Date) v1).getTime();
			}
			set.append(name, v1);
		}

		try {
			// log.debug("data=" + d);
			MongoCollection<Document> c = getCollection(collection);
			Document d = new Document();
			if (!set.isEmpty()) {
				d.append("$set", set);
			}
			UpdateResult r = null;
//			if (v.value("_id") != null) {
//				r = c.updateMany(q.query(), d, new UpdateOptions().upsert(true));
//			} else {
			r = c.updateMany(q.query(), d);
//			}

			if (log.isDebugEnabled()) {
				log.debug("updated, cost=" + t.past() + ", collection=" + collection + ", query=" + q + ", d=" + set
						+ ", n=" + r.getModifiedCount() + ",result=" + r);
			}

			// r.getN();
			// r.getField("nModified");
			return (int) r.getModifiedCount();
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		} finally {

			Helper.Stat.write(collection, t.pastms());

			if (t.pastms() > 1000 && log.isWarnEnabled()) {
				// to fix bug
				// UTF16 String size is 1093649910, should be less than 1073741823
				// at org.giiwa.dao.Helper$V.toString(Helper.java:383)
				log.warn("cost=" + t.past() + ", update [" + collection + "], q=" + q + ", v=" + v.names());
			}

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
	public boolean exists(String collection, W q) {
		TimeStamp t1 = TimeStamp.create();
		boolean b = false;
		try {
			MongoCollection<Document> c = getCollection(collection);
			if (c != null) {
				b = c.find(q.query()).first() != null;
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (log.isDebugEnabled())
				log.debug(
						"exists cost=" + t1.past() + ",  collection=" + collection + ", query=" + q + ", result=" + b);
			Helper.Stat.read(collection, t1.pastms());
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
	public Set<String> getCollections() {

		MongoDatabase d = getDB();
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
			MongoCollection<Document> c = getCollection(collection);
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
	public List<?> distinct(String collection, String key, W q) {

		List<?> l1 = null;
		TimeStamp t = TimeStamp.create();
		try {

			MongoDatabase g = getDB();
			Document d = g.runCommand(
					new BasicDBObject("distinct", collection).append("key", key).append("query", q.query()));
			if (d.containsKey("values")) {
				l1 = (List<?>) (d.get("values"));
			}

		} catch (Exception e) {
			log.error(q.query(), e);

		} finally {
			if (t.pastms() > 1000) {
				log.warn("distinct, cost=" + t.past() + ",  collection=" + collection + ", query=" + q + ", n="
						+ (l1 == null ? "null" : l1.size()));
				GLog.applog.warn("sys", "db", "distinct, cost=" + t.past() + ",  collection=" + collection + ", query="
						+ q + ", n=" + (l1 == null ? "null" : l1.size()));
			} else if (log.isDebugEnabled()) {
				log.debug("distinct, cost=" + t.past() + ",  collection=" + collection + ", query=" + q + ", n="
						+ (l1 == null ? "null" : l1.size()));
			}
			Helper.Stat.read(collection, t.pastms());

		}
		return l1;
	}

	/**
	 * count the number by the query.
	 *
	 * @param collection the collection name
	 * @param q          the query and order
	 * @param db         the db
	 * @return long
	 */
	public long count(String collection, W q) {
		return count(collection, q, "*");
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
				Set<String> ss = getCollections();
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

		MongoCollection<Document> d1 = getCollection(tablename);
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
			inst.delete(tablename, W.create().and(X.ID, jo.get(X.ID)));
			inst.insertTable(tablename, v);
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
	public int inc(String table, W q, String name, int n, V v) {

		TimeStamp t = TimeStamp.create();

		Document d = new Document();

		try {
			d.put(name, n);
			MongoCollection<Document> c = getCollection(table);
			Document d2 = new Document("$inc", d);

			Document d1 = null;
			if (v != null && !v.isEmpty()) {
				d1 = new Document();
				for (String s : v.names()) {
					Object v1 = v.value(s);
					d1.append(s, v1);
				}
				if (!d1.isEmpty()) {
					d2.append("$set", d1);
				}
			}

			UpdateResult r = null;

			try {
				r = c.updateMany(q.query(), d2);
			} catch (MongoWriteException e) {
				// non-numeric type
				d2 = new Document();
				d1 = new Document();
				if (v != null && !v.isEmpty()) {
					for (String s : v.names()) {
						Object v1 = v.value(s);
						d1.append(s, v1);
					}
				}
				d1.append(name, n);
				d2.append("$set", d1);
				r = c.updateMany(q.query(), d2);

			}

			if (log.isDebugEnabled())
				log.debug("updated collection=" + table + ", query=" + q + ", d2=" + d2 + ", n=" + r.getModifiedCount()
						+ ",result=" + r);

			return (int) r.getModifiedCount();
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		} finally {
			if (t.pastms() > 1000) {
				log.warn("inc, cost=" + t.past() + ",  collection=" + table + ", query=" + q);
				GLog.applog.warn("sys", "db", "inc, cost=" + t.past() + ",  collection=" + table + ", query=" + q);
			} else if (log.isDebugEnabled()) {
				log.debug("inc, cost=" + t.past() + ",  collection=" + table + ", query=" + q);
			}

			Helper.Stat.write(table, t.pastms());

		}
		return 0;
	}

	public void createIndex(String table, LinkedHashMap<String, Object> ss, boolean unique) {

		MongoCollection<Document> c = getCollection(table);
		BasicDBObject q = new BasicDBObject();
		int n = 0;
		for (String s : ss.keySet()) {
			Object i = ss.get(s);
			if (X.isSame(i, 2)) {
				q.append(s, "2d");
			} else {
				q.append(s, i);
			}
			n++;
			if (n > 5) {
				break;
			}
		}

		IndexOptions opt = new IndexOptions();
		opt.background(true);
		opt.unique(unique);

		try {
			c.createIndex(q, opt);
		} catch (Exception e) {
			// ingore, dont care
//			if (!X.isCauseBy(e, ".*already exists.*")) {
//				log.error(q, e);
//			}
//			GLog.applog.error("db", "optimaize", "table=" + table + ", key=" + ss, e);
		}
	}

	@Override
	public void dropIndex(String table, String name) {
		MongoCollection<Document> c = getCollection(table);
		c.dropIndex(name);
	}

	@Override
	public List<Map<String, Object>> getIndexes(String table) {

		List<Map<String, Object>> l1 = new ArrayList<Map<String, Object>>();

		MongoCollection<Document> c = getCollection(table);
		MongoCursor<Document> i1 = c.listIndexes().iterator();
		while (i1.hasNext()) {
			Document d1 = i1.next();
			l1.add(d1);
		}

		return l1;
	}

	@Override
	public int insertTable(String collection, List<V> values) {

		if (X.isEmpty(values))
			return 0;

		TimeStamp t = TimeStamp.create();

		try {
			MongoCollection<Document> c = getCollection(collection);
			if (c != null) {

				List<Document> l1 = new ArrayList<Document>();

				for (V v : values) {

					if (v == null || v.isEmpty())
						continue;

					long t1 = Global.now();
					v.force(X.CREATED, t1).force(X.UPDATED, t1);
					v.force("_node", Global.id());

					Document d = new Document();

					Object id = v.value(X.ID);
					if (!X.isEmpty(id)) {
						v.append("_id", v.value(X.ID));
					} else {
						v.append("_id", UUID.randomUUID().toString());
					}

					for (String name : v.names()) {
						Object v1 = v.value(name);
						if (v1 != null) {
							if (v1 instanceof UUID) {
								v1 = v1.toString();
							} else if (v1 instanceof Date) {
								v1 = ((Date) v1).getTime();
							}
							d.append(name, v1);
						}
					}

					l1.add(d);
				}

				try {

					c.insertMany(l1);

					if (log.isDebugEnabled())
						log.debug("inserted collection=" + collection + ", cost=" + t.past() + ", size=" + l1.size());

					return l1.size();
				} catch (Exception e) {
					log.error("cost=" + t.past(), e);

				}
			}

		} finally {
			if (t.pastms() > 1000) {
				log.warn("insert, cost=" + t.past() + ",  collection=" + collection);
				// to avoid dead-loop, do not record in db
//				GLog.applog.warn("sys", "db", "insert, cost=" + t.past() + ",  collection=" + collection);
			} else if (log.isDebugEnabled()) {
				log.debug("insert, cost=" + t.past() + ",  collection=" + collection);
			}

			Helper.Stat.write(collection, t.pastms());

		}
		return 0;
	}

	@Override
	public List<JSON> listTables(String tablename, int n) {

		MongoDatabase g = getDB();

		List<JSON> list = new ArrayList<JSON>();
		if (g != null) {
			ListCollectionsIterable<Document> it = g.listCollections();
			for (Document d : it) {
				JSON j = JSON.create();
				String name = d.getString("name");
				if (X.isEmpty(tablename) || name.matches(tablename)) {
					j.put("name", name);
					j.putAll(d);
					list.add(j);
					if (n > 0 && list.size() > n) {
						break;
					}
				}
			}
		}

		Collections.sort(list, new Comparator<JSON>() {

			@Override
			public int compare(JSON o1, JSON o2) {
				return o1.getString("name").compareToIgnoreCase(o2.getString("name"));
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
	public <T extends Bean> boolean stream(String table, W q, long offset, Function<T, Boolean> func, Class<T> t1) {

		MongoCollection<Document> db1 = null;
		FindIterable<Document> cur = null;
		Bson query = q.query();
		Bson orderBy = q.order();

//		try {

		db1 = getCollection(table);
		if (db1 != null) {
			cur = db1.find(query);
			cur.noCursorTimeout(true);

			if (orderBy != null) {
				cur.sort(orderBy);
			}
			if (offset > 0) {
				cur.skip((int) offset);
			}

			MongoCursor<Document> it = cur.iterator();
			try {
				while (it.hasNext()) {

					try {
						T d = t1.getDeclaredConstructor().newInstance();
						d.load(it.next());
						if (!func.apply(d)) {
							return false;
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
						return false;
					}

				}

				return true;

			} finally {
				it.close();
			}
		}
		return false;
	}

	public static MongoHelper create(String url, String user, String passwd) throws SQLException {
		return create(url, user, passwd, 10, 30000);
	}

	/**
	 * 
	 * @param url, "mongodb://host:port/dbname"
	 * @return the MongoHelper
	 * @throws SQLException
	 */
	public static MongoHelper create(String url, String user, String passwd, int conns, int timeout)
			throws SQLException {
		//
		int i = url.lastIndexOf("/");
		if (i < 0) {
			throw new SQLException("dbname missed, mongdb://[ip]:[port],[ip2]:[port]/dbname");
		}
		String dbname = url.substring(i + 1);
		i = dbname.indexOf("?");
		if (i > 0) {
			dbname = dbname.substring(0, i);
		}
		MongoHelper h = new MongoHelper();
		MongoDatabase g = h.getDB(url, dbname, user, passwd, conns, timeout);
		h.gdb = g;
		return h;
	}

	@Override
	public List<JSON> getMetaData(String tablename) {
		MongoCollection<Document> c = getCollection(tablename);
		List<JSON> list = new ArrayList<JSON>();
		list.add(JSON.create().append("name", tablename).append("size", c.estimatedDocumentCount()));
		return list;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T sum(String collection, W q, String name) {

		TimeStamp t = TimeStamp.create();
		Object n = 0;
		try {

			MongoCollection<Document> c = getCollection(collection);
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
			if (t.pastms() > 1000) {
				log.warn("sum, cost=" + t.past() + ",  collection=" + collection + ", query=" + q + ", n=" + n);
				GLog.applog.warn("sys", "db",
						"sum, cost=" + t.past() + ",  collection=" + collection + ", query=" + q + ", n=" + n);
			} else if (log.isDebugEnabled()) {
				log.debug("sum, cost=" + t.past() + ",  collection=" + collection + ", query=" + q + ", n=" + n);
			}

			Helper.Stat.read(collection, t.pastms());

		}
		return (T) n;
	}

	@SuppressWarnings("unchecked")
	public <T> T avg(String collection, W q, String name) {

		TimeStamp t = TimeStamp.create();
		Object n = 0;
		try {

			MongoCollection<Document> c = getCollection(collection);
			if (c != null) {
				BasicDBObject match = new BasicDBObject("$match", q == null ? new BasicDBObject() : q.query());
				BasicDBObject group = new BasicDBObject();
				group.append("$group", new BasicDBObject().append("_id", name).append(name,
						new BasicDBObject().append("$avg", "$" + name)));

				List<BasicDBObject> l1 = Arrays.asList(match, group);

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
			if (t.pastms() > 1000) {
				log.warn("avg, cost=" + t.past() + ",  collection=" + collection + ", query=" + q + ", n=" + n);
				GLog.applog.warn("sys", "db",
						"avg, cost=" + t.past() + ",  collection=" + collection + ", query=" + q + ", n=" + n);
			} else if (log.isDebugEnabled()) {
				log.debug("avg, cost=" + t.past() + ",  collection=" + collection + ", query=" + q + ", n=" + n);
			}

			Helper.Stat.read(collection, t.pastms());

		}
		return (T) n;
	}

	@Override
	public void repair() {

		MongoDatabase g = getDB();
		g.runCommand(new BasicDBObject().append("repairDatabase", 1));

	}

	@Override
	public void drop(String table) {
		MongoCollection<Document> c = getCollection(table);
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
	@Override
	public List<JSON> count(String collection, W q, String[] name, int n) {
		return count(collection, q, "*", name, n);
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
	public List<JSON> aggregate(String table, String[] func, W q, String[] group) {

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

			db1 = getCollection(table);
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

		} finally {
			Helper.Stat.read(table, t.pastms());
		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<JSON> sum(String table, W q, String name, String[] group) {

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

			db1 = getCollection(table);
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

		} finally {

			if (t.pastms() > 1000) {
				log.warn("sum, cost=" + t.past() + ",  collection=" + table + ", query=" + q);
				GLog.applog.warn("sys", "db", "sum, cost=" + t.past() + ",  collection=" + table + ", query=" + q);
			} else if (log.isDebugEnabled()) {
				log.debug("sum, cost=" + t.past() + ",  collection=" + table + ", query=" + q);
			}

			Helper.Stat.read(table, t.pastms());

		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<JSON> avg(String table, W q, String name, String[] group) {

		TimeStamp t = TimeStamp.create();
		MongoCollection<Document> db1 = null;

		List<JSON> l2 = JSON.createList();

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

			db1 = getCollection(table);
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
		} finally {
			if (t.pastms() > 1000) {
				log.warn("avg, cost=" + t.past() + ",  collection=" + table + ", query=" + q + ", n="
						+ (l2 == null ? "null" : l2.size()));
				GLog.applog.warn("sys", "db", "avg, cost=" + t.past() + ",  collection=" + table + ", query=" + q
						+ ", n=" + (l2 == null ? "null" : l2.size()));
			} else if (log.isDebugEnabled()) {
				log.debug("avg, cost=" + t.past() + ",  collection=" + table + ", query=" + q + ", n="
						+ (l2 == null ? "null" : l2.size()));
			}

			Helper.Stat.read(table, t.pastms());

		}

		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T std_deviation(String collection, W q, String name) {

		TimeStamp t = TimeStamp.create();
		MongoCollection<Document> db1 = null;

		BasicDBObject query = q.query();

		try {
			if (X.isEmpty(collection)) {
				log.error("bad collection=" + collection, new Exception("bad collection=" + collection));
				return null;
			}

			db1 = getCollection(collection);
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
					Document doc = a1.first();
					if (doc != null) {
						return (T) doc.get("e");
					}
				}

			}
		} catch (Exception e) {
			log.error("query=" + query, e);

		} finally {

			if (t.pastms() > 1000) {
				log.warn("std_deviation, cost=" + t.past() + ",  collection=" + collection + ", query=" + query);
				GLog.applog.warn("sys", "db",
						"std_deviation, cost=" + t.past() + ",  collection=" + collection + ", query=" + query);
			} else if (log.isDebugEnabled()) {
				log.debug("std_deviation, cost=" + t.past() + ",  collection=" + collection + ", query=" + query);
			}

		}

		return null;

	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T median(String table, W q, String name) {
		try {
			long n = this.count(table, q);
			if (n % 2 == 1) {
				q.sort(name, 1);
				Beans<Bean> b = this.load(table, q, (int) (n / 2), 1, Bean.class);
				if (b != null && !b.isEmpty()) {
					return (T) b.get(0).get(name);
				}
			} else if (n > 0) {
				q.sort(name, 1);
				Beans<Bean> b = this.load(table, q, (int) (n / 2), 2, Bean.class);
				if (b != null && !b.isEmpty()) {
					if (b.size() == 1) {
						return (T) b.get(0).get(name);
					}
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

		try {
			MongoDatabase g = getAdmin();
			if (g != null) {
				Document d1 = g.runCommand(new BasicDBObject("currentOp", true).append("$all", true));

				JSON j1 = JSON.fromObject(d1);
				List<JSON> l1 = j1.getList("inprog");

				List<JSON> l2 = X.asList(l1, e1 -> {
					JSON e = (JSON) e1;
					JSON d = JSON.create();

					if (e.containsKey("command") && !X.isIn(e.get("op"), "none")) {
						d.put("client", e.get("client"));
						d.put("opid", e.get("opid"));
						d.put("cost", e.get("secs_running"));
						d.put("op", e.get("op"));
						d.put("command", e.get("command").toString());
						d.put("plan", e.get("planSummary"));
						d.put("locks", e.get("locks").toString());
						d.put("table", e.get("ns"));
						d.put("waitingForLock", e.get("waitingForLock"));

						return d;
					}
					return null;
				});

				Collections.sort(l2, new Comparator<JSON>() {

					@Override
					public int compare(JSON o1, JSON o2) {
						long c1 = o1.getLong("cost");
						long c2 = o2.getLong("cost");

						if (c1 > c2) {
							return -1;
						} else if (c1 < c2) {
							return 1;
						}
						return 0;
					}

				});
				return l2;
			}
			return null;
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void killOp(Object id) {

		try {
			MongoDatabase g = getAdmin();
			if (g != null) {
				g.runCommand(new BasicDBObject("killOp", 1).append("op", X.toLong(id)));
			}
		} catch (Exception e) {
			throw e;
		}
	}

	public JSON stats(String table) {

		try {
			MongoDatabase g = getDB();
			if (X.isEmpty(table)) {
				Document d1 = g.runCommand(new BasicDBObject("dbStats", 1).append("scale", 1024 * 1024 * 1024));

//				float M = (1024 * 1024 * 1024);
				JSON j1 = JSON.fromObject(d1);
//				j1.put("storageSize", j1.getLong("storageSize") / M);
//				j1.put("indexSize", j1.getLong("indexSize") / M);
//				j1.put("totalSize", j1.getLong("totalSize") / M);
//				j1.put("fsTotalSize", j1.getLong("fsTotalSize") / M);
				return j1;
			} else {
				Document d1 = g.runCommand(new BasicDBObject("collStats", table));
				JSON j1 = JSON.fromObject(d1);
				return j1;
			}
		} catch (Exception e) {
			throw e;
		}

	}

	@Override
	public long size(String table) {

		JSON j1 = stats(table);
		return j1.getLong("totalSize");

	}

	@Override
	public long count(String table, W q, String name) {

		TimeStamp t1 = TimeStamp.create();
		long n = 0;
		BasicDBObject q1 = q.query();

		try {

			MongoCollection<Document> c = getCollection(table);
			if (c != null) {
				if (q1.isEmpty()) {
					n = c.estimatedDocumentCount();
				} else {
					n = c.countDocuments(q1);// count(q1);
//					FindIterable<Document> fi = c.find(q1);
//					fi.skip(Integer.MAX_VALUE);
//					n = c.countDocuments(q1);
				}
			}
		} catch (Exception e) {
			log.error("q=" + q, e);
			throw e;
		} finally {
			if (t1.pastms() > 1000) {
				log.warn("count, cost=" + t1.past() + ",  collection=" + table + ", query=" + q1 + ", n=" + n);
				GLog.applog.warn("sys", "db",
						"count, cost=" + t1.past() + ",  collection=" + table + ", query=" + q1 + ", n=" + n);
			} else if (log.isDebugEnabled()) {
				log.debug("count, cost=" + t1.past() + ",  collection=" + table + ", query=" + q1 + ", n=" + n);
			}

			Helper.Stat.read(table, t1.pastms());

		}
		return n;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<JSON> count(String table, W q, String name, String[] group, int n) {

		TimeStamp t = TimeStamp.create();
		MongoCollection<Document> db1 = null;

		BasicDBObject query = q.query();
		BasicDBObject order = q.order();

		try {
			if (X.isEmpty(table)) {
				log.error("bad collection=" + table, new Exception("bad collection=" + table));
				return null;
			}

			if (group == null || group.length == 0) {
				log.error("bad name", new Exception("bad name"));
				return null;
			}

			db1 = getCollection(table);
			if (db1 != null) {

				List<Bson> l1 = new ArrayList<Bson>();

				if (!query.isEmpty()) {
					l1.add(new BasicDBObject().append("$match", query));
				}

				BasicDBObject group1 = new BasicDBObject();
				for (String s : group) {
					group1.append(s, "$" + s);
				}
				l1.add(new BasicDBObject().append("$group", new BasicDBObject().append("_id", group1).append("count",
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
					if (l2.size() >= n)
						break;
				}

				if (log.isDebugEnabled())
					log.debug("count " + table + ", cost=" + t.past() + ", query=" + l1 + ", result=" + l2);

				return l2;
			}
		} catch (Exception e) {
			log.error("query=" + query + ", order=" + order, e);

		} finally {
			if (t.pastms() > 1000) {
				log.warn("count, cost=" + t.past() + ",  collection=" + table + ", query=" + q + ", n=" + n);
				GLog.applog.warn("sys", "db",
						"count, cost=" + t.past() + ",  collection=" + table + ", query=" + q + ", n=" + n);
			} else if (log.isDebugEnabled()) {
				log.debug("count, cost=" + t.past() + ",  collection=" + table + ", query=" + q + ", n=" + n);
			}

			Helper.Stat.read(table, t.pastms());

		}

		return null;

	}

	@Override
	public void repair(String table) {

		MongoDatabase g = getDB();
		// g.runCommand(new BasicDBObject().append("validate", table).append("compact",
		// table).append("force", true));
		g.runCommand(new BasicDBObject().append("compact", table).append("force", true));

	}

	class OpChecker extends SysTask {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public String getName() {
			return "gi.db.checker";
		}

		@Override
		public void onExecute() {

			MongoDatabase db = getDB();
			BasicDBObject cmd = new BasicDBObject();
			db.runCommand(cmd);

			// update, query, insert, command,
		}

	}

	@Override
	public JSON status() {
		MongoDatabase g = getDB();
		Document d1 = g.runCommand(new BasicDBObject("serverStatus", 1));

		JSON j2 = JSON.fromObject(d1);

		JSON j1 = JSON.create();
		if (j2 != null) {

//			j1.append("pid", j2.getString("pid"));
			j1.append("conns", j2.getLong("connections.current"));
			j1.append("reads", j2.getLong("opcounters.query") + j2.getLong("opcounters.getmore"));
			j1.append("writes", j2.getLong("opcounters.insert") + j2.getLong("opcounters.update")
					+ j2.getLong("opcounters.delete"));
			j1.append("cursor", j2.getLong("cursor.open cursor count"));
			j1.append("netin", j2.getLong("network.bytesIn"));
			j1.append("netout", j2.getLong("network.bytesOut"));

//			j1.append("insert", j2.getLong("opcounters.insert"));
//			j1.append("query", j2.getLong("opcounters.query"));
//			j1.append("update", j2.getLong("opcounters.update"));
//			j1.append("delete", j2.getLong("opcounters.delete"));
//			j1.append("getmore", j2.getLong("opcounters.getmore"));
//			j1.append("command", j2.getLong("opcounters.command"));

		}
		return j1;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T max(String collection, W q, String name) {

		q = q.copy().clearSort();

		Data e = this.load(collection, q.sort(name, -1), Data.class, false);
		if (e != null) {
			return (T) e.get(name);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T min(String collection, W q, String name) {

		q = q.copy().clearSort();

		Data e = this.load(collection, q.sort(name), Data.class, false);
		if (e != null) {
			return (T) e.get(name);
		}
		return null;

	}

	private transient Optimizer _optimizer;

	@Override
	public Optimizer getOptimizer() {
		if (_optimizer == null) {
			_optimizer = new Optimizer(this);
		}
		return _optimizer;
	}

	@Override
	public boolean distributed(String table, String key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getTime() {
		MongoDatabase g = getDB();
		g.runCommand(null);
		return 0;
	}

	@Override
	public int inc(String table, W q, JSON incvalue, V v) throws SQLException {

		TimeStamp t = TimeStamp.create();

		Document d = new Document();

		try {
			for (String name : incvalue.keySet()) {
				d.put(name, incvalue.getInt(name));
			}
			MongoCollection<Document> c = getCollection(table);
			Document d2 = new Document("$inc", d);

			Document d1 = null;
			if (v != null && !v.isEmpty()) {
				d1 = new Document();
				for (String s : v.names()) {
					Object v1 = v.value(s);
					d1.append(s, v1);
				}
				if (!d1.isEmpty()) {
					d2.append("$set", d1);
				}
			}

			UpdateResult r = c.updateMany(q.query(), d2);

			if (log.isDebugEnabled())
				log.debug("updated collection=" + table + ", query=" + q + ", d2=" + d2 + ", n=" + r.getModifiedCount()
						+ ",result=" + r);

			return (int) r.getModifiedCount();
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		} finally {
			if (t.pastms() > 1000) {
				log.warn("inc, cost=" + t.past() + ",  collection=" + table + ", query=" + q);
				GLog.applog.warn("sys", "db", "inc, cost=" + t.past() + ",  collection=" + table + ", query=" + q);
			} else if (log.isDebugEnabled()) {
				log.debug("inc, cost=" + t.past() + ",  collection=" + table + ", query=" + q);
			}

			Helper.Stat.write(table, t.pastms());

		}
		return 0;

	}

	@Override
	public void copy(String src, String dest, W filter) throws SQLException {

		Task.schedule(t -> {
			try {

				Bson query = filter == null ? null : filter.query();
				MongoCollection<Document> s = getCollection(src);
				MongoCollection<Document> d = getCollection(dest);
				FindIterable<Document> findIterable = query == null ? s.find() : s.find(query);
				MongoCursor<Document> mongoCursor = findIterable.iterator();
				while (mongoCursor.hasNext()) {
					Document doc = mongoCursor.next();
					d.insertOne(doc);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				GLog.applog.error("db", "copy", e.getMessage(), e);
			}
		});

	}

	@Override
	public void delColumn(String tablename, String colname) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<JSON> listColumns(String table) throws SQLException {

		List<JSON> l1 = JSON.createList();

		Data d = this.load(table, W.create(), Data.class);
		if (d != null) {
			Set<String> key = d.keySet();
			for (String name : key) {
				JSON j1 = JSON.create();
				j1.append("name", name.toLowerCase());
				j1.append("display", name);
				Object o = d.get(name);
				if (o != null) {
					if (o instanceof String) {
						j1.append("type1", "text");
						j1.append("type", "text");
					} else if (o instanceof Long || o instanceof Integer) {
						j1.append("type1", "long");
						j1.append("type", "long");
					} else if (o instanceof Double || o instanceof Float) {
						j1.append("type1", "double");
						j1.append("type", "double");
					} else if (o instanceof List || o.getClass().isArray()) {
						List<?> l2 = X.asList(o, e -> e);
						if (l2 != null && !l2.isEmpty()) {
							Object o1 = l2.get(0);
							if (o1 instanceof String) {
								j1.append("type1", "texts");
								j1.append("type", "texts");
							} else if (o1 instanceof Long || o1 instanceof Integer) {
								j1.append("type1", "longs");
								j1.append("type", "longs");
							} else if (o1 instanceof Double || o1 instanceof Float) {
								j1.append("type1", "doubles");
								j1.append("type", "doubles");
							} else {
								j1.append("type1", o1.getClass().getName());
								j1.append("type", o1.getClass().getName());
							}
						}
					} else {
						j1.append("type1", o.getClass().getName());
						j1.append("type", o.getClass().getName());
					}
				}
				l1.add(j1);
			}
		}
		return l1;

	}

	@Override
	public void createTable(String tablename, String memo, List<JSON> cols) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addColumn(String tablename, JSON col) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void alterColumn(String tablename, JSON col) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public Object run(String sql) throws SQLException {
		MongoDatabase d = getDB();
		if (d != null) {
			W q = SQL.parse(sql);
			return d.runCommand(q.query());
		}
		return null;
	}

	@Override
	public String toString() {
		return "MongoHelper [gdb=" + gdb + "]";
	}

	@Override
	public Connection getConnection() {
		throw new RuntimeException("不支持!");
	}

	@Override
	public void alterTable(String tablename, int partitions) throws SQLException {

	}

	@Override
	public int insertTable(String collection, JSON v) throws SQLException {

		if (v == null || v.isEmpty())
			return 0;

		long t1 = Global.now();
		v.put(X.CREATED, t1);
		v.put(X.UPDATED, t1);
		v.append("_node", Global.id());

		TimeStamp t = TimeStamp.create();

		try {
			MongoCollection<Document> c = getCollection(collection);
			if (c != null) {
				Document d = new Document();

				Object id = v.get(X.ID);
				if (!X.isEmpty(id)) {
					v.append("_id", v.get(X.ID));
				} else {
					v.append("_id", UUID.randomUUID().toString());
				}

				for (Map.Entry<String, Object> e : v.entrySet()) {
					Object v1 = e.getValue();
					if (v1 != null) {
						if (v1 == V.ignore) {
							continue;
						} else if (v1 instanceof UUID) {
							v1 = v1.toString();
						} else if (v1 instanceof ObjectId) {
							v1 = ((ObjectId) v1).toHexString();
						} else if (v1 instanceof Date) {
							v1 = ((Date) v1).getTime();
						}
						d.append(e.getKey(), v1);
					}
				}

				try {

					c.insertOne(d);

					if (log.isDebugEnabled()) {
						log.debug("inserted collection=" + collection + ", d=" + d);
					}
					return 1;
				} catch (Exception e) {
					// log.error(d.toString(), e);
					// error

				}
			}
		} finally {

			Helper.Stat.write(collection, t.pastms());

			if (t.pastms() > 1000 && log.isWarnEnabled()) {
				log.warn("cost=" + t.past() + ", insert [" + collection + "], v=" + v);
			}

		}
		return 0;
	}

	@Override
	public int updateTable(String collection, W q, JSON v) throws SQLException {
		if (v == null || v.isEmpty())
			return 0;

		Object o = v.get(X.UPDATED);
		if (o != V.ignore) {
			v.put(X.UPDATED, Global.now());
			if (log.isDebugEnabled()) {
				log.debug("force updated=" + v.get(X.UPDATED));
			}
		} else if (log.isDebugEnabled()) {
			log.debug("updated=" + o + ", ==ignore");
		}

		// TODO, not allow change the created
		o = v.get(X.CREATED);
		if (o != V.ignore) {
			if (X.toLong(o) == 0) {
				v.remove(X.CREATED);
			}
		} else {
			v.remove(X.CREATED);
		}

		TimeStamp t = TimeStamp.create();
		Document set = new Document();

		// int len = v.size();
		for (Map.Entry<String, Object> e : v.entrySet()) {
			Object v1 = e.getValue();
			if (v1 == V.ignore) {
				continue;
			}

			if (v1 instanceof UUID) {
				v1 = v1.toString();
			} else if (v1 instanceof Date) {
				v1 = ((Date) v1).getTime();
			}
			set.append(e.getKey(), v1);
		}

		try {
			// log.debug("data=" + d);
			MongoCollection<Document> c = getCollection(collection);
			Document d = new Document();
			if (!set.isEmpty()) {
				d.append("$set", set);
			}
			UpdateResult r = null;
//			if (v.value("_id") != null) {
//				r = c.updateMany(q.query(), d, new UpdateOptions().upsert(true));
//			} else {
			r = c.updateMany(q.query(), d);
//			}

			if (log.isDebugEnabled()) {
				log.debug("updated, cost=" + t.past() + ", collection=" + collection + ", query=" + q + ", d=" + set
						+ ", n=" + r.getModifiedCount() + ",result=" + r);
			}

			// r.getN();
			// r.getField("nModified");
			return (int) r.getModifiedCount();
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		} finally {

			Helper.Stat.write(collection, t.pastms());

			if (t.pastms() > 1000 && log.isWarnEnabled()) {
				// to fix bug
				// UTF16 String size is 1093649910, should be less than 1073741823
				// at org.giiwa.dao.Helper$V.toString(Helper.java:383)
				log.warn("cost=" + t.past() + ", update [" + collection + "], q=" + q + ", v=" + v.keySet());
			}

		}
		return 0;
	}

}
