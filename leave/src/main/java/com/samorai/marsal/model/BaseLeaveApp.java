

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "BASE_LEAVE_APP")
@Inheritance(strategy = InheritanceType.JOINED)
@SequenceGenerator(name = "idgen", sequenceName = "SEQ_BASE_LEAVE_APP", initialValue = 1, allocationSize = 1)
public abstract class BaseLeaveApp extends BaseAuditorEntity implements Approvable {

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "OFFICE_ORDER_ID")
    @NotFound(action = NotFoundAction.IGNORE)
    private OfficeOrder officeOrder;

    @Column(name = "RELIGION_ID")
    private Long religionId;

    @Column(name = "EMP_CODE")
    private String empCode;

    @Embedded
    private Approved approved;

    @Enumerated(EnumType.STRING)
    @Column(name = "LEAVE_STATUS", nullable = false, length = 30)
    private LeaveStatus leaveStatus = LeaveStatus.START;

    @Enumerated(EnumType.STRING)
    @Column(name = "LEAVE_EXT_STATUS", length = 30)
    private LeaveExtStatus leaveExtStatus = LeaveExtStatus.ON_TIME;

    @Enumerated(EnumType.STRING)
    @Column(name = "DISPATCH_STATUS")
    private DispatchStatus dispatchStatus = DispatchStatus.APPROVED;

    @Column(name = "HR_EMP_ID", nullable = false)
    private Long hrEmpId;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @JoinColumn(name = "POSITION_ID", nullable = false)
    private Position position; // employee's own position for whom leave app is applied

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @JoinColumn(name = "TO_POSITION_ID", nullable = false)
    private Position toPosition; // initial forward or unit head position id

    @Column(name = "TOTAL_DAYS")
    private Integer totalDays;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @JoinColumn(name = "LEAVE_TYPE_ID", nullable = false)
    private LeaveType leaveType;

    @Column(name = "FROM_DATE", nullable = false)
    private LocalDate fromDate;

    @Column(name = "TO_DATE", nullable = false)
    private LocalDate toDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "ADDRESS_TYPE", nullable = false, length = 30)
    private AddressType addressType;//four value

    @Column(name = "OTHERS_ADDRESS", length = 200)
    private String othersAddress;

    @Column(length = 500)
    private String reason;

    @Column(name = "REASON_DETAILS", length = 500)
    private String reasonDetails;

    @Column(name = "JOIN_DATE")
    private LocalDateTime joinDate;

    @Column(name = "DOC_NO")
    private String docNo;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @JoinColumn(name = "LEVEL_TWO_UNIT_ID", nullable = false)
    private Unit levelTwoUnit;

    @Column(name = "IS_EMERGENCY")
    private boolean emergency;

    // if the leave type is external or not, external leave should be forwarded to external position
    @Override
    public String toString() {
        return "emp: " + hrEmpId + " approvalDesc: " + approvalDesc();
    }
}