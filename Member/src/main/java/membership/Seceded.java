package membership;

public class Seceded extends AbstractEvent {

    private Long id;
    private Long mileageId;
    private String name;
    private String grade;
    private String status;

    public Seceded(){
        super();
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
