/*  Getting_BPM_to_Monitor prints the BPM to the Serial Monitor, using the least lines of code and PulseSensor Library.
 *  Now also reads DS18B20 temperature sensor for testing.
 *  Send to NodeRed via HiveMQ Cloud with TLS (ESP8266 compatible)
 */

#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <PulseSensorPlayground.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <time.h>

// ----- WIFI Settings -----
const char* ssid = "TOPNET_6C60";
const char* password = "fdynaaf7fs";

// -------- MQTT (HiveMQ Cloud) --------
const char* mqtt_server = "11d8d8afaa9742b4acb75d6c20a70d65.s1.eu.hivemq.cloud";
const int mqtt_port = 8883;
const char* mqtt_user = "esp8266";
const char* mqtt_password = "Esp8266?";
const char* mqtt_topic = "sensors/health";

// HiveMQ Cloud Let's Encrypt CA certificate (hardcoded)
static const char ca_cert[] PROGMEM = R"EOF(
-----BEGIN CERTIFICATE-----
MIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw
TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh
cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4
WhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu
ZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBY
MTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rVygc
h77ct984kIxuPOZXoHj3dcKi/vVqbvYATyjb3miGbESTtrFj/RQSa78f0uoxmyF+
0TM8ukj13Xnfs7j/EvEhmkvBioZxaUpmZmyPfjxwv60pIgbz5MDmgK7iS4+3mX6U
A5/TR5d8mUgjU+g4rk8Kb4Mu0UlXjIB0ttov0DiNewNwIRt18jA8+o+u3dpjq+sW
T8KOEUt+zwvo/7V3LvSye0rgTBIlDHCNAymg4VMk7BPZ7hm/ELNKjD+Jo2FR3qyH
B5T0Y3HsLuJvW5iB4YlcNHlsdu87kGJ55tukmi8mxdAQ4Q7e2RCOFvu396j3x+UC
B5iPNgiV5+I3lg02dZ77DnKxHZu8A/lJBdiB3QW0KtZB6awBdpUKD9jf1b0SHzUv
KBds0pjBqAlkd25HN7rOrFleaJ1/ctaJxQZBKT5ZPt0m9STJEadao0xAH0ahmbWn
OlFuhjuefXKnEgV4We0+UXgVCwOPjdAvBbI+e0ocS3MFEvzG6uBQE3xDk3SzynTn
jh8BCNAw1FtxNrQHusEwMFxIt4I7mKZ9YIqioymCzLq9gwQbooMDQaHWBfEbwrbw
qHyGO0aoSCqI3Haadr8faqU9GY/rOPNk3sgrDQoo//fb4hVC1CLQJ13hef4Y53CI
rU7m2Ys6xt0nUW7/vGT1M0NPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNV
HRMBAf8EBTADAQH/MB0GA1UdDgQWBBR5tFnme7bl5AFzgAiIyBpY9umbbjANBgkq
hkiG9w0BAQsFAAOCAgEAVR9YqbyyqFDQDLHYGmkgJykIrGF1XIpu+ILlaS/V9lZL
ubhzEFnTIZd+50xx+7LSYK05qAvqFyFWhfFQDlnrzuBZ6brJFe+GnY+EgPbk6ZGQ
3BebYhtF8GaV0nxvwuo77x/Py9auJ/GpsMiu/X1+mvoiBOv/2X/qkSsisRcOj/KK
NFtY2PwByVS5uCbMiogziUwthDyC3+6WVwW6LLv3xLfHTjuCvjHIInNzktHCgKQ5
ORAzI4JMPJ+GslWYHb4phowim57iaztXOoJwTdwJx4nLCgdNbOhdjsnvzqvHu7Ur
TkXWStAmzOVyyghqpZXjFaH3pO3JLF+l+/+sKAIuvtd7u+Nxe5AW0wdeRlN8NwdC
jNPElpzVmbUq4JUagEiuTDkHzsxHpFKVK7q4+63SM1N95R1NbdWhscdCb+ZAJzVc
oyi3B43njTOQ5yOf+1CceWxG1bQVs5ZufpsMljq4Ui0/1lvh+wjChP4kqKOJ2qxq
4RgqsahDYVvTH9w7jXbyLeiNdd8XM2w9U/t7y0Ff/9yi0GE44Za4rF2LN9d11TPA
mRGunUHBcnWEvgJBQl9nJEiU0Zsnvgc/ubhPgXRR4Xq37Z0j4r7g1SgEEzwxA57d
emyPxgcYxn/eR44/KJ4EBs+lVDR3veyJm+kXQ99b21/+jh5Xos1AnX5iItreGCc=
-----END CERTIFICATE-----
)EOF";

BearSSL::WiFiClientSecure espClient;
PubSubClient client(espClient);


//  ----- Pulse Sensor -----
const int PulseWire = 0;
const int LED = LED_BUILTIN;
int Threshold = 550;
int lastBPM = 0;

PulseSensorPlayground pulseSensor;

//  ----- DS18B20 Temperature Sensor -----
#define ONE_WIRE_BUS D1
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);

unsigned long lastPublish = 0;
const unsigned long publishInterval = 2000;

// -------- Set time via NTP (required for X.509 certificate validation) --------
void setClock() {
  configTime(1 * 3600, 0, "pool.ntp.org", "time.nist.gov");  // UTC+1 for Tunisia

  Serial.print("Waiting for NTP time sync: ");
  time_t now = time(nullptr);
  while (now < 8 * 3600 * 2) {
    delay(500);
    Serial.print(".");
    now = time(nullptr);
  }
  Serial.println("");
  struct tm timeinfo;
  gmtime_r(&now, &timeinfo);
  Serial.print("Current time: ");
  Serial.print(asctime(&timeinfo));
}

// -------- Wi-Fi setup --------
void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  randomSeed(micros());

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

// -------- MQTT reconnect --------
void reconnect() {
  char err_buf[256];
  
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    
    // Generate unique client ID
    String clientId = "ESP8266-";
    clientId += String(ESP.getChipId(), HEX);
    
    if (client.connect(clientId.c_str(), mqtt_user, mqtt_password)) {
      Serial.println("connected");
      return;
    } else {
      Serial.print("failed, rc=");
      Serial.println(client.state());
      espClient.getLastSSLError(err_buf, sizeof(err_buf));
      Serial.print("SSL error: ");
      Serial.println(err_buf);
      Serial.println(" try again in 5 seconds");
      delay(5000);
    }
  }
}

// -------- Setup --------
void setup() {
  Serial.begin(115200);
  delay(100);
  
  Serial.println("\n\nESP8266 Health Monitor Starting...");
  
  pinMode(LED, OUTPUT);
  digitalWrite(LED, HIGH);
  
  // Configure TLS certificate (CRITICAL: Must be done BEFORE setup_wifi)
  BearSSL::X509List *serverTrustedCA = new BearSSL::X509List(ca_cert);
  espClient.setTrustAnchors(serverTrustedCA);
  
  // Wi-Fi
  setup_wifi();
  
  // Set time (Required for X.509 certificate validation)
  setClock();
  
  // MQTT
  client.setServer(mqtt_server, mqtt_port);

  // Pulse Sensor
  pulseSensor.analogInput(PulseWire);
  pulseSensor.blinkOnPulse(LED, true);
  pulseSensor.setThreshold(Threshold);
  if (pulseSensor.begin()) {
    Serial.println("PulseSensor ready!");
  }

  // DS18B20
  sensors.begin();
  Serial.println("DS18B20 ready!");
  
  Serial.println("Setup complete!\n");
}

// -------- Loop --------
void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  // ----- Pulse Sensor -----
  if (pulseSensor.sawStartOfBeat()) {
    int currentBPM = pulseSensor.getBeatsPerMinute();
    if (currentBPM > 40 && currentBPM < 200) {
      lastBPM = currentBPM;
      Serial.print("♥ BPM: ");
      Serial.println(lastBPM);
    }
  }

  // ----- Publish at intervals -----
  unsigned long now = millis();
  if (now - lastPublish >= publishInterval) {
    lastPublish = now;
    
    // Read temperature
    sensors.requestTemperatures();
    float tempC = sensors.getTempCByIndex(0);
    Serial.print("Temperature: ");
    Serial.print(tempC);
    Serial.println("°C");

    // Build JSON payload
    String payload = "{";
    payload += "\"bpm\":" + String(lastBPM) + ",";
    payload += "\"temperature\":" + String(tempC, 2);
    payload += "}";
    
    // Publish to MQTT
    if (client.connected()) {
      bool published = client.publish(mqtt_topic, payload.c_str());
      Serial.print("Publish message: ");
      Serial.println(payload);
      if (published) {
        Serial.println("✓ Published successfully");
      } else {
        Serial.println("✗ Publish failed");
      }
    }
  }

  delay(20);
}