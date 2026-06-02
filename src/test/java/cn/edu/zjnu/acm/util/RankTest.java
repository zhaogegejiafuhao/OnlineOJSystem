package cn.edu.zjnu.acm.util;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Contest;
import cn.edu.zjnu.acm.entity.oj.ContestProblem;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.Solution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RankTest {

    private Instant baseTime;
    private List<Boolean> problemHasAc;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.of(2026, 1, 1, 9, 0)
                .atZone(ZoneId.systemDefault()).toInstant();
        problemHasAc = new ArrayList<>(Arrays.asList(false, false, false));
    }

    private User createUser(long id, String name) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setUsername(name);
        return u;
    }

    private Rank.RankRow createRow(User user, int problemNumber) {
        try {
            return new Rank.RankRow(problemNumber, user);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    private Solution createAcSolution(User user, int problemIndex, int minutesAfterStart) {
        Contest contest = new Contest();
        contest.setStartTime("2026-01-01 09:00");
        contest.setStartAndEndTime("2026-01-01 09:00", 180L);

        Solution s = new Solution();
        s.setUser(user);
        s.setResult(Solution.AC);
        s.setContest(contest);
        s.setSubmitTime(baseTime.plusSeconds(minutesAfterStart * 60L));

        Problem p = new Problem();
        p.setId((long) (problemIndex + 1));
        s.setProblem(p);
        return s;
    }

    private Solution createWrongSolution(User user, int problemIndex, int minutesAfterStart) {
        Contest contest = new Contest();
        contest.setStartTime("2026-01-01 09:00");
        contest.setStartAndEndTime("2026-01-01 09:00", 180L);

        Solution s = new Solution();
        s.setUser(user);
        s.setResult("Wrong Answer");
        s.setContest(contest);
        s.setSubmitTime(baseTime.plusSeconds(minutesAfterStart * 60L));

        Problem p = new Problem();
        p.setId((long) (problemIndex + 1));
        s.setProblem(p);
        return s;
    }

    @Test
    @DisplayName("ACM排名：解题数多的排前面")
    void testMoreSolvedRanksHigher() {
        User alice = createUser(1L, "Alice");
        User bob = createUser(2L, "Bob");

        Rank.RankRow aliceRow = createRow(alice, 3);
        Rank.RankRow bobRow = createRow(bob, 3);

        aliceRow.update(createAcSolution(alice, 0, 10), problemHasAc);
        aliceRow.update(createAcSolution(alice, 1, 20), problemHasAc);
        bobRow.update(createAcSolution(bob, 0, 5), problemHasAc);

        List<Rank.RankRow> rows = new ArrayList<>(Arrays.asList(aliceRow, bobRow));
        Collections.sort(rows);

        assertEquals(2, rows.get(0).getSolved());
        assertEquals(1, rows.get(1).getSolved());
        assertEquals("Alice", rows.get(0).getUser().getName());
    }

    @Test
    @DisplayName("ACM排名：解题数相同时罚时少的排前面")
    void testSameSolvedLessPenaltyRanksHigher() {
        User alice = createUser(1L, "Alice");
        User bob = createUser(2L, "Bob");

        Rank.RankRow aliceRow = createRow(alice, 3);
        Rank.RankRow bobRow = createRow(bob, 3);

        aliceRow.update(createAcSolution(alice, 0, 60), problemHasAc);
        bobRow.update(createAcSolution(bob, 0, 10), problemHasAc);

        List<Rank.RankRow> rows = new ArrayList<>(Arrays.asList(aliceRow, bobRow));
        Collections.sort(rows);

        assertEquals("Bob", rows.get(0).getUser().getName());
        assertTrue(rows.get(0).getPenalty() < rows.get(1).getPenalty());
    }

    @Test
    @DisplayName("ACM排名：错误提交每次加20分钟罚时")
    void testWrongSubmissionAddsPenalty() {
        User alice = createUser(1L, "Alice");
        Rank.RankRow row = createRow(alice, 3);

        row.update(createWrongSolution(alice, 0, 5), problemHasAc);
        row.update(createWrongSolution(alice, 0, 8), problemHasAc);
        row.update(createAcSolution(alice, 0, 10), problemHasAc);

        assertEquals(1, row.getSolved());
        assertEquals(10 + 2 * 20, row.getPenalty());
    }

    @Test
    @DisplayName("ACM排名：已通过的题目不再更新")
    void testAcceptedProblemNotUpdated() {
        User alice = createUser(1L, "Alice");
        Rank.RankRow row = createRow(alice, 3);

        row.update(createAcSolution(alice, 0, 10), problemHasAc);
        row.update(createWrongSolution(alice, 0, 15), problemHasAc);

        assertEquals(1, row.getSolved());
        assertEquals(10, row.getPenalty());
    }

    @Test
    @DisplayName("ACM排名：首次通过标记(first)")
    void testFirstBloodMarking() {
        User alice = createUser(1L, "Alice");
        User bob = createUser(2L, "Bob");

        Rank.RankRow aliceRow = createRow(alice, 3);
        Rank.RankRow bobRow = createRow(bob, 3);

        List<Boolean> acFlag = new ArrayList<>(Arrays.asList(false, false, false));

        aliceRow.update(createAcSolution(alice, 0, 10), acFlag);
        bobRow.update(createAcSolution(bob, 0, 15), acFlag);

        assertTrue(aliceRow.getBoxes().get(0).getFirst());
        assertFalse(bobRow.getBoxes().get(0).getFirst());
    }

    @Test
    @DisplayName("ACM排名：PENDING状态不计入排名")
    void testPendingNotCounted() {
        User alice = createUser(1L, "Alice");
        Rank.RankRow row = createRow(alice, 3);

        Contest contest = new Contest();
        contest.setStartTime("2026-01-01 09:00");
        contest.setStartAndEndTime("2026-01-01 09:00", 180L);

        Solution s = new Solution();
        s.setUser(alice);
        s.setResult(Solution.PENDING);
        s.setContest(contest);
        s.setSubmitTime(baseTime.plusSeconds(10 * 60));
        Problem p = new Problem();
        p.setId(1L);
        s.setProblem(p);

        row.update(s, problemHasAc);

        assertEquals(0, row.getSolved());
        assertEquals(0, row.getPenalty());
    }

    @Test
    @DisplayName("ACM排名：多用户多题目综合排名")
    void testComprehensiveRanking() {
        User alice = createUser(1L, "Alice");
        User bob = createUser(2L, "Bob");
        User charlie = createUser(3L, "Charlie");

        List<Boolean> acFlag1 = new ArrayList<>(Arrays.asList(false, false, false));
        List<Boolean> acFlag2 = new ArrayList<>(Arrays.asList(false, false, false));
        List<Boolean> acFlag3 = new ArrayList<>(Arrays.asList(false, false, false));

        Rank.RankRow aliceRow = createRow(alice, 3);
        aliceRow.update(createAcSolution(alice, 0, 10), acFlag1);
        aliceRow.update(createWrongSolution(alice, 1, 15), acFlag1);
        aliceRow.update(createAcSolution(alice, 1, 20), acFlag1);
        aliceRow.update(createAcSolution(alice, 2, 30), acFlag1);

        Rank.RankRow bobRow = createRow(bob, 3);
        bobRow.update(createAcSolution(bob, 0, 5), acFlag2);
        bobRow.update(createAcSolution(bob, 1, 25), acFlag2);
        bobRow.update(createWrongSolution(bob, 2, 35), acFlag2);

        Rank.RankRow charlieRow = createRow(charlie, 3);
        charlieRow.update(createWrongSolution(charlie, 0, 3), acFlag3);
        charlieRow.update(createWrongSolution(charlie, 0, 6), acFlag3);
        charlieRow.update(createAcSolution(charlie, 0, 10), acFlag3);
        charlieRow.update(createAcSolution(charlie, 1, 40), acFlag3);

        List<Rank.RankRow> rows = new ArrayList<>(Arrays.asList(aliceRow, bobRow, charlieRow));
        Collections.sort(rows);

        assertEquals("Alice", rows.get(0).getUser().getName());
        assertEquals(3, rows.get(0).getSolved());

        assertEquals(2, rows.get(1).getSolved());
        assertEquals(2, rows.get(2).getSolved());
        assertTrue(rows.get(1).getPenalty() < rows.get(2).getPenalty(),
                "Bob和Charlie都解2题，Bob罚时少应排前面");
    }
}
