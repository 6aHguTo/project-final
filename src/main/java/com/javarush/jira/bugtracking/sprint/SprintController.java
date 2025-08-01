package com.javarush.jira.bugtracking.sprint;

import com.javarush.jira.bugtracking.Handlers;
import com.javarush.jira.bugtracking.project.ProjectRepository;
import com.javarush.jira.bugtracking.sprint.to.SprintTo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.javarush.jira.bugtracking.ObjectType.SPRINT;
import static com.javarush.jira.common.BaseHandler.REST_URL;
import static com.javarush.jira.common.BaseHandler.createdResponse;

@RestController
@Validated
@RequestMapping(value = REST_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class SprintController {
    private final ProjectRepository projectRepository;

    private final Handlers.SprintHandler handler;

    @GetMapping("/sprints/{id}")
    @Operation(
            summary = "Отримати спринт за ID",
            description = "Повертає повну інформацію про спринт із зазначеним ID"
    )
    public SprintTo get(@PathVariable long id) {
        return handler.getTo(id);
    }

    @GetMapping("/sprints/by-project")
    @Operation(
            summary = "Отримати спринти за проєктом",
            description = "Повертає список усіх спринтів для проєкту. Якщо enabled=true, то тільки активні"
    )
    public List<SprintTo> getAllByProject(@RequestParam long projectId, @RequestParam @Nullable Boolean enabled) {
        log.info("get all for project {} and enabled {}", projectId, enabled);
        checkProjectExists(projectId);
        return handler.getMapper().toToList(
                enabled != Boolean.TRUE ?
                        handler.getRepository().getAllByProject(projectId) :
                        handler.getRepository().getAllEnabledByProject(projectId)
        );
    }

    private void checkProjectExists(long id) {
        projectRepository.getExisted(id);
    }

    @GetMapping("/sprints/by-project-and-status")
    @Operation(
            summary = "Отримати спринти за статусом",
            description = "Повертає спринти проєкту з певним статусом (наприклад: active, done)"
    )
    public List<Sprint> getAllByProjectAndStatus(@RequestParam long projectId, @NotBlank @RequestParam String statusCode) {
        log.info("get all {} sprints for project with id={}", statusCode, projectId);
        checkProjectExists(projectId);
        return handler.getRepository().getAllByProjectAndStatus(projectId, statusCode);
    }

    @PostMapping(path = "/mngr/sprints", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Створити спринт",
            description = "Створює новий спринт у вказаному проєкті"
    )
    public ResponseEntity<Sprint> createWithLocation(@Valid @RequestBody SprintTo sprintTo) {
        Sprint created = handler.createWithBelong(sprintTo, SPRINT, "sprint_author");
        return createdResponse(REST_URL + "/sprints", created);
    }

    @PutMapping(path = "/mngr/sprints/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Оновити спринт",
            description = "Оновлює існуючий спринт за його ID"
    )
    public void update(@Validated @RequestBody SprintTo sprintTo, @PathVariable long id) {
        handler.updateFromTo(sprintTo, id);
    }

    @Transactional
    @PatchMapping("/mngr/sprints/{id}/change-status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Змінити статус спринта",
            description = "Змінює статус спринта (наприклад, з active на done)"
    )
    public void changeStatusCode(@PathVariable long id, @RequestParam String statusCode) {
        log.info("change statusCode of sprint {}", id);
        Sprint sprint = handler.getRepository().getExisted(id);
        sprint.setStatusCode(statusCode);
    }

    @PatchMapping("/mngr/sprints/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Активувати/деактивувати спринт",
            description = "Вмикає або вимикає спринт (enabled = true/false)"
    )
    public void enable(@PathVariable long id, @RequestParam boolean enabled) {
        handler.enable(id, enabled);
    }
}
