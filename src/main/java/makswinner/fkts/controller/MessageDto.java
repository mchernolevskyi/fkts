package makswinner.fkts.controller;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MessageDto {
  private String topic;
  private String user;
  private String text;
}
