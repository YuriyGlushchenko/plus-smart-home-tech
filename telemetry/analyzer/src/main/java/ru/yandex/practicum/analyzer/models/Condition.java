package ru.yandex.practicum.analyzer.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "conditions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class Condition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConditionType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConditionOperation operation;

    private Integer value;

    @OneToMany(mappedBy = "condition", cascade = CascadeType.ALL)
    private List<ScenarioCondition> scenarioConditions;
}
