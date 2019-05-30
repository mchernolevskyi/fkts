package makswinner.fkts;

import static makswinner.fkts.Util.fromSeconds;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Getter
@Setter
@Builder
@EqualsAndHashCode
public class Message {
    private LocalDateTime dateTime;
    private String topic;
    private String user;
    private String text;
    private boolean sent;
    private boolean received;
    private boolean noTrailingBytes;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getTextToSend() {
        return getTopic() + ":" + getUser() + ":" + getText();
    }

    public static Message of(String receivedText, int seconds, boolean noTrailingBytes) {
        String[] split1 = receivedText.split(":", 2);
        String topic = split1[0];
        String[] split2 = split1[1].split(":", 2);
        String user = split2[0];
        String text = split2[1];
        LocalDateTime dateTime = fromSeconds(seconds);
        return Message.builder().received(true).dateTime(dateTime)
            .topic(topic).user(user).text(text).noTrailingBytes(noTrailingBytes).build();
    }

}
