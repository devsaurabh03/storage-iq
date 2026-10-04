package com.storage.intelligence.core.common

/**
 * Multilingual Lexicon and Lexical Analyzer for detecting "Good Morning",
 * greeting cards, festive wishes, and daily inspirational media.
 *
 * Covers major Indian languages (Hindi, Marathi, Gujarati, Tamil, Telugu,
 * Kannada, Malayalam, Bengali, Punjabi, Odia, Urdu, Assamese) in both native
 * scripts and Romanized/Hinglish transliterations, as well as global languages.
 */
object GreetingLexicon {

    /**
     * Lexical tokens across 12+ languages.
     * Normalized tokens (lowercase, alphanumeric).
     */
    private val GREETING_TOKENS: Set<String> = setOf(
        // --- English ---
        "goodmorning", "goodmorning", "morning", "gm", "riseandshine", "morningvibes",
        "morningquote", "morningwishes", "haveaniceday", "haveagreatday",
        "goodnight", "sweetdreams", "greetings", "greeting", "wishes", "wish",
        "dailyquote", "thoughtoftheday", "motivation", "blessings",

        // --- Hindi & Sanskrit (Romanized & Hinglish) ---
        "suprabhat", "shubhprabhat", "shubhdin", "ramram", "radheradhe", "radhekrishna",
        "jaishreekrishna", "jaishreeram", "jaishriram", "harharmahadev", "bholenath",
        "jaihanuman", "jaibajrangbali", "ganeshji", "jaiganesh", "shubham", "namaste",
        "namaskar", "shubhkamna", "shubhkamnaye", "shubhechha", "pranam", "charansparsh",
        "suvichar", "anmolvachan", "shayari", "sandesh", "prabhat",

        // --- Hindi & Sanskrit (Devanagari Script) ---
        "सुप्रभात", "शुभप्रभात", "शुभ प्रभात", "शुभदिन", "शुभ दिन", "राम राम", "राधे राधे",
        "जय श्री कृष्ण", "जय श्रीकृष्ण", "जय श्री राम", "जय श्रीराम", "हर हर महादेव",
        "जय बजरंग बली", "जय हनुमान", "जय गणेश", "शुभम", "नमस्ते", "नमस्कार",
        "शुभकामना", "शुभकामनाएं", "प्रणाम", "वंदन", "अभिनंदन", "सुविचार", "अनमोल वचन",

        // --- Marathi (Script + Romanized) ---
        "शुभ सकाळ", "शुभसकाळ", "सस्नेह नमस्कार", "राम कृष्ण हरी", "जय शिवराय",
        "shubhsakal", "sakal", "sasnehanamaskar", "ramkrishnahari", "jaishivray",

        // --- Gujarati (Script + Romanized) ---
        "શુભ સવાર", "શુભસવાર", "જય શ્રી કૃષ્ણ", "જય સ્વામિનારાયણ",
        "shubhsavar", "shubhsavaar", "jaiswaminarayan",

        // --- Tamil (Script + Romanized) ---
        "காலை வணக்கம்", "காலைவணக்கம்", "வணக்கம்", "வாழ்த்துக்கள்", "இனிய காலை",
        "kalaivanakkam", "kaalaivanakkam", "vanakkam", "vaazhthukkal", "valthukkal", "iniyakaalai",

        // --- Telugu (Script + Romanized) ---
        "శుభోదయం", "శుభోదయము", "నమస్కారం", "శుభాకాంక్షలు", "శుభదినం",
        "subhodayam", "shubhodayam", "shubhodhayam", "subhodhayam", "namaskaraalu",
        "shubhakaankshalu", "subhakaankshalu", "shubhadinam",

        // --- Kannada (Script + Romanized) ---
        "ಶುಭೋದಯ", "ಶುಭೋದಯ", "ನಮಸ್ಕಾರ", "ಶುಭಾಶಯಗಳು",
        "shubhodaya", "subhodaya", "shubhodhaya", "subhodhaya", "namaskara", "shubhashayagalu",

        // --- Malayalam (Script + Romanized) ---
        "സുപ്രഭാതം", "നമസ്കാരം", "ശുഭദിനം", "ആശംസകൾ",
        "suprabhatham", "suprabhatam", "shubhadinam", "subhadinam", "aashamsakal",

        // --- Bengali & Assamese (Script + Romanized) ---
        "শুভ সকাল", "শুভসকাল", "নমস্কার", "প্রণাম", "শুভেচ্ছা", "সুপ্ৰভাত", "শুভ পুৱা",
        "shubhoshokal", "shubhasakal", "nomoshkar", "shubhopuwa",

        // --- Punjabi (Gurmukhi + Romanized) ---
        "ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ", "ਸ਼ੁਭ ਸਵੇਰ", "ਸ਼ੁਭਸਵੇਰ", "ਵਾਹਿਗੁਰੂ",
        "satsriakal", "shubhsaver", "shubhsawer", "waheguru",

        // --- Odia (Script + Romanized) ---
        "ଶୁଭ ସକାଳ", "ନମସ୍କାର", "ଜୟ ଜଗନ୍ନାଥ",
        "shubhasakala", "jaijagannath",

        // --- Urdu & Islamic Greetings (Arabic Script + Romanized) ---
        "صبح بخیر", "السلام علیکم", "شب بخیر", "جمعہ مبارک",
        "subahbakhair", "subahba-khair", "assalamoalaikum", "assalamualaikum",
        "shabbakhair", "jummamubarak", "eidmubarak",

        // --- Indian Festivals & Auspicious Wishes ---
        "diwali", "deepavali", "दीपावली", "दिवाली", "holi", "होली", "navratri", "नवरात्रि",
        "dussehra", "दशहरा", "rakshabandhan", "रक्षाबंधन", "ganeshchaturthi", "गणेशोत्सव",
        "makarsankranti", "मकर संक्रांति", "pongal", "பொங்கல்", "onam", "ഓണം", "vishu",
        "baisakhi", "बैसाखी", "ugadi", "ಉಗಾದಿ", "gudipadwa", "गुढीपाडवा", "karwachauth",
        "chhathpuja", "छठ पूजा", "happynewyear", "navroz",

        // --- International Languages ---
        "buenosdias",      // Spanish
        "bonjour",         // French
        "bonmatin",        // French
        "gutenmorgen",     // German
        "buongiorno",      // Italian
        "bomdia",          // Portuguese
        "sabahalkhair",    // Arabic
        "صباح الخير"       // Arabic script
    )

    /**
     * Performs lexical analysis on a filename and file path.
     * Normalizes text by removing punctuation, delimiters, and spaces,
     * then matches against multilingual greeting tokens.
     */
    fun matchesGreetingLexicon(fileName: String, filePath: String): Boolean {
        val lowerName = fileName.lowercase()
        val lowerPath = filePath.lowercase()

        // 1. Exact substring check for multi-word or non-Latin tokens
        for (token in GREETING_TOKENS) {
            if (lowerName.contains(token) || lowerPath.contains(token)) {
                return true
            }
        }

        // 2. Normalized alphanumeric tokenization (e.g. "Good_Morning_2023.jpg" -> "goodmorning")
        val cleanName = lowerName.filter { it.isLetterOrDigit() }
        val cleanPath = lowerPath.filter { it.isLetterOrDigit() }

        for (token in GREETING_TOKENS) {
            val cleanToken = token.filter { it.isLetterOrDigit() }
            if (cleanToken.length >= 3) {
                if (cleanName.contains(cleanToken) || cleanPath.contains(cleanToken)) {
                    return true
                }
            }
        }

        // 3. Short prefix/suffix patterns like "gm_", "_gm.", "-gm-", "gn_"
        val shortPrefixes = listOf("gm_", "_gm", "-gm-", "gm-", "gn_", "_gn")
        if (shortPrefixes.any { lowerName.contains(it) || lowerPath.contains(it) }) {
            return true
        }

        return false
    }
}
