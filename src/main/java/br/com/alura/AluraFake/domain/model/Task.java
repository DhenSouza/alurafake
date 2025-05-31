package br.com.alura.AluraFake.domain.model;

import br.com.alura.AluraFake.domain.enumeration.Type;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 4, max = 255)
    @Column(nullable = false)
    private String statement;

    @Column(nullable = false, name = "task_order")
    private Integer order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private Type typeTask;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TaskOption> options = new ArrayList<>();

    public void addOption(TaskOption option) {
        this.options.add(option);
        option.setTask(this);
    }

    public void removeOption(TaskOption option) {
        this.options.remove(option);
        option.setTask(null);
    }

}
