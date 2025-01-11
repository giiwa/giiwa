package org.giiwa.sso;

import org.giiwa.json.JSON;
import org.giiwa.net.client.Http;
import org.junit.Test;

public class Sso {

	@Test
	public void ssolink() {

		JSON j1 = JSON.create();

		j1.append("appid", "demo");
		j1.append("secret", "s1UIuUPIuCpDxxeITHJgwFOxhoLm3mvc");
		j1.append("name", "admin");
		j1.append("url", "/dput/#/data-copy/editor?id=67&iframe=1");

		Http.Response r = Http.create().post("http://g07:8080/user/ssolink", j1);
		System.out.println(r.body);
		j1 = r.json();
		System.out.println(j1.getString("url"));

		String s = "mfyha2lehvsgk3lpez2g623fny6vu2ctknihovdwnvvvgolci42gezlhnvrgystwjrdwm5ckpivvs6bxojtfendkknwwsu3bkbjtmu3pizvfuqkijncfcutfnf2u6msljathk4tmhusterteob2xijjsiystemzfgjdgiylumewwg33qpestertfmruxi33seuzum2leeuzuinrxeuzdm2lgojqw2zjfgncdc";

	}

}
