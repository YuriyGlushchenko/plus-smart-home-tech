package ru.yandex.practicum.analyzer.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class Action {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType type;

    private Integer value;

    @OneToMany(mappedBy = "action", cascade = CascadeType.ALL)
    private List<ScenarioAction> scenarioActions;
}
