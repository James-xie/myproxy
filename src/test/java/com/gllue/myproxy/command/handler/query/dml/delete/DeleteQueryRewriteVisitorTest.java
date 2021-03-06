package com.gllue.myproxy.command.handler.query.dml.delete;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gllue.myproxy.command.handler.query.BaseQueryHandlerTest;
import com.gllue.myproxy.command.handler.query.EncryptionHelper;
import com.gllue.myproxy.command.handler.query.EncryptionHelper.EncryptionAlgorithm;
import com.gllue.myproxy.command.handler.query.dml.select.TableScopeFactory;
import com.gllue.myproxy.common.util.RandomUtils;
import com.gllue.myproxy.metadata.model.ColumnMetaData;
import com.gllue.myproxy.metadata.model.ColumnType;
import com.gllue.myproxy.metadata.model.MultiDatabasesMetaData;
import com.gllue.myproxy.metadata.model.TableMetaData;
import com.gllue.myproxy.metadata.model.TableType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeleteQueryRewriteVisitorTest extends BaseQueryHandlerTest {
  private static final String ENCRYPT_KEY = "key";

  DeleteQueryRewriteVisitor newRewriteVisitor(MultiDatabasesMetaData metaData) {
    var factory = new TableScopeFactory(DATASOURCE, DATABASE, metaData);
    return new DeleteQueryRewriteVisitor(
        DATABASE, factory, EncryptionHelper.newEncryptor(EncryptionAlgorithm.AES, ENCRYPT_KEY));
  }

  @Test
  public void testNothingRewriteWithSingleDelete() {
    var table1 = prepareTable("table1", "id", "col1", "col2", "col3");
    var databasesMetaData = prepareMultiDatabasesMetaData(DATASOURCE, DATABASE, table1);
    var rewriter = newRewriteVisitor(databasesMetaData);
    var query =
        "delete from `table1` as `t1` where `t1`.id = 1 and `t1`.col2 = '1234' order by id desc limit 10";
    var stmt = parseDeleteQuery(query);
    stmt.accept(rewriter);
    assertFalse(rewriter.isQueryChanged());
    assertSQLEquals(query, stmt);
  }

  @Test
  public void testNothingRewriteWithMultiDelete() {
    var table1 = prepareTable("table1", "id", "col1", "col2", "col3");
    var databasesMetaData = prepareMultiDatabasesMetaData(DATASOURCE, DATABASE, table1);
    var rewriter = newRewriteVisitor(databasesMetaData);
    var query =
        "delete `t1`, `t2` from `table1` as `t1` "
            + "inner join `table2` as `t2` on t1.id = t2.id "
            + "where `t1`.id = 1 and `t1`.col2 = '1234'";
    var stmt = parseDeleteQuery(query);
    stmt.accept(rewriter);
    assertFalse(rewriter.isQueryChanged());
    assertSQLEquals(query, stmt);
  }

  @Test
  public void testRewriteEncryptColumn() {
    var builder = new TableMetaData.Builder();
    builder
        .setName("table1")
        .setType(TableType.STANDARD)
        .setIdentity(RandomUtils.randomShortUUID())
        .setVersion(1);
    builder.addColumn(new ColumnMetaData.Builder().setName("id").setType(ColumnType.INT).build());
    builder.addColumn(
        new ColumnMetaData.Builder().setName("col1").setType(ColumnType.VARCHAR).build());
    builder.addColumn(
        new ColumnMetaData.Builder().setName("col2").setType(ColumnType.ENCRYPT).build());
    var table1 = builder.build();
    var databasesMetaData = prepareMultiDatabasesMetaData(DATASOURCE, DATABASE, table1);
    var rewriter = newRewriteVisitor(databasesMetaData);
    var query = "delete `table1` from `table1` where table1.id = 1 and table1.col1 = 'abc' and table1.col2 = '1234'";
    var stmt = parseDeleteQuery(query);
    stmt.accept(rewriter);
    assertSQLEquals(
        "DELETE `table1`\n"
            + "FROM `table1`\n"
            + "WHERE table1.id = 1\n"
            + "AND table1.col1 = 'abc'\n"
            + "AND table1.col2 = AES_ENCRYPT('1234', 'key')",
        stmt);
    assertTrue(rewriter.isQueryChanged());
  }

  @Test
  public void testRewritePartitionTableWithSingleDelete() {
    var table1 = preparePartitionTable("table1");
    var databasesMetaData = prepareMultiDatabasesMetaData(DATASOURCE, DATABASE, table1);
    var rewriter = newRewriteVisitor(databasesMetaData);
    var query = "delete from `table1` where table1.id = 1 and table1.col2 = '1234'";
    var stmt = parseDeleteQuery(query);
    stmt.accept(rewriter);
    assertSQLEquals(
        "DELETE `table1`, `$ext_0`\n"
            + "FROM `table1`\n"
            + "LEFT JOIN `db`.`table1_ext_1` `$ext_0` ON `table1`.`$_ext_id` = `$ext_0`.`$_ext_id`\n"
            + "WHERE table1.id = 1\n"
            + "AND `$ext_0`.col2 = AES_ENCRYPT('1234', 'key')",
        stmt);
    assertTrue(rewriter.isQueryChanged());
  }

  @Test
  public void testRewritePartitionTableWithSingleDeleteWithOrderBy() {
    var table1 = preparePartitionTable("table1");
    var databasesMetaData = prepareMultiDatabasesMetaData(DATASOURCE, DATABASE, table1);
    var rewriter = newRewriteVisitor(databasesMetaData);
    var query =
        "delete from `table1` as `t1` where `t1`.id = 1 and `t1`.col2 = '1234' order by id desc limit 10";
    var stmt = parseDeleteQuery(query);
    stmt.accept(rewriter);
    assertSQLEquals(
        "DELETE `t1`, `$ext_0`\n"
            + "FROM `table1` `t1`\n"
            + "  LEFT JOIN `db`.`table1_ext_1` `$ext_0` ON `t1`.`$_ext_id` = `$ext_0`.`$_ext_id`\n"
            + "  INNER JOIN (\n"
            + "    SELECT `t1`.`$_ext_id`\n"
            + "    FROM `table1` `t1`\n"
            + "      LEFT JOIN `db`.`table1_ext_1` `$ext_0` ON `t1`.`$_ext_id` = `$ext_0`.`$_ext_id`\n"
            + "    WHERE `t1`.id = 1\n"
            + "      AND `$ext_0`.col2 = AES_ENCRYPT('1234', 'key')\n"
            + "    ORDER BY id DESC\n"
            + "    LIMIT 10\n"
            + "  ) `$_sub_query`\n"
            + "  ON `t1`.`$_ext_id` = `$_sub_query`.`$_ext_id`",
        stmt);
    assertTrue(rewriter.isQueryChanged());
  }

  @Test
  public void testRewritePartitionTableWithMultiDelete() {
    var table1 = preparePartitionTable("table1");
    var databasesMetaData = prepareMultiDatabasesMetaData(DATASOURCE, DATABASE, table1);
    var rewriter = newRewriteVisitor(databasesMetaData);
    var query =
        "delete `table1`, `table2` from `table1` "
            + "inner join `table2` on `table1`.id = `table2`.id "
            + "where table1.id = 1 and table1.col2 = '1234'";
    var stmt = parseDeleteQuery(query);
    stmt.accept(rewriter);
    assertSQLEquals(
        "DELETE `table1`, `table2`, `$ext_0`\n"
            + "FROM `table1`\n"
            + "  LEFT JOIN `db`.`table1_ext_1` `$ext_0` ON `table1`.`$_ext_id` = `$ext_0`.`$_ext_id`\n"
            + "  INNER JOIN `table2` ON `table1`.id = `table2`.id\n"
            + "WHERE table1.id = 1\n"
            + "  AND `$ext_0`.col2 = AES_ENCRYPT('1234', 'key')",
        stmt);
    assertTrue(rewriter.isQueryChanged());
  }

  @Test
  public void testRewritePartitionTableWithMultiDelete1() {
    var table1 = preparePartitionTable("table1");
    var databasesMetaData = prepareMultiDatabasesMetaData(DATASOURCE, DATABASE, table1);
    var rewriter = newRewriteVisitor(databasesMetaData);
    var query =
        "delete from `db`.`table1`, `db`.`table2` using `db`.`table1` "
            + "inner join `db`.`table2` on `db`.`table1`.`col2` = `db`.`table2`.id "
            + "where `db`.table1.id = 1 and `db`.table1.col2 = '1234'";
    var stmt = parseDeleteQuery(query);
    stmt.accept(rewriter);
    assertSQLEquals(
        "DELETE FROM `db`.`table1`, `db`.`table2`, `$ext_0`\n"
            + "USING `db`.`table1`\n"
            + "  LEFT JOIN `db`.`table1_ext_1` `$ext_0` ON `db`.`table1`.`$_ext_id` = `$ext_0`.`$_ext_id`\n"
            + "  INNER JOIN `db`.`table2` ON `db`.`$ext_0`.`col2` = `db`.`table2`.id\n"
            + "WHERE `db`.table1.id = 1\n"
            + "  AND `db`.`$ext_0`.col2 = AES_ENCRYPT('1234', 'key')",
        stmt);
    assertTrue(rewriter.isQueryChanged());
  }

  @Test
  public void testRewritePartitionTableWithMultiDelete2() {
    var table1 = preparePartitionTable("table1");
    var databasesMetaData = prepareMultiDatabasesMetaData(DATASOURCE, DATABASE, table1);
    var rewriter = newRewriteVisitor(databasesMetaData);
    var query =
        "delete from `db1`.`table1`, `db`.`table2` using `db1`.`table1` "
            + "inner join `db`.`table2` on `db`.`table1`.`col2` = `db`.`table2`.id "
            + "where `db`.table1.id = 1 and `db`.table1.col2 = AES_ENCRYPT('1234', 'key')";
    var stmt = parseDeleteQuery(query);
    stmt.accept(rewriter);
    assertFalse(rewriter.isQueryChanged());
    assertSQLEquals(query, stmt);
  }

  @Test
  public void testRewriteWithSubQuery() {
    var table1 = preparePartitionTable("table1");
    var table2 = preparePartitionTable("table2");
    var databasesMetaData = prepareMultiDatabasesMetaData(DATASOURCE, DATABASE, table1, table2);
    var rewriter = newRewriteVisitor(databasesMetaData);
    var query =
        "delete `t1` from `table1` as `t1`\n"
            + "inner join (\n"
            + "   select id, col3 from `table2` where `col1` = '123'\n"
            + ") `tmp` on `t1`.`id` = `tmp`.`id`\n"
            + "where `t1`.`id` = 1 and `tmp`.`col3` = '1234'";
    var stmt = parseDeleteQuery(query);
    stmt.accept(rewriter);
    assertTrue(rewriter.isQueryChanged());
    assertSQLEquals(
        "DELETE `t1`, `$ext_0`\n"
            + "FROM `table1` `t1`\n"
            + "  LEFT JOIN `db`.`table1_ext_1` `$ext_0` ON `t1`.`$_ext_id` = `$ext_0`.`$_ext_id`\n"
            + "  INNER JOIN (\n"
            + "    SELECT id, col3\n"
            + "    FROM `table2`\n"
            + "    WHERE `col1` = AES_ENCRYPT('123', 'key')\n"
            + "  ) `tmp`\n"
            + "  ON `t1`.`id` = `tmp`.`id`\n"
            + "WHERE `t1`.`id` = 1\n"
            + "  AND `tmp`.`col3` = '1234'",
        stmt);
  }
}
