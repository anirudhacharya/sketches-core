/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hll;

import java.math.BigInteger;

import com.yahoo.sketches.hash.MurmurHash3;

/**
 * Base class and API for all the maps.
 *
 * @author Lee Rhodes
 * @author Alex Saydakov
 * @author Kevin Lang
 */
abstract class Map {
  static final long SEED = 1234567890L;
  static final int SIX_BIT_MASK = 0X3F; // 6 bits
  static final int TEN_BIT_MASK = 0X3FF; //10 bits
  static final double COUPON_MAP_GROW_TRIGGER_FACTOR = 15.0 / 16.0;
  static final double COUPON_MAP_TARGET_FILL_FACTOR = 2.0 / 3.0;

  final int keySizeBytes_;

  Map(final int keySizeBytes) {
    keySizeBytes_ = keySizeBytes;
  }

  /**
   * Update this map with a key and a coupon.
   * Return the cardinality estimate of all identifiers that have been associated with this key,
   * including this update.
   * @param key the dimensional criteria for measuring cardinality
   * @param coupon the property associated with the key for which cardinality is to be measured.
   * @return the cardinality estimate of all identifiers that have been associated with this key,
   * including this update.
   */
  abstract double update(byte[] key, int coupon);

  /**
   * Returns the estimate of the cardinality of identifiers associated with the given key.
   * @param key the given key
   * @return the estimate of the cardinality of identifiers associated with the given key.
   */
  abstract double getEstimate(byte[] key);

  /**
   * Returns the upper bound cardinality with respect to {@link #getEstimate(byte[])} associated
   * with the given key. This approximates the RSE with 68% confidence.
   * @param key the given key
   * @return the upper bound cardinality with respect to {@link #getEstimate(byte[])} associated
   * with the given key.
   */
  abstract double getUpperBound(byte[] key);

  /**
   * Returns the lower bound cardinality with respect to {@link #getEstimate(byte[])} associated
   * with the given key. This approximates the RSE with 68% confidence.
   * @param key the given key
   * @return the lower bound cardinality with respect to {@link #getEstimate(byte[])} associated
   * with the given key.
   */
  abstract double getLowerBound(byte[] key);

  int getKeySizeBytes() {
    return keySizeBytes_;
  }

  abstract double getEntrySizeBytes();

  abstract int getTableEntries();

  abstract int getCapacityEntries();

  abstract int getCurrentCountEntries();

  abstract long getMemoryUsageBytes();

  /**
   * Returns <tt>true</tt> if the two specified sub-arrays of bytes are <i>equal</i> to one another.
   * Two arrays are considered equal if all corresponding pairs of elements in the two arrays are
   * equal. In other words, two arrays are equal if and only if they contain the same elements
   * in the same order.
   *
   * @param a one sub-array to be tested for equality
   * @param offsetA the offset in bytes of the start of sub-array <i>a</i>.
   * @param b the other sub-array to be tested for equality
   * @param offsetB the offset in bytes of the start of sub-array <i>b</i>.
   * @param length the length in bytes of the two sub-arrays.
   * @return <tt>true</tt> if the two sub-arrays are equal
   */
  static final boolean arraysEqual(final byte[] a, final int offsetA, final byte[] b, final int offsetB, final int length) {
    for (int i = 0; i < length; i++) {
      if (a[i + offsetA] != b[i + offsetB]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the HLL array index and value as a 16-bit coupon given the identifier to be hashed
   * and k.
   * @param identifier the given identifier
   * @return the HLL array index and value
   */
  static final int coupon16(final byte[] identifier) {
    final long[] hash = MurmurHash3.hash(identifier, SEED);
    final int hllIdx = (int) (((hash[0] >>> 1) % 1024) & TEN_BIT_MASK); //hash[0] for 10-bit address
    final int lz = Long.numberOfLeadingZeros(hash[1]);
    final int value = (lz > 62 ? 62 : lz) + 1;
    return (value << 10) | hllIdx;
  }

  static final int coupon16Value(final int coupon) {
    return (coupon >>> 10) & SIX_BIT_MASK;
  }

  static final int getIndex(final long hash, final int tableEntries) {
    return (int) ((hash >>> 1) % tableEntries);
  }

  static final int getStride(final long hash, final int tableEntries) {
    return (int) ((hash >>> 1) % (tableEntries - 2L) + 1L);
  }

  static boolean isBitSet(final byte[] byteArr, final int bitIndex) {
    final int byteIndex = bitIndex / 8;
    final int mask = 1 << (bitIndex % 8);
    return (byteArr[byteIndex] & mask) > 0;
  }

  static boolean isBitClear(final byte[] byteArr, final int bitIndex) {
    final int byteIndex = bitIndex / 8;
    final int mask = 1 << (bitIndex % 8);
    return (byteArr[byteIndex] & mask) == 0;
  }

  static void clearBit(final byte[] byteArr, final int index) {
    final int byteIndex = index / 8;
    final int mask = 1 << (index % 8);
    byteArr[byteIndex] &= ~mask;
  }

  static void setBit(final byte[] bits, final int index) {
    final int byteIndex = index / 8;
    final int mask = 1 << (index % 8);
    bits[byteIndex] |= mask;
  }

  /**
   * Returns the next prime number that is greater than the given target. There will be
   * no prime numbers less than the returned prime number that are greater than the given target.
   * @param target the starting value to begin the search for the next prime
   * @return the next prime number that is greater than the given target.
   */
  static int nextPrime(final int target) {
    return BigInteger.valueOf(target).nextProbablePrime().intValueExact();
  }

  static String fmtLong(final long value) {
    return String.format("%,d", value);
  }

  static String fmtDouble(final double value) {
    return String.format("%,.3f", value);
  }
}