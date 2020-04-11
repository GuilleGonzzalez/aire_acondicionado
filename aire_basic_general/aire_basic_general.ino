#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>

#define PIN_RELAY D0

const String device = "0";
const char* ssid = "Gonmar";
const char* password = "618995151609549464";

String message;
String my_info = "off";

bool is_on = false;

ESP8266WebServer server(80);

void turn_on() {
  my_info = "on";
  message = device + "," + my_info;
  server.send(200, "text/plain", message);
  digitalWrite(PIN_RELAY, HIGH);
  is_on = true;
  Serial.println(message);
}

void turn_off() {
  my_info = "off";
  message = device + "," + my_info;
  server.send(200, "text/plain", message);
  digitalWrite(PIN_RELAY, LOW);
  is_on = false;
  Serial.println(message);
}

void upd() {
  is_on ? my_info = "on" : my_info = "off";
  message = device + "," + my_info;
  Serial.println(message);
  server.send(200, "text/plain", message);
}

void setup(void){ 
  Serial.begin(115200);
  WiFi.begin(ssid, password);
  Serial.println("");

  // Wait for connection
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("");
  Serial.print("Connected to ");
  Serial.println(ssid);
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());

  pinMode(PIN_RELAY, OUTPUT);

  digitalWrite(PIN_RELAY, LOW);
 
  server.on("/gen_on", turn_on);
  server.on("/gen_off", turn_off);
  server.on("/gen_update", upd);

  server.begin();
  Serial.println("HTTP server started");
}

void loop(void){
  server.handleClient();
}
