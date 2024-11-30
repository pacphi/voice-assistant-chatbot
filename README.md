# Voice Assistant ChatBot

This is a Spring Boot application that serves as a voice-enabled chatbot assistant. 
It uses [Spring AI](https://docs.spring.io/spring-ai/reference/index.html) to integrate with OpenAI and to leverages its [Audio generation](https://platform.openai.com/docs/guides/audio) features to process voice inputs, and respond with audio outputs. 
The application uses, plain, Java's Sound API for audio recording and playback.

By default, the assistant impersonates Marvin

<img src="doc/marvin-transparent.svg" width="200" align="center"/>

> Marvin - a Paranoid Android, the highly intelligent yet perpetually depressed and pessimistic robot in the Universe. 
> With a brain the size of a planet but endlessly underwhelmed and irritated by the menial tasks given to him... 

## Features

- **Voice Input and Output**: Communicates using recorded voice input and generates audio responses.
- **Chat Memory**: Maintains context using in-memory chat memory.
- **System Prompt**: Configurable system prompt to define the chatbot's behavior.
- **Spring AI Integration**: Utilizes Spring AI's [ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html) to interact with a chat model.

## Requirements

- **Java**: Java 17 or higher.
- **Spring Boot**: Version 3.2.x or higher.
- **Dependencies**: `spring-ai-openai-spring-boot-starter`

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/your-repo/assistant-application.git
cd assistant-application
```

### Configuration

```properties
spring.main.web-application-type=none

# System prompt message
audio.chatbot.prompt=classpath:/marvin.paranoid.android.txt

# OpenAI API key
spring.ai.openai.api-key=${OPENAI_API_KEY}

# Set the OpenAI model
spring.ai.openai.chat.options.model=gpt-4o-audio-preview

# Output audio configuration
spring.ai.openai.chat.options.output-modalities=text,audio
spring.ai.openai.chat.options.output-audio.voice=ONYX
spring.ai.openai.chat.options.output-audio.format=WAV
```

### Build the Application

```bash
./mvnw clean install
```

### Run the Application

```bash
java -jar ./target/voice-assistant-chatbot-0.0.1-SNAPSHOT.jar
```

### Interacting with the Assistant

1. Speak your query when prompted.
2. Press `Enter` to stop recording.
3. Listen to the assistant’s response, which will be played back.

Press `Crl+C` to exit. 

## Code Overview

The application consists of two classes the `VoiceAssistantApplication.java` and the utility `Audio`.

The [VoiceAssistantApplication.java](https://github.com/tzolov/voice-assistant-chatbot/blob/main/src/main/java/spring/ai/demo/ai/marvin/VoiceAssistantApplication.java) is the  main class initializes the chatbot with:

1. **ChatClient**: Configures the chatbot using the system prompt and an in-memory chat memory advisor.
2. **Command Line Runner**: Implements a loop to continuously record, process, and respond to user input.
3. **Audio Recording and Playback**: Manages voice input and output using the `Audio` utility for recording audio input from the user and playing back audio responses.

The [Audio](https://github.com/tzolov/voice-assistant-chatbot/blob/main/src/main/java/spring/ai/demo/ai/marvin/Audio.java) is simple utility that uses the pain `Java Sound API` for capturing and playback audio. 
It supports the WAV audio format and stores the recordings in a temporary file `AudioRecordBuffer.wav`. 
Note that it is not thread-safe and is intended for demonstration purposes.
