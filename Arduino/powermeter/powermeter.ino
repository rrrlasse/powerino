
#include "hx711.h"
#include <SPI.h>
#include <EEPROM.h>
#include <Wire.h>
#include <SoftwareSerial.h>
#include "LowPower.h"
#include <avr/sleep.h>    // Sleep Modes
#include <avr/power.h>

// Bluetooth baudrate. Most Bluetooth modules are configured to 9600 out-o-the-box which will
// give 40-60 samples/second. Using 38400 seems to not increase the rate much and also needs
// you to perform a configuration process of the Bluetooth module.
const int btrate = 9600;
//const int btrate = 38400;

bool debug = false;
bool command_mode = true;

// Power usage when supplied with 3.6 V: 38 mA with these settings. 
int gauge_sleep = 10;
int gauge_averages = 1;

int gyro_sleep = 0;
int gyro_averages = 4;

// The Bluetooth module must use hardware serial (i.e. the Arduino TX and RX pins) because the
// Arduino software serial library is unable to send and receive data simultaneously (sending
// disables incoming packet interrupts) which the smartphone apps depend on.
#define DEVICE Serial

// We connect the HX711 ADC to the Arduino pin 3 (for data) and 2 (for clock) and select an
// amplification gain of 32, which will implicitly make it sense on its channel B. 
HX711 scale(3, 2, 32);

// Read voltage of the Vcc pin on the Arduino board. 
float calibrate_vcc() {
  long result;
  // Read 1.1V reference against AVcc
  ADMUX = _BV(REFS0) | _BV(MUX3) | _BV(MUX2) | _BV(MUX1);
  delay(2); // Wait for Vref to settle
  ADCSRA |= _BV(ADSC); // Convert
  while (bit_is_set(ADCSRA,ADSC));
  result = ADCL;
  result |= ADCH<<8;
  result = 1113500L / result; // Back-calculate AVcc in mV
  return result / 1000.;
}

void setup() {
  Wire.begin();

  wake_gyro();
  gyro_sensitivity();
  
  // Bluetooth baudrate. Must match the configuration of the module.
  Serial.begin(btrate);
}

// Block until at least one incoming Bluetooth byte is available and return it.
char read_device() {
  int res;
  do {
    res = DEVICE.read();
  } while(res == -1);
  return res;
}

// From Bluetooth, read a positive integer given as human readable string, e.g. "1234". Read
// until first byte different from '0' - '9'.
int read_value() {
  String s = "";
  char b;
  do {
    b = read_device();
    if(b >= '0' && b <= '9') {
      s = s + b;
    }
  } while(b >= '0' && b <= '9');
  return s.toInt();
}


void read_command()
{
  if(DEVICE.available() > 0) {
    byte c = read_device();
    if(c == 'e') {
      if(read_device() == 'r') {
        read_eprom();
      }
      else {
        write_eprom();
        DEVICE.write("OK\n");
      }
    }
    else if(c == 's') {
      show_status();
    }
    else if(c == 'd') {
      debug = !debug;
      DEVICE.write("OK\n");
    }
    else if(c == 'p') {
      read_device();
      gyro_sleep = read_value();
      gauge_sleep = read_value();
      DEVICE.write("OK\n");
    }
    else if(c == 'C') {
        command_mode = true;
        DEVICE.write("OK\n");
    }
    else if(c == 'c') {
        command_mode = false;
    }
    else if(c == 'g') {
      read_device();
      gauge_averages = read_value();
      DEVICE.write("OK\n");
    }
    else if(c == 'y') {
      read_device();
      gyro_averages = read_value();
      DEVICE.write("OK\n");
    }
  }
}


void loop() {  
  bool gyro_is_awake = true;
  long gyro_event = 0;

  int gyro;
  int accel;
  int temp;

  for(;;) 
  {
    do {
      read_command();
    } while(command_mode);

    long voltage = scale.read_average(gauge_averages);
    long t = millis();

    read_command();
    
    
    if(gyro_sleep > 0) {
      if(t > gyro_event) {
        if(gyro_is_awake) {
          read_gyro(gyro, accel, temp, gyro_averages);
          sleep_gyro();
          gyro_is_awake = false;
          gyro_event = t + gyro_sleep;
        }
        else if(!gyro_is_awake) {
          wake_gyro();
          gyro_is_awake = true;
          // It takes 100 ms for the gyro to wake up
          gyro_event = t + 100;
        }
      }
    }
    else {
      if(!gyro_is_awake) {
          wake_gyro();
          gyro_is_awake = true;
      }
      read_gyro(gyro, accel, temp, gyro_averages);
    }

    if(gauge_sleep > 0) {
      delay_idle(gauge_sleep);
    }

    read_command();

    if(!debug) {
      DEVICE.print(t);
      DEVICE.print("\t");
    }

    DEVICE.print(voltage);

    if(!debug) {
      DEVICE.print("\t");
      DEVICE.print(gyro);
      DEVICE.print("\t");  
      DEVICE.print(accel);
    }        
    
    DEVICE.print("\n");
  }

}

// Set gyro sensitivity to measure in the -2000 to 2000 degrees/second interval
void gyro_sensitivity()
{
  Wire.beginTransmission(0x68);
  Wire.write(0x1B);
  Wire.write(3 << 3);    
  Wire.endTransmission(true);  
}


// Return rotational velocity, pedal arm position (gravitational acceleration in gyro's Z
// direction, plus lots of noise from vibrations) and temperature
void read_gyro(int& gyro, int& accel, int& temp, int averages) {
  long accel_sum = 0;
  long gyro_sum = 0;
  for(int i = 0; i < averages; i++) {
    Wire.beginTransmission(0x68);
    Wire.write(0x3B + 4);  // starting with register 0x3B (ACCEL_XOUT_H)
    Wire.endTransmission(false);
    Wire.requestFrom(0x68,10,true);  // request a total of 14 registers
    accel_sum += Wire.read()<<8|Wire.read();  // 0x3F (ACCEL_ZOUT_H) & 0x40 (ACCEL_ZOUT_L)
    temp = Wire.read()<<8|Wire.read();  // 0x41 (TEMP_OUT_H) & 0x42 (TEMP_OUT_L)
    Wire.read()<<8; Wire.read();  // 0x43 (GYRO_XOUT_H) & 0x44 (GYRO_XOUT_L)
    Wire.read()<<8; Wire.read();  // 0x45 (GYRO_YOUT_H) & 0x46 (GYRO_YOUT_L)
    gyro_sum += Wire.read()<<8|Wire.read();  // 0x47 (GYRO_ZOUT_H) & 0x48 (GYRO_ZOUT_L)  return GyZ;  
  }
  accel = accel_sum / averages;
  gyro = gyro_sum / averages;
}

void sleep_gyro()
{
  // Put gyro into sleep mode. It uses 0.5 mA in sleep and 4 mA awake.
  Wire.beginTransmission(0x68);
  Wire.write(0x6B);   // PWR_MGMT_1 register
  Wire.write(64);     // set to 1 (sleep the MPU-6050)
  Wire.endTransmission(true);
}

void wake_gyro()
{
  // Wake up gyro from sleep mode. You should wait for 100 ms before reading it! Reading it 
  // before that time may give inaccurate values.
  Wire.beginTransmission(0x68);
  Wire.write(0x6B);  // PWR_MGMT_1 register
  Wire.write(0);     // set to zero (wakes up the MPU-6050)
  Wire.endTransmission(true);
}

// User has sent a "read eprom" command such as "re 8 2". Read the requested address and
// byte count and dump the contents 
void read_eprom() {
  read_device();
  int address = read_value();
  int count = read_value();
  
  DEVICE.print("eprom\t");
  DEVICE.print(address);
  DEVICE.print("\t");
  DEVICE.print(count);
  DEVICE.print("\t");
  
  for(int i = 0; i < count; i++) {
    byte value = EEPROM.read(i + address);
    if(i != 0) {
      DEVICE.print("\t");
    }
    DEVICE.print(value);
  }
  DEVICE.print("\n");
}

// User requested a "write eprom" command, such as "ew 8 4 111 222 111 222". Read address
// and byte count, and then read the payload that must be stored
void write_eprom() {
  read_device();
  int address = read_value();
  int count = read_value();

  for(int i = 0; i < count; i++) {
    int value = read_value();
    EEPROM.write(i + address, value);
  }
}

// Show battery level and temperature
void show_status() {
  float vcc = calibrate_vcc();

  DEVICE.print("battery\t");
  DEVICE.print(vcc);
  DEVICE.print("\n");

  int dummy;
  int temp;
  read_gyro(dummy, dummy, temp, 1);
  float t = temp/340.00+36.53;
  DEVICE.print("temperature\t");
  DEVICE.print(t, 1);
  DEVICE.print("\n");
}

// Delay that puts the Arduino in sleep mode to save power. Saves 3 mA compared to delay().
void delay_idle(long milliseconds)
{  
  long start = micros();
  while (micros() - start < milliseconds * 1000) {
    set_sleep_mode(SLEEP_MODE_IDLE);
    sleep_enable();
    sleep_mode();
    sleep_disable();
    read_command();
  }
}


