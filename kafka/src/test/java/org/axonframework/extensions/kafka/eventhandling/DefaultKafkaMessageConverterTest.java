/*
 * Copyright (c) 2010-2018. Axon Framework
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

package org.axonframework.extensions.kafka.eventhandling;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.messaging.MetaData;
import org.axonframework.serialization.FixedValueRevisionResolver;
import org.axonframework.serialization.SerializedObject;
import org.axonframework.serialization.SimpleSerializedType;
import org.axonframework.serialization.xml.XStreamSerializer;
import org.junit.jupiter.api.*;

import static org.apache.kafka.clients.consumer.ConsumerRecord.NULL_SIZE;
import static org.apache.kafka.common.record.RecordBatch.NO_TIMESTAMP;
import static org.apache.kafka.common.record.TimestampType.NO_TIMESTAMP_TYPE;
import static org.axonframework.eventhandling.GenericEventMessage.asEventMessage;
import static org.axonframework.extensions.kafka.eventhandling.HeaderUtils.*;
import static org.axonframework.extensions.kafka.eventhandling.util.HeaderAssertUtil.assertDomainHeaders;
import static org.axonframework.extensions.kafka.eventhandling.util.HeaderAssertUtil.assertEventHeaders;
import static org.axonframework.messaging.Headers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DefaultKafkaMessageConverter}.
 *
 * @author Nakul Mishra
 */
class DefaultKafkaMessageConverterTest {

    private static final String SOME_TOPIC = "topicFoo";
    private static final int SOME_OFFSET = 0;
    private static final int SOME_PARTITION = 0;
    private static final String SOME_AGGREGATE_IDENTIFIER = "1234";

    private DefaultKafkaMessageConverter testSubject;
    private XStreamSerializer serializer;

    private static void assertEventMessage(EventMessage<?> actual, EventMessage<?> expected) {
        assertEquals(expected.getIdentifier(), actual.getIdentifier());
        assertEquals(expected.getPayloadType(), actual.getPayloadType());
        assertEquals(expected.getMetaData(), actual.getMetaData());
        assertEquals(expected.getPayload(), actual.getPayload());
        assertEquals(expected.getTimestamp().toEpochMilli(), actual.getTimestamp().toEpochMilli());
    }

    private static EventMessage<Object> eventMessage() {
        return asEventMessage("SomePayload").withMetaData(MetaData.with("key", "value"));
    }

    private static GenericDomainEventMessage<String> domainMessage() {
        return new GenericDomainEventMessage<>(
                "Stub", SOME_AGGREGATE_IDENTIFIER, 1L, "Payload", MetaData.with("key", "value")
        );
    }

    private static ConsumerRecord<String, byte[]> toReceiverRecord(ProducerRecord<String, byte[]> message) {
        ConsumerRecord<String, byte[]> receiverRecord = new ConsumerRecord<>(
                SOME_TOPIC, SOME_PARTITION, SOME_OFFSET, message.key(), message.value()
        );
        message.headers().forEach(header -> receiverRecord.headers().add(header));
        return receiverRecord;
    }

    @BeforeEach
    void setUp() {
        serializer = XStreamSerializer.builder()
                                      .revisionResolver(new FixedValueRevisionResolver("stub-revision"))
                                      .build();
        testSubject = DefaultKafkaMessageConverter.builder().serializer(serializer).build();
    }

    @Test
    void testKafkaKeyGenerationEventMessageShouldBeNull() {
        ProducerRecord<String, byte[]> evt = testSubject.createKafkaMessage(eventMessage(), SOME_TOPIC);

        assertNull(evt.key());
    }

    @Test
    void testKafkaKeyGenerationDomainMessageShouldBeAggregateIdentifier() {
        ProducerRecord<String, byte[]> domainEvt = testSubject.createKafkaMessage(domainMessage(), SOME_TOPIC);

        assertEquals(domainMessage().getAggregateIdentifier(), domainEvt.key());
    }

    @Test
    void testWritingEventMessageAsKafkaMessageShouldAppendEventHeaders() {
        EventMessage<?> expected = eventMessage();
        ProducerRecord<String, byte[]> senderMessage = testSubject.createKafkaMessage(expected, SOME_TOPIC);
        SerializedObject<byte[]> serializedObject = expected.serializePayload(serializer, byte[].class);

        assertEventHeaders("key", expected, serializedObject, senderMessage.headers());
    }

    @Test
    void testWritingDomainMessageAsKafkaMessageShouldAppendDomainHeaders() {
        GenericDomainEventMessage<String> expected = domainMessage();
        ProducerRecord<String, byte[]> senderMessage = testSubject.createKafkaMessage(expected, SOME_TOPIC);

        assertDomainHeaders(expected, senderMessage.headers());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testReadingMessage_WhenKafkaReturnNullHeaders_ShouldReturnEmptyMessage() {
        ConsumerRecord source = mock(ConsumerRecord.class);
        when(source.headers()).thenReturn(null);

        assertFalse(testSubject.readKafkaMessage(source).isPresent());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testReadingMessageMissingAxonHeaderShouldReturnEmptyMessage() {
        ConsumerRecord msgWithoutHeaders = new ConsumerRecord("foo", 0, 0, "abc", 1);

        assertFalse(testSubject.readKafkaMessage(msgWithoutHeaders).isPresent());
    }

    @Test
    void testReadingMessageWithoutIdShouldReturnEmptyMessage() {
        EventMessage<?> event = eventMessage();
        ProducerRecord<String, byte[]> msg = testSubject.createKafkaMessage(event, SOME_TOPIC);
        msg.headers().remove(MESSAGE_ID);

        assertFalse(testSubject.readKafkaMessage(toReceiverRecord(msg)).isPresent());
    }

    @Test
    void testReadingMessageWithoutTypeShouldReturnEmptyMessage() {
        EventMessage<?> event = eventMessage();
        ProducerRecord<String, byte[]> msg = testSubject.createKafkaMessage(event, SOME_TOPIC);
        msg.headers().remove(MESSAGE_TYPE);

        assertFalse(testSubject.readKafkaMessage(toReceiverRecord(msg)).isPresent());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testReadingMessagePayloadDifferentThanByteShouldReturnEmptyMessage() {
        EventMessage<Object> eventMessage = eventMessage();
        SerializedObject serializedObject = mock(SerializedObject.class);
        when(serializedObject.getType()).thenReturn(new SimpleSerializedType("foo", null));
        Headers headers = toHeaders(eventMessage, serializedObject, byteMapper());
        ConsumerRecord payloadDifferentThanByte = new ConsumerRecord(
                "foo", 0, 0, NO_TIMESTAMP, NO_TIMESTAMP_TYPE,
                -1L, NULL_SIZE, NULL_SIZE, 1, "123", headers
        );

        assertFalse(testSubject.readKafkaMessage(payloadDifferentThanByte).isPresent());
    }

    @Test
    void testWritingEventMessageShouldBeReadAsEventMessage() {
        EventMessage<?> expected = eventMessage();
        ProducerRecord<String, byte[]> senderMessage = testSubject.createKafkaMessage(expected, SOME_TOPIC);
        EventMessage<?> actual = receiverMessage(senderMessage);

        assertEventMessage(actual, expected);
    }

    @Test
    void testWritingEventMessageWithNullRevisionShouldWriteRevisionAsNull() {
        testSubject = DefaultKafkaMessageConverter.builder()
                                                  .serializer(XStreamSerializer.builder().build())
                                                  .build();
        EventMessage<?> eventMessage = eventMessage();
        ProducerRecord<String, byte[]> senderMessage = testSubject.createKafkaMessage(eventMessage, SOME_TOPIC);

        assertNull(valueAsString(senderMessage.headers(), MESSAGE_REVISION));
    }

    @Test
    void testWritingDomainEventMessageShouldBeReadAsDomainMessage() {
        DomainEventMessage<?> expected = domainMessage();
        ProducerRecord<String, byte[]> senderMessage = testSubject.createKafkaMessage(expected, SOME_TOPIC);
        EventMessage<?> actual = receiverMessage(senderMessage);

        assertEventMessage(actual, expected);
        assertDomainMessage((DomainEventMessage<?>) actual, expected);
    }

    private void assertDomainMessage(DomainEventMessage<?> actual, DomainEventMessage<?> expected) {
        assertEquals(expected.getAggregateIdentifier(), actual.getAggregateIdentifier());
        assertEquals(expected.getSequenceNumber(), actual.getSequenceNumber());
        assertEquals(expected.getType(), actual.getType());
    }

    private EventMessage<?> receiverMessage(ProducerRecord<String, byte[]> senderMessage) {
        return testSubject.readKafkaMessage(
                toReceiverRecord(senderMessage)).orElseThrow(() -> new AssertionError("Expected valid message")
        );
    }
}
