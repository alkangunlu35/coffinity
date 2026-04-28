package com.icoffee.app.data.membership

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MembershipEntitlementsTest {

    // ============================================
    // MembershipPlan.fromStorage tests
    // ============================================

    @Test
    fun `fromStorage 'free' string'ini FREE'ye cevirmeli`() {
        assertEquals(MembershipPlan.FREE, MembershipPlan.fromStorage("free"))
    }

    @Test
    fun `fromStorage 'premium' string'ini PREMIUM'a cevirmeli`() {
        assertEquals(MembershipPlan.PREMIUM, MembershipPlan.fromStorage("premium"))
    }

    @Test
    fun `fromStorage 'business' string'ini BUSINESS'a cevirmeli`() {
        assertEquals(MembershipPlan.BUSINESS, MembershipPlan.fromStorage("business"))
    }

    @Test
    fun `fromStorage uppercase string'i kabul etmeli`() {
        assertEquals(MembershipPlan.PREMIUM, MembershipPlan.fromStorage("PREMIUM"))
        assertEquals(MembershipPlan.BUSINESS, MembershipPlan.fromStorage("Business"))
    }

    @Test
    fun `fromStorage whitespace temizlemeli`() {
        assertEquals(MembershipPlan.FREE, MembershipPlan.fromStorage("  free  "))
    }

    @Test
    fun `fromStorage null icin FREE dondurmeli (default)`() {
        assertEquals(MembershipPlan.FREE, MembershipPlan.fromStorage(null))
    }

    @Test
    fun `fromStorage bilinmeyen string icin FREE dondurmeli (default)`() {
        assertEquals(MembershipPlan.FREE, MembershipPlan.fromStorage("enterprise"))
        assertEquals(MembershipPlan.FREE, MembershipPlan.fromStorage(""))
        assertEquals(MembershipPlan.FREE, MembershipPlan.fromStorage("   "))
    }

    // ============================================
    // MembershipEntitlementResolver.resolve tests
    // ============================================

    @Test
    fun `FREE plan dogru limitlere sahip olmali`() {
        val ent = MembershipEntitlementResolver.resolve(MembershipPlan.FREE)

        assertEquals(4, ent.monthlyJoinLimit)
        assertEquals(1, ent.monthlyCreateLimit)
        assertEquals(10, ent.maxAttendeesPerEvent)
    }

    @Test
    fun `PREMIUM plan unlimited join'a sahip olmali`() {
        val ent = MembershipEntitlementResolver.resolve(MembershipPlan.PREMIUM)

        assertNull("PREMIUM'da join limiti olmamali", ent.monthlyJoinLimit)
        assertEquals(10, ent.monthlyCreateLimit)
        assertEquals(20, ent.maxAttendeesPerEvent)
    }

    @Test
    fun `BUSINESS plan tum unlimited limit'lere sahip olmali`() {
        val ent = MembershipEntitlementResolver.resolve(MembershipPlan.BUSINESS)

        assertNull("BUSINESS'da join limiti olmamali", ent.monthlyJoinLimit)
        assertNull("BUSINESS'da create limiti olmamali", ent.monthlyCreateLimit)
        assertEquals(100, ent.maxAttendeesPerEvent)
    }

    @Test
    fun `plan upgrade ile maxAttendees artmali`() {
        val freeMax = MembershipEntitlementResolver.resolve(MembershipPlan.FREE).maxAttendeesPerEvent
        val premiumMax = MembershipEntitlementResolver.resolve(MembershipPlan.PREMIUM).maxAttendeesPerEvent
        val businessMax = MembershipEntitlementResolver.resolve(MembershipPlan.BUSINESS).maxAttendeesPerEvent

        assert(premiumMax > freeMax) { "PREMIUM > FREE olmali" }
        assert(businessMax > premiumMax) { "BUSINESS > PREMIUM olmali" }
    }
}
