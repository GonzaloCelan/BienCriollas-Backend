package com.bienCriollas.stock.production.recipe.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.production.ingredient.entity.Ingredient;
import com.bienCriollas.stock.production.ingredient.exception.IngredientNotFoundException;
import com.bienCriollas.stock.production.ingredient.repository.IngredientRepository;
import com.bienCriollas.stock.production.recipe.dto.*;
import com.bienCriollas.stock.production.recipe.entity.*;
import com.bienCriollas.stock.production.recipe.exception.*;
import com.bienCriollas.stock.production.recipe.interfaces.IRecipeService;
import com.bienCriollas.stock.production.recipe.repository.RecipeRepository;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.exception.VarietyNotFoundException;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeService implements IRecipeService {

    private static final int CALCULATION_SCALE = 6;
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "varietyName", "version", "baseYieldUnits",
            "active", "createdAt", "updatedAt");

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final EmpanadaVarietyRepository varietyRepository;
    private final Validator validator;

    @Override
    @Transactional
    public RecipeResponseDTO createRecipe(RecipeRequestDTO dto) {
        validateRecipeRequest(dto);
        EmpanadaVariety variety = lockVariety(dto.varietyId());
        if (recipeRepository.existsByVarietyVarietyIdAndActiveTrue(variety.getVarietyId())) {
            throw new RecipeAlreadyExistsException(variety.getName());
        }

        Recipe recipe = newRecipe(
                variety, 1, dto.baseYieldUnits(), normalizeNotes(dto.notes()), dto.ingredients());
        return toResponse(recipeRepository.saveAndFlush(recipe));
    }

    @Override
    public RecipeResponseDTO getRecipeById(Long id) {
        return toResponse(findDetailed(id));
    }

    @Override
    public RecipeResponseDTO getActiveRecipeByVariety(Long varietyId) {
        validateId(varietyId, "El id de la variedad debe ser mayor a cero.");
        if (!varietyRepository.existsById(varietyId)) {
            throw new VarietyNotFoundException(varietyId);
        }
        return toResponse(recipeRepository.findByVarietyVarietyIdAndActiveTrue(varietyId)
                .orElseThrow(() -> RecipeNotFoundException.forVariety(varietyId)));
    }

    @Override
    public Page<RecipeResponseDTO> getActiveRecipes(Pageable pageable) {
        validatePageable(pageable);
        return recipeRepository.findByActiveTrue(toPersistencePageable(pageable))
                .map(this::toResponse);
    }

    @Override
    public Page<RecipeResponseDTO> getRecipesByStatus(Boolean active, Pageable pageable) {
        validatePageable(pageable);
        if (active == null) {
            throw new InvalidRecipeException("El estado activo es obligatorio.");
        }
        Page<Recipe> recipes = active
                ? recipeRepository.findByActiveTrue(toPersistencePageable(pageable))
                : recipeRepository.findByActiveFalse(toPersistencePageable(pageable));
        return recipes.map(this::toResponse);
    }

    @Override
    public List<RecipeResponseDTO> getRecipeHistory(Long varietyId) {
        validateId(varietyId, "El id de la variedad debe ser mayor a cero.");
        if (!varietyRepository.existsById(varietyId)) {
            throw new VarietyNotFoundException(varietyId);
        }
        return recipeRepository.findByVarietyVarietyIdOrderByVersionDesc(varietyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RecipeResponseDTO createNewVersion(Long recipeId, RecipeVersionRequestDTO dto) {
        validateId(recipeId, "El id de la receta debe ser mayor a cero.");
        validateVersionRequest(dto);

        Recipe original = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));
        EmpanadaVariety variety = lockVariety(original.getVariety().getVarietyId());

        Recipe latest = recipeRepository
                .findTopByVarietyVarietyIdOrderByVersionDesc(variety.getVarietyId())
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));
        Recipe current = recipeRepository.findActiveByVarietyForUpdate(variety.getVarietyId())
                .orElseThrow(() -> RecipeNotFoundException.forVariety(variety.getVarietyId()));

        List<ResolvedIngredient> ingredients = resolveIngredients(dto.ingredients());
        current.setActive(false);
        recipeRepository.saveAndFlush(current);

        Recipe next = buildRecipe(
                variety,
                Math.addExact(latest.getVersion(), 1),
                dto.baseYieldUnits(),
                normalizeNotes(dto.notes()),
                ingredients);
        return toResponse(recipeRepository.saveAndFlush(next));
    }

    @Override
    public RecipeCalculationResponseDTO calculateRecipe(Long recipeId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidRecipeYieldException();
        }
        Recipe recipe = findDetailed(recipeId);
        BigDecimal scaleFactor = BigDecimal.valueOf(quantity)
                .divide(BigDecimal.valueOf(recipe.getBaseYieldUnits()),
                        CALCULATION_SCALE, RoundingMode.HALF_UP);

        List<RecipeCalculatedIngredientDTO> calculated = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (RecipeIngredient item : recipe.getIngredients()) {
            Ingredient ingredient = item.getIngredient();
            BigDecimal required = item.getQuantityGrams()
                    .multiply(scaleFactor)
                    .setScale(2, RoundingMode.HALF_UP);
            boolean enough = ingredient.getCurrentStockGrams().compareTo(required) >= 0;
            BigDecimal missing = enough
                    ? BigDecimal.ZERO.setScale(2)
                    : required.subtract(ingredient.getCurrentStockGrams());
            BigDecimal cost = costForGrams(required, ingredient);
            total = total.add(cost);
            calculated.add(new RecipeCalculatedIngredientDTO(
                    ingredient.getId(),
                    ingredient.getName(),
                    item.getQuantityGrams(),
                    required,
                    ingredient.getCurrentStockGrams(),
                    enough,
                    missing,
                    cost));
        }

        return new RecipeCalculationResponseDTO(
                recipe.getId(),
                recipe.getVariety().getVarietyId(),
                recipe.getVariety().getName(),
                recipe.getVersion(),
                recipe.getBaseYieldUnits(),
                quantity,
                scaleFactor.stripTrailingZeros(),
                List.copyOf(calculated),
                total,
                dividePerUnit(total, quantity));
    }

    private Recipe newRecipe(
            EmpanadaVariety variety,
            int version,
            int baseYieldUnits,
            String notes,
            List<RecipeIngredientRequestDTO> requests) {
        return buildRecipe(variety, version, baseYieldUnits, notes, resolveIngredients(requests));
    }

    private Recipe buildRecipe(
            EmpanadaVariety variety,
            int version,
            int baseYieldUnits,
            String notes,
            List<ResolvedIngredient> ingredients) {
        Recipe recipe = Recipe.builder()
                .variety(variety)
                .version(version)
                .baseYieldUnits(baseYieldUnits)
                .notes(notes)
                .active(true)
                .build();
        ingredients.forEach(item -> recipe.addIngredient(RecipeIngredient.builder()
                .ingredient(item.ingredient())
                .quantityGrams(item.quantityGrams())
                .build()));
        return recipe;
    }

    private List<ResolvedIngredient> resolveIngredients(
            List<RecipeIngredientRequestDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new RecipeWithoutIngredientsException();
        }

        LinkedHashSet<Long> ids = requests.stream()
                .map(RecipeIngredientRequestDTO::ingredientId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // La lectura queda bloqueada hasta el commit para que ningún ingrediente
        // pueda desactivarse entre la validación y la creación de la receta.
        Map<Long, Ingredient> byId = ingredientRepository.findAllByIdForRecipe(ids).stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));

        for (Long id : ids) {
            if (!byId.containsKey(id)) {
                throw new IngredientNotFoundException(id);
            }
        }

        Set<Long> seen = new HashSet<>();
        List<ResolvedIngredient> resolved = new ArrayList<>();
        for (RecipeIngredientRequestDTO request : requests) {
            Ingredient ingredient = byId.get(request.ingredientId());
            if (!seen.add(request.ingredientId())) {
                throw new RecipeIngredientDuplicatedException(ingredient.getName());
            }
            if (!Boolean.TRUE.equals(ingredient.getActive())) {
                throw new InactiveIngredientForRecipeException(ingredient.getName());
            }
            resolved.add(new ResolvedIngredient(ingredient, request.quantityGrams()));
        }
        return resolved;
    }

    private RecipeResponseDTO toResponse(Recipe recipe) {
        List<RecipeIngredientResponseDTO> ingredientResponses = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (RecipeIngredient item : recipe.getIngredients()) {
            Ingredient ingredient = item.getIngredient();
            BigDecimal costPerGram = ingredient.getCostPerKilogram().movePointLeft(3);
            BigDecimal estimatedCost = costForGrams(item.getQuantityGrams(), ingredient);
            total = total.add(estimatedCost);
            ingredientResponses.add(new RecipeIngredientResponseDTO(
                    ingredient.getId(),
                    ingredient.getName(),
                    item.getQuantityGrams(),
                    costPerGram,
                    estimatedCost));
        }
        return new RecipeResponseDTO(
                recipe.getId(),
                recipe.getVariety().getVarietyId(),
                recipe.getVariety().getName(),
                recipe.getVersion(),
                recipe.getBaseYieldUnits(),
                recipe.getNotes(),
                List.copyOf(ingredientResponses),
                total,
                dividePerUnit(total, recipe.getBaseYieldUnits()),
                recipe.getActive(),
                recipe.getCreatedAt(),
                recipe.getUpdatedAt());
    }

    private BigDecimal costForGrams(BigDecimal grams, Ingredient ingredient) {
        return grams.multiply(ingredient.getCostPerKilogram()).movePointLeft(3);
    }

    private BigDecimal dividePerUnit(BigDecimal total, int units) {
        return total.divide(BigDecimal.valueOf(units),
                CALCULATION_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private Recipe findDetailed(Long id) {
        validateId(id, "El id de la receta debe ser mayor a cero.");
        return recipeRepository.findDetailedById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    private EmpanadaVariety lockVariety(Long varietyId) {
        validateId(varietyId, "El id de la variedad debe ser mayor a cero.");
        return varietyRepository.findByIdForUpdate(varietyId)
                .orElseThrow(() -> new VarietyNotFoundException(varietyId));
    }

    private void validateRecipeRequest(RecipeRequestDTO dto) {
        if (dto == null) {
            throw new InvalidRecipeException("Los datos de la receta son obligatorios.");
        }
        validateYield(dto.baseYieldUnits());
        validateIngredientsPresent(dto.ingredients());
        validateBean(dto);
    }

    private void validateVersionRequest(RecipeVersionRequestDTO dto) {
        if (dto == null) {
            throw new InvalidRecipeException("Los datos de la receta son obligatorios.");
        }
        validateYield(dto.baseYieldUnits());
        validateIngredientsPresent(dto.ingredients());
        validateBean(dto);
    }

    private void validateYield(Integer yield) {
        if (yield == null || yield <= 0) {
            throw new InvalidRecipeYieldException();
        }
    }

    private void validateIngredientsPresent(List<?> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            throw new RecipeWithoutIngredientsException();
        }
    }

    private void validateBean(Object value) {
        validator.validate(value).stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .findFirst()
                .ifPresent(violation -> {
                    throw new InvalidRecipeException(
                            violation.getPropertyPath() + ": " + violation.getMessage());
                });
    }

    private String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        return notes.strip();
    }

    private void validateId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new InvalidRecipeException(message);
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new InvalidRecipeException("La paginación es obligatoria.");
        }
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidRecipeException(
                        "Campo de ordenamiento inválido: " + order.getProperty());
            }
        });
    }

    private Pageable toPersistencePageable(Pageable pageable) {
        Sort sort = Sort.by(pageable.getSort().stream()
                .map(order -> "varietyName".equals(order.getProperty())
                        ? order.withProperty("variety.name") : order)
                .toList());
        return pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)
                : Pageable.unpaged(sort);
    }

    private record ResolvedIngredient(Ingredient ingredient, BigDecimal quantityGrams) {}
}
