package makswinner.fkts.controller;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TopicDto {

  @NotBlank
  private String topic;

}
