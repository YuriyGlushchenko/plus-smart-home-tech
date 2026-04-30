package ru.yandex.practicum.analyzer.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sensors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {
    @Id  // без @GeneratedValue, т.к. id приходит из сообщений kafka (генерится внешними сервисами).
    private String id;

    @Column(name = "hub_id", nullable = false)
    private String hubId;
}