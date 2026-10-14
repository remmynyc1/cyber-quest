package com.example.demo;

import com.example.demo.SimModels.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.example.demo.Dtos.m;

/** Manager-only: launch simulated phishing campaigns and see training progress. */
@RestController
@RequestMapping("/api/training")
public class TrainingAdminController {

    public record LaunchRequest(String templateId, String name) {}

    private final SimulationCatalog catalog;
    private final PhishCampaignRepository campaigns;
    private final PhishDeliveryRepository deliveries;
    private final SimulationAttemptRepository attempts;
    private final UserRepository users;

    public TrainingAdminController(SimulationCatalog catalog, PhishCampaignRepository campaigns,
                                   PhishDeliveryRepository deliveries, SimulationAttemptRepository attempts,
                                   UserRepository users) {
        this.catalog = catalog;
        this.campaigns = campaigns;
        this.deliveries = deliveries;
        this.attempts = attempts;
        this.users = users;
    }

    private static boolean isLearner(UserAccount u) {
        return "ROLE_EMPLOYEE".equals(u.getRole()) || "ROLE_ANALYST".equals(u.getRole());
    }

    @GetMapping("/templates")
    public List<Map<String, Object>> templates() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PhishTemplate t : catalog.templates()) out.add(m("id", t.id(), "name", t.name(), "subject", t.subject()));
        return out;
    }

    @PostMapping("/campaigns")
    public ResponseEntity<?> launch(@RequestBody LaunchRequest req, Authentication auth) {
        PhishTemplate t = req.templateId() == null ? null : catalog.template(req.templateId());
        if (t == null) return ResponseEntity.badRequest().body(m("message", "Choose a template."));
        PhishCampaign c = new PhishCampaign();
        c.setTemplateId(t.id());
        String name = req.name() == null ? "" : req.name().trim();
        c.setName(name.isEmpty() ? t.name() : name.substring(0, Math.min(name.length(), 80)));
        c.setLaunchedBy(auth.getName());
        c = campaigns.save(c);
        for (UserAccount u : users.findAll()) {
            if (!isLearner(u)) continue;
            PhishDelivery d = new PhishDelivery();
            d.setCampaignId(c.getId());
            d.setUserEmail(u.getEmail());
            deliveries.save(d);
        }
        return ResponseEntity.ok(campaignSummary(c));
    }

    @GetMapping("/campaigns")
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PhishCampaign c : campaigns.findAllByOrderByLaunchedAtDesc()) out.add(campaignSummary(c));
        return out;
    }

    private Map<String, Object> campaignSummary(PhishCampaign c) {
        List<PhishDelivery> ds = deliveries.findByCampaignId(c.getId());
        int targeted = ds.size();
        long clicked = ds.stream().filter(d -> d.getClickedAt() != null).count();
        long reported = ds.stream().filter(d -> d.getReportedAt() != null).count();
        return m("id", c.getId(), "name", c.getName(), "templateId", c.getTemplateId(),
                "launchedAt", c.getLaunchedAt().toString(), "targeted", targeted,
                "clicked", clicked, "reported", reported,
                "clickRate", targeted == 0 ? 0 : (int) (clicked * 100 / targeted),
                "reportRate", targeted == 0 ? 0 : (int) (reported * 100 / targeted));
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        // best percent per scenario per user
        Map<String, Map<String, Integer>> best = new HashMap<>();
        for (SimulationAttempt a : attempts.findByStatus("COMPLETED")) {
            int pct = a.getMaxScore() > 0 ? a.getScore() * 100 / a.getMaxScore() : 0;
            best.computeIfAbsent(a.getScenarioId(), k -> new HashMap<>()).merge(a.getUserEmail(), pct, Math::max);
        }
        List<UserAccount> learners = users.findAll().stream().filter(TrainingAdminController::isLearner).toList();

        List<Map<String, Object>> modules = new ArrayList<>();
        List<String[]> ids = new ArrayList<>();
        for (Scenario s : catalog.scenarios()) ids.add(new String[]{s.id(), s.title()});
        for (Inspect i : catalog.inspects()) ids.add(new String[]{i.id(), i.title()});
        for (String[] id : ids) {
            Map<String, Integer> per = best.getOrDefault(id[0], Map.of());
            double avg = per.values().stream().mapToInt(Integer::intValue).average().orElse(0);
            modules.add(m("id", id[0], "title", id[1], "completed", per.size(), "learners", learners.size(),
                    "avgPercent", (int) Math.round(avg)));
        }

        Map<String, int[]> phish = new HashMap<>(); // [clicked, reported]
        for (PhishDelivery d : deliveries.findAll()) {
            int[] v = phish.computeIfAbsent(d.getUserEmail(), k -> new int[2]);
            if (d.getClickedAt() != null) v[0]++;
            if (d.getReportedAt() != null) v[1]++;
        }
        List<Map<String, Object>> people = new ArrayList<>();
        for (UserAccount u : learners) {
            int done = 0, sum = 0;
            for (String[] id : ids) {
                Integer pct = best.getOrDefault(id[0], Map.of()).get(u.getEmail());
                if (pct != null) { done++; sum += pct; }
            }
            int[] ph = phish.getOrDefault(u.getEmail(), new int[2]);
            people.add(m("name", u.getFullName(), "email", u.getEmail(), "completed", done, "total", ids.size(),
                    "avgPercent", done == 0 ? 0 : sum / done, "totalXp", u.getTotalXp(),
                    "phishClicked", ph[0], "phishReported", ph[1]));
        }
        return m("modules", modules, "learners", people);
    }
}
