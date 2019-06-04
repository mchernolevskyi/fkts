package makswinner.fkts.controller;

import makswinner.fkts.Message;
import makswinner.fkts.service.SerialService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@RestController
public class ChatController {

    @GetMapping(path = "/{topic}", produces = APPLICATION_JSON_UTF8_VALUE)
    public Set<Message> getMessagesByTopic(@PathVariable("topic") String topic) {
        return SerialService.MESSAGES.entrySet().stream().flatMap(x -> x.getValue().stream()).filter(m -> m.isReceived())
                .collect(Collectors.toCollection(() -> new TreeSet<>(
                        Comparator.comparing(Message::getReceivedDateTime))));
    }

}
