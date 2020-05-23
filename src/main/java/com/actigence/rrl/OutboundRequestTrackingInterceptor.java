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

package com.actigence.rrl;

import com.actigence.rrl.aws.SQSClient;
import com.actigence.rrl.dto.NameValue;
import com.actigence.rrl.dto.OutboundRequestLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * RestTemplate interceptor class to publish events to AWS hosted backend
 */
@Slf4j
public class OutboundRequestTrackingInterceptor implements ClientHttpRequestInterceptor {

    private final SQSClient sqsClient = new SQSClient();

    /**
     * Main interceptor method
     *
     * @param request
     * @param body
     * @param execution
     * @return
     * @throws IOException
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ZonedDateTime startTime = ZonedDateTime.now();
        ClientHttpResponse response = execution.execute(request, body);

        Optional<OutboundRequestLog> requestLogOpt = createEventObject(request, body, response, startTime);
        if (requestLogOpt.isPresent()) {
            OutboundRequestLog requestLog = requestLogOpt.get();
            response.getHeaders().add("acs-log-id", requestLog.getId());
            sqsClient.publish(requestLog);
        }

        return response;
    }

    /**
     * Create log object
     *
     * @param request
     * @param requestBody
     * @param response
     * @param startTime
     * @return
     */
    private Optional<OutboundRequestLog> createEventObject(HttpRequest request,
                                                           byte[] requestBody,
                                                           ClientHttpResponse response,
                                                           ZonedDateTime startTime) {
        try {
            return ofNullable(OutboundRequestLog.builder()
                    .startTime(startTime.toString())
                    .endTime(ZonedDateTime.now().toString())
                    .requestUri(request.getURI().toString())
                    .requestMethod(request.getMethodValue())
                    .requestHeaders(requestHeaders(request))
                    .requestBody(new String(requestBody))
                    .responseStatusCode(response.getRawStatusCode())
                    .responseStatusText(response.getStatusText())
                    .responseHeaders(responseHeaders(response))
                    .responseBody(StreamUtils.copyToString(response.getBody(), Charset.defaultCharset()))
                    .build());
        } catch (IOException e) {
            log.error("Unable to publish tracking event", e);
        }
        return Optional.empty();
    }

    /**
     * Extract request headers.
     *
     * @param request
     * @return
     */
    private List<NameValue> requestHeaders(HttpRequest request) {
        HttpHeaders requestHeaders = request.getHeaders();
        return requestHeaders.keySet()
                .stream()
                .map(it -> new NameValue(it, requestHeaders.get(it)))
                .collect(toList());
    }

    /**
     * Extract response headers
     *
     * @param response
     * @return
     */
    private List<NameValue> responseHeaders(ClientHttpResponse response) {
        HttpHeaders requestHeaders = response.getHeaders();
        return requestHeaders.keySet()
                .stream()
                .map(it -> new NameValue(it, requestHeaders.get(it)))
                .collect(toList());
    }
}
