package com.rankwise.chat.service;

import com.rankwise.chat.entity.FaqArticle;
import com.rankwise.chat.repository.FaqArticleRepository;
import com.rankwise.config.AppProperties;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatRagService {

    private final FaqArticleRepository faqRepository;
    private final int maxArticles;

    public ChatRagService(FaqArticleRepository faqRepository, AppProperties props) {
        this.faqRepository = faqRepository;
        this.maxArticles = props.getChat().getRagMaxArticles();
    }

    public List<FaqArticle> retrieve(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return List.of();
        }
        Set<FaqArticle> found = new LinkedHashSet<>();
        for (String term : extractTerms(userMessage)) {
            found.addAll(faqRepository.searchByTerm(term, maxArticles));
            if (found.size() >= maxArticles) {
                break;
            }
        }
        if (found.isEmpty()) {
            found.addAll(faqRepository.findByActiveTrueOrderByCategoryAscTitleAsc().stream().limit(2).toList());
        }
        return found.stream().limit(maxArticles).toList();
    }

    public String formatForPrompt(List<FaqArticle> articles) {
        if (articles.isEmpty()) {
            return "";
        }
        return articles.stream()
                .map(a -> "### " + a.getTitle() + " (" + a.getCategory() + ")\n" + a.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    private static List<String> extractTerms(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        List<String> terms = new ArrayList<>();
        String[] keywords = {
                "counselling", "counseling", "web options", "certificate", "document", "rank",
                "dream", "target", "safe", "branch", "cse", "ece", "fee", "reimbursement",
                "scholarship", "spot", "phase", "allotment", "compare", "college", "eapcet"
        };
        for (String k : keywords) {
            if (lower.contains(k)) {
                terms.add(k.split(" ")[0]);
            }
        }
        if (terms.isEmpty()) {
            String[] words = lower.split("\\s+");
            for (String w : words) {
                if (w.length() >= 4) {
                    terms.add(w);
                }
            }
        }
        return terms.isEmpty() ? List.of("counselling") : terms.stream().distinct().limit(3).toList();
    }
}
