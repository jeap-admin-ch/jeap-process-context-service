package ch.admin.bit.jeap.processcontext.adapter.objectstorage;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.regions.Region;

import static org.springframework.util.StringUtils.hasText;

@Getter
@ToString
public class S3ObjectStorageConnectionProperties {

    @Setter
    private String accessUrl;

    private Region region = Region.AWS_GLOBAL;

    @Setter
    // excluded from toString for security reasons
    @ToString.Exclude
    private String accessKey;

    @Setter
    // excluded from toString for security reasons
    @ToString.Exclude
    private String secretKey;

    public void setRegion(String region) {
        this.region = Region.of(region);
    }

    @ToString.Include
    public boolean accessCredentialsConfigured() {
        return hasText(accessKey) && hasText(secretKey);
    }

}
