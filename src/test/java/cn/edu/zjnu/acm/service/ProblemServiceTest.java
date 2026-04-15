package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.repo.problem.ProblemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProblemServiceTest {
    @Mock
    private ProblemRepository problemRepository;

    @InjectMocks
    private ProblemService problemService;

    @Test
    public void testCreateDraftProblem() {
        Problem problem = new Problem();
        problem.setTitle("Test Problem");
        problem.setDescription("Test Description");
        problem.setInput("Test Input");
        problem.setOutput("Test Output");
        problem.setSampleInput("Test Sample Input");
        problem.setSampleOutput("Test Sample Output");
        problem.setTimeLimit(1000);
        problem.setMemoryLimit(65536);
        problem.setScore(100);

        when(problemRepository.save(any(Problem.class))).thenReturn(problem);

        Problem result = problemService.createDraftProblem(problem);
        assertNotNull(result);
        assertEquals("Test Problem", result.getTitle());
        assertEquals(Problem.Status.DRAFT, result.getStatus());
        assertEquals(0, result.getSubmitted());
        assertEquals(0, result.getAccepted());
    }

    @Test
    public void testGetProblemStatistics() {
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setTitle("Test Problem");
        problem.setSubmitted(100);
        problem.setAccepted(50);

        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));

        java.util.Map<String, Object> stats = problemService.getProblemStatistics(1L);
        assertNotNull(stats);
        assertEquals(1L, stats.get("problem_id"));
        assertEquals("Test Problem", stats.get("title"));
        assertEquals(100, stats.get("submitted"));
        assertEquals(50, stats.get("accepted"));
    }

    @Test
    public void testGetProblemStatistics_ProblemNotFound() {
        when(problemRepository.findById(1L)).thenReturn(Optional.empty());

        java.util.Map<String, Object> stats = problemService.getProblemStatistics(1L);
        assertNull(stats);
    }
}