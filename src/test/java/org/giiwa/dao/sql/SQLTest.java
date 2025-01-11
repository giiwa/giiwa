package org.giiwa.dao.sql;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.giiwa.bean.Stat;
import org.giiwa.dao.Helper.W;
import org.giiwa.web.Language;
import org.junit.Test;

public class SQLTest {

	@Test
	public void testLongTerm() {
		try {
			test2("(type='NEWS' or type='1') and cipid='4210E47EACE64B7E8235045CC1DA47B0'");
//			test2("type='NEWS' and (cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0')");
//			test2("cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0'");
//			test2("type='NEWS' and (cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0' or cipid='4210E47EACE64B7E8235045CC1DA47B0')");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testNot() {
		try {
			test2("not a=1");
			test2("not a='11'");

			W q = W.create();
			q.and("not (not a=1)").sort("created");
			System.out.println(q);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSort() {
		try {
			W q = W.create().sort("a");
			q.and("not (not a=1)").sort("created");
			System.out.println(q);

			System.out.println(q.copy());

			q = SQL.where("a>1 and b<10 order by a");
			System.out.println(q);

			q = SQL.where("order by a");
			System.out.println(q);

			q = SQL.where("order by a desc");
			System.out.println(q);

			q = SQL.where("order by a desc, b, c desc");
			System.out.println(q);

			q = W.create();
			q.and("updated> 0 order by name");
			System.out.println(q);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testAnd() {
		try {
			test("select a,b from y1_data where a_1=1 and b=2");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test() {

		try {

			Stat.lang = Language.getLanguage("zh_cn");

			test("select 1");
			test("select now()");
			test("select uuid()");

			test("select * from table1");
			test("select a,b,c from table1");
			test("select a,b,c from sc.table1");
			test("select * from table1 where (a=1) and d='2'");
			test("select * from table1 where a=1 and d='2'");
			test("select * from table1 where a=1 and d=='2'");
			test("select * from table1 where a.keyword=1 and d='2' group by a.keyword");
			test("select * from table1 where a=1 or b='2'");
			test("select * from table1 where a=1 and (b='2' or b='1')");
			test("select * from table1 where a=1 and b='2|1'");
			test("select * from table1 where a>=1 and a<=1 and a=1 and a like '1'");
			test("select * from table1 where todate=1");
			test("select * from table1 where a=today('yyyy')");
			test("select * from table1 where a=today('yyyy')-2");
			test("select * from table1 where a=today('yyyyMMdd')-7");
			test("select * from table1 where a=uuid('" + UUID.randomUUID() + "')");
			test("select * from table1 where a=todate(20220101, 'yyyyMMdd')");
			test("select * from table1 where a=todate(today())");
			test("select * from table1 where a=todate(today('yyyyMMdd')-7, 'yyyyMMdd')");
			test("select * from table1 where a=tostring(1,2,3,4|5)");
			test("select * from table1 where a=tostring(today('yyyyMMdd') - 7)");
			test("select * from table1 where a=todate(20220101-7, 'yyyyMMdd')");
			test("select * from table1 where a=todate(today()-7d)");
			test("select * from table1 where a>(today()-7d)");

			test("select * from table1 where (a='1|2' and b = '1')");
			test("select * from table1 where (c = null and d like 'a' and f=1|2)");
			test("select * from table1 where (a='1|2' and b = '1') or (c = null and d like 'a' and f=1|2)");

			test("select a,b,c from table1 where (a='1|2' and b = '1') or (c = null and d like 'a' and f=1|2) group by a order by a,b offset 1 limit 10");

			test("from table1 where (a='1|2' and b = '1')");
			test("a from table1 where (a='1|2' and b = '1')");

			test2("code='002163' and name='海南发展'");
			test2("(is_military=-2 or is_military=-1) and type='NEWS'");

			test("select * from table1 where \\from=1");

			test2("code like ''");

			test2("_vindexed_filename=1");

			test2("updated_time > todate(today('yyyyMMdd') - 10, 'yyyyMMdd')");

			test2("a='NUIYU\\'");

			test2("a='NU|IYU'");

			test2("a='NU'|'IYU'");

			test2("status = 1");

			test2("a like '1'");

			test2("a !like '1'");

			test2("a not like '1'");

			test2("a=1 and (not a=1 and b=2)");

			test2("a=1 and not (a=1 and b=2)");

			test2("not a=1 and a=2");

			test2("not a=1 or a=2");

			test2("a!=1");

			test2("not a=1");

			test("select * from y1_data");

			test("select * from y1_data where a_1=1 and b=2");

			test("select * from y1_data where type='IMAGE' and f_tag='ys'");

			test("select * from y1_data where type='IMAGE' and f_tag='ys' and created > today() and upload_status=1");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void test(String sql) throws SQLException {
		System.out.println(sql);
		W q = SQL.parse(sql);
		System.out.println("\t" + q.command);
		System.out.println("\t" + q.params);
		System.out.println("\t" + q);
		System.out.println("\ttable=" + q.table());
		System.out.println("\tfield=" + q.fields());
	}

	private void test2(String sql) throws SQLException {
		System.out.println(sql);
		W q = SQL.where(sql);
		System.out.println("\t" + q);
		System.out.println("\ttable=" + q.table());
		System.out.println("\tfield=" + q.fields());
		System.out.println("\tmongo=" + q.query());
	}

}
