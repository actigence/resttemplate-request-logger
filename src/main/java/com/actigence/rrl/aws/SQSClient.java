/*
 * MIT License
 *
 * Copyright (c) 2020. Actigence Solutions
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.actigence.rrl.aws;

import com.actigence.rrl.dto.OutboundRequestLog;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;

@Slf4j
public class SQSClient {
    private static final String QUEUE_NAME_ENV = "AAL_QUEUE_NAME";
    private static final String QUEUE_NAME_SYS_PROP = "aal_queue_name";
    private static final String CLIENT_ID_ENV = "AAL_CLIENT_ID";
    private static final String CLIENT_ID_SYS_PROP = "aal.client_id";
    private static final String DEFAULT_QUEUE_NAME = "aal_outbound_request_logging_queue";

    private final Gson gson;
    private final String queueUrl;
    private final AmazonSQS sqs;

    public SQSClient() {
        sqs = initSQSClient();
        queueUrl = initQueue();
        gson = new Gson();
    }

    /**
     * Publish message to Api Access Logger SQS queue
     *
     * @param message
     */
    public void publish(OutboundRequestLog message) {
        message.setClientId(getClientId());

        SendMessageRequest send_msg_request = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(gson.toJson(message));
        sqs.sendMessage(send_msg_request);
        log.debug("Message sent successfully: {}", message.getId());
    }

    private AmazonSQS initSQSClient() {
        AmazonSQS sqs;
        sqs = AmazonSQSClientBuilder.defaultClient();
        return sqs;
    }

    private String initQueue() {
        String queueUrl;
        final String queueName = getQueueName();
        log.debug("Connecting to SQS queue name: {}", queueName);


        try {
            CreateQueueResult create_result = sqs.createQueue(queueName);
        } catch (AmazonSQSException e) {
            if (!e.getErrorCode().equals("QueueAlreadyExists")) {
                log.error("Error creating queue with name: {}", queueName);
                throw e;
            } else {
                log.debug("Queue already exists with name: {}", queueName);
            }
        }

        queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
        log.debug("Connected to SQS queue url: {}", queueUrl);
        return queueUrl;
    }

    private String getQueueName() {
        return ofNullable(getenv(QUEUE_NAME_ENV))
                .orElseGet(() -> ofNullable(getProperty(QUEUE_NAME_SYS_PROP))
                        .orElse(DEFAULT_QUEUE_NAME));
    }

    private String getClientId() {
        return ofNullable(getenv(CLIENT_ID_ENV))
                .orElseGet(() -> ofNullable(getProperty(CLIENT_ID_SYS_PROP))
                        .orElse(null));
    }
}
