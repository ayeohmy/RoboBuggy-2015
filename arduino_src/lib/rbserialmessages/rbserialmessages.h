/********************************************************************************
 * All rights are open to the public, to use, share, dump, spread for the       *
 * foreseeable future. This software and documentation constitutes purely 		*
 * published and public work, and contains open source knowledge by a bunch		*
 * of college kids who just want to have fun. All the material and code may be 	*
 * used, copied, duplicated, changed, disclosed... with any human's free will.	*
 * Have a nice day! :)															*
 ********************************************************************************/

/**
 * @author: Ian Hartwig (spacepenguine@gmail.com)
 * @author: Audrey Yeoh (ayeohmy@gmail.com)
 * @date: 7/22/2014
 */


#ifndef RBSERIALMESSAGES_H
#define RBSERIALMESSAGES_H

// #include <Arduino.h>
#include "HardwareSerial.h"

// Implementation Constants
#define RBSM_BUFFER_OUT_LENGTH 11 // minimum to support double message
#define RBSM_NULL_TERM 0x00

// Protocol Constants
#define RBSM_FOOTER 0x0A // \n
#define RBSM_ONE_BYTE_SIZE 8
#define RBSM_TWO_BYTE_SIZE 16
#define RBSM_THREE_BYTE_SIZE 24
#define RBSM_ONE_BYTE_MASK 0xFF
#define RBSM_TWO_BYTE_MASK 0xFFFF
#define RBSM_BAUD_RATE 9600

// Message Types
#define RBSM_MID_ENC_TICKS_LAST 0
#define RBSM_MID_ENC_TICKS_RESET 1
#define RBSM_MID_ENC_TIMESTAMP 2
#define RBSM_MID_MEGA_STEER_ANGLE 10
#define RBSM_MID_MEGA_BRAKE_STATE 11
#define RBSM_MID_RESERVED 252 // 0xFC, message head
#define RBSM_MID_ERROR 254
#define RBSM_MID_DEVICE_ID 255

// Device Types
#define RBSM_DID_MEGA 0
#define RBSM_DID_DRIVE_ENCODER 1


class RBSerialMessages {
 public:
  RBSerialMessages();
  int Begin(HardwareSerial *serial_stream);
  int Send(uint8_t id, uint32_t message);
  // todo: define receive api
 private:
  HardwareSerial *serial_stream_;
  uint8_t buffer_out_[RBSM_BUFFER_OUT_LENGTH];
  uint8_t AppendMessageToBuffer(uint8_t id,
                                uint32_t message,
                                uint8_t out_start_pos);
  uint8_t InitMessageBuffer();
};


// old api. may still want some of these functions
//
// int protocol_init( void ); // TODO: I'm not sure exactly what needs to be initialized yet
// int protocol_run( void ); // TODO:
// unsigned long protocol_send( byte id, unsigned int message); // Currently working on
// unsigned long protocol_getMessage(unsigned long packet);
// unsigned long protocol_getID(unsigned long packet);


#endif // RBSERIALMESSAGES_H
