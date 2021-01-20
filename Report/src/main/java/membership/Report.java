package membership;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="Report_table")
public class Report {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private Long memberId;
        private String name;
        private String grade;
        private String memberStatus;
        private Long mileId;
        private Integer point;
        private String mileStatus;


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
        public String getMemberStatus() {
            return memberStatus;
        }

        public void setMemberStatus(String memberStatus) {
            this.memberStatus = memberStatus;
        }
        public Long getMileId() {
            return mileId;
        }

        public void setMileId(Long mileId) {
            this.mileId = mileId;
        }
        public Integer getPoint() {
            return point;
        }

        public void setPoint(Integer point) {
            this.point = point;
        }
        public String getMileStatus() {
            return mileStatus;
        }

        public void setMileStatus(String mileStatus) {
            this.mileStatus = mileStatus;
        }

}
