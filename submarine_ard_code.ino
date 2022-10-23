#include <Servo.h>

#define MOTOR_1_PIN 9
#define MOTOR_2_PIN 6
#define COMPRESSOR_PIN 8
#define LIGHT_PIN 7

char Incoming_value = 0;  //Variable for storing Incoming_value
Servo motor1;
Servo motor2;
bool compressor = false;
bool light = false;


int motor1Val = 90;
int motor2Val = 90;

void setup() {
  Serial.begin(9600);
  pinMode(MOTOR_1_PIN, OUTPUT);
  pinMode(MOTOR_2_PIN, OUTPUT);
  pinMode(COMPRESSOR_PIN, OUTPUT);
  pinMode(LIGHT_PIN, OUTPUT);
  motor1.attach(MOTOR_1_PIN, 1000, 2000);
  motor2.attach(MOTOR_2_PIN, 1000, 2000);
}
void loop() {
  digitalWrite(COMPRESSOR_PIN, (compressor) ? HIGH : LOW);
  digitalWrite(LIGHT_PIN, (light)? HIGH : LOW);

  motor1.write(motor1Val);  // Send the signal to the ESC
  motor2.write(motor2Val);  // Send the signal to the ESC

  if (Serial.available() > 0) {
    Incoming_value = Serial.read();  //Read the incoming data and store it into variable Incoming_value
    Serial.print(Incoming_value);
    Serial.print("\n");
    if (Incoming_value == '1')
      motor1Val = 0;           
    else if (Incoming_value == '0')  
      motor1Val = 90;                
    else if (Incoming_value == '2')
      compressor = true;
    else if (Incoming_value == '3')
      compressor = false;
    else if (Incoming_value == '4')
      motor2Val = 0;
    else if (Incoming_value == '5')
      motor2Val = 90;
    else if (Incoming_value == '6')
      light = !light;
  }
}