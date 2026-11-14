package com.jbncode.bibliaapp.core.locale

/** Biblias recomendadas por idioma (códigos de listaBiblia.json). La primera es la sugerida. */
object RegionBibleMap {
    private val byLanguage = mapOf(
        "es" to listOf("tla", "rvr60", "ntv"),   // tla viene incluida (sin descarga),
        "en" to listOf("niv1984", "kjv", "gnt"),
        "fr" to listOf("lsg", "bds"),
        "pt" to listOf("arc09", "nvipt"),
        "ro" to listOf("rmnn")
    )

    // Ajustes por país (ISO-2). Solo cambian el ORDEN; añade aquí lo que necesites.
    private val byCountry = mapOf(
        "GB" to listOf("kjv", "niv1984", "gnt")
    )

    fun recommended(language: String, country: String): List<String> =
        byCountry[country.uppercase()]?.takeIf { language == "en" }
            ?: byLanguage[language]
            ?: byLanguage.getValue("en")
}
