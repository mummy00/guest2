
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