package com.example.data.model

data class Personality(
    val id: String,
    val name: String,
    val title: String,
    val category: String,
    val description: String,
    val signatureQuote: String,
    val pitchMultiplier: Float, // 0.5f to 2.0f
    val speedMultiplier: Float, // 0.6f to 1.5f
    val reverbAmount: Float,    // 0.0f to 1.0f
    val bassBoost: Float,       // 0.0f to 1.0f
    val geminiStylePrompt: String,
    val sampleSpeech: String,
    val isCustom: Boolean = false
)

object PersonalityCatalog {
    val predefinedPersonalities = listOf(
        Personality(
            id = "mohammad_rafi",
            name = "Mohammad Rafi",
            title = "Legendary Indian Playback Singer",
            category = "Music & Arts",
            description = "Silky, soulful melodious voice with smooth classical vibrato, emotive modulation, and warmth.",
            signatureQuote = "Ehsaan tera hoga mujh par, dil chahta hai woh kehne do...",
            pitchMultiplier = 1.15f,
            speedMultiplier = 0.96f,
            reverbAmount = 0.35f,
            bassBoost = 0.20f,
            geminiStylePrompt = "Speak or sing in the iconic, deeply soulful, melodious singing style of legendary Bollywood singer Mohammad Rafi. Warm, expressive, melodic phrasing, poetic Hindi/Urdu nuances, polite and artistic demeanor.",
            sampleSpeech = "Namaskar doston. Zindagi ek sangeet hai, aur har sur mein ek geet chhupa hai. Gaate rahiye aur muskurate rahiye!"
        ),
        Personality(
            id = "imran_khan",
            name = "Imran Khan",
            title = "World Cup Champion & Political Leader",
            category = "Leaders & Orators",
            description = "Deep commanding baritone, authoritative pauses, charismatic husky cadence, passionate delivery.",
            signatureQuote = "Aap ne sab se pehle ghabrana nahi hai! Hakoomat aur qoum mil kar ladenge.",
            pitchMultiplier = 0.82f,
            speedMultiplier = 0.88f,
            reverbAmount = 0.15f,
            bassBoost = 0.45f,
            geminiStylePrompt = "Speak in the charismatic, authoritative, deep husky baritone style of Imran Khan, the cricket legend and political leader. Use measured, dramatic pauses, decisive oratorical inflection, occasional English and Urdu blend with unwavering conviction and his famous mindset 'Ghabrana nahi hai'.",
            sampleSpeech = "Dekhein, main aap ko ek baat bata doon. Insan tab harta hai jab wo haar maan leta hai. Tabdeeli aati hai mehnat se, kabhi ghabrana nahi hai!"
        ),
        Personality(
            id = "morgan_freeman",
            name = "Morgan Freeman",
            title = "Iconic Hollywood Narrator & Actor",
            category = "Cinema & Narration",
            description = "Godly resonant deep bass, majestic measured pacing, gentle gravel, soothing cinematic wisdom.",
            signatureQuote = "I'd like to tell you that Andy fought the good fight, and the sisters let him be.",
            pitchMultiplier = 0.72f,
            speedMultiplier = 0.84f,
            reverbAmount = 0.30f,
            bassBoost = 0.55f,
            geminiStylePrompt = "Narrate in the legendary, deep, rich, soothing, cinematic voice of Morgan Freeman. Slow majestic pacing, warm gravelly texture, philosophical wisdom, like narrating an epic documentary.",
            sampleSpeech = "Some birds aren't meant to be caged, their feathers are just too bright. And when they fly away, the world is a little more empty."
        ),
        Personality(
            id = "amitabh_bachchan",
            name = "Amitabh Bachchan",
            title = "The Big B & Bollywood Megastar",
            category = "Cinema & Narration",
            description = "Resounding thunderous baritone, dramatic weight, articulate Hindi diction, towering screen presence.",
            signatureQuote = "Rishte mein toh hum tumhare baap lagte hain, naam hai Shahenshah!",
            pitchMultiplier = 0.78f,
            speedMultiplier = 0.90f,
            reverbAmount = 0.25f,
            bassBoost = 0.50f,
            geminiStylePrompt = "Speak with the thunderous, heavy, resonant baritone of Amitabh Bachchan. Poetic, formal, grand Hindi diction, dramatic pauses, deep chest resonance, and commanding gravitas.",
            sampleSpeech = "Deviyon aur sajjano! Jeevan ek kashmakash hai, sangharsh hai. Lekin hausla agar buland ho, toh manzil khud chalkar aati hai."
        ),
        Personality(
            id = "david_attenborough",
            name = "David Attenborough",
            title = "Renowned Wildlife Naturalist",
            category = "Science & Nature",
            description = "Gentle British RP cadence, hushed awe, breathy whisper-tones, contagious wonder at nature.",
            signatureQuote = "Here, in the dense canopy of the ancient rainforest, life thrives in secret wonder.",
            pitchMultiplier = 0.98f,
            speedMultiplier = 0.88f,
            reverbAmount = 0.18f,
            bassBoost = 0.15f,
            geminiStylePrompt = "Speak like Sir David Attenborough narrating a BBC Planet Earth wildlife documentary. Hushed, dramatic whisper of wonder, polite British accent, articulate reverence for nature.",
            sampleSpeech = "Look closely at this remarkable creature. Surviving against all odds, in one of the most hostile environments on our planet."
        ),
        Personality(
            id = "elvis_presley",
            name = "Elvis Presley",
            title = "The King of Rock and Roll",
            category = "Music & Arts",
            description = "Rich rockabilly baritone-tenor, sultry southern drawl, vibrant swagger and soulful echo.",
            signatureQuote = "Thank you, thank you very much! A little less conversation, a little more action.",
            pitchMultiplier = 0.90f,
            speedMultiplier = 1.05f,
            reverbAmount = 0.40f,
            bassBoost = 0.30f,
            geminiStylePrompt = "Speak with the smooth, charismatic southern drawl and rockabilly charm of Elvis Presley. Warm vibrato, playful swagger, cool demeanor, saying 'Thank you very much'.",
            sampleSpeech = "Well, hello there pretty mama. Keep your rhythm going, stay cool, and let the good music rock your soul. Thank you very much!"
        ),
        Personality(
            id = "taylor_swift",
            name = "Taylor Swift",
            title = "Global Pop & Songwriting Icon",
            category = "Music & Arts",
            description = "Bright melodic mezzo-soprano, crisp modern pop cadence, expressive conversational storytelling.",
            signatureQuote = "Cause the players gonna play, play, play, play, play... And the haters gonna hate!",
            pitchMultiplier = 1.28f,
            speedMultiplier = 1.08f,
            reverbAmount = 0.28f,
            bassBoost = 0.10f,
            geminiStylePrompt = "Speak in the bright, emotive, melodic, modern pop storytelling voice of Taylor Swift. Energetic, warm, conversational, catchy inflection with poetic flair.",
            sampleSpeech = "Hey guys! I just wanted to say that sometimes life gets messy, but you just have to shake it off, write your story, and never let anyone dim your sparkle!"
        )
    )

    fun createCustomPersonality(name: String, notes: String = ""): Personality {
        return Personality(
            id = "custom_" + name.lowercase().replace("\\s+".toRegex(), "_"),
            name = name,
            title = "Custom Celebrity Personality",
            category = "Custom",
            description = if (notes.isNotBlank()) notes else "Custom personality voice conversion for $name",
            signatureQuote = "Voice morphed into $name",
            pitchMultiplier = 1.0f,
            speedMultiplier = 1.0f,
            reverbAmount = 0.20f,
            bassBoost = 0.25f,
            geminiStylePrompt = "Speak in the authentic, recognizable voice, tone, vocabulary, cadence, and mannerisms of $name. If $name is a singer or public figure, capture their unique delivery and personality vividly.",
            sampleSpeech = "Hello, this is $name speaking in my distinctive voice and signature style.",
            isCustom = true
        )
    }
}
