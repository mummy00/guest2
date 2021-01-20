package membership;

import javax.persistence.*;

import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="MileageMgmt_table")
public class MileageMgmt {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private Integer point;
    private String status;

    @PostPersist
    public void onPostPersist(){

        MileageGived mileageGived = new MileageGived();
        BeanUtils.copyProperties(this, mileageGived);
        mileageGived.publishAfterCommit();

    }

    @PostUpdate
    public void onPreUpdate() {
        MileageDeleted mileageDeleted = new MileageDeleted();
        BeanUtils.copyProperties(this, mileageDeleted);
        mileageDeleted.setStatus("remove");
        mileageDeleted.publishAfterCommit();
    }


    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getMemberId() {
        return memberId;
    }
    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
    public Integer getPoint() {
        return point;
    }
    public void setPoint(Integer point) {
        this.point = point;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }




}
