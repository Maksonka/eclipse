package com.example.shadowvibe.Services;

import com.example.shadowvibe.DTO.MessageSearchResultDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MessageSearchService {

    private final MessageService messageService;
    private final GroupService groupService;

    public MessageSearchService(MessageService messageService, GroupService groupService) {
        this.messageService = messageService;
        this.groupService = groupService;
    }

    public List<MessageSearchResultDto> searchAll(String username, String query, int limit) {
        List<MessageSearchResultDto> results = new ArrayList<>();
        results.addAll(messageService.searchAllDirectHistory(username, query, limit));
        results.addAll(groupService.searchAllGroupHistory(username, query, limit));
        results.sort(Comparator.comparingLong(MessageSearchResultDto::getSortTimestamp).reversed());
        if (results.size() > limit) {
            results = new ArrayList<>(results.subList(0, limit));
        }
        return results;
    }
}
