# guest

# 멤버십 가입

# Table of contents

- [멤버십 가입](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

# 서비스 시나리오

기능적 요구사항
1. 고객이 멤버십을 가입한다
1. 멤버십 가입이 완료 되면 마일리지를 준다
1. 고객이 멤버십을 탈퇴할 수 있다
1. 멤버십 탈퇴가 완료 되면 마일리지가 소멸된다
1. 고객은 가입 시 부여된 포인트, 등급을 조회할 수 있다
1. 멤버십 가입이 완료되면 메일로 알림을 보낸다

비기능적 요구사항
1. 트랜잭션
    1. 고객이 멤버십을 탈퇴할 때 반드시 마일리지 소멸이 전제되어야 한다  Sync 호출 
1. 장애격리
    1. 마일리지 부여 기능이 수행되지 않더라도 멤버십 가입은 365일 24시간 받을 수 있어야 한다  Async (event-driven), Eventual Consistency
    1. 마일리지 부여 기능이 과중되면 마일지지 부여 기능을 잠시동안 받지 않고 잠시후에 처리 하도록 유도한다  Circuit breaker, fallback
1. 성능
    1. 고객이 마이페이지에서 멤버십 등급 및 마일리지 상태를 확인할 수 있어야 한다  CQRS


# 분석/설계


### Event Storming 결과

![guest_model](https://user-images.githubusercontent.com/75401911/105178022-dbf72400-5b6a-11eb-9510-49a6117acb5e.png)

 
### 비기능 요구사항에 대한 검증

    - 마이크로 서비스를 넘나드는 시나리오에 대한 트랜잭션 처리
        - 멤버십 탈퇴 시 마일리지 처리:  마일리지가 소멸되지 않으면 멤버십 탈퇴가 되지 않는다는 경영자의 오랜 신념(?) 에 따라, ACID 트랜잭션 적용. 멤버십 탈퇴 전 마일리지 소멸 처리에 대해서는 Request-Response 방식 처리
        - 나머지 모든 inter-microservice 트랜잭션: 멤버십 가입 등 모든 이벤트와 같이 데이터 일관성의 시점이 크리티컬하지 않은 모든 경우가 대부분이라 판단, Eventual Consistency 를 기본으로 채택함.



# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8084 이다)

```
cd member
mvn spring-boot:run

cd mileage
mvn spring-boot:run 

cd report
mvn spring-boot:run  

cd gateway
mvn spring-boot:run 
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 member 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하려고 노력했다.

```
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


```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터소스 유형에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package membership;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface MemberMgmtRepository extends PagingAndSortingRepository<MemberMgmt, Long>{

}
```
- 적용 후 REST API 의 테스트
```
# member 서비스의 멤버십 가입
http POST http://localhost:8081/memberMgmts name='kim' grade='sliver'

# mileage 서비스의 마일리지 부여
http http://mileage:8080/mileageMgmts

# 마일리지 및 등급 조회
http http://report:8080/reports 

```

![member_sign](https://user-images.githubusercontent.com/75401911/105181279-f206e380-5b6e-11eb-9bda-9a2a83dac014.png)

![mileagegive](https://user-images.githubusercontent.com/75401911/105181480-32666180-5b6f-11eb-85d2-980a10dbb016.png)

![reports](https://user-images.githubusercontent.com/75401911/105181537-4316d780-5b6f-11eb-9df1-a218ec3f4efe.png)

## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 멤버 가입(member)-> 마일리지 삭제(mileage) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- mileage 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
# (member) MileageMgmtService.java

package membership.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="Mileage", url="${api.mileage.url}")
public interface MileageMgmtService {

    @RequestMapping(method= RequestMethod.POST, path="/mileageMgmts")
    public void mileageDelete(@RequestBody MileageMgmt mileageMgmt);

}

```

- 마일리지가 소멸 처리되면(@PreRemove) 멤버십 탈퇴가 가능하도록 처리 (데이터 삭제)
```
# MemberMgmt.java (Entity)

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
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, mileage 서비스가 장애나면 멤버십 탈퇴도 안된다는 것을 확인:


```
# 마일리지 (Mileage) 서비스를 잠시 내려놓음 

# 멤버십 탈퇴 처리
http DELETE http://member:8080/memberMgmts/1   #Fail

# 마일리지 서비스 재기동
cd mileage
mvn spring-boot:run

# 멤버십 탈퇴 처리
http DELETE http://member:8080/memberMgmts/1   #Success

```
- 마일리지 서비스 내린 경우

![member_del_err](https://user-images.githubusercontent.com/75401911/105185774-4cef0980-5b74-11eb-83ad-a562bc108cdd.png)

- 마일리지 서비스 재기동한 경우

![member_del_ok](https://user-images.githubusercontent.com/75401911/105185813-58423500-5b74-11eb-8621-a933224b03d7.png)


- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)



## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트


멤버십 가입이 이루어진 후에 마일리지 서비스로 이를 알려주는 행위는 동기식이 아니라 비 동기식으로 처리하여 마일리지 서비스 처리를 위하여 멤버십 서비스가 블로킹 되지 않아도록 처리한다.
 
- 이를 위하여 멤버십 이력에 기록을 남긴 후에 곧바로 마일리지가 부여되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
(MemberMgmt.java)

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
    
```

- 마일리지 서비스에서는 마일리지 적립 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
(PolicyHandler.java)

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

```
member 와 mileage 가 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, mileage 서비스가 유지보수로 인해 잠시 내려간 멤버십 가입을 받는데 문제가 없다:
  
```

( MileageMgmt.java)

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

```

```
# Mileage 서비스를 잠시 내려놓음 

# 멤버십 가입
http POST http://member:8080/memberMgmts name='lee' grade='black'   #Success

# 마일리지 확인
http http://report:8080/reports   #  마일리지 조회 안됨

# 마일리지 서비스 재기동
cd mileage
mvn spring-boot:run

# 마일리지 재확인
http http://report:8080/reports     # 마일리지 조회됨
```
마일리지 조회 안됨

![report_pub_er](https://user-images.githubusercontent.com/75401911/105189327-35b21b00-5b78-11eb-8c9c-272e71f8ed88.png)

마일리지 조회됨

![report_pub_ok](https://user-images.githubusercontent.com/75401911/105189386-42cf0a00-5b78-11eb-8def-428ca0bb2e95.png)

```

마이페이지 서비스(report)는 meber/mileage 서비스와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 마이페이지(report) 서비스가 유지보수로 인해 잠시 내려간 상태라도 멤버십 가입을 받는데 문제가 없다:

```

(ReportRepository.java)

package membership;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends CrudRepository<Report, Long> {

    List<Report> findByMemberId(Long memberId);

}

```
# 마이페이지 서비스 (report) 를 잠시 내려놓음 

# 멤버십 가입
http POST http://member:8080/memberMgmts name='cha' grade='gold'   #Success

# 마이페이지(report) 확인
http http://report:8080/reports      # 마일리지 목록이 조회안됨

# 마이페이지(report) 서비스 기동
cd report
mvn spring-boot:run

# 마이페이지 확인
http http://report:8080/reports       # 마일리지 목록이 조회됨
```

![cqrs_member_assign](https://user-images.githubusercontent.com/75401911/105195288-d951fa00-5b7d-11eb-9b8a-88d46b20a0ee.png)

![cqrs_report_er](https://user-images.githubusercontent.com/75401911/105195364-ea9b0680-5b7d-11eb-8515-e582584489bb.png)

![cqrs_report_ok](https://user-images.githubusercontent.com/75401911/105195426-f71f5f00-5b7d-11eb-8e94-0d14372c2600.png)

# 운영

## CI/CD 설정



## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함

시나리오는 멤버십 가입(member)--> 마일리지 (mileage) 시의 연결을 RESTful Request/Response 으로 구현이 되어있고, 멤버십 가입이 과도할 경우 CB 를 통하여 장애격리.

- Hystrix 를 설정:  요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# application.yml (member)

feign:
  hystrix:
    enabled: true

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610

```

- 피호출 서비스( mileage) 의 임의 부하 처리 - 3000 밀리에서 증감 3220 밀리 정도 왔다갔다 하게, Thread.currentThread().sleep((long) (3000 + Math.random() * 220));
```
# MileageMgmtService.java (Entity)

    @PrePersist
    public void onPrePersist(){
        try {
            Thread.currentThread().sleep((long) (3000 + Math.random() * 220));
            // Thread.currentThread().sleep((long) (800 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 40명
- 600초 동안 실시

```
siege -c40 -t600S -v --content-type "application/json" 'http://member:8080/memberMgmts POST {"name": "kim", "grade":"silver"}'
:
:

```

![cb_3](https://user-images.githubusercontent.com/75401911/105259923-2068db00-5bd0-11eb-800f-3fa866a83159.png)

![cb_0](https://user-images.githubusercontent.com/75401911/105260824-d254d700-5bd1-11eb-9e93-1d58258a42b6.png)

- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌.


### 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 
```
deployment.yaml(member) 수정
          resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
```

- member 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:
```
kubectl autoscale deploy order --min=1 --max=10 --cpu-percent=15
```
- CB 에서 했던 방식대로 워크로드를 1.5분 정도 걸어준다.
```
siege -c40 -t100S -v --content-type "application/json" 'http://member:8080/memberMgmts POST {"name": "kim", "grade":"silver"}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다
```
kubectl get pod -w
kubectl get hpa
```
- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:

![auto1](https://user-images.githubusercontent.com/75401911/105198986-98f47b00-5b81-11eb-88e6-0dce0c47838c.png)

![auto2](https://user-images.githubusercontent.com/75401911/105199031-a4e03d00-5b81-11eb-9390-9332134b8f76.png)

![auto3](https://user-images.githubusercontent.com/75401911/105199088-b32e5900-5b81-11eb-8b1a-9e43d46883c0.png)

## 무정지 재배포

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함

mileage 서비스(deployment.yaml) readiness probe 없는 상태

- seige 로 배포작업 직전에 워크로드를 모니터링 함.
```
siege -c40 -t100S -v --content-type "application/json" 'http://member:8080/memberMgmts POST {"name": "kim", "grade":"silver"}'

```

- 새버전으로의 배포 시작
```
kubectl set image deploy member member=guest.azurecr.io/member:latest --record
```

- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인

![read_ok2](https://user-images.githubusercontent.com/75401911/105199948-aa8a5280-5b82-11eb-8029-1fcb09dd1c57.png)


배포기간중 Availability 가 평소 100%에서 85.36% 대로 떨어지는 것을 확인. 원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함:

```
# deployment.yaml 의 readiness probe 의 설정:


kubectl apply -f deployment.yml
```

- 동일한 시나리오로 재배포 한 후 Availability 확인:

![read_ok](https://user-images.githubusercontent.com/75401911/105199654-57180480-5b82-11eb-8339-f9ee912e7195.png)

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.




