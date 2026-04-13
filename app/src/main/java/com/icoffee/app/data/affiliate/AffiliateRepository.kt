package com.icoffee.app.data.affiliate

import com.icoffee.app.data.model.AffiliateOffer
import com.icoffee.app.data.model.AffiliateTag
import com.icoffee.app.data.model.CoffeeType

object AffiliateRepository {

    // ─── Country-based bean offers ────────────────────────────────────────────
    private val byCountry: Map<String, AffiliateOffer> = mapOf(
        "brazil" to AffiliateOffer(
            id = "aff-brazil-01",
            title = "Brazilian Santos — Bag of 250g",
            subtitle = "Full-bodied, low-acidity. A classic cup from the world's top producer.",
            destinationUrl = "https://www.hasbean.co.uk/collections/brazil",
            retailerName = "Has Bean",
            priceHint = "From £8.50",
            tag = AffiliateTag.BEAN,
            relatedKey = "brazil"
        ),
        "ethiopia" to AffiliateOffer(
            id = "aff-ethiopia-01",
            title = "Ethiopian Yirgacheffe — Single Origin",
            subtitle = "Floral, jasmine and bright citrus. The birthplace of coffee.",
            destinationUrl = "https://www.hasbean.co.uk/collections/ethiopia",
            retailerName = "Has Bean",
            priceHint = "From £9.50",
            tag = AffiliateTag.BEAN,
            relatedKey = "ethiopia"
        ),
        "colombia" to AffiliateOffer(
            id = "aff-colombia-01",
            title = "Colombian Huila — Fresh Roast",
            subtitle = "Caramel sweetness with mild citrus. A crowd-pleaser origin.",
            destinationUrl = "https://www.hasbean.co.uk/collections/colombia",
            retailerName = "Has Bean",
            priceHint = "From £8.75",
            tag = AffiliateTag.BEAN,
            relatedKey = "colombia"
        ),
        "kenya" to AffiliateOffer(
            id = "aff-kenya-01",
            title = "Kenyan AA — Washed Process",
            subtitle = "Bold black currant, juicy acidity. Prized by specialty roasters.",
            destinationUrl = "https://www.hasbean.co.uk/collections/kenya",
            retailerName = "Has Bean",
            priceHint = "From £10.00",
            tag = AffiliateTag.BEAN,
            relatedKey = "kenya"
        ),
        "indonesia" to AffiliateOffer(
            id = "aff-indonesia-01",
            title = "Sumatra Mandheling — Dark & Earthy",
            subtitle = "Syrupy body, low acidity, cedar and dark chocolate.",
            destinationUrl = "https://www.hasbean.co.uk/collections/indonesia",
            retailerName = "Has Bean",
            priceHint = "From £9.00",
            tag = AffiliateTag.BEAN,
            relatedKey = "indonesia"
        ),
        "guatemala" to AffiliateOffer(
            id = "aff-guatemala-01",
            title = "Guatemalan Antigua — Highland Washed",
            subtitle = "Toffee sweetness and gentle spice from volcanic soils.",
            destinationUrl = "https://www.hasbean.co.uk/collections/guatemala",
            retailerName = "Has Bean",
            priceHint = "From £8.50",
            tag = AffiliateTag.BEAN,
            relatedKey = "guatemala"
        ),
        "costa_rica" to AffiliateOffer(
            id = "aff-costa-rica-01",
            title = "Costa Rican Tarrazu — Honey Process",
            subtitle = "Clean, juicy sweetness with stone fruit and milk chocolate.",
            destinationUrl = "https://www.hasbean.co.uk/collections/costa-rica",
            retailerName = "Has Bean",
            priceHint = "From £9.25",
            tag = AffiliateTag.BEAN,
            relatedKey = "costa_rica"
        ),
        "yemen" to AffiliateOffer(
            id = "aff-yemen-01",
            title = "Yemeni Mocha — Natural Dry Process",
            subtitle = "Ancient, wild, complex — wine-like and deeply aromatic.",
            destinationUrl = "https://www.hasbean.co.uk/collections/yemen",
            retailerName = "Has Bean",
            priceHint = "From £14.00",
            tag = AffiliateTag.BEAN,
            relatedKey = "yemen"
        )
    )

    // ─── Brew method gear offers ───────────────────────────────────────────────
    private val byBrewMethod: Map<String, AffiliateOffer> = mapOf(
        "espresso" to AffiliateOffer(
            id = "aff-espresso-01",
            title = "Sage Barista Express — Home Espresso",
            subtitle = "Built-in grinder, 15-bar extraction, steam wand. Pro espresso at home.",
            destinationUrl = "https://www.sageappliances.com/uk/en/products/espresso/bes870.html",
            retailerName = "Sage",
            priceHint = "From £599",
            tag = AffiliateTag.GEAR,
            relatedKey = "espresso"
        ),
        "pour_over" to AffiliateOffer(
            id = "aff-pour-over-01",
            title = "Hario V60 Starter Kit",
            subtitle = "The essential pour-over dripper, filter papers, and gooseneck kettle.",
            destinationUrl = "https://www.hario-europe.com/",
            retailerName = "Hario",
            priceHint = "From £35",
            tag = AffiliateTag.GEAR,
            relatedKey = "pour_over"
        ),
        "french_press" to AffiliateOffer(
            id = "aff-french-press-01",
            title = "Bodum Chambord 8-Cup French Press",
            subtitle = "The classic. Timeless chrome design, perfect full immersion brew.",
            destinationUrl = "https://www.bodum.com/gb/coffee/french-press",
            retailerName = "Bodum",
            priceHint = "From £24",
            tag = AffiliateTag.GEAR,
            relatedKey = "french_press"
        ),
        "aeropress" to AffiliateOffer(
            id = "aff-aeropress-01",
            title = "AeroPress Original",
            subtitle = "Versatile, portable, fast. From espresso-style to smooth cold brew.",
            destinationUrl = "https://aeropress.com/",
            retailerName = "AeroPress",
            priceHint = "From £29",
            tag = AffiliateTag.GEAR,
            relatedKey = "aeropress"
        ),
        "moka_pot" to AffiliateOffer(
            id = "aff-moka-pot-01",
            title = "Bialetti Moka Express 3-Cup",
            subtitle = "The iconic stovetop brewer. Strong, rich coffee in minutes.",
            destinationUrl = "https://www.bialetti.com/en_us/shop/coffee-makers/stovetop.html",
            retailerName = "Bialetti",
            priceHint = "From £22",
            tag = AffiliateTag.GEAR,
            relatedKey = "moka_pot"
        ),
        "cold_brew" to AffiliateOffer(
            id = "aff-cold-brew-01",
            title = "Hario Mizudashi Cold Brew Pot",
            subtitle = "1-litre capacity, fine mesh filter, fridge-ready. Smooth cold brew every time.",
            destinationUrl = "https://www.hario-europe.com/",
            retailerName = "Hario",
            priceHint = "From £20",
            tag = AffiliateTag.GEAR,
            relatedKey = "cold_brew"
        ),
        "chemex" to AffiliateOffer(
            id = "aff-chemex-01",
            title = "Chemex 6-Cup Classic",
            subtitle = "Iconic hourglass pour-over. Bonded filters for crystal-clear cups.",
            destinationUrl = "https://www.chemexcoffeemaker.com/",
            retailerName = "Chemex",
            priceHint = "From £45",
            tag = AffiliateTag.GEAR,
            relatedKey = "chemex"
        ),
        "siphon" to AffiliateOffer(
            id = "aff-siphon-01",
            title = "Hario Technica 5-Cup Siphon",
            subtitle = "Theatre and flavour in one. Full immersion vacuum brewing at its finest.",
            destinationUrl = "https://www.hario-europe.com/",
            retailerName = "Hario",
            priceHint = "From £80",
            tag = AffiliateTag.GEAR,
            relatedKey = "siphon"
        )
    )

    // ─── Coffee-type subscription/product offers ───────────────────────────────
    private val byCoffeeType: Map<CoffeeType, AffiliateOffer> = mapOf(
        CoffeeType.WHOLE_BEAN to AffiliateOffer(
            id = "aff-whole-bean-01",
            title = "Pact Coffee — Whole Bean Subscription",
            subtitle = "Freshly roasted single-origin beans delivered every 1–2 weeks.",
            destinationUrl = "https://www.pactcoffee.com/",
            retailerName = "Pact Coffee",
            priceHint = "From £6.95 / bag",
            tag = AffiliateTag.SUBSCRIPTION,
            relatedKey = "WHOLE_BEAN"
        ),
        CoffeeType.GROUND to AffiliateOffer(
            id = "aff-ground-01",
            title = "Volcano Coffee Works — Ground Boxes",
            subtitle = "Award-winning specialty ground coffee. Roasted fresh per order.",
            destinationUrl = "https://volcanocoffeeworks.com/",
            retailerName = "Volcano Coffee Works",
            priceHint = "From £8.50",
            tag = AffiliateTag.BEAN,
            relatedKey = "GROUND"
        ),
        CoffeeType.CAPSULE to AffiliateOffer(
            id = "aff-capsule-01",
            title = "Nespresso Vertuo — Variety Pack",
            subtitle = "Explore 8 roast profiles. Compostable capsules, crema at every pour.",
            destinationUrl = "https://www.nespresso.com/uk/en/",
            retailerName = "Nespresso",
            priceHint = "From £4.00 / 10 capsules",
            tag = AffiliateTag.SUBSCRIPTION,
            relatedKey = "CAPSULE"
        ),
        CoffeeType.INSTANT to AffiliateOffer(
            id = "aff-instant-01",
            title = "Huel Black Coffee Edition",
            subtitle = "Premium freeze-dried instant with a cleaner ingredient profile.",
            destinationUrl = "https://huel.com/",
            retailerName = "Huel",
            priceHint = "From £3.00 / sachet",
            tag = AffiliateTag.BEAN,
            relatedKey = "INSTANT"
        ),
        CoffeeType.READY_TO_DRINK to AffiliateOffer(
            id = "aff-rtd-01",
            title = "Minor Figures — Oat Milk Lattes",
            subtitle = "Barista-quality cold brew lattes in a can. No compromise.",
            destinationUrl = "https://www.minorfigures.com/",
            retailerName = "Minor Figures",
            priceHint = "From £2.20 / can",
            tag = AffiliateTag.BEAN,
            relatedKey = "READY_TO_DRINK"
        )
    )

    fun forCountry(countryId: String): AffiliateOffer? = byCountry[countryId.lowercase()]

    fun forBrewMethod(methodId: String): AffiliateOffer? = byBrewMethod[methodId.lowercase()]

    fun forCoffeeType(coffeeType: CoffeeType): AffiliateOffer? = byCoffeeType[coffeeType]
}
