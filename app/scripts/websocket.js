//import Paho from 'paho-mqtt'
var broker = {
    hostname: "3f8df013054946d4ab9fb4775293070e.s1.eu.hivemq.cloud",
    port: 8884,
    clientId: "smarthealth_" + Date.now()
};

var topic = "smarthealth/vitals";

// MQTT client instance
var client = new Paho.MQTT.Client(broker.hostname, broker.port, broker.clientId);

function onConnect() {
    console.log("Connected to MQTT Broker");
    client.subscribe(topic);
    client.subscribe("smarthealth/alerts");
    client.subscribe("smarthealth/emergency");
}

export function toggleDevice(switchElement, endpoint) {
    const isChecked = switchElement.checked;
    console.log(`${endpoint} is now ${isChecked ? 'ENABLED' : 'DISABLED'}`);
    isChecked ? sendMessage(`smarthealth/devices/${endpoint}`, 'ENABLED') : sendMessage(`smarthealth/devices/${endpoint}`, 'DISABLED');
}

function onFailure(message) {
    console.log("Connection to MQTT broker failed: " + message.errorMessage);
} 

function onMessageArrived(message) {
    console.log("Received message on topic:", message.destinationName);
    console.log("Payload:", message.payloadString);
    
    // Handle different message types
    if (message.destinationName.includes('vitals')) {
        handleVitalsUpdate(message.payloadString);
    } else if (message.destinationName.includes('alerts')) {
        handleAlert(message.payloadString);
    } else if (message.destinationName.includes('emergency')) {
        handleEmergency(message.payloadString);
    }
}

function handleVitalsUpdate(payload) {
    try {
        const data = JSON.parse(payload);
        // Update UI elements with real-time data
        if (data.heartRate) document.getElementById('heartRate').innerHTML = `${data.heartRate} bpm`;
        if (data.spo2) document.getElementById('spo2').innerHTML = `${data.spo2} %`;
        if (data.bloodPressure) document.getElementById('bloodPressure').innerHTML = `${data.bloodPressure} mmHg`;
        if (data.bodyTemperature) document.getElementById('bodyTemperature').innerHTML = `${data.bodyTemperature} °C`;
    } catch (e) {
        console.error("Error parsing vitals data:", e);
    }
}

function handleAlert(payload) {
    console.log("Alert received:", payload);
    // Display notification to user
    showNotification("Health Alert", payload);
}

function handleEmergency(payload) {
    console.log("Emergency detected:", payload);
    // Trigger emergency UI
    showEmergencyAlert(payload);
}

function showNotification(title, message) {
    // This would integrate with the notification system in the UI
    console.log(`Notification: ${title} - ${message}`);
}

function showEmergencyAlert(message) {
    // This would show a prominent alert in the UI
    alert(`EMERGENCY: ${message}`);
}

// Set callback functions
client.onConnectionLost = onFailure;
client.onMessageArrived = onMessageArrived;

// Connect to MQTT broker
// Function to publish a message to a topic
export function sendMessage(topic, message) {
    if (client.isConnected()) {
    var mqttMessage = new Paho.MQTT.Message(message);
    mqttMessage.destinationName = topic;
    client.send(mqttMessage);
    console.log(`Message sent to topic '${topic}': ${message}`);
    } else {
    console.error("Client is not connected. Unable to send message.");
    alert("Client is not connected. Please try again later.");
    }
    }

export function connectClient(){
    client.connect({
        onSuccess: onConnect,
        onFailure: onFailure,
        useSSL: true,
        userName: "HealthMonitor",
        password: "HealthMonitor2025*"
    });
}
