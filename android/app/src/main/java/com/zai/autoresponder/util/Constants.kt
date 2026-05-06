package com.zai.autoresponder.util

/**
 * Constants for the AI Auto-Responder app
 */
object Constants {

    // Platforms
    const val PLATFORM_WHATSAPP = "whatsapp"
    const val PLATFORM_MESSENGER = "messenger"
    const val PLATFORM_TELEGRAM = "telegram"
    const val PLATFORM_FACEBOOK = "facebook"
    const val PLATFORM_INSTAGRAM = "instagram"

    // AI Personas
    const val PERSONA_PROFESSIONAL = "professional"
    const val PERSONA_FRIENDLY = "friendly"
    const val PERSONA_WITTY = "witty"
    const val PERSONA_MINIMAL = "minimal"

    // Persona prompts (matching the web app)
    val personaPrompts = mapOf(
        PERSONA_PROFESSIONAL to "You are a professional assistant. Respond formally and concisely. Keep responses brief and to the point.",
        PERSONA_FRIENDLY to "You are a friendly assistant. Respond warmly and casually, like chatting with a friend. Use a conversational tone.",
        PERSONA_WITTY to "You are a witty assistant. Respond with humor and charm while being helpful. Keep it light and engaging.",
        PERSONA_MINIMAL to "You are a minimal assistant. Respond with only the essential information. Be extremely brief and direct."
    )

    // Notification package names
    val platformPackages = mapOf(
        PLATFORM_WHATSAPP to listOf("com.whatsapp", "com.whatsapp.w4b"),
        PLATFORM_MESSENGER to listOf("com.facebook.orca", "com.facebook.mlite"),
        PLATFORM_TELEGRAM to listOf("org.telegram.messenger"),
        PLATFORM_FACEBOOK to listOf("com.facebook.katana"),
        PLATFORM_INSTAGRAM to listOf("com.instagram.android")
    )

    // Colors (matching web app CSS variables)
    object Colors {
        const val OBSIDIAN_DARK = "#0f1218"
        const val OBSIDIAN_MEDIUM = "#1a1f26"
        const val ACCENT_WARM = "#948979"
        const val ACCENT_SAND = "#DFD0B8"
        const val TEXT_PRIMARY = "#f5efe6"
        const val TEXT_SECONDARY = "#948979"

        // Platform colors
        const val WHATSAPP_GREEN = "#25D366"
        const val MESSENGER_BLUE = "#0099FF"
        const val TELEGRAM_BLUE = "#26A5E4"
        const val FACEBOOK_BLUE = "#1877F2"
        const val INSTAGRAM_PINK = "#E4405F"

        // Status colors
        const val GREEN = "#22c55e"
        const val RED = "#f43f5e"
        const val AMBER = "#f59e0b"
    }

    // Animation durations
    object Animations {
        const val DURATION_SHORT = 150L
        const val DURATION_MEDIUM = 300L
        const val DURATION_LONG = 500L
        const val GLOW_PULSE_DURATION = 2500L
        const val FLOAT_DURATION = 6000L
    }

    // Glass effect parameters (matching CSS)
    object GlassEffect {
        const val CARD_BLUR = 24f
        const val CARD_SATURATE = 140
        const val CARD_RADIUS = 16f
        const val HEADER_BLUR = 40f
        const val HEADER_SATURATE = 180
    }
}
