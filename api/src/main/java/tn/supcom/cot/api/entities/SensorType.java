package tn.supcom.cot.api.entities;

public enum SensorType {
    HEART_RATE,     // MAX30100
    SPO2,           // MAX30100
    TEMPERATURE,    // DS18B20
    BLOOD_PRESSURE, // MPS20N0040D
    FALL_DETECTION, // AI Camera
    GPS_LOCATION    // For tracking
}