/*
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.confluent.kafkarest.integration;

import static io.confluent.kafkarest.TestUtils.assertErrorResponse;

import io.confluent.kafkarest.Errors;
import io.confluent.kafkarest.Versions;
import io.confluent.kafkarest.entities.BinaryConsumerRecord;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;

public class SimpleConsumerBinaryTest extends AbstractConsumerTest {

  private static final String topicName = "topic1";

  private final List<ProducerRecord<byte[], byte[]>> recordsOnlyValues = Arrays.asList(
      new ProducerRecord<byte[], byte[]>(topicName, "value".getBytes()),
      new ProducerRecord<byte[], byte[]>(topicName, "value2".getBytes()),
      new ProducerRecord<byte[], byte[]>(topicName, "value3".getBytes()),
      new ProducerRecord<byte[], byte[]>(topicName, "value4".getBytes())
  );

  private final List<ProducerRecord<byte[], byte[]>> recordsWithKeys = Arrays.asList(
      new ProducerRecord<byte[], byte[]>(topicName, "key".getBytes(), "value".getBytes()),
      new ProducerRecord<byte[], byte[]>(topicName, "key".getBytes(), "value2".getBytes()),
      new ProducerRecord<byte[], byte[]>(topicName, "key".getBytes(), "value3".getBytes()),
      new ProducerRecord<byte[], byte[]>(topicName, "key".getBytes(), "value4".getBytes())
  );

  private static final GenericType<List<BinaryConsumerRecord>> binaryConsumerRecordType
      = new GenericType<List<BinaryConsumerRecord>>() {
  };

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    final int numPartitions = 1;
    final int replicationFactor = 1;
    createTopic(topicName, numPartitions, (short) replicationFactor);
  }

  @Test
  public void testConsumeOnlyValuesByOffset() {
    produceBinaryMessages(recordsOnlyValues);

    simpleConsumeMessages(
        topicName,
        0,
        null, // No "count" parameter in the query
        recordsOnlyValues.subList(0, 1), // We expect only the first record in the response
        Versions.KAFKA_V1_JSON_BINARY,
        Versions.KAFKA_V1_JSON_BINARY,
        binaryConsumerRecordType,
        null
        );
  }

  @Test
  public void testConsumeWithKeysByOffset() {
    produceBinaryMessages(recordsWithKeys);

    simpleConsumeMessages(
        topicName,
        0,
        null, // No "count" parameter in the query
        recordsWithKeys.subList(0, 1),
        Versions.KAFKA_V1_JSON_BINARY,
        Versions.KAFKA_V1_JSON_BINARY,
        binaryConsumerRecordType,
        null
    );
  }

  @Test
  public void testConsumeOnlyValuesByOffsetAndCount() {
    produceBinaryMessages(recordsOnlyValues);

    simpleConsumeMessages(
        topicName,
        0,
        recordsOnlyValues.size(),
        recordsOnlyValues,
        Versions.KAFKA_V1_JSON_BINARY,
        Versions.KAFKA_V1_JSON_BINARY,
        binaryConsumerRecordType,
        null
    );
  }

  @Test
  public void testConsumeWithKeysByOffsetAndCount() {
    produceBinaryMessages(recordsWithKeys);

    simpleConsumeMessages(
        topicName,
        0,
        recordsWithKeys.size(),
        recordsWithKeys,
        Versions.KAFKA_V1_JSON_BINARY,
        Versions.KAFKA_V1_JSON_BINARY,
        binaryConsumerRecordType,
        null
    );
  }

  @Test(timeout = 10000)
  public void testConsumeMoreMessagesThanAvailable() {
    produceBinaryMessages(recordsOnlyValues);

    simpleConsumeMessages(
        topicName,
        0,
        recordsOnlyValues.size()+1, // Ask for more than there is
        recordsOnlyValues,
        Versions.KAFKA_V1_JSON_BINARY,
        Versions.KAFKA_V1_JSON_BINARY,
        binaryConsumerRecordType,
        null
    );
  }

  @Test
  public void testConsumeInvalidTopic() {

    Response response = request(
        "/topics/nonexistenttopic/partitions/0/messages",
        Collections.singletonMap("offset", "0")
    ).accept(Versions.KAFKA_V1_JSON_BINARY).get();

    assertErrorResponse(Response.Status.NOT_FOUND, response,
        Errors.TOPIC_NOT_FOUND_ERROR_CODE,
        Errors.TOPIC_NOT_FOUND_MESSAGE,
        Versions.KAFKA_V1_JSON_BINARY);
  }

  @Test
  public void testConsumeInvalidPartition() {

    Response response = request(
        "/topics/topic1/partitions/1/messages",
        Collections.singletonMap("offset", "0")
    ).accept(Versions.KAFKA_V1_JSON_BINARY).get();

    assertErrorResponse(Response.Status.NOT_FOUND, response,
        Errors.PARTITION_NOT_FOUND_ERROR_CODE,
        Errors.PARTITION_NOT_FOUND_MESSAGE,
        Versions.KAFKA_V1_JSON_BINARY);
  }

}
