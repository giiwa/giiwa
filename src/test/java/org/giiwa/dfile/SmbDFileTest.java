package org.giiwa.dfile;

import org.giiwa.bean.Disk;
import org.junit.Test;

public class SmbDFileTest {

	@Test
	public void test() {

		try {

			String url = "smb://g10";
			String path = "/disk2/";

			Disk d = new Disk();
			d.url = url;
			d.domain = "";
			d.username = "root";
			d.password = "xxxx";
			d.path = path;

			DFile f1 = SmbDFile.create(d, "/temp/g");
			System.out.println("exists=" + f1.exists());
			DFile[] ff = f1.list();
			for (DFile e : ff) {
				System.out.println("e=" + e.getFilename() + ", name=" + e.getName());
				DFile[] f2 = e.list();
				for (DFile e2 : f2) {
					System.out.println("e2=" + e2.getFilename() + ", name2=" + e2.getName());
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testDelete() {

		try {

			String url = "smb://g30";
			String path = "/disk2/";

			Disk d = new Disk();
			d.url = url;
			d.domain = "WORKGROUP";
			d.username = "root";
			d.password = "xxxx";
			d.path = path;
			d.mount = "/temp/";

			{
				DFile f1 = SmbDFile.create(d, "/temp/o/H/f/A/5864974855987695344/");
				DFile f2 = SmbDFile.create(d,
						"/temp/o/H/f/A/5864974855987695344/640");

				System.out.println("f1=" + f1.getFilename() + ", name=" + f1.getName() + ", exists=" + f1.exists()
						+ ", size=" + f1.length());
//			}
//			if (f1.delete()) {
//				System.out.println("deleted");
//			}

				DFile[] ff = f1.list();
				for (DFile e : ff) {
					System.out.println("e=" + ((SmbDFile) e).file + ", exists=" + e.exists() + ", size=" + e.length());
					if (e.isDirectory()) {
						DFile[] ff1 = e.list();
						for (DFile e2 : ff1) {
							System.out.println("e2=" + e2.getFilename() + ", name2=" + e2.getName());
						}
					} else if (e.isFile()) {
						if (e.delete()) {
							System.out.println("deleted success.");
						} else {
							System.err.println("deleted failed.");
						}
					}
				}

				System.out.println("f2=" + f1.getFilename() + ", name=" + f2.getName() + ", exists=" + f2.exists()
						+ ", size=" + f1.length());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
