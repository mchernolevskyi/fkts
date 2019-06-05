package makswinner.fkts;

import static makswinner.fkts.Util.fromSeconds;

import java.time.LocalDateTime;

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
    private LocalDateTime createdDateTime;
    private String topic;
    private String user;
    private String text;

    private boolean sent;
    private LocalDateTime sentDateTime;

    private boolean received;
    private LocalDateTime receivedDateTime;
    private boolean noTrailingBytes;
    private boolean checksumOk;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getTextToSend() {
        return getTopic() + ":" + getUser() + ":" + getText();
    }

    public static Message received(String receivedText, int seconds, boolean noTrailingBytes, boolean checksumOk) {
        String[] split1 = receivedText.split(":", 2);
        String topic = split1[0];
        String[] split2 = split1[1].split(":", 2);
        String user = split2[0];
        String text = split2[1];
        LocalDateTime dateTime = fromSeconds(seconds);
        return Message.builder().received(true).createdDateTime(dateTime)
            .topic(topic).user(user).text(text).noTrailingBytes(noTrailingBytes).checksumOk(checksumOk)
                .receivedDateTime(LocalDateTime.now()).build();
    }

    public boolean contentEquals(Message other) {
        return createdDateTime.equals(other.getCreatedDateTime()) &&
                topic.equals(other.getTopic()) &&
                user.equals(other.getUser()) &&
                text.equals(other.getText());
    }

}
