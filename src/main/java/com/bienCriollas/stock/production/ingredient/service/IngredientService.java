package com.bienCriollas.stock.production.ingredient.service;

import java.math.BigDecimal;
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
import com.bienCriollas.stock.production.recipe.repository.RecipeIngredientRepository;
import com.bienCriollas.stock.production.repository.ProductionIngredientRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientService implements IIngredientService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "name", "measurementUnit", "purchasePresentation", "purchaseQuantity",
            "purchasePrice", "currentStock", "minimumStock",
            "costPerBaseUnit", "active", "createdAt", "updatedAt");

    private final IngredientRepository ingredientRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final ProductionIngredientRepository productionIngredientRepository;
    private final IngredientMapper ingredientMapper;
    private final IngredientPurchaseCostCalculator purchaseCostCalculator;
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
        applyPurchaseData(ingredient, dto.purchasePresentation(), dto.purchaseQuantity(),
                dto.purchasePrice(), true);
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
        validateMeasurementUnitChange(ingredient, dto);
        ingredientMapper.updateEntityFromDTO(dto, ingredient);
        ingredient.setName(name);
        applyPurchaseData(ingredient, dto.purchasePresentation(), dto.purchaseQuantity(),
                dto.purchasePrice(), false);
        return save(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO setStock(Long id, IngredientStockUpdateDTO dto) {
        validate(dto);
        Ingredient ingredient = findForUpdate(id);
        ingredient.setCurrentStock(dto.currentStock());
        return save(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO increaseStock(Long id, IngredientStockMovementDTO dto) {
        validate(dto);
        Ingredient ingredient = findForUpdate(id);
        ingredient.setCurrentStock(ingredient.getCurrentStock().add(dto.quantity()));
        return save(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO decreaseStock(Long id, IngredientStockMovementDTO dto) {
        validate(dto);
        Ingredient ingredient = findForUpdate(id);
        ingredient.requireActive();
        if (ingredient.getCurrentStock().compareTo(dto.quantity()) < 0) {
            throw new InsufficientIngredientStockException(
                    ingredient.getName(), ingredient.getCurrentStock(), dto.quantity(),
                    ingredient.getMeasurementUnit());
        }
        ingredient.setCurrentStock(ingredient.getCurrentStock().subtract(dto.quantity()));
        return save(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO updateCost(Long id, IngredientCostUpdateDTO dto) {
        validate(dto);
        Ingredient ingredient = findForUpdate(id);
        applyPurchaseData(ingredient, dto.purchasePresentation(), dto.purchaseQuantity(),
                dto.purchasePrice(), true);
        return save(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO updateMinimumStock(Long id, IngredientMinimumStockDTO dto) {
        validate(dto);
        Ingredient ingredient = findForUpdate(id);
        ingredient.setMinimumStock(dto.minimumStock());
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

    private void validateMeasurementUnitChange(
            Ingredient ingredient, IngredientRequestDTO dto) {
        if (ingredient.getMeasurementUnit() == dto.measurementUnit()) {
            return;
        }
        boolean alreadyUsed = ingredient.getCurrentStock().signum() != 0
                || recipeIngredientRepository.existsByIngredientId(ingredient.getId())
                || productionIngredientRepository.existsByIngredientId(ingredient.getId());
        if (alreadyUsed) {
            throw new InvalidIngredientException(
                    "La unidad de medida no puede cambiarse porque el ingrediente ya tiene "
                            + "stock, recetas o producciones asociadas.");
        }
    }

    private void applyPurchaseData(Ingredient ingredient, String presentation,
            BigDecimal quantity, BigDecimal price, boolean required) {
        boolean anyValueProvided = presentation != null || quantity != null || price != null;
        if (!anyValueProvided && !required) {
            return;
        }
        if (presentation == null || presentation.isBlank()) {
            throw new InvalidIngredientException(
                    "La presentación de compra es obligatoria.");
        }
        if (quantity == null) {
            throw new InvalidIngredientException(
                    "La cantidad de la presentación de compra es obligatoria.");
        }
        if (price == null) {
            throw new InvalidIngredientException(
                    "El precio de la presentación de compra es obligatorio.");
        }
        ingredient.setPurchasePresentation(presentation.strip());
        ingredient.setPurchaseQuantity(quantity);
        ingredient.setPurchasePrice(price);
        ingredient.setCostPerBaseUnit(purchaseCostCalculator.calculate(price, quantity));
    }

    private Pageable toPersistencePageable(Pageable pageable) {
        Sort sort = Sort.by(pageable.getSort().stream().toList());
        return pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)
                : Pageable.unpaged(sort);
    }
}
