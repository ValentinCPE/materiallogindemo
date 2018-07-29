#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <WiFiClientSecure.h>
#include <ESP8266HTTPClient.h>


//const String host = "192.168.1.17";
#define PIR_MOTION_SENSOR 16

char *ssid = "MBAL-Detector";

String networkToConnect = "";
String passwordToConnect = "";
String nameMBAL = "";

String networkAvailables[50][2];

boolean isServerEnabled = false;

ESP8266WebServer server(80);

const char* host = "www.mbal.ovh";
const int httpsPort = 443;
const char* fingerprint = "10 3C E6 9D 3E 53 47 B7 56 A0 94 91 00 CC 74 83 ED F7 4F 81";

void sendEventOverSSL();
void scanWifi();

void handleRoot() {
  String informations = "";

  if (server.args() > 0) {
    networkToConnect = server.arg("network");
    passwordToConnect = server.arg("password");
    nameMBAL = server.arg("nameMBAL");
  }

  server.send(200, "text/plain", "OK");
}

void setup()
{
  //  pinMode(PIR_MOTION_SENSOR, INPUT);
  //  Serial.begin(9600);

  delay(1000);
  Serial.begin(115200);
  scanWifi();
  Serial.println("Configuring access point...");
  /* You can remove the password parameter if you want the AP to be open. */
  WiFi.softAP(ssid);

  IPAddress myIP = WiFi.softAPIP();
  char ipAdressServer[18];
  myIP.toString().toCharArray(ipAdressServer, 18);
  Serial.printf(ipAdressServer);
  Serial.println("");
  server.on("/getConnectionInformation", handleRoot);
  server.begin();
}

void loop()
{
  if (!isServerEnabled) {
    server.handleClient();
  }

  if (networkToConnect != "" && passwordToConnect != "") {
    server.close();

    networkToConnect = networkAvailables[networkToConnect.toInt()][0]; //convert number network that has been chosen by user to a real string to connect (SSID)

    sendEventOverSSL();

    networkToConnect = "";
    isServerEnabled = true;
  }


  /*   if(digitalRead(PIR_MOTION_SENSOR))//if it detects the moving people?
         Serial.println("Hi,people is coming");
     else
         Serial.println("Watching"); */

  delay(200);
}

/*void sendEvent() {
  //token retrieve
  String token = "";
  HTTPClient http;
  http.setReuse(true);
  // http.begin("http://" + host + ":8080/MBAL/oauth/token?grant_type=password&username=admin&password=Valentin34");
  http.addHeader("Authorization", "Basic bXktdHJ1c3RlZC1jbGllbnQ6c2VjcmV0");
  http.POST("");
  token = http.getString().substring(17, 53); //parse from 17 to 53 to get token.
  Serial.println("Token : " + token);
  http.end();

  //send event
  // http.begin("http://" + host + ":8080/MBAL/api/event/add/NEW_COURRIEL?username=" + nameMBAL);
  http.addHeader("Authorization", "Bearer " + token);
  http.GET();
  String eventCreated = http.getString();
  http.end();

  if (eventCreated.indexOf("OK") > 0) {
    Serial.println("Event created !");
  }
  }*/

void sendEventOverSSL() {
  char network[50], password[50];
  networkToConnect.toCharArray(network, 50);
  passwordToConnect.toCharArray(password, 50);
  Serial.println("SSID chosen : " + networkToConnect + "  |  Password : " + passwordToConnect);
  WiFi.begin(network, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  WiFiClientSecure client;

  if (!client.connect(host, httpsPort)) {
    Serial.println("connection failed");
    return;
  }

  if (client.verify(fingerprint, host)) {
    Serial.println("certificate matches");
  } else {
    Serial.println("certificate doesn't match");
  }

  String url = "/MBAL/oauth/token?grant_type=password&username=admin&password=Valentin34";
  client.print(String("POST ") + url + " HTTP/1.1\r\n" +
               "Host: " + host + "\r\n" +
               "Authorization: Basic bXktdHJ1c3RlZC1jbGllbnQ6c2VjcmV0\r\n" +
               "User-Agent: BuildFailureDetectorESP8266\r\n" +
               "Connection: close\r\n\r\n");

  String responseToken;

  while (client.connected()) {
    responseToken = client.readString();
  }

  int tokenPresentStart = responseToken.indexOf("access_token");
  int tokenPresentEnd = responseToken.indexOf("token_type");
  if (tokenPresentStart > 0 && tokenPresentEnd > 0) {
    responseToken = responseToken.substring(tokenPresentStart + 15, tokenPresentEnd - 3); //fetch the token from the webservice response (system with indexesof allows size changing for token)
  }

  Serial.println("Token : " + responseToken);
}

void scanWifi() {
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();
  int n = WiFi.scanNetworks();
  if (n == 0) {
    Serial.println("no networks found");
  } else {
    for (int i = 0; i < n; ++i) {
      // Print SSID and RSSI for each network found
      networkAvailables[i + 1][0] = WiFi.SSID(i);
      networkAvailables[i + 1][1] = WiFi.RSSI(i);
      Serial.println(String(i + 1) + " : " + networkAvailables[i + 1][0] + "|" + networkAvailables[i + 1][1]);
      delay(10);
    }
  }
}

