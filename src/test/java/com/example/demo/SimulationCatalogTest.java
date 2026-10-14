package com.example.demo;

import com.example.demo.SimModels.*;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Content integrity checks. No Spring context or database needed. */
class SimulationCatalogTest {

    private final SimulationCatalog catalog = new SimulationCatalog();

    @Test
    void everyScenarioIsAnAcyclicFullyReachableGraph() {
        for (Scenario s : catalog.scenarios()) {
            Set<String> reached = new HashSet<>();
            walk(s, s.startNode(), new HashSet<>(), reached);
            assertEquals(s.nodes().keySet(), reached, "unreachable nodes in " + s.id());
            assertTrue(s.maxPoints() > 0, "no scoring available in " + s.id());
            assertFalse(s.takeaways().isEmpty(), "missing takeaways in " + s.id());
        }
    }

    @Test
    void bestChoiceAtEveryStepIsTheSafeOne() {
        for (Scenario s : catalog.scenarios()) {
            for (Node n : s.nodes().values()) {
                Choice best = n.choices().stream().max((a, b) -> Integer.compare(a.points(), b.points())).orElseThrow();
                assertTrue(best.safe(), "best choice is unsafe in " + s.id() + "/" + n.id());
                for (Choice c : n.choices()) {
                    if (!c.safe()) assertTrue(c.points() <= 3, "unsafe choice earns too much in " + s.id());
                    assertTrue(c.next() == null || s.nodes().containsKey(c.next()), "dangling next in " + s.id());
                }
            }
        }
    }

    @Test
    void inspectorEmailsAreWellFormed() {
        for (Inspect i : catalog.inspects()) {
            assertTrue(i.parts().stream().anyMatch(p -> p.slot().equals("from")), "no sender in " + i.id());
            assertTrue(i.parts().stream().anyMatch(p -> p.slot().equals("subject")), "no subject in " + i.id());
            if (i.phish()) assertTrue(i.flagCount() >= 3, "phish needs several red flags: " + i.id());
            else assertEquals(0, i.flagCount(), "legitimate email must have no red flags: " + i.id());
        }
    }

    @Test
    void phishingTemplatesTeachAtLeastThreeRedFlags() {
        for (PhishTemplate t : catalog.templates()) {
            assertTrue(t.redFlags().size() >= 3, t.id());
        }
    }

    private void walk(Scenario s, String id, Set<String> path, Set<String> reached) {
        assertTrue(path.add(id), "cycle in " + s.id() + " at " + id);
        reached.add(id);
        for (Choice c : s.nodes().get(id).choices()) {
            if (c.next() != null) walk(s, c.next(), path, reached);
        }
        path.remove(id);
    }
}
