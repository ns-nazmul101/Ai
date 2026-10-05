package com.example.ui.model

enum class AiStatus(val labelEn: String, val labelBn: String) {
    IDLE("Idle", "প্রস্তুত"),
    LISTENING("Listening...", "শুনছি..."),
    THINKING("Thinking...", "ভাবছি..."),
    EXECUTING("Executing...", "সম্পাদন করছি..."),
    COMPLETED("Completed", "সম্পন্ন"),
    ERROR("Error", "ত্রুটি")
}
