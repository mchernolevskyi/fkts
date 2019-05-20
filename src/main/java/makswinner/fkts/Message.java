package makswinner.fkts;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Message {
    private LocalDateTime dateTime;
    private String topic;
    private String user;
    private String text;
    private boolean sent;
    private boolean received;
}
