#include "variant.h"
#include <stdio.h>
#include <adk.h>

//definizione pin collegati al LED, sensori Ping e luce
#define LED_PIN  13
int IR_PIN  = A0;            

// Accessory descriptor. It's how Arduino identifies itself to Android.
char descriptionName[] = "UDOOAndroidADKDemoBidirect"; // the app installed in Android
char modelName[] = "UDOO_ADK_BIDIRECT"; // your Arduino Accessory name (Need to be the same defined in the Android App)
char manufacturerName[] = "Aidilab";  // manufacturer (Need to be the same defined in the Android App)

// Make up anything you want for these
char versionNumber[] = "1.0"; // version (Need to be the same defined in the Android App)
char serialNumber[] = "1";
char url[] = "http://www.aidilab.it"; // If there isn't any compatible app installed, Android suggest to visit this url
                                     
USBHost Usb;
ADK adk(&Usb, manufacturerName, modelName, descriptionName, versionNumber, url, serialNumber);

#define RCVSIZE 1   
uint8_t buf[RCVSIZE];
uint32_t bytesRead = 0;

uint8_t bufWrite[1];                     
int distance = 0;

void setup()
{
    Serial.begin(115200);
    pinMode(LED_PIN, OUTPUT);
    delay(500);
    Serial.println("UDOO ADK BIDIRECT demo start...");
}

void loop()
{    
    Usb.Task();
    
    if (adk.isReady()) {      
        adk.read(&bytesRead, RCVSIZE, buf); // read data into bufRead array
        if (bytesRead > 0) {
          if (parseCommand(buf[0]) == 1) {// compare received data
            // Received "1" - turn on LED
            digitalWrite(LED_PIN, HIGH);
          } else if (parseCommand(buf[0]) == 0) {
            // Received "0" - turn off LED
            digitalWrite(LED_PIN, LOW); 
          }
        }
        distance = analogRead(IR_PIN);
        if (distance < 100) {  // 
          distance = 100;  
        } else if (distance > 900){
          distance = 900;
        }
        bufWrite[0] = (uint8_t)(2076/(distance - 11) + 4);  //  calculate the distance in centimeters
  
        adk.write(sizeof(bufWrite), (uint8_t *)bufWrite); //write the distance to Android
    } else {
      digitalWrite(LED_PIN, LOW);
    }  
    
    delay(10);
}

// the characters sent to Arduino are interpreted as ASCII, we decrease 48 to return to ASCII range.
uint8_t parseCommand(uint8_t received) {
  return received - 48;
}

