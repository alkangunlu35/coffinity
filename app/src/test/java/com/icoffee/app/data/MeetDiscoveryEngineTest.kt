package com.icoffee.app.data

import com.icoffee.app.data.model.MeetMood
import org.junit.Assert.assertEquals
import org.junit.Test

class MeetDiscoveryEngineTest {

    @Test
    fun `network kelimesi NETWORKING mood'una eslenmeli`() {
        val mood = MeetDiscoveryEngine.mapPurposeToMood("Networking")

        assertEquals(MeetMood.NETWORKING, mood)
    }

    @Test
    fun `study kelimesi PRODUCTIVE mood'una eslenmeli`() {
        val mood = MeetDiscoveryEngine.mapPurposeToMood("Study session")

        assertEquals(MeetMood.PRODUCTIVE, mood)
    }

    @Test
    fun `work kelimesi PRODUCTIVE mood'una eslenmeli`() {
        val mood = MeetDiscoveryEngine.mapPurposeToMood("Work meeting")

        assertEquals(MeetMood.PRODUCTIVE, mood)
    }

    @Test
    fun `focus kelimesi PRODUCTIVE mood'una eslenmeli`() {
        val mood = MeetDiscoveryEngine.mapPurposeToMood("Focus time")

        assertEquals(MeetMood.PRODUCTIVE, mood)
    }

    @Test
    fun `deep kelimesi DEEP_TALK mood'una eslenmeli`() {
        val mood = MeetDiscoveryEngine.mapPurposeToMood("Deep dive")

        assertEquals(MeetMood.DEEP_TALK, mood)
    }

    @Test
    fun `conversation kelimesi DEEP_TALK mood'una eslenmeli`() {
        val mood = MeetDiscoveryEngine.mapPurposeToMood("Long conversation")

        assertEquals(MeetMood.DEEP_TALK, mood)
    }

    @Test
    fun `bilinmeyen purpose CHILL'e eslenmeli (default)`() {
        val mood = MeetDiscoveryEngine.mapPurposeToMood("kahve içelim")

        assertEquals(MeetMood.CHILL, mood)
    }

    @Test
    fun `bos string CHILL'e eslenmeli`() {
        val mood = MeetDiscoveryEngine.mapPurposeToMood("")

        assertEquals(MeetMood.CHILL, mood)
    }

    @Test
    fun `case insensitive olmali - NETWORK uppercase`() {
        val mood = MeetDiscoveryEngine.mapPurposeToMood("NETWORK EVENT")

        assertEquals(MeetMood.NETWORKING, mood)
    }

    @Test
    fun `whitespace temizlenmeli - bosluklu input`() {
        val mood = MeetDiscoveryEngine.mapPurposeToMood("   study   ")

        assertEquals(MeetMood.PRODUCTIVE, mood)
    }
}
