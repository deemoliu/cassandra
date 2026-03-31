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

package org.apache.cassandra.locator;

import java.util.function.Predicate;

import org.apache.cassandra.utils.FBUtilities;

import static org.apache.cassandra.config.DatabaseDescriptor.getEndpointSnitch;

public class InRemoteDc
{
    private static ReplicaTester replicas;
    private static EndpointTester endpoints;

    final String dc;
    final IEndpointSnitch snitch;

    private InRemoteDc(String dc, IEndpointSnitch snitch)
    {
        this.dc = dc;
        this.snitch = snitch;
    }

    boolean stale()
    {
        String targetDc = FBUtilities.getTargetRemoteDcOrLocal();
        return dc == null
                || !dc.equals(targetDc)
                || snitch != getEndpointSnitch();
    }

    private static final class ReplicaTester extends InRemoteDc implements Predicate<Replica>
    {
        private ReplicaTester(String dc, IEndpointSnitch snitch)
        {
            super(dc, snitch);
        }

        @Override
        public boolean test(Replica replica)
        {
            return dc != null && dc.equals(snitch.getDatacenter(replica.endpoint()));
        }
    }

    private static final class EndpointTester extends InRemoteDc implements Predicate<InetAddressAndPort>
    {
        private EndpointTester(String dc, IEndpointSnitch snitch)
        {
            super(dc, snitch);
        }

        @Override
        public boolean test(InetAddressAndPort endpoint)
        {
            return dc != null && dc.equals(snitch.getDatacenter(endpoint));
        }
    }

    public static Predicate<Replica> replicas()
    {
        ReplicaTester cur = replicas;
        if (cur == null || cur.stale())
            replicas = cur = new ReplicaTester(FBUtilities.getTargetRemoteDcOrLocal(), getEndpointSnitch());
        return cur;
    }

    public static Predicate<InetAddressAndPort> endpoints()
    {
        EndpointTester cur = endpoints;
        if (cur == null || cur.stale())
            endpoints = cur = new EndpointTester(FBUtilities.getTargetRemoteDcOrLocal(), getEndpointSnitch());
        return cur;
    }

    public static boolean isInRemoteDc(Replica replica)
    {
        return replicas().test(replica);
    }

    public static boolean isInRemoteDc(InetAddressAndPort endpoint)
    {
        return endpoints().test(endpoint);
    }
}
