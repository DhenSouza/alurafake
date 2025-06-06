package br.com.alura.AluraFake.domain.model;

import br.com.alura.AluraFake.domain.enumeration.Status;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Course {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String title;
    private String description;
    @ManyToOne
    private User instructor;
    @Setter
    @Enumerated(EnumType.STRING)
    private Status status;
    @Setter
    private LocalDateTime publishedAt;

    @Setter
    @OneToMany(
            mappedBy      = "course",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    @OrderBy("order ASC")
    private List<Task> tasks = new ArrayList<>();

    @Deprecated
    public Course(){}

    public Course(String title, String description, User instructor) {
        Assert.isTrue(instructor.isInstructor(), "The user must be an instructor.");
        this.title = title;
        this.instructor = instructor;
        this.description = description;
        this.status = Status.BUILDING;
    }

    public void addTask(Task task) {
        this.tasks.add(task);
        task.setCourse(this);
    }

    public void removeTask(Task task) {
        this.tasks.remove(task);
        task.setCourse(null);
    }

}
