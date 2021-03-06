package membership;

import membership.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @Autowired
    MileageMgmtRepository mileageMgmtRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverSigned_MileageGive(@Payload Signed signed){

        if(signed.isMe()){
            MileageMgmt mileageMgmt = new MileageMgmt();
            mileageMgmt.setMemberId(signed.getId());
            mileageMgmt.setStatus("give point");
            mileageMgmt.setPoint(1000);

            mileageMgmtRepository.save(mileageMgmt);
        }
    }

}
