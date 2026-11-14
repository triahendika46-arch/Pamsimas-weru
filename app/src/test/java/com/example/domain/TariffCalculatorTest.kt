package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TariffCalculatorTest {
  @Test
  fun usageUnder8_adds5000Beban() {
    val zero = TariffCalculator.calculate(100L, 100L)
    assertEquals(0L, zero.pemakaian)
    assertEquals(0L, zero.biayaPemakaian)
    assertEquals(5000L, zero.biayaBeban)
    assertEquals(5000L, zero.totalTagihan)

    val seven = TariffCalculator.calculate(100L, 107L)
    assertEquals(7L, seven.pemakaian)
    assertEquals(14000L, seven.biayaPemakaian)
    assertEquals(5000L, seven.biayaBeban)
    assertEquals(19000L, seven.totalTagihan)
  }

  @Test
  fun usage8OrMore_noBeban() {
    val eight = TariffCalculator.calculate(50L, 58L)
    assertEquals(8L, eight.pemakaian)
    assertEquals(16000L, eight.biayaPemakaian)
    assertEquals(0L, eight.biayaBeban)
    assertEquals(16000L, eight.totalTagihan)

    val fifteen = TariffCalculator.calculate(50L, 65L)
    assertEquals(15L, fifteen.pemakaian)
    assertEquals(30000L, fifteen.biayaPemakaian)
    assertEquals(0L, fifteen.biayaBeban)
    assertEquals(30000L, fifteen.totalTagihan)
  }
}
