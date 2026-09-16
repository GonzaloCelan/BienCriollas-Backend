package com.bienCriollas.stock.production.process.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bienCriollas.stock.production.process.dto.*;

public interface IProductionProcessService {

    ProductionProcessResponseDTO createProcess(ProductionProcessRequestDTO dto);

    ProductionProcessResponseDTO getProcessById(Long id);

    ProductionProcessResponseDTO getActiveProcessByVariety(Long varietyId);

    Page<ProductionProcessResponseDTO> getActiveProcesses(Pageable pageable);

    Page<ProductionProcessResponseDTO> getProcessesByStatus(Boolean active, Pageable pageable);

    List<ProductionProcessResponseDTO> getProcessHistory(Long varietyId);

    ProductionProcessResponseDTO createNewVersion(
            Long processId, ProductionProcessVersionRequestDTO dto);
}
