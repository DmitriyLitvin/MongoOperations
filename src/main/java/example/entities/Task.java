package example.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("task")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    @Id
    private String id;
    private String name;
    private String description;
    private int storyPoints;
    private int sprintNumber;
    private boolean isDone;
    private Instant timeStamp = Instant.now();
    private int duration;
}
