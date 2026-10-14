package com.example.demo;

import java.util.List;
import java.util.Map;

public final class SimModels {
    private SimModels() {}

    public record Choice(String id, String text, String next, int points, boolean safe, String feedback) {}

    public record Node(String id, String text, List<Choice> choices) {}
    public record Scenario(String id, String title, String category, String intro, int xpReward, String startNode, Map<String, Node> nodes, List<String> takeaways) {
        public int maxPoints() { return best(startNode);}

        private int best(String nodeId) {
            int m = 0;
            for (Choice c : nodes.get(nodeId).choices()) {
                m = Math.max(m, c.points() + (c.next() == null ? 0 : best(c.next())));
                            }
                return m;
        }
    }
    public record Part(String id, String slot, String text, boolean flag, String why ){}

    public record Inspect(String id, String title, String category, String intro, String fromName, boolean phish, int xpReward, List<Part> parts, List<String> takeaways) {
        public long flagCount() { return parts.stream().filter(Part::flag).count();}

    }
    public record PhishTemplate(String id, String name, String fromName, String fromAddr, String subject, String body, String linkText, String linkShown, List<String> redFlags) {}

}


