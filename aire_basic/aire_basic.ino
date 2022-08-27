#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>
#include <DHT.h>
#include "Timer.h"
#include <ArduinoJson.h>

#define PIN_DHT         D8
#define PIN_DHT_PWR     D5
#define PIN_LED_ON      D6
#define PIN_LED_PROG    D7
#define PIN_LED_OFFLINE D0
#define PIN_FAN1        D1
#define PIN_FAN2        D2
#define PIN_FAN3        D3

#define TIME_CHECK_TEMP 10000
#define THRESHOLD_HIGH  3
#define THRESHOLD_LOW   1
#define DEFAULT_TEMP    25

#define DHT_TYPE DHT22

DHT dht(PIN_DHT, DHT_TYPE);
Timer t;

const String device = "1";
const char* ssid = "Gonmar-Livebox";
const char* password = "618995151609549464";

String message;
String my_info;
int temp_set;
int time_set;
bool is_on = false;
bool is_auto = false;
int timeout;

int temp_event;
int time_event;

ESP8266WebServer server(80);

void get_settings()
{
    DynamicJsonDocument doc(512);
    doc["ip"] = WiFi.localIP().toString();
    doc["gw"] = WiFi.gatewayIP().toString();
    doc["nm"] = WiFi.subnetMask().toString();
 
    if (server.arg("all") == "true") {
      doc["signalStrengh"] = WiFi.RSSI();
      doc["chipId"] = ESP.getChipId();
      doc["flashChipId"] = ESP.getFlashChipId();
      doc["flashChipSize"] = ESP.getFlashChipSize();
      doc["flashChipRealSize"] = ESP.getFlashChipRealSize();
      doc["freeHeap"] = ESP.getFreeHeap();
    }
 
    Serial.print("Stream...");
    String buf;
    serializeJson(doc, buf);
    server.send(200, "application/json", buf);
    Serial.println("done.");
}

void turn_on()
{
  is_auto = false;
  my_info = "on";
  message = device + "," + my_info + "," + read_temp();
  server.send(200, "text/plain", message);
  if (!is_on) {
    digitalWrite(PIN_FAN1, HIGH);
    digitalWrite(PIN_FAN2, HIGH);
    digitalWrite(PIN_FAN3, LOW);
  }
  digitalWrite(PIN_LED_ON, HIGH);
  is_on = true;
  Serial.println(message);
}

void turn_off()
{
  t.stop(temp_event);
  t.stop(time_event);
  timeout = 0;
  is_auto = false;
  my_info = "off";
  message = device + "," + my_info + "," + read_temp();
  server.send(200, "text/plain", message);
  digitalWrite(PIN_FAN1, HIGH);
  digitalWrite(PIN_FAN2, HIGH);
  digitalWrite(PIN_FAN3, HIGH);
  digitalWrite(PIN_LED_ON, LOW);
  digitalWrite(PIN_LED_PROG, LOW);
  is_on = false;
  Serial.println(message);
}

void fan1()
{
  t.stop(temp_event);
  is_auto = false;
  my_info = "f1";
  message = device + "," + my_info + "," + read_temp();
  digitalWrite(PIN_FAN1, LOW);
  digitalWrite(PIN_FAN2, HIGH);
  digitalWrite(PIN_FAN3, HIGH);
  Serial.println(message);
  server.send(200, "text/plain", message);
}

void fan2()
{
  t.stop(temp_event);
  is_auto = false;
  my_info = "f2";
  message = device + "," + my_info + "," + read_temp();
  digitalWrite(PIN_FAN1, HIGH);
  digitalWrite(PIN_FAN2, LOW);
  digitalWrite(PIN_FAN3, HIGH);
  Serial.println(message);
  server.send(200, "text/plain", message);
}

void fan3()
{
  t.stop(temp_event);
  is_auto = false;
  my_info = "f3";
  message = device + "," + my_info + "," + read_temp();
  digitalWrite(PIN_FAN1, HIGH);
  digitalWrite(PIN_FAN2, HIGH);
  digitalWrite(PIN_FAN3, LOW);
  Serial.println(message);
  server.send(200, "text/plain", message);
}

void fan_auto()
{
  t.stop(temp_event);
  check_temp();
  is_auto = true;
  temp_event = t.every(TIME_CHECK_TEMP, check_temp);
  my_info = "fauto";
  message = device + "," + my_info + "," + read_temp();
  Serial.println(message);
  server.send(200, "text/plain", message);
}

void upd()
{
  my_info = "update";
  message = device + "," + my_info + "," + (is_on ? "on" : "off") + ";" + read_temp() + ";"+ timeout;
  Serial.println(message);
  server.send(200, "text/plain", message);
}

void other()
{
  String my_uri = server.uri().substring(1);
  char type = my_uri.charAt(0);
  if (type == 't') {
    temp_set = my_uri.substring(1).toInt();
    my_info = "tempset";
    message = device + "," + my_info + "," + read_temp();
    Serial.println(message);
    server.send(200, "text/plain", message);
  }
  if (type == 'd') {
    time_set = my_uri.substring(1).toInt();
    timeout = time_set * 60;
    t.stop(time_event);
    time_event = t.every(1000, check_time);
    digitalWrite(PIN_LED_PROG, HIGH);
    my_info = "timeset";
    message = device + "," + my_info + "," + read_temp();
    Serial.println(message);
    server.send(200, "text/plain", message);
  }
  if (is_auto)
    check_temp();
}

void setup(void)
{
  Serial.begin(115200);
  delay(1000);
  dht.begin();
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    digitalWrite(PIN_LED_OFFLINE, LOW);
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.print("Connected to ");
  Serial.println(ssid);
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());

  if (MDNS.begin("aire")) {
    Serial.println("MDNS responder started");
  }

  pinMode(PIN_DHT_PWR, OUTPUT);
  pinMode(PIN_FAN1, OUTPUT);
  pinMode(PIN_FAN2, OUTPUT);
  pinMode(PIN_FAN3, OUTPUT);
  pinMode(PIN_LED_ON, OUTPUT);
  pinMode(PIN_LED_PROG, OUTPUT);
  pinMode(PIN_LED_OFFLINE, OUTPUT);

  digitalWrite(PIN_DHT_PWR, HIGH);
  digitalWrite(PIN_FAN1, HIGH);
  digitalWrite(PIN_FAN2, HIGH);
  digitalWrite(PIN_FAN3, HIGH);
  digitalWrite(PIN_LED_OFFLINE, HIGH);

  server.on("/", HTTP_GET, []() {
    server.send(200, "text/html",
    "Welcome to the ESP8266 REST Web Server. Aire acondicionado");
  });
  server.on("/on", turn_on);
  server.on("/off", turn_off);
  server.on("/f1", fan1);
  server.on("/f2", fan2);
  server.on("/f3", fan3);
  server.on("/fauto", fan_auto);
  server.on("/update", upd);
  server.on("/settings", get_settings);
  server.onNotFound(other);
  
  server.enableCORS(true);
  server.enableCORS(true);
  server.begin();
  Serial.println("HTTP server started");
}

void loop(void)
{
  server.handleClient();
  while (WiFi.status() != WL_CONNECTED) {
    digitalWrite(PIN_LED_OFFLINE, LOW);
  }
  digitalWrite(PIN_LED_OFFLINE, HIGH);
  t.update();
}

float read_temp()
{
  float temp = dht.readTemperature();
  if (temp > 100) {
    return -1;
  }
  return temp;
}

void check_temp()
{
  Serial.println("Checking temp");
  if (!isnan(dht.readTemperature())) {
    if (dht.readTemperature() > temp_set + THRESHOLD_HIGH) {
      digitalWrite(PIN_FAN1, HIGH);
      digitalWrite(PIN_FAN2, HIGH);
      digitalWrite(PIN_FAN3, LOW);
    } else if (dht.readTemperature() > temp_set + THRESHOLD_LOW) {
      digitalWrite(PIN_FAN1, HIGH);
      digitalWrite(PIN_FAN2, LOW);
      digitalWrite(PIN_FAN3, HIGH);
    } else if (dht.readTemperature() > temp_set) {
      digitalWrite(PIN_FAN1, LOW);
      digitalWrite(PIN_FAN2, HIGH);
      digitalWrite(PIN_FAN3, HIGH);
    } else {
      digitalWrite(PIN_FAN1, HIGH);
      digitalWrite(PIN_FAN2, HIGH);
      digitalWrite(PIN_FAN3, HIGH);
    }
  }
}

void check_time() {
  timeout--;
  if (timeout == 0) {
    t.stop(time_event);
    turn_off();
  }
}
