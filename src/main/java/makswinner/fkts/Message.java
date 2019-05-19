package makswinner.fkts;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Message {
    private String topic;
    private String user;
    private String text;
}
