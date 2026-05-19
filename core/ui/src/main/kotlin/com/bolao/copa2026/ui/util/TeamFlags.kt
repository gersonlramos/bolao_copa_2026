package com.bolao.copa2026.ui.util

private fun isoToFlag(iso2: String): String {
    val offset = 0x1F1E6 - 'A'.code
    return iso2.uppercase().map { c -> String(Character.toChars(c.code + offset)) }.joinToString("")
}

private val TLA_TO_ISO2 = mapOf(
    // Hosts & CONCACAF
    "USA" to "US", "MEX" to "MX", "CAN" to "CA",
    "JAM" to "JM", "PAN" to "PA", "CRC" to "CR", "HON" to "HN",
    "SLV" to "SV", "GUA" to "GT", "TRI" to "TT", "CUB" to "CU",
    "HAI" to "HT", "DOM" to "DO", "CUR" to "CW",
    // CONMEBOL
    "BRA" to "BR", "ARG" to "AR", "COL" to "CO", "URU" to "UY",
    "ECU" to "EC", "PAR" to "PY", "PER" to "PE", "CHI" to "CL",
    "BOL" to "BO", "VEN" to "VE",
    // UEFA
    "ENG" to "GB", "WAL" to "GB", "SCO" to "GB", "NIR" to "GB",
    "FRA" to "FR", "GER" to "DE", "ESP" to "ES", "POR" to "PT",
    "NED" to "NL", "BEL" to "BE", "ITA" to "IT", "SUI" to "CH",
    "DEN" to "DK", "SWE" to "SE", "NOR" to "NO", "FIN" to "FI",
    "POL" to "PL", "CZE" to "CZ", "SVK" to "SK", "HUN" to "HU",
    "ROM" to "RO", "BUL" to "BG", "GRE" to "GR", "TUR" to "TR",
    "CRO" to "HR", "SRB" to "RS", "BIH" to "BA", "SVN" to "SI",
    "AUT" to "AT", "ISL" to "IS", "ALB" to "AL", "MKD" to "MK",
    "UKR" to "UA", "ISR" to "IL", "GEO" to "GE", "AZE" to "AZ",
    "KAZ" to "KZ", "ARM" to "AM", "MNE" to "ME", "LUX" to "LU",
    // CAF
    "MAR" to "MA", "SEN" to "SN", "CMR" to "CM", "GHA" to "GH",
    "NGA" to "NG", "CIV" to "CI", "EGY" to "EG", "ALG" to "DZ",
    "TUN" to "TN", "MLI" to "ML", "BFA" to "BF", "RSA" to "ZA",
    "COD" to "CD", "ANG" to "AO", "ZIM" to "ZW", "KEN" to "KE",
    "GUI" to "GN", "GAB" to "GA", "CAP" to "CV", "EQG" to "GQ",
    "MOZ" to "MZ", "ZAM" to "ZM", "TAN" to "TZ", "BEN" to "BJ",
    // AFC
    "JPN" to "JP", "KOR" to "KR", "AUS" to "AU", "IRN" to "IR",
    "IRI" to "IR", "KSA" to "SA", "QAT" to "QA", "UAE" to "AE",
    "IRQ" to "IQ", "JOR" to "JO", "OMA" to "OM", "BHR" to "BH",
    "LEB" to "LB", "SYR" to "SY", "CHN" to "CN", "IND" to "IN",
    "PHI" to "PH", "VIE" to "VN", "THA" to "TH", "IDN" to "ID",
    "MAS" to "MY", "UZB" to "UZ", "KGZ" to "KG", "TJK" to "TJ",
    "KWT" to "KW", "YEM" to "YE", "PAL" to "PS",
    // OFC
    "NZL" to "NZ", "FIJ" to "FJ",
)

fun tlaToFlag(tla: String): String {
    val iso2 = TLA_TO_ISO2[tla.uppercase()] ?: return "🏳"
    return isoToFlag(iso2)
}
