package com.javarush.jira.ref.internal.web;

import com.javarush.jira.ref.RefTo;
import com.javarush.jira.ref.RefType;
import com.javarush.jira.ref.ReferenceService;
import com.javarush.jira.ref.internal.Reference;
import com.javarush.jira.ref.internal.ReferenceMapper;
import com.javarush.jira.ref.internal.ReferenceRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.javarush.jira.common.BaseHandler.createdResponse;
import static com.javarush.jira.common.util.validation.ValidationUtil.checkNew;

@RestController
@RequestMapping(value = ReferenceController.REST_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@AllArgsConstructor
@Slf4j

public class ReferenceController {
    static final String REST_URL = "/api/admin/refs";
    private ReferenceMapper mapper;
    private ReferenceService service;
    private ReferenceRepository repository;

    @GetMapping("/{type}")
    @Operation(
            summary = "Отримати всі значення довідника за типом",
            description = "Повертає мапу всіх значень довідника певного типу у форматі RefTo"
    )
    public Map<String, RefTo> getRefsByType(@PathVariable RefType type) {
        return ReferenceService.getRefs(type);
    }

    @GetMapping("/{type}/{code}")
    @Operation(
            summary = "Отримати значення довідника за типом і кодом",
            description = "Повертає об'єкт RefTo, який відповідає вказаному типу довідника і коду"
    )
    public RefTo getRefByTypeByCode(@PathVariable RefType type, @PathVariable String code) {
        return ReferenceService.getRefTo(type, code);
    }

    @DeleteMapping("/{type}/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Видалити значення довідника",
            description = "Видаляє елемент довідника за типом і кодом та оновлює кеш"
    )
    public void delete(@PathVariable RefType type, @PathVariable String code) {
        log.debug("delete with type {}, code {}", type, code);
        RefTo ref = ReferenceService.getRefTo(type, code);
        repository.deleteExisted(ref.id());
        service.updateRefs(type);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Створити нове значення довідника",
            description = "Створює нове значення довідника на основі переданих даних RefTo"
    )
    public ResponseEntity<RefTo> create(@Valid @RequestBody RefTo refTo) {
        log.debug("create {}", refTo);
        checkNew(refTo);
        Reference ref = repository.save(mapper.toEntity(refTo));
        refTo.setId(ref.id());
        RefType refType = refTo.getRefType();
        service.updateRefs(refType);
        return createdResponse(REST_URL + "/{type}/{code}", refTo, refType, refTo.getCode());
    }

    @PutMapping("/{type}/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    @Operation(
            summary = "Оновити назву значення довідника",
            description = "Оновлює поле title для обраного елемента довідника за типом і кодом"
    )
    public void updateTitle(@PathVariable RefType type, @PathVariable String code, @RequestParam String title) {
        log.debug("update Ref with type={}, code={} with title={}", title, code, title);
        Reference ref = getExisted(type, code);
        ref.setTitle(title);
        repository.save(ref);
        ReferenceService.getRefTo(type, code).setTitle(title);
    }

    @PatchMapping("/{type}/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    @Operation(
            summary = "Активувати/деактивувати значення довідника",
            description = "Змінює статус enabled (активний/неактивний) для елемента довідника"
    )
    public void enable(@PathVariable RefType type, @PathVariable String code, @RequestParam boolean enabled) {
        log.debug("enable Ref with type={}, code={} with enabled={}", type, code, enabled);
        Reference ref = getExisted(type, code);
        ref.setEnabled(enabled);
        repository.save(ref);
        ReferenceService.getRefTo(type, code).setEnabled(enabled);
    }

    private Reference getExisted(RefType type, String code) {
        return repository.getExistedByTypeAndCode(type, code);
    }
}
