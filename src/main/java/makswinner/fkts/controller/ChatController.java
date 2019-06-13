package makswinner.fkts.controller;

import static java.util.Comparator.comparing;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.validation.Valid;
import makswinner.fkts.Message;
import makswinner.fkts.service.SerialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChatController {

    @Autowired
    private SerialService serialService;

    @GetMapping(path = "/topics", produces = APPLICATION_JSON_UTF8_VALUE)
    public List<String> getTopics() {
        List<String> list = new ArrayList<>(serialService.getTopics());
        Collections.sort(list);
        return list;
    }

    @GetMapping(path = "/messages", produces = APPLICATION_JSON_UTF8_VALUE)
    public List<Message> getMessagesByTopic(@RequestParam("topic") String topic) {
        return Optional.ofNullable(serialService.getTopicMessages(topic)).orElse(new HashSet<>()).stream()
                .sorted(comparing(Message::getCreatedDateTime)).collect(Collectors.toList());
    }

    @PostMapping(path = "/topics", consumes = APPLICATION_JSON_UTF8_VALUE)
    public void createNewTopic(@Valid @RequestBody TopicDto dto) {
        serialService.createNewTopic(dto.getTopic());
    }

    @PostMapping(path = "/messages", consumes = APPLICATION_JSON_UTF8_VALUE)
    public void sendMessage(@Valid @RequestBody MessageDto dto) {
        serialService.sendMessage(convertToMessage(dto));
    }

    @GetMapping(path = "/statistics", produces = APPLICATION_JSON_UTF8_VALUE)
    public Map<String, Object> getStatistics() {
        return serialService.getStatistics();
    }

    private Message convertToMessage(MessageDto dto) {
        return Message.builder()
                .topic(dto.getTopic())
                .user(dto.getUser())
                .text(dto.getText())
                .createdDateTime(LocalDateTime.now())
                .build();
    }

}
