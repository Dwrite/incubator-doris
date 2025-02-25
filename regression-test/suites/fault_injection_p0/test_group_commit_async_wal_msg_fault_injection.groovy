// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

suite("test_group_commit_async_wal_msg_fault_injection","nonConcurrent") {


    def tableName = "wal_test"
 
    // test successful group commit async load
    sql """ DROP TABLE IF EXISTS ${tableName} """

    sql """
        CREATE TABLE IF NOT EXISTS ${tableName} (
            `k` int ,
            `v` int ,
        ) engine=olap
        DISTRIBUTED BY HASH(`k`) 
        BUCKETS 5 
        properties("replication_num" = "1")
        """

    GetDebugPoint().clearDebugPointsForAllBEs()

    def exception = false;
        try {
            GetDebugPoint().enableDebugPointForAllBEs("LoadBlockQueue._finish_group_commit_load.get_wal_back_pressure_msg")
            streamLoad {
                table "${tableName}"
                set 'column_separator', ','
                set 'group_commit', 'async_mode'
                unset 'label'
                file 'group_commit_wal_msg.csv'
                time 10000 
            }
            assertFalse(true);
        } catch (Exception e) {
            logger.info(e.getMessage())
            assertTrue(e.getMessage().contains('pre allocated 0 Bytes'))
            exception = true;
        } finally {
            GetDebugPoint().disableDebugPointForAllBEs("LoadBlockQueue._finish_group_commit_load.get_wal_back_pressure_msg")
            assertTrue(exception)
        }

    // test failed group commit async load
    sql """ DROP TABLE IF EXISTS ${tableName} """

    sql """
        CREATE TABLE IF NOT EXISTS ${tableName} (
            `k` int ,
            `v` int ,
        ) engine=olap
        DISTRIBUTED BY HASH(`k`) 
        BUCKETS 5 
        properties("replication_num" = "1")
        """

    GetDebugPoint().clearDebugPointsForAllBEs()

    exception = false;
        try {
            GetDebugPoint().enableDebugPointForAllBEs("LoadBlockQueue._finish_group_commit_load.get_wal_back_pressure_msg")
            GetDebugPoint().enableDebugPointForAllBEs("LoadBlockQueue._finish_group_commit_load.err_status")
            streamLoad {
                table "${tableName}"
                set 'column_separator', ','
                set 'group_commit', 'async_mode'
                unset 'label'
                file 'group_commit_wal_msg.csv'
                time 10000 
            }
            assertFalse(true);
        } catch (Exception e) {
            logger.info(e.getMessage())
            assertTrue(e.getMessage().contains('pre allocated 0 Bytes'))
            exception = true;
        } finally {
            GetDebugPoint().disableDebugPointForAllBEs("LoadBlockQueue._finish_group_commit_load.get_wal_back_pressure_msg")
            GetDebugPoint().disableDebugPointForAllBEs("LoadBlockQueue._finish_group_commit_load.err_status")
            assertTrue(exception)
        }

}