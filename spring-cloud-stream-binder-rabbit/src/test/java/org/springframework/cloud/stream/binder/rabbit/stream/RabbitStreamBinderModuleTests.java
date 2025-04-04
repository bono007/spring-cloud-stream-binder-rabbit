/*
 * Copyright 2021-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.binder.rabbit.stream;

import com.rabbitmq.stream.ConsumerBuilder;
import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.OffsetSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.BinderFactory;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.rabbit.RabbitMessageChannelBinder;
import org.springframework.cloud.stream.binder.rabbit.properties.RabbitConsumerProperties;
import org.springframework.cloud.stream.binder.rabbit.properties.RabbitConsumerProperties.ContainerType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Gary Russell
 */
public class RabbitStreamBinderModuleTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	public void tearDown() {
		if (context != null) {
			context.close();
			context = null;
		}
	}

	@Test
	@Disabled
	public void testStreamContainer() {
		context = new SpringApplicationBuilder(SimpleProcessor.class)
				.web(WebApplicationType.NONE)
				.run("--server.port=0");
		BinderFactory binderFactory = context.getBean(BinderFactory.class);
		RabbitMessageChannelBinder rabbitBinder = (RabbitMessageChannelBinder) binderFactory.getBinder(null,
				MessageChannel.class);
		RabbitConsumerProperties rProps = new RabbitConsumerProperties();
		rProps.setContainerType(ContainerType.STREAM);
		ExtendedConsumerProperties<RabbitConsumerProperties> props =
				new ExtendedConsumerProperties<RabbitConsumerProperties>(rProps);
		props.setAutoStartup(false);
		Binding<MessageChannel> binding = rabbitBinder.bindConsumer("testStream", "grp", new QueueChannel(), props);
		Object container = TestUtils.getPropertyValue(binding, "lifecycle.messageListenerContainer");
		assertThat(container).isInstanceOf(StreamListenerContainer.class);
		((StreamListenerContainer) container).start();
		verify(this.context.getBean(ConsumerBuilder.class)).offset(OffsetSpecification.first());
		((StreamListenerContainer) container).stop();
	}

	@SpringBootApplication
	public static class SimpleProcessor {

//		@Bean
//		public ListenerContainerCustomizer<MessageListenerContainer> containerCustomizer() {
//			return (cont, dest, group) -> {
//				StreamListenerContainer container = (StreamListenerContainer) cont;
//				container.setConsumerCustomizer((name, builder) -> {
//					builder.offset(OffsetSpecification.first());
//				});
//			};
//		}

		@Bean
		Environment env(ConsumerBuilder builder) {
			Environment env = mock(Environment.class);
			given(env.consumerBuilder()).willReturn(builder);
			return env;
		}

		@Bean
		ConsumerBuilder consumerBuilder() {
			return mock(ConsumerBuilder.class);
		}

	}

}
