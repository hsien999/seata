/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.undo.oceanbaseoracle;

import com.alibaba.druid.mock.MockPreparedStatement;
import io.seata.rm.datasource.mock.MockConnection;
import io.seata.rm.datasource.mock.MockDriver;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.BaseExecutorTest;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test cases for undo-insert executor of OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleUndoInsertExecutorTest extends BaseExecutorTest {
    private static OceanBaseOracleUndoInsertExecutor EXECUTOR;
    private static final String TABLE_NAME = "TABLE_NAME";
    private static final String ID_NAME = "ID";
    private static final String AGE_NAME = "AGE";

    @BeforeAll
    public static void init() {
        TableMeta tableMeta = mock(TableMeta.class);
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Collections.singletonList(ID_NAME));
        when(tableMeta.getTableName()).thenReturn(TABLE_NAME);

        // build before image
        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName(TABLE_NAME);
        beforeImage.setTableMeta(tableMeta);

        List<Row> beforeRows = new ArrayList<>();
        beforeImage.setRows(beforeRows);

        // build after image
        TableRecords afterImage = new TableRecords();
        afterImage.setTableName(TABLE_NAME);
        afterImage.setTableMeta(tableMeta);

        List<Row> afterRows = new ArrayList<>();
        afterImage.setRows(afterRows);

        Row row2 = new Row();
        addField(row2, ID_NAME, 1, "1");
        addField(row2, AGE_NAME, 1, "a");
        afterRows.add(row2);

        Row row3 = new Row();
        addField(row3, ID_NAME, 1, "2");
        addField(row3, AGE_NAME, 1, "b");
        afterRows.add(row3);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.INSERT);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName(TABLE_NAME);
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        EXECUTOR = new OceanBaseOracleUndoInsertExecutor(sqlUndoLog);
    }

    @Test
    public void testBuildUndoSQL() {
        String sql = EXECUTOR.buildUndoSQL();
        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("DELETE"));
        Assertions.assertTrue(sql.contains(TABLE_NAME));
        Assertions.assertTrue(sql.contains(ID_NAME));
        Assertions.assertEquals("DELETE FROM TABLE_NAME WHERE ID = ? ", sql.toUpperCase());
    }

    @Test
    public void testGetUndoRows() {
        Assertions.assertEquals(EXECUTOR.getUndoRows(), EXECUTOR.getSqlUndoLog().getAfterImage());
    }

    @Test
    public void testUndoPrepare() throws SQLException {
        String sql = EXECUTOR.buildUndoSQL().toUpperCase();
        try (MockConnection conn = new MockConnection(new MockDriver(), "jdbc:mock:xxx", null);
             MockPreparedStatement undoPST = (MockPreparedStatement) conn.prepareStatement(sql)) {
            List<Field> fieldList = new ArrayList<>();
            fieldList.add(new Field(ID_NAME, 1, "1"));
            EXECUTOR.undoPrepare(undoPST, new ArrayList<>(), fieldList);
            Assertions.assertEquals(Collections.singletonList("1"), undoPST.getParameters());
        }
    }
}
