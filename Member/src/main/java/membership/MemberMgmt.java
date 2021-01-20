package membership;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="MemberMgmt_table")
public class MemberMgmt {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private Long mileageId;
    private String name;
    private String grade;
    private String status;

    @PostPersist
    public void onPostPersist(){
        Signed signed = new Signed();
        BeanUtils.copyProperties(this, signed);
        signed.setStatus("No Point");
        signed.publishAfterCommit();


    }

    @PreRemove
    public void PreRemove(){
        Seceded seceded = new Seceded();
        BeanUtils.copyProperties(this, seceded);
        seceded.setStatus("end member");
        seceded.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        membership.external.MileageMgmt mileageMgmt = new membership.external.MileageMgmt();
        mileageMgmt.setId(seceded.getMileageId());
        mileageMgmt.setMemberId(seceded.getId());
        mileageMgmt.setPoint(0);
        mileageMgmt.setStatus("removeRequest");
        // mappings goes here
        MemberApplication.applicationContext.getBean(membership.external.MileageMgmtService.class)
            .mileageDelete(mileageMgmt);


    }


    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getMileageId() {
        return mileageId;
    }
    public void setMileageId(Long id) {
        this.mileageId = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getGrade() {
        return grade;
    }
    public void setGrade(String grade) {
        this.grade = grade;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }




}
