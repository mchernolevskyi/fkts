package makswinner.fkts.controller;

import static java.util.Comparator.comparing;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import makswinner.fkts.Message;
import makswinner.fkts.service.SerialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChatController {

    @Autowired
    private SerialService serialService;

    @GetMapping(path = "/topics", produces = APPLICATION_JSON_UTF8_VALUE)
    public Set<String> getTopics() {
        return serialService.getTopics();
    }

    @GetMapping(path = "/topics/{topic}", produces = APPLICATION_JSON_UTF8_VALUE)
    public List<Message> getMessagesByTopic(@PathVariable("topic") @NotBlank String topicBase64) throws UnsupportedEncodingException {
        String topic = new String(Base64.getDecoder().decode(topicBase64), StandardCharsets.UTF_8.name());
        return Optional.ofNullable(serialService.getTopicMessages(topic)).orElse(new HashSet<>()).stream()
                .sorted(comparing(Message::getCreatedDateTime)).collect(Collectors.toList());
    }

    @PostMapping(path = "/messages", consumes = APPLICATION_JSON_UTF8_VALUE)
    public void postMessageToTopic(@Valid @RequestBody MessageDto dto) {
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
