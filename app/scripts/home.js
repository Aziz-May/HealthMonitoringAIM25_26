import { Model, View, Presenter } from './mvp.js';
import { StateChangeEvent } from './events.js';
import { toggleDevice ,sendMessage} from './websocket.js';

const setupDeviceSwitch = (deviceId, deviceName) => {
  const deviceSwitch = document.getElementById(deviceId);
  if (deviceSwitch) {
    deviceSwitch.addEventListener('click', () => toggleDevice(deviceSwitch, deviceName));

  } else {
    console.log(`Switch for ${deviceName} is NULL`);
  }
};

const waitForDeviceSwitch = (deviceId, deviceName) => {
  const interval = setInterval(() => {
    const deviceSwitch = document.getElementById(deviceId);
    if (deviceSwitch) {
      setupDeviceSwitch(deviceId, deviceName);
      clearInterval(interval);
    }
  }, 100);
};

const setupEmergencyButton = () => {
  const interval = setInterval(() => {
    const emergencyBtn = document.getElementById('emergencyAlert');
    if (emergencyBtn) {
      emergencyBtn.addEventListener('click', () => {
        if (confirm('Are you sure you want to trigger an emergency alert?')) {
          sendMessage('smarthealth/emergency', JSON.stringify({
            type: 'MANUAL_SOS',
            timestamp: new Date().toISOString(),
            patientId: sessionStorage.getItem('subject')
          }));
          alert('Emergency alert sent to caregivers!');
        }
      });
      clearInterval(interval);
    }
  }, 100);
};

// Ensure DOM is loaded before executing
document.addEventListener('DOMContentLoaded', () => {
  const devices = [
    { id: 'fallDetection', name: 'FallDetection' },
  ];

  devices.forEach(device => {
    waitForDeviceSwitch(device.id, device.name);
  });
  
  setupEmergencyButton();
});

class HomeModel extends Model {
  constructor() {
    super('healthModel');
  }

  async fetchSensorData(endpoint, elementId, unit = '') {
    // TODO: Replace this URL with your actual health monitoring API endpoint
    const url = `http://localhost:8080/api/sensors/most-recent/${endpoint}`;
    let accessToken = sessionStorage.getItem('accessToken');
    const token = accessToken ;
    try {
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
  
      const data = await response.json();
      const value = Math.round(data.value);
      
      const displayElement = document.getElementById(elementId);
      if (displayElement) {
        displayElement.innerHTML = `${value} ${unit}`;
      }
      
      this.fireStateChangeEvent(data, StateChangeEvent.LOADED);
    } catch (error) {
      console.error(`Error fetching ${endpoint} data:`, error);
    }
  }

  loadModel() {
    // Initial load of all vital signs
    this.fetchSensorData('heartrate', 'heartRate', 'bpm');
    this.fetchSensorData('spo2', 'spo2', '%');
    this.fetchSensorData('bloodpressure', 'bloodPressure', 'mmHg');
    this.fetchSensorData('bodytemperature', 'bodyTemperature', '°C');
    
    // Set up periodic refresh (every 10 seconds)
    this.refreshInterval = setInterval(() => {
      this.fetchSensorData('heartrate', 'heartRate', 'bpm');
      this.fetchSensorData('spo2', 'spo2', '%');
      this.fetchSensorData('bloodpressure', 'bloodPressure', 'mmHg');
      this.fetchSensorData('bodytemperature', 'bodyTemperature', '°C');
    }, 10000);
  }
  
  stopRefresh() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }
}

class HomeView extends View {
  constructor() {
    super('View');
  }

  defineBindings(data) {
      // Bindings can be added here if needed for direct model-view binding
  }

  applyBindings() {
      // Apply bindings logic
  }
  
  updateDisplay(elementId, value, unit) {
      const element = document.getElementById(elementId);
      if(element) {
          element.innerHTML = `${value} ${unit}`;
      }
  }
}

export class HomePresenter extends Presenter {
  constructor() {
    const view = new HomeView();
    const model = new HomeModel();
    super(view, model);

    // Register to listen for state change events
    this.model.register((data) => {
      if (this.model.mvpEvent.isStateChange() && this.model.mvpEvent.event === StateChangeEvent.LOADED) {
         // Logic to handle data updates if needed beyond direct DOM manipulation in Model
      }
    });

    this.model.loadModel();
  }
}
