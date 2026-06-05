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
    "BRA" to "BR", "ARG" to "AR", "COL" to "CO", "URU" to "UY", "URY" to "UY",
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
    "GUI" to "GN", "GAB" to "GA", "CAP" to "CV", "CPV" to "CV", "EQG" to "GQ",
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

private val TLA_TO_NAME_PTBR = mapOf(
    // Hosts & CONCACAF
    "USA" to "Estados Unidos", "MEX" to "México", "CAN" to "Canadá",
    "JAM" to "Jamaica", "PAN" to "Panamá", "CRC" to "Costa Rica",
    "HON" to "Honduras", "SLV" to "El Salvador", "GUA" to "Guatemala",
    "TRI" to "Trinidad e Tobago", "CUB" to "Cuba", "HAI" to "Haiti",
    "DOM" to "República Dominicana", "CUR" to "Curaçao",
    // CONMEBOL
    "BRA" to "Brasil", "ARG" to "Argentina", "COL" to "Colômbia",
    "URU" to "Uruguai", "URY" to "Uruguai", "ECU" to "Equador", "PAR" to "Paraguai",
    "PER" to "Peru", "CHI" to "Chile", "BOL" to "Bolívia", "VEN" to "Venezuela",
    // UEFA
    "ENG" to "Inglaterra", "WAL" to "País de Gales", "SCO" to "Escócia",
    "NIR" to "Irlanda do Norte", "FRA" to "França", "GER" to "Alemanha",
    "ESP" to "Espanha", "POR" to "Portugal", "NED" to "Países Baixos",
    "BEL" to "Bélgica", "ITA" to "Itália", "SUI" to "Suíça",
    "DEN" to "Dinamarca", "SWE" to "Suécia", "NOR" to "Noruega",
    "FIN" to "Finlândia", "POL" to "Polônia", "CZE" to "República Tcheca",
    "SVK" to "Eslováquia", "HUN" to "Hungria", "ROM" to "Romênia",
    "BUL" to "Bulgária", "GRE" to "Grécia", "TUR" to "Turquia",
    "CRO" to "Croácia", "SRB" to "Sérvia", "BIH" to "Bósnia e Herzegovina",
    "SVN" to "Eslovênia", "AUT" to "Áustria", "ISL" to "Islândia",
    "ALB" to "Albânia", "MKD" to "Macedônia do Norte", "UKR" to "Ucrânia",
    "ISR" to "Israel", "GEO" to "Geórgia", "AZE" to "Azerbaijão",
    "KAZ" to "Cazaquistão", "ARM" to "Armênia", "MNE" to "Montenegro",
    "LUX" to "Luxemburgo",
    // CAF
    "MAR" to "Marrocos", "SEN" to "Senegal", "CMR" to "Camarões",
    "GHA" to "Gana", "NGA" to "Nigéria", "CIV" to "Costa do Marfim",
    "EGY" to "Egito", "ALG" to "Argélia", "TUN" to "Tunísia",
    "MLI" to "Mali", "BFA" to "Burkina Faso", "RSA" to "África do Sul",
    "COD" to "Rep. Dem. do Congo", "ANG" to "Angola", "ZIM" to "Zimbábue",
    "KEN" to "Quênia", "GUI" to "Guiné", "GAB" to "Gabão",
    "CAP" to "Cabo Verde", "CPV" to "Cabo Verde", "EQG" to "Guiné Equatorial", "MOZ" to "Moçambique",
    "ZAM" to "Zâmbia", "TAN" to "Tanzânia", "BEN" to "Benim",
    // AFC
    "JPN" to "Japão", "KOR" to "Coreia do Sul", "AUS" to "Austrália",
    "IRN" to "Irã", "IRI" to "Irã", "KSA" to "Arábia Saudita",
    "QAT" to "Catar", "UAE" to "Emirados Árabes Unidos", "IRQ" to "Iraque",
    "JOR" to "Jordânia", "OMA" to "Omã", "BHR" to "Bahrein",
    "LEB" to "Líbano", "SYR" to "Síria", "CHN" to "China",
    "IND" to "Índia", "PHI" to "Filipinas", "VIE" to "Vietnã",
    "THA" to "Tailândia", "IDN" to "Indonésia", "MAS" to "Malásia",
    "UZB" to "Uzbequistão", "KGZ" to "Quirguistão", "TJK" to "Tajiquistão",
    "KWT" to "Kuwait", "YEM" to "Iêmen", "PAL" to "Palestina",
    // OFC
    "NZL" to "Nova Zelândia", "FIJ" to "Fiji",
)

fun tlaToName(tla: String, fallback: String = tla): String =
    TLA_TO_NAME_PTBR[tla.uppercase()] ?: fallback
