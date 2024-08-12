package hyundai.softeer.orange.comment.dto;

import hyundai.softeer.orange.common.util.MessageUtil;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentDto {

    @Size(min = 1, max = 100, message = MessageUtil.OUT_OF_SIZE)
    private String content;
}
