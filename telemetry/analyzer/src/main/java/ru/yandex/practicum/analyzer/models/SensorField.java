package ru.yandex.practicum.analyzer.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SensorField {
    // Датчик движения
    MOTION("motion"),

    // Датчик температуры
    TEMPERATURE_C("temperature_c"),
    TEMPERATURE_F("temperature_f"),
    TEMPERATURE("temperature"),

    // Датчик освещенности
    LUMINOSITY("luminosity"),

    // Климат-датчик
    HUMIDITY("humidity"),
    CO2("co2"),

    // Датчик-переключатель
    STATE("state");

    private final String name;
}