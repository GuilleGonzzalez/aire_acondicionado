#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <Servo.h>

Servo servo1;

#define PIN_SERVO D5

const String device = "10";
const char* ssid = "Gonmar-Livebox";
const char* password = "618995151609549464";

String message;
String my_info;

Servo servo;
ESP8266WebServer server(80);

void other() {
  String my_uri = server.uri().substring(1);
  char type = my_uri.charAt(0);
  if (type == 'a') {
    int angle = my_uri.substring(1).toInt();
    Serial.print("Angle received: ");
    Serial.println(angle);
    servo.write(angle);
    delay(500);
    if (angle > 90) {
      servo.write(angle - 5);
    } else {
      servo.write(angle + 5);
    }
    my_info = "angleset";
    message = device + "," + my_info + ",0";
    Serial.println(message);
    server.send(200, "text/plain", message);
  }
}

void setup(void) {
  Serial.begin(115200);
  delay(1000);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("");
  Serial.print("Connected to ");
  Serial.println(ssid);
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());

  server.onNotFound(other);
  server.begin();
  Serial.println("HTTP server started");

  servo.attach(PIN_SERVO);
}

void loop(void){  
  server.handleClient();
}
