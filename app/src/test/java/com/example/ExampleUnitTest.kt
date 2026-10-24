package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun verifyLiveVoiceStates() {
    val states = com.example.model.LiveVoiceState.entries
    assertEquals(4, states.size)
    assertTrue(states.contains(com.example.model.LiveVoiceState.IDLE))
    assertTrue(states.contains(com.example.model.LiveVoiceState.LISTENING))
    assertTrue(states.contains(com.example.model.LiveVoiceState.PROCESSING))
    assertTrue(states.contains(com.example.model.LiveVoiceState.SPEAKING))
  }

  @Test
  fun verifyHamedTones() {
    val tones = com.example.model.HamedTone.entries
    assertTrue(tones.isNotEmpty())
    assertEquals("formal", com.example.model.HamedTone.FORMAL.id)
    assertEquals("casual", com.example.model.HamedTone.FRIENDLY.id)
    assertEquals("poetic", com.example.model.HamedTone.POETIC.id)
  }
}
