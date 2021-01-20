package membership;

import membership.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ReportViewHandler {


    @Autowired
    private ReportRepository reportRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenSigned_then_CREATE_1 (@Payload Signed signed) {
        try {
            if (signed.isMe()) {
                // view 객체 생성
                Report report = new Report();
                // view 객체에 이벤트의 Value 를 set 함
                report.setMemberId(signed.getId());
                report.setName(signed.getName());
                report.setGrade(signed.getGrade());
                report.setMemberStatus(signed.getStatus());
                // view 레파지 토리에 save
                reportRepository.save(report);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenMileageGived_then_UPDATE_1(@Payload MileageGived mileageGived) {
        try {
            if (mileageGived.isMe()) {
                // view 객체 조회
                List<Report> reportList = reportRepository.findByMemberId(mileageGived.getMemberId());
                for(Report report : reportList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    report.setMileId(mileageGived.getId());
                    report.setPoint(mileageGived.getPoint());
                    report.setMileStatus(mileageGived.getStatus());
                    report.setMemberStatus("complete");
                    // view 레파지 토리에 save
                    reportRepository.save(report);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenMileageDeleted_then_UPDATE_2(@Payload MileageDeleted mileageDeleted) {
        try {
            if (mileageDeleted.isMe()) {
                // view 객체 조회
                List<Report> reportList = reportRepository.findByMemberId(mileageDeleted.getMemberId());
                for(Report report : reportList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    report.setMileStatus(mileageDeleted.getStatus());
                    report.setMemberStatus("end member");
                    report.setPoint(mileageDeleted.getPoint());
                    // view 레파지 토리에 save
                    reportRepository.save(report);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}