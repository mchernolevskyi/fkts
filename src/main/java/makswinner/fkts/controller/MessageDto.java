package makswinner.fkts.controller;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MessageDto {
  @NotBlank
  private String topic;
  @NotBlank
  private String user;
  @NotBlank
  private String text;
}
