package example;

import example.entities.Developer;
import example.entities.Project;
import example.entities.Task;
import example.repositories.DeveloperRepository;
import example.repositories.ProjectRepository;
import example.repositories.TaskRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@SpringBootApplication
@EnableMongoRepositories
public class MongoSpringBootApplication implements ApplicationRunner {
    private final ProjectRepository projectRepository;

    private final TaskRepository taskRepository;

    private final DeveloperRepository developerRepository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public MongoSpringBootApplication(MongoTemplate mongoTemplate, ProjectRepository projectRepository, TaskRepository taskRepository, DeveloperRepository developerRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.developerRepository = developerRepository;
        this.mongoTemplate = mongoTemplate;
    }

    //statistics by dev name
    public List<DeveloperStat> getStatsForDeveloper() {
        TypedAggregation<Developer> agg = newAggregation(Developer.class,
                unwind("tasks"),
                project("name", "salary", "tasks").andExpression("tasks.timeStamp + tasks.duration").as("endDates"),
                group("name").last("name").as("name")
                        .sum("tasks.storyPoints").as("totalStoryPoints")
                        .sum("salary").as("totalSalary")
                        .avg("salary").as("avgSalary")
                        .addToSet("endDates").as("endDates"),
                project("name", "totalSalary", "avgSalary", "totalStoryPoints", "endDates")
        );

        return mongoTemplate.aggregate(agg, DeveloperStat.class).getMappedResults();
    }

    // find projects where all developers did all tasks
    public List<ProjectStat> findProjectsWithDoneTasks() {
        TypedAggregation<Project> agg = newAggregation(Project.class,
                project("projectName", "developers").and("_id").as("projectId"),
                unwind("developers"),
                unwind("developers.tasks"),
                group("projectId").last("projectId").as("projectId")
                        .addToSet("developers.tasks.isDone").as("taskStatuses")
                        .first("projectName").as("projectName"),
                project().andExpression("projectId").as("projectId")
                        .andExpression("projectName").as("projectName")
                        .andExpression("taskStatuses").as("taskStatuses"),
                match(Criteria.where("taskStatuses").not().exists(false))
        );

        return mongoTemplate.aggregate(agg, ProjectStat.class).getMappedResults();
    }

    // find projects where each developer has more than some qty of tasks
    public List<ProjectStat> findProjectsWhereEachDeveloperHasMoreThanTasks(int qty) {
        List<AggregationOperation> aggregationOperations = new LinkedList<>();

        aggregationOperations.add(project("projectName", "developers").and("_id").as("projectId"));
        aggregationOperations.add(unwind("developers"));
        aggregationOperations.add(unwind("developers.tasks"));

        aggregationOperations.add(group("developers._id")
                .last("developers._id").as("developerId")
                .last("projectId").as("projectId")
                .last("projectName").as("projectName")
                .push("developers.tasks").as("tasks"));

        aggregationOperations.add(project("projectId", "developerId", "projectName")
                .and(ArrayOperators.arrayOf("tasks").length()).as("qtyOfTasks"));

        aggregationOperations.add(group("projectId")
                .last("projectId").as("projectId")
                .last("projectName").as("projectName")
                .push("qtyOfTasks").as("qtyOfTasks"));

        aggregationOperations.add(match(Criteria.where("qtyOfTasks").elemMatch(new Criteria().gte(qty))));
        aggregationOperations.add(project("projectId", "projectName"));

        return mongoTemplate.aggregate(newAggregation(Project.class, aggregationOperations), ProjectStat.class).getMappedResults();
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectStat {
        private String projectId;
        private String projectName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeveloperStat {
        private String name;
        private Double totalSalary;
        private Double avgSalary;
        private int totalStoryPoints;
        private List<Instant> endDates;
    }


    public static void main(String[] args) {
        SpringApplication.run(MongoSpringBootApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {

    }
}
