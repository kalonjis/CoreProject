package be.steby.CoreProject.bll.domains.crm.pipeline.services;

import be.steby.CoreProject.bll.domains.crm.pipeline.exceptions.PipelineHasActiveDealsException;
import be.steby.CoreProject.bll.domains.crm.pipeline.exceptions.PipelineNameAlreadyExistsException;
import be.steby.CoreProject.bll.domains.crm.pipeline.exceptions.PipelineNotFoundException;
import be.steby.CoreProject.bll.domains.crm.pipeline.exceptions.PipelineStepNotFoundException;
import be.steby.CoreProject.bll.domains.crm.pipeline.exceptions.PipelineValidationException;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineCreateRequest;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineStatsResult;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineStatsResult.StageStats;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineStepCreateRequest;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineStepReorderRequest;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineStepUpdateRequest;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineUpdateRequest;
import be.steby.CoreProject.dal.repositories.crm.DealRepository;
import be.steby.CoreProject.dal.repositories.crm.DealStageHistoryRepository;
import be.steby.CoreProject.dal.repositories.crm.PipelineRepository;
import be.steby.CoreProject.dal.repositories.crm.PipelineStepRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Pipeline;
import be.steby.CoreProject.dl.entities.crm.PipelineStep;
import be.steby.CoreProject.dl.enums.crm.DealStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of {@link PipelineService}.
 *
 * <h3>Default pipeline</h3>
 * <p>Setting a pipeline as default is always done atomically:
 * the previous default is cleared before the new one is set.
 * Both writes happen within the same transaction.</p>
 *
 * <h3>Step terminal flags</h3>
 * <p>Each pipeline may have at most one Won step and one Lost step.
 * {@code isWon} and {@code isLost} are mutually exclusive on the same step.
 * These constraints are enforced before any write.</p>
 *
 * <h3>Deletion guards</h3>
 * <p>A pipeline or step cannot be deleted if active (open) deals are
 * attached to it. The deal count is checked via {@link DealRepository}
 * before any deletion.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PipelineServiceImpl implements PipelineService {

    private final PipelineRepository pipelineRepository;
    private final PipelineStepRepository pipelineStepRepository;
    private final DealRepository dealRepository;
    private final DealStageHistoryRepository dealStageHistoryRepository;

    // =========================================================================
    // Lookup — Pipeline
    // =========================================================================

    @Override
    public Pipeline getByPublicId(String publicId) {
        return pipelineRepository.findByPublicId(publicId)
                .orElseThrow(() -> PipelineNotFoundException.byPublicId(publicId));
    }

    @Override
    public Pipeline getDefault() {
        return pipelineRepository.findByIsDefaultTrue()
                .orElseThrow(PipelineNotFoundException::noDefault);
    }

    @Override
    public List<Pipeline> findAll() {
        return pipelineRepository.findAllByOrderByDisplayOrderAsc();
    }

    // =========================================================================
    // Lookup — PipelineStep
    // =========================================================================

    @Override
    public PipelineStep getStepByPublicId(String publicId) {
        return pipelineStepRepository.findByPublicId(publicId)
                .orElseThrow(() -> PipelineStepNotFoundException.byPublicId(publicId));
    }

    @Override
    public List<PipelineStep> getSteps(String pipelinePublicId) {
        Pipeline pipeline = getByPublicId(pipelinePublicId);
        return pipelineStepRepository.findByPipelineIdOrderByPositionAsc(pipeline.getId());
    }

    // =========================================================================
    // Stats
    // =========================================================================

    @Override
    public PipelineStatsResult getStats(String pipelinePublicId) {
        Pipeline pipeline = getByPublicId(pipelinePublicId);
        List<PipelineStep> steps = pipelineStepRepository.findByPipelineIdOrderByPositionAsc(pipeline.getId());

        // Build a map of open deal counts per step from a single query
        Map<Long, Long> openDealsByStep = new HashMap<>();
        dealRepository.countOpenDealsByStage(pipeline.getId())
                .forEach(row -> openDealsByStep.put((Long) row[0], (Long) row[1]));

        // Collect dealsEntered per step (needed for conversion rate calculation)
        List<Long> enteredByStep = new ArrayList<>(steps.size());
        for (PipelineStep step : steps) {
            enteredByStep.add(dealStageHistoryRepository.countDealsEnteredStep(step.getId()));
        }

        // Build per-stage stats
        List<StageStats> stageStats = new ArrayList<>(steps.size());
        for (int i = 0; i < steps.size(); i++) {
            PipelineStep step = steps.get(i);
            long dealsCurrently = openDealsByStep.getOrDefault(step.getId(), 0L);
            long dealsEntered   = enteredByStep.get(i);
            Double avgDays      = dealStageHistoryRepository.avgDaysInStep(step.getId());

            Double conversionRate = null;
            if (i < steps.size() - 1 && dealsEntered > 0) {
                long nextEntered = enteredByStep.get(i + 1);
                conversionRate = (nextEntered * 100.0) / dealsEntered;
            }

            stageStats.add(new StageStats(
                    step.getPublicId(),
                    step.getName(),
                    step.getPosition(),
                    step.isWon(),
                    step.isLost(),
                    dealsCurrently,
                    dealsEntered,
                    conversionRate,
                    avgDays
            ));
        }

        // Overall KPIs
        long wonCount  = dealRepository.countByPipelineIdAndStatus(pipeline.getId(), DealStatus.WON);
        long lostCount = dealRepository.countByPipelineIdAndStatus(pipeline.getId(), DealStatus.LOST);
        long closedTotal = wonCount + lostCount;
        Double winRate = closedTotal > 0 ? (wonCount * 100.0) / closedTotal : null;
        Double avgDealCycleDays = dealRepository.avgDealCycleDaysForPipeline(pipeline.getId());

        return new PipelineStatsResult(
                pipeline.getPublicId(),
                pipeline.getName(),
                stageStats,
                winRate,
                avgDealCycleDays
        );
    }

    // =========================================================================
    // Pipeline — Create / Update / Delete
    // =========================================================================

    @Override
    @Transactional
    public Pipeline create(PipelineCreateRequest request, User actor) {
        log.debug("Creating pipeline — name: '{}', by: {}", request.name(), actor.getUsername());

        if (pipelineRepository.findByPublicId(request.name()).isPresent()
                || pipelineRepository.findAllByOrderByDisplayOrderAsc().stream()
                        .anyMatch(p -> p.getName().equalsIgnoreCase(request.name()))) {
            throw PipelineNameAlreadyExistsException.forName(request.name());
        }

        if (request.isDefault()) {
            clearCurrentDefault();
        }

        Pipeline pipeline = Pipeline.builder()
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .isDefault(request.isDefault())
                .displayOrder(request.displayOrder())
                .build();

        pipeline.setPublicId(UUID.randomUUID().toString());

        Pipeline saved = pipelineRepository.save(pipeline);
        log.info("Pipeline created — publicId: {}, by: {}", saved.getPublicId(), actor.getUsername());

        return saved;
    }

    @Override
    @Transactional
    public Pipeline update(String publicId, PipelineUpdateRequest request, User actor) {
        log.debug("Updating pipeline — publicId: {}, by: {}", publicId, actor.getUsername());

        Pipeline pipeline = getByPublicId(publicId);

        if (request.name() != null && !request.name().equalsIgnoreCase(pipeline.getName())) {
            if (pipelineRepository.findAllByOrderByDisplayOrderAsc().stream()
                    .anyMatch(p -> !p.getPublicId().equals(publicId)
                            && p.getName().equalsIgnoreCase(request.name()))) {
                throw PipelineNameAlreadyExistsException.forName(request.name());
            }
            pipeline.setName(request.name().trim());
        }

        if (request.description() != null) {
            pipeline.setDescription(request.description().isBlank() ? null : request.description().trim());
        }

        if (request.displayOrder() != null) {
            pipeline.setDisplayOrder(request.displayOrder());
        }

        if (request.isDefault() != null) {
            if (Boolean.TRUE.equals(request.isDefault()) && !pipeline.isDefault()) {
                clearCurrentDefault();
                pipeline.setDefault(true);
            } else if (Boolean.FALSE.equals(request.isDefault()) && pipeline.isDefault()) {
                pipeline.setDefault(false);
                log.warn("Pipeline '{}' is no longer the default — no default pipeline is set", publicId);
            }
        }

        Pipeline saved = pipelineRepository.save(pipeline);
        log.info("Pipeline updated — publicId: {}, by: {}", publicId, actor.getUsername());

        return saved;
    }

    @Override
    @Transactional
    public void delete(String publicId, User actor) {
        log.debug("Deleting pipeline — publicId: {}, by: {}", publicId, actor.getUsername());

        Pipeline pipeline = getByPublicId(publicId);

        long activeDeals = dealRepository.findByPipelineIdAndStatus(pipeline.getId(), DealStatus.OPEN).size();
        if (activeDeals > 0) {
            throw PipelineHasActiveDealsException.onPipelineDelete(publicId, activeDeals);
        }

        pipelineRepository.delete(pipeline);
        log.info("Pipeline deleted — publicId: {}, by: {}", publicId, actor.getUsername());
    }

    // =========================================================================
    // PipelineStep — Create / Update / Delete / Reorder
    // =========================================================================

    @Override
    @Transactional
    public PipelineStep addStep(String pipelinePublicId, PipelineStepCreateRequest request, User actor) {
        log.debug("Adding step '{}' to pipeline '{}', by: {}",
                request.name(), pipelinePublicId, actor.getUsername());

        Pipeline pipeline = getByPublicId(pipelinePublicId);

        guardTerminalFlags(request.name(), request.isWon(), request.isLost());
        guardDuplicateTerminal(pipeline, request.isWon(), request.isLost());

        PipelineStep step = PipelineStep.builder()
                .name(request.name().trim())
                .color(request.color())
                .position(request.position())
                .isWon(request.isWon())
                .isLost(request.isLost())
                .winProbability(request.winProbability())
                .pipeline(pipeline)
                .build();

        step.setPublicId(UUID.randomUUID().toString());

        PipelineStep saved = pipelineStepRepository.save(step);
        log.info("Step created — publicId: {}, pipeline: {}, by: {}",
                saved.getPublicId(), pipelinePublicId, actor.getUsername());

        return saved;
    }

    @Override
    @Transactional
    public PipelineStep updateStep(String stepPublicId, PipelineStepUpdateRequest request, User actor) {
        log.debug("Updating step — publicId: {}, by: {}", stepPublicId, actor.getUsername());

        PipelineStep step = getStepByPublicId(stepPublicId);

        if (request.name() != null)            step.setName(request.name().trim());
        if (request.color() != null)           step.setColor(request.color().isBlank() ? null : request.color().trim());
        if (request.winProbability() != null)  step.setWinProbability(request.winProbability());

        PipelineStep saved = pipelineStepRepository.save(step);
        log.info("Step updated — publicId: {}, by: {}", stepPublicId, actor.getUsername());

        return saved;
    }

    @Override
    @Transactional
    public List<PipelineStep> reorderSteps(String pipelinePublicId,
                                           PipelineStepReorderRequest request,
                                           User actor) {
        log.debug("Reordering steps of pipeline '{}', by: {}", pipelinePublicId, actor.getUsername());

        Pipeline pipeline = getByPublicId(pipelinePublicId);
        List<PipelineStep> existingSteps =
                pipelineStepRepository.findByPipelineIdOrderByPositionAsc(pipeline.getId());

        guardReorderRequest(pipelinePublicId, existingSteps, request);

        // Build a lookup map for fast access
        java.util.Map<String, PipelineStep> stepByPublicId = new java.util.HashMap<>();
        for (PipelineStep s : existingSteps) {
            stepByPublicId.put(s.getPublicId(), s);
        }

        for (PipelineStepReorderRequest.StepPosition entry : request.orderedSteps()) {
            stepByPublicId.get(entry.stepPublicId()).setPosition(entry.newPosition());
        }

        List<PipelineStep> saved = pipelineStepRepository.saveAll(existingSteps);
        log.info("Steps reordered for pipeline '{}', by: {}", pipelinePublicId, actor.getUsername());

        return saved;
    }

    @Override
    @Transactional
    public void deleteStep(String stepPublicId, User actor) {
        log.debug("Deleting step — publicId: {}, by: {}", stepPublicId, actor.getUsername());

        PipelineStep step = getStepByPublicId(stepPublicId);

        long activeDeals = dealRepository.findByPipelineStepIdOrderByCreatedAtDesc(step.getId())
                .stream()
                .filter(d -> d.getStatus() == DealStatus.OPEN)
                .count();

        if (activeDeals > 0) {
            throw PipelineHasActiveDealsException.onStepDelete(stepPublicId, activeDeals);
        }

        pipelineStepRepository.delete(step);
        log.info("Step deleted — publicId: {}, by: {}", stepPublicId, actor.getUsername());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Clears the {@code isDefault} flag on the currently default pipeline, if any.
     */
    private void clearCurrentDefault() {
        pipelineRepository.findByIsDefaultTrue().ifPresent(current -> {
            current.setDefault(false);
            pipelineRepository.save(current);
            log.debug("Cleared default flag from pipeline '{}'", current.getPublicId());
        });
    }

    /**
     * Guards against a step having both {@code isWon} and {@code isLost} set to {@code true}.
     */
    private void guardTerminalFlags(String stepName, boolean isWon, boolean isLost) {
        if (isWon && isLost) {
            throw PipelineValidationException.mutuallyExclusiveTerminalFlags(stepName);
        }
    }

    /**
     * Guards against adding a second Won or Lost step to a pipeline that already has one.
     */
    private void guardDuplicateTerminal(Pipeline pipeline, boolean isWon, boolean isLost) {
        if (isWon && pipelineStepRepository.findByPipelineIdAndIsWonTrue(pipeline.getId()).isPresent()) {
            throw PipelineValidationException.duplicateWonStep(pipeline.getPublicId());
        }
        if (isLost && pipelineStepRepository.findByPipelineIdAndIsLostTrue(pipeline.getId()).isPresent()) {
            throw PipelineValidationException.duplicateLostStep(pipeline.getPublicId());
        }
    }

    /**
     * Validates the reorder request against the pipeline's actual steps.
     *
     * <p>Checks that:</p>
     * <ul>
     *   <li>All step public IDs in the request belong to the pipeline</li>
     *   <li>No step of the pipeline is missing from the request</li>
     *   <li>No duplicate positions are present</li>
     * </ul>
     */
    private void guardReorderRequest(String pipelinePublicId,
                                     List<PipelineStep> existingSteps,
                                     PipelineStepReorderRequest request) {
        Set<String> existingIds = new HashSet<>();
        for (PipelineStep s : existingSteps) existingIds.add(s.getPublicId());

        Set<String> requestIds  = new HashSet<>();
        Set<Integer> positions  = new HashSet<>();

        for (PipelineStepReorderRequest.StepPosition entry : request.orderedSteps()) {
            if (!existingIds.contains(entry.stepPublicId())) {
                throw PipelineStepNotFoundException.notInPipeline(entry.stepPublicId(), pipelinePublicId);
            }
            requestIds.add(entry.stepPublicId());
            if (!positions.add(entry.newPosition())) {
                throw PipelineValidationException.reorderDuplicatePositions(pipelinePublicId);
            }
        }

        if (!requestIds.equals(existingIds)) {
            throw PipelineValidationException.reorderIncomplete(pipelinePublicId);
        }
    }
}