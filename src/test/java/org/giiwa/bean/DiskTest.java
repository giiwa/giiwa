package org.giiwa.bean;

import org.giiwa.dfile.DFile;
import org.giiwa.misc.Base32;
import org.junit.Test;

public class DiskTest {

	@Test
	public void test() {
		String s = "/f/g/f5sha5luf4zdamrrf4ytclzrgaxtcmbpnfwwox3lfzyg4zy/img_k.png";

		try {

			System.out.println(new String(Base32.decode("f5sha5luf4zdamrrf4ytclzrgaxtcmbpnfwwox3lfzyg4zy")));

			DFile f1 = Disk.seek(s);
			System.out.println(f1.getFilename());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
