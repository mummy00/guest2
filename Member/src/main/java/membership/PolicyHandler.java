package membership;

import membership.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PolicyHandler{
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @Autowired
    MemberMgmtRepository memberMgmtRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverMileageGived_MemberStatus(@Payload MileageGived mileageGived){

        if(mileageGived.isMe()){

            MemberMgmt memberMgmt = memberMgmtRepository.findById(mileageGived.getMemberId()).get();
            memberMgmt.setStatus("complete");
            memberMgmt.setMileageId(mileageGived.getId());
            memberMgmtRepository.save(memberMgmt);
        }
    }

}
