
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;

@Data
@Entity
//@AllArgsConstructor
//@NoArgsConstructor
@Table(name = "LEAVE_APP")
public class LeaveApp extends BaseLeaveApp {
    public LeaveApp(){
    }

    @Override
    public String toString() {
        return super.toString();
    }

}