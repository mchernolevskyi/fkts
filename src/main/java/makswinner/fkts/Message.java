package makswinner.fkts;

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
}
