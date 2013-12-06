/*
 *  This file is part of hijack-infinity.
 *
 *  hijack-infinity is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  hijack-infinity is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with hijack-infinity.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef __CSM_H__
#define __CSM_H__

#include "config.h"

#include <inttypes.h>

////////////////////////////////////////
// Public Memebers:
////////////////////////////////////////

// Struct to pass capture data to the receive timing function.
typedef struct csm_timer_struct {
	uint16_t elapsedTime;  // The number of timer counts between this and the
	                       // last value
	uint8_t signal;        // If the signal line is low or high
} csm_rx_input_t;

// Definition of callback function to be called
// when a byte is received.
typedef void csm_byteReceiver (uint8_t);

// Definition of callback function to be called when
// a buffer has been sent.
typedef void csm_bufferSent (void);

// Receives a pair of timing data and current line
// signal data from the comparator.
void csm_receiveTiming(struct csm_timer_struct * in);

// Called by higher software, will send the passed in
// byte by setting the state to transmit. Returns 1 if
// already transmitting.
uint8_t csm_sendBuffer (uint8_t* buf, uint8_t len);

// Registers a callback function to be executed when a
// full byte has been received
void csm_registerReceiveBuffer (csm_byteReceiver* func);

// Registers a callback function to be executed when a
// byte has been transmitted.
void csm_registerTransmitBuffer (csm_bufferSent* func);

// Initializes the coder's state.
void csm_init(void);

inline uint8_t csm_int2man (uint8_t val);

void csm_txTimerInterrupt (void);

#define START_BIT 0
#define IDLE_BIT 0
#define PREAMBLE_BIT 1

////////////////////////////////////////
// Private Memebers:
////////////////////////////////////////

typedef void    csm_rxDispatchFunc(void);
typedef uint8_t csm_txDispatchFunc(void);

typedef enum csm_transmitStateEnum {
	CSM_TXSTATE_IDLE,
	CSM_TXSTATE_PREAMBLE,
	CSM_TXSTATE_START,
	CSM_TXSTATE_DATA,
	CSM_TXSTATE_POSTAMBLE,
} csm_txState_e;

typedef enum csm_receiveStateEnum {
	CSM_RXSTATE_IDLE,
	CSM_RXSTATE_DATA,
	CSM_RXSTATE_DATA_EXTRA
} csm_rxState_e;

typedef enum csm_receiveBits {
	CSM_RX_BIT_DIFFERENT,
	CSM_RX_BIT_SAME
} csm_receiveBitType_e;

#define MAX_BUF_SIZE 128
#define RX_PREAMBLE_LEN 4

// Keep all the state associated with this module
// in a struct.
struct csm_state_struct {

	/// State

	csm_txState_e txState;
	csm_rxState_e rxState;

	// Set to 1 if we are currently transmitting a packet
	uint8_t transmittingPacket;

	// Whether the mic pin is configured as an output or input
	uint8_t txPinOutput;


	/// TX

	// Storage for the raw, fully-formed packet from the upper layer that is to
	// be transmitted
	uint8_t txBufRaw[MAX_BUF_SIZE];
	// Length of the raw out buffer
	uint8_t txLen;
	// Which byte from the raw buffer we are currently transmitting
	uint8_t txByteIdx;
	// Which bit of the byte we are transmitting
	uint8_t txBitIdx;
	// Which half of the manchester bit we are sending
	uint8_t txBitHalf;
	// How many bits to send in the preamble
	uint8_t preambleBitLen;
	// How long should the mic line be held low after a packet
	uint8_t postambleBitLen;

	// The value to set on the pin the next time the txTimerInterrupt fires.
	// By using this we can determine what the pin should be in advance, and
	// then immediately update the pin when the timer fires.
	uint8_t txPinVal;

	/// Callbacks
	// Called after a buffer has been sent
	csm_bufferSent* txCallback;


	/// RX

	// Timing of two consecutive edges (single baud)
	uint16_t rxDeltaT;

	// Buffer to hold the last four elapsed time between edges. If these are all
	// about the same, and then we get a double length elapsed time at a rising
	// edge, then we have detected a start bit to a packet.
	uint16_t rxPreambleBuffer[RX_PREAMBLE_LEN];
	uint8_t  rxPreambleReceivedEdges;
	uint8_t  rxPreambleIdx;

	// Buffer to put the raw data read from the phone in
	uint8_t  rxBufRaw[MAX_BUF_SIZE];
	// Number of bits received for this byte
	uint8_t  rxBitIdx;
	// Total number of bytes received
	uint8_t  rxByteIdx;
	// The value of the last bit we added to the array
	uint8_t  rxPreviousBit;

	// RX Callback triggers when a packet has been received
	csm_bufferReceived* rxCallback;







	// Encoding state
	csm_txDispatchFunc *        txDispatch[CSM_TXSTATECOUNT];
	uint16_t                    txByte;
	uint8_t                     txBytePosition;
	uint8_t                     txBitPosition;
	uint8_t                     txIdleCycles;
	// ^ Actually stores start bit + parity too

	// Decoding state
	uint16_t                    threshold;

	csm_rxDispatchFunc *        rxDispatch[CSM_RXSTATECOUNT];


} csm;

// PRIVATE METHODS:

// Receive state machine dispatchers
void csm_receiveIdle(void) ;
void csm_receiveData(void);
void csm_receiveDataNext(void);

// Transmit state machine dispatchers
uint8_t csm_transmitIdle(void);
uint8_t csm_transmitPending(void);
uint8_t csm_transmitData(void);

// Helper functions
void csm_advanceReceiveDataState(void);
uint8_t csm_isWithinThreshold(uint16_t value, uint16_t desired);
uint8_t csm_calcByteParity(uint8_t byte) ;

#endif
