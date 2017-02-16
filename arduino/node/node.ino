/* An application for arduino sensing nodes.
   This application uses S2 XBEEs to wirelessly
   send and receive data
   @author: Geymerson Ramos <geymerson.r@gmail.com>
*/

//Still in progress

#include <XBee.h>

//Setting up XBEE
XBee xbee = XBee();
XBeeResponse response = XBeeResponse();
ZBRxResponse rx = ZBRxResponse();
ModemStatusResponse msr = ModemStatusResponse();

int lights = 8;

void setup() {
  //Setup leds
  pinMode(lights,  OUTPUT);

  // start serial
  Serial.begin(9600);
  xbee.begin(Serial);
}

void loop() {

  xbee.readPacket();

  if (xbee.getResponse().isAvailable()) {
    xbee.getResponse().getZBRxResponse(rx);
    char command = (char)rx.getData(0);

    if (command == '0') {
      digitalWrite(lights, LOW);
      Serial.print(command);
    }
    else if (command == '1') {
      Serial.print(command);
      digitalWrite(lights, HIGH);
    }
    Serial.println();
  }
}
