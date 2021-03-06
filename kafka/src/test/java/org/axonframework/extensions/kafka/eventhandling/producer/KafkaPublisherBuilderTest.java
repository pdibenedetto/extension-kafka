/*
 * Copyright (c) 2010-2018. Axon Framework
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

package org.axonframework.extensions.kafka.eventhandling.producer;

import org.axonframework.common.AxonConfigurationException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link KafkaPublisher.Builder}.
 *
 * @author Nakul Mishra
 */
class KafkaPublisherBuilderTest {

    @Test
    void testConfiguringInvalidProducerFactory() {
        assertThrows(AxonConfigurationException.class, () -> KafkaPublisher.builder().producerFactory(null).build());
    }

    @Test
    void testConfiguringInvalidMessageConverter() {
        assertThrows(AxonConfigurationException.class, () -> KafkaPublisher.builder().messageConverter(null).build());
    }

    @Test
    void testConfiguringInvalidMessageMonitor() {
        assertThrows(AxonConfigurationException.class, () -> KafkaPublisher.builder().messageMonitor(null).build());
    }

    @Test
    void testConfiguringInvalidKafkaTopic() {
        assertThrows(AxonConfigurationException.class, () -> KafkaPublisher.builder().topic(null).build());
    }

    @Test
    void testConfiguringInvalidAckTimeout() {
        assertThrows(AxonConfigurationException.class, () -> KafkaPublisher.builder().publisherAckTimeout(-12).build());
    }
}