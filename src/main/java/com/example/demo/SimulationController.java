package com.example.demo;

import com.example.demo.SimModels.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

import static com.example.demo.Dtos.m;

/** Employee-facing training: branching missions, "spot the red flags" emails, and the practice inbox. */
@RestController
@RequestMapping("/api/sim")
public class SimulationController {

    public record InspectSubmission(List<String> flagged, String action) {}

    private static final int REPORT_XP = 25;

    private final SimulationCatalog catalog;
    private final SimulationAttemptRepository attempts;
    private final PhishDeliveryRepository deliveries;
    private final PhishCampaignRepository campaigns;
    private final XpService xp;

    public SimulationController(SimulationCatalog catalog, SimulationAttemptRepository attempts,
                                PhishDeliveryRepository deliveries, PhishCampaignRepository campaigns, XpService xp) {
        this.catalog = catalog;
        this.attempts = attempts;
        this.deliveries = deliveries;
        this.campaigns = campaigns;
        this.xp = xp;
    }

    // ---------- catalog ----------
    @GetMapping("/catalog")
    public List<Map<String, Object>> catalog(Authentication auth) {
        List<SimulationAttempt> mine = attempts.findByUserEmail(auth.getName());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Scenario s : catalog.scenarios())
            out.add(summary(s.id(), s.title(), s.category(), "BRANCH", s.intro(), s.xpReward(), mine));
        for (Inspect i : catalog.inspects())
            out.add(summary(i.id(), i.title(), i.category(), "INSPECT", i.intro(), i.xpReward(), mine));
        return out;
    }

    private Map<String, Object> summary(String id, String title, String cat, String kind, String intro,
                                        int reward, List<SimulationAttempt> mine) {
        int best = -1;
        boolean inProgress = false;
        for (SimulationAttempt a : mine) {
            if (!a.getScenarioId().equals(id)) continue;
            if ("IN_PROGRESS".equals(a.getStatus())) inProgress = true;
            if ("COMPLETED".equals(a.getStatus()) && a.getMaxScore() > 0)
                best = Math.max(best, a.getScore() * 100 / a.getMaxScore());
        }
        return m("id", id, "title", title, "category", cat, "kind", kind, "intro", intro,
                "xpReward", reward, "bestPercent", best, "inProgress", inProgress);
    }

    // ---------- branching missions ----------
    private Map<String, Object> nodeView(Node n) {
        List<Map<String, Object>> cs = new ArrayList<>();
        for (Choice c : n.choices()) cs.add(m("id", c.id(), "text", c.text()));
        return m("id", n.id(), "text", n.text(), "choices", cs);
    }

    @PostMapping("/branch/{id}/start")
    public ResponseEntity<?> start(@PathVariable String id, Authentication auth) {
        Scenario s = catalog.scenario(id);
        if (s == null) return ResponseEntity.notFound().build();
        SimulationAttempt at = attempts
                .findByUserEmailAndScenarioIdAndStatus(auth.getName(), id, "IN_PROGRESS").stream().findFirst()
                .orElseGet(() -> {
                    SimulationAttempt n = new SimulationAttempt();
                    n.setUserEmail(auth.getName());
                    n.setScenarioId(id);
                    n.setKind("BRANCH");
                    n.setCurrentNode(s.startNode());
                    n.setMaxScore(s.maxPoints());
                    return attempts.save(n);
                });
        return ResponseEntity.ok(m("attemptId", at.getId(), "title", s.title(),
                "node", nodeView(s.nodes().get(at.getCurrentNode())), "score", at.getScore(), "max", at.getMaxScore()));
    }

    @PostMapping("/branch/attempt/{attemptId}/choose")
    public ResponseEntity<?> choose(@PathVariable Long attemptId, @RequestBody Map<String, String> body,
                                    Authentication auth) {
        SimulationAttempt at = attempts.findById(attemptId)
                .filter(a -> a.getUserEmail().equals(auth.getName()) && "BRANCH".equals(a.getKind()))
                .orElse(null);
        if (at == null) return ResponseEntity.notFound().build();
        if (!"IN_PROGRESS".equals(at.getStatus()))
            return ResponseEntity.badRequest().body(m("message", "This attempt is already finished."));

        Scenario s = catalog.scenario(at.getScenarioId());
        Node cur = s.nodes().get(at.getCurrentNode());
        String choiceId = body.getOrDefault("choiceId", "");
        Choice ch = cur.choices().stream().filter(c -> c.id().equals(choiceId)).findFirst().orElse(null);
        if (ch == null) return ResponseEntity.badRequest().body(m("message", "Unknown choice."));

        at.setScore(at.getScore() + ch.points());
        if (!ch.safe()) at.setRiskyChoices(at.getRiskyChoices() + 1);

        Map<String, Object> out = m("feedback", ch.feedback(), "safe", ch.safe(), "points", ch.points());
        if (ch.next() != null) {
            at.setCurrentNode(ch.next());
            attempts.save(at);
            out.put("done", false);
            out.put("node", nodeView(s.nodes().get(ch.next())));
            out.put("score", at.getScore());
            return ResponseEntity.ok(out);
        }
        int max = Math.max(1, s.maxPoints());
        int pct = Math.min(100, at.getScore() * 100 / max);
        out.putAll(complete(at, s.id(), s.xpReward(), pct));
        out.put("done", true);
        out.put("score", at.getScore());
        out.put("max", max);
        out.put("takeaways", s.takeaways());
        return ResponseEntity.ok(out);
    }

    /** Marks the attempt complete and awards only the XP improvement over the user's previous best. */
    private Map<String, Object> complete(SimulationAttempt at, String scenarioId, int reward, int pct) {
        int earned = reward * pct / 100;
        int prevBest = attempts.findByUserEmailAndScenarioId(at.getUserEmail(), scenarioId).stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .mapToInt(SimulationAttempt::getXpEarned).max().orElse(0);
        int delta = Math.max(0, earned - prevBest);
        at.setXpEarned(earned);
        at.setStatus("COMPLETED");
        at.setCompletedAt(LocalDateTime.now());
        attempts.save(at);
        xp.award(at.getUserEmail(), delta);
        return m("percent", pct, "xpAwarded", delta);
    }

    // ---------- spot the red flags ----------
    @GetMapping("/inspector/{id}")
    public ResponseEntity<?> inspector(@PathVariable String id) {
        Inspect i = catalog.inspect(id);
        if (i == null) return ResponseEntity.notFound().build();
        List<Map<String, Object>> parts = new ArrayList<>();
        for (Part p : i.parts()) parts.add(m("id", p.id(), "slot", p.slot(), "text", p.text()));
        return ResponseEntity.ok(m("id", i.id(), "title", i.title(), "intro", i.intro(),
                "fromName", i.fromName(), "xpReward", i.xpReward(), "parts", parts));
    }

    @PostMapping("/inspector/{id}/submit")
    public ResponseEntity<?> inspectorSubmit(@PathVariable String id, @RequestBody InspectSubmission req,
                                             Authentication auth) {
        Inspect i = catalog.inspect(id);
        if (i == null) return ResponseEntity.notFound().build();
        String action = req.action() == null ? "" : req.action();
        if (!Set.of("REPORT", "DELETE", "TRUST").contains(action))
            return ResponseEntity.badRequest().body(m("message", "Choose what you would do with this email."));

        Set<String> sel = new HashSet<>(req.flagged() == null ? List.of() : req.flagged());
        int pts = 0, hits = 0, falseFlags = 0, missed = 0;
        List<Map<String, Object>> review = new ArrayList<>();
        for (Part p : i.parts()) {
            boolean picked = sel.contains(p.id());
            if (p.flag() && picked) { hits++; pts += 10; }
            else if (p.flag()) missed++;
            else if (picked) { falseFlags++; pts -= 5; }
            review.add(m("id", p.id(), "flag", p.flag(), "selected", picked, "why", p.why()));
        }
        int actionPts = i.phish()
                ? switch (action) { case "REPORT" -> 20; case "DELETE" -> 10; default -> 0; }
                : switch (action) { case "TRUST" -> 20; case "DELETE" -> 10; default -> 5; };
        pts = Math.max(0, pts) + actionPts;
        int max = (int) i.flagCount() * 10 + 20;
        int pct = Math.min(100, pts * 100 / max);

        SimulationAttempt at = new SimulationAttempt();
        at.setUserEmail(auth.getName());
        at.setScenarioId(id);
        at.setKind("INSPECT");
        at.setScore(pts);
        at.setMaxScore(max);
        at.setRiskyChoices(i.phish() && "TRUST".equals(action) ? 1 : 0);
        Map<String, Object> done = complete(at, id, i.xpReward(), pct);

        String best = i.phish() ? "Report it as phishing" : "Treat it as legitimate";
        Map<String, Object> out = m("score", pts, "max", max, "hits", hits, "missed", missed,
                "falseFlags", falseFlags, "isPhish", i.phish(), "bestAction", best,
                "review", review, "takeaways", i.takeaways());
        out.putAll(done);
        return ResponseEntity.ok(out);
    }

    // ---------- practice inbox (simulated phishing campaigns) ----------
    @GetMapping("/inbox")
    public List<Map<String, Object>> inbox(Authentication auth) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PhishDelivery d : deliveries.findByUserEmailOrderByDeliveredAtDesc(auth.getName())) {
            PhishCampaign camp = campaigns.findById(d.getCampaignId()).orElse(null);
            PhishTemplate t = camp == null ? null : catalog.template(camp.getTemplateId());
            if (t == null) continue;
            String status = d.getReportedAt() != null ? "REPORTED" : d.getClickedAt() != null ? "CLICKED" : "NEW";
            out.add(m("id", d.getId(), "fromName", t.fromName(), "fromAddr", t.fromAddr(), "subject", t.subject(),
                    "body", t.body(), "linkText", t.linkText(), "linkShown", t.linkShown(),
                    "status", status, "deliveredAt", d.getDeliveredAt().toString()));
        }
        return out;
    }

    private PhishDelivery mine(Long id, Authentication auth) {
        return deliveries.findById(id).filter(d -> d.getUserEmail().equals(auth.getName())).orElse(null);
    }

    private List<String> flagsFor(PhishDelivery d) {
        return campaigns.findById(d.getCampaignId()).map(c -> catalog.template(c.getTemplateId()))
                .map(PhishTemplate::redFlags).orElse(List.of());
    }

    /** The link in a simulated email never leaves the app. Clicking it records the click and teaches. */
    @PostMapping("/inbox/{id}/click")
    public ResponseEntity<?> click(@PathVariable Long id, Authentication auth) {
        PhishDelivery d = mine(id, auth);
        if (d == null) return ResponseEntity.notFound().build();
        if (d.getClickedAt() == null && d.getReportedAt() == null) {
            d.setClickedAt(LocalDateTime.now());
            deliveries.save(d);
        }
        return ResponseEntity.ok(m("message", "This was a training simulation. In a real attack, your click could have exposed your password.",
                "redFlags", flagsFor(d)));
    }

    @PostMapping("/inbox/{id}/report")
    public ResponseEntity<?> report(@PathVariable Long id, Authentication auth) {
        PhishDelivery d = mine(id, auth);
        if (d == null) return ResponseEntity.notFound().build();
        int awarded = 0;
        if (d.getReportedAt() == null) {
            d.setReportedAt(LocalDateTime.now());
            deliveries.save(d);
            awarded = xp.award(auth.getName(), REPORT_XP);
        }
        return ResponseEntity.ok(m("message", "Well spotted. Reporting suspicious email is exactly what protects your coworkers.",
                "xpAwarded", awarded, "redFlags", flagsFor(d)));
    }
}
