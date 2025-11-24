from picamera2 import Picamera2
import cv2
import numpy as np
import tensorflow as tf
from http import server
import socketserver
from threading import Condition, Thread

# ---------------------------
# HTML page
# ---------------------------
PAGE = """\
<html>
<head>
<title>Raspberry Pi - TFLite Detection</title>
</head>
<body>
<center><h1>Raspberry Pi - TFLite YOLO Detection Stream</h1></center>
<center><img src="stream.mjpg" width="320" height="240"></center>
</body>
</html>
"""

# ---------------------------
# Streaming classes
# ---------------------------
class StreamingOutput:
    def __init__(self):
        self.frame = None
        self.condition = Condition()

    def update(self, frame):
        with self.condition:
            self.frame = frame
            self.condition.notify_all()

class StreamingHandler(server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/':
            self.send_response(301)
            self.send_header('Location', '/index.html')
            self.end_headers()
        elif self.path == '/index.html':
            content = PAGE.encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'text/html')
            self.send_header('Content-Length', len(content))
            self.end_headers()
            self.wfile.write(content)
        elif self.path == '/stream.mjpg':
            self.send_response(200)
            self.send_header('Cache-Control', 'no-cache')
            self.send_header('Content-Type',
                             'multipart/x-mixed-replace; boundary=FRAME')
            self.end_headers()
            try:
                while True:
                    with output.condition:
                        output.condition.wait()
                        frame = output.frame
                    self.wfile.write(b'--FRAME\r\n')
                    self.send_header('Content-Type', 'image/jpeg')
                    self.send_header('Content-Length', len(frame))
                    self.end_headers()
                    self.wfile.write(frame)
                    self.wfile.write(b'\r\n')
            except Exception as e:
                print("Removed client:", e)
        else:
            self.send_error(404)
            self.end_headers()

class StreamingServer(socketserver.ThreadingMixIn, server.HTTPServer):
    allow_reuse_address = True
    daemon_threads = True

# ---------------------------
# Load TFLite model
# ---------------------------
tflite_model_path = "best_float16.tflite"  # INT8 quantized model
interpreter = tf.lite.Interpreter(model_path=tflite_model_path)
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()
input_height = input_details[0]['shape'][1]
input_width = input_details[0]['shape'][2]

print(f"Model input size: {input_width}x{input_height}")
print(f"Output shape: {output_details[0]['shape']}")

# ---------------------------
# PiCamera setup
# ---------------------------
picam2 = Picamera2()
config = picam2.create_preview_configuration(main={"format": "RGB888", "size": (320, 240)})
picam2.configure(config)
picam2.start()

output = StreamingOutput()

# ---------------------------
# YOLO postprocessing
# ---------------------------
def process_yolo_output(output_data, conf_threshold=0.5, orig_width=320, orig_height=240):
    """
    Process YOLO TFLite output (Ultralytics format)
    Output shape is typically [1, 6, 8400] for YOLOv8
    Format: [x, y, w, h, confidence, class]
    """
    detections = []

    # Handle different output shapes
    if len(output_data.shape) == 3:
        output_data = output_data[0]  # Remove batch dimension

    # Ultralytics format: transpose if needed
    if output_data.shape[0] < output_data.shape[1]:
        output_data = output_data.T

    for detection in output_data:
        # Format: [x_center, y_center, width, height, confidence, class_scores...]
        if len(detection) >= 5:
            confidence = detection[4]

            if confidence > conf_threshold:
                # Get box coordinates (normalized 0-1)
                x_center = detection[0]
                y_center = detection[1]
                w = detection[2]
                h = detection[3]

                # Convert to pixel coordinates
                x_center_px = int(x_center * orig_width)
                y_center_px = int(y_center * orig_height)
                w_px = int(w * orig_width)
                h_px = int(h * orig_height)

                # Convert to corner coordinates
                x1 = int(x_center_px - w_px / 2)
                y1 = int(y_center_px - h_px / 2)
                x2 = int(x_center_px + w_px / 2)
                y2 = int(y_center_px + h_px / 2)

                # Clamp to frame boundaries
                x1 = max(0, min(x1, orig_width))
                y1 = max(0, min(y1, orig_height))
                x2 = max(0, min(x2, orig_width))
                y2 = max(0, min(y2, orig_height))

                detections.append({
                    'bbox': (x1, y1, x2, y2),
                    'confidence': float(confidence)
                })

    return detections

# ---------------------------
# Camera + TFLite loop
# ---------------------------
def camera_loop():
    frame_count = 0
    last_detections = []

    while True:
        frame = picam2.capture_array()
        frame_count += 1

        # Only run inference every 3rd frame (adjust to 2 for faster detection)
        if frame_count % 3 == 0:
            # Prepare input
            input_frame = cv2.resize(frame, (input_width, input_height))
            input_frame = np.expand_dims(input_frame, axis=0).astype(np.float32) / 255.0

            # Run inference
            interpreter.set_tensor(input_details[0]['index'], input_frame)
            interpreter.invoke()
            output_data = interpreter.get_tensor(output_details[0]['index'])

            # Process detections and cache them
            last_detections = process_yolo_output(
                output_data,
                conf_threshold=0.5,
                orig_width=320,
                orig_height=240
            )

        # Draw detections on every frame (using cached detections)
        for det in last_detections:
            x1, y1, x2, y2 = det['bbox']
            conf = det['confidence']

            cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 0, 255), 2)
            label = f"Fall: {conf:.2f}"
            cv2.putText(frame, label, (x1, y1-5),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)

        # Encode and stream
        _, jpeg = cv2.imencode(".jpg", frame)
        output.update(jpeg.tobytes())


# Start camera thread
Thread(target=camera_loop, daemon=True).start()

# Start web server
address = ('0.0.0.0', 8000)
server = StreamingServer(address, StreamingHandler)
print("Streaming at: http://<your-pi-ip>:8000")
server.serve_forever()
