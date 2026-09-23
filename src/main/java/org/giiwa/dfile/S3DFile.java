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
package org.giiwa.dfile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;
import org.giiwa.bean.GLog;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;
import org.giiwa.task.Consumer;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * S3 Compatible Object Storage DFile Implementation url format:
 * http(s)://endpoint/bucket?region=cn‑north‑1 disk.path = prefix path in bucket
 * e.g. /data/
 *
 * 注意： 兼容Minio，阿里云OSS，不兼容腾讯OSS， 腾讯OSS走自己的OBS。
 * 
 * @author joe
 */
public class S3DFile extends DFile {

	private static final long serialVersionUID = 1L;
	private static Log log = LogFactory.getLog(S3DFile.class);

	/**
	 * cache s3 client by disk.id
	 */
	private static final Map<Long, S3Client> cached = new ConcurrentHashMap<>();

	private String url;
	private Disk disk_obj;
	private FileInfo info;

	/** resolved bucket name */
	private String bucket;
	/** s3 key prefix from disk config */
	private String bucketPrefix;

	private transient S3Client s3Client;

	// 持有输出流实例，修复getInnerOutputStream不存在问题
	private transient S3PlaceholderOutputStream placeholderOut;

	public Disk getDisk_obj() {
		return disk_obj;
	}

	@Override
	public boolean exists() throws IOException {
		try {
			getInfo();
			return info != null && info.exists;
		} catch (Exception e) {
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
			throw new IOException(e);
		}
	}

	@Override
	protected boolean delete0(long age) {
		try {
			FileInfo fi = getInfo();
			if (fi != null && fi.exists && fi.lastmodified < System.currentTimeMillis() - age) {
				delete();
			}
			return true;
		} catch (Exception e) {
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
			GLog.applog.error("dfile", "delete0", url + ":" + disk_obj.path + ":" + filename, e);
		}
		return false;
	}

	/**
	 * recursive delete file or virtual directory
	 */
	private void deleteS3Object(String fullKey, boolean isDir) {
		if (isDir) {
			// virtual folder: list all keys under prefix and delete all
			ListObjectsV2Request listReq = ListObjectsV2Request.builder().bucket(bucket).prefix(fullKey).build();
			ListObjectsV2Iterable iterable = s3Client.listObjectsV2Paginator(listReq);
			for (S3Object obj : iterable.contents()) {
				s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(obj.key()).build());
			}
		} else {
			s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(fullKey).build());
		}
	}

	/**
	 * get cached S3Client, parse url: s3://endpoint/bucket?region=xxx&ssl=true
	 */
	private synchronized S3Client getS3Client() throws IOException {
		if (s3Client != null) {
			return s3Client;
		}
		Disk d1 = disk_obj;
		S3Client client = cached.get(d1.id);
		if (client != null) {
			this.s3Client = client;
			parseBucketPrefix();
			return client;
		}
		synchronized (cached) {
			client = cached.get(d1.id);
			if (client == null) {
				Url u = Url.create(d1.url);
				String ak = this.getDisk_obj().username;
				String sk = this.getDisk_obj().password;

				AwsBasicCredentials cred = AwsBasicCredentials.create(ak, sk);
				software.amazon.awssdk.services.s3.S3Configuration.Builder s3confbuilder = software.amazon.awssdk.services.s3.S3Configuration
						.builder();
				if (X.isSame(u.get("pathstyle"), "true")) {
					// minio
					s3confbuilder.pathStyleAccessEnabled(true);
				} else {
					// 腾讯OSS, 阿里云 OSS, 华为 OBS
					s3confbuilder.pathStyleAccessEnabled(false);
				}

				software.amazon.awssdk.services.s3.S3Configuration s3Conf = s3confbuilder.build();
				S3ClientBuilder s3builder = S3Client.builder()
						.credentialsProvider(StaticCredentialsProvider.create(cred)).serviceConfiguration(s3Conf);

				String endpoint = url;
				int i = endpoint.indexOf("?");
				if (i > 0) {
					endpoint = endpoint.substring(0, i);
				}
				i = endpoint.indexOf("/", 8);
				if (i > 0) {
					endpoint = endpoint.substring(0, i);
				}
				i = endpoint.indexOf("//");
				if (i > 0) {
					endpoint = endpoint.substring(i + 2);
				}

				if (X.isSame(u.get("ssl"), "ssl")) {
					s3builder.endpointOverride(java.net.URI.create("https://" + endpoint));
				} else {
					s3builder.endpointOverride(java.net.URI.create("http://" + endpoint));
				}

				String regionStr = u.get("region");
				if (!X.isEmpty(regionStr)) {
					s3builder.region(Region.of(regionStr));
				}

				client = s3builder.build();
				if (d1.id > 0) {
					cached.put(d1.id, client);
				}
			}
			this.s3Client = client;
		}
		parseBucketPrefix();
		return s3Client;
	}

	/**
	 * convert logical filename to full s3 key: bucketPrefix + filename
	 */
	private void parseBucketPrefix() {
		// http(s)://endpoint/bucket?region=xxx
		if (X.isEmpty(bucket)) {
			String s = disk_obj.url;
			Url u = Url.create(s);
			bucket = u.get("bucket");
			if (X.isEmpty(bucket)) {
				int i = s.indexOf("?");
				if (i > 0) {
					s = s.substring(0, i);
				}
				i = s.lastIndexOf("/");
				if (i > 0) {
					bucket = s.substring(i + 1);
				}
			}

			// ->/saas
			bucketPrefix = disk_obj.path;
		}
	}

	/**
	 * get full s3 object key
	 */
	private String getFullKey() throws IOException {
		getS3Client();
		String logical = rewrite(filename);
		if (X.isEmpty(bucketPrefix)) {
			return logical.startsWith("/") ? logical.substring(1) : logical;
		}
		String key = bucketPrefix;
		if (!key.endsWith("/")) {
			key = key + "/";
		}
		if (logical.startsWith("/")) {
			logical = logical.substring(1);
		}
		return key + logical;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		String key = getFullKey();
		GetObjectRequest req = GetObjectRequest.builder().bucket(bucket).key(key).build();
		InputStream is = s3Client.getObject(req);
		return DFileInputStream.create(disk_obj, this, is);
	}

	/**
	 * S3 note: S3 does NOT support random offset append write. offset >0 not
	 * supported.
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		return getOutputStream(0);
	}

	@Override
	public OutputStream getOutputStream(long offset) throws IOException {
		if (offset > 0) {
			throw new IOException("S3 storage not support offset append write, offset must be 0");
		}
		placeholderOut = new S3PlaceholderOutputStream(this);
		return DFileOutputStream.create(disk_obj, placeholderOut, filename, offset, (o1, bb, len) -> {
			if (bb != null && len > 0) {
				try {
					placeholderOut.write(bb, 0, len);
				} catch (Exception e) {
					log.error("write s3 error filename=" + filename, e);
				}
			}
			return o1 + len;
		});
	}

	/**
	 * placeholder outputstream, buffer all bytes then upload to s3 on close()
	 */
	static class S3PlaceholderOutputStream extends OutputStream {
		private final S3DFile owner;
		private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		public S3PlaceholderOutputStream(S3DFile owner) {
			this.owner = owner;
		}

		@Override
		public void write(int b) throws IOException {
			baos.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			baos.write(b, off, len);
		}

		@Override
		public void close() throws IOException {
			byte[] data = baos.toByteArray();
			String key = owner.getFullKey();
			PutObjectRequest req = PutObjectRequest.builder().bucket(owner.bucket).key(key).build();
			// SDK v2.54.2 必须用 RequestBody
			owner.s3Client.putObject(req, RequestBody.fromBytes(data));
			baos.reset();
			owner.refresh();
		}

		@Override
		public void flush() throws IOException {
			// no‑op, real flush happen on close
		}
	}

	@Override
	public boolean mkdirs() {
		try {
			// S3 virtual directory: create zero‑byte key endswith /
			getS3Client();
			String key = getFullKey();
			if (!key.endsWith("/")) {
				key = key + "/";
			}
			PutObjectRequest req = PutObjectRequest.builder().bucket(bucket).key(key).build();
			// 空字节数组RequestBody
			s3Client.putObject(req, RequestBody.fromBytes(new byte[0]));
			refresh();
			return true;
		} catch (Exception e) {
			log.error(url + ":" + this.disk_obj.path + ":" + filename, e);
		}
		return false;
	}

	@Override
	public DFile getParentFile() {
		int i = filename.lastIndexOf("/", filename.length() - 1);
		if (i > 0) {
			return create(disk_obj, filename.substring(0, i));
		} else if (i == 0) {
			return create(disk_obj, "/");
		} else {
			return null;
		}
	}

	private FileInfo getInfo() throws IOException {
		if (info != null) {
			return info;
		}
		getS3Client();
		FileInfo fi = new FileInfo();
		String key = getFullKey();
		try {
			HeadObjectRequest headReq = HeadObjectRequest.builder().bucket(bucket).key(key).build();
			HeadObjectResponse resp = s3Client.headObject(headReq);
			fi.exists = true;
			fi.isfile = true;
			fi.length = resp.contentLength();
			fi.lastmodified = resp.lastModified().toEpochMilli();
		} catch (NoSuchKeyException e) {
			// check virtual directory, try key + /
			String dirKey;
			if (key.endsWith("/")) {
				dirKey = key;
			} else {
				dirKey = key + "/";
			}
			try {
				HeadObjectRequest dirHead = HeadObjectRequest.builder().bucket(bucket).key(dirKey).build();
				s3Client.headObject(dirHead);
				fi.exists = true;
				fi.isfile = false;
			} catch (NoSuchKeyException e2) {
				fi.exists = false;
			}
		} catch (S3Exception e) {
			// 打印完整服务端信息
			log.error("S3调用异常 statusCode=" + e.statusCode() + ", awsErrorMsg=" + e.awsErrorDetails().errorMessage()
					+ ", errorCode=" + e.awsErrorDetails().errorCode());
			log.error("服务端返回XML详情:\n" + e.awsErrorDetails().rawResponse());
			e.printStackTrace();
		}
		this.info = fi;
		return fi;
	}

	@Override
	public boolean isDirectory() {
		try {
			getInfo();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return info != null && !info.isfile;
	}

	@Override
	public boolean isFile() {
		try {
			getInfo();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return info != null && info.isfile;
	}

	@Override
	public String getName() {
		String[] ss = X.split(filename, "[/]");
		if (ss != null && ss.length > 0) {
			return ss[ss.length - 1];
		}
		return X.EMPTY;
	}

	@Override
	protected DFile[] list() throws IOException {
		getS3Client();
		String keyPrefix = getFullKey();
		if (!keyPrefix.endsWith("/")) {
			keyPrefix = keyPrefix + "/";
		}
		ListObjectsV2Request listReq = ListObjectsV2Request.builder().bucket(bucket).prefix(keyPrefix).delimiter("/")
				.build();
		ListObjectsV2Iterable paginator = s3Client.listObjectsV2Paginator(listReq);
		List<DFile> resultList = new ArrayList<>();
		String parentLogicalPath = this.filename;
		if (!parentLogicalPath.endsWith("/")) {
			parentLogicalPath = parentLogicalPath + "/";
		}
		for (ListObjectsV2Response page : paginator) {
			// common prefix = sub virtual dir
			for (CommonPrefix cp : page.commonPrefixes()) {
				String fullKey = cp.prefix();
				String subLogical = fullKey.substring(keyPrefix.length());
				if (subLogical.endsWith("/")) {
					subLogical = subLogical.substring(0, subLogical.length() - 1);
				}
				String logicalPath = parentLogicalPath + subLogical;
				FileInfo j1 = new FileInfo();
				j1.name = subLogical;
				j1.exists = true;
				j1.isfile = false;
				j1.length = 0;
				j1.lastmodified = 0;
				resultList.add(S3DFile.create(disk_obj, X.getCanonicalPath(logicalPath), j1));
			}
			// normal file objects
			for (S3Object obj : page.contents()) {
				String objKey = obj.key();
				String subName = objKey.substring(keyPrefix.length());
				if (X.isEmpty(subName)) {
					continue;
				}
				FileInfo j1 = new FileInfo();
				j1.name = subName;
				j1.exists = true;
				j1.isfile = true;
				j1.length = obj.size();
				j1.lastmodified = obj.lastModified().toEpochMilli();
				String logicalPath = parentLogicalPath + subName;
				resultList.add(S3DFile.create(disk_obj, X.getCanonicalPath(logicalPath), j1));
			}
		}
		if (resultList.isEmpty()) {
			return null;
		}
		return resultList.toArray(new S3DFile[0]);
	}

	@Override
	public long getCreation() {
		try {
			getInfo();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return info == null ? 0 : info.creation;
	}

	@Override
	public long lastModified() {
		try {
			getInfo();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return info == null ? 0 : info.lastmodified;
	}

	public String getCanonicalPath() {
		return filename;
	}

	@Override
	public long length() {
		try {
			getInfo();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return info == null ? 0 : info.length;
	}

	@Override
	public boolean move(String filename) throws IOException {
		return move(Disk.seek(filename));
	}

	@Override
	public boolean move(DFile file) {
		try {
			X.IO.copy(this, file);
			return this.delete();
		} catch (Exception e) {
			log.error(url, e);
		}
		return false;
	}

	@Override
	public boolean delete() {
		try {
			getS3Client();
			FileInfo fi = getInfo();
			if (!fi.exists) {
				return true;
			}
			String fullKey = getFullKey();
			deleteS3Object(fullKey, !fi.isfile);
			refresh();
			return true;
		} catch (Exception err) {
			log.error(filename, err);
		}
		return false;
	}

	public static DFile create(Disk d, String filename) {
		return create(d, filename, null);
	}

	public static DFile create(Disk d, String filename, FileInfo info) {
		S3DFile e = new S3DFile();
		e.url = d.url;
		e.disk_obj = d;
		e.filename = filename;
		e.info = info;
		return e;
	}

	@Override
	public long count(Consumer<String> moni) {
		long n = 0;
		try {
			if (this.isDirectory()) {
				DFile[] ff = this.listFiles();
				if (ff != null) {
					for (DFile f : ff) {
						n += f.count(moni);
					}
				}
			} else {
				n++;
			}
			if (moni != null) {
				moni.accept(this.getFilename());
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return n;
	}

	@Override
	public long sum(Consumer<String> moni) {
		long n = 0;
		if (this.isDirectory()) {
			try {
				DFile[] ff = this.listFiles();
				if (ff != null) {
					for (DFile f : ff) {
						n += f.sum(moni);
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		n += this.length();
		if (moni != null) {
			moni.accept(this.filename);
		}
		return n;
	}

	@Override
	public Path getPath() {
		return Paths.get(URI.create(filename));
	}

	@Override
	public void refresh() {
		info = null;
		placeholderOut = null;
	}

	@Override
	public long getFreeSpace() {
		// S3 object storage cannot get free space
		return 0;
	}

	@Override
	public long getTotalSpace() {
		return 0;
	}

	@Override
	public boolean rename(String name) throws IOException {
		int i = filename.lastIndexOf("/");
		String newLogicalName;
		if (i < 0) {
			newLogicalName = "/" + name;
		} else {
			newLogicalName = filename.substring(0, i + 1) + name;
		}
		DFile target = create(disk_obj, newLogicalName);
		// S3 rename = copy + delete old
		String srcKey = this.getFullKey();
		String dstKey = ((S3DFile) target).getFullKey();
		CopyObjectRequest copyReq = CopyObjectRequest.builder().sourceBucket(bucket).sourceKey(srcKey)
				.destinationBucket(bucket).destinationKey(dstKey).build();
		s3Client.copyObject(copyReq);
		this.delete();
		refresh();
		return true;
	}

}
