package com.bienCriollas.stock.production.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.production.dto.*;
import com.bienCriollas.stock.production.analytics.entity.ProductionCostSettings;
import com.bienCriollas.stock.production.analytics.repository.ProductionCostSettingsRepository;
import com.bienCriollas.stock.production.entity.*;
import com.bienCriollas.stock.production.enums.ProductionStatus;
import com.bienCriollas.stock.production.exception.*;
import com.bienCriollas.stock.production.ingredient.entity.Ingredient;
import com.bienCriollas.stock.production.ingredient.exception.*;
import com.bienCriollas.stock.production.ingredient.repository.IngredientRepository;
import com.bienCriollas.stock.production.interfaces.IProductionService;
import com.bienCriollas.stock.production.mapper.ProductionMapper;
import com.bienCriollas.stock.production.process.entity.ProductionProcess;
import com.bienCriollas.stock.production.process.repository.ProductionProcessRepository;
import com.bienCriollas.stock.production.recipe.entity.*;
import com.bienCriollas.stock.production.recipe.enums.AdditionalCostType;
import com.bienCriollas.stock.production.recipe.repository.RecipeRepository;
import com.bienCriollas.stock.production.recipe.service.RecipeCostCalculator;
import com.bienCriollas.stock.production.repository.*;
import com.bienCriollas.stock.stock.dto.StockDTO;
import com.bienCriollas.stock.stock.interfaces.IStockService;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.exception.VarietyNotFoundException;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionService implements IProductionService {

    private static final BigDecimal SIXTY = new BigDecimal("60");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "productionDate", "varietyName", "plannedUnits", "finalUnits",
            "totalMinutes", "peopleCount", "wasteUnits", "status", "createdAt",
            "updatedAt", "finalizedAt");

    private final ProductionRepository productionRepository;
    private final ProductionIngredientRepository productionIngredientRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final ProductionProcessRepository processRepository;
    private final EmpanadaVarietyRepository varietyRepository;
    private final IStockService stockService;
    private final ProductionMapper productionMapper;
    private final Validator validator;
    private final ProductionCostSettingsRepository costSettingsRepository;
    private final RecipeCostCalculator recipeCostCalculator;

    @Override
    @Transactional
    public ProductionResponseDTO createProduction(ProductionCreateRequestDTO dto) {
        validateCreate(dto);
        EmpanadaVariety variety = varietyRepository.findById(dto.varietyId())
                .orElseThrow(() -> new VarietyNotFoundException(dto.varietyId()));
        Recipe recipe = recipeRepository.findByVarietyVarietyIdAndActiveTrue(dto.varietyId())
                .orElseThrow(() -> new ActiveRecipeRequiredException(variety.getName()));
        ProductionProcess process = processRepository
                .findByVarietyVarietyIdAndActiveTrue(dto.varietyId()).orElse(null);

        Production production = productionMapper.toEntity(dto);
        production.setVariety(variety);
        production.setRecipe(recipe);
        production.setProcess(process);
        production.setStatus(ProductionStatus.DRAFT);
        production.setWasteUnits(0);
        production.setNotes(normalizeOptional(production.getNotes()));
        production.setIngredients(new ArrayList<>());
        production.setAdditionalCosts(new ArrayList<>());
        production.setAdditionalCostsSnapshotted(true);

        BigDecimal planned = BigDecimal.valueOf(dto.plannedUnits());
        BigDecimal baseYield = BigDecimal.valueOf(recipe.getBaseYieldUnits());
        BigDecimal expectedIngredientCost = BigDecimal.ZERO;
        for (RecipeIngredient recipeIngredient : recipe.getIngredients()) {
            Ingredient ingredient = recipeIngredient.getIngredient();
            BigDecimal expected = recipeIngredient.getQuantity()
                    .multiply(planned)
                    .divide(baseYield, 4, RoundingMode.HALF_UP);
            production.addIngredient(ProductionIngredient.builder()
                    .ingredient(ingredient)
                    .expectedQuantity(expected)
                    .actualQuantity(null)
                    .costPerBaseUnitSnapshot(ingredient.getCostPerBaseUnit())
                    .measurementUnitSnapshot(ingredient.getMeasurementUnit())
                    .build());
            expectedIngredientCost = expectedIngredientCost.add(
                    expected.multiply(ingredient.getCostPerBaseUnit()));
        }
        Map<Long, BigDecimal> calculatedById = recipeCostCalculator.calculate(
                recipe, dto.plannedUnits(), expectedIngredientCost).rawAdditionalCostsById();
        for (RecipeAdditionalCost recipeCost : recipe.getAdditionalCosts()) {
            if (!Boolean.TRUE.equals(recipeCost.getActive())) continue;
            production.addAdditionalCost(ProductionAdditionalCost.builder()
                    .recipeAdditionalCost(recipeCost)
                    .costType(recipeCost.getCostType())
                    .nameSnapshot(recipeCost.getName())
                    .calculationModeSnapshot(recipeCost.getCalculationMode())
                    .valueSnapshot(recipeCost.getValue())
                    .calculatedExpectedCostSnapshot(calculatedById.get(recipeCost.getId()))
                    .sortOrder(recipeCost.getSortOrder())
                    .build());
        }
        return toResponse(productionRepository.saveAndFlush(production));
    }

    @Override
    public ProductionResponseDTO getProductionById(Long id) {
        return toResponse(findDetailed(id));
    }

    @Override
    public Page<ProductionResponseDTO> getProductions(Pageable pageable) {
        validatePageable(pageable);
        return productionRepository.findAll(toPersistencePageable(pageable)).map(this::toResponse);
    }

    @Override
    public Page<ProductionResponseDTO> getProductionsByStatus(
            ProductionStatus status, Pageable pageable) {
        if (status == null) throw new InvalidProductionException("El estado es obligatorio.");
        validatePageable(pageable);
        return productionRepository.findByStatus(status, toPersistencePageable(pageable))
                .map(this::toResponse);
    }

    @Override
    public Page<ProductionResponseDTO> getProductionsByDateRange(
            LocalDate from, LocalDate to, Pageable pageable) {
        if (from == null || to == null) {
            throw new InvalidProductionException("Las fechas desde y hasta son obligatorias.");
        }
        if (from.isAfter(to)) {
            throw new InvalidProductionException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        validatePageable(pageable);
        return productionRepository.findByProductionDateBetween(
                from, to, toPersistencePageable(pageable)).map(this::toResponse);
    }

    @Override
    @Transactional
    public ProductionResponseDTO updateProduction(Long id, ProductionUpdateDTO dto) {
        validateUpdate(dto);
        Production production = lockProduction(id);
        requireDraft(production, "Solo se puede editar una producción en estado DRAFT.");
        productionMapper.update(production, dto);
        production.setWasteReason(normalizeOptional(production.getWasteReason()));
        production.setNotes(normalizeOptional(production.getNotes()));
        return toResponse(productionRepository.saveAndFlush(production));
    }

    @Override
    @Transactional
    public ProductionResponseDTO updateIngredientConsumption(
            Long productionId, ProductionIngredientUpdateDTO dto) {
        validateIngredientRequest(dto);
        Production production = lockProduction(productionId);
        requireDraft(production, "Solo se puede editar una producción en estado DRAFT.");
        ProductionIngredient consumption = productionIngredientRepository
                .findByProductionIdOrderByIngredientId(productionId).stream()
                .filter(item -> item.getIngredient().getId().equals(dto.ingredientId()))
                .findFirst()
                .orElseThrow(() -> new IngredientNotFoundException(dto.ingredientId()));
        consumption.setActualQuantity(normalizeQuantity(dto.actualQuantity()));
        productionIngredientRepository.saveAndFlush(consumption);
        return toResponse(findDetailed(productionId));
    }

    @Override
    @Transactional
    public ProductionResponseDTO addExtraIngredient(
            Long productionId, ProductionIngredientUpdateDTO dto) {
        validateIngredientRequest(dto);
        Production production = lockProduction(productionId);
        requireDraft(production, "Solo se puede editar una producción en estado DRAFT.");

        boolean alreadyPresent = productionIngredientRepository
                .findByProductionIdOrderByIngredientId(productionId).stream()
                .anyMatch(item -> item.getIngredient().getId().equals(dto.ingredientId()));
        if (alreadyPresent) {
            throw new InvalidProductionStateException(
                    "El ingrediente ya forma parte de la producción.");
        }
        Ingredient ingredient = ingredientRepository.findByIdForUpdate(dto.ingredientId())
                .orElseThrow(() -> new IngredientNotFoundException(dto.ingredientId()));
        ingredient.requireActive();
        ProductionIngredient extra = ProductionIngredient.builder()
                .production(production)
                .ingredient(ingredient)
                .expectedQuantity(new BigDecimal("0.0000"))
                .actualQuantity(normalizeQuantity(dto.actualQuantity()))
                .costPerBaseUnitSnapshot(ingredient.getCostPerBaseUnit())
                .measurementUnitSnapshot(ingredient.getMeasurementUnit())
                .build();
        productionIngredientRepository.saveAndFlush(extra);
        return toResponse(findDetailed(productionId));
    }

    @Override
    @Transactional
    public ProductionResponseDTO finalizeProduction(Long id) {
        Production production = lockProduction(id);
        if (production.getStatus() == ProductionStatus.FINALIZED) {
            throw new ProductionAlreadyFinalizedException();
        }
        if (production.getStatus() == ProductionStatus.CANCELED) {
            throw new InvalidProductionStateException(
                    "Una producción cancelada no puede finalizarse.");
        }
        if (production.getFinalUnits() == null) {
            throw new InvalidProductionStateException(
                    "La cantidad final debe informarse antes de finalizar.");
        }
        if (production.getFinalUnits() < 0) {
            throw new InvalidProductionStateException(
                    "La cantidad final no puede ser menor a cero.");
        }

        ProductionCostSettings costSettings = costSettingsRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> new InvalidProductionStateException(
                        "Configurá los costos de producción antes de finalizar la tanda."));
        production.setLaborHourlyCostSnapshot(costSettings.getAverageHourlyLaborCost());
        BigDecimal recipeEnergyPercentage = production.getAdditionalCosts().stream()
                .filter(item -> item.getCostType() == AdditionalCostType.ENERGY)
                .map(ProductionAdditionalCost::getValueSnapshot)
                .findFirst().orElse(null);
        production.setEnergyPercentageSnapshot((recipeEnergyPercentage == null
                ? costSettings.getEnergyPercentage() : recipeEnergyPercentage)
                .setScale(2, RoundingMode.HALF_UP));

        List<ProductionIngredient> consumptions = productionIngredientRepository
                .findByProductionIdOrderByIngredientId(id);
        List<Long> ingredientIds = consumptions.stream()
                .map(item -> item.getIngredient().getId()).sorted().toList();
        List<Ingredient> lockedIngredients = ingredientRepository
                .findAllByIdForProduction(ingredientIds);
        if (lockedIngredients.size() != ingredientIds.size()) {
            Set<Long> found = lockedIngredients.stream().map(Ingredient::getId).collect(Collectors.toSet());
            Long missing = ingredientIds.stream().filter(item -> !found.contains(item)).findFirst().orElse(null);
            throw new IngredientNotFoundException(missing);
        }
        Map<Long, Ingredient> byId = lockedIngredients.stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));

        for (ProductionIngredient consumption : consumptions) {
            Ingredient ingredient = byId.get(consumption.getIngredient().getId());
            BigDecimal actual = effectiveActual(consumption);
            if (ingredient.getCurrentStock().compareTo(actual) < 0) {
                throw new InsufficientIngredientStockException(
                        ingredient.getName(), ingredient.getCurrentStock(), actual,
                        consumption.getMeasurementUnitSnapshot());
            }
        }
        for (ProductionIngredient consumption : consumptions) {
            Ingredient ingredient = byId.get(consumption.getIngredient().getId());
            BigDecimal actual = effectiveActual(consumption);
            consumption.setActualQuantity(actual);
            consumption.setIngredient(ingredient);
            ingredient.setCurrentStock(ingredient.getCurrentStock().subtract(actual));
        }
        ingredientRepository.saveAll(lockedIngredients);
        productionIngredientRepository.saveAll(consumptions);

        if (production.getFinalUnits() > 0) {
            stockService.updateStock(List.of(new StockDTO(
                    production.getVariety().getVarietyId(),
                    production.getProductionDate(),
                    production.getFinalUnits())));
        }
        production.setStatus(ProductionStatus.FINALIZED);
        production.setFinalizedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        productionRepository.saveAndFlush(production);
        return toResponse(findDetailed(id));
    }

    @Override
    @Transactional
    public ProductionResponseDTO cancelProduction(Long id) {
        Production production = lockProduction(id);
        if (production.getStatus() == ProductionStatus.FINALIZED) {
            throw new InvalidProductionStateException(
                    "Una producción finalizada no puede cancelarse.");
        }
        if (production.getStatus() == ProductionStatus.CANCELED) {
            throw new InvalidProductionStateException("La producción ya está cancelada.");
        }
        production.setStatus(ProductionStatus.CANCELED);
        return toResponse(productionRepository.saveAndFlush(production));
    }

    private ProductionResponseDTO toResponse(Production production) {
        List<ProductionIngredientResponseDTO> ingredients = new ArrayList<>();
        List<ProductionAdditionalCostResponseDTO> additionalCosts = production.getAdditionalCosts()
                .stream()
                .map(item -> new ProductionAdditionalCostResponseDTO(
                        item.getId(),
                        item.getRecipeAdditionalCost() == null
                                ? null : item.getRecipeAdditionalCost().getId(),
                        item.getCostType(), item.getNameSnapshot(),
                        item.getCalculationModeSnapshot(), item.getValueSnapshot(),
                        money(item.getCalculatedExpectedCostSnapshot()), item.getSortOrder()))
                .toList();
        BigDecimal expectedTotal = BigDecimal.ZERO;
        BigDecimal actualTotal = BigDecimal.ZERO;
        for (ProductionIngredient item : production.getIngredients()) {
            BigDecimal actual = effectiveActual(item);
            BigDecimal difference = actual.subtract(item.getExpectedQuantity()).setScale(4);
            BigDecimal differencePercentage = item.getExpectedQuantity().signum() > 0
                    ? difference.multiply(ONE_HUNDRED)
                            .divide(item.getExpectedQuantity(), 2, RoundingMode.HALF_UP)
                    : null;
            BigDecimal expectedCost = money(item.getExpectedQuantity()
                    .multiply(item.getCostPerBaseUnitSnapshot()));
            BigDecimal actualCost = money(actual.multiply(item.getCostPerBaseUnitSnapshot()));
            BigDecimal current = item.getIngredient().getCurrentStock();
            BigDecimal projected = current.subtract(actual).setScale(4);
            ingredients.add(new ProductionIngredientResponseDTO(
                    item.getIngredient().getId(), item.getIngredient().getName(),
                    item.getExpectedQuantity(), actual, difference,
                    differencePercentage, item.getMeasurementUnitSnapshot(),
                    item.getCostPerBaseUnitSnapshot(), expectedCost,
                    actualCost, current, projected, current.compareTo(actual) >= 0));
            expectedTotal = expectedTotal.add(expectedCost);
            actualTotal = actualTotal.add(actualCost);
        }
        expectedTotal = money(expectedTotal);
        actualTotal = money(actualTotal);

        BigDecimal costPerUnit = production.getFinalUnits() != null
                && production.getFinalUnits() > 0
                ? actualTotal.divide(BigDecimal.valueOf(production.getFinalUnits()), 2, RoundingMode.HALF_UP)
                : null;
        BigDecimal standardRate = standardUnitsPerHour(production.getProcess());
        BigDecimal actualRate = production.getFinalUnits() != null
                && production.getTotalMinutes() != null
                ? unitsPerHour(production.getFinalUnits(), production.getTotalMinutes()) : null;
        BigDecimal variation = standardRate != null && actualRate != null
                ? actualRate.divide(standardRate, 8, RoundingMode.HALF_UP)
                        .subtract(BigDecimal.ONE).multiply(ONE_HUNDRED)
                        .setScale(2, RoundingMode.HALF_UP)
                : null;
        ProductionProcess process = production.getProcess();
        return new ProductionResponseDTO(
                production.getId(), production.getProductionDate(),
                production.getVariety().getVarietyId(), production.getVariety().getName(),
                production.getRecipe().getId(), production.getRecipe().getVersion(),
                process == null ? null : process.getId(), process == null ? null : process.getVersion(),
                production.getPlannedUnits(), production.getFinalUnits(), production.getWasteUnits(),
                production.getWasteReason(), production.getTotalMinutes(), production.getPeopleCount(),
                production.getStatus(), production.getNotes(), List.copyOf(ingredients),
                additionalCosts,
                expectedTotal, actualTotal, costPerUnit, standardRate, actualRate, variation,
                production.getCreatedAt(), production.getFinalizedAt());
    }

    private BigDecimal standardUnitsPerHour(ProductionProcess process) {
        if (process == null) return null;
        int minutes = process.getSteps().stream().mapToInt(step -> step.getEstimatedMinutes()).sum();
        return unitsPerHour(process.getReferenceYieldUnits(), minutes);
    }

    private BigDecimal unitsPerHour(int units, int minutes) {
        return BigDecimal.valueOf(units).multiply(SIXTY)
                .divide(BigDecimal.valueOf(minutes), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal effectiveActual(ProductionIngredient item) {
        return item.getActualQuantity() == null
                ? item.getExpectedQuantity() : item.getActualQuantity();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        try {
            return value.setScale(4, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvalidProductionException(
                    "La cantidad real admite como máximo cuatro decimales.");
        }
    }

    private Production findDetailed(Long id) {
        validateId(id);
        return productionRepository.findDetailedById(id)
                .orElseThrow(() -> new ProductionNotFoundException(id));
    }

    private Production lockProduction(Long id) {
        validateId(id);
        return productionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ProductionNotFoundException(id));
    }

    private void requireDraft(Production production, String message) {
        if (production.getStatus() == ProductionStatus.FINALIZED) {
            throw new ProductionAlreadyFinalizedException();
        }
        if (production.getStatus() != ProductionStatus.DRAFT) {
            throw new InvalidProductionStateException(message);
        }
    }

    private void validateCreate(ProductionCreateRequestDTO dto) {
        if (dto == null) throw new InvalidProductionException("Los datos de la producción son obligatorios.");
        validateBean(dto);
        validateId(dto.varietyId());
    }

    private void validateUpdate(ProductionUpdateDTO dto) {
        if (dto == null) throw new InvalidProductionException("Los datos de la producción son obligatorios.");
        validateBean(dto);
    }

    private void validateIngredientRequest(ProductionIngredientUpdateDTO dto) {
        if (dto == null) throw new InvalidProductionException("Los datos del ingrediente son obligatorios.");
        validateBean(dto);
        validateId(dto.ingredientId());
        normalizeQuantity(dto.actualQuantity());
    }

    private void validateBean(Object value) {
        validator.validate(value).stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .findFirst().ifPresent(v -> {
                    throw new InvalidProductionException(v.getPropertyPath() + ": " + v.getMessage());
                });
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) throw new InvalidProductionException("El id debe ser mayor a cero.");
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null) throw new InvalidProductionException("La paginación es obligatoria.");
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidProductionException(
                        "Campo de ordenamiento inválido: " + order.getProperty());
            }
        });
    }

    private Pageable toPersistencePageable(Pageable pageable) {
        Sort sort = Sort.by(pageable.getSort().stream()
                .map(order -> "varietyName".equals(order.getProperty())
                        ? order.withProperty("variety.name") : order).toList());
        return pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)
                : Pageable.unpaged(sort);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
