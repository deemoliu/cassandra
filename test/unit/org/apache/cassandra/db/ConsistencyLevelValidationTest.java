/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.db;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.SchemaLoader;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.locator.SimpleStrategy;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.SchemaTestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ConsistencyLevelValidationTest
{
    @BeforeClass
    public static void setup()
    {
        DatabaseDescriptor.daemonInitialization();
        SchemaLoader.prepareServer();
        SchemaLoader.createKeyspace("CLValidation", KeyspaceParams.simple(3), SchemaLoader.standardCFMD("CLValidation", "Standard1"));
    }

    @Test
    public void testRemoteQuorumCasCommitRejected()
    {
        Keyspace ks = Keyspace.open("CLValidation");
        try
        {
            ConsistencyLevel.REMOTE_QUORUM.validateForCasCommit(ks.getReplicationStrategy());
            fail("Expected InvalidRequestException");
        }
        catch (InvalidRequestException e)
        {
            // expected
        }
    }

    @Test
    public void testRemoteQuorumCounterRejected()
    {
        try
        {
            ConsistencyLevel.REMOTE_QUORUM.validateCounterForWrite(
                Keyspace.open("CLValidation").getColumnFamilyStore("Standard1").metadata());
            fail("Expected InvalidRequestException");
        }
        catch (InvalidRequestException e)
        {
            // expected
        }
    }

    @Test
    public void testRemoteQuorumSerialRejected()
    {
        // REMOTE_QUORUM is not serial
        assertEquals(false, ConsistencyLevel.REMOTE_QUORUM.isSerialConsistency());
    }

    @Test
    public void testRemoteQuorumBlockForWithSimpleStrategy()
    {
        // With SimpleStrategy RF=3, REMOTE_QUORUM should fallback to quorumFor
        // since getTargetRemoteDcOrLocal returns localDc, and SimpleStrategy
        // delegates to quorumFor
        Keyspace ks = Keyspace.open("CLValidation");
        int blockFor = ConsistencyLevel.REMOTE_QUORUM.blockFor(ks.getReplicationStrategy());
        // SimpleStrategy RF=3: quorum = 3/2+1 = 2
        assertEquals(2, blockFor);
    }

    @Test
    public void testRemoteQuorumFromCode()
    {
        assertEquals(ConsistencyLevel.REMOTE_QUORUM, ConsistencyLevel.fromCode(12));
    }

    @Test
    public void testRemoteQuorumNotDatacenterLocal()
    {
        assertEquals(false, ConsistencyLevel.REMOTE_QUORUM.isDatacenterLocal());
    }
}
