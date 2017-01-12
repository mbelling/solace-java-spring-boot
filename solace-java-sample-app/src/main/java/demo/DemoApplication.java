/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package demo;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.SpringJCSMPFactory;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageProducer;

@SpringBootApplication 
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}


	@Component
	static class Runner implements CommandLineRunner {

		private static final Logger logger = LoggerFactory.getLogger(Runner.class);

		private final Topic topic = JCSMPFactory.onlyInstance().createTopic("tutorial/topic");
		
		@Autowired
		private SpringJCSMPFactory solaceFactory;
		
		private DemoMessageConsumer msgConsumer = new DemoMessageConsumer();
		private DemoPublishEventHandler pubEventHandler = new DemoPublishEventHandler();
        
		
		@Override
		public void run(String... strings) throws Exception {
		    final String msg = "Hello World";
		    final JCSMPSession session = solaceFactory.createSession();

            XMLMessageConsumer cons = session.getMessageConsumer(msgConsumer);
            
            session.addSubscription(topic);
            logger.info("Connected. Awaiting message...");
            cons.start();
            // Consumer session is now hooked up and running!

            /** Anonymous inner-class for handling publishing events */
            XMLMessageProducer prod = session.getMessageProducer(pubEventHandler);
            // Publish-only session is now hooked up and running!

            TextMessage jcsmpMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            jcsmpMsg.setText(msg);
            
            logger.info("============= Sending " + msg);
            prod.send(jcsmpMsg,topic);
            
            try {
                msgConsumer.getLatch().await(10, TimeUnit.SECONDS); // block here until message received, and latch will flip
            } catch (InterruptedException e) {
                logger.error("I was awoken while waiting");
            }
            // Close consumer
            cons.close();
            logger.info("Exiting.");
            session.closeSession();
		}
	}
}



