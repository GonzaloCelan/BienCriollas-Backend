package com.bienCriollas.stock.production.ingredient.service;

import java.util.List;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import com.bienCriollas.stock.production.ingredient.dto.*;
import com.bienCriollas.stock.production.ingredient.entity.Ingredient;
import com.bienCriollas.stock.production.ingredient.exception.*;
import com.bienCriollas.stock.production.ingredient.interfaces.IIngredientService;
import com.bienCriollas.stock.production.ingredient.mapper.IngredientMapper;
import com.bienCriollas.stock.production.ingredient.repository.IngredientRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientService implements IIngredientService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "name", "currentStockGrams", "minimumStockGrams",
            "costPerGram", "costPerKilogram", "active", "createdAt", "updatedAt");

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;
    private final Validator validator;

    @Override
    @Transactional
    public IngredientResponseDTO createIngredient(IngredientRequestDTO dto) {
        validate(dto);
        String name = dto.name().strip();
        if (ingredientRepository.existsByNameIgnoreCase(name)) {
            throw new IngredientAlreadyExistsException(name);
        }
        Ingredient ingredient = ingredientMapper.toEntity(dto);
        ingredient.setName(name);
        ingredient.setActive(true);
        return save(ingredient);
    }

    @Override
    public IngredientResponseDTO getIngredientById(Long id) {
        validateId(id);
        return ingredientMapper.toResponseDTO(ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id)));
    }

    @Override
    public Page<IngredientResponseDTO> getIngredients(Pageable pageable) {
        validatePageable(pageable);
        return ingredientRepository.findByActiveTrue(toPersistencePageable(pageable)).map(ingredientMapper::toResponseDTO);
    }

    @Override
    public Page<IngredientResponseDTO> getIngredientsByStatus(Boolean active, Pageable pageable) {
        validatePageable(pageable);
        if (active == null) {
            throw new InvalidIngredientException("El estado activo es obligatorio.");
        }
        Page<Ingredient> ingredients = active
                ? ingredientRepository.findByActiveTrue(toPersistencePageable(pageable))
                : ingredientRepository.findByActiveFalse(toPersistencePageable(pageable));
        return ingredients.map(ingredientMapper::toResponseDTO);
    }

    @Override
    public Page<IngredientResponseDTO> searchIngredients(String query, Pageable pageable) {
        validatePageable(pageable);
        if (query == null || query.isBlank() || query.length() > 100) {
            throw new InvalidIngredientException("La búsqueda debe contener entre 1 y 100 caracteres.");
        }
        return ingredientRepository.search(query.strip(), toPersistencePageable(pageable)).map(ingredientMapper::toResponseDTO);
    }

    @Override
    @Transactional
    public IngredientResponseDTO updateIngredient(Long id, IngredientRequestDTO dto) {
        validate(dto);
        Ingredient ingredient = findForUpdate(id);
        String name = dto.name().strip();
        ingredientRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new IngredientAlreadyExistsException(name); });
        ingredientMapper.updateEntityFromDTO(dto, ingredient);
        ingredient.setName(name);
        return save(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO setStock(Long id, IngredientStockUpdateDTO dto) {
        validate(dto);
        Ingredient ingredient = findForUpdate(id);
        ingredient.setCurrentStockGrams(dto.stockGrams());
        return save(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO increaseStock(Long id, IngredientStockMovementDTO dto) {
        validate(dto);
        Ingredient ingredient = findForUpdate(id);
        ingredient.setCurrentStockGrams(ingredient.getCurrentStockGrams().add(dto.quantityGrams()));
        return save(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO decreaseStock(Long id, IngredientStockMovementDTO dto) {
        validate(dto);
        Ingredient ingredient = findForUpdate(id);
        ingredient.requireActive();
        if (ingredient.getCurrentStockGrams().compareTo(dto.quantityGrams()) < 0) {
            throw new InsufficientIngredientStockException(
                    ingredient.getName(), ingredient.getCurrentStockGrams(), dto.quantityGrams());
        }
        ingredient.setCurrentStockGrams(ingredient.getCurrentStockGrams().subtract(dto.quantityGrams()));
        return save(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO updateCost(Long id, IngredientCostUpdateDTO dto) {
        validate(dto);
        Ingredient ingredient = findForUpdate(id);
        ingredient.setCostPerKilogram(dto.costPerKilogram());
        return save(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO updateMinimumStock(Long id, IngredientMinimumStockDTO dto) {
        validate(dto);
        Ingredient ingredient = findForUpdate(id);
        ingredient.setMinimumStockGrams(dto.minimumStockGrams());
        return save(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO activateIngredient(Long id) {
        Ingredient ingredient = findForUpdate(id);
        ingredient.setActive(true);
        return save(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO deactivateIngredient(Long id) {
        Ingredient ingredient = findForUpdate(id);
        ingredient.setActive(false);
        return save(ingredient);
    }

    @Override
    public List<IngredientResponseDTO> getLowStockIngredients() {
        return ingredientRepository.findLowStockIngredients().stream()
                .map(ingredientMapper::toResponseDTO).toList();
    }

    @Override
    public IngredientSummaryDTO getSummary() {
        return ingredientRepository.getSummary();
    }

    private Ingredient findForUpdate(Long id) {
        validateId(id);
        // Todas las escrituras comparten el bloqueo, incluso cambios de estado y PUT.
        return ingredientRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
    }

    private IngredientResponseDTO save(Ingredient ingredient) {
        validate(ingredient);
        try {
            // Ejecuta callbacks y restricciones antes de construir la respuesta.
            return ingredientMapper.toResponseDTO(ingredientRepository.saveAndFlush(ingredient));
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                        && violation.getConstraintName() != null
                        && violation.getConstraintName().toLowerCase(Locale.ROOT).contains("uk_ingredients_name")) {
                    throw new IngredientAlreadyExistsException(ingredient.getName());
                }
            }
            throw exception;
        }
    }

    private void validate(Object dto) {
        if (dto == null) {
            throw new InvalidIngredientException("Los datos del ingrediente son obligatorios.");
        }
        validator.validate(dto).stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .findFirst()
                .ifPresent(violation -> {
                    throw new InvalidIngredientException(
                            violation.getPropertyPath() + ": " + violation.getMessage());
                });
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidIngredientException("El id del ingrediente debe ser mayor a cero.");
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new InvalidIngredientException("La paginación es obligatoria.");
        }
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidIngredientException("Campo de ordenamiento inválido: " + order.getProperty());
            }
        });
    }

    private Pageable toPersistencePageable(Pageable pageable) {
        Sort sort = Sort.by(pageable.getSort().stream()
                .map(order -> "costPerGram".equals(order.getProperty())
                        ? order.withProperty("costPerKilogram") : order)
                .toList());
        return pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)
                : Pageable.unpaged(sort);
    }
}
