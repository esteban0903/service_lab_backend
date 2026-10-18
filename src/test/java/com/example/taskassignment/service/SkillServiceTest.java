package com.example.taskassignment.service;

import com.example.taskassignment.domain.Task;
import com.example.taskassignment.domain.TaskStatus;
import com.example.taskassignment.domain.Worker;
import com.example.taskassignment.dto.SkillCatalogItemDto;
import com.example.taskassignment.dto.SkillCoverageItemDto;
import com.example.taskassignment.dto.TaskCoverageRequestDto;
import com.example.taskassignment.dto.TaskCoverageResponseDto;
import com.example.taskassignment.repository.TaskRepository;
import com.example.taskassignment.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillService Unit Tests")
class SkillServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private SkillService skillService;

    private Worker alice;
    private Worker bob;
    private Worker charlie;

    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        alice = new Worker("Alice", 8, Set.of("Java", "Spring Boot"));
        alice.setId(1L);

        bob = new Worker("Bob", 8, Set.of("java", "react"));
        bob.setId(2L);

        charlie = new Worker("Charlie", 8, Set.of("liderazgo", "Plomería"));
        charlie.setId(3L);

        task1 = new Task("Backend API", 8, TaskStatus.NOT_DONE, Set.of("JAVA", "spring boot"));
        task1.setId(10L);

        task2 = new Task("Plumbing repair", 4, TaskStatus.NOT_DONE, Set.of("plomeria"));
        task2.setId(20L);
    }

    @Test
    @DisplayName("Catalog collapses accents and casing into distinct normalized entries and counts distinct workers and tasks")
    void getSkillCatalog_normalizesAndCountsAccurately() {
        when(workerRepository.findAllWithSkills()).thenReturn(List.of(alice, bob, charlie));
        when(taskRepository.findAllWithSkills()).thenReturn(List.of(task1, task2));

        List<SkillCatalogItemDto> catalog = skillService.getSkillCatalog();

        // Distinct normalized skills: java, liderazgo, plomeria, react, spring boot (sorted by name)
        assertThat(catalog).hasSize(5);

        // java: Alice (1) and Bob (2) -> workerCount=2; Task 1 (10) -> taskCount=1
        SkillCatalogItemDto javaItem = catalog.stream().filter(c -> c.name().equals("java")).findFirst().orElseThrow();
        assertThat(javaItem.workerCount()).isEqualTo(2);
        assertThat(javaItem.taskCount()).isEqualTo(1);

        // plomeria: Charlie (3) had 'Plomería' (accent stripped); Task 2 (20) had 'plomeria'
        SkillCatalogItemDto plomeriaItem = catalog.stream().filter(c -> c.name().equals("plomeria")).findFirst().orElseThrow();
        assertThat(plomeriaItem.workerCount()).isEqualTo(1);
        assertThat(plomeriaItem.taskCount()).isEqualTo(1);

        // spring boot: Alice (1) -> workerCount=1; Task 1 (10) -> taskCount=1
        SkillCatalogItemDto springItem = catalog.stream().filter(c -> c.name().equals("spring boot")).findFirst().orElseThrow();
        assertThat(springItem.workerCount()).isEqualTo(1);
        assertThat(springItem.taskCount()).isEqualTo(1);

        // react: Bob (2) -> workerCount=1; taskCount=0
        SkillCatalogItemDto reactItem = catalog.stream().filter(c -> c.name().equals("react")).findFirst().orElseThrow();
        assertThat(reactItem.workerCount()).isEqualTo(1);
        assertThat(reactItem.taskCount()).isEqualTo(0);

        // liderazgo: Charlie (3) -> workerCount=1; taskCount=0
        SkillCatalogItemDto liderazgoItem = catalog.stream().filter(c -> c.name().equals("liderazgo")).findFirst().orElseThrow();
        assertThat(liderazgoItem.workerCount()).isEqualTo(1);
        assertThat(liderazgoItem.taskCount()).isEqualTo(0);

        // Order is alphabetical
        assertThat(catalog.stream().map(SkillCatalogItemDto::name))
                .containsExactly("java", "liderazgo", "plomeria", "react", "spring boot");
    }

    @Test
    @DisplayName("Empty catalog returns empty list without errors")
    void getSkillCatalog_emptyDb_returnsEmptyList() {
        when(workerRepository.findAllWithSkills()).thenReturn(List.of());
        when(taskRepository.findAllWithSkills()).thenReturn(List.of());

        List<SkillCatalogItemDto> catalog = skillService.getSkillCatalog();
        assertThat(catalog).isEmpty();
    }

    @Test
    @DisplayName("Coverage with empty skill list: coveredBy = total workers, warning = null")
    void evaluateCoverage_emptySkills_returnsAllWorkersAndNullWarning() {
        when(workerRepository.findAllWithSkills()).thenReturn(List.of(alice, bob, charlie));

        TaskCoverageResponseDto response = skillService.evaluateCoverage(new TaskCoverageRequestDto(List.of()));

        assertThat(response.coveredBy()).isEqualTo(3);
        assertThat(response.perSkill()).isEmpty();
        assertThat(response.missingSkills()).isEmpty();
        assertThat(response.warning()).isNull();
    }

    @Test
    @DisplayName("Coverage with null request: coveredBy = total workers, warning = null")
    void evaluateCoverage_nullRequest_returnsAllWorkersAndNullWarning() {
        when(workerRepository.findAllWithSkills()).thenReturn(List.of(alice, bob, charlie));

        TaskCoverageResponseDto response = skillService.evaluateCoverage(null);

        assertThat(response.coveredBy()).isEqualTo(3);
        assertThat(response.perSkill()).isEmpty();
        assertThat(response.missingSkills()).isEmpty();
        assertThat(response.warning()).isNull();
    }

    @Test
    @DisplayName("Coverage when fully covered by at least one worker: coveredBy > 0, warning = null")
    void evaluateCoverage_fullyCovered_returnsCoveredCountAndNullWarning() {
        when(workerRepository.findAllWithSkills()).thenReturn(List.of(alice, bob, charlie));

        // Alice has both java and spring boot
        TaskCoverageResponseDto response = skillService.evaluateCoverage(
                new TaskCoverageRequestDto(List.of("Java", "Spring Boot"))
        );

        assertThat(response.coveredBy()).isEqualTo(1);
        assertThat(response.warning()).isNull();
        assertThat(response.missingSkills()).isEmpty();
        assertThat(response.perSkill()).containsExactlyInAnyOrder(
                new SkillCoverageItemDto("java", 2),
                new SkillCoverageItemDto("spring boot", 1)
        );
    }

    @Test
    @DisplayName("Coverage when each skill exists, but no single worker has all of them")
    void evaluateCoverage_eachSkillExists_butNoSingleWorkerHasAll() {
        when(workerRepository.findAllWithSkills()).thenReturn(List.of(alice, bob, charlie));

        // spring boot is held by Alice, react is held by Bob. No single worker has both.
        TaskCoverageResponseDto response = skillService.evaluateCoverage(
                new TaskCoverageRequestDto(List.of("spring boot", "react"))
        );

        assertThat(response.coveredBy()).isEqualTo(0);
        assertThat(response.missingSkills()).isEmpty();
        assertThat(response.warning()).isEqualTo("Each skill exists, but no single worker has all of them");
        assertThat(response.perSkill()).containsExactlyInAnyOrder(
                new SkillCoverageItemDto("spring boot", 1),
                new SkillCoverageItemDto("react", 1)
        );
    }

    @Test
    @DisplayName("Coverage when some skills have no worker at all")
    void evaluateCoverage_missingSkillsFromAllWorkers() {
        when(workerRepository.findAllWithSkills()).thenReturn(List.of(alice, bob, charlie));

        // kubernetes is held by 0 workers
        TaskCoverageResponseDto response = skillService.evaluateCoverage(
                new TaskCoverageRequestDto(List.of("java", "kubernetes"))
        );

        assertThat(response.coveredBy()).isEqualTo(0);
        assertThat(response.missingSkills()).containsExactly("kubernetes");
        assertThat(response.warning()).isEqualTo("No worker has skills: [kubernetes]");
        assertThat(response.perSkill()).containsExactlyInAnyOrder(
                new SkillCoverageItemDto("java", 2),
                new SkillCoverageItemDto("kubernetes", 0)
        );
    }
}
