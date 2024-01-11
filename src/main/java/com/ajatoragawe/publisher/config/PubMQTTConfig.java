package com.ajatoragawe.publisher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import com.fazecast.jSerialComm.SerialPort;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class PubMQTTConfig {
    @Bean
    ApplicationRunner triggerMqttMessage(MqttPahoMessageHandler outboundAdapter) {
        return args -> {
            try {
                sendMqttMessage(outboundAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    private void sendMqttMessage(MqttPahoMessageHandler outboundAdapter) throws Exception {
        System.out.println("List COM ports");
        SerialPort comPorts[] = SerialPort.getCommPorts();
        for (int i = 0; i < comPorts.length; i++)
            System.out.println("comPorts[" + i + "] = " + comPorts[i].getDescriptivePortName());
        int port = 0; // array index to select COM port
        comPorts[port].openPort();
        System.out.println("open port comPorts[" + port + "]  " + comPorts[port].getDescriptivePortName());
        comPorts[port].setBaudRate(9600);
        try {
            while (true) {
                Thread.sleep(100);
                byte[] readBuffer = new byte[comPorts[port].bytesAvailable()];
                int numRead = comPorts[port].readBytes(readBuffer, readBuffer.length);
                String data = new String(readBuffer, 0, numRead);
                // System.out.println("data : " + data);
                if (data.equals("")) {
                } else {
                    String[] parts = data.split(" ");

                    String timestamp = parts[0].trim();
                    String topic = parts[1].trim();
                    String mssg = parts[2].trim();

                    var message = MessageBuilder.withPayload(timestamp+";"+mssg).build();
                    outboundAdapter.setDefaultTopic(topic);
                    outboundAdapter.handleMessage(message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // comPorts[port].closePort();

    }

    @Bean
    MqttPahoMessageHandler outboundAdapter(MqttPahoClientFactory factory) {
        var mh = new MqttPahoMessageHandler("producer", factory);
        return mh;
    }

    @Bean
    IntegrationFlow outboundFlow(MessageChannel out,
            MqttPahoMessageHandler outboundAdapter) {
        return IntegrationFlow
                .from(out)
                .handle(outboundAdapter)
                .get();
    }

    @Bean
    MessageChannel out() {
        return MessageChannels.direct().getObject();
    }
}
