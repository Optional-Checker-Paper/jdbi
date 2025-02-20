/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.sqlobject;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.transaction.SerializableTransactionRunner;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.BatchChunkSize;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.Collections.emptySet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

public class TestBatching {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());
    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
    }

    @Test
    public void testInsertSingleIterable() {
        UsesBatching b = handle.attach(UsesBatching.class);
        List<Something> toInsert = Arrays.asList(new Something(1, "Tom"), new Something(2, "Tatu"));
        int[] counts = b.insertBeans(toInsert);

        assertThat(counts).containsExactly(1, 1);
        assertThat(b.size()).isEqualTo(2);
    }

    @Test
    public void testInsertSingleIteratorNoTx() {
        UsesBatching b = handle.attach(UsesBatching.class);
        List<Something> toInsert = Arrays.asList(new Something(1, "Tom"), new Something(2, "Tatu"));
        int[] counts = b.insertBeansNoTx(toInsert.iterator());

        assertThat(counts).containsExactly(1, 1);
        assertThat(b.size()).isEqualTo(2);
    }

    @Test
    public void testBindConstantNull() {
        UsesBatching b = handle.attach(UsesBatching.class);
        List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);

        b.withConstantValue(ids, null);

        assertThat(b.size()).isEqualTo(5);

        List<String> names = handle.createQuery("select distinct name from something")
                                   .mapTo(String.class)
                                   .list();
        assertThat(names).containsExactly((String) null);
    }

    @Test
    public void testBindConstantValue() {
        UsesBatching b = handle.attach(UsesBatching.class);
        List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);

        b.withConstantValue(ids, "Johan");

        assertThat(b.size()).isEqualTo(5);

        List<String> names = handle.createQuery("select distinct name from something")
                                   .mapTo(String.class)
                                   .list();
        assertThat(names).containsExactly("Johan");
    }

    @Test
    public void testZipping() {
        UsesBatching b = handle.attach(UsesBatching.class);
        List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);
        List<String> names = Arrays.asList("David", "Tim", "Mike");

        b.zipArgumentsTogether(ids, names);

        assertThat(b.size()).isEqualTo(3);

        List<String> insNames = handle.createQuery("select distinct name from something order by name")
                                       .mapTo(String.class)
                                       .list();
        assertThat(insNames).containsExactly("David", "Mike", "Tim");
    }

    @Test
    public void testChunkedBatching() {
        UsesBatching b = handle.attach(UsesBatching.class);
        List<Something> things = Arrays.asList(new Something(1, "Brian"),
                                               new Something(2, "Henri"),
                                               new Something(3, "Patrick"),
                                               new Something(4, "Robert"),
                                               new Something(5, "Maniax"));
        int[] counts = b.insertChunked(things);
        assertThat(counts).hasSize(5).containsOnly(1);
    }

    @Test
    public void testChunkedBatchingOnParam() {
        UsesBatching b = handle.attach(UsesBatching.class);
        List<Something> things = Arrays.asList(new Something(1, "Brian"),
            new Something(2, "Henri"),
            new Something(3, "Patrick"),
            new Something(4, "Robert"),
            new Something(5, "Maniax"));
        int[] counts = b.insertChunked(3, things);
        assertThat(counts).hasSize(5).containsOnly(1);
    }

    @Test
    @Timeout(5)
    public void testNoIterable() {
        BadBatch b = handle.attach(BadBatch.class);
        assertThatThrownBy(() -> b.insertBeans(new Something(1, "x"))).isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    @Timeout(5)
    public void testNoParameterAtAll() {
        BadBatch b = handle.attach(BadBatch.class);
        assertThatThrownBy(b::insertBeans).isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    @Timeout(5)
    public void testForgotIterableInt() {
        handle.execute("CREATE TABLE test (id int)");
        UsesBatching b = handle.attach(UsesBatching.class);
        assertThatThrownBy(() -> b.invalidInsertInt(1)).isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    @Timeout(5)
    public void testForgotIterableString() {
        handle.execute("CREATE TABLE test (id varchar)");
        UsesBatching b = handle.attach(UsesBatching.class);
        assertThatThrownBy(() -> b.invalidInsertString("bob")).isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testEmptyBatch() {
        handle.execute("CREATE TABLE test (id varchar)");
        UsesBatching b = handle.attach(UsesBatching.class);
        assertThat(b.insertBeans(emptySet())).isEmpty();
    }

    @Test
    public void testBooleanReturn() {
        BooleanBatchDao dao = handle.attach(BooleanBatchDao.class);
        assertThat(dao.insert(new Something(1, "foo"), new Something(2, "bar"))).containsExactly(true, true);
        assertThat(dao.update(new Something(1, "baz"), new Something(3, "buz"))).containsExactly(true, false);
    }

    @Test
    public void testBatchingWithSerializableTransactionRunner() {
        Jdbi jdbi = h2Extension.getJdbi();
        jdbi.setTransactionHandler(new SerializableTransactionRunner());

        jdbi.useHandle(jdbiHandle -> {
            Handle spyHandle = spy(jdbiHandle);
            given(spyHandle.commit()).willAnswer(invocation -> {
                throw new SQLException("Test exception", "40001");
            }).willCallRealMethod();

            UsesBatching b = spyHandle.attach(UsesBatching.class);
            List<Something> things = Arrays.asList(new Something(1, "Brian"),
                new Something(2, "Henri"),
                new Something(3, "Patrick"),
                new Something(4, "Robert"),
                new Something(5, "Maniax"));
            int[] counts = b.insertChunked(2, things);
            assertThat(counts).hasSize(5).containsOnly(1);
        });
    }

    @BatchChunkSize(4)
    @RegisterRowMapper(SomethingMapper.class)
    public interface UsesBatching {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        int[] insertBeans(@BindBean Iterable<Something> elements);

        @SqlBatch(value = "insert into something (id, name) values (:id, :name)", transactional = false)
        int[] insertBeansNoTx(@BindBean Iterator<Something> elements);

        @SqlBatch("insert into something (id, name) values (:id, :name)")
        int[] withConstantValue(@Bind("id") Iterable<Integer> ids, @Bind("name") String name);

        @SqlBatch("insert into something (id, name) values (:id, :name)")
        int[] zipArgumentsTogether(@Bind("id") Iterable<Integer> ids, @Bind("name") List<String> name);

        @SqlBatch("insert into something (id, name) values (:it.id, :it.name)")
        @BatchChunkSize(2)
        int[] insertChunked(@BindBean("it") Iterable<Something> its);

        @SqlBatch("insert into something (id, name) values (:it.id, :it.name)")
        int[] insertChunked(@BatchChunkSize int size, @BindBean("it") Iterable<Something> its);

        @SqlQuery("select count(*) from something")
        int size();

        @SqlBatch("insert into test (id) values (:id)")
        void invalidInsertInt(@Bind("id") int id);

        @SqlBatch("insert into test (id) values (:id)")
        void invalidInsertString(@Bind("id") String id);
    }

    public interface BadBatch {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        int[] insertBeans(@BindBean Something elements); // whoops, no Iterable!

        @SqlBatch("insert into something (id, name) values (0, '')")
        int[] insertBeans(); // whoops, no parameters at all!
    }

    public interface BooleanBatchDao {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        boolean[] insert(@BindBean Something... values);

        @SqlBatch("update something set name = :name where id = :id")
        boolean[] update(@BindBean Something... values);
    }
}
