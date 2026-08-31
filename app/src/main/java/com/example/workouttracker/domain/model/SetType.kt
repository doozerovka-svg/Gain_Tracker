package com.example.workouttracker.domain.model

enum class SetType(
    val titleRu: String,
    val shortTag: String,
    val descriptionRu: String
) {
    NORMAL("Обычный", "О", "Рабочий подход"),
    WARMUP("Разминка", "Р", "Не входит в тоннаж/1RM"),
    DROP_SET("Дропсет", "Д", "Снижение веса"),
    FAILURE("Отказ", "!", "Мышечный отказ");

    val tag: String get() = shortTag
}
