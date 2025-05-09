package org.giiwa.net.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.apache.commons.fileupload2.core.FileItem;
import org.giiwa.bean.App;
import org.giiwa.bean.Temp;
import org.giiwa.conf.Config;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Html;
import org.giiwa.misc.Url;
import org.junit.Test;

public class HttpTest {

	@Test
	public void testPost() {
		String url = "http://g09:8080/f/echo";
		Http h = Http.create();
		JSON j1 = JSON.create();
		j1.append("a", "测试");

//		Http.Response r = h.post(url, j1);
		Http.Response r = h.post(url, j1, j1);
//		Http.Response r = h.json(url, j1);

		System.out.println(r.body.replaceAll("<br>", "\r\n"));
	}

	@Test
	public void testDns() {
		String s = "https://www.doctrine.af.mil/";
		Http h = Http.create();
		h.dns("www.doctrine.af.mil", "104.69.79.225");
		Http.Response r = h.get(s);
		System.out.println(r.body);

	}

	@Test
	public void testRedirect() {

		String s = "https://flightaware.com/squawks/link/1/24_hours/popular/84059/JetBlue_will_deploy_the_Airbus_A220_on_4_more_highly_competitive_routes";

		Http h = Http.create();
		Http.Response r = h.get(s);
		System.out.println(r.url);

//		System.out.println(r.body);
	}

	@Test
	public void testSSL() {

		Config.init();

		String s = "https://duckduckgo.com/?q=site%3Aglobalsecurity.org&k1=-1&k9=%230101C4&kj=%23913100&km=l&kq=-1&kr=c&kt=h&ku=1&kv=1&ia=web";
		s = "https://codesite-archive.appspot.com/search/query?query&page=1";
		s = "https://mp.weixin.qq.com/s?__biz=MzU2NzUxODg4MA==&mid=2247522208&idx=1&sn=4cd1044aa49383aab687b2899e2b4b4b&chksm=fde5e50913d9fd77084bfb72d5b5c5d942168b5164143e62fbaeb3e5af300c46bbb53edc8267&scene=0&xtrack=1#rd";
		Http h = Http.create();
//		h.dns("duckduckgo.com", "104.16.251.55");

		Http.Response r = h.get(s);

		System.out.println(r.body);

//		Temp t = r.save("a.pdf");

//		System.out.println(t.getFile().getAbsolutePath());

//		s = "https://www.defense.gov/Newsroom/Transcripts/";
//		h.dns("www.defense.gov", "23.45.33.197");
//		r = h.get(s);
//		System.out.println(r.type);
//		System.out.println(r.body);

	}

	@Test
	public void testDownload() {

		System.out.println("start ...");

		Temp.ROOT = "/Users/joe/Downloads/temp/";

//		String s = "https://intelshare.intelink.gov/sites/alsacenter/SiteCollectionDocuments/jfire_2019.pdf";
		String s = "https://codeload.github.com/freeCodeCamp/freeCodeCamp/zip/refs/heads/i18n-sync-client";
		Http h = Http.create();

//		h.get("https://www.alsa.mil/mttps/jfire/");

//		h.dns("intelshare.intelink.gov", "67.133.98.35");
//		Temp t = Temp.create("a.pdf");
		try {
//			h.download(s, t.getOutputStream());
			Temp t = h.download(s);
			File f1 = t.getFile();
			System.out.println(f1.length());
			System.out.println(f1.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testDownloadcnki() {

		String url = "https://kns.cnki.net/kcms/detail/detail.aspx?dbcode=CJFD&dbname=CJFDAUTO&filename=KMSG202104008&uniplatform=NZKPT&v=%25mmd2BT%25mmd2F2xnNIaVVUlfcE2y3Zbb4u%25mmd2FcQSnUOOYaCnAg2tGx%25mmd2BKgfkHqO11IIf6yRwsU2Y3&PlatForm=kdoc&UID=WEEvREcwSlJHSldSdmVqMDh6a1doNkJTa0l5U2l0elQ4T0padkpiUlpqYz0%3d%249A4hF_YAuvQ5obgVAqNKPCYcEjKensW4IQMovwHtwkF4VYPoHbKxJw!!&PlatForm=kdoc";

		Http h = Http.create();

		try {
			JSON head = JSON.create();

			// 第一步， 获取真实下载链接， 需要登录配置信息
			// 链接为页面中提取的下载链接
			try {
				url = "https://kns.cnki.net/kcms/detail/detail.aspx?dbcode=CJFD&dbname=CJFDAUTO&filename=CDJI202108011&uniplatform=NZKPT&v=cm%25mmd2FfE5wwGlU1vx5iO2KLSnEluYn0gSYlmw96Ar708d7kjg8bVkk7%25mmd2BPFhEiCCw42r";
				head.append("cookie",
						"ecp_uid5=79570061fecda3cd474cab2e5b760d9b; Ecp_notFirstLogin=pLwdJY; Ecp_ClientId=2210901113401688190; ASP.NET_SessionId=iilz5phi1hc2hedutdchvstt; SID_kns8=123111; cnkiUserKey=2b01f2ca-acc1-7715-64bf-35bb5abea2d7; SID_kns_new=kns123109; CurrSortField=%e5%8f%91%e8%a1%a8%e6%97%b6%e9%97%b4%2f(%e5%8f%91%e8%a1%a8%e6%97%b6%e9%97%b4%2c%27TIME%27); CurrSortFieldType=desc; Ecp_ClientIp=117.136.35.86; SID_kcms=124112; knsLeftGroupSelectItem=; sensorsdata2015jssdkcross=%7B%22distinct_id%22%3A%2217b9f6f5d48d46-059344741753a-35667c03-1296000-17b9f6f5d4abea%22%2C%22first_id%22%3A%22%22%2C%22props%22%3A%7B%22%24latest_traffic_source_type%22%3A%22%E7%9B%B4%E6%8E%A5%E6%B5%81%E9%87%8F%22%2C%22%24latest_search_keyword%22%3A%22%E6%9C%AA%E5%8F%96%E5%88%B0%E5%80%BC_%E7%9B%B4%E6%8E%A5%E6%89%93%E5%BC%80%22%2C%22%24latest_referrer%22%3A%22%22%7D%2C%22%24device_id%22%3A%2217b9f6f5d48d46-059344741753a-35667c03-1296000-17b9f6f5d4abea%22%7D; Ecp_loginuserjf=ntcs001; IsLogin=; Ecp_IpLoginFail=21090136.149.1.80; Ecp_loginuserbk=lescetc28; _pk_ses=*; c_m_LinID=LinID=WEEvREcwSlJHSldSdmVqMDh6a1doNkJTa0l5U2l0elQ4T0padkpiUlpqYz0=$9A4hF_YAuvQ5obgVAqNKPCYcEjKensW4IQMovwHtwkF4VYPoHbKxJw!!&ot=09/09/2021 07:18:07; LID=WEEvREcwSlJHSldSdmVqMDh6a1doNkJTa0l5U2l0elQ4T0padkpiUlpqYz0=$9A4hF_YAuvQ5obgVAqNKPCYcEjKensW4IQMovwHtwkF4VYPoHbKxJw!!; Ecp_session=1; _pk_id=c1fb4f97-ca12-4990-a5f8-1bf58cbc5699.1630467271.4.1630538289.1630485882.; Ecp_LoginStuts={\"IsAutoLogin\":false,\"UserName\":\"lescetc28\",\"ShowName\":\"%E4%B8%AD%E5%9B%BD%E7%94%B5%E5%AD%90%E7%A7%91%E6%8A%80%E9%9B%86%E5%9B%A2%E5%85%AC%E5%8F%B8%E7%AC%AC%E4%BA%8C%E5%8D%81%E5%85%AB%E7%A0%94%E7%A9%B6%E6%89%80\",\"UserType\":\"bk\",\"BUserName\":\"\",\"BShowName\":\"\",\"BUserType\":\"\",\"r\":\"pLwdJY\"}; c_m_expire=2021-09-02 07:36:17");
				Http.Response r = h.get(url, head);

				System.out.println("head=" + head);

				System.out.println(r.body);
				Html h1 = r.html();
				url = h1.select("#pdfDown").get(0).attr("href");
				url = h1.format(url);
				System.out.println(url);

				h.redirect(false);

				r = h.get(url, head);
				System.out.println(r.body);

				h1 = r.html();
				url = h1.select("a").get(0).attr("href");

				r = h.get(url, head);
				System.out.println(r.body);

				h1 = r.html();
				url = h1.select("a").get(0).attr("href");

				r = h.get(url, head);
				System.out.println(r.body);

				h1 = r.html();
				url = h1.select("a").get(0).attr("href");
				url = url.replaceFirst("%", "%25").replace("cajdown", "pdfdown");
				System.out.println(url);

				int i1 = url.indexOf("title=");
				int i2 = url.indexOf("&", i1);

				String title = url.substring(i1 + 6, i2);
				title = Url.decode(title);

				Temp t = Temp.create(title + ".pdf");
				h.download(url, head, t.getOutputStream());

//				JSAPI._X1.create(0).savefile1("", t);

				System.out.println(t.getFile().getAbsolutePath());

			} catch (Exception e) {
				e.printStackTrace();
			}

			// 2，使用真实的下载链接下载资源，不需要登录配置信息
//			url = "https://caj.d.cnki.net/kfdoc/down.aspx?uid=WEEvREcwSlJHSldSdmVqMDh6a1doNkJTa0l5U2l0elQ4T0padkpiUlpqYz0=$9A4hF_YAuvQ5obgVAqNKPCYcEjKensW4IQMovwHtwkF4VYPoHbKxJw!!&fn=zjB%25mmd2FQm9f7eFmTPSOQxnmX9NzDAL4zWCTmwj0uQlsFEY=&title=%e6%ae%8a%e6%96%b9%e5%bc%82%e7%89%a9%e4%b8%8e%e4%b8%ad%e5%8f%a4%e5%9b%ad%e6%9e%97%e4%b8%ad%e7%9a%84%e5%9c%b0%e7%90%86%e7%a9%ba%e9%97%b4%e6%84%8f%e6%b6%b5_%e9%be%9a%e7%8f%8d&dbcode=cjfq&rescode=CJFD&dflag=pdfdown&lang=gb&t=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBpZCI6IjgwNzA5IiwidGltZXN0YW1wIjoxNjMwNTQxOTM4LCJub25jZSI6InYxN3lZcE92QjAifQ.dc6fRCrweogVnKn1ztJ4vUO5JmPSlp_rWQJj8HRuOO4&zt=C038&doi=CNKI:SUN:KMSG.0.2021-04-008&sid=%e4%b8%ad%e5%bf%83%e7%bd%91%e7%ab%99&filetitle=%ca%e2%b7%bd%d2%ec%ce%ef%d3%eb%d6%d0%b9%c5%d4%b0%c1%d6%d6%d0%b5%c4%b5%d8%c0%ed%bf%d5%bc%e4%d2%e2%ba%ad_%b9%a8%d5%e4";
//			Temp t = Temp.create("a.pdf");
//			h.download(url, head, t.getOutputStream());
//
//			System.out.println(t.getFile().getAbsolutePath());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testPost2() {
		String url = "https://sourcegraph.com/.api/graphql";
		String param = "{\n"
				+ " \"query\": \"query ($repository: String!) {\\n  repository(name: $repository) {\\n    branches {\\n      nodes {\\n        displayName\\n        target {\\n          commit {\\n            oid\\n            author {\\n              date\\n              __typename\\n            }\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    tags {\\n      nodes {\\n        displayName\\n        target {\\n          commit {\\n            oid\\n            author {\\n              date\\n              __typename\\n            }\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\",\n"
				+ " \"variables\": {\n" + "  \"repository\": \"github.com/freeCodeCamp/freeCodeCamp\"\n" + " }\n" + "}";

		Http h = Http.create();
		Http.Response r = h.post(url, param);
		System.out.println(r.body);

	}

	@Test
	public void testXml() {

		String s = "https://www.dia.mil/DesktopModules/ArticleCS/RSS.ashx?ContentType=1&Site=661&isdashboardselected=0&max=24";
		Http h = Http.create();
		h.dns("www.dia.mil", "23.3.104.163");
		Http.Response r = h.get(s);
//		System.out.println(r.body);

		JSON j1 = r.xml();
		System.out.println(j1.toPrettyString());

	}

	@Test
	public void testEsp() {

		String s = "https://www.rtx.com//sxa/search/results/?l=en&s={C8C0246C-349E-48A6-87B1-76BE3124A567}&itemid={02858F12-F8F2-4E35-9FAB-1389CBD2D883}&sig=newssearchresults&p=8&e=16&o=NewsDate%2CDescending&v=%7B5343FDD1-C9C0-4A7D-8194-1839B7C853A7%7D";
		Http h = Http.create();
		Http.Response r = h.get(s);
		System.out.println(r.body);

	}

	@Test
	public void testVgg() {

		Http h = Http.create();
		File f = new File("/Users/joe/Documents/ed57c6ad1302e13de2b7f3bf4bce0080.jpg");

		try {

			String crlf = "\r\n";
			String twoHyphens = "--";
			String boundary = "*****";

			HttpURLConnection httpUrlConnection = null;
			URL url = new URL("http://g10:50000/vector");
			httpUrlConnection = (HttpURLConnection) url.openConnection();
			httpUrlConnection.setUseCaches(false);
			httpUrlConnection.setDoOutput(true);

			httpUrlConnection.setRequestMethod("POST");
			httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
			httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
			httpUrlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

			DataOutputStream request = new DataOutputStream(httpUrlConnection.getOutputStream());

			request.writeBytes(twoHyphens + boundary + crlf);
			request.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + f.getName() + "\"" + crlf);
			request.writeBytes(crlf);

			InputStream in = new FileInputStream(f);
			byte[] bb = new byte[32 * 1024];
			int len = in.read(bb);
			while (len > 0) {
				request.write(bb, 0, len);
				len = in.read(bb);
			}
			in.close();

			request.writeBytes(crlf);
			request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
			request.flush();
			request.close();

			InputStream responseStream = new BufferedInputStream(httpUrlConnection.getInputStream());

			BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));

			String line = "";
			StringBuilder stringBuilder = new StringBuilder();

			while ((line = responseStreamReader.readLine()) != null) {
				stringBuilder.append(line).append("\n");
			}
			responseStreamReader.close();

			String response = stringBuilder.toString();
			responseStream.close();
			httpUrlConnection.disconnect();

			System.out.println(response);

//			Http.Response r = h.post("http://g10:50000/vector", null, JSON.create().append("a", 1), "file", f.getName(),
//					new FileInputStream(f), f.length());

			Http.Response r = h.post("http://g10:50000/vector", JSON.create().append("a", 1).toString());

			System.out.println(r.body);

			JSON j1 = r.json();
			List<Float> l1 = (List<Float>) j1.get("list");

			System.out.println(l1.size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testTimeout() {

		String appid = "demo1";
		String secret = "voeQ1dr1DREMefOf5u4EP1iAlO84lsn7";
		String api = "https://pdcapitest.capub.cn/api/560/[auth_token]";
		String body = "text=外国文学&s=0&n=5&type=sale&cate=";
		String proxy = "";

		TimeStamp t = TimeStamp.create();

		// test
		Http h = new Http.Builder().proxy(proxy, null, null).timeout(X.AMINUTE).create();

		try {

			FileItem file = null;

			if (!X.isEmpty(appid)) {
				String time = Long.toString(System.currentTimeMillis());
				time = App.encode(time, secret);
				Url u = Url.create(api);
				u.setUri("/api/auth");

				Http.Response r = h.post(u.encodedUrl(), JSON.create().append("appid", appid).append("time", time));
				JSON j1 = JSON.fromObject(r.body);
				if (j1 == null || j1.getInt(X.STATE) != 200) {
					System.out.println("result=" + r.body);
				} else {
					String s = api.replaceAll("\\[auth_token\\]", j1.getString("token"));
					JSON j2 = JSON.fromObject(body);
					if (j2 == null) {
						j2 = JSON.create();
					}
					if (file == null) {
						r = h.post(s, j2.toString());
					} else {
						r = h.post(s, JSON.create(), j2, "file", file.getName(), file.getInputStream());
					}
					if (r.status == 200) {
						JSON j3 = JSON.fromObject(r.body);
						if (j3 == null) {
							System.out.println("result=" + r.body);
						} else {
							System.out.println("result=" + j3.toPrettyString());
						}
					} else {
						System.out.println("result=" + r.body);
					}
				}
			} else {
				JSON j2 = JSON.fromObject(body);
				if (j2 == null) {
					j2 = JSON.create();
				}
				Http.Response r = null;
				if (file == null) {
					r = h.post(api, j2.toString());
				} else {
					r = h.post(api, JSON.create(), j2, "file", file.getName(), file.getInputStream());
				}
				if (r.status == 200) {
					System.out.println("result=" + JSON.fromObject(r.body).toPrettyString());
				} else {
					System.out.println("result=" + r.body);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("cost=" + t.past());

	}

	@Test
	public void testResume() {

		Http h = Http.create();

		String url = "http://g09:8080/f/g/f5sha5luf5qwi3ljnyxwizlnn4xtcltnoa2a/1.mp4";

		Temp.ROOT = "/Users/joe/Downloads/temp";

		try {

			Temp t = h.download(url, true);
			System.out.println("done: " + t.length() + ", file=" + t.getFile().getAbsolutePath());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testFormPost() {
		String cmd = X.IO.read(new File("/Users/joe/workspace/mlp/test/glo.py"), "utf-8");
//		String cmd = "print('a=1')";
		Http h = Http.create();
//		Http.Response r1 = h.get("http://g09:40000/run?cmd=" + Url.encode(cmd));
		Http.Response r1 = h.post("http://joe.mac:40000/run", JSON.create().append("cmd", cmd));
		System.out.println(r1.body);

	}

	@Test
	public void testPostWithFile() {

		String url = "http://g09:8080/f/upload";

		try {
			FileInputStream in = new FileInputStream("/Users/joe/Downloads/hyperic-sigar-1.6.4.tar.gz");

			Http h = Http.create();
			Http.Response r = h.post(url, JSON.create(), JSON.create(), "file", "filename", in, true);

			System.out.println(r.body);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test() {

		Http h = Http.create();
		String api = "http://g09:8080/api/69/1K9Cndodp2zmsQBwuD16";
		JSON body = JSON.create();
		Http.Response r = h.post(api, body);
		System.out.println(r.json().toPrettyString());

	}

	@Test
	public void testDownload1() {
		Http h = Http.create();
		String url = "https://pdc.capub.cn/file/2022/09/16/1f007b62bfc6501321c466048544da71.png";
		Temp t = h.download(url);
		try {
			System.out.println(t.getFile().getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testVideo() {

		System.out.println("start ...");

		Temp.ROOT = "/Users/joe/Downloads/temp/";

		String s = "https://mpvideo.qpic.cn/0b2ebeaceaaayeagm5zonbsvacodeieqaiqa.f10002.mp4?dis_k=bff53759bdcf23d2ab0c19e4a009b517&dis_t=1700128173&play_scene=10120&auth_info=c7GhjaNjNkZWjpeqzHh3LnczOktJMz1LYmAhGDh5E2VVA3duD2trKTQ7AjkaXGAUfQ==&auth_key=2e5879068ac7254084c66548be2624e2&vid=wxv_3153089999635120134&format_id=10002&support_redirect=0&mmversion=false";
		Http h = Http.create();

		try {
			Temp t = h.download(s, JSON.create().append("Referer", "https://mp.weixin.qq.com/"));
			File f1 = t.getFile();
			System.out.println(f1.length());
			System.out.println(f1.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testProxy() {
		
		Config.init();
		String url = "https://m.weibo.cn/status/5044505288120827?sourcetype=weixin&jumpfrom=weibocom";
		Http h = Http.create();
		h = h.proxy("o848.kdltps.com:15818", "t11565336580533", "o8abcm7s1");
		h = h.proxy("172.31.201.12:50002");
		
		Http.Response r = h.get(url);
		System.out.println(r.body);

	}

	@Test
	public void testText() {
		String url = "https://blog.csdn.net/qq_59138417/article/details/121885517";
		String s1 = Http.create().get(url).text();
		System.out.println(s1);
	}

}
