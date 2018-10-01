package org.giiwa.core.base;

import java.util.Base64;

import org.giiwa.core.bean.UID;
import org.giiwa.core.json.JSON;
import org.junit.Test;

public class RSATest {

	@Test
	public void test() {
		RSA.Key k = RSA.generate(1024);
		System.out.println("pubkey:" + k.pub_key);
		System.out.println("prikey:" + k.pri_key);

		try {
			k.pub_key = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCk5240ahtufYmmgosXIJL/0E9R3eqqdpV+N5xMHMKsSYtP3aRVA2piYAeVXLHo7V88hrLoy7tuEnpqkAkWcMeaiZSJ4VUmyx8O3eiqX4pbLMz0tf3o0Lbrr0K2F2xo1HUUXyaAcsh+zj4vcICkGr6loIhUaWB6VsLOjagioH+2lQIDAQAB";
			k.pri_key = "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAKTnbjRqG259iaaCixcgkv/QT1Hd6qp2lX43nEwcwqxJi0/dpFUDamJgB5VcsejtXzyGsujLu24SemqQCRZwx5qJlInhVSbLHw7d6KpfilsszPS1/ejQtuuvQrYXbGjUdRRfJoByyH7OPi9wgKQavqWgiFRpYHpWws6NqCKgf7aVAgMBAAECgYAPBEQ04bS9yxDN6PVhlcXNJdsTSXIlM8dJRyckhznzFn9pNnXQiA8YBkaqg1ZwL62MhXelW2gmDei364XoVTzX4FXgswU1lqP1kNpij0HzI1+vCdkskdmhYiJfmLLBQmIOIDl84Qz+517+q9z2mHDOL9BAom8l6h+j0LGmmKYKAQJBANjN8Nvq0dMmRcjTfrw1e0cMJhLpARKQnNauh4MKj/SZTDzhVpVgG0dfW03KHComZA2YWfcpWtT53wDTdAY25P0CQQDCt3RDnh+c95NXqJWa5vWhr4uniJ6E6yUpZ38XXqy6p5ldp267WqoH/XRFN3R9qSFKQPIYUNhq5dmSxfTaQdd5AkEA0vZaB/SP5fGY+BjseoFkCrdEmSQHeiQjqTa9AAMYHv/IUnlGgiW5hZLBSol/LHic5Sm5aSrhitn1aF1Zye5ClQJBALhcwzSSrxQMRfIlqSE1tTUV5YEHFjB8BH+jgu45sgo5TjkiovG58mwGSuSOkTm5vc90lsT3JzJv1wXlcOSGmuECQQDWOONn1DA0KNJiDrJv6I6kcDVGVPr05obOWFvxE3GKPkjuADDNwD6L+cnhJiXmqVAlLBF/8yVkLt2iZ0mjfoGp";

			JSON jo = JSON.create();
			jo.append("x", 1).append("company", "太原交警总队").append("email", "aaaa@g.com").append("limited", 500);

			System.out.println("---- encoding ----");
			String code = UID.random(32);
			System.out.println(code);
			String s1 = Base64.getEncoder().encodeToString(RSA.encode(code.getBytes(), k.pub_key));
			System.out.println(s1);
			String s2 = Base64.getEncoder().encodeToString(Digest.aes_encrypt(jo.toString().getBytes(), code));
			System.out.println(s2);

			code = new String(RSA.decode(Base64.getDecoder().decode(s1), k.pri_key));
			System.out.println(code);
			String content = new String(Digest.aes_decrypt(Base64.getDecoder().decode(s2), code));
			System.out.println(content);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
