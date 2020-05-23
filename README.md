# RestTemplate Request Logger
![Java CI with Maven](https://github.com/actigence/resttemplate-request-logger/workflows/Java%20CI%20with%20Maven/badge.svg)

This package can be used to log request and response details of API calls made using Spring RestTemplates. 
The logs are stored in AWS DynamoDB using [API Access Tracker (Backend)](https://github.com/actigence/api-access-tracker-backend).
Please visit [API Access Tracker (Backend)](https://github.com/actigence/api-access-tracker-backend) to setup
AWS stack before using this package for logging your requests.

This package, provides a simple RestTemplate interceptor, to send request and response data to AWS stack using AWS SQS.

# Usage
1. Setup your AWS stack as shown in [API Access Tracker (Backend)](https://github.com/actigence/api-access-tracker-backend).

2. Include this package into your build:
* For Maven builds add below dependency to your pom.xml.
```xml
<dependency>
    <groupId>com.actigence</groupId>
    <artifactId>resttemplate-request-logger</artifactId>
    <version>0.0.1</version>
</dependency>
```

* For Gradle add below dependency to your `build.gradle` file
```xml
compile group: 'com.actigence', name: 'resttemplate-request-logger', version: '0.0.1'
```

3. To add RestTemplate interceptor to your project, create the RestTemplate bean as shown below.
```java
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(singletonList(new OutboundRequestTrackingInterceptor()));
        return restTemplate;
    }
}
```

4. Now whenever you access any URL using this RestTemplate bean. 
The request will be seamlessly intercepted by the `OutboundRequestTrackingInterceptor` class and an event will be 
published to the AWS SQS for storing that request and response. 
```java
Quote quote = restTemplate.getForObject(
        "https://gturnquist-quoters.cfapps.io/api/random", Quote.class);
```

5. See [resttemplate-request-logger-demo](https://github.com/actigence/resttemplate-request-logger-demo) for full example code.