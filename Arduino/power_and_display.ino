/*********************************************************************
This is an example for our Monochrome OLEDs based on SSD1306 drivers

  Pick one up today in the adafruit shop!
  ------> http://www.adafruit.com/category/63_98

This example is for a 128x64 size display using I2C to communicate
3 pins are required to interface (2 I2C and one reset)

Adafruit invests time and resources providing this open source code, 
please support Adafruit and open-source hardware by purchasing 
products from Adafruit!

Written by Limor Fried/Ladyada  for Adafruit Industries.  
BSD license, check license.txt for more information
All text above, and the splash screen must be included in any redistribution
*********************************************************************/
/*
 Create a BLE server that, once we receive a connection, will send periodic notifications.
 The service advertises itself as: 4fafc201-1fb5-459e-8fcc-c5c9c331914b
 And has a characteristic of: beb5483e-36e1-4688-b7f5-ea07361b26a8

 The design of creating the BLE server is:
 1. Create a BLE Server
 2. Create a BLE Service
 3. Create a BLE Characteristic on the Service
 4. Create a BLE Descriptor on the characteristic
 5. Start the service.
 6. Start advertising.

 A connect hander associated with the server starts a background task that performs notification
 every couple of seconds.
*/

/******** BLE *************/
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>

byte flags = 0b00111110;
int pwr = 100;
byte power[9] = { 0b00010000, 0b00000000, 100, 0, 0, 0, 0, 0, 10};
byte feature[4] = { 0b00010000, 0b00000000, 0b00000000, 0b00000000};
byte pwrPos[1] = {5};

bool _BLEClientConnected = false;

#define powerMeterService BLEUUID((uint16_t)0x1818)
BLECharacteristic powerMeterMeasurementCharacteristics(BLEUUID((uint16_t)0x2A63), BLECharacteristic::PROPERTY_NOTIFY);
BLECharacteristic cyclingPowerFeatureCharacteristics(BLEUUID((uint16_t)0x2A65), BLECharacteristic::PROPERTY_READ);
BLECharacteristic sensorLocationCharacteristic(BLEUUID((uint16_t)0x2A5D), BLECharacteristic::PROPERTY_READ);
BLEDescriptor powerMeterMeasuremenDescriptor(BLEUUID((uint16_t)0x2901));
BLEDescriptor cyclingPowerFeatureDescriptor(BLEUUID((uint16_t)0x2901));
BLEDescriptor sensorLocationDescriptor(BLEUUID((uint16_t)0x2901));

class MyServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      _BLEClientConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      _BLEClientConnected = false;
    }
};

/******** DISPLAY *************/
#include <SPI.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

Adafruit_SSD1306 display(4);

static const unsigned char PROGMEM logo16_glcd_bmp[] =
{ B00000000, B11000000,
  B00000001, B11000000,
  B00000001, B11000000,
  B00000011, B11100000,
  B11110011, B11100000,
  B11111110, B11111000,
  B01111110, B11111111,
  B00110011, B10011111,
  B00011111, B11111100,
  B00001101, B01110000,
  B00011011, B10100000,
  B00111111, B11100000,
  B00111111, B11110000,
  B01111100, B11110000,
  B01110000, B01110000,
  B00000000, B00110000 };

void setup()   {                
  Serial.begin(115200);
  
  /******** BLE *************/
  
  InitBLE();
  Serial.println("Service started...");
  pwr = 1;
  
  /******** DISPLAY *************/
  display.begin(SSD1306_SWITCHCAPVCC, 0x3C);  // initialize with the I2C addr 0x3C (for the 128x64)
  display.clearDisplay();

  // text display tests
  display.setTextSize(2);
  display.setTextColor(WHITE);
  display.setCursor(10,20);
  display.println("Teszt!");
  display.display();
}


void loop() {
  
  power[3] = (byte) (pwr / 256);
  power[2] = (byte) (pwr - (power[3] * 256));
  Serial.println(pwr);
  for(int i = 0; i < 10; i++)
  {
    Serial.print(power[i]);
    Serial.print(", ");
  }
  Serial.println();
  
  display.clearDisplay();
  display.setCursor(10,20);
  display.print(pwr);
  display.print(" W");
  display.display();

  powerMeterMeasurementCharacteristics.setValue(power, 9);
  powerMeterMeasurementCharacteristics.notify();
  cyclingPowerFeatureCharacteristics.setValue(feature, 4);
  sensorLocationCharacteristic.setValue(pwrPos, 1);
  
  pwr+=10;
  if (pwr > 1000)
    pwr = 10;

  delay(2000);
  
}

void InitBLE() {
    BLEDevice::init("Dani's Power Meter");
    // Create the BLE Server
    BLEServer *pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());
  
    // Create the BLE Service
    BLEService *pPower = pServer->createService(powerMeterService);
  
    pPower->addCharacteristic(&powerMeterMeasurementCharacteristics);
    powerMeterMeasuremenDescriptor.setValue("sint 16bit");
    powerMeterMeasurementCharacteristics.addDescriptor(&powerMeterMeasuremenDescriptor);
    powerMeterMeasurementCharacteristics.addDescriptor(new BLE2902());
  
    pPower->addCharacteristic(&cyclingPowerFeatureCharacteristics);
    cyclingPowerFeatureDescriptor.setValue("Bits 0 - 21");
    cyclingPowerFeatureCharacteristics.addDescriptor(&cyclingPowerFeatureDescriptor);

      
    pPower->addCharacteristic(&sensorLocationCharacteristic);
    sensorLocationDescriptor.setValue("Position 0 - 16");
    sensorLocationCharacteristic.addDescriptor(&sensorLocationDescriptor);
  
    pServer->getAdvertising()->addServiceUUID(powerMeterService);
  
    pPower->start();
    // Start advertising
    pServer->getAdvertising()->start();
  }


