#include "variant.h"
#include <stdio.h>
#include <adk.h>

int ledPin = 13;        
int IRpin  = A0;            

// Accessory descriptor. It's how Arduino identifies itself to Android.
char applicationName[] = "UDOOAndroidADKDemoBidirect"; // the app installed in Android
char accessoryName[] = "UDOO_ADK_BIDIRECT"; // your Arduino Accessory name (Need to be the same defined in the Android App)
char companyName[] = "Aidilab";  // manufacturer (Need to be the same defined in the Android App)

// Make up anything you want for these
char versionNumber[] = "1.0"; // version (Need to be the same defined in the Android App)
char serialNumber[] = "1";
char url[] = "http://www.aidilab.it"; // If there isn't any compatible app installed, Android suggest to visit this url
                                     

USBHost Usb;
ADK adk(&Usb, companyName, accessoryName, applicationName, versionNumber, url, serialNumber);
                     
int distance = 0;

void setup()
{
    Serial.begin(115200);
    pinMode(ledPin, OUTPUT);
    cpu_irq_enable(); 
    printf("\r\nADK demo start\r\n");
    delay(200);
}

#define RCVSIZE 1

void loop()
{
    uint8_t bufRead[RCVSIZE];
    uint32_t nbread = 0;
    uint8_t bufWrite[1];
    
    Usb.Task();
    
    if (adk.isReady()) {      
        adk.read(&nbread, RCVSIZE, bufRead); // read data into bufRead array
        if (nbread > 0) {
          if (bufRead[0] == 1) // compare received data
            digitalWrite(ledPin, HIGH); // turn on light
          else
            digitalWrite(ledPin, LOW); // turn off light
        }
        distance = analogRead(IRpin);
        if (distance < 100) { 
          distance = 100;  
        } else if (distance > 900){
          distance = 900;
        }
        bufWrite[0] = (uint8_t)(2076/(distance - 11) + 4);  //  calculate the distance in centimeters
  
        adk.write(sizeof(bufWrite), (uint8_t *)bufWrite); // write the distance to Android app
    } else {
      digitalWrite(ledPin, LOW);
    }  
    
    delay(50);
}

