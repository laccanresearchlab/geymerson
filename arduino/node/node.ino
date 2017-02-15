
/* A application for arduino sensing nodes
   this application uses series 2 XBEEs to wirelessly
   send and receive data

   @author: Geymerson Ramos <geymerson.r@gmail.com>
*/

//Still in progress

#include <XBee.h>

//Setting up XBEE
XBee xbee = XBee();
XBeeResponse response = XBeeResponse();

// create reusable response objects for responses we expect to handle
ZBRxResponse rx = ZBRxResponse();
ModemStatusResponse msr = ModemStatusResponse();

int lights = 8;

void setup() {
  //Set serial ports
  Serial.begin(9600);
  xbee.begin(Serial);
  pinMode(lights, OUTPUT);
}

void loop() {

  /***************** A simple  test *****************/

  delay(3000);
  digitalWrite(lights, !digitalRead(lights));

  /**************************************************/


   //TODO
   
  //Keep waiting for packets
  xbee.readPacket();

  //If a packet arrived
  if (xbee.getResponse().isAvailable()) {
    xbee.getResponse().getZBRxResponse(rx);
  }

  else if (xbee.getResponse().isError()) {
    //TODO
  }
}
