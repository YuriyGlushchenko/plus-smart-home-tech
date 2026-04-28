package ru.yandex.practicum.analyzer.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "scenarios", uniqueConstraints = @UniqueConstraint(columnNames = {"hub_id", "name"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Scenario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hub_id", nullable = false)
    private String hubId;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ScenarioCondition> scenarioConditions = new HashSet<>();  // Set вместо List

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ScenarioAction> scenarioActions = new HashSet<>();
}