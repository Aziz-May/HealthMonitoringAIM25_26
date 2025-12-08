package tn.supcom.cot.api.mqtt;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jdk.jfr.Event;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
@Startup
public class MQTTService {

    private static final Logger logger = Logger.getLogger(MQTTService.class.getName());

    @Inject
    private SensorRepository sensorRepository;

    @Inject
    private SensorReadingRepository readingRepository;

    @Inject
    @ConfigProperty(name = "mqtt.broker.url")
    private String brokerUrl;

    @Inject
    @ConfigProperty(name = "mqtt.broker.port")
    private Integer brokerPort;

    @Inject
    @ConfigProperty(name = "mqtt.username")
    private String username;

    @Inject
    @ConfigProperty(name = "mqtt.password")
    private String password;

    @Inject
    @ConfigProperty(name = "mqtt.client.id")
    private String clientId;

    @Inject
    @ConfigProperty(name = "mqtt.use.tls", defaultValue = "true")
    private Boolean useTls;

    private Mqtt5AsyncClient mqttClient;

    @Inject
    private Event<SensorReadingEvent> sensorReadingEvent;

    @PostConstruct
    public void init() {
        try {
            logger.info("Initializing MQTT Connection to HiveMQ Cloud Broker...");

            // Build MQTT 5 Client
            var clientBuilder = MqttClient.builder()
                    .useMqttVersion5()
                    .identifier(clientId + "-" + UUID.randomUUID())
                    .serverHost(brokerUrl)
                    .serverPort(brokerPort);

            if (useTls) {
                clientBuilder.sslWithDefaultConfig();
            }

            mqttClient = clientBuilder.buildAsync();

            // Connect with credentials
            mqttClient.connectWith()
                    .simpleAuth()
                    .username(username)
                    .password(UTF_8.encode(password))
                    .applySimpleAuth()
                    .send()
                    .whenComplete((connAck, throwable) -> {
                        if (throwable != null) {
                            logger.severe("Failed to connect to MQTT broker: " + throwable.getMessage());
                        } else {
                            logger.info("Connected to MQTT broker!");
                        }
                    });
        } catch (Exception e) {
            logger.severe("Error initializing MQTT service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Subscribe to topic
    public void subscribeToSensorData() {
        String topic = "sensors/health";

        mqttClient.subscribeWith()
                .topicFilter(topic)
                .callback(this.handleSensorData)
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        logger.severe("Failed to subscribe to topic: '" + topic + "': " + throwable.getMessage());
                    } else {
                        logger.info("Successfully subscribed to topic: '" + topic + "'");
                    }
                });
    }

    // Handle incoming full sensor data payload
    private void handleSensorData(Mqtt5Publish publish) {
        try {
            String payload = new String(publish.getPayloadAsBytes(), UTF_8);
            logger.info("Received payload: " + payload);

            JsonReader reader = Json.createReader(new StringReader(payload));
            JsonObject json = reader.readObject();

            SensorData data = new SensorData();

            // Device
            String deviceId = "esp8266";

            // Temperature
            if (json.containsKey("temperature"))
                data.setTemperature(json.getJsonNumber("temperature").doubleValue());

            // Heart rate
            if (json.containsKey("bpm"))
                data.setHeartRate(json.getJsonNumber("bpm").intValue());

            saveReading(deviceId, data);
        } catch (Exception e) {
            logger.severe("Error processing sensors/health message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Save reading in DB
    private void saveReading(String deviceId, SensorData data) {
        try {
            Sensor sensor = null;
            try {
                sensor = sensorRepository.findByDeviceId(deviceId).orElse(null);
            } catch (Exception e) {
                logger.warning("Sensor not found for deviceId: " + deviceId);
            }

            SensorReading reading = new SensorReading();
            reading.setId(UUID.randomUUID().toString());
            reading.setSensorId(sensor != null ? sensor.getId() : deviceId);
            reading.setTimestamp(LocalDateTime.now());
            reading.setData(data);

            readingRepository.save(reading);
            logger.info("✅ Saved reading for device " + deviceId);

            if (sensor != null) {
                sensor.setLastConnection(LocalDateTime.now());
                sensor.setStatus("active");
                sensorRepository.save(sensor);
            }
            // Fire CDI event asynchronously
            sensorReadingEvent.fireAsync(new SensorReadingEvent(reading, "MQTT"));
            logger.info("Fired SensorReadingEvent for reading: " + reading.getId());

        } catch (Exception e) {
            logger.severe("Failed to save reading: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Publish message
    public void publishMessage(String topic, String message) {
        if (mqttClient != null && mqttClient.getState().isConnected()) {
            mqttClient.publishWith()
                    .topic(topic)
                    .payload(message.getBytes(StandardCharsets.UTF_8))
                    .send()
                    .whenComplete((ack, throwable) -> {
                        if (throwable != null) {
                            logger.severe("Failed to publish message: " + throwable.getMessage());
                        } else {
                            logger.info("Published message to topic: " + topic);
                        }
                    });
        } else {
            logger.warning("MQTT client not connected; message not sent");
        }
    }

    @PreDestroy
    public void cleanup() {
        if (mqttClient != null && mqttClient.getState().isConnected()) {
            mqttClient.disconnect();
            logger.info("MQTT client disconnected");
        }
    }

}
