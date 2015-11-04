/*
 *      Copyright (C) 2012-2015 DataStax Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.core;

import java.util.Collection;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import static com.datastax.driver.core.Assertions.assertThat;
import static com.datastax.driver.core.DataType.*;
import static com.datastax.driver.core.DataTypeParser.parse;
public class DataTypeParserTest extends CCMBridge.PerClassSingleNodeCluster {

    @Test(groups = "short")
    public void should_parse_native_types() {
        assertThat(parse("ascii", cluster, null, null, false)).isEqualTo(ascii());
        assertThat(parse("bigint", cluster, null, null, false)).isEqualTo(bigint());
        assertThat(parse("blob", cluster, null, null, false)).isEqualTo(blob());
        assertThat(parse("boolean", cluster, null, null, false)).isEqualTo(cboolean());
        assertThat(parse("counter", cluster, null, null, false)).isEqualTo(counter());
        assertThat(parse("decimal", cluster, null, null, false)).isEqualTo(decimal());
        assertThat(parse("double", cluster, null, null, false)).isEqualTo(cdouble());
        assertThat(parse("float", cluster, null, null, false)).isEqualTo(cfloat());
        assertThat(parse("inet", cluster, null, null, false)).isEqualTo(inet());
        assertThat(parse("int", cluster, null, null, false)).isEqualTo(cint());
        assertThat(parse("text", cluster, null, null, false)).isEqualTo(text());
        assertThat(parse("varchar", cluster, null, null, false)).isEqualTo(varchar());
        assertThat(parse("timestamp", cluster, null, null, false)).isEqualTo(timestamp());
        assertThat(parse("date", cluster, null, null, false)).isEqualTo(date());
        assertThat(parse("time", cluster, null, null, false)).isEqualTo(time());
        assertThat(parse("uuid", cluster, null, null, false)).isEqualTo(uuid());
        assertThat(parse("varint", cluster, null, null, false)).isEqualTo(varint());
        assertThat(parse("timeuuid", cluster, null, null, false)).isEqualTo(timeuuid());
        assertThat(parse("tinyint", cluster, null, null, false)).isEqualTo(tinyint());
        assertThat(parse("smallint", cluster, null, null, false)).isEqualTo(smallint());
    }

    @Test(groups = "short")
    public void should_ignore_whitespace() {
        assertThat(parse("  int  ", cluster, null, null, false)).isEqualTo(cint());
        assertThat(parse("  set < bigint > ", cluster, null, null, false)).isEqualTo(set(bigint()));
        assertThat(parse("  map  <  date  ,  timeuuid  >  ", cluster, null, null, false)).isEqualTo(map(date(), timeuuid()));
    }

    @Test(groups = "short")
    public void should_ignore_case() {
        assertThat(parse("INT", cluster, null, null, false)).isEqualTo(cint());
        assertThat(parse("SET<BIGint>", cluster, null, null, false)).isEqualTo(set(bigint()));
        assertThat(parse("FROZEN<mAp<Date,Tuple<timeUUID>>>", cluster, null, null, false)).isEqualTo(map(date(), cluster.getMetadata().newTupleType(timeuuid()), true));
    }

    @Test(groups = "short")
    public void should_parse_collection_types() {
        assertThat(parse("list<int>", cluster, null, null, false)).isEqualTo(list(cint()));
        assertThat(parse("set<bigint>", cluster, null, null, false)).isEqualTo(set(bigint()));
        assertThat(parse("map<date,timeuuid>", cluster, null, null, false)).isEqualTo(map(date(), timeuuid()));
    }

    @Test(groups = "short")
    public void should_parse_frozen_collection_types() {
        assertThat(parse("frozen<list<int>>", cluster, null, null, false)).isEqualTo(list(cint(), true));
        assertThat(parse("frozen<set<bigint>>", cluster, null, null, false)).isEqualTo(set(bigint(), true));
        assertThat(parse("frozen<map<date,timeuuid>>", cluster, null, null, false)).isEqualTo(map(date(), timeuuid(), true));
    }

    @Test(groups = "short")
    public void should_parse_nested_collection_types() {
        assertThat(parse("list<list<int>>", cluster, null, null, false)).isEqualTo(list(list(cint())));
        assertThat(parse("set<list<frozen<map<bigint,varchar>>>>", cluster, null, null, false)).isEqualTo(set(list(map(bigint(), varchar(), true))));
    }

    @Test(groups = "short")
    public void should_parse_tuple_types() {
        assertThat(parse("tuple<int,list<text>>", cluster, null, null, false)).isEqualTo(cluster.getMetadata().newTupleType(cint(), list(text())));
    }

    @Test(groups = "short")
    public void should_parse_user_defined_types() {
        Metadata metadata = cluster.getMetadata();
        KeyspaceMetadata keyspaceMetadata = metadata.getKeyspace(this.keyspace);
        UserType a = keyspaceMetadata.getUserType("\"A\"");
        UserType b = keyspaceMetadata.getUserType("\"B\"");
        UserType c = keyspaceMetadata.getUserType("\"C\"");
        UserType d = keyspaceMetadata.getUserType("\"D\"");
        UserType e = keyspaceMetadata.getUserType("\"E\"");
        UserType f = keyspaceMetadata.getUserType("\"F\"");
        UserType g = keyspaceMetadata.getUserType("\"G\"");
        UserType h = keyspaceMetadata.getUserType("\"H\"");

        assertThat(a).isNotNull().isFrozen();
        assertThat(b).isNotNull().isFrozen();
        assertThat(c).isNotNull().isFrozen();
        assertThat(d).isNotNull().isFrozen();
        assertThat(e).isNotNull().isFrozen();
        assertThat(f).isNotNull().isFrozen();
        assertThat(g).isNotNull().isFrozen();
        assertThat(h).isNotNull().isFrozen();

        assertThat(a).hasField("f1", c);
        assertThat(b).hasField("f1", set(d));
        assertThat(c).hasField("f1", map(e, d));
        assertThat(d).hasField("f1", metadata.newTupleType(f, g, h));
        assertThat(e).hasField("f1", list(g));
        assertThat(f).hasField("f1", h);
        assertThat(g).hasField("f1", cint());
        assertThat(h).hasField("f1", cint());
    }

    @Override
    protected Collection<String> getTableDefinitions() {
        return Lists.newArrayList(
            /*
            Creates the following acyclic graph (edges directed upwards
            meaning "depends on"):

                H   G
               / \ /\
              F   |  E
               \ /  /
                D  /
               / \/
              B  C
                 |
                 A

             Topological sort order should be : GH,FE,D,BC,A
             */
            String.format("CREATE TYPE %s.\"H\" (f1 int)", keyspace),
            String.format("CREATE TYPE %s.\"G\" (f1 int)", keyspace),
            String.format("CREATE TYPE %s.\"F\" (f1 frozen<\"H\">)", keyspace),
            String.format("CREATE TYPE %s.\"E\" (f1 frozen<list<\"G\">>)", keyspace),
            String.format("CREATE TYPE %s.\"D\" (f1 frozen<tuple<\"F\",\"G\",\"H\">>)", keyspace),
            String.format("CREATE TYPE %s.\"C\" (f1 frozen<map<\"E\",\"D\">>)", keyspace),
            String.format("CREATE TYPE %s.\"B\" (f1 frozen<set<\"D\">>)", keyspace),
            String.format("CREATE TYPE %s.\"A\" (f1 frozen<\"C\">)", keyspace)
        );
    }
    
}
