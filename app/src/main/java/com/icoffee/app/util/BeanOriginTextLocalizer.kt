package com.icoffee.app.util

import java.util.Locale

object BeanOriginTextLocalizer {
    private data class NoteTranslations(
        val tr: String,
        val de: String,
        val fr: String,
        val es: String,
        val ptBr: String
    )

    private val noteMap: Map<String, NoteTranslations> = mapOf(
        "chocolate" to NoteTranslations("Çikolata", "Schokolade", "Chocolat", "Chocolate", "Chocolate"),
        "dark chocolate" to NoteTranslations("Bitter Çikolata", "Dunkle Schokolade", "Chocolat noir", "Chocolate negro", "Chocolate amargo"),
        "milk chocolate" to NoteTranslations("Sütlü Çikolata", "Milchschokolade", "Chocolat au lait", "Chocolate con leche", "Chocolate ao leite"),
        "nutty" to NoteTranslations("Fındıksı", "Nussig", "Noisette", "Frutos secos", "Amendoado"),
        "caramel" to NoteTranslations("Karamel", "Karamell", "Caramel", "Caramelo", "Caramelo"),
        "toffee" to NoteTranslations("Toffee", "Toffee", "Toffee", "Toffee", "Toffee"),
        "mild" to NoteTranslations("Yumuşak", "Mild", "Doux", "Suave", "Suave"),
        "smooth" to NoteTranslations("Pürüzsüz", "Sanft", "Onctueux", "Suave", "Aveludado"),
        "full body" to NoteTranslations("Dolgun Gövde", "Voller Körper", "Corps généreux", "Cuerpo completo", "Corpo encorpado"),
        "light body" to NoteTranslations("Hafif Gövde", "Leichter Körper", "Corps léger", "Cuerpo ligero", "Corpo leve"),
        "balanced" to NoteTranslations("Dengeli", "Ausgewogen", "Équilibré", "Equilibrado", "Equilibrado"),
        "bright" to NoteTranslations("Canlı", "Lebendig", "Vif", "Vivo", "Vibrante"),
        "bright acidity" to NoteTranslations("Canlı Asidite", "Lebendige Säure", "Acidité vive", "Acidez viva", "Acidez vibrante"),
        "low acidity" to NoteTranslations("Düşük Asidite", "Niedrige Säure", "Faible acidité", "Baja acidez", "Baixa acidez"),
        "citrus" to NoteTranslations("Narenciye", "Zitrus", "Agrumes", "Cítrico", "Cítrico"),
        "citrus zest" to NoteTranslations("Narenciye Kabuğu", "Zitruszeste", "Zeste d’agrumes", "Ralladura cítrica", "Raspas cítricas"),
        "floral" to NoteTranslations("Çiçeksi", "Floral", "Floral", "Floral", "Floral"),
        "honey" to NoteTranslations("Bal", "Honig", "Miel", "Miel", "Mel"),
        "fruit" to NoteTranslations("Meyvemsi", "Fruchtig", "Fruité", "Frutal", "Frutado"),
        "fruity" to NoteTranslations("Meyvemsi", "Fruchtig", "Fruité", "Frutal", "Frutado"),
        "tropical fruit" to NoteTranslations("Tropik Meyve", "Tropische Früchte", "Fruits tropicaux", "Fruta tropical", "Frutas tropicais"),
        "stone fruit" to NoteTranslations("Çekirdekli Meyve", "Steinobst", "Fruit à noyau", "Fruta de hueso", "Fruta de caroço"),
        "dried fruit" to NoteTranslations("Kuru Meyve", "Trockenfrucht", "Fruit sec", "Fruta seca", "Frutas secas"),
        "red fruit" to NoteTranslations("Kırmızı Meyve", "Rote Früchte", "Fruits rouges", "Fruta roja", "Frutas vermelhas"),
        "dark fruit" to NoteTranslations("Koyu Meyve", "Dunkle Früchte", "Fruits noirs", "Fruta oscura", "Frutas escuras"),
        "berry" to NoteTranslations("Yaban Mersini Notası", "Beeren", "Baies", "Bayas", "Frutas vermelhas"),
        "blueberry" to NoteTranslations("Yaban Mersini", "Blaubeere", "Myrtille", "Arándano", "Mirtilo"),
        "blackcurrant" to NoteTranslations("Siyah Frenk Üzümü", "Schwarze Johannisbeere", "Cassis", "Grosella negra", "Cassis"),
        "peach" to NoteTranslations("Şeftali", "Pfirsich", "Pêche", "Durazno", "Pêssego"),
        "peach tea" to NoteTranslations("Şeftali Çayı", "Pfirsichtee", "Thé à la pêche", "Té de durazno", "Chá de pêssego"),
        "apricot" to NoteTranslations("Kayısı", "Aprikose", "Abricot", "Albaricoque", "Damasco"),
        "jasmine" to NoteTranslations("Yasemin", "Jasmin", "Jasmin", "Jazmín", "Jasmim"),
        "bergamot" to NoteTranslations("Bergamot", "Bergamotte", "Bergamote", "Bergamota", "Bergamota"),
        "cocoa" to NoteTranslations("Kakao", "Kakao", "Cacao", "Cacao", "Cacau"),
        "sweet" to NoteTranslations("Tatlı", "Süß", "Doux", "Dulce", "Doce"),
        "clean" to NoteTranslations("Temiz", "Sauber", "Net", "Limpio", "Limpo"),
        "delicate" to NoteTranslations("Narin", "Zart", "Délicat", "Delicado", "Delicado"),
        "intense" to NoteTranslations("Yoğun", "Intensiv", "Intense", "Intenso", "Intenso"),
        "bold" to NoteTranslations("Güçlü", "Kräftig", "Intense", "Intenso", "Marcante"),
        "concentrated" to NoteTranslations("Konsantre", "Konzentriert", "Concentré", "Concentrado", "Concentrado"),
        "refined" to NoteTranslations("Rafine", "Raffiniert", "Raffiné", "Refinado", "Refinado"),
        "wine-like" to NoteTranslations("Şarabımsı", "Weinartig", "Vineux", "Vinoso", "Vínico"),
        "winey" to NoteTranslations("Şarabımsı", "Weinartig", "Vineux", "Vinoso", "Vínico"),
        "wine" to NoteTranslations("Şarap Notası", "Wein", "Vin", "Vino", "Vinho"),
        "silky" to NoteTranslations("İpeksi", "Seidig", "Soyeux", "Sedoso", "Sedoso"),
        "spice" to NoteTranslations("Baharat", "Gewürz", "Épices", "Especias", "Especiarias"),
        "spicy" to NoteTranslations("Baharatlı", "Würzig", "Épicé", "Especiado", "Picante"),
        "smoky" to NoteTranslations("İsli", "Rauchig", "Fumé", "Ahumado", "Defumado"),
        "earthy" to NoteTranslations("Topraksı", "Erdig", "Terreux", "Terroso", "Terroso"),
        "tea-like" to NoteTranslations("Çay Benzeri", "Teeartig", "Type thé", "Tipo té", "Semelhante a chá"),
        "aromatic" to NoteTranslations("Aromatik", "Aromatisch", "Aromatique", "Aromático", "Aromático"),
        "soft" to NoteTranslations("Yumuşak", "Sanft", "Doux", "Suave", "Suave"),
        "wild" to NoteTranslations("Yabansı", "Wild", "Sauvage", "Silvestre", "Selvagem"),
        "complex" to NoteTranslations("Karmaşık", "Komplex", "Complexe", "Complejo", "Complexo"),
        "rich" to NoteTranslations("Zengin", "Reichhaltig", "Riche", "Rico", "Encorpado"),
        "juicy" to NoteTranslations("Sulu", "Saftig", "Juteux", "Jugoso", "Suculento"),
        "tomato" to NoteTranslations("Domates", "Tomate", "Tomate", "Tomate", "Tomate")
    )

    fun localizedFlavorNote(rawNote: String, appLanguageCode: String?): String {
        val language = normalizedLanguage(appLanguageCode)
        if (language == "en") return rawNote

        val key = normalizeKey(rawNote)
        val mapped = noteMap[key] ?: return rawNote
        return when (language) {
            "tr" -> mapped.tr
            "de" -> mapped.de
            "fr" -> mapped.fr
            "es" -> mapped.es
            "pt-br" -> mapped.ptBr
            else -> rawNote
        }
    }

    fun localizedDescription(
        rawDescription: String,
        countryName: String,
        flavorNotes: List<String>,
        appLanguageCode: String?
    ): String {
        val language = normalizedLanguage(appLanguageCode)
        if (language == "en") return rawDescription

        val localizedCountry = CountryDisplayNames.localizedName(countryName, appLanguageCode)
        val localizedNotes = flavorNotes
            .take(3)
            .map { localizedFlavorNote(it, appLanguageCode) }
            .filter { it.isNotBlank() }

        if (localizedNotes.isEmpty()) return rawDescription
        val notesText = localizedNotes.joinToString(", ")

        return when (language) {
            "tr" -> "$localizedCountry kökenli bu çekirdeğin öne çıkan notaları: $notesText."
            "de" -> "Bei dieser Bohne aus $localizedCountry stehen folgende Noten im Vordergrund: $notesText."
            "fr" -> "Pour ce café originaire de $localizedCountry, les notes dominantes sont : $notesText."
            "es" -> "Para este café de $localizedCountry, las notas destacadas son: $notesText."
            "pt-br" -> "Para este café de $localizedCountry, as notas em destaque são: $notesText."
            else -> rawDescription
        }
    }

    private fun normalizedLanguage(rawLanguageCode: String?): String {
        val normalized = rawLanguageCode
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.replace('_', '-')
            .orEmpty()

        return when {
            normalized == "tr" || normalized.startsWith("tr-") -> "tr"
            normalized == "en" || normalized.startsWith("en-") -> "en"
            normalized == "de" || normalized.startsWith("de-") -> "de"
            normalized == "fr" || normalized.startsWith("fr-") -> "fr"
            normalized == "es" || normalized.startsWith("es-") -> "es"
            normalized == "pt" || normalized == "pt-br" || normalized.startsWith("pt-") -> "pt-br"
            else -> "en"
        }
    }

    private fun normalizeKey(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace("’", "'")
}
