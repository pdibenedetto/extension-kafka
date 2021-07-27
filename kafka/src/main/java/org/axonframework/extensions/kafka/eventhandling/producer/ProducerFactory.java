/*
 * Copyright (c) 2010-2021. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.extensions.kafka.eventhandling.producer;

import org.apache.kafka.clients.producer.Producer;

/**
 * A functional interface towards building {@link Producer} instances.
 *
 * @param <K> the key type of a build {@link Producer} instance
 * @param <V> the value type of a build {@link Producer} instance
 * @author Nakul Mishra
 * @author Steven van Beelen
 * @since 4.0
 */
public interface ProducerFactory<K, V> {

    /**
     * Create a {@link Producer}.
     *
     * @return a {@link Producer}
     */
    Producer<K, V> createProducer();

    /**
     * The {@link ConfirmationMode} all created {@link Producer} instances should comply to. Defaults to {@link
     * ConfirmationMode#NONE}.
     *
     * @return the configured confirmation mode
     */
    default ConfirmationMode confirmationMode() {
        return ConfirmationMode.NONE;
    }

    /**
     * Closes all {@link Producer} instances created by this factory.
     */
    void shutDown();
}
