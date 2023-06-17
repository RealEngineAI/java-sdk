# RealEngine AI Java SDK

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/RealEngineAI/java-sdk/blob/main/LICENSE)

Java-based client library for [RealEngine AI API](https://www.realengine.ai/api.html).

## Prerequisites
- Java 11 or newer
- API token from the [RealEngine AI Dashboard](https://app.realengine.ai).

## Installation

### Maven

Add the following dependency to your pom.xml file:

```xml
<dependency>
    <groupId>ai.realengine</groupId>
    <artifactId>realengine</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

Add the following dependency to your build.gradle file:

```groovy
implementation 'ai.realengine:realengine:1.0.0'
```

## Usage

Here's a simple example of how to use the client:

```java
RealEngineAIClient client = RealEngineAIClient.newBuilder()
        .setToken("PASTE YOUR TOKEN HERE")
        .build();

CompletableFuture<String> caption = client.getCaption("http://link.to/image.jpg")
```

## Exception Handling

This library includes the RealEngineAIException class for error handling. 
This custom exception includes a unique error ID that can be used to identify the error by RealEngine AI support team. 


## Contributing

Contributions are welcome! 

### License

This project is licensed under the terms of the MIT license. See the [License](https://github.com/RealEngineAI/java-sdk/blob/main/LICENSE)
file for details.