/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.persistence.payload;

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.xodus.EnvironmentUtil;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Brandl
 */
public class PublishPayloadXodusLocalPersistenceTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private LocalPersistenceFileUtil localPersistenceFileUtil;
    private PersistenceStartup persistenceStartup;
    private PublishPayloadXodusLocalPersistence persistence;

    @Before
    public void before() throws Exception {
        localPersistenceFileUtil = mock(LocalPersistenceFileUtil.class);
        InternalConfigurations.PERSISTENCE_CLOSE_RETRIES.set(3);
        InternalConfigurations.PERSISTENCE_CLOSE_RETRY_INTERVAL.set(5);
        InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.set(8);
        when(localPersistenceFileUtil.getVersionedLocalPersistenceFolder(anyString(), anyString())).thenReturn(
                temporaryFolder.newFolder());

        persistenceStartup = new PersistenceStartup();

        persistence = new PublishPayloadXodusLocalPersistence(localPersistenceFileUtil,
                new EnvironmentUtil(),
                persistenceStartup);
        persistence.start();
    }

    @After
    public void cleanUp() throws Exception {
        persistence.closeDB();
        persistenceStartup.finish();
    }

    @Test
    public void test_add_get_payload() {

        final byte[] payload1 = "payload".getBytes();
        final byte[] payload2 = "payload".getBytes();

        persistence.put(0L, payload1);
        persistence.put(1L, payload2);

        final byte[] result1 = persistence.get(0L);
        final byte[] result2 = persistence.get(1L);

        assertArrayEquals(result1, payload1);
        assertArrayEquals(result2, payload2);
    }

    @Test
    public void test_add_remove_get_payload() {

        final byte[] payload1 = "payload".getBytes();
        final byte[] payload2 = "payload".getBytes();

        persistence.put(0L, payload1);
        persistence.put(1L, payload2);

        persistence.remove(1L);

        final byte[] result1 = persistence.get(0L);
        final byte[] result2 = persistence.get(1L);

        assertArrayEquals(result1, payload1);
        assertNull(result2);
    }

    @Test
    public void test_add_get_big_payload() {

        final byte[] payload1 = "payload".getBytes();
        final byte[] payload2 = RandomStringUtils.random(10 * 1024 * 1024 + 100, true, true).getBytes();

        persistence.put(0L, payload1);
        persistence.put(1L, payload2);

        final byte[] result1 = persistence.get(0L);
        final byte[] result2 = persistence.get(1L);

        assertArrayEquals(result1, payload1);
        assertArrayEquals(result2, payload2);
    }

    @Test
    public void test_add_remove_get_big_payload() {

        final byte[] payload1 = "payload".getBytes();
        final byte[] payload2 = RandomStringUtils.random(10 * 1024 * 1024 + 100, true, true).getBytes();

        persistence.put(0L, payload1);
        persistence.put(1L, payload2);

        persistence.remove(1L);

        final byte[] result1 = persistence.get(0L);
        final byte[] result2 = persistence.get(1L);

        assertArrayEquals(result1, payload1);
        assertNull(result2);
    }

    @Test
    public void test_get_all_ids() {

        final byte[] payload1 = "payload".getBytes();

        persistence.put(0L, payload1);
        persistence.put(1L, payload1);
        persistence.put(2L, payload1);

        persistence.remove(1L);

        final List<Long> allIds = persistence.getAllIds();
        assertEquals(2, allIds.size());
        assertFalse(allIds.contains(1L));
    }

    @Test
    public void init() {
        final int highestPayloadId = 123456789;
        persistence.put(highestPayloadId, new byte[]{1, 2, 3});
        persistence.stop();
        final PublishPayloadXodusLocalPersistence newPersistence = new PublishPayloadXodusLocalPersistence(
                localPersistenceFileUtil,
                new EnvironmentUtil(),
                persistenceStartup);
        newPersistence.start();
        assertTrue(PUBLISH.PUBLISH_COUNTER.get() > highestPayloadId);
        newPersistence.stop();
    }
}