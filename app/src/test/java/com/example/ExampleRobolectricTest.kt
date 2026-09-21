package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PersonalityCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Voice Changer", appName)
  }

  @Test
  fun `verify personality catalog contains requested celebrities`() {
    val personalities = PersonalityCatalog.predefinedPersonalities
    val rafi = personalities.find { it.name == "Mohammad Rafi" }
    val imran = personalities.find { it.name == "Imran Khan" }

    assertNotNull("Mohammad Rafi should be present in personalities", rafi)
    assertNotNull("Imran Khan should be present in personalities", imran)
    assertTrue("Mohammad Rafi should have elevated melodic pitch", (rafi?.pitchMultiplier ?: 0f) > 1.0f)
    assertTrue("Imran Khan should have deeper baritone pitch", (imran?.pitchMultiplier ?: 0f) < 1.0f)

    val custom = PersonalityCatalog.createCustomPersonality("Narendra Modi")
    assertEquals("Narendra Modi", custom.name)
    assertTrue(custom.isCustom)
  }
}

