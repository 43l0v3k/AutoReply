package com.example.autoreply

object Apps {
    data class App(val title: String, val packages: List<String>)

    val ALL = listOf(
        App("Telegram", listOf("org.telegram.messenger", "org.telegram.messenger.web")),
        App("WhatsApp", listOf("com.whatsapp", "com.whatsapp.w4b")),
        App("Instagram", listOf("com.instagram.android")),
        App("Viber", listOf("com.viber.voip")),
        App("VK", listOf("com.vkontakte.android")),
        App("MAX", listOf("ru.oneme.app"))
    )

    fun byPackage(pkg: String): App? = ALL.firstOrNull { pkg in it.packages }
}
