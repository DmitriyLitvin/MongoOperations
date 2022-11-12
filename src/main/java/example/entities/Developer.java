package example.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document("developer")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Developer {
    @Id
    private String id;
    private String name;
    private Double salary;
    private int age;
    private String seniority;
    private int experiences;
    private boolean maritalStatus;
    private String nationality;
    private String gender;
    private List<Task> tasks;
    private List<String> skills;
}