

import com.ibcs.ngpims.model.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.codehaus.jackson.annotate.JsonProperty;

import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveAppDto {
    private Long id;
    private Long officeOrder;
    private String approvalStatus;
    private LocalDate appDate;
    private String remarks;
    private Long religionId;
    private Long hrEmpId;
    private String hrEmpCode;
    private String ofvisCode;
    private Long position;
    private Long toPosition;
    private Long leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate plFromDate;
    private LocalDate plToDate;
    private Long country;
    private boolean applied;//used
    private boolean emergency;
    private String addressType;//four value
    private String othersAddress;
    private String reason;
    private String reasonDetails;
    private LocalDate cancelDate;
    private Long userId;
    private Integer version;
}
