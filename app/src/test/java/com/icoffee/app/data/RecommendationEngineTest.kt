package com.icoffee.app.data

import com.icoffee.app.data.model.MoodType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class RecommendationEngineTest {

    @Test
    fun `sabah saatlerinde varsayilan mood FOCUSED olmali`() {
        // 05-11 -> MORNING
        val morning = LocalTime.of(8, 30)

        val mood = RecommendationEngine.defaultMoodForNow(morning)

        assertEquals(MoodType.FOCUSED, mood)
    }

    @Test
    fun `ogleden sonra varsayilan mood SOCIAL olmali`() {
        // 12-16 -> AFTERNOON
        val afternoon = LocalTime.of(14, 0)

        val mood = RecommendationEngine.defaultMoodForNow(afternoon)

        assertEquals(MoodType.SOCIAL, mood)
    }

    @Test
    fun `aksam saatlerinde varsayilan mood RELAXED olmali`() {
        // 17-21 -> EVENING
        val evening = LocalTime.of(19, 0)

        val mood = RecommendationEngine.defaultMoodForNow(evening)

        assertEquals(MoodType.RELAXED, mood)
    }

    @Test
    fun `gece saatlerinde varsayilan mood RELAXED olmali`() {
        // 22-04 -> NIGHT
        val night = LocalTime.of(23, 30)

        val mood = RecommendationEngine.defaultMoodForNow(night)

        assertEquals(MoodType.RELAXED, mood)
    }

    @Test
    fun `gece yarisindan sonra erken saatler de NIGHT bucket icinde olmali`() {
        // 02:00 -> NIGHT (22-04 araligi)
        val earlyMorning = LocalTime.of(2, 0)

        val mood = RecommendationEngine.defaultMoodForNow(earlyMorning)

        assertEquals(MoodType.RELAXED, mood)
    }

    @Test
    fun `MORNING bucket sinir testi - 5 ve 11 saatleri`() {
        val fiveAM = LocalTime.of(5, 0)
        val elevenAM = LocalTime.of(11, 59)

        assertEquals(MoodType.FOCUSED, RecommendationEngine.defaultMoodForNow(fiveAM))
        assertEquals(MoodType.FOCUSED, RecommendationEngine.defaultMoodForNow(elevenAM))
    }
}
