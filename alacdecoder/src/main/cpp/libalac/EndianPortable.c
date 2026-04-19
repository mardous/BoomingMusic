/*
 * Copyright (c) 2011 Apple Inc. All rights reserved.
 *
 * @APPLE_APACHE_LICENSE_HEADER_START@
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @APPLE_APACHE_LICENSE_HEADER_END@
 */

#include <stdio.h>
#include <stdint.h>
#include "EndianPortable.h"

// Modern optimization: Use compiler built-ins for byte swapping
// These translate to a single CPU instruction (like REV on ARM)
#if defined(__GNUC__) || defined(__clang__)
    #define BSWAP16(x) __builtin_bswap16(x)
    #define BSWAP32(x) __builtin_bswap32(x)
    #define BSWAP64(x) __builtin_bswap64(x)
#else
    #define BSWAP16(x) (((x << 8) | ((x >> 8) & 0x00ff)))
    #define BSWAP32(x) (((x << 24) | ((x << 8) & 0x00ff0000) | ((x >> 8) & 0x0000ff00) | ((x >> 24) & 0x000000ff)))
    #define BSWAP64(x) ((((int64_t)x << 56) | (((int64_t)x << 40) & 0x00ff000000000000LL) | \
                        (((int64_t)x << 24) & 0x0000ff0000000000LL) | (((int64_t)x << 8) & 0x000000ff00000000LL) | \
                        (((int64_t)x >> 8) & 0x00000000ff000000LL) | (((int64_t)x >> 24) & 0x0000000000ff0000LL) | \
                        (((int64_t)x >> 40) & 0x000000000000ff00LL) | (((int64_t)x >> 56) & 0x00000000000000ffLL)))
#endif

// Android is always Little Endian (ARM and x86)
#define TARGET_RT_LITTLE_ENDIAN 1

uint16_t Swap16NtoB(uint16_t inUInt16)
{
    return BSWAP16(inUInt16);
}

uint16_t Swap16BtoN(uint16_t inUInt16)
{
    return BSWAP16(inUInt16);
}

uint32_t Swap32NtoB(uint32_t inUInt32)
{
    return BSWAP32(inUInt32);
}

uint32_t Swap32BtoN(uint32_t inUInt32)
{
    return BSWAP32(inUInt32);
}

uint64_t Swap64BtoN(uint64_t inUInt64)
{
    return BSWAP64(inUInt64);
}

uint64_t Swap64NtoB(uint64_t inUInt64)
{
    return BSWAP64(inUInt64);
}

float SwapFloat32BtoN(float in)
{
	union {
		float f;
		int32_t i;
	} x;
	x.f = in;	
	x.i = BSWAP32(x.i);
	return x.f;
}

float SwapFloat32NtoB(float in)
{
	union {
		float f;
		int32_t i;
	} x;
	x.f = in;	
	x.i = BSWAP32(x.i);
	return x.f;
}

double SwapFloat64BtoN(double in)
{
	union {
		double f;
		int64_t i;
	} x;
	x.f = in;	
	x.i = BSWAP64(x.i);
	return x.f;
}

double SwapFloat64NtoB(double in)
{
	union {
		double f;
		int64_t i;
	} x;
	x.f = in;	
	x.i = BSWAP64(x.i);
	return x.f;
}

void Swap16(uint16_t * inUInt16)
{
	*inUInt16 = BSWAP16(*inUInt16);
}

void Swap24(uint8_t * inUInt24)
{
	uint8_t tempVal = inUInt24[0];
	inUInt24[0] = inUInt24[2];
	inUInt24[2] = tempVal;
}

void Swap32(uint32_t * inUInt32)
{
	*inUInt32 = BSWAP32(*inUInt32);
}
