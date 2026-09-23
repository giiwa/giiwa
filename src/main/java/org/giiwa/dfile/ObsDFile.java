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

import java.io.ByteArrayInputStream;
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
import org.giiwa.cache.Cache;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;
import org.giiwa.task.Consumer;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.*;

/**
 * Huawei OBS DFile Implementation, use huawei official obs sdk url format:
 * http(s)://obs-endpoint/bucket?region=cn‑east‑3 disk.path = prefix path in
 * bucket e.g. /data/
 *
 * @author joe
 */
public class ObsDFile extends DFile {

	private static final long serialVersionUID = 1L;
	private static Log log = LogFactory.getLog(ObsDFile.class);

	/** cache obs client by disk.id */
	private static final Map<Long, ObsClient> cached = new ConcurrentHashMap<>();

	private String url;
	private Disk disk_obj;
	private FileInfo info;

	/** resolved bucket name */
	private String bucket;
	/** obs key prefix from disk config */
	private String bucketPrefix;

	private transient ObsClient obsClient;

	public Disk getDisk_obj() {
		return disk_obj;
	}

	@Override
	public boolean exists() throws IOException {
		try {
			getInfo();

			log.info("filename=" + filename + ", info=" + info);

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

	/** recursive delete file or virtual directory */
	private void deleteObsObject(String fullKey, boolean isDir) {
		if (isDir) {
			ListObjectsRequest listReq = new ListObjectsRequest();
			listReq.setBucketName(bucket);
			listReq.setPrefix(fullKey);
			ObjectListing listing;
			do {
				listing = obsClient.listObjects(listReq);
				for (ObsObject obj : listing.getObjects()) {
					obsClient.deleteObject(bucket, obj.getObjectKey());
				}
				listReq.setMarker(listing.getNextMarker());
			} while (listing.isTruncated());
		} else {
			obsClient.deleteObject(bucket, fullKey);
		}
	}

	/** get cached ObsClient, parse url: http(s)://endpoint/bucket?region=xxx */
	private ObsClient getObsClient() throws IOException {

		if (obsClient != null) {
			return obsClient;
		}

		Disk d1 = disk_obj;
		ObsClient client = cached.get(d1.id);
		if (client != null) {
			this.obsClient = client;
			parseBucketPrefix();
			return client;
		}
		synchronized (cached) {
			client = cached.get(d1.id);
			if (client == null) {

				Url u = Url.create(d1.url);
				String ak = this.getDisk_obj().username;
				String sk = this.getDisk_obj().password;

				// strip query params for endpoint
				String endpoint = d1.url;
				int i = endpoint.indexOf("?");
				if (i > 0) {
					endpoint = endpoint.substring(0, i);
				}
				i = endpoint.indexOf("://");
				if (i > 0) {
					endpoint = endpoint.substring(i + 3);
				}

				// ?ssl=true, 或空默认ssl=true
				boolean ssl = "true".equalsIgnoreCase(u.get("ssl")) || X.isEmpty(u.get("ssl"));
				ObsConfiguration obsConfig = new ObsConfiguration();
				endpoint = ssl ? ("https://" + endpoint) : ("http://" + endpoint);
				obsConfig.setEndPoint(endpoint);

				client = new ObsClient(ak, sk, obsConfig);
				if (d1.id > 0) {
					cached.put(d1.id, client);
				}
			}
			this.obsClient = client;
		}
		parseBucketPrefix();
		return obsClient;
	}

	private void parseBucketPrefix() {
		if (X.isEmpty(bucket)) {
			String s = disk_obj.url;
			Url u = Url.create(s);

			// 从链接参数中获取
			bucket = u.get("bucket");
			if (X.isEmpty(bucket)) {
				bucket = u.get("bucketName");
			}
			if (X.isEmpty(bucket)) {
				// 从路径中获取
				int i = s.indexOf("?");
				if (i > 0) {
					s = s.substring(0, i);
				}
				i = s.lastIndexOf("/");
				if (i > 0) {
					bucket = s.substring(i + 1);
				}
			}
			bucketPrefix = disk_obj.path;
		}
	}

	/** get full obs object key */
	private String getFullKey() throws IOException {
		getObsClient();
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
		return X.getCanonicalPath(key + logical);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		String key = getFullKey();
		ObsObject obj = obsClient.getObject(bucket, key);
		return DFileInputStream.create(disk_obj, this, obj.getObjectContent());
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return getOutputStream(0);
	}

	@Override
	public OutputStream getOutputStream(long offset) throws IOException {

		getObsClient();

		if (log.isInfoEnabled()) {
			log.info("obs upload, offset=" + offset + ", filename=" + filename);
		}

		String uploadId = null;
		if (offset > 0) {
			// ========== 这里重点 ==========
			// offset>0 断点续传，你需要从业务数据库读取该文件对应的uploadId
			uploadId = Cache.get("obs://" + disk_obj.id + "/" + filename);

			if (log.isInfoEnabled()) {
				log.info("obs upload, offset=" + offset + ", filename=" + filename + ", uploadid=" + uploadId);
			}

			// uploadId = db.getUploadId(disk_obj.id, filename);
			if (X.isEmpty(uploadId)) {
				throw new IOException("resume upload failed, no uploadId found for offset=" + offset);
			}
		}

		try {
			ObsMultipartOutputStream mpOut = new ObsMultipartOutputStream(this, uploadId);
			if (offset <= 0) {
				Cache.set("obs://" + disk_obj.id + "/" + filename, mpOut.uploadId, X.ADAY);
			}
			// 如果上层业务需要，mpOut.getUploadId() 需要保存入库
			return DFileOutputStream.create(disk_obj, mpOut, filename, offset, (offset1, bb, len) -> {

				if (log.isInfoEnabled()) {
					log.info("obs upload, offset=" + offset1 + ", len=" + len + ", bb=" + bb);
				}

				if (bb != null && len > 0) {
					try {
						mpOut.write(bb, 0, len);
					} catch (Exception e) {
						log.error("write obs multipart error filename=" + filename, e);
					}
				}
				return offset1 + len;
			});
		} catch (Throwable err) {
			log.error("obs upload, filename=" + filename + ", uploadid=" + uploadId, err);
			throw new IOException(err);
		}
	}

	@Override
	public boolean mkdirs() {
		try {
			getObsClient();
			String key = getFullKey();
			if (!key.endsWith("/")) {
				key = key + "/";
			}
			PutObjectRequest req = new PutObjectRequest();
			req.setBucketName(bucket);
			req.setObjectKey(key);
			req.setInput(new ByteArrayInputStream(new byte[0]));
			obsClient.putObject(req);
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
		getObsClient();

		FileInfo fi = new FileInfo();

		String key = getFullKey();

//		log.info("filename=" + filename + ", bucket=" + bucket + ", key=" + key + ", bucketPrefix=" + bucketPrefix);

		// ========== 重点：根路径，key为空字符串，代表桶根，固定为虚拟目录 ==========
		if (X.isEmpty(key) || X.isSame(key, "/") || X.isSame(key, bucketPrefix) || X.isSame(key, bucketPrefix + "/")) {
			fi.isfile = false;
			fi.exists = true;
			fi.length = 0;
			this.info = fi;
			return fi;
		}

		try {

//			log.info("filename=" + filename + ", bucket=" + bucket + ", key=" + key);

			// 1.优先head当前key，如果head成功，代表是真实文件
			ObjectMetadata meta = obsClient.getObjectMetadata(bucket, key);
			fi.exists = true;
			fi.length = meta.getContentLength();
			fi.lastmodified = meta.getLastModified().getTime();

			// ========== 重点：根路径，key为空字符串，代表桶根，固定为虚拟目录 ==========
			if (fi.length <= 0) {
				fi.isfile = false;
			} else {
				fi.isfile = true;
			}

			this.info = fi;
			return fi;
		} catch (ObsException e) {

			log.error("filename=" + filename + ", key=" + key, e);
			// head(key)返回404，可能：不存在 / 逻辑目录（虚拟目录）
			String dirKey;
			if (key.endsWith("/")) {
				dirKey = key;
			} else {
				dirKey = key + "/";
			}

			boolean isVirtualDir = false;
			try {
				// 方式A：检查是否存在0字节占位目录对象（mkdirs生成）
				obsClient.getObjectMetadata(bucket, dirKey);
				isVirtualDir = true;
			} catch (ObsException e2) {

				log.error("filename=" + filename + ", key=" + dirKey, e);

//				if (e2.getResponseCode() == 404) {
				// 方式B：没有占位对象，调用listObjects，prefix=dirKey，maxKeys=1，判断是否存在子对象，代表逻辑目录
				ListObjectsRequest listReq = new ListObjectsRequest();
				listReq.setBucketName(bucket);
				listReq.setPrefix(dirKey);
				listReq.setMaxKeys(1);
				ObjectListing listing = obsClient.listObjects(listReq);
				// 有对象 或者 有commonPrefix子目录，代表逻辑目录存在
				if ((listing.getObjects() != null && !listing.getObjects().isEmpty())
						|| (listing.getCommonPrefixes() != null && !listing.getCommonPrefixes().isEmpty())) {
					isVirtualDir = true;
				}
			}

			if (isVirtualDir || key.endsWith("/")) {
				fi.exists = true;
				fi.isfile = false;
				fi.length = 0L;
				fi.lastmodified = 0L;
			} else {
				fi.exists = false;
			}
			this.info = fi;
			return fi;
		}
	}

	@Override
	public boolean isDirectory() {
		try {
			getInfo();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return info != null && info.exists && !info.isfile;
	}

	@Override
	public boolean isFile() {
		try {
			getInfo();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return info != null && info.exists && info.isfile;
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

		getObsClient();
		String keyPrefix = getFullKey();

		if (!keyPrefix.endsWith("/")) {
			keyPrefix = keyPrefix + "/";
		}

		ListObjectsRequest listReq = new ListObjectsRequest();
		listReq.setBucketName(bucket);
		listReq.setPrefix(keyPrefix);
		listReq.setDelimiter("/");

		List<DFile> resultList = new ArrayList<>();
		String parentLogicalPath = this.filename;
		if (!parentLogicalPath.endsWith("/")) {
			parentLogicalPath = parentLogicalPath + "/";
		}

		ObjectListing listing;
		do {
			listing = obsClient.listObjects(listReq);
			// sub virtual dir
			for (String cp : listing.getCommonPrefixes()) {
				String subLogical = unwrapname(cp, keyPrefix);
				String logicalPath = parentLogicalPath + subLogical;
				FileInfo j1 = new FileInfo();
				j1.name = subLogical;
				j1.exists = true;
				j1.isfile = false;
				// 尝试head虚拟目录对应的占位对象
				try {
					ObjectMetadata dirMeta = obsClient.getObjectMetadata(bucket, cp);
					if (dirMeta != null) {
						j1.lastmodified = dirMeta.getLastModified().getTime();
					}
				} catch (ObsException e) {
					// 404：没有占位对象，自动生成的逻辑目录，保留lastmodified=0
					if (e.getResponseCode() != 404) {
						log.warn("head dir placeholder failed, key=" + cp, e);
					}
				}
				resultList.add(ObsDFile.create(disk_obj, X.getCanonicalPath(logicalPath), j1));
			}

			// normal file
			for (ObsObject obj : listing.getObjects()) {
				String objKey = obj.getObjectKey();
				String subName = unwrapname(objKey, keyPrefix);
				if (X.isEmpty(subName)) {
					continue;
				}
				FileInfo j1 = new FileInfo();
				j1.name = subName;
				j1.exists = true;
				j1.length = obj.getMetadata().getContentLength();
				if (j1.length <= 0) {
					j1.isfile = false;
				} else {
					j1.isfile = true;
				}
				j1.lastmodified = obj.getMetadata().getLastModified().getTime();
				String logicalPath = parentLogicalPath + subName;
				resultList.add(ObsDFile.create(disk_obj, X.getCanonicalPath(logicalPath), j1));
			}
			listReq.setMarker(listing.getNextMarker());
		} while (listing.isTruncated());

		if (resultList.isEmpty()) {
			return null;
		}
		return resultList.toArray(new ObsDFile[resultList.size()]);
	}

	private String unwrapname(String cp, String keyPrefix) {
		if (!X.isEmpty(keyPrefix) && cp.startsWith(keyPrefix)) {
			return cp.substring(keyPrefix.length());
		}
		return cp;
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
			getObsClient();
			FileInfo fi = getInfo();
			if (!fi.exists) {
				return true;
			}
			String fullKey = getFullKey();
			deleteObsObject(fullKey, !fi.isfile);
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
		ObsDFile e = new ObsDFile();
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
	}

	@Override
	public long getFreeSpace() {
		return getTotalSpace() - getUsedSpace();
	}

	@Override
	public long getTotalSpace() {
		try {
			getObsClient();
			BucketQuota bucketQuota = obsClient.getBucketQuota(bucket);
			long quota = bucketQuota.getBucketQuota();
			// quota=0 代表桶没有设置容量上限
			if (quota <= 0 || quota > this.getDisk_obj().quota) {
				return this.getDisk_obj().quota * X.GB;
			}
			return quota;
		} catch (Exception err) {
			// 可能没有权限
			// ignore
//			log.error(bucket, err);
		}
		return this.getDisk_obj().quota * X.GB;
	}

	public long getUsedSpace() {
		try {
			getObsClient();

			// used
			BucketStorageInfo storageInfo = obsClient.getBucketStorageInfo(bucket);
			return storageInfo.getSize();
		} catch (Exception err) {
			// 可能没有权限
			log.error(bucket, err);
		}
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
		String srcKey = this.getFullKey();
		String dstKey = ((ObsDFile) target).getFullKey();

		CopyObjectRequest copyReq = new CopyObjectRequest();
		copyReq.setSourceBucketName(bucket);
		copyReq.setSourceObjectKey(srcKey);
		copyReq.setDestinationBucketName(bucket);
		copyReq.setDestinationObjectKey(dstKey);
		obsClient.copyObject(copyReq);

		this.delete();
		refresh();
		return true;
	}

	static class ObsMultipartOutputStream extends OutputStream {
		private final ObsDFile owner;
		private final String key;
		private String uploadId;
//		@SuppressWarnings("unused")
//		private final int partNumberBase = 1;
		private int nextPartNum = 1;
		// OBS分片最小 5MB
		public static final int PART_MIN_SIZE = 5 * 1024 * 1024;

		private ByteArrayOutputStream buffer = new ByteArrayOutputStream(PART_MIN_SIZE);
		private final List<PartEtag> partEtags = new ArrayList<>();

		private Disk.Counter writer;

		/**
		 * @param uploadId null代表新建上传；非null代表恢复断点续传
		 */
		public ObsMultipartOutputStream(ObsDFile owner, String uploadId) throws IOException {

			this.owner = owner;
			this.writer = Disk.Counter.write(owner.disk_obj);
			this.key = owner.getFullKey();
			this.uploadId = uploadId;
			if (X.isEmpty(uploadId)) {
				// 新建分片上传
				InitiateMultipartUploadResult res = owner.obsClient
						.initiateMultipartUpload(new InitiateMultipartUploadRequest(owner.bucket, key));
				this.uploadId = res.getUploadId();
			} else {
				// 断点恢复：查询已经上传的分片 【3.26.6：使用ListPartsResult，Part替代MultipartPart】
				ListPartsRequest req = new ListPartsRequest(owner.bucket, key, uploadId);
				ListPartsResult partResult;
				do {
					partResult = owner.obsClient.listParts(req);
					for (Multipart part : partResult.getMultipartList()) {
						// PartEtag 参数1是字符串类型分片号
						partEtags.add(new PartEtag(part.getEtag(), part.getPartNumber()));
					}

					String nextMarker = partResult.getNextPartNumberMarker();
					if (X.isEmpty(nextMarker)) {
						req.setPartNumberMarker(null);
					} else {
						try {
							Integer num = Integer.parseInt(nextMarker);
							req.setPartNumberMarker(num);
						} catch (NumberFormatException e) {
							// 非法marker，结束分页
							req.setPartNumberMarker(null);
						}
					}
				} while (partResult.isTruncated());

				// 下一个分片编号
				if (!partEtags.isEmpty()) {
					nextPartNum = partEtags.stream().mapToInt(p -> p.getPartNumber()).max().getAsInt() + 1;
				}
			}
		}

		@Override
		public void write(int b) throws IOException {
			buffer.write(b);
			flushIfFull();
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			buffer.write(b, off, len);
			flushIfFull();
		}

		/** buffer >=5MB，上传分片 */
		private void flushIfFull() throws IOException {
			if (buffer.size() >= PART_MIN_SIZE) {
				uploadCurrentPart();
			}
		}

		private void uploadCurrentPart() throws IOException {
			if (buffer.size() <= 0) {
				return;
			}
			TimeStamp t = TimeStamp.create();

			byte[] data = buffer.toByteArray();
			UploadPartRequest req = new UploadPartRequest();
			req.setBucketName(owner.bucket);
			req.setObjectKey(key);
			req.setUploadId(uploadId);
			req.setPartNumber(nextPartNum);
			req.setInput(new ByteArrayInputStream(data));
			UploadPartResult result = owner.obsClient.uploadPart(req);

			if (log.isInfoEnabled()) {
				log.info("obs upload, req=[key=" + key + ", uploadid=" + uploadId + ", partnumber=" + nextPartNum
						+ ", size=" + data.length + "], result=" + result);
			}

			// PartEtag第一个参数必须字符串
			partEtags.add(new PartEtag(result.getEtag(), nextPartNum));
			nextPartNum++;
			buffer.reset();

			writer.add(data.length, t.pastms());

		}

		@Override
		public void flush() throws IOException {
			// 分片必须达到5MB阈值，flush不触发上传
			flushIfFull();
		}

		/** 完成分片合并，close必须调用 */
		@Override
		public void close() throws IOException {
			try {
				// 上传剩余buffer（最后一片，允许小于5MB）
				int size = buffer.size();
				if (size <= 0) {
					return;
				}

				uploadCurrentPart();

				// ========== 3.26.6 修复：使用带partEtags的构造函数 ==========
				if (size < PART_MIN_SIZE) {
					if (log.isInfoEnabled()) {
						log.info("obs upload, last part, size=" + size + ", filename=" + owner.filename + ", upliadid="
								+ uploadId);
					}
					TimeStamp t = TimeStamp.create();
					// 最后一片， 合并所有分片
					CompleteMultipartUploadRequest complete = new CompleteMultipartUploadRequest(owner.bucket, key,
							uploadId, partEtags);
					owner.obsClient.completeMultipartUpload(complete);
					owner.refresh();

					writer.add(0, t.pastms());

					Cache.remove("obs://" + owner.disk_obj.id + "/" + owner.filename);
				}
			} catch (ObsException e) {
				abort();
				throw new IOException("multipart upload failed", e);
			} finally {
				buffer.close();
			}
		}

		/** 放弃分片，丢弃上传 */
		public void abort() throws IOException {
			if (!X.isEmpty(uploadId)) {
				AbortMultipartUploadRequest abortReq = new AbortMultipartUploadRequest(owner.bucket, key, uploadId);
				owner.obsClient.abortMultipartUpload(abortReq);
			}
		}

		public String getUploadId() {
			return uploadId;
		}
	}

}
